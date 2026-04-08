import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, RouterLink,
    MatCardModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule
  ],
  template: `
    <div style="display:flex;justify-content:center;align-items:center;min-height:100vh;background:#f5f5f5">
      <mat-card style="width:400px;padding:24px">
        <mat-card-header>
          <mat-card-title style="font-size:1.5rem">Welcome to NexaBank</mat-card-title>
          <mat-card-subtitle>Sign in to your account</mat-card-subtitle>
        </mat-card-header>

        <mat-card-content style="margin-top:16px">
          <form [formGroup]="form" (ngSubmit)="submit()">
            <mat-form-field appearance="outline" style="width:100%">
              <mat-label>Email</mat-label>
              <input matInput formControlName="email" type="email" placeholder="john@example.com">
              <mat-error *ngIf="form.get('email')?.invalid">Valid email required</mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline" style="width:100%;margin-top:8px">
              <mat-label>Password</mat-label>
              <input matInput formControlName="password" [type]="hidePassword ? 'password' : 'text'">
              <button mat-icon-button matSuffix (click)="hidePassword=!hidePassword" type="button">
                <mat-icon>{{ hidePassword ? 'visibility_off' : 'visibility' }}</mat-icon>
              </button>
              <mat-error *ngIf="form.get('password')?.invalid">Password required</mat-error>
            </mat-form-field>

            <p *ngIf="error" style="color:#f44336">{{ error }}</p>

            <button mat-raised-button color="primary" type="submit"
                    [disabled]="form.invalid || loading" style="width:100%;margin-top:8px">
              {{ loading ? 'Signing in...' : 'Sign In' }}
            </button>

            <p style="text-align:center;margin-top:16px">
              Don't have an account? <a routerLink="/register">Register</a>
            </p>
          </form>
        </mat-card-content>
      </mat-card>
    </div>
  `
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  form: FormGroup = this.fb.group({
    email:    ['', [Validators.required, Validators.email]],
    password: ['', Validators.required]
  });

  hidePassword = true;
  loading = false;
  error = '';

  submit(): void {
    if (this.form.invalid) return;
    this.loading = true;
    this.error = '';

    const { email, password } = this.form.value;
    this.authService.login(email, password).subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: (err) => {
        this.error = err.error?.detail || 'Invalid credentials. Please try again.';
        this.loading = false;
      }
    });
  }
}
