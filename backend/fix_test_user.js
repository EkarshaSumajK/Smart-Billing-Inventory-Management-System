// FINAL FIX - Direct MongoDB Commands
// Copy and paste these commands ONE BY ONE in mongosh

// 1. First, let's see the current state of test@shop.com user
db.users.findOne({ email: "test@shop.com" })

// 2. If the above shows shopId is null or missing, run this:
db.users.updateOne(
    { email: "test@shop.com" },
    {
        $set: {
            shopId: "691eda7e988b0a73f737d62e"  // Use the shopId from your migration output
        }
    }
)

// 3. Verify the update worked:
db.users.findOne({ email: "test@shop.com" })

// 4. Check if the shop exists:
db.shops.findOne({ _id: ObjectId("691eda7e988b0a73f737d62e") })

// 5. If shop doesn't exist, create it:
db.shops.insertOne({
    _id: ObjectId("691eda7e988b0a73f737d62e"),
    name: "Legacy Shop - test@shop.com",
    ownerId: "68bf47c7b3cff68e048c3c77",  // Replace with actual user._id from step 1
    createdAt: new Date()
})

// After running these, restart the backend and try logging in
