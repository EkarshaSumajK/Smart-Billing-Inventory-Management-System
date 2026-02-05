import React, { useEffect, useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  listBills,
  downloadBillPdf,
  resendBillEmail,
  generatePdfToken,
  downloadBillPdfWithFlow,
  handlePdfDownload
} from '../../services/billService';
import { useToast } from '../../context/ToastContext';
import authService from '../../services/authService';
import Fuse from 'fuse.js';
import { motion, AnimatePresence } from 'framer-motion';
import DatePicker from 'react-datepicker';
import "react-datepicker/dist/react-datepicker.css";
import { useTranslation } from 'react-i18next';

// --- ICONS ---
const SearchIcon = () => (
  <svg className="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
  </svg>
);
const SortIcon = () => (
  <svg className="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 4h13M3 8h9M3 12h9m-9 4h13m-3-4v8m0 0l-4-4m4 4l4-4" />
  </svg>
);
const CalendarIcon = () => (
  <svg className="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
  </svg>
);
const ExportIcon = () => (
  <svg className="w-5 h-5 mr-1.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" />
  </svg>
);
const PdfIcon = () => (
  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
  </svg>
);
const ResendIcon = () => (
  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
  </svg>
);
const CheckCircleIcon = () => (
  <svg className="w-4 h-4 text-[#34C759]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
  </svg>
);
// --- END ICONS ---

// --- Skeleton Row ---
const SkeletonRow = () => (
  <tr className="animate-pulse">
    <td><div className="h-4 bg-gray-200 dark:bg-gray-700 rounded w-24"></div></td>
    <td><div className="h-4 bg-gray-200 dark:bg-gray-700 rounded w-32"></div></td>
    <td><div className="h-4 bg-gray-200 dark:bg-gray-700 rounded w-20"></div></td>
    <td><div className="h-4 bg-gray-200 dark:bg-gray-700 rounded w-36"></div></td>
    <td><div className="h-4 bg-gray-200 dark:bg-gray-700 rounded w-20"></div></td>
    <td className="flex gap-2 justify-center">
      <div className="h-9 w-9 bg-gray-200 dark:bg-gray-700 rounded-lg"></div>
      <div className="h-9 w-9 bg-gray-200 dark:bg-gray-700 rounded-lg"></div>
    </td>
  </tr>
);

// --- Customer Avatar Component ---
const CustomerAvatar = ({ name, className = "w-8 h-8" }) => {
  const initials = name ? name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2) : 'WI';
  const colors = [
    'bg-gradient-to-br from-[#0A84FF] to-[#2F81F7]',
    'bg-gradient-to-br from-[#5E5CE6] to-[#AF52DE]',
    'bg-gradient-to-br from-[#FF9F0A] to-[#FF453A]',
    'bg-gradient-to-br from-[#34C759] to-[#30D158]',
    'bg-gradient-to-br from-[#64D2FF] to-[#5E5CE6]'
  ];
  const colorIndex = name ? name.charCodeAt(0) % colors.length : 0;

  return (
    <div className={`${className} ${colors[colorIndex]} rounded-full flex items-center justify-center text-white text-xs font-bold shadow-lg`}>
      {initials}
    </div>
  );
};

