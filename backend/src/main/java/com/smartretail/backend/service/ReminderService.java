package com.smartretail.backend.service;

import com.smartretail.backend.models.Bill;
import com.smartretail.backend.models.Customer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReminderService {

    private static final Logger logger = LoggerFactory.getLogger(ReminderService.class);

    private final CustomerService customerService;
    private final NotificationService notificationService;
    private final MessageSource messageSource;

    public ReminderService(CustomerService customerService,
                           NotificationService notificationService,
                           MessageSource messageSource) {
        this.customerService = customerService;
        this.notificationService = notificationService;
        this.messageSource = messageSource;
    }

    /**
     * Sends monthly purchase reminders to customers with frequently bought items.
     * Runs on the 1st of every month at 9:00 AM.
     */
    @Scheduled(cron = "0 0 9 1 * ?")
    public void sendMonthlyPurchaseReminders() {
        logger.info("[REMINDER SERVICE] Starting monthly purchase reminders");

        List<Customer> customers = customerService.getAllCustomers();
        int processedCount = 0;

        for (Customer customer : customers) {
            if (customer.getEmail() == null || customer.getEmail().isBlank()) {
                logger.debug("[REMINDER SERVICE] Skipping {}: no email", customer.getMobile());
                continue;
            }

            List<Bill> purchaseHistory = customerService.getCustomerPurchaseHistory(
                    customer.getId(), null, null, Locale.ENGLISH);

            if (purchaseHistory.isEmpty()) {
                logger.debug("[REMINDER SERVICE] Skipping {}: no purchase history", customer.getMobile());
                continue;
            }

            // Count product frequency
            Map<String, Long> productFrequency = purchaseHistory.stream()
                    .flatMap(bill -> bill.getItems().stream())
                    .collect(Collectors.groupingBy(
                            Bill.BillItem::getProductName,
                            Collectors.counting()
                    ));

            List<String> frequentItems = productFrequency.entrySet().stream()
                    .filter(entry -> entry.getValue() >= 2)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            if (frequentItems.isEmpty()) {
                logger.debug("[REMINDER SERVICE] Skipping {}: no frequent items", customer.getMobile());
                continue;
            }

            try {
                String subject = messageSource.getMessage("reminder.subject", null, Locale.ENGLISH);
                String body = messageSource.getMessage(
                        "reminder.body",
                        new Object[]{customer.getName(), String.join("\n", frequentItems)},
                        Locale.ENGLISH
                );

                notificationService.sendEmail(customer.getEmail(), subject, body);
                logger.info("[REMINDER SERVICE] Sent reminder to {} for items: {}", customer.getMobile(), frequentItems);
                processedCount++;
            } catch (Exception e) {
                logger.error("[REMINDER SERVICE] Failed to send reminder to {}: {}", customer.getEmail(), e.getMessage());
            }
        }

        logger.info("[REMINDER SERVICE] Monthly reminders complete. Processed {} customers.", processedCount);
    }

    /**
     * Sends re-engagement emails to customers inactive for 30+ days.
     * Runs daily at 10:00 AM.
     */
    @Scheduled(cron = "0 0 10 * * ?")
    public void sendReEngagementReminders() {
        logger.info("[REMINDER SERVICE] Starting daily re-engagement email task...");

        // Calculate threshold: 30 days ago
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -30);
        Date thirtyDaysAgo = cal.getTime();

        // Find inactive customers
        List<Customer> inactiveCustomers = customerService.findInactiveCustomers(thirtyDaysAgo);

        if (inactiveCustomers.isEmpty()) {
            logger.info("[REMINDER SERVICE] No inactive customers found. Task complete.");
            return;
        }

        logger.info("[REMINDER SERVICE] Found {} inactive customers to re-engage.", inactiveCustomers.size());

        String subject = messageSource.getMessage("re_engagement.subject", null, Locale.ENGLISH);
        int successCount = 0;

        for (Customer customer : inactiveCustomers) {
            if (customer.getEmail() == null || customer.getEmail().isBlank()) {
                logger.debug("[REMINDER SERVICE] Skipping inactive customer {}: no email", customer.getId());
                continue;
            }

            try {
                String body = messageSource.getMessage(
                        "re_engagement.body",
                        new Object[]{Optional.ofNullable(customer.getName()).orElse("Valued Customer")},
                        Locale.ENGLISH
                );

                notificationService.sendEmail(customer.getEmail(), subject, body);
                successCount++;
                logger.debug("[REMINDER SERVICE] Re-engagement email sent to: {}", customer.getEmail());
            } catch (Exception e) {
                logger.error("[REMINDER SERVICE] Failed to send re-engagement email to {}: {}", customer.getEmail(), e.getMessage());
            }
        }

        logger.info("[REMINDER SERVICE] Re-engagement task complete. Successfully sent {} emails.", successCount);
    }
}