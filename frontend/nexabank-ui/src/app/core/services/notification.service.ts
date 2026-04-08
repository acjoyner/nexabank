import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Notification {
  id: number;
  customerId: number;
  channel: string;
  type: string;
  subject: string;
  body: string;
  isRead: boolean;
  sentAt: string;
  readAt?: string;
  sourceTopic?: string;
  correlationId?: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private http = inject(HttpClient);
  private readonly API = '/api/notifications';

  getNotifications(customerId: number): Observable<Notification[]> {
    return this.http.get<Notification[]>(`${this.API}/customer/${customerId}`);
  }

  getUnreadCount(customerId: number): Observable<{ unreadCount: number }> {
    return this.http.get<{ unreadCount: number }>(`${this.API}/customer/${customerId}/unread-count`);
  }

  markRead(notificationId: number): Observable<Notification> {
    return this.http.patch<Notification>(`${this.API}/${notificationId}/read`, {});
  }
}
