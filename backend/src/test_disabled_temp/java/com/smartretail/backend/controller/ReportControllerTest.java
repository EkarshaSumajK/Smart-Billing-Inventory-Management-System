
package com.smartretail.backend.controller;

import com.smartretail.backend.models.Bill;
import com.smartretail.backend.models.Product;
import com.smartretail.backend.service.ReportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.SimpleDateFormat;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    @Mock
    private ReportService reportService;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private ReportController reportController;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    @Test
    void testGetSalesReport_Success() throws Exception {
        // Arrange
        String startDateStr = "2025-09-01";
        String endDateStr = "2025-09-30";
        Date startDate = dateFormat.parse(startDateStr);
        Date endDate = new Date(dateFormat.parse(endDateStr).getTime() + 24 * 60 * 60 * 1000 - 1);

        Map<String, Object> report = new HashMap<>();
        report.put("totalSales", 10000.0);
        report.put("billCount", 50);
        report.put("averageBillAmount", 200.0);
        Bill bill = new Bill();
        bill.setBillId("b123");
        bill.setTotalAmount(200.0);
        bill.setCreatedAt(new Date());
        report.put("bills", List.of(bill));

        when(reportService.getSalesReport(startDate, endDate, Locale.ENGLISH)).thenReturn(report);

        // Act
        ResponseEntity<Map<String, Object>> response = reportController.getSalesReport(startDateStr, endDateStr, Locale.ENGLISH);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(report, response.getBody());
        assertEquals(10000.0, response.getBody().get("totalSales"));
        assertEquals(50, response.getBody().get("billCount"));
        assertEquals(200.0, response.getBody().get("averageBillAmount"));
        assertEquals(1, ((List<?>) response.getBody().get("bills")).size());
        verify(reportService, times(1)).getSalesReport(startDate, endDate, Locale.ENGLISH);
    }

    @Test
    void testGetSalesReport_InvalidDateFormat() {
        // Arrange
        String startDateStr = "2025-13-01"; // Invalid month
        String endDateStr = "2025-09-30";
        when(messageSource.getMessage(eq("report.date.invalid"), any(), eq(Locale.ENGLISH)))
                .thenReturn("Invalid date format: 2025-13-01 or 2025-09-30");

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> reportController.getSalesReport(startDateStr, endDateStr, Locale.ENGLISH));
        assertEquals("Invalid date format: 2025-13-01 or 2025-09-30", exception.getMessage());
        verify(reportService, never()).getSalesReport(any(), any(), any());
    }

    @Test
    void testGetSalesReport_StartDateAfterEndDate() throws Exception {
        // Arrange
        String startDateStr = "2025-09-30";
        String endDateStr = "2025-09-01";
        Date startDate = dateFormat.parse(startDateStr);
        Date endDate = new Date(dateFormat.parse(endDateStr).getTime() + 24 * 60 * 60 * 1000 - 1);
        when(reportService.getSalesReport(startDate, endDate, Locale.ENGLISH))
                .thenThrow(new IllegalArgumentException("Start date 2025-09-30 must be before end date 2025-09-01"));
        when(messageSource.getMessage(eq("report.date.range.invalid"), any(), eq(Locale.ENGLISH)))
                .thenReturn("Start date 2025-09-30 must be before end date 2025-09-01");

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> reportController.getSalesReport(startDateStr, endDateStr, Locale.ENGLISH));
        assertEquals("Start date 2025-09-30 must be before end date 2025-09-01", exception.getMessage());
        verify(reportService, times(1)).getSalesReport(startDate, endDate, Locale.ENGLISH);
    }

    @Test
    void testGetSalesReport_NoBillsFound() throws Exception {
        // Arrange
        String startDateStr = "2025-09-01";
        String endDateStr = "2025-09-30";
        Date startDate = dateFormat.parse(startDateStr);
        Date endDate = new Date(dateFormat.parse(endDateStr).getTime() + 24 * 60 * 60 * 1000 - 1);
        Map<String, Object> report = new HashMap<>();
        report.put("totalSales", 0.0);
        report.put("billCount", 0);
        report.put("averageBillAmount", 0.0);
        report.put("bills", Collections.emptyList());
        when(reportService.getSalesReport(startDate, endDate, Locale.ENGLISH)).thenReturn(report);

        // Act
        ResponseEntity<Map<String, Object>> response = reportController.getSalesReport(startDateStr, endDateStr, Locale.ENGLISH);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0.0, response.getBody().get("totalSales"));
        assertEquals(0, response.getBody().get("billCount"));
        assertEquals(0.0, response.getBody().get("averageBillAmount"));
        assertEquals(0, ((List<?>) response.getBody().get("bills")).size());
        verify(reportService, times(1)).getSalesReport(startDate, endDate, Locale.ENGLISH);
    }

    @Test
    void testGetInventoryReport_Success() {
        // Arrange
        int lowStockThreshold = 10;
        int expiryDays = 30;
        Product product = new Product();
        product.setProductId("p123");
        product.setName("Product A");
        product.setQuantity(5);
        product.setExpiryDate(new Date());
        Map<String, Object> report = new HashMap<>();
        report.put("totalProducts", 100);
        report.put("lowStockProducts", List.of(product));
        report.put("expiringProducts", List.of(product));
        report.put("lowStockCount", 1);
        report.put("expiringCount", 1);
        when(reportService.getInventoryReport(lowStockThreshold, expiryDays, Locale.ENGLISH)).thenReturn(report);

        // Act
        ResponseEntity<Map<String, Object>> response = reportController.getInventoryReport(lowStockThreshold, expiryDays, Locale.ENGLISH);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(100, response.getBody().get("totalProducts"));
        assertEquals(1, response.getBody().get("lowStockCount"));
        assertEquals(1, response.getBody().get("expiringCount"));
        assertEquals(1, ((List<?>) response.getBody().get("lowStockProducts")).size());
        assertEquals(1, ((List<?>) response.getBody().get("expiringProducts")).size());
        verify(reportService, times(1)).getInventoryReport(lowStockThreshold, expiryDays, Locale.ENGLISH);
    }

    @Test
    void testGetInventoryReport_NegativeLowStockThreshold() {
        // Arrange
        int lowStockThreshold = -1;
        int expiryDays = 30;
        when(reportService.getInventoryReport(lowStockThreshold, expiryDays, Locale.ENGLISH))
                .thenThrow(new IllegalArgumentException("Low stock threshold must be positive"));
        when(messageSource.getMessage(eq("report.lowStockThreshold.invalid"), any(), eq(Locale.ENGLISH)))
                .thenReturn("Low stock threshold must be positive");

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> reportController.getInventoryReport(lowStockThreshold, expiryDays, Locale.ENGLISH));
        assertEquals("Low stock threshold must be positive", exception.getMessage());
        verify(reportService, times(1)).getInventoryReport(lowStockThreshold, expiryDays, Locale.ENGLISH);
    }

    @Test
    void testGetInventoryReport_NegativeExpiryDays() {
        // Arrange
        int lowStockThreshold = 10;
        int expiryDays = -1;
        when(reportService.getInventoryReport(lowStockThreshold, expiryDays, Locale.ENGLISH))
                .thenThrow(new IllegalArgumentException("Expiry days must be positive"));
        when(messageSource.getMessage(eq("report.expiryDays.invalid"), any(), eq(Locale.ENGLISH)))
                .thenReturn("Expiry days must be positive");

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> reportController.getInventoryReport(lowStockThreshold, expiryDays, Locale.ENGLISH));
        assertEquals("Expiry days must be positive", exception.getMessage());
        verify(reportService, times(1)).getInventoryReport(lowStockThreshold, expiryDays, Locale.ENGLISH);
    }

    @Test
    void testGetInventoryReport_NoProductsFound() {
        // Arrange
        int lowStockThreshold = 10;
        int expiryDays = 30;
        when(reportService.getInventoryReport(lowStockThreshold, expiryDays, Locale.ENGLISH))
                .thenThrow(new IllegalArgumentException("No products found"));
        when(messageSource.getMessage(eq("report.no.products.found"), any(), eq(Locale.ENGLISH)))
                .thenReturn("No products found");

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> reportController.getInventoryReport(lowStockThreshold, expiryDays, Locale.ENGLISH));
        assertEquals("No products found", exception.getMessage());
        verify(reportService, times(1)).getInventoryReport(lowStockThreshold, expiryDays, Locale.ENGLISH);
    }
}
