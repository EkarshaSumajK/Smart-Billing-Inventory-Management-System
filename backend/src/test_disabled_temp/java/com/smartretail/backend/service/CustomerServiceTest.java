package com.smartretail.backend.service;

import com.smartretail.backend.models.Bill;
import com.smartretail.backend.models.Customer;
import com.smartretail.backend.repository.BillRepository;
import com.smartretail.backend.repository.CustomerRepository;
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
import java.util.Optional;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private BillRepository billRepository;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private CustomerServiceImpl customerService;

    private Customer customer;
    private Bill.CustomerInfo customerInfo;
    private Locale locale;
    private Bill bill1;
    private Bill bill2;
    private Bill bill3;

    @BeforeEach
    void setUp() {
        locale = Locale.forLanguageTag("en");

        customer = new Customer();
        customer.setId("68d7eb12a2d18777fbd48685");
        customer.setName("Sita");
        customer.setEmail("sita@gmail.com");
        customer.setMobile("5554447788");
        customer.setCreatedAt(new Date());
        customer.setPurchaseHistory(Arrays.asList("bill001", "bill002", "bill003"));

        customerInfo = new Bill.CustomerInfo();
        customerInfo.setName("Sita");
        customerInfo.setEmail("sita@gmail.com");
        customerInfo.setMobile("5554447788");

        bill1 = new Bill();
        bill1.setBillId("bill001");
        bill1.setTotalAmount(500.0);
        bill1.setCreatedAt(new Date(2025 - 1900, 8, 27, 10, 0, 0));
        Bill.CustomerInfo billCustomerInfo1 = new Bill.CustomerInfo();
        billCustomerInfo1.setMobile("5554447788");
        bill1.setCustomer(billCustomerInfo1);

        bill2 = new Bill();
        bill2.setBillId("bill002");
        bill2.setTotalAmount(300.0);
        bill2.setCreatedAt(new Date(2025 - 1900, 8, 27, 12, 0, 0));
        Bill.CustomerInfo billCustomerInfo2 = new Bill.CustomerInfo();
        billCustomerInfo2.setMobile("5554447788");
        bill2.setCustomer(billCustomerInfo2);

        bill3 = new Bill();
        bill3.setBillId("bill003");
        bill3.setTotalAmount(200.0);
        bill3.setCreatedAt(new Date(2025 - 1900, 8, 27, 15, 0, 0));
        Bill.CustomerInfo billCustomerInfo3 = new Bill.CustomerInfo();
        billCustomerInfo3.setMobile("5554447788");
        bill3.setCustomer(billCustomerInfo3);

        when(messageSource.getMessage(eq("customer.exists"), any(), eq(locale)))
                .thenReturn("Customer already exists: 5554447788");
        when(messageSource.getMessage(eq("customer.not.found"), any(), eq(locale)))
                .thenReturn("Customer not found: {0}");
        when(messageSource.getMessage(eq("customer.id.required"), any(), eq(locale)))
                .thenReturn("Customer ID is required");
        when(messageSource.getMessage(eq("report.date.range.invalid"), any(), eq(locale)))
                .thenReturn("Start date {0} must be before end date {1}");
    }

    @Test
    void testCreateCustomerSuccess() {
        when(customerRepository.findByMobile("5554447788")).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        Customer result = customerService.createCustomer(customer, locale);

        assertNotNull(result);
        assertEquals("Sita", result.getName());
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void testCreateCustomerDuplicateMobile() {
        when(customerRepository.findByMobile("5554447788")).thenReturn(Optional.of(customer));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> customerService.createCustomer(customer, locale));
        assertEquals("Customer already exists: 5554447788", exception.getMessage());
    }

    @Test
    void testFindOrCreateCustomerExisting() {
        when(customerRepository.findByMobile("5554447788")).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        Customer result = customerService.findOrCreateCustomer(customerInfo, locale);

        assertNotNull(result);
        assertEquals("Sita", result.getName());
        verify(customerRepository).save(customer);
    }

    @Test
    void testFindOrCreateCustomerNew() {
        when(customerRepository.findByMobile("5554447788")).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        Customer result = customerService.findOrCreateCustomer(customerInfo, locale);

        assertNotNull(result);
        assertEquals("Sita", result.getName());
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void testAddBillId() {
        when(customerRepository.findByMobile("5554447788")).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        customerService.addBillId("5554447788", "bill004");

        assertTrue(customer.getPurchaseHistory().contains("bill004"));
        verify(customerRepository).save(customer);
    }

    @Test
    void testAddBillIdCustomerNotFound() {
        when(customerRepository.findByMobile("5554447788")).thenReturn(Optional.empty());
        when(messageSource.getMessage(eq("customer.not.found"), any(), eq(Locale.ENGLISH)))
                .thenReturn("Customer not found: 5554447788");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> customerService.addBillId("5554447788", "bill004"));
        assertEquals("Customer not found: 5554447788", exception.getMessage());
    }

    @Test
    void testGetCustomerByIdSuccess() {
        when(customerRepository.findById("68d7eb12a2d18777fbd48685")).thenReturn(Optional.of(customer));

        Customer result = customerService.getCustomerById("68d7eb12a2d18777fbd48685", locale);

        assertNotNull(result);
        assertEquals("Sita", result.getName());
    }

    @Test
    void testGetCustomerByIdNotFound() {
        when(customerRepository.findById("68d7eb12a2d18777fbd48685")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> customerService.getCustomerById("68d7eb12a2d18777fbd48685", locale));
        assertEquals("Customer not found: 68d7eb12a2d18777fbd48685", exception.getMessage());
    }

    @Test
    void testGetAllCustomers() {
        when(customerRepository.findAll()).thenReturn(Arrays.asList(customer));

        List<Customer> customers = customerService.getAllCustomers();

        assertEquals(1, customers.size());
        assertEquals("Sita", customers.get(0).getName());
    }

    @Test
    void testUpdateCustomerSuccess() {
        Customer updatedData = new Customer();
        updatedData.setName("Sita Updated");
        updatedData.setEmail("sita.updated@gmail.com");
        updatedData.setMobile("5554447789");

        when(customerRepository.findById("68d7eb12a2d18777fbd48685")).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        Customer result = customerService.updateCustomer("68d7eb12a2d18777fbd48685", updatedData, locale);

        assertEquals("Sita Updated", customer.getName());
        assertEquals("sita.updated@gmail.com", customer.getEmail());
        assertEquals("5554447789", customer.getMobile());
        verify(customerRepository).save(customer);
    }

    @Test
    void testUpdateCustomerNotFound() {
        when(customerRepository.findById("68d7eb12a2d18777fbd48685")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> customerService.updateCustomer("68d7eb12a2d18777fbd48685", customer, locale));
        assertEquals("Customer not found: 68d7eb12a2d18777fbd48685", exception.getMessage());
    }

    @Test
    void testDeleteCustomerSuccess() {
        when(customerRepository.existsById("68d7eb12a2d18777fbd48685")).thenReturn(true);

        customerService.deleteCustomer("68d7eb12a2d18777fbd48685", locale);

        verify(customerRepository).deleteById("68d7eb12a2d18777fbd48685");
    }

    @Test
    void testDeleteCustomerNotFound() {
        when(customerRepository.existsById("68d7eb12a2d18777fbd48685")).thenReturn(false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> customerService.deleteCustomer("68d7eb12a2d18777fbd48685", locale));
        assertEquals("Customer not found: 68d7eb12a2d18777fbd48685", exception.getMessage());
    }

    @Test
    void testGetCustomerPurchaseHistory_Success() {
        when(customerRepository.findById("68d7eb12a2d18777fbd48685")).thenReturn(Optional.of(customer));
        when(billRepository.findAllById(Arrays.asList("bill001", "bill002", "bill003")))
                .thenReturn(Arrays.asList(bill1, bill2, bill3));

        List<Bill> purchaseHistory = customerService.getCustomerPurchaseHistory("68d7eb12a2d18777fbd48685", null, null, locale);

        assertEquals(3, purchaseHistory.size());
        assertEquals("bill001", purchaseHistory.get(0).getBillId());
        assertEquals("bill002", purchaseHistory.get(1).getBillId());
        assertEquals("bill003", purchaseHistory.get(2).getBillId());
        verify(billRepository).findAllById(Arrays.asList("bill001", "bill002", "bill003"));
    }

    @Test
    void testGetCustomerPurchaseHistory_BillNotFound() {
        customer.setPurchaseHistory(Arrays.asList("bill001", "bill002", "bill004"));
        when(customerRepository.findById("68d7eb12a2d18777fbd48685")).thenReturn(Optional.of(customer));
        when(billRepository.findAllById(Arrays.asList("bill001", "bill002", "bill004")))
                .thenReturn(Arrays.asList(bill1, bill2));

        List<Bill> purchaseHistory = customerService.getCustomerPurchaseHistory("68d7eb12a2d18777fbd48685", null, null, locale);

        assertEquals(2, purchaseHistory.size());
        assertEquals("bill001", purchaseHistory.get(0).getBillId());
        assertEquals("bill002", purchaseHistory.get(1).getBillId());
        verify(billRepository).findAllById(Arrays.asList("bill001", "bill002", "bill004"));
    }

    @Test
    void testGetCustomerPurchaseHistory_WithDateRange() {
        Date startDate = new Date(2025 - 1900, 8, 27, 0, 0, 0); // 2025-09-27 00:00:00
        Date endDate = new Date(2025 - 1900, 8, 27, 23, 59, 59); // 2025-09-27 23:59:59
        when(customerRepository.findById("68d7eb12a2d18777fbd48685")).thenReturn(Optional.of(customer));
        when(billRepository.findAllById(Arrays.asList("bill001", "bill002", "bill003")))
                .thenReturn(Arrays.asList(bill1, bill2, bill3));

        List<Bill> purchaseHistory = customerService.getCustomerPurchaseHistory("68d7eb12a2d18777fbd48685", startDate, endDate, locale);

        assertEquals(3, purchaseHistory.size());
        assertEquals("bill001", purchaseHistory.get(0).getBillId());
        assertEquals("bill002", purchaseHistory.get(1).getBillId());
        assertEquals("bill003", purchaseHistory.get(2).getBillId());
        verify(billRepository).findAllById(Arrays.asList("bill001", "bill002", "bill003"));
    }

    @Test
    void testGetCustomerPurchaseHistory_CustomerNotFound() {
        when(customerRepository.findById("68d7eb12a2d18777fbd48685")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> customerService.getCustomerPurchaseHistory("68d7eb12a2d18777fbd48685", null, null, locale));
        assertEquals("Customer not found: 68d7eb12a2d18777fbd48685", exception.getMessage());
        verify(billRepository, never()).findAllById(any());
    }

    @Test
    void testGetCustomerPurchaseHistory_EmptyPurchaseHistory() {
        customer.setPurchaseHistory(new ArrayList<>());
        when(customerRepository.findById("68d7eb12a2d18777fbd48685")).thenReturn(Optional.of(customer));

        List<Bill> purchaseHistory = customerService.getCustomerPurchaseHistory("68d7eb12a2d18777fbd48685", null, null, locale);

        assertTrue(purchaseHistory.isEmpty());
        verify(billRepository, never()).findAllById(any());
    }

    @Test
    void testGetCustomerPurchaseHistory_InvalidCustomerId() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> customerService.getCustomerPurchaseHistory("", null, null, locale));
        assertEquals("Customer ID is required", exception.getMessage());
        verify(customerRepository, never()).findById(any());
        verify(billRepository, never()).findAllById(any());
    }

    @Test
    void testGetCustomerPurchaseHistory_InvalidDateRange() {
        Date startDate = new Date();
        Date endDate = new Date(startDate.getTime() - 86400000); // Yesterday
        when(customerRepository.findById("68d7eb12a2d18777fbd48685")).thenReturn(Optional.of(customer));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> customerService.getCustomerPurchaseHistory("68d7eb12a2d18777fbd48685", startDate, endDate, locale));
        assertEquals("Start date " + startDate + " must be before end date " + endDate, exception.getMessage());
        verify(billRepository, never()).findAllById(any());
    }
}