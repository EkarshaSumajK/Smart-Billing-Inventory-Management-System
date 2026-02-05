import React, { useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import authService from '../../services/authService';
import { useCart } from '../../context/CartContext';
import { useTheme } from '../../context/ThemeContext';
import { useTranslation } from 'react-i18next';
import i18n from '../../i18n';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  ShoppingBag,
  ShoppingCart,
  Sun,
  Moon,
  Globe,
  LogOut,
  User,
  LayoutDashboard,
  Package,
  FileText,
  Users,
  BarChart3,
  Search,
  Menu
} from 'lucide-react';

export default function Navbar() {
  const { t } = useTranslation();
  const nav = useNavigate();
  const user = authService.getUserFromToken();
  const { cartCount } = useCart();
  const { theme, toggleTheme } = useTheme();
  const [language, setLanguage] = useState(i18n.language);
  const [isMenuOpen, setIsMenuOpen] = useState(false);

  const navLinks = useMemo(() => ([
    { to: '/dashboard', label: t('nav.dashboard') },
    { to: '/products', label: t('nav.products') },
    { to: '/bills', label: t('nav.bills') },
    { to: '/customers', label: t('nav.customers') },
    { to: '/reports', label: t('nav.reports') },
  ]), [t]);

  const handleLanguageChange = (value) => {
    setLanguage(value);
    localStorage.setItem("sr_lang", value);
    i18n.changeLanguage(value);
  };

  const logout = () => {
    authService.logout();
    window.location.href = '/login';
  };

  return (
    // Apple.com style navbar: 44px height, translucent background, minimal design
    <nav className="sticky top-0 z-50 bg-white/80 dark:bg-black/70 backdrop-blur-2xl border-b border-apple-gray-200/70 dark:border-white/10">
      <div className="max-w-content mx-auto px-4 sm:px-6">
        <div className="flex justify-between items-center h-11">
          {/* LEFT: Logo */}
          <Link to="/" className="flex items-center gap-2 flex-shrink-0 -ml-2">
            <div className="w-7 h-7 flex items-center justify-center">
              <ShoppingBag className="w-5 h-5 text-apple-gray-900 dark:text-apple-gray-100" />
            </div>
            <span className="text-apple-gray-900 dark:text-apple-gray-100 text-[20px] font-semibold tracking-[-0.02em]">
              {user?.shopName || 'Store'}
            </span>
          </Link>

          {/* CENTER: Navigation (Apple style - small, clean) */}
          {user && (
            <div className="hidden md:flex items-center gap-8">
              {navLinks.map(link => (
                <Link
                  key={link.to}
                  to={link.to}
                  className="text-apple-gray-700 dark:text-apple-gray-100 text-xs tracking-[0.18em] uppercase font-medium opacity-80 hover:opacity-100 transition-opacity"
                >
                  {link.label}
                </Link>
              ))}
            </div>
          )}

          {/* RIGHT: Actions */}
          {user ? (
            <div className="flex items-center gap-3 sm:gap-4">
              {/* Search Icon */}
              <button className="hidden sm:inline-flex text-apple-gray-700 dark:text-apple-gray-100 opacity-80 hover:opacity-100 transition-opacity">
                <Search className="w-4 h-4" />
              </button>

              {/* Cart */}
              <Link to="/checkout" className="relative">
                <button className="text-apple-gray-700 dark:text-apple-gray-100 opacity-80 hover:opacity-100 transition-opacity">
                  <ShoppingCart className="w-4 h-4" />
                </button>
                {cartCount > 0 && (
                  <Badge className="absolute -top-1.5 -right-1.5 h-4 min-w-[16px] px-1 flex items-center justify-center text-[10px] bg-apple-blue text-white border-0">
                    {cartCount > 9 ? '9+' : cartCount}
                  </Badge>
                )}
              </Link>

              {/* User Menu */}
              <div className="hidden sm:block">
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <button className="text-apple-gray-700 dark:text-apple-gray-100 opacity-80 hover:opacity-100 transition-opacity">
                      <User className="w-4 h-4" />
                    </button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end" className="w-52 rounded-2xl bg-white/90 dark:bg-apple-gray-900 border-apple-gray-200 dark:border-apple-gray-800 backdrop-blur-xl shadow-apple-lg">
                    <DropdownMenuLabel className="flex flex-col">
                      <span className="text-sm font-semibold text-apple-gray-900 dark:text-apple-gray-100">{user.email}</span>
                      <span className="text-xs text-apple-gray-500">{user.shopName}</span>
                    </DropdownMenuLabel>
                    <DropdownMenuSeparator className="bg-apple-gray-200 dark:bg-apple-gray-800" />

                    {/* Language & Theme inline */}
                    <div className="px-2 py-1.5 space-y-2">
                      <div className="flex items-center justify-between">
                        <span className="text-xs text-apple-gray-500">Theme</span>
                        <button onClick={toggleTheme} className="p-1.5 rounded-full hover:bg-apple-gray-200 dark:hover:bg-apple-gray-800 transition-colors">
                          {theme === 'light' ? <Moon className="w-3.5 h-3.5" /> : <Sun className="w-3.5 h-3.5" />}
                        </button>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-xs text-apple-gray-500">Language</span>
                        <Select value={language} onValueChange={handleLanguageChange}>
                          <SelectTrigger className="w-20 h-7 text-xs border-apple-gray-300 dark:border-apple-gray-700 rounded-full">
                            <SelectValue />
                          </SelectTrigger>
                          <SelectContent>
                            <SelectItem value="en">EN</SelectItem>
                            <SelectItem value="hi">HI</SelectItem>
                            <SelectItem value="mr">MR</SelectItem>
                            <SelectItem value="te">TE</SelectItem>
                          </SelectContent>
                        </Select>
                      </div>
                    </div>

                    <DropdownMenuSeparator className="bg-apple-gray-200 dark:bg-apple-gray-800" />
                    <DropdownMenuItem onClick={logout} className="cursor-pointer text-[#FF3B30] focus:text-[#FF3B30] rounded-lg">
                      <LogOut className="w-4 h-4 mr-2" />
                      {t('nav.logout')}
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              </div>

              <button
                onClick={() => setIsMenuOpen(prev => !prev)}
                className="inline-flex md:hidden text-apple-gray-700 dark:text-apple-gray-100 opacity-80 hover:opacity-100 transition-opacity"
                aria-label="Toggle menu"
                aria-expanded={isMenuOpen}
                aria-controls="mobile-nav"
              >
                <Menu className="w-5 h-5" />
              </button>
            </div>
          ) : (
            <Link to="/login">
              <Button className="h-8 px-4 bg-apple-blue hover:bg-apple-blue-light text-white text-xs rounded-full">
                {t('nav.login')}
              </Button>
            </Link>
          )}
        </div>

        {user && (
          <div
            id="mobile-nav"
            className={`md:hidden border-t border-apple-gray-200/70 dark:border-white/10 ${isMenuOpen ? 'block' : 'hidden'}`}
          >
            <div className="flex flex-col gap-3 py-4">
              {navLinks.map(link => (
                <Link
                  key={link.to}
                  to={link.to}
                  onClick={() => setIsMenuOpen(false)}
                  className="text-apple-gray-800 dark:text-apple-gray-100 text-sm font-semibold tracking-wide"
                >
                  {link.label}
                </Link>
              ))}

              <div className="pt-3 border-t border-apple-gray-200/70 dark:border-white/10 flex items-center justify-between">
                <span className="text-xs text-apple-gray-500">Theme</span>
                <button onClick={toggleTheme} className="p-2 rounded-full hover:bg-apple-gray-200 dark:hover:bg-apple-gray-800 transition-colors">
                  {theme === 'light' ? <Moon className="w-4 h-4" /> : <Sun className="w-4 h-4" />}
                </button>
              </div>

              <div className="flex items-center justify-between">
                <span className="text-xs text-apple-gray-500">Language</span>
                <Select value={language} onValueChange={handleLanguageChange}>
                  <SelectTrigger className="w-24 h-8 text-xs border-apple-gray-300 dark:border-apple-gray-700 rounded-full">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="en">EN</SelectItem>
                    <SelectItem value="hi">HI</SelectItem>
                    <SelectItem value="mr">MR</SelectItem>
                    <SelectItem value="te">TE</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <button
                onClick={logout}
                className="text-left text-sm font-semibold text-[#FF3B30]"
              >
                {t('nav.logout')}
              </button>
            </div>
          </div>
        )}
      </div>
    </nav>
  );
}