// src/services/ussdService.js
const apiService = require('./apiService');
const sessionService = require('./sessionService');
const menus = require('../constants/menus');
const logger = require('../utils/logger');

/**
 * Main USSD processing function
 */
async function process({ sessionId, phoneNumber, userInput, level }) {
  logger.debug(`Processing USSD: session=${sessionId}, phone=${phoneNumber}, input="${userInput}", level=${level}`);

  // 1. Get or create session
  let session = sessionService.getOrCreateSession(sessionId, phoneNumber);

  // Store current input in session for multi-step handlers
  session.input = userInput;

  // 2. Check if user exists
  let user = null;
  try {
    const userResponse = await apiService.getUserByPhone(phoneNumber);
    if (userResponse && userResponse.id) {
      user = userResponse;
    }
  } catch (error) {
    logger.error('Error checking user:', error.message);
    return 'END System error. Please try again later.';
  }

  // 3. If user not registered, show registration options
  if (!user) {
    return handleUnregistered(level, userInput, session);
  }

  // 4. If user is not logged in, show login
  if (!session.loggedIn) {
    return await handleLogin(level, userInput, session, user);
  }

  // 5. User is logged in – show main menu based on role
  return await handleMainMenu(level, userInput, session, user);
}

/**
 * Handle unregistered user
 */
function handleUnregistered(level, input, session) {
  if (level === 0) {
    session.state = 'UNREGISTERED';
    return menus.unregisteredMenu;
  }

  switch (input) {
    case '1':
      session.state = 'LOGIN_EMAIL';
      // Clear any previous input
      session.input = '';
      return menus.loginEmail;
    case '2':
      sessionService.deleteSession(session.sessionId);
      return menus.downloadApp;
    case '0':
      sessionService.deleteSession(session.sessionId);
      return menus.exit;
    default:
      return menus.invalidOption(menus.unregisteredMenu);
  }
}

/**
 * Handle login flow
 */
async function handleLogin(level, input, session, user) {
  if (session.state === 'LOGIN_EMAIL') {
    session.state = 'LOGIN_PASSWORD';
    session.pendingEmail = input;
    session.input = '';
    return menus.loginPassword;
  }

  if (session.state === 'LOGIN_PASSWORD') {
    const email = session.pendingEmail;
    const password = input;
    try {
      const loginResponse = await apiService.login(email, password);
      if (loginResponse && loginResponse.userId) {
        const token = loginResponse.token || loginResponse.accessToken;
        if (!token) {
          throw new Error('No token received');
        }
        session.loggedIn = true;
        session.token = token;
        session.userData = loginResponse.user || { id: loginResponse.userId };
        // Get full profile
        const profile = await apiService.getUserProfile(session.userData.id, token);
        session.userData = profile;
        session.state = 'MAIN';
        session.input = '';
        return menus.mainMenu(session.userData.displayName, session.userData.role);
      } else {
        return 'END Invalid credentials. Please try again later.';
      }
    } catch (error) {
      logger.error('Login error:', error.message);
      return 'END Invalid email or password. Please try again.';
    }
  }

  // Fallback
  return menus.invalidOption(menus.mainMenu('User', ''));
}

/**
 * Handle main menu (logged in)
 */
async function handleMainMenu(level, input, session, user) {
  // If it's the first interaction (level 0), show main menu
  if (level === 0) {
    session.state = 'MAIN';
    session.input = '';
    return menus.mainMenu(user.displayName, user.role);
  }

  // Process user's selection
  switch (input) {
    case '1':
      return handleProfile(session, user);
    case '2':
      return await handleBalance(session, user);
    case '3':
      return await handleEscrows(session, user);
    case '4':
      return await handleTrackEscrow(session, user);
    case '5':
      return await handleConfirmDelivery(session, user);
    case '6':
      return await handleDispute(session, user);
    case '7':
      return menus.helpMenu;
    case '0':
      sessionService.deleteSession(session.sessionId);
      return menus.exit;
    default:
      return menus.invalidOption(menus.mainMenu(user.displayName, user.role));
  }
}

// ----- Profile -----
function handleProfile(session, user) {
  return menus.profileMenu(user);
}

