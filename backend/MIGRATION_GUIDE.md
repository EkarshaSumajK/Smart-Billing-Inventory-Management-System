# Legacy User Migration Guide

## Problem
User `test@shop.com` was created before multi-tenancy implementation and doesn't have a `shopId`, preventing login.

## Solution
Run the MongoDB migration script to:
1. Create a shop for the legacy user
2. Assign the shop to the user
3. Update all existing data (products, bills, customers, etc.) with the new `shopId`

---

## Steps to Migrate

### Option 1: Using MongoDB Compass (GUI)

1. Open **MongoDB Compass**
2. Connect to your database
3. Click on your database name (likely `smart_retail` or similar)
4. Click the **">"** button (Mongosh) at the bottom to open the shell
5. Copy and paste the entire content from `migrate_legacy_user.js`
6. Press **Enter** to execute
7. You should see output like:
   ```
   Found user: test@shop.com
   Created shop: Legacy Shop with ID: 507f1f77bcf86cd799439011
   Updated user with shopId
   Updated X products
   Updated X bills
   Updated X customers
   ...
   === Migration Complete! ===
   ```

### Option 2: Using mongosh (Command Line)

1. Open terminal/command prompt
2. Connect to MongoDB:
   ```bash
   mongosh mongodb://localhost:27017/your_database_name
   ```
3. Load and run the script:
   ```bash
   load('g:/FINALCODE/backend/migrate_legacy_user.js')
   ```

### Option 3: Direct Execution

1. Open terminal
2. Run:
   ```bash
   mongosh mongodb://localhost:27017/your_database_name < g:/FINALCODE/backend/migrate_legacy_user.js
   ```

---

## After Migration

1. **Try logging in** with `test@shop.com` - it should work now!
2. **Verify data**: All your previous products, bills, and customers should be visible
3. **Check navbar**: Should display "Legacy Shop" (or whatever name you set in the script)

---

## Customization

If you want to change the shop name, edit line 5 in `migrate_legacy_user.js`:
```javascript
const shopName = "Your Custom Shop Name"; // Change this
```

---

## What Gets Updated

The script updates the following collections with `shopId`:
- ✅ `users` - Assigns shopId to the user
- ✅ `shops` - Creates new shop entry
- ✅ `products` - All products created by this user
- ✅ `bills` - All bills created by this user
- ✅ `customers` - All customers added by this user
- ✅ `payments` - All payments
- ✅ `notifications` - All notifications
- ✅ `auditLogs` - All audit logs for this user
- ✅ `pushSubscriptions` - All push subscriptions for this user

---

## Troubleshooting

**Issue**: "User not found!"
- **Fix**: Check the email in line 4 of the script matches exactly

**Issue**: Script runs but login still fails
- **Fix**: Restart the backend server to clear any caches

**Issue**: Data not showing after migration
- **Fix**: Check MongoDB to verify `shopId` was added to documents
