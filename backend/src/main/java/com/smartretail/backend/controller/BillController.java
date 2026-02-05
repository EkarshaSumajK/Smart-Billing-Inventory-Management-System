package com.smartretail.backend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.bean.CsvToBeanBuilder;
import com.smartretail.backend.models.Bill;
import com.smartretail.backend.service.BillService;
import com.smartretail.backend.service.PdfService;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/api/bills")
@PreAuthorize("permitAll()")
@CrossOrigin(origins = "*")
public class BillController {

    private static final Logger logger = LoggerFactory.getLogger(BillController.class);
    private final BillService billService;
    private final PdfService pdfService;
    private final MessageSource messageSource;
    private final ObjectMapper objectMapper;

    public BillController(BillService billService, PdfService pdfService, MessageSource messageSource) {
        this.billService = billService;
        this.pdfService = pdfService;
        this.messageSource = messageSource;
        this.objectMapper = new ObjectMapper();
    }

    // ✅ Create a new bill
    @PostMapping
    public ResponseEntity<Map<String, Object>> createBill(@RequestBody Bill bill) {
        Locale locale = LocaleContextHolder.getLocale();
        logger.info("[BILL CONTROLLER] Creating bill request received: {}", bill.getBillId());

        // Defensive validation
        if (bill.getItems() == null || bill.getItems().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Bill items cannot be empty"));
        }

        // ✅ Ensure total is recalculated even if frontend didn't send it
        bill.setTotalAmount(bill.calculateTotal());

        // Save using service
        Bill createdBill = billService.createBill(bill, locale);

        // ✅ Generate PDF access token for immediate download
        String pdfToken = billService.generatePdfAccessToken(createdBill.getBillId());

        logger.info("[BILL CONTROLLER] Bill {} created successfully. Total = ₹{}",
                createdBill.getBillId(), createdBill.getTotalAmount());

        // Return bill along with PDF token
        Map<String, Object> response = new HashMap<>();
        response.put("bill", createdBill);
        response.put("pdfAccessToken", pdfToken);
        response.put("pdfDownloadUrl", "/api/bills/" + createdBill.getBillId() + "/pdf?token=" + pdfToken);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // ✅ Get bill by ID
    @GetMapping("/{billId}")
    public ResponseEntity<Bill> getBillById(@PathVariable String billId) {
        Locale locale = LocaleContextHolder.getLocale();
        logger.debug("[BILL CONTROLLER] Fetching bill: {}", billId);
        Bill bill = billService.getBillById(billId, locale);
        return ResponseEntity.ok(bill);
    }

    // ✅ Get all bills
    @GetMapping
    public ResponseEntity<List<Bill>> getAllBills() {
        logger.debug("[BILL CONTROLLER] Fetching all bills");
        List<Bill> bills = billService.getAllBills();
        return ResponseEntity.ok(bills);
    }

    // ✅ Generate PDF access token for existing bill
    @PostMapping("/{billId}/generate-token")
    public ResponseEntity<Map<String, String>> generatePdfToken(@PathVariable String billId) {
        Locale locale = LocaleContextHolder.getLocale();
        logger.info("[BILL CONTROLLER] Generating PDF token for bill: {}", billId);

        // Verify bill exists
        billService.getBillById(billId, locale);

        String pdfToken = billService.generatePdfAccessToken(billId);

        Map<String, String> response = new HashMap<>();
        response.put("billId", billId);
        response.put("pdfAccessToken", pdfToken);
        response.put("pdfDownloadUrl", "/api/bills/" + billId + "/pdf?token=" + pdfToken);

        return ResponseEntity.ok(response);
    }

    // ✅ Generate Bill PDF (Optimized version)
    @GetMapping(value = "/{billId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadBillPdf(@PathVariable String billId,
            @RequestParam(required = false) String token) {
        Locale locale = LocaleContextHolder.getLocale();
        logger.info("[BILL CONTROLLER] Generating PDF for Bill ID: {}", billId);

        // Token validation – allow download only with a valid token
        if (token == null || !billService.validatePdfAccessToken(billId, token)) {
            logger.warn("[BILL CONTROLLER] Invalid or missing token for Bill {}", billId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(null);
        }

        try {
            Bill bill = billService.getBillById(billId, locale);
            byte[] pdfBytes = pdfService.generateBillPdf(bill, locale);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "Bill_" + billId + ".pdf");
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            headers.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Content-Disposition");

            logger.info("[BILL CONTROLLER] PDF generated successfully for Bill ID: {}", billId);
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("[BILL CONTROLLER] Error generating PDF for Bill {}: {}", billId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ✅ Resend Bill Email
    @PostMapping("/resend-email/{billId}")
    public ResponseEntity<?> resendBillEmail(@PathVariable String billId) {
        Locale locale = LocaleContextHolder.getLocale();
        try {
            logger.info("[BILL CONTROLLER] Resend request received for bill: {}", billId);
            Bill bill = billService.getBillById(billId, locale);

            if (bill == null) {
                logger.warn("[BILL CONTROLLER] Bill not found: {}", billId);
                return ResponseEntity.status(404)
                        .body(Map.of("error", "Bill not found: " + billId));
            }

            billService.resendBillEmail(bill, locale);
            logger.info("[BILL CONTROLLER] Bill email resent successfully for: {}", billId);

            // Generate new token for the resent email
            String newToken = billService.generatePdfAccessToken(billId);

            return ResponseEntity.ok(Map.of(
                    "message", "Bill email resent successfully",
                    "pdfAccessToken", newToken));

        } catch (RuntimeException e) {
            logger.error("[BILL CONTROLLER] Failed to resend bill email: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to resend bill email"));
        }
    }

    // ✅ Bulk upload (CSV)
    @PostMapping("/bulk")
    public ResponseEntity<Map<String, Object>> bulkUploadBills(
            @RequestPart("file") MultipartFile file) {
        Locale locale = LocaleContextHolder.getLocale();
        logger.info("[BILL CONTROLLER] Starting bulk upload");

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(messageSource.getMessage("file.missing", null, locale));
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            throw new IllegalArgumentException(messageSource.getMessage("file.invalid.format", null, locale));
        }

        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> successfulBills = new ArrayList<>();
        List<String> skippedBills = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();

        // DateFormat for 'createdAt' field
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            List<BillCsvBean> csvBeans = new CsvToBeanBuilder<BillCsvBean>(reader)
                    .withType(BillCsvBean.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .withIgnoreEmptyLine(true)
                    .build()
                    .parse();

            for (int i = 0; i < csvBeans.size(); i++) {
                BillCsvBean csvBean = csvBeans.get(i);
                int row = i + 2;

                try {
                    if (csvBean.getBillId() == null || csvBean.getBillId().trim().isEmpty()) {
                        errorMessages.add("Row " + row + ": Missing billId");
                        continue;
                    }

                    if (billService.existsByBillId(csvBean.getBillId())) {
                        skippedBills.add("Row " + row + ": Duplicate bill ID " + csvBean.getBillId());
                        continue;
                    }

                    // Parse JSON items
                    List<Bill.BillItem> items = objectMapper.readValue(
                            csvBean.getItems(),
                            new TypeReference<List<Bill.BillItem>>() {
                            });

                    Bill bill = new Bill();
                    bill.setBillId(csvBean.getBillId());
                    bill.setAddedBy(csvBean.getAddedBy());
                    Bill.CustomerInfo info = new Bill.CustomerInfo();
                    info.setMobile(csvBean.getCustomerMobile());
                    bill.setCustomer(info);
                    bill.setItems(items);

                    // Parse and set createdAt from CSV
                    try {
                        if (csvBean.getCreatedAt() != null && !csvBean.getCreatedAt().trim().isEmpty()) {
                            bill.setCreatedAt(df.parse(csvBean.getCreatedAt()));
                        } else {
                            bill.setCreatedAt(new Date()); // Fallback to now
                        }
                    } catch (Exception e) {
                        logger.warn("Row " + row + ": Invalid date format for " + csvBean.getCreatedAt()
                                + ". Defaulting to now.");
                        bill.setCreatedAt(new Date());
                    }

                    bill.setTotalAmount(bill.calculateTotal());

                    Bill saved = billService.createBill(bill, locale, true); // Use sync mode for bulk upload

                    // Generate token for each successful bill
                    String pdfToken = billService.generatePdfAccessToken(saved.getBillId());

                    Map<String, Object> billResponse = new HashMap<>();
                    billResponse.put("bill", saved);
                    billResponse.put("pdfAccessToken", pdfToken);
                    successfulBills.add(billResponse);

                } catch (Exception e) {
                    errorMessages.add("Row " + row + ": " + e.getMessage());
                }
            }

        } catch (Exception e) {
            errorMessages.add("File processing failed: " + e.getMessage());
        }

        result.put("successful", successfulBills.size());
        result.put("skipped", skippedBills.size());
        result.put("errors", errorMessages.size());
        result.put("successfulBills", successfulBills);
        result.put("skippedDetails", skippedBills);
        result.put("errorDetails", errorMessages);

        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }

    // --- RECOMMENDATION ENGINE ENDPOINTS ---

    @GetMapping("/suggestions")
    public ResponseEntity<List<com.smartretail.backend.models.Product>> getSuggestions(
            @RequestParam List<String> items) {
        if (items == null || items.isEmpty()) {
            return ResponseEntity.ok(new ArrayList<>());
        }
        return ResponseEntity.ok(billService.getUpsellSuggestions(items));
    }

    @PostMapping("/train-recommendations")
    public ResponseEntity<String> trainRecommendations() {
        billService.triggerRecommendationTraining();
        return ResponseEntity.ok("Training Initiated (Background Task)");
    }

    public static class BillCsvBean {
        private String billId;
        private String customerMobile;
        private String items;
        private String createdAt;
        private String addedBy;

        // Manual getters and setters
        public String getBillId() {
            return billId;
        }

        public void setBillId(String billId) {
            this.billId = billId;
        }

        public String getCustomerMobile() {
            return customerMobile;
        }

        public void setCustomerMobile(String customerMobile) {
            this.customerMobile = customerMobile;
        }

        public String getItems() {
            return items;
        }

        public void setItems(String items) {
            this.items = items;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }

        public String getAddedBy() {
            return addedBy;
        }

        public void setAddedBy(String addedBy) {
            this.addedBy = addedBy;
        }
    }
}