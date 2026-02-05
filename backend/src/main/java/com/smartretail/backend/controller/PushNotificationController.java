package com.smartretail.backend.controller;

import com.smartretail.backend.models.PushSubscription;
import com.smartretail.backend.service.PushNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class PushNotificationController {

    private final PushNotificationService pushNotificationService;

    public PushNotificationController(PushNotificationService pushNotificationService) {
        this.pushNotificationService = pushNotificationService;
    }

    @PostMapping("/subscribe")
    public ResponseEntity<String> subscribe(@RequestBody PushSubscription subscription) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = auth.getName(); // Email or userId from JWT
        pushNotificationService.saveSubscription(userId, subscription);
        return ResponseEntity.ok("Subscription saved");
    }

    @GetMapping("/vapid-public-key")
    public ResponseEntity<String> getVapidPublicKey() {
        return ResponseEntity.ok(pushNotificationService.getVapidPublicKey());
    }
}