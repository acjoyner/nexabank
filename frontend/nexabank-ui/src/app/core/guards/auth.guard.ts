import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

/**
 * Auth Guard — protects routes that require a logged-in user.
 *
 * Functional guard style (Angular 17+) — no class required.
 * Checks for JWT token in localStorage. If missing, redirects to /login.
 */
export const authGuard: CanActivateFn = () => {
  const router = inject(Router);
  const token = localStorage.getItem('nexabank_token');

  if (token) {
    return true;
  }

  router.navigate(['/login']);
  return false;
};
