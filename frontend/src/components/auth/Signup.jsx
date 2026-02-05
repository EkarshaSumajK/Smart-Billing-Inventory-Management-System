import React, { useState } from 'react';
import authService from '../../services/authService';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardHeader } from '@/components/ui/card';
import { AlertCircle, ShoppingBag } from 'lucide-react';

export default function Signup() {
  const { t } = useTranslation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [shopName, setShopName] = useState('');
  const [role, setRole] = useState('OWNER');
  const [error, setError] = useState(null);
  const nav = useNavigate();

  async function submit(e) {
    e.preventDefault();
    setError(null);
    try {
      await authService.signup({ name, email, password, role, shopName });
      alert('Account created successfully! Please sign in.');
      nav('/login');
    } catch (err) {
      setError(err.response?.data?.message || err.response?.data?.error || 'Signup failed');
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-apple-gray-50 dark:bg-apple-gray-950 px-4 py-12">
      <div className="w-full max-w-md space-y-8">
        {/* App Logo */}
        <div className="text-center space-y-3">
          <div className="flex items-center justify-center">
            <div className="w-16 h-16 rounded-xl bg-apple-blue flex items-center justify-center shadow-apple-md">
              <ShoppingBag className="h-9 w-9 text-white" strokeWidth={2} />
            </div>
          </div>
          <h1 className="text-3xl font-semibold text-apple-gray-900 dark:text-white tracking-tight">
            Smart Retail
          </h1>
        </div>

        {/* Main Signup Card */}
        <Card className="border-apple-gray-200 shadow-apple-lg rounded-xl overflow-hidden bg-white dark:bg-apple-gray-900/80">
          <CardHeader className="space-y-2 pb-6 pt-8 px-8">
            <h2 className="text-2xl font-semibold text-center text-apple-gray-900 dark:text-white">
              Create Account
            </h2>
            <p className="text-center text-sm text-apple-gray-600 dark:text-apple-gray-400">
              Join Smart Retail and start selling
            </p>
          </CardHeader>
          <CardContent className="space-y-5 px-8 pb-8">
            {error && (
              <div className="flex items-start gap-3 p-4 bg-red-50 dark:bg-red-950/30 border-l-4 border-red-500 rounded-lg">
                <AlertCircle className="h-5 w-5 text-red-600 dark:text-red-400 mt-0.5 shrink-0" />
                <div className="text-sm text-red-800 dark:text-red-200">
                  <div className="font-medium">{error}</div>
                </div>
              </div>
            )}

            <form onSubmit={submit} className="space-y-4">
              {/* Shop Name */}
              <div className="space-y-2">
                <Label htmlFor="shopName" className="text-sm font-medium text-apple-gray-700 dark:text-apple-gray-300">
                  Shop Name
                </Label>
                <Input
                  id="shopName"
                  type="text"
                  value={shopName}
                  onChange={e => setShopName(e.target.value)}
                  required
                  placeholder="My Awesome Shop"
                  className="h-10 rounded-md border-apple-gray-300 dark:border-apple-gray-700 bg-white dark:bg-apple-gray-900 
                           focus:border-apple-blue focus:ring-apple-blue/30 focus:ring-2 text-sm
                           placeholder:text-apple-gray-400"
                />
              </div>

              {/* Full Name */}
              <div className="space-y-2">
                <Label htmlFor="name" className="text-sm font-medium text-apple-gray-700 dark:text-apple-gray-300">
                  Full Name
                </Label>
                <Input
                  id="name"
                  type="text"
                  value={name}
                  onChange={e => setName(e.target.value)}
                  required
                  placeholder={t('auth.namePlaceholder')}
                  className="h-10 rounded-md border-apple-gray-300 dark:border-apple-gray-700 bg-white dark:bg-apple-gray-900
                           focus:border-apple-blue focus:ring-apple-blue/30 focus:ring-2 text-sm
                           placeholder:text-apple-gray-400"
                />
              </div>

              {/* Email */}
              <div className="space-y-2">
                <Label htmlFor="email" className="text-sm font-medium text-apple-gray-700 dark:text-apple-gray-300">
                  Email
                </Label>
                <Input
                  id="email"
                  type="email"
                  value={email}
                  onChange={e => setEmail(e.target.value)}
                  required
                  placeholder="name@example.com"
                  className="h-10 rounded-md border-apple-gray-300 dark:border-apple-gray-700 bg-white dark:bg-apple-gray-900
                           focus:border-apple-blue focus:ring-apple-blue/30 focus:ring-2 text-sm
                           placeholder:text-apple-gray-400"
                />
              </div>

              {/* Password */}
              <div className="space-y-2">
                <Label htmlFor="password" className="text-sm font-medium text-apple-gray-700 dark:text-apple-gray-300">
                  Password
                </Label>
                <Input
                  id="password"
                  type="password"
                  value={password}
                  onChange={e => setPassword(e.target.value)}
                  required
                  placeholder="At least 8 characters"
                  className="h-10 rounded-md border-apple-gray-300 dark:border-apple-gray-700 bg-white dark:bg-apple-gray-900
                           focus:border-apple-blue focus:ring-apple-blue/30 focus:ring-2 text-sm
                           placeholder:text-apple-gray-400"
                />
              </div>

              {/* Role */}
              <div className="space-y-2">
                <Label htmlFor="role" className="text-sm font-medium text-apple-gray-700 dark:text-apple-gray-300">
                  Role
                </Label>
                <select
                  id="role"
                  value={role}
                  onChange={e => setRole(e.target.value)}
                  className="w-full h-10 px-3 border border-apple-gray-300 dark:border-apple-gray-700 bg-white dark:bg-apple-gray-900 
                           text-apple-gray-900 dark:text-white rounded-md text-sm
                           focus:outline-none focus:ring-2 focus:ring-apple-blue/30 focus:border-apple-blue
                           appearance-none cursor-pointer transition-colors
                           bg-[url('data:image/svg+xml;charset=UTF-8,%3csvg%20width%3d%2220%22%20height%3d%2220%22%20viewBox%3d%220%200%2020%2020%22%20fill%3d%22none%22%20xmlns%3d%22http%3a%2f%2fwww.w3.org%2f2000%2fsvg%22%3e%3cpath%20d%3d%22M5%207.5L10%2012.5L15%207.5%22%20stroke%3d%22%23475569%22%20stroke-width%3d%222%22%20stroke-linecap%3d%22round%22%20stroke-linejoin%3d%22round%22%2f%3e%3c%2fsvg%3e')]
                           bg-no-repeat bg-[center_right_1rem]"
                >
                  <option value="OWNER">Owner</option>
                  <option value="MANAGER">Manager</option>
                  <option value="CASHIER">Cashier</option>
                </select>
              </div>

              {/* Create Account Button */}
              <Button
                type="submit"
                className="w-full h-10 bg-apple-blue hover:bg-apple-blue-light 
                         text-white font-semibold rounded-md shadow-apple-md
                         transition-colors duration-200 text-sm mt-2"
              >
                Create Account
              </Button>
            </form>

            {/* Privacy Text */}
            <div className="text-center text-xs text-apple-gray-500 dark:text-apple-gray-400 pt-2">
              By creating an account, you agree to our{' '}
              <a href="#" className="text-apple-blue hover:underline">
                Terms
              </a>{' '}
              and{' '}
              <a href="#" className="text-apple-blue hover:underline">
                Privacy Policy
              </a>
            </div>
          </CardContent>
        </Card>

        {/* Sign In Link */}
        <div className="text-center">
          <span className="text-sm text-apple-gray-600 dark:text-apple-gray-400">
            Already have an account?{' '}
          </span>
          <button
            onClick={() => nav('/login')}
            className="text-sm font-semibold text-apple-blue hover:text-apple-blue-light transition-colors"
          >
            Sign In
          </button>
        </div>

        {/* Footer */}
        <div className="text-center pt-6 space-y-3 border-t border-apple-gray-200 dark:border-apple-gray-800">
          <div className="flex justify-center gap-6 text-xs font-medium">
            <a href="#" className="text-apple-gray-600 dark:text-apple-gray-400 hover:text-apple-blue transition-colors">
              Terms
            </a>
            <a href="#" className="text-apple-gray-600 dark:text-apple-gray-400 hover:text-apple-blue transition-colors">
              Privacy
            </a>
            <a href="#" className="text-apple-gray-600 dark:text-apple-gray-400 hover:text-apple-blue transition-colors">
              Support
            </a>
          </div>
          <div className="text-xs text-apple-gray-500 dark:text-apple-gray-500">
            © 2026 Smart Retail Inc. All rights reserved.
          </div>
        </div>
      </div>
    </div>
  );
}