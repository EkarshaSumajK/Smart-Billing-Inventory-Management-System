package com.smartretail.backend.service;

import com.smartretail.backend.dto.FullReportResponse;
import java.util.Locale;

public interface ReportPdfService {
    /**
     * Generates a full executive report as a PDF byte array.
     * @param reportData The combined report data DTO
     * @param locale The locale for any I18n strings (if needed)
     * @return A byte[] containing the PDF data
     */
    byte[] generateFullReportPdf(FullReportResponse reportData, String startDate, String endDate, Locale locale);
}