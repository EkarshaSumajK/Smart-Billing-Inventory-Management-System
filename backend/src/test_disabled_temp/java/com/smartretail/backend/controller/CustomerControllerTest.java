package com.smartretail.backend.controller;

import com.smartretail.backend.models.Bill;
import com.smartretail.backend.models.Customer;
import com.smartretail.backend.service.CustomerService;
import com.smartretail.backend.service.ReminderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale; // --- FIX: Import Locale ---
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {

    @Mock
    private CustomerService customerService;

    @Mock
    private ReminderService reminderService;

    @InjectMocks
    private CustomerController customerController;

    @Test
    void testCreateCustomer_Success() {
        Customer customer = new Customer();
        customer.setId("68d7eb12a2d18777fbd48685");
        customer.setMobile("5554447788");
        customer.setName("Sita");

        // --- FIX: Mock expects a Locale object, not a String "en" ---
        when(customerService.createCustomer(any(Customer.class), eq(new Locale("en")))).thenReturn(customer);
        // --- End Fix ---

        ResponseEntity<Customer> response = customerController.createCustomer(customer, "en");

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("68d7eb12a2d18777fbd48685", response.getBody().getId());
        
        // --- FIX: Verify with Locale object ---
        verify(customerService).createCustomer(customer, eq(new Locale("en")));
        // --- End Fix ---
    }

    @Test
    void testGetCustomerById_Success() {
        Customer customer = new Customer();
        customer.setId("68d7eb12a2d18777fbd48685");
        customer.setMobile("5554447788");

        // --- FIX: Mock expects a Locale object ---
        when(customerService.getCustomerById("68d7eb12a2d18777fbd48685", eq(new Locale("en")))).thenReturn(customer);
        // --- End Fix ---

        ResponseEntity<Customer> response = customerController.getCustomerById("68d7eb12a2d18777fbd48685", "en");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("68d7eb12a2d18777fbd48685", response.getBody().getId());
        
        // --- FIX: Verify with Locale object ---
        verify(customerService).getCustomerById("68d7eb12a2d18777fbd48685", eq(new Locale("en")));
        // --- End Fix ---
    }

    @Test
    void testGetAllCustomers_Success() {
        Customer customer = new Customer();
        customer.setId("68d7eb12a2d18777fbd48685");
        when(customerService.getAllCustomers()).thenReturn(Arrays.asList(customer));

        ResponseEntity<List<Customer>> response = customerController.getAllCustomers("en");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        verify(customerService).getAllCustomers();
    }

    @Test
    void testUpdateCustomer_Success() {
        Customer customer = new Customer();
        customer.setId("68d7eb12a2d18777fbd48685");
        customer.setMobile("5554447788");

        // --- FIX: Mock expects a Locale object ---
        when(customerService.updateCustomer(eq("68d7eb12a2d18777fbd48685"), any(Customer.class), eq(new Locale("en"))))
                .thenReturn(customer);
        // --- End Fix ---

        ResponseEntity<Customer> response = customerController.updateCustomer("68d7eb12a2d18777fbd48685", customer, "en");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("68d7eb12a2d18777fbd48685", response.getBody().getId());
        
        // --- FIX: Verify with Locale object ---
        verify(customerService).updateCustomer("68d7eb12a2d18777fbd48685", customer, eq(new Locale("en")));
        // --- End Fix ---
    }

    @Test
    void testDeleteCustomer_Success() {
        // --- FIX: Mock expects a Locale object ---
        doNothing().when(customerService).deleteCustomer("68d7eb12a2d18777fbd48685", eq(new Locale("en")));
        // --- End Fix ---

        ResponseEntity<Void> response = customerController.deleteCustomer("68d7eb12a2d18777fbd48685", "en");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        
        // --- FIX: Verify with Locale object ---
        verify(customerService).deleteCustomer("68d7eb12a2d18777fbd48685", eq(new Locale("en")));
        // --- End Fix ---
    }

    @Test
    void testGetCustomerPurchaseHistory_Success() {
        Bill bill1 = new Bill();
        bill1.setBillId("bill001");
        bill1.setTotalAmount(500.0);
        bill1.setCreatedAt(new Date(2025 - 1900, 8, 27, 10, 0, 0));
        Bill.CustomerInfo ci1 = new Bill.CustomerInfo();
        ci1.setMobile("5554447788");
        bill1.setCustomer(ci1);

        Bill bill2 = new Bill();
        bill2.setBillId("bill002");
        bill2.setTotalAmount(300.0);
        bill2.setCreatedAt(new Date(2025 - 1900, 8, 27, 12, 0, 0));
        Bill.CustomerInfo ci2 = new Bill.CustomerInfo();
        ci2.setMobile("5554447788");
        bill2.setCustomer(ci2);

        Bill bill3 = new Bill();
        bill3.setBillId("bill003");
        bill3.setTotalAmount(200.0);
        bill3.setCreatedAt(new Date(2025 - 1900, 8, 27, 15, 0, 0));
        Bill.CustomerInfo ci3 = new Bill.CustomerInfo();
        ci3.setMobile("5554447788");
        bill3.setCustomer(ci3);

        // --- FIX: Mock expects a Locale object ---
        when(customerService.getCustomerPurchaseHistory("68d7eb12a2d18777fbd48685", null, null, eq(new Locale("en"))))
                .thenReturn(Arrays.asList(bill1, bill2, bill3));
        // --- End Fix ---

        ResponseEntity<List<Bill>> response = customerController
                .getCustomerPurchaseHistory("68d7eb12a2d18777fbd48685", null, null, "en");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(3, response.getBody().size());
        assertEquals("bill001", response.getBody().get(0).getBillId());
        
        // --- FIX: Verify with Locale object ---
        verify(customerService).getCustomerPurchaseHistory("68d7eb12a2d18777fbd48685", null, null, eq(new Locale("en")));
        // --- End Fix ---
    }

    @Test
    void testTriggerMonthlyPurchaseReminders_Success() {
        doNothing().when(reminderService).sendMonthlyPurchaseReminders();

        ResponseEntity<Map<String, String>> response = customerController.triggerMonthlyPurchaseReminders("en");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Monthly purchase reminders triggered successfully", response.getBody().get("message"));
        verify(reminderService).sendMonthlyPurchaseReminders();
    }
}