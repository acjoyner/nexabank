import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';

import { routes } from './app.routes';
import { jwtInterceptor } from './core/interceptors/jwt.interceptor';
import { errorInterceptor } from './core/interceptors/error.interceptor';

/**
 * Application configuration — standalone Angular 17.
 *
 * Key providers:
 * - provideHttpClient(withInterceptors([...])) — functional HTTP interceptors
 *   (Angular 17+ preferred style over class-based interceptors)
 * - provideAnimations() — required for Angular Material components
 *
 * See docs/learning/09-angular-standalone-components.md
 */
export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(
      withInterceptors([jwtInterceptor, errorInterceptor])
    ),
    provideAnimations(),
  ]
};
