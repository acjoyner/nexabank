import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, RouterLink,
    MatCardModule, MatFormFieldModule, MatInputModule, MatButtonModule
  ],
  template: `
    <div style="display:flex;justify-content:center;align-items:center;min-height:100vh;background:#f5f5f5">
      <mat-card style="width:450px;padding:24px">
        <mat-card-header>
          <mat-card-title>Open Your NexaBank Account</mat-card-title>
          <mat-card-subtitle>A CHECKING account will be created automatically</mat-card-subtitle>
        </mat-card-header>

        <mat-card-content style="margin-top:16px">
          <form [formGroup]="form" (ngSubmit)="submit()">
            <div style="display:grid;grid-template-columns:1fr 1fr;gap:8px">
              <mat-form-field appearance="outline">
                <mat-label>First Name</mat-label>
                <input matInput formControlName="firstName">
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>Last Name</mat-label>
                <input matInput formControlName="lastName">
              </mat-form-field>
            </div>

            <mat-form-field appearance="outline" style="width:100%">
              <mat-label>Email</mat-label>
              <input matInput formControlName="email" type="email">
            </mat-form-field>

            <mat-form-field appearance="outline" style="width:100%;margin-top:8px">
              <mat-label>Password (min 8 chars)</mat-label>
              <input matInput formControlName="password" type="password">
              <mat-error *ngIf="form.get('password')?.hasError('minlength')">
                Minimum 8 characters required
              </mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline" style="width:100%;margin-top:8px">
              <mat-label>Phone (optional)</mat-label>
              <input matInput formControlName="phone" type="tel">
            </mat-form-field>

            <p *ngIf="error" style="color:#f44336">{{ error }}</p>

            <button mat-raised-button color="primary" type="submit"
                    [disabled]="form.invalid || loading" style="width:100%;margin-top:8px">
              {{ loading ? 'Creating Account...' : 'Create Account' }}
            </button>

            <p style="text-align:center;margin-top:16px">
              Already have an account? <a routerLink="/login">Sign In</a>
            </p>
          </form>
        </mat-card-content>
      </mat-card>
    </div>
  `
})
export class RegisterComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  form: FormGroup = this.fb.group({
    firstName: ['', Validators.required],
    lastName:  ['', Validators.required],
    email:     ['', [Validators.required, Validators.email]],
    password:  ['', [Validators.required, Validators.minLength(8)]],
    phone:     ['']
  });

  loading = false;
  error = '';

  submit(): void {
    if (this.form.invalid) return;
    this.loading = true;
    this.error = '';

    this.authService.register(this.form.value).subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: (err) => {
        this.error = err.error?.detail || 'Registration failed. Email may already be in use.';
        this.loading = false;
      }
    });
  }
}
