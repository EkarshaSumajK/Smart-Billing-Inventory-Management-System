package com.smartretail.backend.service;

import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.smartretail.backend.dto.FullReportResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.*;

// --- NEW IMPORT ---
import org.springframework.context.MessageSource;
// --- END NEW IMPORT ---

@Service
public class ReportPdfServiceImpl implements ReportPdfService {

    private static final Logger logger = LoggerFactory.getLogger(ReportPdfServiceImpl.class);

    // --- NEW FIELD ---
    private final MessageSource messageSource;

    // --- NEW CONSTRUCTOR ---
    public ReportPdfServiceImpl(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    // --- NEW HELPER METHOD ---
    private String t(String key, Locale locale) {
        return messageSource.getMessage(key, null, key, locale);
    }

    @Override
    public byte[] generateFullReportPdf(FullReportResponse reportData, String startDate, String endDate, Locale locale) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4);
            document.setMargins(40, 40, 40, 40);

            PdfFont font;
            PdfFont boldFont;
            PdfFont titleFont;
            try {
                font = PdfFontFactory.createFont("Helvetica");
                boldFont = PdfFontFactory.createFont("Helvetica-Bold");
                titleFont = PdfFontFactory.createFont("Helvetica-Bold");
            } catch (IOException e) {
                font = PdfFontFactory.createFont();
                boldFont = PdfFontFactory.createFont();
                titleFont = PdfFontFactory.createFont();
            }

            // Build PDF sections WITHOUT Low Stock Details
            addHeader(document, startDate, endDate, titleFont, font, locale); // <-- Pass locale
            addAISummary(document, reportData, boldFont, font, locale); // <-- Pass locale
            addKeyMetrics(document, reportData, boldFont, font, locale); // <-- Pass locale
            addInventoryAlerts(document, reportData, boldFont, font, locale); // <-- Pass locale
            addTopProducts(document, reportData, boldFont, font, locale); // <-- Pass locale
            // REMOVED: addLowStockDetails(document, reportData, boldFont, font, locale);

