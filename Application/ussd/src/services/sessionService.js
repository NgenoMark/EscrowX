// src/services/sessionService.js
// In-memory session store (for development)
// For production, replace with Redis

const sessions = new Map();

/**
 * Create or retrieve a session
 */
function getOrCreateSession(sessionId, phoneNumber) {
  if (sessions.has(sessionId)) {
    return sessions.get(sessionId);
  }
  const session = {
    sessionId,
    phoneNumber,
    state: 'MAIN',           // Current menu state
    loggedIn: false,
    userData: null,          // User object from backend
    token: null,             // Auth token
    selectedEscrowId: null,  // For tracking/disputing
    pendingAction: null,     // For multi-step flows
    createdAt: Date.now()
  };
  sessions.set(sessionId, session);
  return session;
}

/**
 * Update session data
 */
function updateSession(sessionId, updates) {
  if (sessions.has(sessionId)) {
    const session = sessions.get(sessionId);
    Object.assign(session, updates);
    sessions.set(sessionId, session);
    return session;
  }
  return null;
}

/**
 * Get session by ID
 */
function getSession(sessionId) {
  return sessions.get(sessionId) || null;
}

/**
 * Delete session (on exit)
 */
function deleteSession(sessionId) {
  sessions.delete(sessionId);
}

/**
 * Clear all sessions (for testing)
 */
function clearAllSessions() {
  sessions.clear();
}

module.exports = {
  getOrCreateSession,
  updateSession,
  getSession,
  deleteSession,
  clearAllSessions
};