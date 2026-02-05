import React, { useEffect, useState, useCallback } from 'react';
import { createBill, getSuggestions } from '../../services/billService';
import { listCustomers } from '../../services/customerService';
import { listProducts } from '../../services/productService';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useToast } from '../../context/ToastContext';

export default function BillForm() {
  const { t } = useTranslation();
  const { showToast } = useToast();
  const [customers, setCustomers] = useState([]);
  const [products, setProducts] = useState([]);
  const [selectedCustomer, setSelectedCustomer] = useState('');
  const [items, setItems] = useState([{ productId: '', quantity: 1 }]);
  const [total, setTotal] = useState(0);

  // Suggestion State
  const [suggestions, setSuggestions] = useState([]);
  const [loadingSuggestions, setLoadingSuggestions] = useState(false);

  const nav = useNavigate();

  useEffect(() => {
    (async () => {
      try {
        const [custs, prods] = await Promise.all([listCustomers(), listProducts()]);
        setCustomers(custs);
        setProducts(prods);
      } catch (err) {
        showToast("Failed to load customers or products", 'error');
      }
    })();
  }, [showToast]);

  useEffect(() => {
    let t = 0;
    for (const item of items) {
      const prod = products.find(p => p.productId === item.productId);
      if (prod) t += prod.price * item.quantity;
    }
    setTotal(t);
    setTotal(t);
  }, [items, products]);

  // --- REAL-TIME RECOMMENDATIONS (Debounced) ---
  useEffect(() => {
    const fetchRecs = async () => {
      // Extract valid product IDs
      const productIds = items.map(i => i.productId).filter(id => id);

      if (productIds.length === 0) {
        setSuggestions([]);
        return;
      }

      try {
        setLoadingSuggestions(true);
        const recs = await getSuggestions(productIds);
        setSuggestions(recs);
      } catch (e) {
        console.error("Failed to get suggestions", e);
      } finally {
        setLoadingSuggestions(false);
      }
    };

    const debounceId = setTimeout(fetchRecs, 500); // 500ms debounce
    return () => clearTimeout(debounceId);
  }, [items]); // Dep: items array (deep check implies rapid firing, but debounce handles it)

  function addItem() {
    setItems([...items, { productId: '', quantity: 1 }]);
  }

  function updateItem(index, field, value) {
    const updated = [...items];
    updated[index][field] = value;
    setItems(updated);
  }

  function removeItem(index) {
    const updated = [...items];
    updated.splice(index, 1);
    setItems(updated);
  }

  async function handleSubmit(e) {
    e.preventDefault();
    const selected = customers.find(
      c => c.customerId === selectedCustomer || c.id === selectedCustomer || c._id === selectedCustomer
    );

    if (!selected) {
      showToast(t('billForm.errorCustomer'), 'error');
      return;
    }

    try {
      const billRequest = {
        billId: 'B' + Date.now(),
        customer: {
          name: selected.name,
          email: selected.email,
          mobile: selected.mobile || selected.phone || selected.contactNumber,
        },
        items: items.map(it => ({
          productId: it.productId,
          qty: parseInt(it.quantity, 10),
        })),
        addedBy: 'system',
      };

      await createBill(billRequest);
      showToast(t('billForm.success'), 'success');
      nav('/bills');
    } catch (err) {
      const msg = err.response?.data?.error || err.response?.data?.message || t('billForm.errorCreate');
      showToast(msg, 'error');
    }
  }

  return (
    <div className="space-y-12">
      <div className="page-hero max-w-3xl mx-auto">
        <div className="page-hero-content">
          <div>
            <p className="page-hero-eyebrow">Billing</p>
            <h2 className="page-hero-title">{t('billForm.title')}</h2>
            <p className="page-hero-subtitle">Create a new bill for a customer</p>
          </div>
        </div>
      </div>

      <div className="page-section max-w-3xl mx-auto">
        <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="block mb-1.5 font-medium text-apple-gray-600 dark:text-apple-gray-400">{t('billForm.customer')}</label>
          <select
            value={selectedCustomer}
            onChange={e => setSelectedCustomer(e.target.value)}
            className="form-input-no-icon w-full"
            required
          >
            <option value="">{t('billForm.selectCustomer')}</option>
            {customers.map((c, index) => (
              <option
                key={c.customerId || c.id || c._id || index}
                value={c.customerId || c.id || c._id}
              >
                {c.name} ({c.email || c.mobile})
              </option>
            ))}
          </select>
        </div>

        <div>
          <label className="block mb-2 font-medium text-apple-gray-600 dark:text-apple-gray-400">{t('billForm.items')}</label>
          <div className="space-y-2">
            {items.map((item, index) => (
              <div key={index} className="flex gap-2 items-center">
                <select
                  value={item.productId}
                  onChange={e => updateItem(index, 'productId', e.target.value)}
                  className="form-input-no-icon flex-1"
                  required
                >
                  <option value="">{t('billForm.selectProduct')}</option>
                  {products.map((p, i) => (
                    <option
                      key={p.productId || p.id || i}
                      value={p.productId || p.id}
                    >
                      {p.name} (₹{p.price})
                    </option>
                  ))}
                </select>
                <input
                  type="number"
                  min="1"
                  value={item.quantity}
                  onChange={e => updateItem(index, 'quantity', e.target.value)}
                  className="form-input-no-icon w-24"
                  required
                />
                <button
                  type="button"
                  onClick={() => removeItem(index)}
                  className="bg-red-600 text-white px-3 py-2 rounded-full hover:bg-red-700 transition-colors"
                >
                  ✕
                </button>
              </div>
            ))}
            <button
              type="button"
              onClick={addItem}
              className="button-secondary !bg-green-600 !text-white hover:!bg-green-700 rounded-full"
            >
              {t('billForm.addItem')}
            </button>
          </div>
        </div>

        <div className="text-right text-xl font-semibold text-apple-gray-900 dark:text-white">
          {t('billForm.total')} ₹{total.toFixed(2)}
        </div>

        <div className="text-center pt-4">
          <button type="submit" className="button-primary w-1/2">
            {t('billForm.createBill')}
          </button>
        </div>
        </form>

      <div className="mt-8 pt-6 border-t border-dashed border-apple-gray-300 dark:border-apple-gray-700">
        <h3 className="text-sm font-semibold text-apple-blue uppercase tracking-widest mb-3 flex items-center gap-2">
          ✨ AI Suggested Upsells
          {loadingSuggestions && <span className="animate-spin text-xs">↻</span>}
        </h3>

        {suggestions.length === 0 ? (
          <p className="text-xs text-apple-gray-500 italic">Add items to see frequently bought together bundles...</p>
        ) : (
          <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
            {suggestions.map(s => (
              <div key={s.productId} className="bg-white dark:bg-apple-gray-900 border border-apple-gray-200 dark:border-apple-gray-800 p-3 rounded-2xl shadow-sm hover:shadow-apple-md transition-all flex flex-col group relative overflow-hidden">
                <div className="absolute top-0 right-0 bg-apple-blue text-white text-[9px] font-bold px-1.5 py-0.5 rounded-bl">
                  HOT
                </div>

                <div className="text-sm font-semibold text-apple-gray-800 dark:text-apple-gray-200 truncate">{s.name}</div>
                <div className="text-xs text-apple-blue font-mono mb-2">₹{s.price}</div>

                <button
                  type="button"
                  onClick={() => {
                    // Check if already in list
                    const exists = items.find(i => i.productId === s.productId);
                    if (exists) {
                      // update qty
                      const newItems = [...items];
                      const idx = newItems.findIndex(i => i.productId === s.productId);
                      newItems[idx].quantity += 1;
                      setItems(newItems);
                      showToast(`Added another ${s.name}`, 'success');
                    } else {
                      // add new
                      setItems([...items, { productId: s.productId, quantity: 1 }]);
                      showToast(`Upselled ${s.name}!`, 'success');
                    }
                  }}
                  className="mt-auto bg-apple-gray-100 text-apple-blue dark:bg-apple-gray-800/60 dark:text-apple-gray-100 text-xs font-semibold py-1.5 rounded-full hover:bg-apple-gray-200 transition-colors flex items-center justify-center gap-1"
                >
                  <span>+ Quick Add</span>
                </button>
              </div>
            ))}
          </div>
        )}
      </div>
      </div>
    </div>
  );
}