            document.close();
            logger.info("Successfully generated PDF report");
            return outputStream.toByteArray();

        } catch (Exception e) {
            logger.error("Failed to generate PDF report: {}", e.getMessage(), e);
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    // --- MODIFIED METHOD ---
    private void addHeader(Document document, String startDate, String endDate, PdfFont titleFont, PdfFont font, Locale locale) {
        // Main title matching website
        Paragraph title = new Paragraph(t("pdf.report.title", locale)) // "Executive Report"
                .setFont(titleFont)
                .setFontSize(24)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.BLACK)
                .setMarginBottom(5);
        document.add(title);

        // Date range matching website format
        Paragraph dateRange = new Paragraph(startDate + " " + t("pdf.report.to", locale) + " " + endDate) // "to"
                .setFont(font)
                .setFontSize(14)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.DARK_GRAY)
                .setMarginBottom(30);
        document.add(dateRange);

        // Add a subtle separator line
        Paragraph separator = new Paragraph("")
                .setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f))
                .setMarginBottom(30);
        document.add(separator);
    }

    // --- MODIFIED METHOD ---
    private void addAISummary(Document document, FullReportResponse reportData, PdfFont boldFont, PdfFont font, Locale locale) {
        try {
            // Extract AI summary from the correct location in response
            String summary = extractAISummary(reportData);
            if (summary != null && !summary.trim().isEmpty()) {
                Paragraph sectionTitle = new Paragraph(t("pdf.report.aiSummary", locale)) // "AI Generated Summary"
                        .setFont(boldFont)
                        .setFontSize(18)
                        .setFontColor(ColorConstants.BLACK)
                        .setMarginBottom(10);
                document.add(sectionTitle);

                Paragraph summaryParagraph = new Paragraph(summary)
                        .setFont(font)
                        .setFontSize(11)
                        .setFontColor(ColorConstants.DARK_GRAY)
                        .setItalic()
                        .setPadding(15)
                        .setBorder(new SolidBorder(getColorFromHex("#0ea5e9"), 2f)) // Cyan border
                        .setBorderLeft(new SolidBorder(getColorFromHex("#0ea5e9"), 4f))
                        .setBackgroundColor(getColorFromHex("#f8fafc")) // Light gray background
                        .setMarginBottom(25);
                document.add(summaryParagraph);
            }
        } catch (Exception e) {
            logger.warn("Could not parse AI summary for PDF: {}", e.getMessage());
        }
    }

    private String extractAISummary(FullReportResponse reportData) {
        // Try multiple possible locations for AI summary
        if (reportData.getTextSummary() != null) {
            if (reportData.getTextSummary() instanceof Map) {
                Map<?, ?> textSummaryMap = (Map<?, ?>) reportData.getTextSummary();
                Object summary = textSummaryMap.get("report");
                if (summary instanceof String) {
                    return (String) summary;
                }
            } else if (reportData.getTextSummary() instanceof String) {
                return (String) reportData.getTextSummary();
            }
        }

        // Check if summary is in sales report
        if (reportData.getSalesReport() != null && reportData.getSalesReport() instanceof Map) {
            Map<?, ?> salesReport = (Map<?, ?>) reportData.getSalesReport();
            Object summary = salesReport.get("aiSummary");
            if (summary instanceof String) {
                return (String) summary;
            }
        }

        return "AI summary not available for this report period.";
    }

    // --- MODIFIED METHOD ---
    private void addKeyMetrics(Document document, FullReportResponse reportData, PdfFont boldFont, PdfFont font, Locale locale) {
        try {
            Map<String, Object> salesSummary = extractSalesSummary(reportData);
            if (salesSummary != null && !salesSummary.isEmpty()) {
                NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

                Paragraph sectionTitle = new Paragraph(t("pdf.report.keyMetrics", locale)) // "Key Metrics"
                        .setFont(boldFont)
                        .setFontSize(18)
                        .setFontColor(ColorConstants.BLACK)
                        .setMarginBottom(15);
                document.add(sectionTitle);

                // Extract metrics with fallbacks
                double totalRevenue = getDoubleValue(salesSummary.get("total_revenue"));
                int totalOrders = getIntValue(salesSummary.get("total_orders"));
                int productsSold = getIntValue(salesSummary.get("total_products_sold"));

                // If direct values not found, try alternative keys
                if (totalRevenue == 0) {
                    totalRevenue = getDoubleValue(salesSummary.get("totalRevenue"));
                }
                if (totalOrders == 0) {
                    totalOrders = getIntValue(salesSummary.get("totalOrders"));
                }
                if (productsSold == 0) {
                    productsSold = getIntValue(salesSummary.get("totalProductsSold"));
                }

                Table table = new Table(UnitValue.createPercentArray(3)).useAllAvailableWidth();

                // Revenue metric (green color from website)
                table.addCell(createMetricCell(t("pdf.report.totalRevenue", locale), currencyFormat.format(totalRevenue), // "Total Revenue"
                        boldFont, font, ColorConstants.DARK_GRAY, getColorFromHex("#10b981")));

                // Orders metric (blue color from website)
                table.addCell(createMetricCell(t("pdf.report.totalOrders", locale), String.valueOf(totalOrders), // "Total Orders"
                        boldFont, font, ColorConstants.DARK_GRAY, getColorFromHex("#3b82f6")));

                // Products sold metric (cyan color from website)
                table.addCell(createMetricCell(t("pdf.report.productsSold", locale), String.valueOf(productsSold), // "Products Sold"
                        boldFont, font, ColorConstants.DARK_GRAY, getColorFromHex("#06b6d4")));

                document.add(table.setMarginBottom(30));
            }
        } catch (Exception e) {
            logger.warn("Could not parse key metrics for PDF: {}", e.getMessage());
        }
    }

    private Map<String, Object> extractSalesSummary(FullReportResponse reportData) {
        if (reportData.getSalesReport() instanceof Map) {
            Map<?, ?> salesReport = (Map<?, ?>) reportData.getSalesReport();
            Object summaryObj = salesReport.get("summary");
            if (summaryObj instanceof Map) {
                return (Map<String, Object>) summaryObj;
            }
            // If no summary object, use the sales report itself
            return (Map<String, Object>) salesReport;
        }
        return null;
    }

    // --- MODIFIED METHOD ---
    private void addInventoryAlerts(Document document, FullReportResponse reportData, PdfFont boldFont, PdfFont font, Locale locale) {
        try {
            Map<String, Object> inventoryReport = reportData.getInventoryReport();
            if (inventoryReport != null) {
                Paragraph sectionTitle = new Paragraph(t("pdf.report.inventoryAlerts", locale)) // "Inventory Alerts"
                        .setFont(boldFont)
                        .setFontSize(18)
                        .setFontColor(ColorConstants.BLACK)
                        .setMarginBottom(15);
                document.add(sectionTitle);

                // Extract counts with multiple fallback keys
                int lowStockCount = getIntValue(inventoryReport.get("lowStockCount"));
                if (lowStockCount == 0) {
                    lowStockCount = getIntValue(inventoryReport.get("low_stock_count"));
                }

                int expiringCount = getIntValue(inventoryReport.get("expiringCount"));
                if (expiringCount == 0) {
                    expiringCount = getIntValue(inventoryReport.get("expiring_count"));
                }

                Table table = new Table(UnitValue.createPercentArray(2)).useAllAvailableWidth();

                // Low stock (orange color from website)
                table.addCell(createMetricCell(t("pdf.report.lowStock", locale), String.valueOf(lowStockCount), // "Low Stock Items"
                        boldFont, font, ColorConstants.DARK_GRAY, getColorFromHex("#f59e0b")));

                // Expiring soon (red color from website)
                table.addCell(createMetricCell(t("pdf.report.expiring", locale), String.valueOf(expiringCount), // "Expiring Soon Items"
                        boldFont, font, ColorConstants.DARK_GRAY, getColorFromHex("#ef4444")));

                document.add(table.setMarginBottom(30));
            }
        } catch (Exception e) {
            logger.warn("Could not parse inventory alerts for PDF: {}", e.getMessage());
        }
    }

    // --- MODIFIED METHOD ---
    private void addTopProducts(Document document, FullReportResponse reportData, PdfFont boldFont, PdfFont font, Locale locale) {
        try {
            List<Map<String, Object>> topProducts = extractTopProducts(reportData);
            if (topProducts != null && !topProducts.isEmpty()) {
                Paragraph sectionTitle = new Paragraph(t("pdf.report.topProducts", locale)) // "Top Selling Products"
                        .setFont(boldFont)
                        .setFontSize(18)
                        .setFontColor(ColorConstants.BLACK)
                        .setMarginBottom(10);
                document.add(sectionTitle);

                Table table = new Table(UnitValue.createPercentArray(new float[]{3, 2, 2, 2})).useAllAvailableWidth();

                // Header row
                table.addHeaderCell(createHeaderCell(t("pdf.report.productName", locale), boldFont)); // "Product Name"
                table.addHeaderCell(createHeaderCell(t("pdf.report.category", locale), boldFont)); // "Category"
                table.addHeaderCell(createHeaderCell(t("pdf.report.unitsSold", locale), boldFont)); // "Units Sold"
                table.addHeaderCell(createHeaderCell(t("pdf.report.totalRevenue", locale), boldFont)); // "Total Revenue"

                NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

                for (Map<String, Object> product : topProducts) {
                    String name = getStringValue(product.get("name"));
                    String category = getStringValue(product.get("category"));
                    int qty = getIntValue(product.get("qty"));
                    double revenue = getDoubleValue(product.get("revenue"));

                    table.addCell(createBodyCell(name, font));
                    table.addCell(createBodyCell(category, font));
                    table.addCell(createBodyCell(String.valueOf(qty), font));
                    table.addCell(createBodyCell(currencyFormat.format(revenue), font));
                }
                document.add(table.setMarginBottom(25));
            } else {
                // Add message when no top products
                Paragraph noData = new Paragraph(t("pdf.report.noTopProducts", locale)) // "No top selling products data available for this period."
                        .setFont(font)
                        .setFontSize(11)
                        .setFontColor(ColorConstants.DARK_GRAY)
                        .setItalic()
                        .setMarginBottom(25);
                document.add(noData);
            }
        } catch (Exception e) {
            logger.warn("Could not parse top products for PDF: {}", e.getMessage());
        }
    }

    private List<Map<String, Object>> extractTopProducts(FullReportResponse reportData) {
        if (reportData.getSalesReport() instanceof Map) {
            Map<?, ?> salesReport = (Map<?, ?>) reportData.getSalesReport();
            Object topProductsObj = salesReport.get("top_products");
            if (topProductsObj instanceof List) {
                return (List<Map<String, Object>>) topProductsObj;
            }
        }
        return null;
    }

    // --- MODIFIED METHOD (if you decide to re-enable low stock details) ---
    private void addLowStockDetails(Document document, FullReportResponse reportData, PdfFont boldFont, PdfFont font, Locale locale) {
        try {
            document.add(new Paragraph(t("pdf.report.lowStockDetails", locale)) // "Low Stock Details"
                    .setFont(boldFont)
                    .setFontSize(18)
                    .setFontColor(ColorConstants.BLACK)
                    .setMarginBottom(10));

            List<Map<String, Object>> lowStockProducts = extractLowStockProducts(reportData);

            if (lowStockProducts != null && !lowStockProducts.isEmpty()) {
                logger.debug("Displaying {} low stock products in PDF", lowStockProducts.size());

                Table table = new Table(UnitValue.createPercentArray(new float[]{3, 2, 2, 2}))
                        .useAllAvailableWidth()
                        .setMarginBottom(20);

                // Header row
                table.addHeaderCell(createHeaderCell(t("pdf.report.productName", locale), boldFont)); // "Product Name"
                table.addHeaderCell(createHeaderCell(t("pdf.report.category", locale), boldFont)); // "Category"
                table.addHeaderCell(createHeaderCell(t("pdf.report.remainingQty", locale), boldFont)); // "Remaining Qty"
                table.addHeaderCell(createHeaderCell(t("pdf.report.reorderLevel", locale), boldFont)); // "Re-order Level"

                for (Map<String, Object> product : lowStockProducts) {
                    // Extract data with multiple fallback keys
                    String name = getStringValue(product.get("name"));
                    if (name.equals("N/A")) name = getStringValue(product.get("productName"));

                    String category = getStringValue(product.get("category"));
                    if (category.equals("N/A")) category = getStringValue(product.get("productCategory"));

                    int quantity = getIntValue(product.get("quantity"));
                    if (quantity == 0) quantity = getIntValue(product.get("stock"));
                    if (quantity == 0) quantity = getIntValue(product.get("remainingQuantity"));

                    int reorderLevel = getIntValue(product.get("reorderLevel"));
                    if (reorderLevel == 0) reorderLevel = getIntValue(product.get("reorderLevel"));
                    if (reorderLevel == 0) reorderLevel = getIntValue(product.get("reorder_level"));

                    table.addCell(createBodyCell(name, font));
                    table.addCell(createBodyCell(category, font));
                    table.addCell(createBodyCell(String.valueOf(quantity), font));
                    table.addCell(createBodyCell(String.valueOf(reorderLevel), font));
                }
                document.add(table);
            } else {
                logger.debug("No low stock products to display");
                Paragraph noData = new Paragraph(t("pdf.report.noLowStock", locale)) // "No low stock items to display"
                        .setFont(font)
                        .setFontSize(11)
                        .setFontColor(ColorConstants.DARK_GRAY)
                        .setItalic()
                        .setMarginBottom(20);
                document.add(noData);
            }
        } catch (Exception e) {
            logger.error("Error in addLowStockDetails: {}", e.getMessage(), e);

            Paragraph errorMsg = new Paragraph(t("pdf.report.lowStockError", locale) + e.getMessage()) // "Error loading low stock details: "
                    .setFont(font)
                    .setFontSize(10)
                    .setFontColor(ColorConstants.RED)
                    .setItalic()
                    .setMarginBottom(20);
            document.add(errorMsg);
        }
    }

    private List<Map<String, Object>> extractLowStockProducts(FullReportResponse reportData) {
        try {
            if (reportData == null) {
                logger.debug("Report data is null");
                return null;
            }

            logger.debug("=== DEEP INVENTORY REPORT ANALYSIS ===");

            // Method 1: Direct extraction from inventory report
            if (reportData.getInventoryReport() != null) {
                Map<String, Object> inventoryReport = reportData.getInventoryReport();
                logger.debug("Inventory Report Type: {}", inventoryReport.getClass().getSimpleName());
                logger.debug("All Inventory Report Keys: {}", inventoryReport.keySet());

                // Print ALL keys and their types
                for (Map.Entry<String, Object> entry : inventoryReport.entrySet()) {
                    logger.debug("Key: '{}', Type: {}, Value: {}",
                            entry.getKey(),
                            entry.getValue() != null ? entry.getValue().getClass().getSimpleName() : "null",
                            entry.getValue() instanceof List ? "List with " + ((List<?>) entry.getValue()).size() + " items" : entry.getValue());
                }

                // Try EVERY possible key pattern
                String[] allPossibleKeys = {
                        "lowStockProducts", "lowStockItems", "low_stock_products",
                        "lowStockList", "lowStockDetails", "low_stock_items",
                        "lowStock", "low_stock", "lowStockProductsList",
                        "products", "items", "inventoryItems", "stockItems",
                        "lowStockData", "low_stock_data", "lowInventory"
                };

                for (String key : allPossibleKeys) {
                    if (inventoryReport.containsKey(key)) {
                        Object obj = inventoryReport.get(key);
                        logger.debug("Found potential data with key '{}': {}", key, obj);

                        if (obj instanceof List) {
                            List<?> list = (List<?>) obj;
                            logger.debug("List with key '{}' has {} items", key, list.size());
                            if (!list.isEmpty()) {
                                logger.debug("First item in list: {}", list.get(0));
                                if (list.get(0) instanceof Map) {
                                    Map<?, ?> firstItem = (Map<?, ?>) list.get(0);
                                    logger.debug("First item keys: {}", firstItem.keySet());
                                }
                            }

                            List<Map<String, Object>> result = convertToListOfMaps(list);
                            if (!result.isEmpty()) {
                                logger.debug("SUCCESS: Found low stock data with key '{}', {} items", key, result.size());
                                return result;
                            }
                        }
                    }
                }

                // Method 2: Look for ANY list that contains product-like objects
                logger.debug("=== SEARCHING FOR ANY PRODUCT-LIKE LISTS ===");
                for (Map.Entry<String, Object> entry : inventoryReport.entrySet()) {
                    if (entry.getValue() instanceof List) {
                        List<?> list = (List<?>) entry.getValue();
                        if (!list.isEmpty() && list.get(0) instanceof Map) {
                            Map<?, ?> firstItem = (Map<?, ?>) list.get(0);
                            logger.debug("Found list with key '{}', first item keys: {}", entry.getKey(), firstItem.keySet());

                            // Check if this looks like product data
                            if (hasProductFields(firstItem)) {
                                List<Map<String, Object>> result = convertToListOfMaps(list);
                                logger.debug("SUCCESS: Found product data with key '{}', {} items", entry.getKey(), result.size());
                                return result;
                            }
                        }
                    }
                }
            }

            // Method 3: Check if low stock data is at the root level
            logger.debug("=== CHECKING ROOT LEVEL ===");
            if (reportData instanceof Map) {
                Map<?, ?> rootMap = (Map<?, ?>) reportData;
                logger.debug("Root level keys: {}", rootMap.keySet());

                for (String key : new String[]{"lowStockProducts", "lowStockItems"}) {
                    if (rootMap.containsKey(key)) {
                        Object obj = rootMap.get(key);
                        if (obj instanceof List) {
                            List<Map<String, Object>> result = convertToListOfMaps((List<?>) obj);
                            logger.debug("SUCCESS: Found low stock data at root level with key '{}', {} items", key, result.size());
                            return result;
                        }
                    }
                }
            }

            logger.debug("FAILED: No low stock products found after exhaustive search");
            return null;

        } catch (Exception e) {
            logger.error("Error extracting low stock products: {}", e.getMessage(), e);
            return null;
        }
    }

    // Helper method to convert any list to List<Map<String, Object>>
    private List<Map<String, Object>> convertToListOfMaps(List<?> rawList) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (rawList != null) {
            for (Object item : rawList) {
                if (item instanceof Map) {
                    result.add((Map<String, Object>) item);
                }
            }
        }
        return result;
    }

    // Helper method to check if a map has product-like fields
    private boolean hasProductFields(Map<?, ?> item) {
        Set<?> keys = item.keySet();
        return keys.contains("name") || keys.contains("productName") ||
                keys.contains("quantity") || keys.contains("stock") ||
                keys.contains("reorderLevel") || keys.contains("category");
    }

    private String determineStockStatus(int quantity, int reorderLevel) {
        if (quantity <= 0) return "Out of Stock";
        if (quantity <= 2) return "Critical";
        if (reorderLevel > 0 && quantity <= reorderLevel) return "Below Reorder Level";
        return "Low Stock";
    }

    private Cell createMetricCell(String title, String value, PdfFont boldFont, PdfFont font,
                                  com.itextpdf.kernel.colors.Color titleColor, com.itextpdf.kernel.colors.Color valueColor) {
        Cell cell = new Cell();
        cell.setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 1f))
                .setPadding(15)
                .setBackgroundColor(ColorConstants.WHITE)
                .add(new Paragraph(title)
                        .setFont(font).setFontSize(11)
                        .setFontColor(titleColor)
                        .setTextAlignment(TextAlignment.CENTER))
                .add(new Paragraph(value)
                        .setFont(boldFont).setFontSize(20)
                        .setFontColor(valueColor)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginTop(5));
        return cell;
    }

    private Cell createHeaderCell(String text, PdfFont font) {
        Cell cell = new Cell();
        cell.add(new Paragraph(text))
                .setFont(font).setFontSize(11)
                .setFontColor(ColorConstants.WHITE)
                .setBackgroundColor(ColorConstants.DARK_GRAY)
                .setPadding(8)
                .setTextAlignment(TextAlignment.LEFT);
        return cell;
    }

    private Cell createBodyCell(String text, PdfFont font) {
        Cell cell = new Cell();
        cell.add(new Paragraph(text))
                .setFont(font).setFontSize(10)
                .setFontColor(ColorConstants.BLACK)
                .setPadding(8)
                .setTextAlignment(TextAlignment.LEFT);
        return cell;
    }

    // Helper method to convert hex color to iText Color
    private com.itextpdf.kernel.colors.Color getColorFromHex(String hexColor) {
        try {
            if (hexColor.startsWith("#")) {
                hexColor = hexColor.substring(1);
            }
            int r = Integer.parseInt(hexColor.substring(0, 2), 16);
            int g = Integer.parseInt(hexColor.substring(2, 4), 16);
            int b = Integer.parseInt(hexColor.substring(4, 6), 16);
            return new com.itextpdf.kernel.colors.DeviceRgb(r, g, b);
        } catch (Exception e) {
            return ColorConstants.BLACK;
        }
    }

    // Helper methods for safe type conversion
    private double getDoubleValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }

    private int getIntValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    private String getStringValue(Object value) {
        if (value instanceof String) {
            return (String) value;
        }
        return value != null ? value.toString() : "N/A";
    }
}