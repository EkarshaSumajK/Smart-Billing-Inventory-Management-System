
package com.smartretail.backend.service;

import com.smartretail.backend.models.Notification;

import java.util.List;
import java.util.Locale;

public interface NotificationService {
    Notification createNotification(Notification notification);
    List<Notification> getAllNotifications();
    void sendEmail(String to, String subject, String text);
    void sendBillNotification(String customerEmail, String billId, double total, byte[] pdfContent, Locale locale);
    void sendLowStockNotification(String managerEmail, String productName, int quantity, int reorderLevel);
}
