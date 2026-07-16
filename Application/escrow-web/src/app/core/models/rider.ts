// src/app/core/models/rider.model.ts
import { User } from './user';

/**
 * Delivery Assignment Status from backend
 * Matches DeliveryAssignmentStatus enum
 */
export type DeliveryAssignmentStatus = 
  | 'ASSIGNED'
  | 'ACCEPTED'
  | 'PICKED_UP'
  | 'IN_TRANSIT'
  | 'ARRIVED_AT_BUYER'
  | 'DELIVERED_TO_BUYER'
  | 'FAILED'
  | 'CANCELLED';

/**
 * Rider Profile from backend
 * Matches rider_profiles table
 */
export interface RiderProfile {
  userId: string;
  displayName: string;
  phone: string;
  operationArea: string;
  licenseNumber: string;
  vehicleType: string;
  vehiclePlate: string;
  riderStatus: 'AVAILABLE' | 'BUSY' | 'OFFLINE' | 'SUSPENDED';
  createdAt: string;
  updatedAt: string;
}

/**
 * Delivery Assignment from backend
 * Matches delivery_assignments table
 */
export interface DeliveryAssignment {
  id: string;
  transactionId: string;
  riderUserId: string;
  assignedByUserId: string;
  status: DeliveryAssignmentStatus;
  pickupAddress: string;
  dropoffAddress: string;
  pickupDueAt: string;
  pickedUpAt: string;
  arrivedAtBuyerAt: string;
  deliveredAt: string;
  riderMarkedDeliveredAt: string;
  sellerConfirmedDeliveredAt: string;
  buyerConfirmedDeliveredAt: string;
  createdAt: string;
  updatedAt: string;
}

/**
 * Rider with delivery statistics for assignment UI
 */
export interface RiderWithStats extends Rider {
  activeDeliveries: number;
  maxDeliveries: number;
  isAvailable: boolean;
  riderStatus: 'AVAILABLE' | 'BUSY' | 'OFFLINE' | 'SUSPENDED';
  vehicleType: string;
  vehiclePlate: string;
}

/**
 * Extended Rider model with all fields from backend
 */
export interface Rider extends User {
  vehicleType?: string;
  vehiclePlate?: string;
  rating?: number;
  totalDeliveries?: number;
  isActive?: boolean;
  profileImage?: string;
  riderStatus?: 'AVAILABLE' | 'BUSY' | 'OFFLINE' | 'SUSPENDED';
  operationArea?: string;
  licenseNumber?: string;
}

/**
 * Assign rider request
 */
export interface AssignRiderRequest {
  riderId: string;
  transactionId: string;
  pickupAddress?: string;
  dropoffAddress?: string;
  pickupDueAt?: string;
}

/**
 * Assign rider response
 */
export interface AssignRiderResponse {
  assignmentId: string;
  transactionId: string;
  riderId: string;
  riderName: string;
  status: DeliveryAssignmentStatus;
  assignedAt: string;
}