// --- Bill Details Drawer ---
const BillDetailsDrawer = ({ bill, isOpen, onClose, t, onDownload, onResend }) => {
  if (!isOpen) return null;

  const { date, time } = formatDate(bill.createdAt || bill.date);

  return (
    <div className="fixed inset-0 z-50 overflow-hidden">
      {/* Backdrop */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        className="absolute inset-0 bg-black/60 backdrop-blur-sm"
        onClick={onClose}
      ></motion.div>

      {/* Drawer */}
      <motion.div
        initial={{ x: '100%' }}
        animate={{ x: 0 }}
        exit={{ x: '100%' }}
        transition={{ type: 'spring', damping: 30, stiffness: 300 }}
        className="absolute right-0 top-0 h-full w-full max-w-md bg-white dark:bg-apple-gray-950/95 dark:backdrop-blur-xl border-l border-apple-gray-200 dark:border-apple-gray-800 shadow-apple-xl"
      >
        <div className="p-6 h-full flex flex-col">
          {/* Header */}
          <div className="flex items-center justify-between mb-6">
            <h3 className="text-xl font-semibold text-apple-gray-900 dark:text-white">{t('billList.drawerTitle')}</h3>
            <button
              onClick={onClose}
              className="p-2 text-apple-gray-500 dark:text-apple-gray-400 hover:bg-apple-gray-100 dark:hover:bg-apple-gray-800 rounded-full transition-colors"
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          {/* Content */}
          <div className="flex-1 space-y-6 overflow-y-auto custom-scrollbar pr-2">
            {/* Bill Info */}
            <div className="space-y-4">
              <div>
                <label className="text-sm text-apple-gray-500 dark:text-apple-gray-400">{t('billList.headerId')}</label>
                <p className="text-lg font-mono text-apple-blue">{bill.billId}</p>
              </div>
              <div>
                <label className="text-sm text-apple-gray-500 dark:text-apple-gray-400">{t('billList.drawerDate')}</label>
                <p className="text-apple-gray-900 dark:text-white">{date} at {time}</p>
              </div>
              <div>
                <label className="text-sm text-apple-gray-500 dark:text-apple-gray-400">{t('billList.drawerTotal')}</label>
                <p className="text-2xl font-semibold text-[#34C759]">
                  {formatCurrency(bill.totalAmount)}
                </p>
              </div>
            </div>

            {/* Customer Info */}
            <div className="space-y-3">
              <label className="text-sm text-apple-gray-500 dark:text-apple-gray-400">{t('billList.drawerCustomer')}</label>
              <div className="flex items-center gap-3 p-3 bg-apple-gray-100 dark:bg-apple-gray-900/50 rounded-2xl">
                <CustomerAvatar name={bill.customer?.name} />
                <div>
                  <p className="text-apple-gray-900 dark:text-white font-medium">{bill.customer?.name || t('billList.walkIn')}</p>
                  <p className="text-sm text-apple-gray-500 dark:text-apple-gray-400">{bill.customer?.mobile || t('billList.noMobile')}</p>
                </div>
              </div>
            </div>

            {/* Items */}
            <div className="space-y-3">
              <label className="text-sm text-apple-gray-500 dark:text-apple-gray-400">{t('billList.drawerItems', { count: bill.items?.length || 0 })}</label>
              <div className="space-y-2 max-h-48 overflow-y-auto custom-scrollbar pr-2">
                {bill.items?.map((item, index) => (
                  <div key={index} className="flex justify-between items-center p-2 bg-apple-gray-100 dark:bg-apple-gray-900/30 rounded-xl">
                    <span className="text-apple-gray-800 dark:text-white text-sm">{item.productName || item.name}</span>
                    <span className="text-[#34C759] text-sm">
                      {formatCurrency(item.price)} x {item.qty}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* Actions */}
          <div className="space-y-3 pt-6 border-t border-apple-gray-200 dark:border-apple-gray-800">
            <button
              onClick={() => onDownload(bill)}
              className="w-full button-primary flex items-center justify-center gap-2"
            >
              <PdfIcon />
              {t('billList.downloadPDF')}
            </button>
            <button
              onClick={() => onResend(bill.billId)}
              className="w-full button-secondary flex items-center justify-center gap-2"
            >
              <ResendIcon />
              {t('billList.resendEmail')}
            </button>
          </div>
        </div>
      </motion.div>
    </div>
  );
};

// Formatting Helpers
const formatCurrency = (amount) => {
  return (amount || 0).toLocaleString('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 2,
  });
};

const formatDate = (dateString) => {
  if (!dateString) return { date: 'N/A', time: '' };
  const date = new Date(dateString);
  return {
    date: date.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }),
    time: date.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', hour12: true })
  };
};

// Custom DatePicker Input Component
const CustomDateInput = React.forwardRef(({ value, onClick, placeholder }, ref) => (
  <button
    type="button"
    ref={ref}
    onClick={onClick}
    className="form-input w-full !pl-10 !py-3 rounded-2xl cursor-pointer text-left"
  >
    <span className={value ? 'text-apple-gray-900 dark:text-white' : 'text-apple-gray-500'}>
      {value || placeholder}
    </span>
  </button>
));

