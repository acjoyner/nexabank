# Angular Standalone Components — NexaBank Frontend

> **Paste into Ollama/Open WebUI** for AI-assisted learning on this topic.

## What Changed in Angular 17

Before Angular 14, every component had to be declared inside an `NgModule`:

```typescript
// OLD way (NgModule required)
@NgModule({
  declarations: [DashboardComponent],
  imports: [CommonModule, RouterModule]
})
export class DashboardModule {}
```

Angular 14+ introduced standalone components. Angular 17 made them the **default**. NexaBank uses the Angular 17 standalone approach throughout:

```typescript
// NEW way (no NgModule needed)
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, MatCardModule],
  template: `...`
})
export class DashboardComponent {}
```

**Why it matters:** Standalone components are simpler, more tree-shakeable (smaller bundles), and align with how React/Vue work — a skill transfer that interviewers notice.

## NexaBank Frontend Structure

```
frontend/nexabank-ui/src/app/
├── app.config.ts                  ← Root providers (replaces AppModule)
├── app.routes.ts                  ← Route definitions
├── core/
│   ├── interceptors/
│   │   ├── jwt.interceptor.ts     ← Attaches Bearer token to every request
│   │   └── error.interceptor.ts   ← Catches 401 → redirect to login
│   ├── guards/
│   │   └── auth.guard.ts          ← Blocks unauthenticated navigation
│   └── services/
│       ├── auth.service.ts
│       ├── account.service.ts
│       ├── transaction.service.ts
│       ├── notification.service.ts
│       └── loan.service.ts
└── features/
    ├── auth/                      ← login + register components
    ├── dashboard/                 ← account summary + recent transactions
    ├── accounts/                  ← account-list + account-detail
    ├── transactions/              ← deposit, withdrawal, transfer forms
    ├── notifications/             ← notification list with unread badge
    └── loans/
        ├── loan-apply/            ← Multi-step form with AI decision display
        └── loan-status/           ← Status timeline
```

## app.config.ts — The New Root Module

`app.config.ts` replaces `AppModule`. It configures the application providers:

```typescript
import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { jwtInterceptor } from './core/interceptors/jwt.interceptor';
import { errorInterceptor } from './core/interceptors/error.interceptor';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(withInterceptors([jwtInterceptor, errorInterceptor])),
    provideAnimations()
  ]
};
```

This is bootstrapped in `main.ts`:
```typescript
bootstrapApplication(AppComponent, appConfig);
```

## Functional HttpInterceptors

Angular 15+ introduced functional interceptors — no class required:

```typescript
// core/interceptors/jwt.interceptor.ts
import { HttpInterceptorFn } from '@angular/common/http';

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem('nexabank_token');
  if (token) {
    const cloned = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });
    return next(cloned);
  }
  return next(req);
};
```

```typescript
// core/interceptors/error.interceptor.ts
import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  return next(req).pipe(
    catchError(err => {
      if (err.status === 401) {
        localStorage.removeItem('nexabank_token');
        router.navigate(['/auth/login']);
      }
      return throwError(() => err);
    })
  );
};
```

**Why functional interceptors?** They use Angular's `inject()` function instead of constructor injection, which works cleanly with the standalone model and tree-shaking.

## Functional Route Guards

```typescript
// core/guards/auth.guard.ts
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

export const authGuard: CanActivateFn = () => {
  const router = inject(Router);
  const token = localStorage.getItem('nexabank_token');
  if (token) {
    return true;
  }
  router.navigate(['/auth/login']);
  return false;
};
```

Applied in routes:
```typescript
// app.routes.ts
export const routes: Routes = [
  { path: 'auth/login', component: LoginComponent },
  {
    path: 'dashboard',
    component: DashboardComponent,
    canActivate: [authGuard]   // Blocks unauthenticated access
  },
  {
    path: 'loans/apply',
    component: LoanApplyComponent,
    canActivate: [authGuard]
  }
];
```

## Reactive Forms — Loan Application (Multi-Step)

The loan application is the most complex form in NexaBank — a 3-step MatStepper:

```typescript
@Component({
  selector: 'app-loan-apply',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatStepperModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    AsyncPipe
  ]
})
export class LoanApplyComponent {
  private fb = inject(FormBuilder);
  private loanService = inject(LoanService);

  // Step 1: Loan details
  loanDetailsForm = this.fb.group({
    loanType: ['', Validators.required],
    requestedAmount: [null, [Validators.required, Validators.min(1000)]],
    termMonths: [12, [Validators.required, Validators.min(6), Validators.max(360)]]
  });

  // Step 2: Financial info
  financialForm = this.fb.group({
    annualIncome: [null, [Validators.required, Validators.min(10000)]],
    creditScore: [null, [Validators.required, Validators.min(300), Validators.max(850)]]
  });

  // Step 3: AI decision (read-only, populated after submit)
  aiDecision: 'APPROVED' | 'REJECTED' | 'AI_REVIEW' | null = null;
  aiReason = '';
  strategyUsed = '';

  submit() {
    const payload = {
      ...this.loanDetailsForm.value,
      ...this.financialForm.value
    };
    this.loanService.apply(payload).subscribe({
      next: (response) => {
        this.aiDecision = response.status;
        this.aiReason = response.aiReason;
        this.strategyUsed = response.strategyUsed;
        this.stepper.next();  // Advance to step 3
      }
    });
  }
}
```

## HttpClient Services Pattern

Every feature has a service that encapsulates API calls:

```typescript
// core/services/account.service.ts
@Injectable({ providedIn: 'root' })
export class AccountService {
  private http = inject(HttpClient);
  private readonly BASE = '/api/accounts';

  getAll(): Observable<AccountResponse[]> {
    return this.http.get<AccountResponse[]>(this.BASE);
  }

  getBalance(accountId: number): Observable<BalanceResponse> {
    return this.http.get<BalanceResponse>(`${this.BASE}/${accountId}/balance`);
  }

  create(request: AccountRequest): Observable<AccountResponse> {
    return this.http.post<AccountResponse>(this.BASE, request);
  }
}
```

The `jwtInterceptor` automatically attaches `Authorization: Bearer <token>` to every request — no service needs to know about JWT.

## Nginx Reverse Proxy (Production Pattern)

In the Docker container, Angular is served by Nginx with two critical configurations:

```nginx
# frontend/nexabank-ui/nginx.conf
server {
    listen 80;
    root /usr/share/nginx/html;

    # Angular client-side routing — always serve index.html
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Proxy API calls to avoid CORS issues
    location /api/ {
        proxy_pass http://api-gateway:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

**Why `try_files $uri $uri/ /index.html`:** Angular handles routing in the browser. If a user refreshes `localhost/dashboard`, Nginx must serve `index.html` — not return a 404. Angular's router then loads the correct component.

**Why proxy `/api/`:** Browser makes request to `localhost/api/transfer`. Nginx proxies to `http://api-gateway:8080/api/transfer`. No CORS preflight needed because the origin appears to be the same host.

## Multi-Stage Docker Build

```dockerfile
# Stage 1: Build Angular
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build -- --configuration=production

# Stage 2: Serve with Nginx
FROM nginx:alpine
COPY --from=build /app/dist/nexabank-ui/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

The final image is ~25MB (Nginx + compiled JS). The Node.js build tools (~300MB) are discarded.

## Interview Talking Points
- **What is a standalone component vs NgModule-based component?** Standalone components declare their own imports directly, eliminating the need for NgModule as a grouping mechanism. Simpler mental model, better tree-shaking.
- **What is an HttpInterceptor?** Middleware for HTTP requests — runs before every request goes out and after every response comes back. Used for auth token injection, global error handling, loading spinners, request logging.
- **What is a route guard?** `CanActivate` runs before a route activates. If it returns `false`, navigation is cancelled. Used to block unauthenticated users from protected routes.
- **What is a reactive form vs template-driven form?** Reactive forms define the form model in the component class (`FormBuilder`, `FormGroup`, `FormControl`). More explicit, easier to test, better for complex validation. Template-driven forms use `ngModel` in the template — simpler but harder to test.
- **Why does Angular use Observables instead of Promises?** Observables are cancellable, composable (pipe operators), and can emit multiple values — critical for real-time features like notifications. Promises resolve once and can't be cancelled.

## Questions to Ask Your AI
- "What is the difference between `provideHttpClient(withFetch())` and the default HttpClient?"
- "How do you implement lazy loading for Angular feature modules (or standalone routes)?"
- "What is Angular signals and how do they differ from RxJS observables?"
- "How would you add real-time notifications to NexaBank using WebSockets or Server-Sent Events?"
- "How do you write unit tests for an Angular component using TestBed?"
- "What is the Angular change detection strategy and when would you use OnPush?"
