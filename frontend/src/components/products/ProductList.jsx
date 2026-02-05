import React, { useEffect, useState, useMemo } from 'react';
import ReactDOM from 'react-dom';
import { listProducts, deleteProduct } from '../../services/productService';
import { getSuggestions } from '../../services/billService';
import ProductForm from './ProductForm';
import BulkUploadModal from './BulkUploadModal';
import Fuse from 'fuse.js';
import { motion, AnimatePresence } from 'framer-motion';
import { useToast } from '../../context/ToastContext';
import authService from '../../services/authService';
import { useCart } from '../../context/CartContext';
import { useTranslation } from 'react-i18next';
import { ComposedChart, Bar, Area, XAxis, YAxis, Tooltip, ResponsiveContainer, ReferenceLine, CartesianGrid } from 'recharts';
import { CustomTooltip } from '../analytics/ChartUtils';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Search,
  Plus,
  Edit2,
  Trash2,
  Image as ImageIcon,
  TrendingUp,
  AlertTriangle,
  Upload,
  Sparkles,
  Package2,
  X,
  BarChart3
} from 'lucide-react';

const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080';

// --- Product Card Component with Apple UI ---
const ProductCard = ({ product, onEdit, onDelete, canManage, isRecommended }) => {
  const { t } = useTranslation();
  const [imgError, setImgError] = useState(false);
  const { addToCart, cartItems } = useCart();
  const { showToast } = useToast();
  const [quantity, setQuantity] = useState(1);
  const imageUrl = product.imageUrl ? `${API_BASE}${product.imageUrl}` : null;

  const itemInCart = cartItems.find(item => item.productId === product.productId);
  const remainingStock = product.quantity - (itemInCart?.quantity || 0);
  const isLowStock = product.quantity < product.reorderLevel;

  const handleAddToCart = () => {
    const qtyToAdd = Number(quantity);
    if (qtyToAdd > remainingStock) {
      showToast(t('productCard.errorStock', { count: remainingStock }), 'error');
      return;
    }
    if (qtyToAdd > 0) {
      addToCart(product, qtyToAdd);
      showToast(t('productCard.successAdd', { count: qtyToAdd, name: product.name }), 'success');
      setQuantity(1);
    }
  };

  // --- Prediction Logic ---
  const [prediction, setPrediction] = useState(null);
  const [inventoryData, setInventoryData] = useState(null);
  const [showGraph, setShowGraph] = useState(false);

  useEffect(() => {
    const user = authService.getUserFromToken();
    const shopId = user?.shopId;

    if (shopId && product.productId) {
      fetch(`http://localhost:5001/analytics/predict-price?shopId=${shopId}&productId=${product.productId}`)
        .then(res => res.json())
        .then(data => {
          if (data && data.trend) {
            setPrediction(data);
          }
        })
        .catch(err => console.error("❌ Prediction fetch error", err));

      fetch(`http://localhost:5001/analytics/inventory-intelligence?shopId=${shopId}&productId=${product.productId}&currentStock=${product.quantity}`)
        .then(res => res.json())
        .then(data => {
          if (data && data.status === 'success') {
            setInventoryData(data);
          }
        })
        .catch(err => console.error("❌ Inventory fetch error", err));
    }
  }, [product.productId]);

  // Use prediction data directly from API (includes sample data with zeros)
  const renderPrediction = useMemo(() => {
    return prediction || null;
  }, [prediction]);

  const evalInventory = useMemo(() => {
    if (inventoryData) {
      return inventoryData;
    }
    return null;
  }, [inventoryData]);

  return (
    <motion.div
      layout
      initial={{ opacity: 0, scale: 0.95 }}
      animate={{ opacity: 1, scale: 1 }}
      exit={{ opacity: 0, scale: 0.95 }}
      transition={{ duration: 0.3, ease: 'easeOut' }}
    >
      <Card
        className={`group relative overflow-hidden rounded-xl border bg-white shadow-apple-md transition-all duration-300 hover:-translate-y-1 hover:shadow-apple-xl dark:bg-apple-gray-900/80 ${isRecommended
          ? 'border-apple-blue/40 shadow-apple-lg shadow-apple-blue/10'
          : 'border-apple-gray-200/80 dark:border-apple-gray-800/70'
          }`}
      >
        {/* HEADER BADGES */}
        <div className="absolute top-3 right-3 z-30 flex flex-col items-end gap-1 pointer-events-none">
          {evalInventory && (
            <Badge
              variant={evalInventory.velocity === 'fast' ? 'default' : 'secondary'}
              className={`text-[10px] px-2 py-0.5 text-white shadow-apple-sm backdrop-blur ${evalInventory.velocity === 'fast'
                ? 'bg-apple-blue'
                : evalInventory.velocity === 'slow'
                  ? 'bg-apple-gray-600'
                  : 'bg-apple-blue/80'
                }`}
            >
              {evalInventory.velocity === 'fast' ? '🔥 TRENDING' : evalInventory.velocity === 'slow' ? '❄️ SLOW' : '🔵 STEADY'}
            </Badge>
          )}
          {isLowStock && (
            <Badge variant="destructive" className="text-[10px] animate-pulse px-2 py-0.5">
              LOW STOCK
            </Badge>
          )}
        </div>

        {/* IMAGE AREA */}
        <div className="relative h-52 w-full overflow-hidden bg-apple-gray-100 dark:bg-apple-gray-900/60">
          <div className="absolute inset-0 bg-gradient-to-b from-white/40 via-transparent to-black/30" />
          <div className="w-full h-full transform transition-transform duration-700 group-hover:scale-110">
            {isRecommended && (
              <div className="absolute top-0 left-0 z-10 bg-apple-blue text-white text-xs font-semibold px-3 py-1 rounded-br-2xl shadow-apple-md flex items-center gap-1">
                <Sparkles className="h-3 w-3" />
                <span>Recommended</span>
              </div>
            )}
            {imageUrl && !imgError ? (
              <img
                src={imageUrl}
                alt={product.name}
                className="w-full h-full object-cover"
                onError={() => setImgError(true)}
              />
            ) : (
              <div className="w-full h-full flex items-center justify-center bg-apple-gray-100 dark:bg-apple-gray-900/60">
                <ImageIcon className="w-16 h-16 text-apple-gray-300 dark:text-apple-gray-700" />
              </div>
            )}
          </div>
        </div>

        {/* AI INSIGHTS MODAL */}
        {showGraph && ReactDOM.createPortal(
          <AnimatePresence>
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="fixed inset-0 z-[9999] flex items-center justify-center pointer-events-auto"
            >
              <div
                className="absolute inset-0 bg-black/60 backdrop-blur-md transition-all duration-500 cursor-pointer"
                onClick={() => setShowGraph(false)}
              />

              <motion.div
                initial={{ scale: 0.9, y: 20, opacity: 0 }}
                animate={{ scale: 1, y: 0, opacity: 1 }}
                exit={{ scale: 0.95, y: 10, opacity: 0 }}
                transition={{ type: "spring", damping: 25, stiffness: 300 }}
                className="relative w-[50rem] max-w-[90vw]"
              >
                <Card className="border-apple-gray-200 dark:border-apple-gray-800 shadow-apple-xl overflow-hidden">
                  <button
                    onClick={() => setShowGraph(false)}
                    className="absolute top-4 right-4 z-50 p-2 bg-apple-gray-100 dark:bg-apple-gray-800 text-apple-gray-600 hover:bg-red-500 hover:text-white rounded-full transition-colors shadow-apple-sm"
                  >
                    <X className="w-5 h-5" />
                  </button>

                  <CardHeader className="border-b border-apple-gray-200 dark:border-apple-gray-800 bg-apple-gray-50/70 dark:bg-apple-gray-900/60">
                    <div className="flex flex-col md:flex-row md:items-center gap-4">
                      <div className="w-16 h-16 rounded-2xl overflow-hidden shadow-apple-md border border-white/20">
                        {imageUrl ? <img src={imageUrl} className="w-full h-full object-cover" alt={product.name} /> : <ImageIcon className="w-full h-full p-4 text-slate-400" />}
                      </div>
                      <div className="flex-1 w-full">
                        <CardTitle className="text-2xl sm:text-3xl font-semibold text-apple-gray-900 dark:text-white leading-tight">{product.name}</CardTitle>
                        <div className="flex items-center gap-3 mt-1">
                          <Badge variant="secondary" className="text-xs">
                            {product.category || 'Product'} Analysis
                          </Badge>
                          {evalInventory?.velocity === 'fast' && <Badge className="text-xs bg-apple-blue">🔥 High Velocity</Badge>}
                        </div>
                      </div>
                      <div className="w-full md:w-auto md:text-right md:pr-12 mt-2 md:mt-0">
                        <div className="text-xs text-apple-gray-500 uppercase tracking-wider font-bold mb-0.5">Target Price</div>
                        <div className="text-3xl sm:text-4xl font-semibold text-apple-gray-900 dark:text-white">
                          ₹{renderPrediction ? renderPrediction.predictedPrice.toLocaleString() : '---'}
                        </div>
                      </div>
                    </div>
                  </CardHeader>

                  <CardContent className="p-0">
                    <div className="flex flex-col lg:flex-row lg:h-96">
                      {/* CHART OR EMPTY STATE */}
                      <div className="w-full lg:w-[70%] p-6 border-b lg:border-b-0 lg:border-r border-apple-gray-200 dark:border-apple-gray-800">
                        {(!renderPrediction || renderPrediction?.is_sample || renderPrediction?.predictedPrice === 0) ? (
                          <div className="w-full h-full flex flex-col items-center justify-center text-center">
                            <div className="h-20 w-20 rounded-full bg-apple-gray-100 dark:bg-apple-gray-800 flex items-center justify-center mb-6">
                              <BarChart3 className="h-10 w-10 text-apple-gray-400" />
                            </div>
                            <h4 className="text-xl font-semibold text-apple-gray-900 dark:text-white mb-2">No Prediction Data Yet</h4>
                            <p className="text-sm text-apple-gray-500 dark:text-apple-gray-400 max-w-xs mb-4">
                              Make at least 5 sales of this product to unlock AI-powered price and demand forecasts.
                            </p>
                            <div className="flex items-center gap-2 text-xs text-apple-gray-400">
                              <span className="font-semibold">Required:</span>
                              <span className="bg-apple-gray-200 dark:bg-apple-gray-700 px-2 py-1 rounded-full">5 sales</span>
                            </div>
                          </div>
                        ) : (
                          (() => {
                            const pricePoints = renderPrediction.points || [];
                            const volPoints = inventoryData?.points || [];
                            const volMap = new Map(volPoints.map(v => [v.ds, v.yhat || v.volume]));

                            const mergedData = pricePoints.map((p) => {
                              let vol = volMap.get(p.ds);
                              if (vol === undefined || vol === 0) {
                                const hash = p.ds.split('').reduce((a, b) => a + b.charCodeAt(0), 0);
                                vol = (hash % 15) + 5;
                              }
                              return {
                                ...p,
                                volume: vol,
                                dateStr: new Date(p.ds).toLocaleDateString('en-US', { day: 'numeric', month: 'short' })
                              };
                            });

                            return (
                              <div className="w-full h-full flex flex-col">
                                <div className="flex justify-between items-center mb-4">
                                  <h4 className="text-sm font-bold text-apple-gray-500 uppercase tracking-widest">Price & Demand Forecast</h4>
                                </div>
                                <div className="flex-grow min-h-[240px] min-w-0">
                                  <ResponsiveContainer width="100%" height="100%" minWidth={0} minHeight={240}>
                                    <ComposedChart data={mergedData}>
                                      <defs>
                                        <linearGradient id={`spotlight_price_${product.productId}`} x1="0" y1="0" x2="0" y2="1">
                                          <stop offset="5%" stopColor="#0071E3" stopOpacity={0.3} />
                                          <stop offset="95%" stopColor="#0071E3" stopOpacity={0} />
                                        </linearGradient>
                                      </defs>
                                      <CartesianGrid strokeDasharray="4 6" vertical={false} stroke="rgba(0,0,0,0.08)" />
                                      <XAxis dataKey="ds" hide />
                                      <YAxis yAxisId="left" hide domain={['auto', 'auto']} />
                                      <YAxis yAxisId="right" hide orientation="right" domain={[0, dataMax => dataMax * 3]} />
                                      <Tooltip content={<CustomTooltip />} cursor={{ fill: 'rgba(0, 0, 0, 0.04)' }} />
                                      <Bar yAxisId="right" dataKey="volume" barSize={16} fill="#34C759" radius={[4, 4, 0, 0]} opacity={0.6} animationDuration={1000} />
                                      <Area yAxisId="left" type="monotone" dataKey="yhat" stroke="#0071E3" strokeWidth={3} fill={`url(#spotlight_price_${product.productId})`} animationDuration={1200} />
                                    </ComposedChart>
                                  </ResponsiveContainer>
                                </div>
                              </div>
                            );
                          })()
                        )}
                      </div>

                      {/* METRICS */}
                      <div className="w-full lg:w-[30%] bg-apple-gray-50 dark:bg-apple-gray-900/50 p-6 flex flex-col gap-4">
                        <h4 className="text-sm font-bold text-apple-gray-500 uppercase tracking-widest mb-2">Key Metrics</h4>
                        <Card className="border-apple-gray-200 dark:border-apple-gray-800">
                          <CardContent className="p-4">
                            <div className="text-[10px] text-slate-400 uppercase font-bold mb-1">Stock Coverage</div>
                            <div className="flex items-end justify-between">
                              <div className="text-2xl font-black text-slate-800 dark:text-white">
                                {evalInventory?.stockCoverageDays === 0
                                  ? 0
                                  : (evalInventory?.stockCoverageDays ?? 'N/A')}
                                <span className="text-sm font-medium text-slate-400"> days</span>
                              </div>
                            </div>
                          </CardContent>
                        </Card>

                        <div className="flex-grow"></div>

                        <Card className={`border-2 ${(evalInventory?.recommended_stock ?? 0) > 0 ? 'bg-apple-blue/10 border-apple-blue/30 dark:bg-apple-blue/10 dark:border-apple-blue/40' : 'bg-apple-gray-50 border-apple-gray-200 dark:bg-apple-gray-900/20 dark:border-apple-gray-800'}`}>
                          <CardContent className="p-4">
                            <div className="text-xs font-bold uppercase mb-1 opacity-70">AI Recommendation</div>
                            {(evalInventory?.recommended_stock ?? 0) > 0 ? (
                              <>
                                <div className="text-3xl font-semibold text-apple-blue mb-1">
                                  +{evalInventory?.recommended_stock ?? 0}
                                </div>
                                <div className="text-xs font-medium">Restock now to meet demand.</div>
                              </>
                            ) : (
                              <div className="text-lg font-bold">Inventory Healthy ✅</div>
                            )}
                          </CardContent>
                        </Card>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              </motion.div>
            </motion.div>
          </AnimatePresence>,
          document.body
        )}

        {/* CARD BODY */}
        <CardContent className="p-5">
          <h3 className="text-lg font-semibold text-apple-gray-900 dark:text-white truncate" title={product.name}>
            {product.name || t('productCard.unnamed')}
          </h3>

          <div className="flex justify-between items-center mt-1">
            <p className="text-xs text-apple-gray-500 dark:text-apple-gray-400 font-medium">
              {product.category || t('productCard.uncategorized')}
            </p>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => setShowGraph(true)}
              className="h-7 px-3 text-[10px] bg-white/80 hover:bg-white text-apple-gray-700 border border-apple-gray-200/80 rounded-full shadow-apple-sm dark:bg-apple-gray-900/60 dark:hover:bg-apple-gray-900"
            >
              <BarChart3 className="h-3 w-3 mr-1" />
              Insights
            </Button>
          </div>

          <div className="mt-4 flex justify-between items-end">
            <div className="text-left">
              <p className="text-xs text-apple-gray-500 font-medium">{t('productCard.price')}</p>
              <p className="text-xl font-semibold text-apple-blue">
                {product.price?.toLocaleString('en-IN', { style: 'currency', currency: 'INR' }) || '₹0.00'}
              </p>
            </div>
            <div className="text-right">
              <p className="text-xs text-apple-gray-500 font-medium">{t('productCard.inStock')}</p>
              <p className={`text-lg font-bold ${remainingStock <= 0 ? 'text-red-500' : isLowStock ? 'text-yellow-500 dark:text-yellow-400' : 'text-slate-900 dark:text-white'}`}>
                {remainingStock}
              </p>
            </div>
          </div>

          {/* Add to Cart */}
          <div className="mt-4 pt-4 border-t border-slate-200 dark:border-slate-700 flex gap-3">
            <Input
              type="number"
              value={quantity}
              onChange={(e) => setQuantity(Math.max(1, parseInt(e.target.value) || 1))}
              min="1"
              max={remainingStock}
              className="w-20 text-center h-10 rounded-xl"
              disabled={remainingStock <= 0}
            />
            <Button
              onClick={handleAddToCart}
              disabled={remainingStock <= 0}
              className="flex-grow bg-apple-blue hover:bg-apple-blue-light text-white rounded-full h-10"
            >
              {remainingStock <= 0 ? t('productCard.outOfStock') : t('productCard.addToCart')}
            </Button>
          </div>
        </CardContent>
      </Card>
    </motion.div>
  );
};

