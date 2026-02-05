import api from '../api/apiClient';
import jwtDecode from 'jwt-decode';
import i18n from '../i18n'; // <-- Import i18n instance

// LocalStorage key constants
const ACCESS_KEY = 'sr_access';
const REFRESH_KEY = 'sr_refresh';

/**
 * Save tokens to localStorage
 */
function saveTokens({ accessToken, refreshToken }) {
  if (accessToken) localStorage.setItem(ACCESS_KEY, accessToken);
  if (refreshToken) localStorage.setItem(REFRESH_KEY, refreshToken);
}

/**
 * Get stored tokens
 */
function getAccessToken() {
  return localStorage.getItem(ACCESS_KEY);
}
function getRefreshToken() {
  return localStorage.getItem(REFRESH_KEY);
}

/**
 * Clear tokens from storage (logout)
 */
function clearTokens() {
  localStorage.removeItem(ACCESS_KEY);
  localStorage.removeItem(REFRESH_KEY);
}

/**
 * Authenticate user with backend
 * POST /api/auth/login
 */
async function login({ email, password }) {
  const res = await api.post('/api/auth/login', { email, password });
  const payload = res.data;

  if (payload?.accessToken) {
    saveTokens({
      accessToken: payload.accessToken,
      refreshToken: payload.refreshToken || ''
    });
  } else if (payload?.token) {
    saveTokens({
      accessToken: payload.token,
      refreshToken: payload.refreshToken || ''
    });
  } else {
    // Use translated error
    throw new Error(i18n.t('errors.noToken'));
  }

  return payload;
}

/**
 * Create a new user account
 * POST /api/auth/signup
 */
async function signup(req) {
  const res = await api.post('/api/auth/signup', req);
  return res.data;
}

/**
 * Refresh JWT using refresh token
 * POST /api/auth/refresh
 */
async function refresh() {
  const refreshToken = getRefreshToken();
  if (!refreshToken) throw new Error(i18n.t('errors.noRefresh'));

  const res = await api.post('/api/auth/refresh', refreshToken, {
    headers: { 'Content-Type': 'text/plain' }
  });

  const newAccess = res.data?.accessToken;
  if (newAccess) {
    saveTokens({ accessToken: newAccess });
    return { accessToken: newAccess };
  }

  throw new Error(i18n.t('errors.refreshFailed'));
}

/**
 * Decode JWT and return structured user info
 */
function getUserFromToken() {
  const token = getAccessToken();
  if (!token) return null;

  try {
    const decoded = jwtDecode(token);
    const email = decoded.sub || decoded.email || "unknown@domain.com";
    const role = decoded.role || (decoded.roles ? decoded.roles[0] : "CUSTOMER");

    return {
      email,
      name: email.split("@")[0],
      role: role.toUpperCase(),
      shopId: decoded.shopId,
      shopName: decoded.shopName,
      exp: decoded.exp,
      iat: decoded.iat,
    };
  } catch (err) {
    console.error("JWT decode error:", err);
    return null;
  }
}

function getUserRole() {
  const token = getAccessToken();
  if (!token) return null;
  try {
    const decoded = jwtDecode(token);
    return decoded.role || (decoded.roles ? decoded.roles[0] : null);
  } catch (err) {
    console.error('Error decoding JWT for role:', err);
    return null;
  }
}

/**
 * Logout user
 */
function logout() {
  clearTokens();
}

export default {
  login,
  signup,
  refresh,
  saveTokens,
  getAccessToken,
  getRefreshToken,
  clearTokens,
  getUserFromToken,
  logout,
  getUserRole
};