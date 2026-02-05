package com.smartretail.backend.service;

import com.smartretail.backend.exception.DuplicateResourceException;
import com.smartretail.backend.exception.ProductNotFoundException;
import com.smartretail.backend.models.Product;
import com.smartretail.backend.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.smartretail.backend.security.SecurityUtils;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductServiceImpl.class);

    private final ProductRepository productRepository;
    private final NotificationService notificationService;
    private final FileService fileService;
    private final MessageSource messageSource;
    private final AuditLogService auditLogService;
    private final SecurityUtils securityUtils;

    @Autowired
    public ProductServiceImpl(ProductRepository productRepository,
                              NotificationService notificationService,
                              FileService fileService,
                              MessageSource messageSource,
                              AuditLogService auditLogService,
                              SecurityUtils securityUtils) {
        this.productRepository = productRepository;
        this.notificationService = notificationService;
        this.fileService = fileService;
        this.messageSource = messageSource;
        this.auditLogService = auditLogService;
        this.securityUtils = securityUtils;
    }

    private String currentUser() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(auth -> auth.getName())
                .orElse("system");
    }

    private void audit(String action, String entityId, Map<String, Object> details) {
        auditLogService.logAction(action, entityId, currentUser(), details);
    }

    /* ------------------- EXISTS ------------------- */
    @Override
    public boolean existsByProductId(String productId) {
        String shopId = securityUtils.getCurrentShopId();
        audit("PRODUCT_EXISTENCE_CHECKED", productId, Map.of("productId", productId));
        return productRepository.existsByProductIdAndShopId(productId, shopId);
    }

    /* ------------------- CREATE ------------------- */
    @Override
    @Transactional
    public Product createProduct(Product product, MultipartFile imageFile, Locale locale) throws IOException {
        logger.debug("[SERVICE] Creating product (image={}) – {}", imageFile != null, product.getName());

        String shopId = securityUtils.getCurrentShopId();
        if (productRepository.existsByProductIdAndShopId(product.getProductId(), shopId)) {
            audit("PRODUCT_CREATION_FAILED", product.getProductId(),
                    Map.of("productId", product.getProductId(), "reason", "DUPLICATE_PRODUCT_ID"));
            throw new DuplicateResourceException("product.exists", product.getProductId());
        }

        String imageId = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            imageId = fileService.uploadImage(imageFile, "product_" + product.getProductId());
            product.setImageId(imageId);
            product.setImageUrl("/api/products/image/" + imageId);
        }

        product.setShopId(shopId);
        product.setCreatedAt(new Date());
        product.setLastUpdated(new Date());
        Product saved = productRepository.save(product);

        audit("PRODUCT_CREATED", saved.getProductId(), Map.of(
                "name", saved.getName(),
                "category", saved.getCategory(),
                "price", saved.getPrice(),
                "quantity", saved.getQuantity(),
                "minQuantity", saved.getMinQuantity(),
                "reorderLevel", saved.getReorderLevel(),
                "hasImage", imageId != null));

        logger.info("[SERVICE] Product created – {}", saved.getProductId());
        return saved;
    }

    @Override
    @Transactional
    public Product createProduct(Product product, Locale locale) {
        try {
            return createProduct(product, null, locale);
        } catch (IOException e) {
            throw new RuntimeException("Unexpected IO error", e);
        }
    }

    /* ------------------- READ ------------------- */
    @Override
    public Product getProductById(String productId, Locale locale) {
        String shopId = securityUtils.getCurrentShopId();
        return productRepository.findByProductIdAndShopId(productId, shopId)
                .map(p -> {
                    audit("PRODUCT_ACCESSED", productId, Map.of("name", p.getName()));
                    return p;
                })
                .orElseThrow(() -> {
                    audit("PRODUCT_ACCESS_FAILED", productId, Map.of("reason", "PRODUCT_NOT_FOUND"));
                    return new ProductNotFoundException(
                            messageSource.getMessage("product.not.found", new Object[]{productId}, locale));
                });
    }

    @Override
    public List<Product> getAllProducts() {
        String shopId = securityUtils.getCurrentShopId();
        audit("PRODUCTS_BULK_ACCESSED", "ALL", Map.of("action", "FETCH_ALL"));
        return productRepository.findByShopId(shopId);
    }

    /* ------------------- UPDATE (multipart) ------------------- */
    @Override
    @Transactional
    public Product updateProduct(String productId, Product patch, MultipartFile imageFile, Locale locale) throws IOException {
        String shopId = securityUtils.getCurrentShopId();
        Product existing = productRepository.findByProductIdAndShopId(productId, shopId)
                .orElseThrow(() -> {
                    audit("PRODUCT_UPDATE_FAILED", productId, Map.of("reason", "PRODUCT_NOT_FOUND"));
                    return new ProductNotFoundException(
                            messageSource.getMessage("product.not.found", new Object[]{productId}, locale));
                });

        Map<String, Object> oldVals = new HashMap<>();
        Map<String, Object> newVals = new HashMap<>();

        // ----- fields -----
        // Use all fields from the 'patch' object, not just a few
        updateIfChanged(existing::getName, existing::setName, patch.getName(), "name", oldVals, newVals);
        updateIfChanged(existing::getCategory, existing::setCategory, patch.getCategory(), "category", oldVals, newVals);

        // --- FIX: Use patch.getPrice() and patch.getQuantity() ---
        if (patch.getPrice() > 0) { // Check for valid price
            updateIfChanged(() -> existing.getPrice(), v -> existing.setPrice(v), patch.getPrice(), "price", oldVals, newVals);
        }
        if (patch.getQuantity() >= 0) { // Check for valid quantity
            updateIfChanged(existing::getQuantity, existing::setQuantity, patch.getQuantity(), "quantity", oldVals, newVals);
        }
        if (patch.getMinQuantity() > 0) {
            updateIfChanged(existing::getMinQuantity, existing::setMinQuantity, patch.getMinQuantity(), "minQuantity", oldVals, newVals);
        }
        if (patch.getReorderLevel() > 0) {
            updateIfChanged(existing::getReorderLevel, existing::setReorderLevel, patch.getReorderLevel(), "reorderLevel", oldVals, newVals);
        }
        // --- END FIX ---

        updateIfChanged(existing::getExpiryDate, existing::setExpiryDate, patch.getExpiryDate(), "expiryDate", oldVals, newVals);
        updateIfChanged(existing::getSupplierEmail, existing::setSupplierEmail, patch.getSupplierEmail(), "supplierEmail", oldVals, newVals);
        updateIfChanged(existing::getAddedBy, existing::setAddedBy, patch.getAddedBy(), "addedBy", oldVals, newVals);

        // Don't update imageUrl from JSON patch, only from imageFile
        // updateIfChanged(existing::getImageUrl, existing::setImageUrl, patch.getImageUrl(), "imageUrl", oldVals, newVals);

        // ----- image -----
        if (imageFile != null && !imageFile.isEmpty()) {
            String newId = fileService.uploadImage(imageFile, "product_" + productId);
            String newUrl = "/api/products/image/" + newId; // Use the correct URL structure
            oldVals.put("imageId", existing.getImageId());
            newVals.put("imageId", newId);
            newVals.put("imageUrl", newUrl);
            existing.setImageId(newId);
            existing.setImageUrl(newUrl); // Set the URL
        }

        existing.setLastUpdated(new Date());
        Product updated = productRepository.save(existing);

        boolean lowStock = sendLowStockIfNeeded(updated);
        audit("PRODUCT_UPDATED", productId, Map.of(
                "oldValues", oldVals,
                "newValues", newVals,
                "lowStockNotificationSent", lowStock));

        return updated;
    }

    @Override
    @Transactional
    public Product updateProduct(String productId, Product patch, Locale locale) {
        try {
            return updateProduct(productId, patch, null, locale);
        } catch (IOException e) {
            throw new RuntimeException("Unexpected IO error", e);
        }
    }

    /* ------------------- SYNC-MODE UPDATE ------------------- */
    @Override
    @Transactional
    public Product updateProduct(String productId, Product patch, Locale locale, boolean isSyncMode) {
        String shopId = securityUtils.getCurrentShopId();
        Product existing = productRepository.findByProductIdAndShopId(productId, shopId)
                .orElseThrow(() -> new ProductNotFoundException(
                        messageSource.getMessage("product.not.found", new Object[]{productId}, locale)));

        if (isSyncMode && noChanges(existing, patch)) {
            logger.info("[SERVICE] Sync-mode: no changes for {}", productId);
            return existing;
        }

        Map<String, Object> oldVals = new HashMap<>();
        Map<String, Object> newVals = new HashMap<>();

        updateIfChanged(existing::getName, existing::setName, patch.getName(), "name", oldVals, newVals);
        updateIfChanged(existing::getCategory, existing::setCategory, patch.getCategory(), "category", oldVals, newVals);
        updateIfChanged(() -> existing.getPrice(), v -> existing.setPrice(v), patch.getPrice(), "price", oldVals, newVals);
        updateIfChanged(existing::getQuantity, existing::setQuantity, patch.getQuantity(), "quantity", oldVals, newVals);
        updateIfChanged(existing::getMinQuantity, existing::setMinQuantity, patch.getMinQuantity(), "minQuantity", oldVals, newVals);
        updateIfChanged(existing::getReorderLevel, existing::setReorderLevel, patch.getReorderLevel(), "reorderLevel", oldVals, newVals);
        updateIfChanged(existing::getExpiryDate, existing::setExpiryDate, patch.getExpiryDate(), "expiryDate", oldVals, newVals);
        updateIfChanged(existing::getSupplierEmail, existing::setSupplierEmail, patch.getSupplierEmail(), "supplierEmail", oldVals, newVals);
        updateIfChanged(existing::getAddedBy, existing::setAddedBy, patch.getAddedBy(), "addedBy", oldVals, newVals);
        updateIfChanged(existing::getImageUrl, existing::setImageUrl, patch.getImageUrl(), "imageUrl", oldVals, newVals);

        existing.setLastUpdated(new Date());
        Product updated = productRepository.save(existing);

        boolean lowStock = !isSyncMode && sendLowStockIfNeeded(updated);
        audit("PRODUCT_UPDATED", productId, Map.of(
                "oldValues", oldVals,
                "newValues", newVals,
                "syncMode", isSyncMode,
                "lowStockNotificationSent", lowStock));

        return updated;
    }

    private boolean noChanges(Product existing, Product incoming) {
        return Objects.equals(existing.getQuantity(), incoming.getQuantity())
                && Objects.equals(existing.getName(), incoming.getName())
                && existing.getPrice() == incoming.getPrice()
                && Objects.equals(existing.getCategory(), incoming.getCategory())
                && existing.getMinQuantity() == incoming.getMinQuantity()
                && existing.getReorderLevel() == incoming.getReorderLevel();
    }

    private boolean sendLowStockIfNeeded(Product p) {
        if (p.getQuantity() < p.getReorderLevel()) {
            notificationService.sendLowStockNotification(
                    "manager@shop.com", p.getName(), p.getQuantity(), p.getReorderLevel());
            return true;
        }
        return false;
    }

    /* ------------------- DELETE ------------------- */
    @Override
    @Transactional
    public void deleteProduct(String productId, Locale locale) {
        String shopId = securityUtils.getCurrentShopId();
        Product p = productRepository.findByProductIdAndShopId(productId, shopId)
                .orElseThrow(() -> {
                    audit("PRODUCT_DELETION_FAILED", productId, Map.of("reason", "PRODUCT_NOT_FOUND"));
                    return new ProductNotFoundException(
                            messageSource.getMessage("product.not.found", new Object[]{productId}, locale));
                });

        audit("PRODUCT_DELETED", productId, Map.of(
                "name", p.getName(),
                "category", p.getCategory(),
                "quantity", p.getQuantity(),
                "price", p.getPrice(),
                "minQuantity", p.getMinQuantity()));

        productRepository.deleteByProductIdAndShopId(productId, shopId);
    }

    /* ------------------- LOW STOCK / EXPIRY ------------------- */
    @Override
    public List<Product> getLowStockProducts() {
        String shopId = securityUtils.getCurrentShopId();
        audit("LOW_STOCK_PRODUCTS_ACCESSED", "LOW_STOCK", Map.of());
        return productRepository.findLowStockProducts(shopId);
    }

    @Override
    public List<Product> getExpiringProducts(Date threshold) {
        String shopId = securityUtils.getCurrentShopId();
        audit("EXPIRING_PRODUCTS_ACCESSED", "EXPIRING", Map.of("threshold", threshold));
        return productRepository.findByShopIdAndExpiryDateBefore(shopId, threshold).stream()
                .filter(p -> p.getExpiryDate() != null)
                .collect(Collectors.toList());
    }

    /* ------------------- RESTOCK ------------------- */
    @Override
    @Transactional
    public Product restockProduct(String productId, int restockQty, Locale locale) {
        if (restockQty <= 0) {
            audit("PRODUCT_RESTOCK_FAILED", productId, Map.of("reason", "INVALID_QUANTITY"));
            throw new IllegalArgumentException(
                    messageSource.getMessage("product.restock.invalid.quantity", new Object[]{restockQty}, locale));
        }

        String shopId = securityUtils.getCurrentShopId();
        Product p = productRepository.findByProductIdAndShopId(productId, shopId)
                .orElseThrow(() -> {
                    audit("PRODUCT_RESTOCK_FAILED", productId, Map.of("reason", "PRODUCT_NOT_FOUND"));
                    return new ProductNotFoundException(
                            messageSource.getMessage("product.not.found", new Object[]{productId}, locale));
                });

        int oldQty = p.getQuantity();
        p.setQuantity(oldQty + restockQty);
        p.setLastUpdated(new Date());
        Product saved = productRepository.save(p);

        boolean low = sendLowStockIfNeeded(saved);
        audit("PRODUCT_RESTOCKED", productId, Map.of(
                "oldQuantity", oldQty,
                "restockQty", restockQty,
                "newQuantity", saved.getQuantity(),
                "lowStockNotificationSent", low));

        return saved;
    }

    /* ------------------- QUANTITY UPDATE ------------------- */
    @Override
    @Transactional
    public Product updateProductQuantity(String productId, int quantity, Locale locale) {
        String shopId = securityUtils.getCurrentShopId();
        Product p = productRepository.findByProductIdAndShopId(productId, shopId)
                .orElseThrow(() -> {
                    audit("PRODUCT_QUANTITY_UPDATE_FAILED", productId, Map.of("reason", "PRODUCT_NOT_FOUND"));
                    return new ProductNotFoundException(
                            messageSource.getMessage("product.not.found", new Object[]{productId}, locale));
                });

        int oldQty = p.getQuantity();
        int newQty = oldQty - quantity;
        if (newQty < 0) {
            audit("PRODUCT_QUANTITY_UPDATE_FAILED", productId, Map.of(
                    "oldQuantity", oldQty,
                    "quantityChange", quantity,
                    "reason", "INSUFFICIENT_STOCK"));
            throw new IllegalArgumentException(
                    messageSource.getMessage("product.insufficient.stock", new Object[]{productId}, locale));
        }

        p.setQuantity(newQty);
        p.setLastUpdated(new Date());
        Product saved = productRepository.save(p);

        boolean low = sendLowStockIfNeeded(saved);
        audit("PRODUCT_QUANTITY_UPDATED", productId, Map.of(
                "oldQuantity", oldQty,
                "quantityChange", quantity,
                "newQuantity", newQty,
                "lowStockNotificationSent", low));

        return saved;
    }

    /* ------------------- FIELD PATCH HELPER ------------------- */
    private <T> void updateIfChanged(Supplier<T> getter, Consumer<T> setter,
                                     T incoming, String field,
                                     Map<String, Object> oldMap,
                                     Map<String, Object> newMap) {
        T current = getter.get();
        if (incoming != null && !Objects.equals(incoming, current)) {
            oldMap.put(field, current);
            newMap.put(field, incoming);
            setter.accept(incoming);
        }
    }
}