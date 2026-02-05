package com.smartretail.backend.service;

import com.smartretail.backend.models.Bill;
import com.smartretail.backend.models.Customer;
import com.smartretail.backend.models.Product;
import com.smartretail.backend.repository.BillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import java.util.Arrays;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillServiceTest {

    @Mock
    private BillRepository billRepository;
    @Mock
    private CustomerService customerService;
    @Mock
    private ProductService productService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private BillServiceImpl billService;

    private Bill bill;
    private Bill.BillItem item1;
    private Bill.BillItem item2;
    private Product product1;
    private Product product2;
    private Customer customer;

    @BeforeEach
    void setUp() {
        // Setup test data
        bill = new Bill();
        bill.setBillId("b12345678");
        bill.setAddedBy("manager@shop.com");

        // Create customer info
        Bill.CustomerInfo customerInfo = new Bill.CustomerInfo();
        customerInfo.setMobile("1234567890");
        bill.setCustomer(customerInfo);

        // Create bill items
        item1 = new Bill.BillItem();
        item1.setProductId("p12345678");
        item1.setQty(2);

        item2 = new Bill.BillItem();
        item2.setProductId("p98765432");
        item2.setQty(1);

        bill.setItems(Arrays.asList(item1, item2));

        // Create products
        product1 = new Product();
        product1.setProductId("p12345678");
        product1.setPrice(999.99);
        product1.setQuantity(10);

        product2 = new Product();
        product2.setProductId("p98765432");
        product2.setPrice(49.99);
        product2.setQuantity(5);

        // Create customer
        customer = new Customer();
        customer.setMobile("1234567890");
        customer.setName("Test Customer");
        customer.setEmail("test@customer.com");
    }

    @Test
    void testCreateBill_DuplicateBillId_ThrowsException() {
        // Given
        when(billRepository.existsByBillId("b12345678")).thenReturn(true);
        when(messageSource.getMessage("bill.exists", new Object[]{"b12345678"}, Locale.ENGLISH))
                .thenReturn("Bill already exists: b12345678");

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            billService.createBill(bill, Locale.ENGLISH);
        });

        assertEquals("Bill already exists: b12345678", exception.getMessage());
        verify(billRepository, times(1)).existsByBillId("b12345678");
        verify(billRepository, never()).save(any(Bill.class));
    }

    @Test
    void testCreateBill_MissingAddedBy_ThrowsException() {
        // Given
        bill.setAddedBy(null);
        when(billRepository.existsByBillId("b12345678")).thenReturn(false);
        when(messageSource.getMessage("bill.addedBy.missing.service", new Object[]{"b12345678"}, Locale.ENGLISH))
                .thenReturn("Added by field is required for bill: b12345678");

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            billService.createBill(bill, Locale.ENGLISH);
        });

        assertEquals("Added by field is required for bill: b12345678", exception.getMessage());
        verify(billRepository, times(1)).existsByBillId("b12345678");
        verify(billRepository, never()).save(any(Bill.class));
    }

    @Test
    void testCreateBill_InvalidQuantity_ThrowsException() {
        // Given
        item1.setQty(0); // Invalid quantity
        when(billRepository.existsByBillId("b12345678")).thenReturn(false);
        when(productService.getProductById("p12345678", Locale.ENGLISH)).thenReturn(product1);
        when(messageSource.getMessage("bill.items.invalid.quantity", new Object[]{"b12345678"}, Locale.ENGLISH))
                .thenReturn("Invalid quantity for items in bill: b12345678");

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            billService.createBill(bill, Locale.ENGLISH);
        });

        assertEquals("Invalid quantity for items in bill: b12345678", exception.getMessage());
        verify(productService, times(1)).getProductById("p12345678", Locale.ENGLISH);
        verify(billRepository, never()).save(any(Bill.class));
    }

    @Test
    void testCreateBill_InvalidProductPrice_ThrowsException() {
        // Given
        product1.setPrice(0); // Invalid price
        when(billRepository.existsByBillId("b12345678")).thenReturn(false);
        when(productService.getProductById("p12345678", Locale.ENGLISH)).thenReturn(product1);
        when(messageSource.getMessage("bill.items.invalid.price", new Object[]{"b12345678"}, Locale.ENGLISH))
                .thenReturn("Invalid price for items in bill: b12345678");

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            billService.createBill(bill, Locale.ENGLISH);
        });

        assertEquals("Invalid price for items in bill: b12345678", exception.getMessage());
        verify(productService, times(1)).getProductById("p12345678", Locale.ENGLISH);
        verify(billRepository, never()).save(any(Bill.class));
    }

    @Test
    void testCreateBill_Success() {
        // Given
        when(billRepository.existsByBillId("b12345678")).thenReturn(false);
        when(productService.getProductById("p12345678", Locale.ENGLISH)).thenReturn(product1);
        when(productService.getProductById("p98765432", Locale.ENGLISH)).thenReturn(product2);
        when(customerService.findOrCreateCustomer(any(Bill.CustomerInfo.class), eq(Locale.ENGLISH))).thenReturn(customer);
        when(billRepository.save(any(Bill.class))).thenReturn(bill);

        // When
        Bill result = billService.createBill(bill, Locale.ENGLISH);

        // Then
        assertNotNull(result);
        assertEquals("b12345678", result.getBillId());

        // Verify total amount calculation: (999.99 * 2) + (49.99 * 1) = 1999.98 + 49.99 = 2049.97
        assertEquals(2049.97, result.getTotalAmount(), 0.01);

        verify(billRepository, times(1)).existsByBillId("b12345678");
        verify(productService, times(1)).getProductById("p12345678", Locale.ENGLISH);
        verify(productService, times(1)).getProductById("p98765432", Locale.ENGLISH);
        verify(productService, times(1)).updateProductQuantity("p12345678", 2, Locale.ENGLISH);
        verify(productService, times(1)).updateProductQuantity("p98765432", 1, Locale.ENGLISH);
        verify(customerService, times(1)).findOrCreateCustomer(any(Bill.CustomerInfo.class), eq(Locale.ENGLISH));
        verify(customerService, times(1)).addBillId("1234567890", "b12345678");
        verify(billRepository, times(1)).save(any(Bill.class));
    }

    @Test
    void testCreateBill_WithoutCustomer_Success() {
        // Given
        bill.setCustomer(null); // No customer
        when(billRepository.existsByBillId("b12345678")).thenReturn(false);
        when(productService.getProductById("p12345678", Locale.ENGLISH)).thenReturn(product1);
        when(productService.getProductById("p98765432", Locale.ENGLISH)).thenReturn(product2);
        when(billRepository.save(any(Bill.class))).thenReturn(bill);

        // When
        Bill result = billService.createBill(bill, Locale.ENGLISH);

        // Then
        assertNotNull(result);
        assertEquals("b12345678", result.getBillId());

        // Customer service should not be called when there's no customer
        verify(customerService, never()).findOrCreateCustomer(any(), any());
        verify(customerService, never()).addBillId(anyString(), anyString());

        verify(billRepository, times(1)).save(any(Bill.class));
    }

    @Test
    void testCreateBill_ProductNotFound_ThrowsException() {
        // Given
        when(billRepository.existsByBillId("b12345678")).thenReturn(false);
        when(productService.getProductById("p12345678", Locale.ENGLISH))
                .thenThrow(new RuntimeException("Product not found"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            billService.createBill(bill, Locale.ENGLISH);
        });

        assertEquals("Product not found", exception.getMessage());
        verify(productService, times(1)).getProductById("p12345678", Locale.ENGLISH);
        verify(billRepository, never()).save(any(Bill.class));
    }
    @Test
    void testGetBillById_NotFound_ThrowsException() {
        when(billRepository.findByBillId("nonexistent")).thenReturn(java.util.Optional.empty());
        when(messageSource.getMessage("bill.not.found", new Object[]{"nonexistent"}, Locale.ENGLISH))
                .thenReturn("Bill not found: nonexistent");

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            billService.getBillById("nonexistent", Locale.ENGLISH);
        });

        assertEquals("Bill not found: nonexistent", exception.getMessage());
    }

    @Test
    void testExistsByBillId() {
        when(billRepository.existsByBillId("b12345678")).thenReturn(true);

        boolean exists = billService.existsByBillId("b12345678");

        assertTrue(exists);
        verify(billRepository, times(1)).existsByBillId("b12345678");
    }

    @Test
    void testValidatePdfAccessToken_ValidToken() {
        Bill bill = new Bill();
        bill.setPdfAccessToken("valid-token");

        when(billRepository.findByBillId("b12345678")).thenReturn(java.util.Optional.of(bill));

        boolean isValid = billService.validatePdfAccessToken("b12345678", "valid-token");

        assertTrue(isValid);
    }

    @Test
    void testValidatePdfAccessToken_InvalidToken() {
        Bill bill = new Bill();
        bill.setPdfAccessToken("valid-token");

        when(billRepository.findByBillId("b12345678")).thenReturn(java.util.Optional.of(bill));

        boolean isValid = billService.validatePdfAccessToken("b12345678", "invalid-token");

        assertFalse(isValid);
    }
}