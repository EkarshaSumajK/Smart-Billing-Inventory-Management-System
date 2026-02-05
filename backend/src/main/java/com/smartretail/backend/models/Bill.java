package com.smartretail.backend.models;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
@Document(collection = "bills")
public class Bill {

    @Id
    private String billId;
    private CustomerInfo customer;
    private List<BillItem> items;
    private double totalAmount;
    private Date createdAt;
    private String pdfAccessToken;
    private String addedBy;
    private String shopId;

    // Manual getters and setters
    public String getBillId() {
        return billId;
    }

    public void setBillId(String billId) {
        this.billId = billId;
    }

    public CustomerInfo getCustomer() {
        return customer;
    }

    public void setCustomer(CustomerInfo customer) {
        this.customer = customer;
    }

    public List<BillItem> getItems() {
        return items;
    }
    // setItems is already defined below

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getPdfAccessToken() {
        return pdfAccessToken;
    }

    public void setPdfAccessToken(String pdfAccessToken) {
        this.pdfAccessToken = pdfAccessToken;
    }

    public String getAddedBy() {
        return addedBy;
    }

    public void setAddedBy(String addedBy) {
        this.addedBy = addedBy;
    }

    public String getShopId() {
        return shopId;
    }

    public void setShopId(String shopId) {
        this.shopId = shopId;
    }

    public Bill() {
        ensureDefaults();
    }

    public Bill(String billId, List<BillItem> items, String addedBy, String shopId) {
        ensureDefaults();
        this.billId = billId;
        this.items = items;
        this.addedBy = addedBy;
        this.shopId = shopId;
        this.totalAmount = calculateTotal();
    }

    public Bill(String billId, CustomerInfo customer, List<BillItem> items,
            double totalAmount, String addedBy, String shopId) {
        this(billId, items, addedBy, shopId);
        this.customer = customer;
        this.totalAmount = totalAmount;
    }

    // Ensure defaults are set every time (constructor OR deserialization)
    private void ensureDefaults() {
        if (this.createdAt == null)
            this.createdAt = new Date();
        if (this.pdfAccessToken == null || this.pdfAccessToken.isEmpty())
            this.pdfAccessToken = UUID.randomUUID().toString();
    }

    public static class BillItem {
        private String productId;
        private String productName;
        private int qty;
        private double price;

        public BillItem() {
        }

        public BillItem(String productId, String productName, int qty, double price) {
            this.productId = productId;
            this.productName = productName;
            this.qty = qty;
            this.price = price;
        }

        public double getItemTotal() {
            return this.qty * this.price;
        }

        // Manual getters and setters for BillItem
        public String getProductId() {
            return productId;
        }

        public void setProductId(String productId) {
            this.productId = productId;
        }

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }

        public int getQty() {
            return qty;
        }

        public void setQty(int qty) {
            this.qty = qty;
        }

        public double getPrice() {
            return price;
        }

        public void setPrice(double price) {
            this.price = price;
        }
    }

    public static class CustomerInfo {
        private String name;
        private String email;
        private String mobile;

        public CustomerInfo() {
        }

        public CustomerInfo(String name, String email, String mobile) {
            this.name = name;
            this.email = email;
            this.mobile = mobile;
        }

        public boolean isValid() {
            return this.mobile != null && !this.mobile.trim().isEmpty();
        }

        // Manual getters and setters for CustomerInfo
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getMobile() {
            return mobile;
        }

        public void setMobile(String mobile) {
            this.mobile = mobile;
        }
    }

    public boolean isValid() {
        return this.billId != null &&
                !this.billId.trim().isEmpty() &&
                this.items != null &&
                !this.items.isEmpty() &&
                this.addedBy != null &&
                !this.addedBy.trim().isEmpty();
    }

    public double calculateTotal() {
        if (this.items == null || this.items.isEmpty()) {
            return 0.0;
        }
        return this.items.stream()
                .mapToDouble(BillItem::getItemTotal)
                .sum();
    }

    // Automatically recalculate when items are updated
    public void setItems(List<BillItem> items) {
        this.items = items;
        this.totalAmount = calculateTotal();
    }
}