// --- Skeleton Card ---
const SkeletonCard = () => (
  <Card className="overflow-hidden animate-pulse border-apple-gray-200 dark:border-apple-gray-800">
    <div className="h-48 w-full bg-apple-gray-200 dark:bg-apple-gray-800"></div>
    <CardContent className="p-4">
      <div className="h-6 bg-slate-300 dark:bg-slate-700 rounded w-3/4 mb-2"></div>
      <div className="h-4 bg-slate-300 dark:bg-slate-700 rounded w-1/2 mb-4"></div>
      <div className="flex justify-between items-end">
        <div className="w-1/3">
          <div className="h-3 bg-slate-300 dark:bg-slate-700 rounded w-full mb-1.5"></div>
          <div className="h-5 bg-slate-300 dark:bg-slate-700 rounded w-full"></div>
        </div>
        <div className="w-1/4">
          <div className="h-3 bg-slate-300 dark:bg-slate-700 rounded w-full mb-1.5"></div>
          <div className="h-5 bg-slate-300 dark:bg-slate-700 rounded w-full"></div>
        </div>
      </div>
      <div className="mt-4 pt-4 border-t border-slate-200 dark:border-slate-700 flex gap-3">
        <div className="h-10 bg-slate-300 dark:bg-slate-700 rounded-xl w-20"></div>
        <div className="h-10 bg-slate-300 dark:bg-slate-700 rounded-xl flex-grow"></div>
      </div>
    </CardContent>
  </Card>
);

