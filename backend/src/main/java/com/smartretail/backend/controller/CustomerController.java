package com.smartretail.backend.controller;

import com.smartretail.backend.dto.CustomEmailRequest; // <-- ADDED IMPORT
import com.smartretail.backend.models.Bill;
import com.smartretail.backend.models.Customer;
import com.smartretail.backend.service.CustomerService;
import com.smartretail.backend.service.ReminderService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerController.class);

    private final CustomerService customerService;
    private final ReminderService reminderService;

    public CustomerController(CustomerService customerService, ReminderService reminderService) {
        this.customerService = customerService;
        this.reminderService = reminderService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'CASHIER')")
    public ResponseEntity<Customer> createCustomer(
            @Valid @RequestBody Customer customer,
            @RequestHeader(value = "Accept-Language", defaultValue = "en", required = false) String language) {
        Locale locale = parseLocale(language);
        logger.debug("Creating customer with mobile: {}", customer.getMobile());
        Customer savedCustomer = customerService.createCustomer(customer, locale);
        return ResponseEntity.status(201).body(savedCustomer);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'CASHIER')")
    public ResponseEntity<Customer> getCustomerById(
            @PathVariable String id,
            @RequestHeader(value = "Accept-Language", defaultValue = "en", required = false) String language) {
        Locale locale = parseLocale(language);
        logger.debug("Fetching customer with ID: {}", id);
        Customer customer = customerService.getCustomerById(id, locale);
        return ResponseEntity.ok(customer);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'CASHIER')")
    public ResponseEntity<List<Customer>> getAllCustomers(
            @RequestHeader(value = "Accept-Language", defaultValue = "en", required = false) String language) {
        Locale locale = parseLocale(language);
        logger.debug("Fetching all customers");
        List<Customer> customers = customerService.getAllCustomers();
        return ResponseEntity.ok(customers);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public ResponseEntity<Customer> updateCustomer(
            @PathVariable String id,
            @Valid @RequestBody Customer customer,
            @RequestHeader(value = "Accept-Language", defaultValue = "en", required = false) String language) {
        Locale locale = parseLocale(language);
        logger.debug("Updating customer with ID: {}", id);
        Customer updatedCustomer = customerService.updateCustomer(id, customer, locale);
        return ResponseEntity.ok(updatedCustomer);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public ResponseEntity<Void> deleteCustomer(
            @PathVariable String id,
            @RequestHeader(value = "Accept-Language", defaultValue = "en", required = false) String language) {
        Locale locale = parseLocale(language);
        logger.debug("Deleting customer with ID: {}", id);
        customerService.deleteCustomer(id, locale);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/purchase-history")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'CASHIER')")
    public ResponseEntity<List<Bill>> getCustomerPurchaseHistory(
            @PathVariable String id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date endDate,
            @RequestHeader(value = "Accept-Language", defaultValue = "en", required = false) String language) {
        Locale locale = parseLocale(language);
        logger.debug("Fetching purchase history for customer ID: {}, startDate: {}, endDate: {}", id, startDate, endDate);
        List<Bill> purchaseHistory = customerService.getCustomerPurchaseHistory(id, startDate, endDate, locale);
        return ResponseEntity.ok(purchaseHistory);
    }

    // --- NEW ENDPOINT: Send Custom Email ---
    @PostMapping("/send-email")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public ResponseEntity<Map<String, String>> sendCustomEmail(
            @Valid @RequestBody CustomEmailRequest request,
            @RequestHeader(value = "Accept-Language", defaultValue = "en", required = false) String language) {

        Locale locale = parseLocale(language);
        logger.info("[CUSTOMER CONTROLLER] Received request to send custom email to {} customers", request.getCustomerIds().size());

        // Fire-and-forget async execution (non-blocking)
        new Thread(() -> {
            try {
                customerService.sendCustomEmail(request, locale);
            } catch (Exception e) {
                logger.error("[CUSTOMER CONTROLLER] Error in async email sending", e);
            }
        }).start();

        return ResponseEntity.ok(
                Map.of("message", "Email sending process initiated for " + request.getCustomerIds().size() + " customers.")
        );
    }

    @PostMapping("/reminders")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public ResponseEntity<Map<String, String>> triggerMonthlyPurchaseReminders(
            @RequestHeader(value = "Accept-Language", defaultValue = "en", required = false) String language) {
        Locale locale = parseLocale(language);
        logger.debug("Triggering monthly purchase reminders manually");
        reminderService.sendMonthlyPurchaseReminders();
        return ResponseEntity.ok(Map.of("message", "Monthly purchase reminders triggered successfully"));
    }

    private Locale parseLocale(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isEmpty()) {
            return Locale.ENGLISH;
        }
        String primaryLang = acceptLanguage.split(",")[0].split(";")[0].trim();
        String[] parts = primaryLang.split("-");
        if (parts.length == 2) {
            return new Locale(parts[0], parts[1]);
        }
        return new Locale(parts[0]);
    }
}