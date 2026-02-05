import React from 'react';
import { LineChart, Line, ResponsiveContainer } from 'recharts';
import { RevenueIcon, OrdersIcon, ProductsSoldIcon } from './icons';

const iconMap = {
  revenue: <RevenueIcon />,
  orders: <OrdersIcon />,
  products: <ProductsSoldIcon />,
};

export function StatCard({
  title,
  value,
  change,
  icon,
  sparkData,
  sparkKey,
}) {
  const isUp = change ? parseFloat(change) >= 0 : true;
  const trendClr = isUp ? 'text-[#40C057]' : 'text-[#FA5252]';
  const sparkClr = isUp ? '#40C057' : '#FA5252';

  return (
    <div className="group card border-apple-gray-200 dark:border-apple-gray-800 p-5 rounded-xl shadow-apple-md transition-all hover:shadow-apple-lg hover:-translate-y-1">
      <div className="flex items-center gap-4">
        <div className="p-3 bg-apple-gray-100 dark:bg-apple-gray-900/70 rounded-lg group-hover:scale-110 transition-transform">
          {iconMap[icon]}
        </div>

        <div className="flex-1">
          <p className="text-xs font-semibold text-apple-gray-600 dark:text-apple-gray-400">{title}</p>
          <p className="text-2xl font-semibold text-apple-gray-900 dark:text-white mt-2">{value}</p>
        </div>
      </div>

      <div className="flex items-end justify-between mt-5">
        {change && (
          <p className={`flex items-center text-base font-semibold ${trendClr}`}>
            {isUp ? '▲' : '▼'} {change}
          </p>
        )}

        {sparkData && sparkData.length > 0 && (
          <div className="w-28 h-12 min-w-[112px] min-h-[48px]">
            <ResponsiveContainer width="100%" height="100%" minWidth={0} minHeight={48}>
              <LineChart data={sparkData} margin={{ top: 5, right: 0, left: 0, bottom: 5 }}>
                <Line
                  type="monotone"
                  dataKey={sparkKey}
                  stroke={sparkClr}
                  strokeWidth={2.5}
                  dot={false}
                  animationDuration={800}
                />
              </LineChart>
            </ResponsiveContainer>
          </div>
        )}
      </div>
    </div>
  );
}