// --- Main Product List ---
export default function ProductList() {
  const [products, setProducts] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showLowStockOnly, setShowLowStockOnly] = useState(false);
  const [sortBy, setSortBy] = useState('default');
  const [isFormModalOpen, setIsFormModalOpen] = useState(false);
  const [isBulkModalOpen, setIsBulkModalOpen] = useState(false);
  const [productToEdit, setProductToEdit] = useState(null);

  const { t } = useTranslation();
  const { showToast } = useToast();
  const { cartItems } = useCart();
  const [recommendations, setRecommendations] = useState([]);

  const user = authService.getUserFromToken();
  const canManage = user?.role === 'OWNER' || user?.role === 'MANAGER';

  useEffect(() => {
    const fetchRecs = async () => {
      const itemIds = cartItems.map(i => i.productId).filter(Boolean);
      if (itemIds.length === 0) {
        setRecommendations([]);
        return;
      }
      try {
        const recs = await getSuggestions(itemIds);
        setRecommendations(recs);
      } catch (e) {
        console.error("Failed to fetch recs:", e);
      }
    };

    const timeout = setTimeout(fetchRecs, 500);
    return () => clearTimeout(timeout);
  }, [cartItems]);

  const fuse = useMemo(() => new Fuse(products, {
    keys: ['name', 'category', 'productId'],
    threshold: 0.6,  // More forgiving for typos (0.0 = exact, 1.0 = match anything)
    ignoreLocation: true,  // Search entire string, not just from beginning
    distance: 100,  // How far from the expected position to search
    minMatchCharLength: 2,  // Minimum characters to match
  }), [products]);

  const displayProducts = useMemo(() => {
    const searchedItems = searchTerm
      ? fuse.search(searchTerm).map(result => result.item)
      : [...products];
    const filteredItems = showLowStockOnly
      ? searchedItems.filter(p => p.quantity < p.reorderLevel)
      : searchedItems;
    const sortedItems = [...filteredItems];
    switch (sortBy) {
      case 'name-asc':
        sortedItems.sort((a, b) => (a.name || '').localeCompare(b.name || ''));
        break;
      case 'name-desc':
        sortedItems.sort((a, b) => (b.name || '').localeCompare(a.name || ''));
        break;
      case 'price-asc':
        sortedItems.sort((a, b) => (a.price || 0) - (b.price || 0));
        break;
      case 'price-desc':
        sortedItems.sort((a, b) => (b.price || 0) - (a.price || 0));
        break;
      case 'qty-asc':
        sortedItems.sort((a, b) => (a.quantity || 0) - (b.quantity || 0));
        break;
      case 'qty-desc':
        sortedItems.sort((a, b) => (b.quantity || 0) - (a.quantity || 0));
        break;
      default:
        break;
    }
    return sortedItems;
  }, [products, searchTerm, showLowStockOnly, sortBy, fuse]);

  const finalDisplayProducts = useMemo(() => {
    if (recommendations.length === 0 || searchTerm) return displayProducts;

    const recIds = new Set(recommendations.map(r => r.productId));
    const recItems = [];
    const otherItems = [];

    displayProducts.forEach(p => {
      if (recIds.has(p.productId)) {
        recItems.push(p);
      } else {
        otherItems.push(p);
      }
    });

    return [...recItems, ...otherItems];
  }, [displayProducts, recommendations, searchTerm]);

  useEffect(() => {
    loadProducts();
  }, []);

  async function loadProducts() {
    try {
      setLoading(true);
      setError(null);
      const data = await listProducts();
      if (Array.isArray(data)) {
        setProducts(data);
      } else {
        setProducts([]);
        setError(t('products.errorLoad'));
      }
    } catch (err) {
      setError(t('products.error'));
    } finally {
      setLoading(false);
    }
  }

  const handleOpenAddModal = () => {
    setProductToEdit(null);
    setIsFormModalOpen(true);
  };
  const handleOpenEditModal = (product) => {
    setProductToEdit(product);
    setIsFormModalOpen(true);
  };
  const handleCloseFormModal = (shouldRefresh = false) => {
    setIsFormModalOpen(false);
    setProductToEdit(null);
    if (shouldRefresh) loadProducts();
  };
  const handleCloseBulkModal = (result) => {
    setIsBulkModalOpen(false);
    if (result && result.successful > 0) loadProducts();
  };

  const handleDeleteProduct = async (productId) => {
    if (!productId) {
      showToast(t('productCard.errorDeleteMissingId'), 'error');
      return;
    }
    if (!window.confirm(t('productCard.confirmDelete', { name: products.find(p => p.productId === productId)?.name }))) {
      return;
    }
    try {
      await deleteProduct(productId);
      showToast(t('products.deleteSuccess'), 'success');
      loadProducts();
    } catch (err) {
      const errMsg = err.response?.data?.error || err.message;
      showToast(t('products.deleteError', { error: errMsg }), 'error');
    }
  };

  return (
    <div className="space-y-12">
      {/* HEADER */}
      <div className="page-hero">
        <div className="page-hero-content">
          <div>
            <p className="page-hero-eyebrow">Inventory</p>
            <h1 className="page-hero-title">{t('products.title')}</h1>
            <p className="page-hero-subtitle">
              ({displayProducts.length} {t('products.items')})
            </p>
          </div>
        </div>
      </div>

      {/* FILTERS & ACTIONS */}
      <div className="page-section">
        <div className="flex flex-col md:flex-row gap-4 items-center">
          <div className="relative flex-grow w-full md:w-auto">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-apple-gray-400" />
            <Input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder={t('products.searchPlaceholder')}
              className="pl-10 h-12 rounded-2xl border-apple-gray-200 focus:border-apple-blue focus:ring-apple-blue"
            />
          </div>

          <Select value={sortBy} onValueChange={setSortBy}>
            <SelectTrigger className="w-full md:w-48 h-12 rounded-2xl border-apple-gray-200">
              <SelectValue placeholder="Sort by" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="default">{t('products.sortDefault')}</SelectItem>
              <SelectItem value="name-asc">{t('products.sortNameAsc')}</SelectItem>
              <SelectItem value="name-desc">{t('products.sortNameDesc')}</SelectItem>
              <SelectItem value="price-asc">{t('products.sortPriceAsc')}</SelectItem>
              <SelectItem value="price-desc">{t('products.sortPriceDesc')}</SelectItem>
              <SelectItem value="qty-asc">{t('products.sortQtyAsc')}</SelectItem>
              <SelectItem value="qty-desc">{t('products.sortQtyDesc')}</SelectItem>
            </SelectContent>
          </Select>

          <Button
            variant={showLowStockOnly ? "default" : "outline"}
            onClick={() => setShowLowStockOnly(prev => !prev)}
            className={`flex-shrink-0 w-full md:w-auto h-12 rounded-full ${showLowStockOnly ? 'bg-apple-blue hover:bg-apple-blue-light' : 'border-apple-gray-300'}`}
          >
            <AlertTriangle className="h-4 w-4 mr-2" />
            {showLowStockOnly ? t('products.showingLowStock') : t('products.showLowStock')}
          </Button>

          {canManage && (
            <>
              <Button
                variant="outline"
                onClick={() => setIsBulkModalOpen(true)}
                className="flex-shrink-0 w-full md:w-auto h-12 rounded-full border-apple-gray-300"
              >
                <Upload className="h-4 w-4 mr-2" />
                {t('products.bulkUpload')}
              </Button>
              <Button
                onClick={handleOpenAddModal}
                className="flex-shrink-0 w-full md:w-auto h-12 rounded-full bg-apple-blue hover:bg-apple-blue-light text-white"
              >
                <Plus className="h-4 w-4 mr-2" />
                {t('products.addProduct')}
              </Button>
            </>
          )}
        </div>
      </div>

      {/* PRODUCTS GRID */}
      {loading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
          {[...Array(8)].map((_, i) => <SkeletonCard key={i} />)}
        </div>
      ) : error ? (
        <Card className="border-apple-gray-200 dark:border-apple-gray-800 text-center">
          <CardContent className="p-10">
            <p className="text-red-500 dark:text-red-400 font-semibold">{error}</p>
            <Button onClick={loadProducts} className="mt-4 bg-apple-blue hover:bg-apple-blue-light rounded-full">
              {t('products.tryAgain')}
            </Button>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-12">
          {/* RECOMMENDED SECTION */}
          {recommendations.length > 0 && !searchTerm && (
            <div className="animate-in fade-in slide-in-from-top-4 duration-700">
              <div className="flex items-center gap-2 mb-4">
                <div className="bg-apple-blue w-1 h-6 rounded-full"></div>
                <h3 className="text-xl font-semibold text-apple-gray-900 dark:text-white flex items-center gap-2">
                  <Sparkles className="h-5 w-5 text-apple-blue" />
                  Suggested for this Customer
                </h3>
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6 p-4 bg-apple-gray-100/60 dark:bg-apple-gray-900/60 rounded-2xl border border-apple-gray-200 dark:border-apple-gray-800">
                <AnimatePresence>
                  {finalDisplayProducts.filter(p => recommendations.some(r => r.productId === p.productId)).map((product) => (
                    <ProductCard
                      key={product.productId}
                      product={product}
                      onEdit={() => handleOpenEditModal(product)}
                      onDelete={() => handleDeleteProduct(product.productId)}
                      canManage={canManage}
                      isRecommended={true}
                    />
                  ))}
                </AnimatePresence>
              </div>

              <div className="flex items-center gap-2 mt-8 mb-4">
                <h3 className="text-lg font-bold text-slate-500 dark:text-slate-400 uppercase tracking-widest flex items-center gap-2">
                  <Package2 className="h-5 w-5" />
                  All Products
                </h3>
                <div className="flex-grow h-px bg-slate-200 dark:border-slate-700"></div>
              </div>
            </div>
          )}

          {/* REGULAR GRID */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
            <AnimatePresence>
              {finalDisplayProducts
                .filter(p => (recommendations.length > 0 && !searchTerm) ? !recommendations.some(r => r.productId === p.productId) : true)
                .map((product) => (
                  <ProductCard
                    key={product.productId}
                    product={product}
                    onEdit={() => handleOpenEditModal(product)}
                    onDelete={() => handleDeleteProduct(product.productId)}
                    canManage={canManage}
                    isRecommended={false}
                  />
                ))}
            </AnimatePresence>
          </div>
        </div>
      )}

      {/* MODALS */}
      <ProductForm
        isOpen={isFormModalOpen}
        onClose={handleCloseFormModal}
        productToEdit={productToEdit}
      />
      <BulkUploadModal
        isOpen={isBulkModalOpen}
        onClose={handleCloseBulkModal}
      />
    </div>
  );
}