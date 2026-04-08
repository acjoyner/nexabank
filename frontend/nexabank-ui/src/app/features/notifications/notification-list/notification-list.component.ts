import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService, Notification } from '../../../core/services/notification.service';

@Component({
  selector: 'app-notification-list',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatListModule, MatIconModule, MatButtonModule, MatChipsModule],
  template: `
    <mat-card>
      <mat-card-header>
        <mat-icon mat-card-avatar>notifications</mat-icon>
        <mat-card-title>Notifications</mat-card-title>
        <mat-card-subtitle>{{ unreadCount() }} unread</mat-card-subtitle>
      </mat-card-header>
      <mat-card-content>
        <div *ngIf="notifications().length === 0" style="padding:24px;text-align:center;color:#666">
          No notifications yet.
        </div>
        <mat-list>
          <mat-list-item *ngFor="let n of notifications()"
                         [style.background]="n.isRead ? 'transparent' : '#f3f8ff'"
                         style="height:auto;padding:12px 0;border-bottom:1px solid #eee">
            <mat-icon matListItemIcon [style.color]="getTypeColor(n.type)">
              {{ getTypeIcon(n.type) }}
            </mat-icon>
            <div matListItemTitle style="font-weight:500">{{ n.subject }}</div>
            <div matListItemLine style="color:#666;white-space:normal">{{ n.body }}</div>
            <div matListItemLine style="color:#999;font-size:0.8rem">
              {{ n.sentAt | date:'medium' }}
              <mat-chip *ngIf="n.sourceTopic" style="font-size:0.7rem;margin-left:8px">
                {{ n.sourceTopic }}
              </mat-chip>
            </div>
            <button mat-icon-button *ngIf="!n.isRead" (click)="markRead(n)" matListItemMeta>
              <mat-icon>mark_email_read</mat-icon>
            </button>
          </mat-list-item>
        </mat-list>
      </mat-card-content>
    </mat-card>
  `
})
export class NotificationListComponent implements OnInit {
  private authService = inject(AuthService);
  private notificationService = inject(NotificationService);

  notifications = signal<Notification[]>([]);
  unreadCount = signal(0);

  ngOnInit(): void {
    const customerId = this.authService.getCustomerId();
    if (!customerId) return;
    this.notificationService.getNotifications(customerId).subscribe(
      ns => {
        this.notifications.set(ns);
        this.unreadCount.set(ns.filter(n => !n.isRead).length);
      }
    );
  }

  markRead(notification: Notification): void {
    this.notificationService.markRead(notification.id).subscribe(updated => {
      this.notifications.update(ns => ns.map(n => n.id === updated.id ? updated : n));
      this.unreadCount.update(c => Math.max(0, c - 1));
    });
  }

  getTypeIcon(type: string): string {
    const icons: Record<string, string> = {
      TXN_COMPLETED: 'swap_horiz',
      ACCOUNT_CREATED: 'account_balance',
      LOAN_STATUS_CHANGED: 'request_page',
      TXN_MQ_CONFIRMED: 'done_all'
    };
    return icons[type] || 'notifications';
  }

  getTypeColor(type: string): string {
    return type === 'TXN_COMPLETED' ? '#1976d2' :
           type === 'ACCOUNT_CREATED' ? '#4caf50' :
           type === 'LOAN_STATUS_CHANGED' ? '#ff9800' : '#9e9e9e';
  }
}
