// src/components/analytics/ChartUtils.jsx
import React from 'react';

/**
 * A "premium" tooltip component for our charts.
 * It matches the glassmorphic theme and formats numbers as currency.
 */
export const CustomTooltip = ({ active, payload, label }) => {
  if (active && payload && payload.length) {
    
    const value = payload[0].value;
    const formattedValue = typeof value === 'number' 
      ? value.toLocaleString('en-IN', {
          style: 'currency',
          currency: 'INR',
          maximumFractionDigits: 0,
        })
      : value;

    const name = payload[0].name;

    return (
      <div className="bg-white/90 dark:bg-apple-gray-950/90 backdrop-blur-xl border border-apple-gray-200 dark:border-apple-gray-800 p-3 rounded-2xl shadow-apple-xl">
        <p className="label text-xs uppercase tracking-[0.2em] text-apple-gray-500 dark:text-apple-gray-400 mb-1">
          {`${label || name}`}
        </p>
        <p style={{ color: payload[0].color || payload[0].payload.fill }} className="text-sm font-semibold">
          {`${payload[0].name}: ${formattedValue}`}
        </p>
      </div>
    );
  }
  return null;
};

// Define a light color for chart text (Apple gray)
export const chartTextColor = "#6e6e73";

// Colors for the Pie/Donut Chart (Apple-inspired)
export const PIE_COLORS = ['#0071E3', '#2F81F7', '#34C759', '#FF9F0A', '#BF5AF2', '#636366'];