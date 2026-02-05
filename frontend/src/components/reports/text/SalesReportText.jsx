import React, { useEffect, useState } from "react";
import api from "../../../api/apiClient";
import { useTranslation } from 'react-i18next'; // <-- Import

export default function SalesReportText() {
  const { t } = useTranslation(); // <-- Get hook
  const [textReport, setTextReport] = useState("");
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function fetchTextReport() {
      try {
        setLoading(true);
        const res = await api.get("/api/analytics/report/text");

        const reportData =
          typeof res.data === "object" && res.data.report
            ? res.data.report
            : res.data;

        setTextReport(reportData || "No report data available");
      } catch (err) {
        console.error("Error fetching text report:", err);
        setError("Failed to load text report. Is the analytics service running?");
      } finally {
        setLoading(false);
      }
    }
    fetchTextReport();
  }, []);

  if (loading) {
    return (
      <div className="card p-6 text-center text-apple-gray-500 dark:text-apple-gray-400">
        Loading text report...
      </div>
    );
  }

  if (error) {
    return <div className="card p-6 text-red-500 dark:text-red-400">{error}</div>;
  }

  return (
    // UPDATED: Theme-aware classes
    <div className="card">
      <h3 className="section-title p-6 pb-0">{t('reports.placeholder2')}</h3>
      <div className="p-6">
        <pre className="bg-apple-gray-100 dark:bg-apple-gray-900 p-4 rounded-2xl text-sm text-apple-gray-700 dark:text-apple-gray-300 whitespace-pre-wrap overflow-auto">
          {textReport}
        </pre>
      </div>
    </div>
  );
}