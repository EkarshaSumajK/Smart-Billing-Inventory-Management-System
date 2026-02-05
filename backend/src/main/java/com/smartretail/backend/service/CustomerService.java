package com.smartretail.backend.service;

import com.smartretail.backend.models.Bill;
import com.smartretail.backend.models.Customer;
import com.smartretail.backend.dto.CustomEmailRequest;

import java.util.Date;
import java.util.List;
import java.util.Locale;

public interface CustomerService {

    Customer findOrCreateCustomer(Bill.CustomerInfo customerInfo, Locale locale);

    void addBillId(String mobile, String billId);

    Customer createCustomer(Customer customer, Locale locale);

    Customer getCustomerById(String id, Locale locale);

    List<Customer> getAllCustomers();

    Customer updateCustomer(String id, Customer customer, Locale locale);

    void deleteCustomer(String id, Locale locale);

    List<Bill> getCustomerPurchaseHistory(String customerId, Date startDate, Date endDate, Locale locale);

    void sendCustomEmail(CustomEmailRequest request, Locale locale);

    // --- NEW METHOD: Find customers with no purchases after threshold date ---
    /**
     * Finds customers who have not made any purchase after the given threshold date.
     * Useful for re-engagement campaigns (e.g., "We miss you!" emails).
     *
     * @param thresholdDate Customers with last purchase before this date are considered inactive
     * @return List of inactive customers
     */
    List<Customer> findInactiveCustomers(Date thresholdDate);
}