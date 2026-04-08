import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from './shared/components/navbar/navbar.component';
import { CommonModule } from '@angular/common';
import { AuthService } from './core/services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, NavbarComponent, CommonModule],
  template: `
    <app-navbar *ngIf="authService.isLoggedIn()"></app-navbar>
    <main class="main-content" [class.with-nav]="authService.isLoggedIn()">
      <router-outlet></router-outlet>
    </main>
  `,
  styles: [`
    .main-content { padding: 24px; }
    .main-content.with-nav { padding-top: 88px; }
  `]
})
export class AppComponent {
  title = 'NexaBank';
  constructor(public authService: AuthService) {}
}
