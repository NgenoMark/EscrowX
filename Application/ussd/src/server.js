// src/server.js
const express = require('express');
const ussdService = require('./services/ussdService');
const logger = require('./utils/logger');

const app = express();

// Middleware – Africa's Talking sends form-urlencoded data
app.use(express.urlencoded({ extended: true }));
app.use(express.json());

// Health check
app.get('/health', (req, res) => {
  res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

// USSD webhook endpoint
app.post('/api/v1/ussd/callback', async (req, res) => {
  logger.info('📥 Incoming USSD request:', req.body);
  
  try {
    const { sessionId, phoneNumber, userInput, level } = req.body;
    
    const response = await ussdService.process({
      sessionId,
      phoneNumber,
      userInput: userInput || '',
      level: parseInt(level) || 0
    });
    
    logger.info('📤 Sending response:', response.substring(0, 50) + '...');
    res.set('Content-Type', 'text/plain');
    res.send(response);
  } catch (error) {
    logger.error('❌ USSD error:', error.message);
    res.send('END An error occurred. Please try again.');
  }
});

module.exports = app;