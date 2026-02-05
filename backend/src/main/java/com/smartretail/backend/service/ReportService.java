package com.smartretail.backend.service;

import com.smartretail.backend.dto.FullReportResponse;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public interface ReportService {
    Map<String, Object> getSalesReport(Date startDate, Date endDate, Locale locale);
    Map<String, Object> getInventoryReport(int lowStockThreshold, int expiryDays, Locale locale);

    // --- ADDED NEW METHOD ---
    FullReportResponse getFullReportData(String startDate, String endDate, int lowStockThreshold, int expiryDays, Locale locale);
}