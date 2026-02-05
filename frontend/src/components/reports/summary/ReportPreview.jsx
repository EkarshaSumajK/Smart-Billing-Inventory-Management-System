import React from 'react';
import { motion } from 'framer-motion';
import { useTranslation } from 'react-i18next'; // <-- Import

// --- Formatting Helpers ---
const formatCurrency = (amount) => {
  if (amount === null || amount === undefined) return 'N/A';
  return (amount).toLocaleString('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  });
};

const formatNumber = (num) => {
  if (num === null || num === undefined) return 'N/A';
  return (num).toLocaleString('en-IN');
};

// --- Sub-Components for the Report ---
const MetricBox = ({ title, value, className = 'text-apple-blue' }) => (
  <div className="bg-white border border-apple-gray-200 p-4 rounded-lg shadow-apple-sm">
    <p className="text-xs font-semibold text-apple-gray-500">{title}</p>
    <p className={`text-2xl font-semibold ${className}`}>{value}</p>
  </div>
);

const ReportTable = ({ headers, data, title }) => (
  <section className="space-y-3">
    <h3 className="text-lg font-semibold text-apple-gray-900 border-b border-apple-gray-200 pb-2">{title}</h3>
    <div className="overflow-x-auto border border-apple-gray-200 rounded-lg">
      <table className="min-w-full divide-y divide-apple-gray-200">
        <thead className="bg-apple-gray-100">
          <tr>
            {headers.map(header => (
              <th key={header} className="px-6 py-3 text-left text-xs font-semibold text-apple-gray-600">
                {header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="bg-white divide-y divide-apple-gray-200">
          {data.length === 0 ? (
            <tr>
              <td colSpan={headers.length} className="px-6 py-4 text-center text-apple-gray-500">
                No data available for this section.
              </td>
            </tr>
          ) : (
            data.map((row, rowIndex) => (
              <tr key={rowIndex} className={rowIndex % 2 === 0 ? 'bg-white' : 'bg-apple-gray-50/70'}>
                {headers.map((header, colIndex) => (
                  <td key={colIndex} className="px-6 py-4 whitespace-nowrap text-sm text-apple-gray-700">
                    {row[header]}
                  </td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  </section>
);

// --- Main Report Preview Component ---
export default function ReportPreview({ reportData, startDate, endDate }) {
  const { t } = useTranslation(); // <-- Get hook
  if (!reportData) return null;

  const { salesReport, textSummary, inventoryReport } = reportData;
  const { summary } = salesReport || {};

  // Format data for tables
  const topProductsData = (salesReport?.top_products || []).map(p => ({
    "Product Name": p.name,
    "Category": p.category || 'N/A',
    "Units Sold": formatNumber(p.qty),
    "Total Revenue": formatCurrency(p.revenue),
  }));

  // No change to lowStockData, it's not rendered

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0 }}
      transition={{ duration: 0.5 }}
      // This component STAYS light-themed
      className="bg-white text-apple-gray-900 max-w-4xl mx-auto rounded-xl shadow-apple-lg p-8 md:p-12 border border-apple-gray-200"
      id="report-preview-content"
    >
      <div className="space-y-12">
        {/* 1. Header */}
        <header className="text-center border-b border-apple-gray-200 pb-6">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-apple-gray-100 text-apple-gray-700 text-xs font-semibold">
            {t('reports.title')}
          </div>
          <h1 className="text-3xl font-semibold text-apple-gray-900 mt-3">Full Report</h1>
          <p className="text-base text-apple-gray-600 mt-2">
            {startDate} to {endDate}
          </p>
        </header>

        {/* 2. AI Summary */}
        {textSummary?.report && (
          <section>
            <h2 className="text-xl font-semibold text-apple-gray-900 mb-3">{t('reports.placeholder2')}</h2>
            <blockquote className="text-apple-gray-700 text-base leading-relaxed italic border-l-4 border-apple-blue pl-4">
              {textSummary.report}
            </blockquote>
          </section>
        )}

        {/* 3. Key Metrics */}
        <section>
          <h2 className="text-xl font-semibold text-apple-gray-900 mb-4">Key Metrics</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <MetricBox
              title={t('dashboard.totalRevenue')}
              value={formatCurrency(summary?.total_revenue)}
              className="text-[#40C057]"
            />
            <MetricBox
              title={t('dashboard.totalOrders')}
              value={formatNumber(summary?.total_orders)}
              className="text-apple-blue"
            />
            <MetricBox
              title={t('dashboard.productsSold')}
              value={formatNumber(summary?.total_products_sold)}
              className="text-[#5C7CFA]"
            />
          </div>
        </section>

        {/* 4. Inventory Alerts */}
        <section>
          <h2 className="text-xl font-semibold text-apple-gray-900 mb-4">Inventory Alerts</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <MetricBox
              title={t('customerList.showingLowStock')}
              value={formatNumber(inventoryReport?.lowStockCount)}
              className="text-[#FAB005]"
            />
            <MetricBox
              title="Expiring Soon Items"
              value={formatNumber(inventoryReport?.expiringCount)}
              className="text-[#FAB005]"
            />
          </div>
        </section>

        {/* 5. Top Products Table */}
        <ReportTable
          title={t('dashboard.topProducts')}
          headers={["Product Name", "Category", "Units Sold", "Total Revenue"]}
          data={topProductsData}
        />

        {/* 6. Low Stock Details Table (Removed as per your file) */}
      </div>
    </motion.div>
  );
}