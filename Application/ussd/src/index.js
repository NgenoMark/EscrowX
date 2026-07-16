// src/index.js
require('dotenv').config();
const app = require('./server');

const PORT = process.env.PORT || 8080;

app.listen(PORT, () => {
  console.log(`🚀 USSD Service running on port ${PORT}`);
  console.log(`📡 Backend URL: ${process.env.BACKEND_URL}`);
});