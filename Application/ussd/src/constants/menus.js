// src/constants/menus.js

// Main menu (shown after authentication)
exports.mainMenu = (name, role) => {
  let roleText = role === 'BUYER' ? 'Buyer' : role === 'SELLER' ? 'Seller' : 'Rider';
  return `CON Welcome back, ${name || 'User'}!
You are: ${roleText}

1. My Profile
2. Balance
3. My Escrows
4. Track Escrow
5. Confirm Delivery
6. File Dispute
7. Help
0. Exit`;
};

// Unregistered user menu
exports.unregisteredMenu = `CON Welcome to EscrowX!
You are not registered.

1. Login with existing account
2. Download App to register
0. Exit`;

// Login flows
exports.loginEmail = `CON Enter your registered email:`;
exports.loginPassword = `CON Enter your password:`;

// Profile menu
exports.profileMenu = (user) => `CON My Profile
Name: ${user.displayName || 'N/A'}
Email: ${user.email || 'N/A'}
Phone: ${user.phone || 'N/A'}
Role: ${user.role || 'N/A'}

1. Change PIN (coming soon)
2. Back`;

// Balance
exports.balance = (balance) => `END Your current escrow balance is KES ${balance}`;

// Escrow listing (buyer/seller)
exports.escrowList = (role, escrows) => {
  if (!escrows || escrows.length === 0) {
    return `END You have no escrows as a ${role}.`;
  }
  let list = escrows.map((e, i) => 
    `${i+1}. ${e.id.slice(0, 8)} - KES ${e.amount} - ${e.status}`
  ).join('\n');
  return `CON Your ${role} escrows:\n${list}\n\nEnter number for details, or 0 to go back.`;
};

// Escrow details (after selecting)
exports.escrowDetails = (escrow) => {
  return `CON Escrow: ${escrow.id}
Status: ${escrow.status}
Amount: KES ${escrow.amount}
Buyer: ${escrow.buyerId}
Seller: ${escrow.sellerId}
Created: ${new Date(escrow.createdAt).toLocaleDateString()}

1. View History
2. Track Delivery
3. Confirm Delivery (buyer only)
4. File Dispute
5. Back`;
};

// Help
exports.helpMenu = `END For support, call +254 700 000 000 or email help@escrowx.com`;

// Exit
exports.exit = `END Thank you for using EscrowX!`;

// Invalid option
exports.invalidOption = (menu) => `CON Invalid option. Please try again.\n${menu}`;

// Dispute reasons
exports.disputeReasons = `CON Reason for dispute:
1. Item not received
2. Item not as described
3. Seller not responding
4. Other`;

// Dispute confirmation
exports.disputeConfirm = `END ✅ Dispute filed successfully.
Reference: {reference}
Admin will review shortly.`;

// Delivery confirmation
exports.confirmDelivery = (escrowId) => `CON Confirm delivery for ${escrowId}?
1. Yes
2. No`;

// Download app
exports.downloadApp = `END Download EscrowX from the Play Store or App Store.
Visit https://escrowx.com/download`;