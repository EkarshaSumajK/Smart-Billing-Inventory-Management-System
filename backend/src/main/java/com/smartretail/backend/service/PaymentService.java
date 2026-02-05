package com.smartretail.backend.service;

import com.smartretail.backend.models.Payment;
import java.util.List;

public interface PaymentService {
    Payment createPayment(Payment payment);
    Payment getPaymentById(String paymentId);
    List<Payment> getAllPayments();
}