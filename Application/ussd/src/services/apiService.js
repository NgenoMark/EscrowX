// src/services/apiService.js
const axios = require('axios');
const config = require('../config');
const logger = require('../utils/logger');

const api = axios.create({
  baseURL: config.backendUrl,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
});

/**
 * Check if user exists by phone number
 */
async function getUserByPhone(phone) {
  try {
    const response = await api.get(`/users/by-phone/${phone}`);
    logger.debug('getUserByPhone success:', response.data);
    return response.data;
  } catch (error) {
    if (error.response?.status === 404) {
      logger.debug('User not found:', phone);
      return null;
    }
    logger.error('getUserByPhone error:', error.message);
    throw error;
  }
}

/**
 * Login with email and password
 */
async function login(email, password) {
  try {
    const response = await api.post('/auth/login', { email, password });
    logger.debug('Login success for:', email);
    return response.data;
  } catch (error) {
    logger.error('Login error:', error.message);
    if (error.response?.status === 401) {
      throw new Error('Invalid credentials');
    }
    throw error;
  }
}

/**
 * Get user profile (needs token and actor ID)
 */
async function getUserProfile(userId, token) {
  const response = await api.get(`/users/${userId}`, {
    headers: {
      'Authorization': `Bearer ${token}`,
      'X-Actor-User-Id': userId
    }
  });
  return response.data;
}

/**
 * Get user's escrow balance
 */
async function getBalance(userId, token) {
  const response = await api.get('/payments/intents/me', {
    headers: {
      'Authorization': `Bearer ${token}`,
      'X-Actor-User-Id': userId
    }
  });
  return response.data;
}

/**
 * Get buyer's escrows
 */
async function getBuyerEscrows(buyerId, token) {
  const response = await api.get(`/transactions/buyer/${buyerId}`, {
    headers: {
      'Authorization': `Bearer ${token}`,
      'X-Actor-User-Id': buyerId
    }
  });
  return response.data;
}

/**
 * Get seller's escrows
 */
async function getSellerEscrows(sellerId, token) {
  const response = await api.get(`/transactions/seller/${sellerId}`, {
    headers: {
      'Authorization': `Bearer ${token}`,
      'X-Actor-User-Id': sellerId
    }
  });
  return response.data;
}

/**
 * Get escrow details by ID
 */
async function getEscrowDetails(escrowId, token, actorId) {
  const response = await api.get(`/transactions/${escrowId}`, {
    headers: {
      'Authorization': `Bearer ${token}`,
      'X-Actor-User-Id': actorId
    }
  });
  return response.data;
}

/**
 * Get escrow status history
 */
async function getEscrowHistory(escrowId, token, actorId) {
  const response = await api.get(`/transactions/${escrowId}/status-history`, {
    headers: {
      'Authorization': `Bearer ${token}`,
      'X-Actor-User-Id': actorId
    }
  });
  return response.data;
}

/**
 * Get delivery assignment details
 */
async function getDeliveryAssignment(escrowId, token, actorId) {
  const response = await api.get(`/transactions/${escrowId}/delivery-assignments`, {
    headers: {
      'Authorization': `Bearer ${token}`,
      'X-Actor-User-Id': actorId
    }
  });
  return response.data;
}

/**
 * Buyer confirms delivery (triggers auto‑release)
 */
async function buyerConfirmDelivery(escrowId, token, actorId) {
  const response = await api.post(`/transactions/${escrowId}/buyer-confirm-delivery`, {}, {
    headers: {
      'Authorization': `Bearer ${token}`,
      'X-Actor-User-Id': actorId
    }
  });
  return response.data;
}

/**
 * File a dispute
 */
async function fileDispute(data, token, actorId) {
  const response = await api.post('/disputes', data, {
    headers: {
      'Authorization': `Bearer ${token}`,
      'X-Actor-User-Id': actorId
    }
  });
  return response.data;
}

module.exports = {
  getUserByPhone,
  login,
  getUserProfile,
  getBalance,
  getBuyerEscrows,
  getSellerEscrows,
  getEscrowDetails,
  getEscrowHistory,
  getDeliveryAssignment,
  buyerConfirmDelivery,
  fileDispute
};