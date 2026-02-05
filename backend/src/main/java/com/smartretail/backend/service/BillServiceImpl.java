package com.smartretail.backend.service;

import com.smartretail.backend.models.Bill;
import com.smartretail.backend.models.Customer;
import com.smartretail.backend.models.Product;
import com.smartretail.backend.repository.BillRepository;
import com.smartretail.backend.repository.CustomerRepository;
import com.smartretail.backend.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.smartretail.backend.security.SecurityUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;
import org.springframework.web.client.RestTemplate;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@Service
public class BillServiceImpl implements BillService {

    private static final Logger logger = LoggerFactory.getLogger(BillServiceImpl.class);
    private final BillRepository billRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;
    private final NotificationService notificationService;
    private final PdfService pdfService;
    private final MessageSource messageSource;
    private final AuditLogService auditLogService;
    private final SecurityUtils securityUtils;

    public BillServiceImpl(BillRepository billRepository,
            CustomerRepository customerRepository,
            ProductRepository productRepository,
            ProductService productService,
            NotificationService notificationService,
            PdfService pdfService,
            MessageSource messageSource,
            AuditLogService auditLogService,
            SecurityUtils securityUtils) {
        this.billRepository = billRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.productService = productService;
        this.notificationService = notificationService;
        this.pdfService = pdfService;
        this.messageSource = messageSource;
        this.auditLogService = auditLogService;
        this.securityUtils = securityUtils;
    }

    // --- RECOMMENDATION ENGINE (Proxy & Cache) ---
    private final RestTemplate restTemplate = new RestTemplate();
    private final String ANALYTICS_URL = "http://localhost:5001/analytics";

    // Simple Cache: Key=SortedItemIds, Value={Timestamp, List<Product>}
    private final Map<String, CacheEntry> recommendationCache = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long CACHE_TTL = 5 * 60 * 1000; // 5 Minutes

    private static class CacheEntry {
        long timestamp;
        List<Product> suggestions;

        public CacheEntry(List<Product> suggestions) {
            this.timestamp = System.currentTimeMillis();
            this.suggestions = suggestions;
        }
    }

