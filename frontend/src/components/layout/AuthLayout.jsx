import React from 'react';
import { Outlet } from 'react-router-dom';

/**
 * This layout wraps all public auth pages (Login, Signup).
 * It's a simple wrapper to allow these pages to control the full screen.
 */
export default function AuthLayout() {
  return (
    <div className="page-shell transition-colors duration-200">
      {/* Auth pages (Login, Signup) will render here */}
      <Outlet />
    </div>
  );
}