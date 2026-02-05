package com.smartretail.backend.service;

import com.smartretail.backend.models.Bill;
import com.smartretail.backend.models.Product;
import com.smartretail.backend.repository.BillRepository;
import com.smartretail.backend.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private BillRepository billRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private MongoTemplate mongoTemplate;
    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private ReportServiceImpl reportService;

    @Test
    void testGetSalesReport_ValidRange_ReturnsReport() {
        Date startDate = new Date(2025 - 1900, 8, 1);
        Date endDate = new Date(2025 - 1900, 8, 30);
        Map<String, Object> aggregationResult = new HashMap<>();
        aggregationResult.put("totalSales", 5000.0);
        aggregationResult.put("billCount", 10);
        aggregationResult.put("averageBillAmount", 500.0);

        when(mongoTemplate.aggregate(any(), eq("bills"), eq(Map.class)))
                .thenReturn(new AggregationResults<>(List.of(aggregationResult), null));
        when(billRepository.findAll()).thenReturn(List.of(new Bill()));

        Map<String, Object> result = reportService.getSalesReport(startDate, endDate, Locale.ENGLISH);

        assertEquals(5000.0, result.get("totalSales"));
        assertEquals(10, result.get("billCount"));
        assertEquals(500.0, result.get("averageBillAmount"));
        assertFalse(((List<?>) result.get("bills")).isEmpty());
    }

    @Test
    void testGetSalesReport_NoBills_ThrowsException() {
        Date startDate = new Date(2025 - 1900, 8, 1);
        Date endDate = new Date(2025 - 1900, 8, 30);
        when(mongoTemplate.aggregate(any(), eq("bills"), eq(Map.class)))
                .thenReturn(new AggregationResults<>(List.of(), null));
        when(messageSource.getMessage("report.no.bills.found", null, Locale.ENGLISH))
                .thenReturn("No bills found for the given date range");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            reportService.getSalesReport(startDate, endDate, Locale.ENGLISH);
        });

        assertEquals("No bills found for the given date range", exception.getMessage());
    }

    @Test
    void testGetInventoryReport_ValidParams_ReturnsReport() {
        Product product = new Product();
        product.setProductId("p123");
        product.setQuantity(5);
        product.setExpiryDate(new Date(System.currentTimeMillis() + 15 * 24 * 60 * 60 * 1000L)); // 15 days

        when(productRepository.findAll()).thenReturn(List.of(product));

        Map<String, Object> result = reportService.getInventoryReport(10, 30, Locale.ENGLISH);

        assertEquals(1, result.get("totalProducts"));
        assertEquals(1, ((List<?>) result.get("lowStockProducts")).size());
        assertEquals(1, ((List<?>) result.get("expiringProducts")).size());
    }
}