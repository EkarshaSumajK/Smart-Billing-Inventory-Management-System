package com.smartretail.backend.service;

import com.smartretail.backend.dto.CustomEmailRequest;
import com.smartretail.backend.models.Bill;
import com.smartretail.backend.models.Customer;
import com.smartretail.backend.repository.BillRepository;
import com.smartretail.backend.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import com.smartretail.backend.security.SecurityUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CustomerServiceImpl implements CustomerService {

    private static final Logger logger = LoggerFactory.getLogger(CustomerServiceImpl.class);

    private final CustomerRepository customerRepository;
    private final BillRepository billRepository;
    private final MessageSource messageSource;
    private final NotificationService notificationService;
    private final SecurityUtils securityUtils;

    public CustomerServiceImpl(CustomerRepository customerRepository,
                               BillRepository billRepository,
                               MessageSource messageSource,
                               NotificationService notificationService,
                               SecurityUtils securityUtils) {
        this.customerRepository = customerRepository;
        this.billRepository = billRepository;
        this.messageSource = messageSource;
        this.notificationService = notificationService;
        this.securityUtils = securityUtils;
    }

    @Override
    public Customer findOrCreateCustomer(Bill.CustomerInfo customerInfo, Locale locale) {
        logger.debug("[CUSTOMER SERVICE] Finding or creating customer with mobile: {}", customerInfo.getMobile());
        if (customerInfo.getMobile() == null || customerInfo.getMobile().trim().isEmpty()) {
            logger.error("[CUSTOMER SERVICE] Customer mobile is required");
            throw new IllegalArgumentException(messageSource.getMessage("customer.mobile.required", null, locale));
        }
        String shopId = securityUtils.getCurrentShopId();
        Optional<Customer> existingCustomer = customerRepository.findByMobileAndShopId(customerInfo.getMobile(), shopId);
        if (existingCustomer.isPresent()) {
            Customer customer = existingCustomer.get();
            logger.debug("[CUSTOMER SERVICE] Found existing customer: {}", customer.getMobile());
            logger.debug("[CUSTOMER SERVICE] Existing customer email: {}", customer.getEmail());
            if (customerInfo.getName() != null && !customerInfo.getName().isEmpty()) {
                customer.setName(customerInfo.getName());
                logger.debug("[CUSTOMER SERVICE] Updated customer name to: {}", customerInfo.getName());
            }
            if (customerInfo.getEmail() != null && !customerInfo.getEmail().isEmpty()) {
                customer.setEmail(customerInfo.getEmail());
                logger.debug("[CUSTOMER SERVICE] Updated customer email to: {}", customerInfo.getEmail());
            }
            Customer savedCustomer = customerRepository.save(customer);
            logger.debug("[CUSTOMER SERVICE] Customer saved with email: {}", savedCustomer.getEmail());
            return savedCustomer;
        } else {
            Customer newCustomer = new Customer();
            newCustomer.setMobile(customerInfo.getMobile());
            newCustomer.setName(customerInfo.getName() != null ? customerInfo.getName() : "Customer");
            newCustomer.setEmail(customerInfo.getEmail());
            newCustomer.setCreatedAt(new Date());
            newCustomer.setShopId(shopId);
            newCustomer.setPurchaseHistory(new ArrayList<>());
            logger.debug("[CUSTOMER SERVICE] Creating new customer: {}", newCustomer.getMobile());
            logger.debug("[CUSTOMER SERVICE] New customer email: {}", newCustomer.getEmail());
            Customer savedCustomer = customerRepository.save(newCustomer);
            logger.debug("[CUSTOMER SERVICE] New customer created with email: {}", savedCustomer.getEmail());
            return savedCustomer;
        }
    }

    @Override
    public void addBillId(String mobile, String billId) {
        logger.debug("[CUSTOMER SERVICE] Adding bill ID {} to customer with mobile: {}", billId, mobile);
        String shopId = securityUtils.getCurrentShopId();
        Customer customer = customerRepository.findByMobileAndShopId(mobile, shopId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("customer.not.found", new Object[]{mobile}, Locale.getDefault())));

        if (customer.getPurchaseHistory() == null) {
            customer.setPurchaseHistory(new ArrayList<>());
        }
        if (!customer.getPurchaseHistory().contains(billId)) {
            customer.getPurchaseHistory().add(billId);
            customerRepository.save(customer);
            logger.info("[CUSTOMER SERVICE] Bill ID {} added to customer: {}", billId, mobile);
        }
    }

    @Override
    public Customer createCustomer(Customer customer, Locale locale) {
        logger.debug("[CUSTOMER SERVICE] Creating customer with mobile: {}", customer.getMobile());
        String shopId = securityUtils.getCurrentShopId();
        if (customerRepository.findByMobileAndShopId(customer.getMobile(), shopId).isPresent()) {
            logger.error("[CUSTOMER SERVICE] Customer already exists: {}", customer.getMobile());
            throw new IllegalArgumentException(
                    messageSource.getMessage("customer.exists", new Object[]{customer.getMobile()}, locale));
        }
        customer.setCreatedAt(new Date());
        customer.setShopId(shopId);
        Customer savedCustomer = customerRepository.save(customer);
        logger.info("[CUSTOMER SERVICE] Customer created: {}", savedCustomer.getMobile());
        return savedCustomer;
    }

    @Override
    public Customer getCustomerById(String id, Locale locale) {
        logger.debug("[CUSTOMER SERVICE] Fetching customer with ID: {}", id);
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("customer.not.found", new Object[]{id}, locale)));
        logger.info("[CUSTOMER SERVICE] Found customer: {}", customer.getMobile());
        return customer;
    }

    @Override
    public List<Customer> getAllCustomers() {
        logger.debug("[CUSTOMER SERVICE] Fetching all customers");
        String shopId = securityUtils.getCurrentShopId();
        List<Customer> customers = customerRepository.findByShopId(shopId);
        logger.info("[CUSTOMER SERVICE] Found {} customers", customers.size());
        return customers;
    }

    @Override
    public Customer updateCustomer(String id, Customer customer, Locale locale) {
        logger.debug("[CUSTOMER SERVICE] Updating customer with ID: {}", id);
        Customer existing = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("customer.not.found", new Object[]{id}, locale)));
        existing.setName(customer.getName());
        existing.setEmail(customer.getEmail());
        existing.setMobile(customer.getMobile());
        Customer updatedCustomer = customerRepository.save(existing);
        logger.info("[CUSTOMER SERVICE] Customer updated: {}", updatedCustomer.getMobile());
        return updatedCustomer;
    }

    @Override
    public void deleteCustomer(String id, Locale locale) {
        logger.debug("[CUSTOMER SERVICE] Deleting customer with ID: {}", id);
        if (!customerRepository.existsById(id)) {
            logger.error("[CUSTOMER SERVICE] Customer not found: {}", id);
            throw new IllegalArgumentException(
                    messageSource.getMessage("customer.not.found", new Object[]{id}, locale));
        }
        customerRepository.deleteById(id);
        logger.info("[CUSTOMER SERVICE] Customer deleted: {}", id);
    }

    private Date getEndOfDay(Date date) {
        if (date == null) {
            return null;
        }
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        return c.getTime();
    }

    @Override
    public List<Bill> getCustomerPurchaseHistory(String customerId, Date startDate, Date endDate, Locale locale) {
        logger.debug("[CUSTOMER SERVICE] Fetching purchase history for customer ID: {}, startDate: {}, endDate: {}",
                customerId, startDate, endDate);
        if (customerId == null || customerId.isEmpty()) {
            logger.error("[CUSTOMER SERVICE] Customer ID is required");
            throw new IllegalArgumentException(messageSource.getMessage("customer.id.required", null, locale));
        }
        if (startDate != null && endDate != null && startDate.after(endDate)) {
            logger.error("[CUSTOMER SERVICE] Invalid date range: startDate={} is after endDate={}", startDate, endDate);
            throw new IllegalArgumentException(
                    messageSource.getMessage("report.date.range.invalid", new Object[]{startDate, endDate}, locale));
        }
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("customer.not.found", new Object[]{customerId}, locale)));
        List<String> billIds = customer.getPurchaseHistory();
        if (billIds == null || billIds.isEmpty()) {
            logger.info("[CUSTOMER SERVICE] No purchase history found for customer: {}", customerId);
            return new ArrayList<>();
        }

        final Date finalEndDate = getEndOfDay(endDate);
        List<Bill> bills = billRepository.findAllById(billIds).stream()
                .filter(bill -> bill.getCreatedAt() != null)
                .filter(bill -> startDate == null || !bill.getCreatedAt().before(startDate))
                .filter(bill -> finalEndDate == null || !bill.getCreatedAt().after(finalEndDate))
                .collect(Collectors.toList());

        if (bills.size() < billIds.size()) {
            List<String> foundBillIds = bills.stream().map(Bill::getBillId).collect(Collectors.toList());
            List<String> missingBillIds = new ArrayList<>(billIds);
            missingBillIds.removeAll(foundBillIds);
            logger.warn("[CUSTOMER SERVICE] Some bill IDs not found: missing IDs {}, expected {}, found {}",
                    missingBillIds, billIds.size(), bills.size());
        }
        logger.info("[CUSTOMER SERVICE] Found {} bills in purchase history for customer: {}", bills.size(), customerId);
        return bills;
    }

    @Override
    public void sendCustomEmail(CustomEmailRequest request, Locale locale) {
        logger.info("[CUSTOMER SERVICE] Received request to send custom email to {} customers", request.getCustomerIds().size());

        List<Customer> customers = customerRepository.findAllById(request.getCustomerIds());
        int successCount = 0;

        for (Customer customer : customers) {
            if (customer.getEmail() == null || customer.getEmail().isBlank()) {
                logger.warn("[CUSTOMER SERVICE] Skipping customer {}: no email address", customer.getId());
                continue;
            }

            try {
                String personalizedBody = request.getBody()
                        .replace("{{customerName}}", Optional.ofNullable(customer.getName()).orElse("Customer"))
                        .replace("{{customerNameCase}}", Optional.ofNullable(customer.getName()).orElse("Customer"));

                notificationService.sendEmail(
                        customer.getEmail(),
                        request.getSubject(),
                        personalizedBody
                );
                successCount++;
            } catch (Exception e) {
                logger.error("[CUSTOMER SERVICE] Failed to send email to {}: {}", customer.getEmail(), e.getMessage());
            }
        }

        logger.info("[CUSTOMER SERVICE] Successfully sent {} out of {} emails", successCount, customers.size());
    }

    // --- NEW METHOD: Find Inactive Customers ---
    @Override
    public List<Customer> findInactiveCustomers(Date thresholdDate) {
        logger.debug("[CUSTOMER SERVICE] Finding customers inactive since: {}", thresholdDate);

        if (thresholdDate == null) {
            logger.warn("[CUSTOMER SERVICE] Threshold date is null, returning empty list");
            return Collections.emptyList();
        }

        String shopId = securityUtils.getCurrentShopId();
        return customerRepository.findByShopIdAndLastPurchaseDateBeforeOrLastPurchaseDateIsNull(shopId, thresholdDate);
    }
}