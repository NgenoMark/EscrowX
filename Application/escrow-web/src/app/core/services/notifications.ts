import { Injectable, computed, signal } from '@angular/core';

export interface AppNotification {
  id: number;
  title: string;
  message: string;
  createdAt: string;
  read: boolean;
  tone: 'info' | 'success' | 'warning' | 'danger';
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private nextId = 4;
  private notificationsSignal = signal<AppNotification[]>([
    {
      id: 1,
      title: 'KYC review pending',
      message: 'Grace Adhiambo submitted seller documents for review.',
      createdAt: new Date(Date.now() - 1000 * 60 * 45).toISOString(),
      read: false,
      tone: 'warning'
    },
    {
      id: 2,
      title: 'Dispute requires action',
      message: 'DSP-103 is under review and awaiting an admin decision.',
      createdAt: new Date(Date.now() - 1000 * 60 * 90).toISOString(),
      read: false,
      tone: 'danger'
    },
    {
      id: 3,
      title: 'Funds released',
      message: 'ESC-TX1005 was completed successfully.',
      createdAt: new Date(Date.now() - 1000 * 60 * 180).toISOString(),
      read: true,
      tone: 'success'
    }
  ]);

  notifications = this.notificationsSignal.asReadonly();
  unreadCount = computed(() => this.notificationsSignal().filter(item => !item.read).length);

  add(title: string, message: string, tone: AppNotification['tone'] = 'info'): void {
    const notification: AppNotification = {
      id: this.nextId++,
      title,
      message,
      createdAt: new Date().toISOString(),
      read: false,
      tone
    };

    this.notificationsSignal.set([notification, ...this.notificationsSignal()].slice(0, 25));
  }

  markRead(id: number): void {
    this.notificationsSignal.update(items =>
      items.map(item => item.id === id ? { ...item, read: true } : item)
    );
  }

  markAllRead(): void {
    this.notificationsSignal.update(items => items.map(item => ({ ...item, read: true })));
  }

  clearRead(): void {
    this.notificationsSignal.update(items => items.filter(item => !item.read));
  }
}
