import React from "react";
import authService from "../../services/authService";
import { useTranslation } from 'react-i18next'; // <-- NEW IMPORT

export default function ProfilePage() {
  const { t } = useTranslation(); // <-- NEW
  const user = authService.getUserFromToken();

  if (!user) {
    return (
      <div className="page-section max-w-md mx-auto text-center">
        <p className="text-red-500">{t('profile.notLoggedIn')}</p>
      </div>
    );
  }

  return (
    <div className="space-y-12">
      <div className="page-hero max-w-2xl mx-auto">
        <div className="page-hero-content">
          <div>
            <p className="page-hero-eyebrow">Account</p>
            <h2 className="page-hero-title">{t('profile.title')}</h2>
            <p className="page-hero-subtitle">Manage your account details</p>
          </div>
        </div>
      </div>

      <div className="page-section max-w-2xl mx-auto">
        <div className="space-y-4">
          <div className="p-4 bg-apple-gray-100 dark:bg-apple-gray-900/60 rounded-2xl">
            <label className="block text-sm font-medium text-apple-gray-500">{t('profile.email')}</label>
            <p className="text-lg text-apple-gray-900 dark:text-white">{user.email}</p>
          </div>
          <div className="p-4 bg-apple-gray-100 dark:bg-apple-gray-900/60 rounded-2xl">
            <label className="block text-sm font-medium text-apple-gray-500">{t('profile.role')}</label>
            <p className="text-lg text-apple-gray-900 dark:text-white font-semibold">{user.role || "N/A"}</p>
          </div>
        </div>

        <p className="mt-6 text-apple-gray-500 text-xs text-center">
          {t('profile.tokenIssued')}: {new Date(user.iat * 1000).toLocaleString()}
          <br />
          {t('profile.tokenExpires')}: {new Date(user.exp * 1000).toLocaleString()}
        </p>
      </div>
    </div>
  );
}