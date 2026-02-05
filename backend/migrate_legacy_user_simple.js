// SIMPLIFIED Migration - Run this in mongosh
// This version uses simpler syntax that should work better

// 1. Find the user
var user = db.users.findOne({ email: "test@shop.com" });

if (!user) {
    print("ERROR: User not found!");
} else {
    print("Found user: " + user.email);

    // 2. Create shop ID as a string (MongoDB ObjectId as string)
    var shopId = new ObjectId().toString();
    print("Generated shopId: " + shopId);

    // 3. Create the shop document
    db.shops.insertOne({
        _id: ObjectId(shopId),
        name: "Legacy Shop",
        ownerId: user._id.toString(),
        createdAt: new Date()
    });
    print("Created shop");

    // 4. Update user with shopId (as STRING, not ObjectId)
    db.users.updateOne(
        { email: "test@shop.com" },
        { $set: { shopId: shopId } }
    );
    print("Updated user with shopId: " + shopId);

    // 5. Verify the update
    var updatedUser = db.users.findOne({ email: "test@shop.com" });
    print("User shopId after update: " + updatedUser.shopId);
    print("Type of shopId: " + typeof updatedUser.shopId);

    // 6. Update all related data
    db.products.updateMany(
        { addedBy: "test@shop.com" },
        { $set: { shopId: shopId } }
    );
    print("Updated products");

    db.bills.updateMany(
        { addedBy: "test@shop.com" },
        { $set: { shopId: shopId } }
    );
    print("Updated bills");

    db.customers.updateMany(
        { addedBy: "test@shop.com" },
        { $set: { shopId: shopId } }
    );
    print("Updated customers");

    print("\n=== MIGRATION COMPLETE ===");
    print("Shop ID: " + shopId);
    print("Try logging in now!");
}
