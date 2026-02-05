// src/components/analytics/TopProductsChart.jsx
import React from 'react';
import {
  ResponsiveContainer, BarChart, Bar,
  XAxis, YAxis, CartesianGrid, Tooltip,
} from 'recharts';
import { CustomTooltip, chartTextColor } from './ChartUtils';

export default function TopProductsChart({ data }) {
  // Reverse the data so the #1 product is at the top of the chart
  const processedData = [...data].slice(0, 7).reverse(); 

  return (
    <div className="card lg:col-span-2 h-96">
      <h3 className="section-title mb-4">Top 7 Products (by Revenue)</h3>
      <div className="w-full h-[320px] min-h-[320px] min-w-0">
        <ResponsiveContainer width="100%" height="100%" minWidth={0} minHeight={320}>
          <BarChart 
            data={processedData} 
            layout="vertical" 
            margin={{ top: 5, right: 30, left: 30, bottom: 5 }}
          >
            <CartesianGrid strokeDasharray="4 6" stroke="#D2D2D7" strokeOpacity={0.6} />
            <XAxis 
              type="number" 
              stroke={chartTextColor} 
              fontSize={12} 
              tick={{ fill: chartTextColor, fontWeight: 500 }}
              tickFormatter={(value) => `₹${value.toLocaleString()}`} 
            />
            <YAxis 
              dataKey="name" 
              type="category" 
              stroke={chartTextColor} 
              fontSize={12} 
              width={100}
              tick={{ fill: chartTextColor, fontWeight: 500 }}
            />
            <Tooltip content={<CustomTooltip />} cursor={{ fill: 'rgba(0, 0, 0, 0.04)' }} />
            <Bar 
              dataKey="revenue" 
              fill="#0071E3"
              name="Revenue" 
              isAnimationActive={true}
              animationDuration={1000}
            />
          </BarChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}