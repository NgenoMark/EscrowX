// src/app/core/services/cache.service.ts
import { Injectable } from '@angular/core';

interface CacheItem<T> {
  data: T;
  expiresAt: number; // timestamp in milliseconds
}

@Injectable({
  providedIn: 'root'
})
export class CacheService {
  private cache = new Map<string, CacheItem<any>>();
  private defaultTTL = 5 * 60 * 1000; // 5 minutes in milliseconds

  /**
   * Store data in cache with optional TTL (in milliseconds).
   * Default TTL is 5 minutes.
   */
  set<T>(key: string, data: T, ttlMs: number = this.defaultTTL): void {
    this.cache.set(key, {
      data,
      expiresAt: Date.now() + ttlMs
    });
  }

  /**
   * Retrieve data from cache. Returns null if expired or not found.
   */
  get<T>(key: string): T | null {
    const item = this.cache.get(key);
    if (!item) return null;
    if (Date.now() > item.expiresAt) {
      this.cache.delete(key);
      return null;
    }
    return item.data as T;
  }

  /**
   * Clear a specific cache entry.
   */
  clear(key: string): void {
    this.cache.delete(key);
  }

  /**
   * Clear all cache entries.
   */
  clearAll(): void {
    this.cache.clear();
  }

  /**
   * Check if a cache entry exists and is valid.
   */
  has(key: string): boolean {
    const item = this.cache.get(key);
    if (!item) return false;
    if (Date.now() > item.expiresAt) {
      this.cache.delete(key);
      return false;
    }
    return true;
  }
}