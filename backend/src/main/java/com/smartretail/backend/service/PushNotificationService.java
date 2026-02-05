package com.smartretail.backend.service;

import com.smartretail.backend.models.PushSubscription;
import com.smartretail.backend.repository.PushSubscriptionRepository;
import lombok.Getter;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.Optional;

@Service
public class PushNotificationService {

    private final PushSubscriptionRepository subscriptionRepository;
    private final PushService pushService;
    private final com.smartretail.backend.security.SecurityUtils securityUtils;

    @Getter
    private final String vapidPublicKey;
    private final String vapidPrivateKey;

    public PushNotificationService(
            PushSubscriptionRepository subscriptionRepository,
            @Value("${vapid.public.key}") String vapidPublicKey,
            @Value("${vapid.private.key}") String vapidPrivateKey,
            com.smartretail.backend.security.SecurityUtils securityUtils) throws GeneralSecurityException {
        this.subscriptionRepository = subscriptionRepository;
        this.vapidPublicKey = vapidPublicKey;
        this.vapidPrivateKey = vapidPrivateKey;
        this.securityUtils = securityUtils;

        Security.addProvider(new BouncyCastleProvider());

        this.pushService = new PushService(vapidPublicKey, vapidPrivateKey);
    }

    public void saveSubscription(String userId, PushSubscription subscription) {
        subscription.setUserId(userId);
        String shopId = securityUtils.getCurrentShopId();
        subscription.setShopId(shopId);
        subscriptionRepository.save(subscription);
        System.out.println("[PUSH] Subscription saved for user: " + userId);
    }

    public void sendPushNotification(String userId, String title, String message) {
        String shopId = securityUtils.getCurrentShopId();
        Optional<PushSubscription> subscriptionOpt = subscriptionRepository.findByUserIdAndShopId(userId, shopId);
        if (subscriptionOpt.isPresent()) {
            PushSubscription subscriptionEntity = subscriptionOpt.get();

            try {
                // Convert your entity to web-push library's Subscription object
                Subscription subscription = new Subscription(
                        subscriptionEntity.getEndpoint(),
                        new Subscription.Keys(
                                subscriptionEntity.getP256dh(),
                                subscriptionEntity.getAuth()));

                // Create notification payload (you can use JSON for more complex messages)
                String payload = "{\"title\":\"" + title + "\",\"body\":\"" + message + "\"}";

                // Create and send notification
                Notification notification = new Notification(subscription, payload);
                pushService.send(notification);

                System.out.println("[PUSH] Notification sent to user: " + userId);
            } catch (Exception e) {
                System.err.println(
                        "[PUSH] Failed to send notification to user: " + userId + ", error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // Manual getter for vapidPublicKey (Lombok @Getter not working)
    public String getVapidPublicKey() {
        return vapidPublicKey;
    }
}