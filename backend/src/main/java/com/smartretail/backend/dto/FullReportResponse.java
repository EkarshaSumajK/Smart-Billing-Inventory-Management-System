package com.smartretail.backend.dto;

import java.util.Map;

public class FullReportResponse {
    private Object salesReport;  // Changed from Map<String, Object> to Object
    private Object textSummary;  // This is what you're using in builder
    private Map<String, Object> inventoryReport;

    // Default constructor
    public FullReportResponse() {}

    // Constructor with all fields
    public FullReportResponse(Object salesReport, Object textSummary, Map<String, Object> inventoryReport) {
        this.salesReport = salesReport;
        this.textSummary = textSummary;
        this.inventoryReport = inventoryReport;
    }

    // Getters and setters
    public Object getSalesReport() {
        return salesReport;
    }

    public void setSalesReport(Object salesReport) {
        this.salesReport = salesReport;
    }

    public Object getTextSummary() {
        return textSummary;
    }

    public void setTextSummary(Object textSummary) {
        this.textSummary = textSummary;
    }

    public Map<String, Object> getInventoryReport() {
        return inventoryReport;
    }

    public void setInventoryReport(Map<String, Object> inventoryReport) {
        this.inventoryReport = inventoryReport;
    }

    // Builder pattern methods
    public static FullReportResponseBuilder builder() {
        return new FullReportResponseBuilder();
    }

    public static class FullReportResponseBuilder {
        private Object salesReport;
        private Object textSummary;
        private Map<String, Object> inventoryReport;

        public FullReportResponseBuilder salesReport(Object salesReport) {
            this.salesReport = salesReport;
            return this;
        }

        public FullReportResponseBuilder textSummary(Object textSummary) {
            this.textSummary = textSummary;
            return this;
        }

        public FullReportResponseBuilder inventoryReport(Map<String, Object> inventoryReport) {
            this.inventoryReport = inventoryReport;
            return this;
        }

        public FullReportResponse build() {
            return new FullReportResponse(salesReport, textSummary, inventoryReport);
        }
    }

    @Override
    public String toString() {
        return "FullReportResponse{" +
                "salesReport=" + salesReport +
                ", textSummary=" + textSummary +
                ", inventoryReport=" + inventoryReport +
                '}';
    }
}