    @Override
    public List<Product> getUpsellSuggestions(List<String> currentItemIds) {
        if (currentItemIds == null || currentItemIds.isEmpty())
            return new ArrayList<>();

        // 1. Check Cache
        String cacheKey = currentItemIds.stream().sorted().collect(Collectors.joining(","));
        CacheEntry entry = recommendationCache.get(cacheKey);
        if (entry != null && (System.currentTimeMillis() - entry.timestamp < CACHE_TTL)) {
            logger.debug("[RECOMMEND] Cache Hit for: {}", cacheKey);
            return entry.suggestions;
        }

        // 2. Call Python Analytics Service
        try {
            logger.debug("[RECOMMEND] Fetching from AI for: {}", cacheKey);
            String shopId = securityUtils.getCurrentShopId();

            Map<String, Object> request = new HashMap<>();
            request.put("cart", currentItemIds);
            request.put("shopId", shopId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            // Expecting: { "recommendations": [ { "productId": "...", ... } ] }
            Map response = restTemplate.postForObject(ANALYTICS_URL + "/recommend", entity, Map.class);

            if (response != null && response.containsKey("recommendations")) {
                List<Map<String, Object>> recs = (List<Map<String, Object>>) response.get("recommendations");
                List<Product> suggestions = new ArrayList<>();

                for (Map<String, Object> rec : recs) {
                    Product p = new Product();
                    p.setProductId((String) rec.get("productId"));
                    p.setName((String) rec.get("name"));
                    // Safe cast for price
                    Object priceObj = rec.get("price");
                    if (priceObj instanceof Number) {
                        p.setPrice(((Number) priceObj).doubleValue());
                    }
                    suggestions.add(p);
                }

                // 3. Update Cache
                recommendationCache.put(cacheKey, new CacheEntry(suggestions));
                return suggestions;
            }
        } catch (Exception e) {
            logger.error("[RECOMMEND] Failed to fetch suggestions: {}", e.getMessage());
        }

        return new ArrayList<>();
    }

    @Override
    public void triggerRecommendationTraining() {
        try {
            restTemplate.postForObject(ANALYTICS_URL + "/train-recommendations", null, String.class);
            logger.info("[RECOMMEND] Training triggered successfully");
        } catch (Exception e) {
            logger.error("[RECOMMEND] Failed to trigger training: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public Bill createBill(Bill bill, Locale locale) {
        return createBill(bill, locale, false);
    }

    @Override
    @Transactional
    public Bill createBill(Bill bill, Locale locale, boolean isSyncMode) {
        logger.debug("[BILL SERVICE] Creating bill with ID: {}", bill.getBillId());

        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        String shopId = securityUtils.getCurrentShopId();
        bill.setShopId(shopId);

        // ✅ Auto-generate Bill ID if missing
        if (bill.getBillId() == null || bill.getBillId().trim().isEmpty()) {
            bill.setBillId("B" + System.currentTimeMillis());
            logger.debug("[BILL SERVICE] Auto-generated bill ID: {}", bill.getBillId());
        }

        // ✅ AddedBy fallback
        if (bill.getAddedBy() == null || bill.getAddedBy().trim().isEmpty()) {
            bill.setAddedBy(userEmail != null ? userEmail : "system");
        }

        // ✅ Prevent duplicates
        if (!isSyncMode && billRepository.existsByBillIdAndShopId(bill.getBillId(), shopId)) {
            throw new RuntimeException("Bill already exists: " + bill.getBillId());
        }

        // ✅ Validate customer
        if (bill.getCustomer() == null || bill.getCustomer().getMobile() == null) {
            throw new IllegalArgumentException("Customer details are required");
        }

        // ✅ Recalculate totalAmount based on product prices from DB
        double total = 0.0;
        if (bill.getItems() == null || bill.getItems().isEmpty()) {
            logger.warn("[BILL SERVICE] Bill {} has no items!", bill.getBillId());
        } else {
            for (Bill.BillItem item : bill.getItems()) {
                if (item.getProductId() == null || item.getProductId().trim().isEmpty()) {
                    logger.warn("[BILL SERVICE] Skipping invalid item without productId");
                    continue;
                }

                Product product = productService.getProductById(item.getProductId(), locale);
                if (product == null) {
                    logger.error("[BILL SERVICE] Product not found for ID: {}", item.getProductId());
                    continue;
                }

                item.setProductName(product.getName());
                item.setPrice(product.getPrice());

                double itemTotal = item.getItemTotal();
                total += itemTotal;

                logger.debug("[BILL SERVICE] {} × {} = {}", product.getName(), item.getQty(), itemTotal);

                // ✅ Update stock after sale
                productService.updateProductQuantity(item.getProductId(), item.getQty(), locale);
            }
        }

        // ✅ Calculate total again using built-in method (redundancy check)
        double computedTotal = bill.calculateTotal();
        if (computedTotal > 0)
            total = computedTotal;

        bill.setTotalAmount(total);
        bill.setCreatedAt(new Date());

        // ✅ Generate PDF access token using the new method
        if (bill.getPdfAccessToken() == null || bill.getPdfAccessToken().trim().isEmpty()) {
            bill.setPdfAccessToken(generatePdfAccessToken(bill.getBillId()));
            logger.debug("[BILL SERVICE] Generated new PDF access token for {}", bill.getBillId());
        }

        logger.debug("[BILL SERVICE] Final total for bill {} = ₹{}", bill.getBillId(), total);

        // ✅ Customer management
        Customer customer = customerRepository.findByMobileAndShopId(bill.getCustomer().getMobile(), shopId)
                .orElseGet(() -> {
                    return new Customer(
                            bill.getCustomer().getName(),
                            bill.getCustomer().getEmail(),
                            bill.getCustomer().getMobile(),
                            shopId);
                });

        // ✅ Update customer stats
        customer.setTotalPurchaseCount(customer.getTotalPurchaseCount() + 1);
        customer.setLastPurchaseDate(new Date());
        if (customer.getPurchaseHistory() == null) {
            customer.setPurchaseHistory(new ArrayList<>());
        }
        if (!customer.getPurchaseHistory().contains(bill.getBillId())) {
            customer.getPurchaseHistory().add(bill.getBillId());
        }

        customerRepository.save(customer);

        // ✅ Save Bill
        Bill savedBill = billRepository.save(bill);

        // ✅ Recalculate and persist totalAmount again
        savedBill.setTotalAmount(savedBill.calculateTotal());
        savedBill = billRepository.save(savedBill);

        logger.info("[BILL SERVICE] Bill {} saved successfully with total ₹{}", savedBill.getBillId(),
                savedBill.getTotalAmount());

        // ✅ Log action
        auditLogService.logAction("BILL_CREATED", savedBill.getBillId(), userEmail, Map.of(
                "totalAmount", savedBill.getTotalAmount(),
                "customerEmail", customer.getEmail()));

        // ✅ Optional: Send PDF + Email
        try {
            if (bill.getCustomer().getEmail() != null && !bill.getCustomer().getEmail().isBlank()) {
                byte[] pdfContent = pdfService.generateBillPdf(savedBill, locale);
                notificationService.sendBillNotification(
                        bill.getCustomer().getEmail(),
                        savedBill.getBillId(),
                        savedBill.getTotalAmount(),
                        pdfContent,
                        locale);
                logger.info("[BILL SERVICE] Email sent for bill {}", savedBill.getBillId());
            }
        } catch (Exception e) {
            logger.error("[BILL SERVICE] Failed to send email for bill {}: {}", savedBill.getBillId(), e.getMessage());
        }

        final String finalShopId = shopId;
        final Bill finalSavedBill = savedBill;

        // ✅ Async Trigger for ML Model Update
        CompletableFuture.runAsync(() -> {
            try {
                Set<String> productIds = finalSavedBill.getItems().stream()
                        .map(Bill.BillItem::getProductId)
                        .collect(Collectors.toSet());

                if (!productIds.isEmpty()) {
                    String analyticsUrl = "http://127.0.0.1:5000/analytics/update-model"; // Python service URL
                    RestTemplate restTemplate = new RestTemplate();

                    Map<String, Object> payload = new HashMap<>();
                    payload.put("shopId", finalShopId);
                    payload.put("productIds", productIds);

                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

                    restTemplate.postForObject(analyticsUrl, request, String.class);
                    logger.info("[BILL SERVICE] Triggered ML update for {} products", productIds.size());
                }
            } catch (Exception e) {
                // Log warning but don't fail the transaction
                logger.warn("[BILL SERVICE] Failed to trigger ML update: {}", e.getMessage());
            }
        });

        return savedBill;
    }

    @Override
    public Bill getBillById(String billId, Locale locale) {
        String shopId = securityUtils.getCurrentShopId();
        return billRepository.findByBillIdAndShopId(billId, shopId)
                .orElseThrow(() -> new RuntimeException("Bill not found: " + billId));
    }

    @Override
    public List<Bill> getAllBills() {
        String shopId = securityUtils.getCurrentShopId();
        return billRepository.findByShopId(shopId);
    }

    @Override
    public List<Bill> getBillsByDateRange(Date startDate, Date endDate, Locale locale) {
        String shopId = securityUtils.getCurrentShopId();
        return billRepository.findByShopIdAndCreatedAtBetween(shopId, startDate, endDate);
    }

    @Override
    public boolean validatePdfAccessToken(String billId, String token) {
        try {
            // Token format: billId|randomUuid|exp (e.g. "B123|uuid|1734000000")
            String[] parts = token.split("\\|");
            if (parts.length != 3)
                return false;
            if (!parts[0].equals(billId))
                return false;

            long exp = Long.parseLong(parts[2]);
            if (System.currentTimeMillis() > exp * 1000) {
                logger.warn("[BILL SERVICE] PDF token expired for bill: {}", billId);
                return false;
            }

            // Optional: You could add signature verification here if needed
            return true;
        } catch (Exception e) {
            logger.error("[BILL SERVICE] Invalid PDF token format for bill {}: {}", billId, e.getMessage());
            return false;
        }
    }

    /**
     * Generate a short-lived PDF access token
     * Format: billId|randomUuid|expirationTimestamp
     */
    public String generatePdfAccessToken(String billId) {
        String uuid = UUID.randomUUID().toString();
        long exp = System.currentTimeMillis() / 1000 + 3600; // 1 hour expiration
        return billId + "|" + uuid + "|" + exp;
    }

    @Override
    public boolean existsByBillId(String billId) {
        String shopId = securityUtils.getCurrentShopId();
        return billRepository.existsByBillIdAndShopId(billId, shopId);
    }

    @Override
    public void resendBillEmail(Bill bill, Locale locale) {
        try {
            byte[] pdfContent = pdfService.generateBillPdf(bill, locale);
            notificationService.sendBillNotification(
                    bill.getCustomer().getEmail(),
                    bill.getBillId(),
                    bill.getTotalAmount(),
                    pdfContent,
                    locale);
            logger.info("[BILL SERVICE] Bill email resent to {}", bill.getCustomer().getEmail());
        } catch (Exception e) {
            logger.error("[BILL SERVICE] Failed to resend email for bill {}: {}", bill.getBillId(), e.getMessage());
        }
    }
}