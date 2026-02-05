import React from 'react';
import { Outlet } from 'react-router-dom';
import Navbar from './Navbar';

/**
 * This layout wraps all protected pages *after* login.
 * It includes the main Navbar and the centered, padded content area.
 */
export default function MainLayout() {
  return (
    <div className="page-shell transition-colors duration-200">
      <Navbar />
      <main className="page">
        {/* All protected child routes will render here */}
        <Outlet />
      </main>
    </div>
  );
}