// Quick verification script - Run this in mongosh to check the migration status

print("=== Checking Migration Status ===\n");

// Check if user exists and has shopId
const user = db.users.findOne({ email: "test@shop.com" });
if (!user) {
    print("❌ User not found!");
} else {
    print("✅ User found: " + user.email);
    print("   User ID: " + user._id);
    print("   Shop ID: " + (user.shopId || "NULL - NOT ASSIGNED!"));
    print("   Role: " + user.role);
}

print("\n");

// Check if shop exists
const shopCount = db.shops.countDocuments({});
print("Total shops in database: " + shopCount);

if (user && user.shopId) {
    const shop = db.shops.findOne({ _id: ObjectId(user.shopId) });
    if (shop) {
        print("✅ Shop found: " + shop.name);
        print("   Shop ID: " + shop._id);
        print("   Owner ID: " + shop.ownerId);
    } else {
        print("❌ Shop not found for shopId: " + user.shopId);
    }
}

print("\n");

// Check products
const productCount = db.products.countDocuments({ addedBy: "test@shop.com" });
print("Products by test@shop.com: " + productCount);
if (productCount > 0) {
    const sampleProduct = db.products.findOne({ addedBy: "test@shop.com" });
    print("   Sample product shopId: " + (sampleProduct.shopId || "NULL"));
}

print("\n");

// Check bills
const billCount = db.bills.countDocuments({ addedBy: "test@shop.com" });
print("Bills by test@shop.com: " + billCount);
if (billCount > 0) {
    const sampleBill = db.bills.findOne({ addedBy: "test@shop.com" });
    print("   Sample bill shopId: " + (sampleBill.shopId || "NULL"));
}

print("\n=== End of Check ===");
