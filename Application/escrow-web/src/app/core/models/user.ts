export interface User {
  id: number;
  name: string;
  phone: string;
  email: string;
  role: 'BUYER' | 'SELLER';
  status: 'ACTIVE' | 'SUSPENDED';
  isPhoneVerified: boolean;  // OTP verification status
  isEmailVerified: boolean;  // Email verification status
  kycStatus: 'PENDING' | 'SUBMITTED' | 'APPROVED' | 'REJECTED';  // For higher transaction limits
  blacklisted: boolean;
  registrationDate: string;
  totalTransactions?: number;
  totalVolume?: number;
}