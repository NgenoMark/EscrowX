export interface User {
  id: string;
  phone: string;
  email: string;
  role: 'BUYER' | 'SELLER' | 'ADMIN' | 'SUPER_ADMIN';
  status: 'PENDING_VERIFICATION' | 'ACTIVE' | 'SUSPENDED' | 'BLACKLISTED';
  blacklistStatus: 'NOT_BLACKLISTED' | 'TEMPORARILY_MUTED' | 'PERMANENTLY_BANNED' | 'UNDER_INVESTIGATION';
  displayName: string;
  businessName: string | null;
  avatarUrl?: string | null;
  createdAt: string;
  updatedAt?: string;
}