// ----- Balance -----
async function handleBalance(session, user) {
  try {
    const balance = await apiService.getBalance(user.id, session.token);
    return menus.balance(balance.totalBalance || 0);
  } catch (error) {
    logger.error('Balance error:', error.message);
    return 'END Could not fetch balance. Please try again.';
  }
}

// ----- Escrows -----
async function handleEscrows(session, user) {
  const role = user.role;
  if (role === 'BUYER') {
    try {
      const escrows = await apiService.getBuyerEscrows(user.id, session.token);
      return menus.escrowList('buyer', escrows.content || []);
    } catch (error) {
      return 'END Could not fetch escrows.';
    }
  } else if (role === 'SELLER') {
    try {
      const escrows = await apiService.getSellerEscrows(user.id, session.token);
      return menus.escrowList('seller', escrows.content || []);
    } catch (error) {
      return 'END Could not fetch escrows.';
    }
  } else {
    return 'END This feature is only available for buyers and sellers.';
  }
}

// ----- Track Escrow (multi-step) -----
async function handleTrackEscrow(session, user) {
  // If we are not waiting for input, ask for escrow ID
  if (session.state !== 'AWAITING_ESCROW_ID') {
    session.state = 'AWAITING_ESCROW_ID';
    session.input = '';
    return `CON Enter the Escrow ID to track:`;
  }

  // We have an escrow ID in session.input
  const escrowId = session.input;
  session.state = 'MAIN';
  try {
    const escrow = await apiService.getEscrowDetails(escrowId, session.token, user.id);
    // Clear input after use
    session.input = '';
    return menus.escrowDetails(escrow);
  } catch (error) {
    session.state = 'MAIN';
    session.input = '';
    return 'END Escrow not found. Please check the ID and try again.';
  }
}

// ----- Confirm Delivery (multi-step) -----
async function handleConfirmDelivery(session, user) {
  if (user.role !== 'BUYER') {
    return 'END Only buyers can confirm delivery.';
  }

  if (session.state !== 'AWAITING_CONFIRM_ESCROW_ID') {
    session.state = 'AWAITING_CONFIRM_ESCROW_ID';
    session.input = '';
    return `CON Enter the Escrow ID to confirm delivery:`;
  }

  const escrowId = session.input;
  session.state = 'MAIN';
  try {
    await apiService.buyerConfirmDelivery(escrowId, session.token, user.id);
    session.input = '';
    return `END ✅ Delivery confirmed for ${escrowId}. Funds will be released to seller.`;
  } catch (error) {
    session.input = '';
    return 'END Could not confirm delivery. Please try again.';
  }
}

// ----- Dispute (multi-step) -----
async function handleDispute(session, user) {
  if (session.state === 'MAIN' || session.state === '') {
    session.state = 'AWAITING_DISPUTE_ESCROW_ID';
    session.input = '';
    return `CON Enter the Escrow ID to dispute:`;
  }

  if (session.state === 'AWAITING_DISPUTE_ESCROW_ID') {
    session.pendingEscrowId = session.input;
    session.state = 'AWAITING_DISPUTE_REASON';
    session.input = '';
    return menus.disputeReasons;
  }

  if (session.state === 'AWAITING_DISPUTE_REASON') {
    const reasonMap = {
      '1': 'Item not received',
      '2': 'Item not as described',
      '3': 'Seller not responding',
      '4': 'Other'
    };
    const reason = reasonMap[session.input] || 'Other';
    session.pendingReason = session.input;
    session.state = 'AWAITING_DISPUTE_DESCRIPTION';
    session.input = '';
    return `CON Enter additional details (optional):`;
  }

  if (session.state === 'AWAITING_DISPUTE_DESCRIPTION') {
    const description = session.input || 'No additional details';
    const data = {
      transactionId: session.pendingEscrowId,
      raisedBy: user.role,
      reason: reasonMap[session.pendingReason] || 'Other',
      description: description
    };
    try {
      const result = await apiService.fileDispute(data, session.token, user.id);
      session.state = 'MAIN';
      session.input = '';
      return `END ✅ Dispute filed successfully.\nReference: ${result.reference || 'N/A'}\nAdmin will review shortly.`;
    } catch (error) {
      session.state = 'MAIN';
      session.input = '';
      return 'END Could not file dispute. Please try again.';
    }
  }

  return menus.invalidOption(menus.mainMenu(user.displayName, user.role));
}

module.exports = {
  process
};