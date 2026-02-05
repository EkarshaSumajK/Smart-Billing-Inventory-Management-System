package com.smartretail.backend.service;

import com.smartretail.backend.models.Bill;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public interface BillService {
    Bill createBill(Bill bill, Locale locale);

    Bill createBill(Bill bill, Locale locale, boolean isSyncMode);

    Bill getBillById(String billId, Locale locale);

    List<Bill> getAllBills();

    List<Bill> getBillsByDateRange(Date startDate, Date endDate, Locale locale);

    boolean validatePdfAccessToken(String billId, String token);

    String generatePdfAccessToken(String billId); // Added method

    boolean existsByBillId(String billId);

    void resendBillEmail(Bill bill, Locale locale);

    List<com.smartretail.backend.models.Product> getUpsellSuggestions(List<String> currentItemIds);

    void triggerRecommendationTraining();
}