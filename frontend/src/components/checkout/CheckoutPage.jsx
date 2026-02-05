import React, { useState } from 'react';
import { useCart } from '../../context/CartContext';
import { createBill } from '../../services/billService';
import authService from '../../services/authService';
import { useNavigate } from 'react-router-dom';
import { useToast } from '../../context/ToastContext';
import { useTranslation } from 'react-i18next';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';
import { ShoppingCart, Trash2, Receipt, User, Mail, Phone, Package, Sparkles } from 'lucide-react';
import { motion } from 'framer-motion';

export default function CheckoutPage() {
  const { cartItems, cartTotal, updateQuantity, removeFromCart, clearCart } = useCart();
  const { showToast } = useToast();
  const { t } = useTranslation();

  const [customerInfo, setCustomerInfo] = useState({
    name: '',
    email: '',
    mobile: ''
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const user = authService.getUserFromToken();
  const navigate = useNavigate();

  const handleCustomerChange = (e) => {
    const { name, value } = e.target;
    setCustomerInfo(prev => ({ ...prev, [name]: value }));
  };

  const handleGenerateBill = async () => {
    if (!customerInfo.mobile) {
      const msg = t('checkout.errorMobile');
      setError(msg);
      showToast(msg, 'error');
      return;
    }
    if (cartItems.length === 0) {
      const msg = t('checkout.errorEmptyCart');
      setError(msg);
      showToast(msg, 'error');
      return;
    }

    setLoading(true);
    setError(null);

    const billRequest = {
      billId: `B${Date.now()}`,
      customer: {
        name: customerInfo.name || 'Customer',
        email: customerInfo.email,
        mobile: customerInfo.mobile,
      },
      items: cartItems.map(item => ({
        productId: item.productId,
        productName: item.name,
        qty: item.quantity,
        price: item.price,
      })),
      addedBy: user?.email || 'system',
    };

    try {
      const newBill = await createBill(billRequest);
      showToast(t('checkout.billSuccess', { billId: newBill.billId }), 'success');
      clearCart();
      setCustomerInfo({ name: '', email: '', mobile: '' });
      navigate('/bills');
    } catch (err) {
      const errMsg = err.response?.data?.error || err.message;
      setError(t('checkout.billError', { error: errMsg }));
      showToast(t('checkout.billError', { error: errMsg }), 'error');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-12">
      {/* HEADER */}
      <div className="page-hero">
        <div className="page-hero-content">
          <div>
            <p className="page-hero-eyebrow">Checkout</p>
            <h1 className="page-hero-title">{t('checkout.title')}</h1>
            <p className="page-hero-subtitle">Complete your purchase and generate bill</p>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* CART ITEMS */}
        <div className="lg:col-span-2">
          <div className="page-section">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2">
                <Package className="h-5 w-5 text-apple-blue" />
                <h3 className="section-title">Cart Items</h3>
              </div>
              <Badge variant="secondary" className="bg-apple-blue text-white">
                {cartItems.length} {cartItems.length === 1 ? 'item' : 'items'}
              </Badge>
            </div>
            {error && (
              <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-100 px-4 py-3 rounded-xl mb-4">
                {error}
              </div>
            )}
            <div className="space-y-3">
              {cartItems.length === 0 ? (
                <div className="text-center py-12">
                  <ShoppingCart className="h-16 w-16 text-apple-gray-300 dark:text-apple-gray-700 mx-auto mb-4" />
                  <p className="text-apple-gray-500 dark:text-apple-gray-400">{t('checkout.empty')}</p>
                </div>
              ) : (
                cartItems.map((item, index) => (
                  <motion.div
                    key={item.productId}
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: index * 0.05 }}
                    className="flex items-center gap-4 p-4 bg-apple-gray-50 dark:bg-apple-gray-900/50 rounded-2xl hover:bg-apple-gray-100 dark:hover:bg-apple-gray-900 transition-colors border border-apple-gray-200 dark:border-apple-gray-800"
                  >
                    <div className="flex-grow">
                      <p className="font-semibold text-apple-gray-900 dark:text-white">{item.name}</p>
                      <p className="text-sm text-apple-gray-500 dark:text-apple-gray-400">
                        {item.quantity} × {item.price.toLocaleString('en-IN', { style: 'currency', currency: 'INR' })}
                      </p>
                    </div>
                    <Input
                      type="number"
                      value={item.quantity}
                      onChange={(e) => updateQuantity(item.productId, parseInt(e.target.value))}
                      min="1"
                      className="w-20 text-center h-10 rounded-2xl border-apple-gray-300"
                    />
                    <p className="text-lg font-semibold text-apple-blue w-28 text-right">
                      {(item.quantity * item.price).toLocaleString('en-IN', { style: 'currency', currency: 'INR' })}
                    </p>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => removeFromCart(item.productId)}
                      className="text-red-500 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-2xl"
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </motion.div>
                ))
              )}
            </div>
          </div>
        </div>

        {/* SUMMARY & CUSTOMER INFO */}
        <div className="lg:col-span-1">
          <div className="page-section sticky top-6">
            <div className="flex items-center gap-2 mb-4">
              <Receipt className="h-5 w-5 text-apple-blue" />
              <h3 className="section-title">Summary</h3>
            </div>
            <div className="space-y-6">
              {/* TOTAL */}
              <div className="space-y-3">
                <div className="flex justify-between text-apple-gray-700 dark:text-apple-gray-300">
                  <span>{t('checkout.subtotal')}</span>
                  <span className="font-semibold">{cartTotal.toLocaleString('en-IN', { style: 'currency', currency: 'INR' })}</span>
                </div>
                <div className="flex justify-between text-apple-gray-700 dark:text-apple-gray-300">
                  <span>{t('checkout.taxes')}</span>
                  <span className="font-semibold">{t('checkout.taxesValue')}</span>
                </div>
                <Separator />
                <div className="flex justify-between text-apple-gray-900 dark:text-white text-xl font-bold">
                  <span>{t('checkout.total')}</span>
                  <span className="text-apple-blue">{cartTotal.toLocaleString('en-IN', { style: 'currency', currency: 'INR' })}</span>
                </div>
              </div>

              <Separator />

              {/* CUSTOMER DETAILS */}
              <div className="space-y-4">
                <div className="flex items-center gap-2">
                  <User className="h-4 w-4 text-apple-blue" />
                  <Label className="text-sm font-bold text-apple-gray-700 dark:text-apple-gray-300">
                    {t('checkout.customerDetails')}
                  </Label>
                </div>

                <div className="space-y-3">
                  <div>
                    <Label htmlFor="name" className="text-xs text-apple-gray-600 dark:text-apple-gray-400 mb-1.5 block">
                      Customer Name
                    </Label>
                    <div className="relative">
                      <User className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-apple-gray-400" />
                      <Input
                        id="name"
                        type="text"
                        name="name"
                        value={customerInfo.name}
                        onChange={handleCustomerChange}
                        placeholder={t('checkout.customerName')}
                        className="pl-10 h-12 rounded-2xl border-apple-gray-300 focus:border-apple-blue focus:ring-apple-blue"
                      />
                    </div>
                  </div>

                  <div>
                    <Label htmlFor="mobile" className="text-xs text-apple-gray-600 dark:text-apple-gray-400 mb-1.5 block">
                      Mobile * (Required)
                    </Label>
                    <div className="relative">
                      <Phone className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-apple-gray-400" />
                      <Input
                        id="mobile"
                        type="text"
                        name="mobile"
                        value={customerInfo.mobile}
                        onChange={handleCustomerChange}
                        placeholder={t('checkout.customerMobile')}
                        className="pl-10 h-12 rounded-2xl border-apple-gray-300 focus:border-apple-blue focus:ring-apple-blue"
                        required
                      />
                    </div>
                  </div>

                  <div>
                    <Label htmlFor="email" className="text-xs text-apple-gray-600 dark:text-apple-gray-400 mb-1.5 block">
                      Email (Optional)
                    </Label>
                    <div className="relative">
                      <Mail className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-apple-gray-400" />
                      <Input
                        id="email"
                        type="email"
                        name="email"
                        value={customerInfo.email}
                        onChange={handleCustomerChange}
                        placeholder={t('checkout.customerEmail')}
                        className="pl-10 h-12 rounded-2xl border-apple-gray-300 focus:border-apple-blue focus:ring-apple-blue"
                      />
                    </div>
                  </div>
                </div>
              </div>

              {/* GENERATE BILL BUTTON */}
              <Button
                onClick={handleGenerateBill}
                disabled={loading || cartItems.length === 0 || !customerInfo.mobile}
                className="w-full h-14 rounded-full bg-apple-blue hover:bg-apple-blue-light text-white text-lg font-semibold shadow-apple-lg transition-all"
              >
                {loading ? (
                  <div className="flex items-center gap-2">
                    <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white"></div>
                    {t('checkout.generatingBill')}
                  </div>
                ) : (
                  <div className="flex items-center gap-2">
                    <Sparkles className="h-5 w-5" />
                    {t('checkout.generateBill')}
                  </div>
                )}
              </Button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}