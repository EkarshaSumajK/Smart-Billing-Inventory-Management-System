// MongoDB Migration Script to Assign Shop to Legacy User
// Run this in MongoDB Compass or mongosh

// Step 1: Create a shop for the legacy user
const legacyUserEmail = "test@shop.com";
const shopName = "Legacy Shop"; // You can change this name

// Find the user
const user = db.users.findOne({ email: legacyUserEmail });

if (!user) {
    print("User not found!");
} else {
    print("Found user: " + user.email);

    // Create a new shop
    const newShop = {
        _id: ObjectId(),
        name: shopName,
        ownerId: user._id,
        createdAt: new Date()
    };

    db.shops.insertOne(newShop);
    print("Created shop: " + newShop.name + " with ID: " + newShop._id);

    // Update the user with shopId
    db.users.updateOne(
        { _id: user._id },
        { $set: { shopId: newShop._id.toString() } }
    );
    print("Updated user with shopId");

    // Update all products created by this user
    const productsUpdated = db.products.updateMany(
        { addedBy: user.email },
        { $set: { shopId: newShop._id.toString() } }
    );
    print("Updated " + productsUpdated.modifiedCount + " products");

    // Update all bills created by this user
    const billsUpdated = db.bills.updateMany(
        { addedBy: user.email },
        { $set: { shopId: newShop._id.toString() } }
    );
    print("Updated " + billsUpdated.modifiedCount + " bills");

    // Update all customers
    const customersUpdated = db.customers.updateMany(
        { addedBy: user.email },
        { $set: { shopId: newShop._id.toString() } }
    );
    print("Updated " + customersUpdated.modifiedCount + " customers");

    // Update all payments
    const paymentsUpdated = db.payments.updateMany(
        {},
        { $set: { shopId: newShop._id.toString() } }
    );
    print("Updated " + paymentsUpdated.modifiedCount + " payments");

    // Update all notifications
    const notificationsUpdated = db.notifications.updateMany(
        {},
        { $set: { shopId: newShop._id.toString() } }
    );
    print("Updated " + notificationsUpdated.modifiedCount + " notifications");

    // Update all audit logs
    const auditLogsUpdated = db.auditLogs.updateMany(
        { userEmail: user.email },
        { $set: { shopId: newShop._id.toString() } }
    );
    print("Updated " + auditLogsUpdated.modifiedCount + " audit logs");

    // Update all push subscriptions
    const pushSubsUpdated = db.pushSubscriptions.updateMany(
        { userId: user._id.toString() },
        { $set: { shopId: newShop._id.toString() } }
    );
    print("Updated " + pushSubsUpdated.modifiedCount + " push subscriptions");

    print("\n=== Migration Complete! ===");
    print("Shop ID: " + newShop._id.toString());
    print("Shop Name: " + newShop.name);
    print("User can now login with: " + user.email);
}
