import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatBadgeModule } from '@angular/material/badge';
import { MatMenuModule } from '@angular/material/menu';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [
    CommonModule, RouterLink, RouterLinkActive,
    MatToolbarModule, MatButtonModule, MatIconModule,
    MatBadgeModule, MatMenuModule
  ],
  template: `
    <mat-toolbar color="primary" style="position:fixed;top:0;z-index:100;width:100%">
      <span style="font-weight:700;font-size:1.3rem">NexaBank</span>
      <span style="flex:1"></span>

      <a mat-button routerLink="/dashboard" routerLinkActive="active-link">
        <mat-icon>dashboard</mat-icon> Dashboard
      </a>
      <a mat-button routerLink="/accounts" routerLinkActive="active-link">
        <mat-icon>account_balance</mat-icon> Accounts
      </a>
      <a mat-button [matMenuTriggerFor]="txnMenu">
        <mat-icon>swap_horiz</mat-icon> Transactions
      </a>
      <mat-menu #txnMenu>
        <a mat-menu-item routerLink="/deposit"><mat-icon>add_circle</mat-icon> Deposit</a>
        <a mat-menu-item routerLink="/withdrawal"><mat-icon>remove_circle</mat-icon> Withdraw</a>
        <a mat-menu-item routerLink="/transfer"><mat-icon>swap_horiz</mat-icon> Transfer</a>
      </mat-menu>

      <a mat-button routerLink="/loans" routerLinkActive="active-link">
        <mat-icon>request_page</mat-icon> Loans
      </a>

      <a mat-icon-button routerLink="/notifications">
        <mat-icon [matBadge]="unreadCount() || null" matBadgeColor="warn">
          notifications
        </mat-icon>
      </a>

      <button mat-icon-button [matMenuTriggerFor]="userMenu">
        <mat-icon>account_circle</mat-icon>
      </button>
      <mat-menu #userMenu>
        <span mat-menu-item disabled>{{ customer?.fullName }}</span>
        <button mat-menu-item (click)="authService.logout()">
          <mat-icon>logout</mat-icon> Logout
        </button>
      </mat-menu>
    </mat-toolbar>
  `
})
export class NavbarComponent implements OnInit {
  authService = inject(AuthService);
  private notificationService = inject(NotificationService);

  customer = this.authService.getCustomer();
  unreadCount = signal(0);

  ngOnInit(): void {
    const customerId = this.authService.getCustomerId();
    if (customerId) {
      this.notificationService.getUnreadCount(customerId).subscribe(
        res => this.unreadCount.set(res.unreadCount)
      );
    }
  }
}
