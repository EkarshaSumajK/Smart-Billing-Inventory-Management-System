import React, { useEffect, useState } from 'react';
import {
  createProductJson,
  createProductMultipart,
  updateProduct,
  updateProductMultipart
} from '../../services/productService';
import { motion, AnimatePresence } from 'framer-motion';
import { useToast } from '../../context/ToastContext';
import { useTranslation } from 'react-i18next';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { Upload, X } from 'lucide-react';

const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080';

export default function ProductForm({ isOpen, onClose, productToEdit }) {
  const { t } = useTranslation();
  const { showToast } = useToast();
  const [product, setProduct] = useState({
    productId: '',
    name: '',
    category: '',
    price: 0,
    quantity: 0,
    reorderLevel: 10,
  });
  const [imageFile, setImageFile] = useState(null);
  const [imagePreview, setImagePreview] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  const isEdit = Boolean(productToEdit);

  useEffect(() => {
    if (isOpen) {
      if (isEdit) {
        setLoading(true);
        setProduct({
          productId: productToEdit.productId || '',
          name: productToEdit.name || '',
          category: productToEdit.category || '',
          price: productToEdit.price || 0,
          quantity: productToEdit.quantity || 0,
          reorderLevel: productToEdit.reorderLevel || 10,
        });
        if (productToEdit.imageUrl) {
          setImagePreview(`${API_BASE}${productToEdit.imageUrl}`);
        } else {
          setImagePreview(null);
        }
        setImageFile(null);
        setError(null);
        setLoading(false);
      } else {
        setProduct({
          productId: '', name: '', category: '', price: 0, quantity: 0, reorderLevel: 10,
        });
        setImageFile(null);
        setImagePreview(null);
        setError(null);
        setLoading(false);
      }
    }
  }, [productToEdit, isEdit, isOpen]);

  const handleImageChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      setImageFile(file);
      setImagePreview(URL.createObjectURL(file));
    }
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setProduct(prev => ({
      ...prev,
      [name]: value,
    }));
  };

  async function handleSubmit(e) {
    e.preventDefault();
    setLoading(true);
    setError(null);

    const productData = {
      productId: product.productId,
      name: product.name,
      category: product.category,
      price: Number(product.price),
      quantity: Number(product.quantity),
      reorderLevel: Number(product.reorderLevel),
    };

    try {
      if (isEdit) {
        if (imageFile) {
          await updateProductMultipart(productToEdit.productId, productData, imageFile);
        } else {
          await updateProduct(productToEdit.productId, productData);
        }
        showToast(t('products.toastUpdateSuccess'), 'success');
      } else {
        if (imageFile) {
          await createProductMultipart(productData, imageFile);
        } else {
          await createProductJson(productData);
        }
        showToast(t('products.toastCreateSuccess'), 'success');
      }
      onClose(true);
    } catch (err) {
      console.error(err);
      const errMsg = err.response?.data?.error || err.message || t('products.toastError');
      setError(errMsg);
      showToast(errMsg, 'error');
    } finally {
      setLoading(false);
    }
  }

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !loading && !open && onClose(false)}>
      <DialogContent className="max-w-2xl border-apple-gray-200 dark:border-apple-gray-800 rounded-xl p-0 gap-0">
        {/* HEADER */}
        <DialogHeader className="p-6 border-b border-apple-gray-200 dark:border-apple-gray-800 bg-apple-gray-50/70 dark:bg-apple-gray-900/60 rounded-t-xl">
          <DialogTitle className="text-2xl font-semibold text-apple-gray-900 dark:text-white">
            {isEdit ? t('products.formTitleEdit') : t('products.formTitleAdd')}
          </DialogTitle>
          <DialogDescription className="text-apple-gray-500 dark:text-apple-gray-400">
            {isEdit ? 'Update product details' : 'Add a new product to your inventory'}
          </DialogDescription>
        </DialogHeader>

        {/* BODY */}
        <form onSubmit={handleSubmit} className="p-6 overflow-y-auto max-h-[calc(90vh-200px)]">
          {error && (
            <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-100 px-4 py-3 rounded-xl mb-4" role="alert">
              <span className="font-semibold">{t('products.error')}:</span> {error}
            </div>
          )}

          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {/* IMAGE UPLOADER */}
            <div className="md:col-span-1">
              <Label className="block text-sm font-semibold text-apple-gray-600 dark:text-apple-gray-400 mb-2">
                {t('products.formImage')}
              </Label>
              <div className="aspect-square">
                <div className="w-full h-full border-2 border-dashed border-apple-gray-300 dark:border-apple-gray-700 rounded-lg flex items-center justify-center text-center relative overflow-hidden hover:border-apple-blue transition-colors group">
                  {imagePreview ? (
                    <>
                      <img src={imagePreview} alt="Preview" className="w-full h-full object-cover rounded-lg" />
                      <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
                        <Upload className="h-8 w-8 text-white" />
                      </div>
                    </>
                  ) : (
                    <div className="flex flex-col items-center pointer-events-none">
                      <Upload className="h-10 w-10 text-apple-gray-400 dark:text-apple-gray-500 mb-2" />
                      <span className="text-sm text-apple-gray-500 dark:text-apple-gray-400">{t('products.formImageDrop')}</span>
                    </div>
                  )}
                  <input
                    type="file"
                    accept="image/png, image/jpeg, image/webp"
                    onChange={handleImageChange}
                    className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
                    title={isEdit ? t('products.formImageReplace') : t('products.formImageUpload')}
                    disabled={loading}
                  />
                </div>
              </div>
            </div>

            {/* FORM FIELDS */}
            <div className="md:col-span-2 grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="sm:col-span-2">
                <Label htmlFor="productId" className="block text-sm font-semibold text-apple-gray-600 dark:text-apple-gray-400 mb-1.5">
                  {t('products.formId')} *
                </Label>
                <Input
                  id="productId"
                  name="productId"
                  type="text"
                  value={product.productId}
                  onChange={handleChange}
                  placeholder={t('products.formIdPlaceholder')}
                  className="h-10 rounded-md border-apple-gray-300 focus:border-apple-blue focus:ring-apple-blue/30"
                  required
                  disabled={isEdit || loading}
                />
              </div>

              <div className="sm:col-span-2">
                <Label htmlFor="name" className="block text-sm font-semibold text-apple-gray-600 dark:text-apple-gray-400 mb-1.5">
                  {t('products.formName')} *
                </Label>
                <Input
                  id="name"
                  name="name"
                  type="text"
                  value={product.name}
                  onChange={handleChange}
                  placeholder={t('products.formNamePlaceholder')}
                  className="h-10 rounded-md border-apple-gray-300 focus:border-apple-blue focus:ring-apple-blue/30"
                  required
                  disabled={loading}
                />
              </div>

              <div>
                <Label htmlFor="category" className="block text-sm font-semibold text-apple-gray-600 dark:text-apple-gray-400 mb-1.5">
                  {t('products.formCategory')}
                </Label>
                <Input
                  id="category"
                  name="category"
                  type="text"
                  value={product.category}
                  onChange={handleChange}
                  placeholder={t('products.formCategoryPlaceholder')}
                  className="h-10 rounded-md border-apple-gray-300 focus:border-apple-blue focus:ring-apple-blue/30"
                  disabled={loading}
                />
              </div>

              <div>
                <Label htmlFor="price" className="block text-sm font-semibold text-apple-gray-600 dark:text-apple-gray-400 mb-1.5">
                  {t('products.formPrice')} *
                </Label>
                <Input
                  id="price"
                  name="price"
                  type="number"
                  min="0"
                  step="0.01"
                  value={product.price}
                  onChange={handleChange}
                  placeholder={t('products.formPricePlaceholder')}
                  className="h-10 rounded-md border-apple-gray-300 focus:border-apple-blue focus:ring-apple-blue/30"
                  required
                  disabled={loading}
                />
              </div>

              <div>
                <Label htmlFor="quantity" className="block text-sm font-semibold text-apple-gray-600 dark:text-apple-gray-400 mb-1.5">
                  {t('products.formQuantity')} *
                </Label>
                <Input
                  id="quantity"
                  name="quantity"
                  type="number"
                  min="0"
                  step="1"
                  value={product.quantity}
                  onChange={handleChange}
                  placeholder={t('products.formQuantityPlaceholder')}
                  className="h-10 rounded-md border-apple-gray-300 focus:border-apple-blue focus:ring-apple-blue/30"
                  required
                  disabled={loading}
                />
              </div>

              <div>
                <Label htmlFor="reorderLevel" className="block text-sm font-semibold text-apple-gray-600 dark:text-apple-gray-400 mb-1.5">
                  {t('products.formReorder')} *
                </Label>
                <Input
                  id="reorderLevel"
                  name="reorderLevel"
                  type="number"
                  min="0"
                  step="1"
                  value={product.reorderLevel}
                  onChange={handleChange}
                  placeholder={t('products.formReorderPlaceholder')}
                  className="h-10 rounded-md border-apple-gray-300 focus:border-apple-blue focus:ring-apple-blue/30"
                  required
                  disabled={loading}
                />
              </div>
            </div>
          </div>
        </form>

        {/* FOOTER */}
        <DialogFooter className="p-6 border-t border-apple-gray-200 dark:border-apple-gray-800 bg-apple-gray-50 dark:bg-apple-gray-900/50 rounded-b-xl">
          <Button
            type="button"
            variant="outline"
            onClick={() => onClose(false)}
            className="rounded-md h-10 border-apple-gray-300"
            disabled={loading}
          >
            {t('products.formCancel')}
          </Button>
          <Button
            type="submit"
            onClick={handleSubmit}
            className="rounded-md h-10 bg-apple-blue hover:bg-apple-blue-light text-white"
            disabled={loading}
          >
            {loading
              ? (isEdit ? t('products.formUpdating') : t('products.formSaving'))
              : (isEdit ? t('products.formSave') : t('products.formCreate'))}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}