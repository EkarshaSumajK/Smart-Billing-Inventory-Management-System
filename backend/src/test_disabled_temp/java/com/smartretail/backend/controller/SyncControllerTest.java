package com.smartretail.backend.controller;

import com.smartretail.backend.models.Bill;
import com.smartretail.backend.models.Product;
import com.smartretail.backend.service.BillService;
import com.smartretail.backend.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SyncControllerTest {

    @Mock
    private BillService billService;

    @Mock
    private ProductService productService;

    @InjectMocks
    private SyncController syncController;

    private Bill bill;
    private Product product;
    private SyncController.SyncRequest syncRequest;

    @BeforeEach
    void setUp() {
        bill = new Bill();
        bill.setBillId("bill005");
        bill.setTotalAmount(600.0);
        bill.setCreatedAt(new Date());
        Bill.CustomerInfo customerInfo = new Bill.CustomerInfo();
        customerInfo.setMobile("5554447788");
        customerInfo.setEmail("sambhavkumath9753@gmail.com");
        customerInfo.setName("Sambhav");
        bill.setCustomer(customerInfo);
        Bill.BillItem item = new Bill.BillItem();
        item.setProductId("p123");
        item.setProductName("Laptop");
        item.setQty(1);
        item.setPrice(600.0);
        bill.setItems(Arrays.asList(item));

        product = new Product();
        product.setProductId("p123"); // FIXED: Use setProductId instead of setId
        product.setName("Laptop");
        product.setPrice(600.0);
        product.setQuantity(9);

        syncRequest = new SyncController.SyncRequest();
        syncRequest.setBills(Arrays.asList(bill));
        syncRequest.setInventoryUpdates(Arrays.asList(product));
    }

    @Test
    void testSyncPendingData_Success() {
        when(billService.createBill(any(Bill.class), eq(Locale.ENGLISH), eq(true))).thenReturn(bill);
        when(productService.updateProduct(eq("p123"), any(Product.class), eq(Locale.ENGLISH), eq(true))).thenReturn(product);

        ResponseEntity<Map<String, Object>> response = syncController.syncPendingData(syncRequest, Locale.ENGLISH);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertEquals(Arrays.asList("bill005"), body.get("processedBills"));
        assertEquals(Arrays.asList("p123"), body.get("processedProducts"));
        assertTrue(((List<?>) body.get("errors")).isEmpty());
        assertEquals("Sync completed successfully", body.get("message"));
        verify(billService).createBill(bill, Locale.ENGLISH, true);
        verify(productService).updateProduct("p123", product, Locale.ENGLISH, true);
    }

    @Test
    void testSyncPendingData_BillDuplicate() {
        when(billService.createBill(any(Bill.class), eq(Locale.ENGLISH), eq(true)))
                .thenThrow(new IllegalArgumentException("Bill bill005 already exists"));
        when(productService.updateProduct(eq("p123"), any(Product.class), eq(Locale.ENGLISH), eq(true))).thenReturn(product);

        ResponseEntity<Map<String, Object>> response = syncController.syncPendingData(syncRequest, Locale.ENGLISH);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertTrue(((List<?>) body.get("processedBills")).isEmpty());
        assertEquals(Arrays.asList("p123"), body.get("processedProducts"));
        assertEquals(Arrays.asList("Bill bill005: Bill bill005 already exists"), body.get("errors"));
        assertEquals("Sync completed with errors", body.get("message"));
        verify(billService).createBill(bill, Locale.ENGLISH, true);
        verify(productService).updateProduct("p123", product, Locale.ENGLISH, true);
    }

    @Test
    void testSyncPendingData_ProductError() {
        when(billService.createBill(any(Bill.class), eq(Locale.ENGLISH), eq(true))).thenReturn(bill);
        when(productService.updateProduct(eq("p123"), any(Product.class), eq(Locale.ENGLISH), eq(true)))
                .thenThrow(new IllegalArgumentException("Product p123 not found"));

        ResponseEntity<Map<String, Object>> response = syncController.syncPendingData(syncRequest, Locale.ENGLISH);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertEquals(Arrays.asList("bill005"), body.get("processedBills"));
        assertTrue(((List<?>) body.get("processedProducts")).isEmpty());
        assertEquals(Arrays.asList("Product p123: Product p123 not found"), body.get("errors"));
        assertEquals("Sync completed with errors", body.get("message"));
        verify(billService).createBill(bill, Locale.ENGLISH, true);
        verify(productService).updateProduct("p123", product, Locale.ENGLISH, true);
    }

    @Test
    void testSyncPendingData_EmptyRequest() {
        syncRequest.setBills(List.of());
        syncRequest.setInventoryUpdates(List.of());

        ResponseEntity<Map<String, Object>> response = syncController.syncPendingData(syncRequest, Locale.ENGLISH);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertTrue(((List<?>) body.get("processedBills")).isEmpty());
        assertTrue(((List<?>) body.get("processedProducts")).isEmpty());
        assertTrue(((List<?>) body.get("errors")).isEmpty());
        assertEquals("Sync completed successfully", body.get("message"));
        verify(billService, never()).createBill(any(), any(), anyBoolean());
        verify(productService, never()).updateProduct(any(), any(), any(), anyBoolean());
    }

    @Test
    void testSyncPendingData_NullLists() {
        syncRequest.setBills(null);
        syncRequest.setInventoryUpdates(null);

        ResponseEntity<Map<String, Object>> response = syncController.syncPendingData(syncRequest, Locale.ENGLISH);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertTrue(((List<?>) body.get("processedBills")).isEmpty());
        assertTrue(((List<?>) body.get("processedProducts")).isEmpty());
        assertTrue(((List<?>) body.get("errors")).isEmpty());
        assertEquals("Sync completed successfully", body.get("message"));
        verify(billService, never()).createBill(any(), any(), anyBoolean());
        verify(productService, never()).updateProduct(any(), any(), any(), anyBoolean());
    }
}