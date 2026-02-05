package com.smartretail.backend.controller;

import com.smartretail.backend.models.Bill;
import com.smartretail.backend.models.Product;
import com.smartretail.backend.service.BillService;
import com.smartretail.backend.service.ProductService;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/sync")
public class SyncController {
    private static final Logger logger = LoggerFactory.getLogger(SyncController.class);
    private final BillService billService;
    private final ProductService productService;

    public SyncController(BillService billService, ProductService productService) {
        this.billService = billService;
        this.productService = productService;
    }

    @PostMapping("/pending")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'CASHIER')")
    public ResponseEntity<Map<String, Object>> syncPendingData(
            @RequestBody SyncRequest syncRequest,
            @RequestHeader(value = "Accept-Language", defaultValue = "en") Locale locale) {
        logger.debug("[SYNC CONTROLLER] Processing sync request with {} bills and {} inventory updates",
                syncRequest.getBills().size(), syncRequest.getInventoryUpdates().size());

        List<String> processedBillIds = new ArrayList<>();
        List<String> processedProductIds = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        // Process bills
        for (Bill bill : syncRequest.getBills()) {
            try {
                Bill savedBill = billService.createBill(bill, locale, true); // true = sync mode
                processedBillIds.add(savedBill.getBillId());
                logger.info("[SYNC CONTROLLER] Successfully synced bill: {}", savedBill.getBillId());
            } catch (Exception e) {
                logger.error("[SYNC CONTROLLER] Failed to sync bill {}: {}", bill.getBillId(), e.getMessage());
                errors.add("Bill " + bill.getBillId() + ": " + e.getMessage());
            }
        }

        // Process inventory updates
        for (Product product : syncRequest.getInventoryUpdates()) {
            try {
                // Use productId instead of id
                productService.updateProduct(product.getProductId(), product, locale, true); // true = sync mode
                processedProductIds.add(product.getProductId());
                logger.info("[SYNC CONTROLLER] Successfully synced product: {}", product.getProductId());
            } catch (Exception e) {
                logger.error("[SYNC CONTROLLER] Failed to sync product {}: {}", product.getProductId(), e.getMessage());
                errors.add("Product " + product.getProductId() + ": " + e.getMessage());
            }
        }

        Map<String, Object> response = Map.of(
                "processedBills", processedBillIds,
                "processedProducts", processedProductIds,
                "errors", errors,
                "message", errors.isEmpty() ? "Sync completed successfully" : "Sync completed with errors"
        );

        return ResponseEntity.ok(response);
    }

    // DTO for sync request
    @Setter
    public static class SyncRequest {
        private List<Bill> bills;
        private List<Product> inventoryUpdates;

        public List<Bill> getBills() {
            return bills != null ? bills : new ArrayList<>();
        }

        public List<Product> getInventoryUpdates() {
            return inventoryUpdates != null ? inventoryUpdates : new ArrayList<>();
        }
    }
}