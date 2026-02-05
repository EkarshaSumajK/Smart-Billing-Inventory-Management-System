package com.smartretail.backend.service;

import com.smartretail.backend.models.Bill;
import com.smartretail.backend.models.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReminderServiceTest {

    @Mock
    private CustomerService customerService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private ReminderService reminderService;

    private Customer customer;
    private Bill bill1;
    private Bill bill2;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setId("68d7eb12a2d18777fbd48685");
        customer.setName("Sita");
        customer.setEmail("sita@gmail.com");
        customer.setMobile("5554447788");
        customer.setPurchaseHistory(Arrays.asList("bill001", "bill002"));

        bill1 = new Bill();
        bill1.setBillId("bill001");
        bill1.setCreatedAt(new Date());
        Bill.CustomerInfo billCustomerInfo1 = new Bill.CustomerInfo();
        billCustomerInfo1.setMobile("5554447788");
        bill1.setCustomer(billCustomerInfo1);
        Bill.BillItem item1 = new Bill.BillItem();
        item1.setProductName("Laptop");
        item1.setProductId("p123");
        item1.setQty(1);
        item1.setPrice(500.0);
        bill1.setItems(Arrays.asList(item1));

        bill2 = new Bill();
        bill2.setBillId("bill002");
        bill2.setCreatedAt(new Date());
        Bill.CustomerInfo billCustomerInfo2 = new Bill.CustomerInfo();
        billCustomerInfo2.setMobile("5554447788");
        bill2.setCustomer(billCustomerInfo2);
        Bill.BillItem item2 = new Bill.BillItem();
        item2.setProductName("Laptop");
        item2.setProductId("p123");
        item2.setQty(1);
        item2.setPrice(500.0);
        bill2.setItems(Arrays.asList(item2));

        when(customerService.getAllCustomers()).thenReturn(Arrays.asList(customer));
        when(customerService.getCustomerPurchaseHistory(eq("68d7eb12a2d18777fbd48685"), any(), any(), eq(Locale.ENGLISH)))
                .thenReturn(Arrays.asList(bill1, bill2));
        when(messageSource.getMessage(eq("reminder.no.email"), any(), eq(Locale.ENGLISH)))
                .thenReturn("Customer 5554447788 has no email address");
        when(messageSource.getMessage(eq("reminder.no.purchase.history"), any(), eq(Locale.ENGLISH)))
                .thenReturn("Customer 5554447788 has no purchase history");
        when(messageSource.getMessage(eq("reminder.no.frequent.items"), any(), eq(Locale.ENGLISH)))
                .thenReturn("Customer 5554447788 has no frequently purchased items");
        when(messageSource.getMessage(eq("reminder.subject"), any(), eq(Locale.ENGLISH)))
                .thenReturn("Monthly Purchase Reminder from Smart Retail");
        when(messageSource.getMessage(eq("reminder.body"), any(), eq(Locale.ENGLISH)))
                .thenReturn("Dear Sita,\n\nWe noticed you frequently purchase the following items:\nLaptop\nVisit us to restock your favorites!\n\nBest regards,\nSmart Retail Team");
    }

    @Test
    void testSendMonthlyPurchaseReminders_Success() {
        reminderService.sendMonthlyPurchaseReminders();

        verify(customerService).getAllCustomers();
        verify(customerService).getCustomerPurchaseHistory("68d7eb12a2d18777fbd48685", null, null, Locale.ENGLISH);
        verify(notificationService).sendEmail(
                eq("sita@gmail.com"),
                eq("Monthly Purchase Reminder from Smart Retail"),
                contains("Dear Sita,\n\nWe noticed you frequently purchase the following items:\nLaptop\n")
        );
    }

    @Test
    void testSendMonthlyPurchaseReminders_NoEmail() {
        customer.setEmail(null);
        when(customerService.getAllCustomers()).thenReturn(Arrays.asList(customer));

        reminderService.sendMonthlyPurchaseReminders();

        verify(customerService).getAllCustomers();
        verify(customerService, never()).getCustomerPurchaseHistory(anyString(), any(), any(), any());
        verify(notificationService, never()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void testSendMonthlyPurchaseReminders_NoPurchaseHistory() {
        when(customerService.getCustomerPurchaseHistory(eq("68d7eb12a2d18777fbd48685"), any(), any(), eq(Locale.ENGLISH)))
                .thenReturn(List.of());

        reminderService.sendMonthlyPurchaseReminders();

        verify(customerService).getAllCustomers();
        verify(customerService).getCustomerPurchaseHistory("68d7eb12a2d18777fbd48685", null, null, Locale.ENGLISH);
        verify(notificationService, never()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void testSendMonthlyPurchaseReminders_NoFrequentItems() {
        bill2.getItems().get(0).setProductName("Mouse"); // Different product
        when(customerService.getCustomerPurchaseHistory(eq("68d7eb12a2d18777fbd48685"), any(), any(), eq(Locale.ENGLISH)))
                .thenReturn(Arrays.asList(bill1, bill2));

        reminderService.sendMonthlyPurchaseReminders();

        verify(customerService).getAllCustomers();
        verify(customerService).getCustomerPurchaseHistory("68d7eb12a2d18777fbd48685", null, null, Locale.ENGLISH);
        verify(notificationService, never()).sendEmail(anyString(), anyString(), anyString());
    }
}