export default function BillList() {
  const { t } = useTranslation();
  const [bills, setBills] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [resendingId, setResendingId] = useState(null);
  const [downloadingId, setDownloadingId] = useState(null);
  const [selectedBill, setSelectedBill] = useState(null);
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);

  const [searchTerm, setSearchTerm] = useState('');
  const [sortBy, setSortBy] = useState('createdAt-desc');
  const [startDate, setStartDate] = useState(null);
  const [endDate, setEndDate] = useState(null);
  const [autoEmail, setAutoEmail] = useState(true);
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 10;

  const { showToast } = useToast();
  const navigate = useNavigate();
  const user = authService.getUserFromToken();
  const canManage = user?.role === 'OWNER' || user?.role === 'MANAGER';

  const fuse = useMemo(() => new Fuse(bills, {
    keys: ['billId', 'customer.name', 'customer.mobile', 'paymentMethod'],
    threshold: 0.4,
  }), [bills]);

  const displayBills = useMemo(() => {
    let items = searchTerm ? fuse.search(searchTerm).map(r => r.item) : [...bills];

    if (startDate || endDate) {
      const start = startDate ? new Date(startDate.setHours(0, 0, 0, 0)) : new Date(0);
      const end = endDate ? new Date(endDate.setHours(23, 59, 59, 999)) : new Date();
      items = items.filter(b => {
        const billDate = new Date(b.createdAt || b.date);
        return billDate >= start && billDate <= end;
      });
    }

    const [key, dir] = sortBy.split('-');
    return [...items].sort((a, b) => {
      let valA = key === 'createdAt' ? new Date(a.createdAt || a.date).getTime() : (a.totalAmount || 0);
      let valB = key === 'createdAt' ? new Date(b.createdAt || b.date).getTime() : (b.totalAmount || 0);
      return dir === 'asc' ? valA - valB : valB - valA;
    });
  }, [bills, searchTerm, sortBy, startDate, endDate, fuse]);

  const totalPages = Math.ceil(displayBills.length / itemsPerPage);
  const paginatedBills = useMemo(() => {
    const startIndex = (currentPage - 1) * itemsPerPage;
    return displayBills.slice(startIndex, startIndex + itemsPerPage);
  }, [displayBills, currentPage, itemsPerPage]);

  useEffect(() => { loadBills(); }, []);

  async function loadBills() {
    try {
      setLoading(true);
      setError(null);
      const data = await listBills();
      setBills(Array.isArray(data) ? data : []);
    } catch (err) {
      const msg = err.response?.data?.message || err.message;
      setError(msg);
      showToast(msg, 'error');
    } finally {
      setLoading(false);
    }
  }

  const handleDownload = async (bill, e) => {
    e?.stopPropagation();
    setDownloadingId(bill.billId);
    try {
      showToast(t('billList.downloading', { billId: bill.billId }), 'info');

      // Use the complete flow that handles token generation automatically
      const pdfBlob = await downloadBillPdfWithFlow(bill.billId);
      handlePdfDownload(pdfBlob, bill.billId);

      showToast(t('billList.downloadSuccess'), 'success');
    } catch (err) {
      console.error('Error downloading PDF:', err);
      const errorMessage = err.response?.data?.message || err.message || t('billList.downloadFailed');
      showToast(errorMessage, 'error');
    } finally {
      setDownloadingId(null);
    }
  };

  const handleResend = async (billId, e) => {
    e?.stopPropagation();
    setResendingId(billId);
    try {
      await resendBillEmail(billId);
      showToast(t('billList.resendSuccess'), 'success');
    } catch (err) {
      const errorMessage = err.response?.data?.message || err.message || t('billList.resendFailed');
      showToast(errorMessage, 'error');
    } finally {
      setResendingId(null);
    }
  };

  const handleRowClick = (bill) => {
    setSelectedBill(bill);
    setIsDrawerOpen(true);
  };

  const handleDrawerDownload = async (bill) => {
    try {
      showToast(t('billList.downloading', { billId: bill.billId }), 'info');
      const pdfBlob = await downloadBillPdfWithFlow(bill.billId);
      handlePdfDownload(pdfBlob, bill.billId);
      showToast(t('billList.downloadSuccess'), 'success');
    } catch (err) {
      console.error('Error downloading PDF:', err);
      const errorMessage = err.response?.data?.message || err.message || t('billList.downloadFailed');
      showToast(errorMessage, 'error');
    }
  };

  const handleDrawerResend = async (billId) => {
    try {
      await resendBillEmail(billId);
      showToast(t('billList.resendSuccess'), 'success');
    } catch (err) {
      const errorMessage = err.response?.data?.message || err.message || t('billList.resendFailed');
      showToast(errorMessage, 'error');
    }
  };

  const exportToCsv = () => {
    if (!displayBills.length) return showToast(t('billList.exportError'), 'error');
    const headers = ["Bill ID", "Customer", "Mobile", "Amount", "Date", "Items", "Payment Method"];
    const rows = displayBills.map(b => [
      b.billId,
      b.customer?.name || '',
      b.customer?.mobile || '',
      formatCurrency(b.totalAmount),
      new Date(b.createdAt || b.date).toLocaleString(),
      b.items?.length || 0,
      b.paymentMethod || 'cash'
    ]);
    const csv = [headers, ...rows.map(r => r.join(','))].join('\n');
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `SmartRetail_Bills_${new Date().toISOString().slice(0,10)}.csv`;
    a.click();
    URL.revokeObjectURL(url);
    showToast(t('billList.exportSuccess', { count: displayBills.length }), 'success');
  };

  const EmptyState = () => (
    <div className="flex flex-col items-center justify-center py-16">
      <svg className="w-16 h-16 text-gray-400 dark:text-gray-500 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 17v-6h6v6m-6-4h.01M12 3v4m0 0h8m-8 0H4" />
      </svg>
      <p className="text-lg text-gray-500 dark:text-gray-400 font-light">{t('billList.emptyState')}</p>
      <p className="text-sm text-gray-400 dark:text-gray-500 mt-1">{t('billList.emptyStateHint')}</p>
    </div>
  );

  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4 }}
      className="space-y-12"
    >
      {/* Frosted Header Bar */}
      <div className="page-hero">
        <div className="page-hero-content">
          <div>
            <p className="page-hero-eyebrow">Billing</p>
            <h2 className="page-hero-title">
          {t('billList.title')}
            </h2>
            <p className="page-hero-subtitle">
              ({displayBills.length} {t('billList.matching')})
            </p>
          </div>
          {canManage && (
            <div className="flex items-center gap-3">
              <label className="flex items-center gap-2 text-sm text-apple-gray-600 dark:text-apple-gray-300 cursor-pointer">
                <div className="relative">
                  <input
                    type="checkbox"
                    checked={autoEmail}
                    onChange={(e) => setAutoEmail(e.target.checked)}
                    className="sr-only"
                  />
                  <div className={`w-10 h-6 rounded-full transition-colors ${
                    autoEmail ? 'bg-apple-blue' : 'bg-apple-gray-400 dark:bg-apple-gray-600'
                  }`}></div>
                  <div className={`absolute top-1 w-4 h-4 rounded-full bg-white transition-transform ${
                    autoEmail ? 'transform translate-x-5' : 'transform translate-x-1'
                  }`}></div>
                </div>
                {t('billList.autoEmail')}
              </label>
            </div>
          )}
        </div>
      </div>

      {/* Control Bar */}
      <div className="page-section">
        <div className="flex flex-col lg:flex-row gap-4 items-center">
          <div className="relative flex-grow w-full md:w-auto">
            <div className="absolute inset-y-0 left-0 flex items-center pl-3 pointer-events-none z-10">
              <SearchIcon />
            </div>
            <input
              type="text"
              value={searchTerm}
              onChange={e => setSearchTerm(e.target.value)}
              placeholder={t('billList.searchPlaceholder')}
              className="form-input w-full !py-3 rounded-2xl transition-all"
            />
          </div>

          <div className="relative flex-shrink-0 w-full md:w-64">
            <div className="absolute inset-y-0 left-0 flex items-center pl-3 pointer-events-none z-10">
              <CalendarIcon />
            </div>
            <DatePicker
              selectsRange
              startDate={startDate}
              endDate={endDate}
              onChange={([start, end]) => {
                setStartDate(start);
                setEndDate(end);
                setCurrentPage(1);
              }}
              isClearable
              placeholderText={t('billList.datePlaceholder')}
              customInput={<CustomDateInput placeholder={t('billList.datePlaceholder')} />}
              className="w-full"
              popperClassName="react-datepicker-dark z-50"
              popperPlacement="bottom-start"
              dateFormat="MMM d, yyyy"
              showPopperArrow={false}
              wrapperClassName="w-full"
            />
          </div>

          <div className="relative flex-shrink-0 w-full md:w-56">
            <div className="absolute inset-y-0 left-0 flex items-center pl-3 pointer-events-none z-10">
              <SortIcon />
            </div>
            <select
              value={sortBy}
              onChange={e => setSortBy(e.target.value)}
              className="form-input appearance-none w-full !py-3 rounded-2xl"
            >
              <option value="createdAt-desc">{t('billList.sortNewest')}</option>
              <option value="createdAt-asc">{t('billList.sortOldest')}</option>
              <option value="totalAmount-desc">{t('billList.sortHigh')}</option>
              <option value="totalAmount-asc">{t('billList.sortLow')}</option>
            </select>
          </div>

          {canManage && (
            <button
              onClick={exportToCsv}
              className="button-secondary !px-6 !py-3 rounded-full flex items-center justify-center gap-2 flex-shrink-0 w-full md:w-auto font-semibold transition-all duration-200"
            >
              <ExportIcon />
              {t('billList.export')}
            </button>
          )}
        </div>
      </div>

      {error && (
        <div className="page-section text-center">
          <p className="text-red-500 dark:text-red-400 font-semibold">{error}</p>
          <button onClick={loadBills} className="button-primary mt-4">{t('common.tryAgain')}</button>
        </div>
      )}

      {!error && (
        <div className="page-section overflow-x-auto p-0">
          <table className="table w-full">
            <thead>
              <tr>
                <th className="text-left">{t('billList.headerId')}</th>
                <th className="text-left">{t('billList.headerCustomer')}</th>
                <th className="text-right">{t('billList.headerTotal')}</th>
                <th className="text-left">{t('billList.headerDate')}</th>
                <th className="text-left">{t('billList.headerPayment')}</th>
                <th className="text-left">{t('billList.headerStatus')}</th>
                <th className="text-center">{t('billList.headerActions')}</th>
              </tr>
            </thead>
            <motion.tbody layout>
              <AnimatePresence>
                {loading ? (
                  Array(5).fill().map((_, i) => <SkeletonRow key={i} />)
                ) : paginatedBills.length === 0 ? (
                  <tr>
                    <td colSpan="7">
                      <EmptyState />
                    </td>
                  </tr>
                ) : (
                  paginatedBills.map((bill, i) => {
                    const { date, time } = formatDate(bill.createdAt || bill.date);
                    return (
                      <motion.tr
                        key={bill.billId}
                        layout
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        transition={{ delay: i * 0.02, duration: 0.2 }}
                        className="hover:bg-apple-gray-100/70 dark:hover:bg-apple-gray-900/60 transition-all duration-200 cursor-pointer group"
                        onClick={() => handleRowClick(bill)}
                      >
                        <td className="font-mono text-apple-blue">{bill.billId}</td>
                        <td className="min-w-[200px]">
                          <div className="flex items-center gap-3">
                            <CustomerAvatar name={bill.customer?.name} />
                            <div>
                              <p className="font-medium text-apple-gray-900 dark:text-white group-hover:text-apple-blue transition-colors">
                                {bill.customer?.name || t('billList.walkIn')}
                              </p>
                              <p className="text-xs text-apple-gray-500 dark:text-apple-gray-400">{bill.customer?.mobile || t('billList.noMobile')}</p>
                            </div>
                          </div>
                        </td>
                        <td className="text-right font-semibold text-[#34C759] min-w-[120px]">
                          {formatCurrency(bill.totalAmount)}
                        </td>
                        <td className="min-w-[170px]">
                          <div>
                            <p className="text-apple-gray-900 dark:text-white font-medium">{date}</p>
                            <p className="text-xs text-apple-gray-500 dark:text-apple-gray-400">{time}</p>
                          </div>
                        </td>
                        <td>
                          <span className={`inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium ${
                            bill.paymentMethod === 'upi' ? 'bg-[#AF52DE]/10 text-[#AF52DE] border border-[#AF52DE]/20' :
                            bill.paymentMethod === 'card' ? 'bg-apple-blue/10 text-apple-blue border border-apple-blue/20' :
                            bill.paymentMethod === 'credit' ? 'bg-[#FF9F0A]/15 text-[#FF9F0A] border border-[#FF9F0A]/30' :
                            'bg-apple-gray-100 text-apple-gray-700 border border-apple-gray-200 dark:bg-apple-gray-900/60 dark:text-apple-gray-300 dark:border-apple-gray-800'
                          }`}>
                            {bill.paymentMethod?.toUpperCase() || t('billList.paymentMethod')}
                          </span>
                        </td>
                        <td>
                          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-[#34C759]/15 text-[#34C759] border border-[#34C759]/30">
                            <CheckCircleIcon />
                            {t('billList.statusCompleted')}
                          </span>
                        </td>
                        <td className="text-center">
                          <div className="flex justify-center gap-2" onClick={(e) => e.stopPropagation()}>
                            <button
                              onClick={(e) => handleDownload(bill, e)}
                              disabled={downloadingId === bill.billId}
                              className="p-2 rounded-full bg-apple-blue text-white shadow-apple-md hover:shadow-apple-lg transform hover:scale-105 transition-all disabled:opacity-50"
                              title={t('billList.downloadPDF')}
                            >
                              {downloadingId === bill.billId ? (
                                <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
                              ) : (
                                <PdfIcon />
                              )}
                            </button>
                            <button
                              onClick={(e) => handleResend(bill.billId, e)}
                              disabled={resendingId === bill.billId}
                              className="p-2 rounded-full bg-[#AF52DE] text-white shadow-apple-md hover:shadow-apple-lg transform hover:scale-105 transition-all disabled:opacity-50"
                              title={t('billList.resendEmail')}
                            >
                              {resendingId === bill.billId ? (
                                <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
                              ) : (
                                <ResendIcon />
                              )}
                            </button>
                          </div>
                        </td>
                      </motion.tr>
                    );
                  })
                )}
              </AnimatePresence>
            </motion.tbody>
          </table>

          {totalPages > 1 && (
            <div className="flex justify-between items-center p-4 border-t border-apple-gray-200 dark:border-apple-gray-800/50">
              <p className="text-sm text-gray-600 dark:text-gray-400">
                {t('billList.showing', {
                  start: ((currentPage - 1) * itemsPerPage) + 1,
                  end: Math.min(currentPage * itemsPerPage, displayBills.length),
                  total: displayBills.length
                })}
              </p>
              <div className="flex gap-2">
                <button
                  onClick={() => setCurrentPage(prev => Math.max(prev - 1, 1))}
                  disabled={currentPage === 1}
                  className="px-3 py-1 rounded-full bg-apple-gray-100 dark:bg-apple-gray-800 text-apple-gray-700 dark:text-apple-gray-300 disabled:opacity-50 disabled:cursor-not-allowed hover:bg-apple-gray-200 dark:hover:bg-apple-gray-700 transition-colors"
                >
                  {t('common.previous')}
                </button>
                <span className="px-3 py-1 rounded-full bg-apple-blue text-white">
                  {currentPage}
                </span>
                <button
                  onClick={() => setCurrentPage(prev => Math.min(prev + 1, totalPages))}
                  disabled={currentPage === totalPages}
                  className="px-3 py-1 rounded-full bg-apple-gray-100 dark:bg-apple-gray-800 text-apple-gray-700 dark:text-apple-gray-300 disabled:opacity-50 disabled:cursor-not-allowed hover:bg-apple-gray-200 dark:hover:bg-apple-gray-700 transition-colors"
                >
                  {t('common.next')}
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Bill Details Drawer */}
      <BillDetailsDrawer
        bill={selectedBill}
        isOpen={isDrawerOpen}
        onClose={() => setIsDrawerOpen(false)}
        t={t}
        onDownload={handleDrawerDownload}
        onResend={handleDrawerResend}
      />
    </motion.div>
  );
}