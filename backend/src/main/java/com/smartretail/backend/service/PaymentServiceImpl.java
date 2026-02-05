package com.smartretail.backend.service;

import com.smartretail.backend.models.Payment;
import com.smartretail.backend.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final com.smartretail.backend.security.SecurityUtils securityUtils;

    public PaymentServiceImpl(PaymentRepository paymentRepository, com.smartretail.backend.security.SecurityUtils securityUtils) {
        this.paymentRepository = paymentRepository;
        this.securityUtils = securityUtils;
    }

    @Override
    public Payment createPayment(Payment payment) {
        System.out.println("[SERVICE] Creating payment: " + payment.getPaymentId());
        String paymentId = "pay_" + UUID.randomUUID().toString().substring(0, 8);
        payment.setPaymentId(paymentId);
        String shopId = securityUtils.getCurrentShopId();
        payment.setShopId(shopId);
        if (paymentRepository.existsByPaymentIdAndShopId(paymentId, shopId)) {
            System.out.println("[SERVICE] Create failed: Payment ID already exists: " + paymentId);
            throw new RuntimeException("Payment ID already exists");
        }
        payment.setCreatedAt(new Date());
        Payment savedPayment = paymentRepository.save(payment);
        System.out.println("[SERVICE] Payment created successfully: " + savedPayment.getPaymentId());
        return savedPayment;
    }

    @Override
    public Payment getPaymentById(String paymentId) {
        System.out.println("[SERVICE] Fetching payment with ID: " + paymentId);
        String shopId = securityUtils.getCurrentShopId();
        return paymentRepository.findByPaymentIdAndShopId(paymentId, shopId).orElse(null);
    }

    @Override
    public List<Payment> getAllPayments() {
        System.out.println("[SERVICE] Fetching all payments.");
        String shopId = securityUtils.getCurrentShopId();
        return paymentRepository.findByShopId(shopId);
    }
}