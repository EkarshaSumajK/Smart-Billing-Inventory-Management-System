package com.smartretail.backend.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.smartretail.backend.models.Bill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.ByteArrayOutputStream;
import java.util.Locale;

// --- NEW IMPORT ---
import org.springframework.context.MessageSource;
// --- END NEW IMPORT ---

@Service
public class PdfServiceImpl implements PdfService {
    private static final Logger logger = LoggerFactory.getLogger(PdfServiceImpl.class);

    // --- NEW FIELD ---
    private final MessageSource messageSource;

    // --- MODIFIED CONSTRUCTOR ---
    public PdfServiceImpl(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    // --- Helper method for translation ---
    private String t(String key, Locale locale) {
        // Fallback to the key itself if not found, to avoid errors
        return messageSource.getMessage(key, null, key, locale);
    }

    @Override
    public byte[] generateBillPdf(Bill bill, Locale locale) {
        if (bill == null) {
            throw new IllegalArgumentException("Bill cannot be null");
        }

       // ... (logging remains the same) [cite: 174]

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            PdfFont font = PdfFontFactory.createFont("Helvetica");
            PdfFont boldFont = PdfFontFactory.createFont("Helvetica-Bold");

            // Header - MODIFIED
            addHeader(document, boldFont, font, locale);

            // Bill details - MODIFIED
            addBillDetails(document, bill, boldFont, font, locale);

            // Items table - MODIFIED
            addItemsTable(document, bill, boldFont, font, locale);

            // Total - MODIFIED
            addTotal(document, bill, boldFont, locale);

            document.close();
           // ... (logging remains the same) [cite: 175]
            return outputStream.toByteArray();

        } catch (Exception e) {
            // ... (error logging remains the same) [cite: 175]
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    // --- MODIFIED METHOD ---
    private void addHeader(Document document, PdfFont boldFont, PdfFont font, Locale locale) {
        document.add(new Paragraph("SmartRetail")
                .setFont(boldFont).setFontSize(24).setTextAlignment(TextAlignment.CENTER));
        document.add(new Paragraph(t("pdf.bill.receiptTitle", locale)) // "Official Receipt"
                .setFont(font).setFontSize(14).setTextAlignment(TextAlignment.CENTER).setMarginBottom(20));
    }

    // --- MODIFIED METHOD ---
    private void addBillDetails(Document document, Bill bill, PdfFont boldFont, PdfFont font, Locale locale) {
        Table detailsTable = new Table(new float[]{1, 2});
        detailsTable.setWidth(UnitValue.createPercentValue(50));

        detailsTable.addCell(createCell(boldFont, t("pdf.bill.billId", locale) + ":", false)); // "Bill ID:"
        detailsTable.addCell(createCell(font, bill.getBillId(), false));

        if (bill.getCustomer() != null) {
            detailsTable.addCell(createCell(boldFont, t("pdf.bill.customer", locale) + ":", false)); // "Customer:"
            detailsTable.addCell(createCell(font,
                    bill.getCustomer().getName() != null ? bill.getCustomer().getName() : "N/A", false));

            detailsTable.addCell(createCell(boldFont, t("pdf.bill.mobile", locale) + ":", false)); // "Mobile:"
            detailsTable.addCell(createCell(font,
                    bill.getCustomer().getMobile() != null ? bill.getCustomer().getMobile() : "N/A", false));
        }

        document.add(detailsTable.setMarginBottom(20));
    }

    // --- MODIFIED METHOD ---
    private void addItemsTable(Document document, Bill bill, PdfFont boldFont, PdfFont font, Locale locale) {
        Table itemsTable = new Table(new float[]{3, 1, 1, 1});
        itemsTable.setWidth(UnitValue.createPercentValue(100));

        // Headers
        itemsTable.addHeaderCell(createCell(boldFont, t("pdf.bill.product", locale), true)); // "Product"
        itemsTable.addHeaderCell(createCell(boldFont, t("pdf.bill.qty", locale), true)); // "Qty"
        itemsTable.addHeaderCell(createCell(boldFont, t("pdf.bill.price", locale), true)); // "Price"
        itemsTable.addHeaderCell(createCell(boldFont, t("pdf.bill.subtotal", locale), true)); // "Subtotal"

        // ... (items loop remains the same) [cite: 177, 178]
        if (bill.getItems() != null) {
            for (Bill.BillItem item : bill.getItems()) {
                String productName = item.getProductName() != null ?
                        item.getProductName() : "Product " + item.getProductId();
                double itemPrice = item.getPrice();
                double subtotal = item.getQty() * itemPrice;
                itemsTable.addCell(createCell(font, productName, false));
                itemsTable.addCell(createCell(font, String.valueOf(item.getQty()), false));
                itemsTable.addCell(createCell(font, String.format("₹%.2f", itemPrice), false));
                itemsTable.addCell(createCell(font, String.format("₹%.2f", subtotal), false));
            }
        }
        document.add(itemsTable);
    }

    // --- MODIFIED METHOD ---
    private void addTotal(Document document, Bill bill, PdfFont boldFont, Locale locale) {
        String totalText = String.format("%s: ₹%.2f", t("pdf.bill.total", locale), bill.getTotalAmount()); // "Total:"
        document.add(new Paragraph(totalText)
                .setFont(boldFont).setFontSize(12)
                .setTextAlignment(TextAlignment.RIGHT).setMarginTop(10));
    }

   // ... (createCell method remains the same) [cite: 179]
    private Cell createCell(PdfFont font, String text, boolean isHeader) {
        Cell cell = new Cell().setFont(font).add(new Paragraph(text));
        if (isHeader) {
            cell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
        } else {
            cell.setBorder(Border.NO_BORDER);
        }
        return cell;
    }
}