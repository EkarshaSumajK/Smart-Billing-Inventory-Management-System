package com.smartretail.backend.controller;

import com.smartretail.backend.dto.FullReportResponse;
import com.smartretail.backend.service.AnalyticsService;
import com.smartretail.backend.service.ReportPdfService;
import com.smartretail.backend.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);
    private final ReportService reportService;
    private final MessageSource messageSource;
    private final AnalyticsService analyticsService;
    private final ReportPdfService reportPdfService;

    public ReportController(ReportService reportService,
                            MessageSource messageSource,
                            AnalyticsService analyticsService,
                            ReportPdfService reportPdfService) {
        this.reportService = reportService;
        this.messageSource = messageSource;
        this.analyticsService = analyticsService;
        this.reportPdfService = reportPdfService;
    }

    @GetMapping("/sales")
    public ResponseEntity<Map<String, Object>> getSalesReport(
            @RequestParam String startDate,
            @RequestParam String endDate,
            Locale locale) {
        logger.debug("Fetching sales report: startDate={}, endDate={}", startDate, endDate);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC")); // Parse dates in UTC
        dateFormat.setLenient(false);
        try {
            Date start = dateFormat.parse(startDate);
            Date end = dateFormat.parse(endDate);
            // Adjust endDate to include the full day
            end = new Date(end.getTime() + 24 * 60 * 60 * 1000 - 1); // End of day
            Map<String, Object> report = reportService.getSalesReport(start, end, locale);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            logger.error("Invalid date format: startDate={} or endDate={}", startDate, endDate, e);
            throw new IllegalArgumentException(
                    messageSource.getMessage("report.date.invalid", new Object[]{startDate + " or " + endDate}, locale));
        }
    }

    @GetMapping("/inventory")
    public ResponseEntity<Map<String, Object>> getInventoryReport(
            @RequestParam(defaultValue = "10") int lowStockThreshold,
            @RequestParam(defaultValue = "30") int expiryDays,
            Locale locale) {
        logger.debug("Fetching inventory report: lowStockThreshold={}, expiryDays={}", lowStockThreshold, expiryDays);
        Map<String, Object> report = reportService.getInventoryReport(lowStockThreshold, expiryDays, locale);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/full")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ResponseEntity<FullReportResponse> getFullReport(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "10") int lowStockThreshold,
            @RequestParam(defaultValue = "30") int expiryDays,
            Locale locale) {

        logger.debug("Generating full report: {} to {}", startDate, endDate);
        // Delegate all logic to the service
        FullReportResponse fullReport = reportService.getFullReportData(startDate, endDate, lowStockThreshold, expiryDays, locale);
        return ResponseEntity.ok(fullReport);
    }

    @GetMapping("/full/pdf")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ResponseEntity<byte[]> downloadFullReportPdf(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "10") int lowStockThreshold,
            @RequestParam(defaultValue = "30") int expiryDays,
            Locale locale) {

        logger.debug("Generating PDF report: {} to {}", startDate, endDate);

        // 1. Get the same data DTO
        FullReportResponse fullReport = reportService.getFullReportData(startDate, endDate, lowStockThreshold, expiryDays, locale);

        // 2. Pass DTO to PDF service
        byte[] pdfBytes = reportPdfService.generateFullReportPdf(fullReport, startDate, endDate, locale);

        // 3. Prepare headers for file download
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        String filename = String.format("SmartRetail_Report_%s_to_%s.pdf", startDate, endDate);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}