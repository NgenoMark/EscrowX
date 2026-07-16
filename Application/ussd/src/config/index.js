// src/config/index.js
require('dotenv').config();

module.exports = {
  port: process.env.PORT || 8080,
  backendUrl: process.env.BACKEND_URL,
  env: process.env.NODE_ENV || 'development'
};