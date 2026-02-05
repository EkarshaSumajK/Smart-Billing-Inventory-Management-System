package com.smartretail.backend.service;

import com.smartretail.backend.models.Bill;
import java.util.Locale;

public interface PdfService {
    byte[] generateBillPdf(Bill bill, Locale locale);
}