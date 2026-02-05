// src/components/analytics/SalesByCategoryChart.jsx
import React, { useMemo } from 'react';
import { ResponsiveContainer, PieChart, Pie, Cell, Tooltip } from 'recharts';
import { CustomTooltip, PIE_COLORS } from './ChartUtils';
import { motion } from 'framer-motion';

// This component calculates the total value to display in the center of the donut
const DonutCenterLabel = ({ total }) => {
  return (
    <>
      <text x="50%" y="45%" textAnchor="middle" dominantBaseline="middle"
        className="text-xs uppercase tracking-[0.2em] fill-apple-gray-500 dark:fill-apple-gray-400">
        Total
      </text>
      <text x="50%" y="58%" textAnchor="middle" dominantBaseline="middle"
        className="font-display text-3xl font-semibold fill-apple-gray-900 dark:fill-white">
        {total.toLocaleString('en-IN', {
          style: 'currency',
          currency: 'INR',
          maximumFractionDigits: 0,
        })}
      </text>
    </>
  );
};

export default function SalesByCategoryChart({ data }) {
  // Process the top_products data to get sales by category
  const { categoryData, totalRevenue } = useMemo(() => {
    if (!data) return { categoryData: [], totalRevenue: 0 };
    
    const categoryMap = data.reduce((acc, product) => {
      const category = product.category || 'Unknown';
      const rawRevenue = product.revenue ?? 0;
      const revenue = typeof rawRevenue === 'number'
        ? rawRevenue
        : Number(String(rawRevenue).replace(/,/g, '')) || 0;
      acc[category] = (acc[category] || 0) + revenue;
      return acc;
    }, {});
    
    const total = Object.values(categoryMap).reduce((sum, val) => sum + val, 0);

    const chartData = Object.keys(categoryMap).map((name, index) => ({
      name,
      value: categoryMap[name],
      percent: total > 0 ? (categoryMap[name] / total) * 100 : 0,
      color: PIE_COLORS[index % PIE_COLORS.length],
    })).sort((a, b) => b.value - a.value);

    return { categoryData: chartData, totalRevenue: total };
  }, [data]);

  const hasData = categoryData.some(entry => entry.value > 0);

  return (
    <div className="card h-[450px] lg:col-span-4 flex flex-col">
      <h3 className="section-title mb-4 flex-shrink-0">Sales by Category</h3>
      <div className="w-full flex-grow min-w-0 h-[260px] min-h-[260px]">
        {hasData ? (
          <ResponsiveContainer width="100%" height="100%" minWidth={0} minHeight={260}>
            <PieChart margin={{ top: 8, right: 8, bottom: 8, left: 8 }}>
              {/* SVG Filter for the glow effect */}
              <defs>
                <filter id="glow" x="-50%" y="-50%" width="200%" height="200%">
                  <feDropShadow dx="0" dy="0" stdDeviation="3" floodColor="#228BE6" floodOpacity="0.25" />
                </filter>
              </defs>
              <Pie
                data={categoryData}
                dataKey="value"
                nameKey="name"
                cx="50%"
                cy="50%"
                innerRadius={60}
                outerRadius={100}
                fill="#8884d8"
                paddingAngle={2}
                isAnimationActive={true}
                animationDuration={1000}
              >
                {categoryData.map((entry) => (
                  <Cell 
                    key={entry.name} 
                    fill={entry.color} 
                    stroke={entry.color}
                    strokeWidth={1}
                    style={{ filter: 'url(#glow)' }}
                  />
                ))}
              </Pie>
              <Tooltip content={<CustomTooltip />} />
              {/* Custom label in the center */}
              <DonutCenterLabel total={totalRevenue} />
            </PieChart>
          </ResponsiveContainer>
        ) : (
          <div className="h-full w-full flex items-center justify-center text-sm text-apple-gray-500">
            No category data yet
          </div>
        )}
      </div>
      {/* Custom Legend */}
      <div className="w-full space-y-2 overflow-y-auto custom-scrollbar pt-4 mt-4 border-t border-apple-gray-200 dark:border-apple-gray-800">
        {categoryData.map((entry) => (
          <div key={entry.name} className="flex items-center justify-between text-sm">
            <div className="flex items-center gap-2">
              <span className="w-3 h-3 rounded-full" style={{ backgroundColor: entry.color }} />
              <span className="text-apple-gray-700 dark:text-apple-gray-300 font-medium">{entry.name}</span>
            </div>
            <span className="font-semibold text-apple-gray-900 dark:text-white">{entry.percent.toFixed(1)}%</span>
          </div>
        ))}
      </div>
    </div>
  );
}