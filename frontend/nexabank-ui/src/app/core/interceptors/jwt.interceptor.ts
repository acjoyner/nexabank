import { HttpInterceptorFn } from '@angular/common/http';

/**
 * JWT Interceptor — functional style (Angular 17+).
 *
 * Automatically attaches the Bearer token to every outgoing HTTP request.
 * This is the Angular 17 functional interceptor pattern — replaces
 * class-based interceptors (no need to extend HttpInterceptor).
 *
 * Token is stored in localStorage under 'nexabank_token'.
 * The API Gateway reads the Authorization header and validates the JWT.
 *
 * See docs/learning/09-angular-standalone-components.md
 */
export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem('nexabank_token');

  if (token) {
    const cloned = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
    return next(cloned);
  }

  return next(req);
};
