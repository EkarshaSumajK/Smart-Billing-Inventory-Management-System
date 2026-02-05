import React from 'react';
import {
  ResponsiveContainer, AreaChart, Area, XAxis, YAxis,
  CartesianGrid, Tooltip, Legend,
} from 'recharts';
import { CustomTooltip, chartTextColor } from './ChartUtils';

export default function RevenueTrendChart({ data = [] }) {
  return (
    <div className="card rounded-xl p-6 shadow-apple-lg h-[420px]">
      <h3 className="section-title mb-5">Revenue Trend</h3>

      <div className="w-full h-[320px] min-h-[320px] min-w-0">
        <ResponsiveContainer width="100%" height="100%" minWidth={0} minHeight={320}>
          <AreaChart data={data} margin={{ top: 10, right: 30, left: 0, bottom: 0 }}>
          <defs>
            <linearGradient id="gradRevenue" x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%" stopColor="#228BE6" stopOpacity={0.35} />
              <stop offset="95%" stopColor="#228BE6" stopOpacity={0.02} />
            </linearGradient>
          </defs>

          <CartesianGrid strokeDasharray="4 6" stroke="#DEE2E6" strokeOpacity={0.6} />
          <XAxis dataKey="day" stroke={chartTextColor} tick={{ fontSize: 12, fontWeight: 500, fill: chartTextColor }} />
          <YAxis
            stroke={chartTextColor}
            tick={{ fontSize: 12, fontWeight: 500, fill: chartTextColor }}
            tickFormatter={v => `₹${(v / 1_000).toFixed(0)}k`}
          />
          <Tooltip content={<CustomTooltip />} />
          <Legend wrapperStyle={{ paddingTop: '12px', color: chartTextColor, fontSize: 12 }} />

          <Area
            type="monotone"
            dataKey="totalRevenue"
            stroke="#228BE6"
            fill="url(#gradRevenue)"
            strokeWidth={3}
            name="Revenue"
            dot={false}
            animationDuration={1200}
          />
          </AreaChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}