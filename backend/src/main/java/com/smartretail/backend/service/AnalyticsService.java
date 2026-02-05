package com.smartretail.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

// --- NEW IMPORTS ---
import org.springframework.context.i18n.LocaleContextHolder;
import java.util.Locale;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
// --- END NEW IMPORTS ---

@Service
public class AnalyticsService {
    private final RestTemplate restTemplate;
    private final com.smartretail.backend.security.SecurityUtils securityUtils;

    @Value("${flask.base.url:http://localhost:5001}")
    private String flaskBaseUrl;

    public AnalyticsService(RestTemplate restTemplate, com.smartretail.backend.security.SecurityUtils securityUtils) {
        this.restTemplate = restTemplate;
        this.securityUtils = securityUtils;
    }

    // --- MODIFIED METHOD ---
    public Object getReport(String startDate, String endDate) {
        String url = flaskBaseUrl + "/analytics/report";
        return getFromFlask(url, startDate, endDate);
    }

    // --- MODIFIED METHOD ---
    public Object getTextReport(String startDate, String endDate) {
        String url = flaskBaseUrl + "/analytics/report/text";
        return getFromFlask(url, startDate, endDate);
    }

    // --- NEW HELPER METHOD ---
    private Object getFromFlask(String url, String startDate, String endDate) {
        // 1. Get the locale from the current request
        Locale locale = LocaleContextHolder.getLocale();

        // 2. Build the URL with query params
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
        if (startDate != null) builder.queryParam("startDate", startDate);
        if (endDate != null) builder.queryParam("endDate", endDate);
        
        String shopId = securityUtils.getCurrentShopId();
        if (shopId != null) builder.queryParam("shopId", shopId);

        // 3. Create headers and set the language
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept-Language", locale.toLanguageTag());

        // 4. Create an HttpEntity with the headers
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // 5. Use restTemplate.exchange to send the request with headers
        ResponseEntity<Object> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                Object.class
        );

        return response.getBody();
    }
}