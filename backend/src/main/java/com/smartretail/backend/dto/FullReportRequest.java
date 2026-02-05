package com.smartretail.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FullReportRequest {
    private String startDate;
    private String endDate;
    private int lowStockThreshold = 10;
    private int expiryDays = 30;

    // Default constructor
    public FullReportRequest() {}

    // Getters and setters
    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public int getLowStockThreshold() {
        return lowStockThreshold;
    }

    public void setLowStockThreshold(int lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    public int getExpiryDays() {
        return expiryDays;
    }

    public void setExpiryDays(int expiryDays) {
        this.expiryDays = expiryDays;
    }

    @Override
    public String toString() {
        return "FullReportRequest{" +
                "startDate='" + startDate + '\'' +
                ", endDate='" + endDate + '\'' +
                ", lowStockThreshold=" + lowStockThreshold +
                ", expiryDays=" + expiryDays +
                '}';
    }
}