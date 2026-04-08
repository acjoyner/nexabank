import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';
import { of } from 'rxjs';
import { DashboardComponent } from './dashboard.component';
import { AuthService, AuthResponse } from '../../core/services/auth.service';
import { AccountService, AccountResponse } from '../../core/services/account.service';
import { TransactionService, TransactionResponse } from '../../core/services/transaction.service';
import { NotificationService } from '../../core/services/notification.service';

const MOCK_CUSTOMER: AuthResponse = {
  token: 'test-token',
  tokenType: 'Bearer',
  expiresAt: '2099-01-01T00:00:00Z',
  customerId: 42,
  email: 'alice@nexabank.com',
  fullName: 'Alice Smith',
};

const MOCK_ACCOUNTS: AccountResponse[] = [
  {
    id: 1001,
    accountNumber: 'CHK-0000001001',
    accountType: 'CHECKING',
    balance: 1250.75,
    status: 'ACTIVE',
    openedAt: '2025-01-01T00:00:00Z',
    customerId: 42,
    customerEmail: 'alice@nexabank.com',
  },
];

const MOCK_TRANSACTIONS: TransactionResponse[] = [
  {
    id: 1,
    referenceNumber: 'abc12345-abcd',
    sourceAccountId: 1001,
    transactionType: 'DEPOSIT',
    amount: 500.00,
    currency: 'USD',
    status: 'COMPLETED',
    createdAt: '2025-06-01T10:00:00Z',
  },
  {
    id: 2,
    referenceNumber: 'def67890-abcd',
    sourceAccountId: 1001,
    transactionType: 'WITHDRAWAL',
    amount: 100.00,
    currency: 'USD',
    status: 'COMPLETED',
    createdAt: '2025-06-02T12:00:00Z',
  },
];

describe('DashboardComponent', () => {
  let fixture: ComponentFixture<DashboardComponent>;
  let component: DashboardComponent;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let accountServiceSpy: jasmine.SpyObj<AccountService>;
  let transactionServiceSpy: jasmine.SpyObj<TransactionService>;

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['getCustomer', 'getCustomerId']);
    accountServiceSpy = jasmine.createSpyObj('AccountService', ['getAccounts']);
    transactionServiceSpy = jasmine.createSpyObj('TransactionService', ['getTransactions']);

    authServiceSpy.getCustomer.and.returnValue(MOCK_CUSTOMER);
    authServiceSpy.getCustomerId.and.returnValue(42);
    accountServiceSpy.getAccounts.and.returnValue(of(MOCK_ACCOUNTS));
    transactionServiceSpy.getTransactions.and.returnValue(of(MOCK_TRANSACTIONS));

    await TestBed.configureTestingModule({
      imports: [DashboardComponent, RouterTestingModule, NoopAnimationsModule],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: AccountService, useValue: accountServiceSpy },
        { provide: TransactionService, useValue: transactionServiceSpy },
        { provide: NotificationService, useValue: {} },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display the customer full name in the heading', () => {
    const h1: HTMLElement = fixture.nativeElement.querySelector('h1');
    expect(h1.textContent).toContain('Alice Smith');
  });

  it('should fetch accounts for the logged-in customer on init', () => {
    expect(accountServiceSpy.getAccounts).toHaveBeenCalledWith(42);
    expect(component.accounts()).toEqual(MOCK_ACCOUNTS);
  });

  it('should fetch transactions for the first account on init', () => {
    expect(transactionServiceSpy.getTransactions).toHaveBeenCalledWith(1001);
    expect(component.recentTransactions().length).toBe(2);
  });

  it('should limit recent transactions to 5 entries', fakeAsync(() => {
    const sixTransactions: TransactionResponse[] = Array.from({ length: 6 }, (_, i) => ({
      ...MOCK_TRANSACTIONS[0],
      id: i + 1,
      referenceNumber: `ref-${i}`,
    }));
    transactionServiceSpy.getTransactions.and.returnValue(of(sixTransactions));

    component.ngOnInit();
    tick();

    expect(component.recentTransactions().length).toBe(5);
  }));

  it('should not fetch transactions when there are no accounts', () => {
    accountServiceSpy.getAccounts.and.returnValue(of([]));
    transactionServiceSpy.getTransactions.calls.reset();

    component.ngOnInit();

    expect(transactionServiceSpy.getTransactions).not.toHaveBeenCalled();
  });

  it('should render an account card for each account', () => {
    const cards = fixture.debugElement.queryAll(By.css('mat-card'));
    // At least one card per account (plus quick-actions card and recent-transactions card)
    expect(cards.length).toBeGreaterThanOrEqual(MOCK_ACCOUNTS.length);
  });

  it('should display account balance formatted as currency', () => {
    const balanceEl: HTMLElement = fixture.nativeElement.querySelector('[data-testid="account-balance"]');
    expect(balanceEl).withContext('balance element with data-testid="account-balance" must exist').toBeTruthy();
    expect(balanceEl.textContent!.trim()).toContain('1,250.75');
  });

  // ── TDD: this test DRIVES the layout fix ──────────────────────────────────
  // Bug: the 2rem balance text has no bottom margin, so it runs directly into
  // the status chip below it. Fix: add margin-bottom to the balance element.
  it('should have margin-bottom on the balance element so it does not collide with the status chip', () => {
    const balanceEl: HTMLElement = fixture.nativeElement.querySelector('[data-testid="account-balance"]');
    expect(balanceEl).withContext('balance element with data-testid="account-balance" must exist').toBeTruthy();

    const marginBottom = window.getComputedStyle(balanceEl).marginBottom;
    // Any positive margin separates the balance from the chip below it
    expect(parseInt(marginBottom, 10)).withContext('balance must have a margin-bottom > 0').toBeGreaterThan(0);
  });

  it('should show "No recent transactions" message when list is empty', () => {
    component.recentTransactions.set([]);
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('No recent transactions');
  });

  it('should show green colour for DEPOSIT transaction amounts', () => {
    const amountEls = fixture.debugElement.queryAll(By.css('[data-testid="txn-amount"]'));
    expect(amountEls.length).toBeGreaterThan(0);

    const depositEl: HTMLElement = amountEls[0].nativeElement;
    expect(depositEl.style.color).toBe('rgb(76, 175, 80)'); // #4caf50
  });

  it('should show red colour for WITHDRAWAL transaction amounts', () => {
    const amountEls = fixture.debugElement.queryAll(By.css('[data-testid="txn-amount"]'));
    expect(amountEls.length).toBeGreaterThanOrEqual(2);

    const withdrawalEl: HTMLElement = amountEls[1].nativeElement;
    expect(withdrawalEl.style.color).toBe('rgb(244, 67, 54)'); // #f44336
  });
});
