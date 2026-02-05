// Check the EXACT structure of the user document in MongoDB
// Run this in mongosh to see the raw document

print("=== Raw User Document ===");
var user = db.users.findOne({ email: "test@shop.com" });
printjson(user);

print("\n=== Field Types ===");
print("shopId field exists: " + ("shopId" in user));
print("shopId value: " + user.shopId);
print("shopId type: " + typeof user.shopId);
print("shopId length: " + (user.shopId ? user.shopId.length : "N/A"));

print("\n=== All Fields ===");
Object.keys(user).forEach(function (key) {
    print(key + ": " + typeof user[key] + " = " + user[key]);
});
