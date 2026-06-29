// src/app/core/models/dispute.ts

export type DisputeStatus =
  | 'OPEN'
  | 'PENDING'
  | 'UNDER_REVIEW'
  | 'ACTION_REQUIRED'
  | 'RESOLVED'
  | 'REJECTED'
  | 'ESCALATED';

export interface Dispute {
  id: string;
  transactionId: string;
  transactionReference?: string;
  raisedById: string;           // API: raisedById
  raisedByName: string;         // API: raisedByName
  category: string;             // e.g., 'DEFECTIVE_ITEM'
  description: string;
  status: DisputeStatus;
  assignedAdminId?: string | null;
  resolution?: string | null;
  resolvedAt?: string | null;
  evidenceUrls: string[];       // API: evidenceUrls (array of image URLs)
  amount: number;               // from transaction or dispute
  createdAt: string;
  updatedAt: string;
  // UI-only fields (computed or local)
  against?: string;             // derived from transaction
  adminNotes?: string;
  partialAmount?: number;
}