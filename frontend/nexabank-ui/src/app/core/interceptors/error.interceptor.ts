import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

/**
 * Error Interceptor — handles HTTP errors globally.
 *
 * 401 Unauthorized: JWT expired/invalid → clear storage and redirect to login
 * 403 Forbidden: user lacks permission → redirect to dashboard
 * Other errors: log and re-throw for component-level handling
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);

  return next(req).pipe(
    catchError(err => {
      if (err.status === 401) {
        localStorage.removeItem('nexabank_token');
        localStorage.removeItem('nexabank_customer');
        router.navigate(['/login']);
      } else if (err.status === 403) {
        router.navigate(['/dashboard']);
      }
      return throwError(() => err);
    })
  );
};
