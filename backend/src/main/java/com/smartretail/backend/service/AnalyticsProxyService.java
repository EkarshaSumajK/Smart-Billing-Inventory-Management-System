package com.smartretail.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AnalyticsProxyService {

    private final RestTemplate restTemplate;
    @Value("${flask.base.url}")
    private String flaskBaseUrl;

    public AnalyticsProxyService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Object getFromFlask(String path) {
        String url = flaskBaseUrl + path;
        return restTemplate.getForObject(url, Object.class);
    }

    public byte[] getPdfFromFlask(String path) {
        String url = flaskBaseUrl + path;
        ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);
        return response.getBody();
    }
}
