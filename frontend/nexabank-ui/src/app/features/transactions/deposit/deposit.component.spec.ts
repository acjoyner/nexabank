import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError, Subject } from 'rxjs';
import { DepositComponent } from './deposit.component';
import { AuthService } from '../../../core/services/auth.service';
import { AccountService, AccountResponse } from '../../../core/services/account.service';
import { TransactionService, TransactionResponse } from '../../../core/services/transaction.service';

const MOCK_ACCOUNTS: AccountResponse[] = [
  {
    id: 1001,
    accountNumber: 'CHK-0000001001',
    accountType: 'CHECKING',
    balance: 500.00,
    status: 'ACTIVE',
    openedAt: '2025-01-01T00:00:00Z',
    customerId: 42,
    customerEmail: 'alice@nexabank.com',
  },
];

const MOCK_DEPOSIT_RESULT: TransactionResponse = {
  id: 1,
  referenceNumber: 'abc123',
  sourceAccountId: 1001,
  transactionType: 'DEPOSIT',
  amount: 250.00,
  currency: 'USD',
  status: 'COMPLETED',
  createdAt: '2025-06-01T10:00:00Z',
};

describe('DepositComponent', () => {
  let fixture: ComponentFixture<DepositComponent>;
  let component: DepositComponent;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let accountServiceSpy: jasmine.SpyObj<AccountService>;
  let transactionServiceSpy: jasmine.SpyObj<TransactionService>;

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['getCustomerId']);
    accountServiceSpy = jasmine.createSpyObj('AccountService', ['getAccounts']);
    transactionServiceSpy = jasmine.createSpyObj('TransactionService', ['deposit']);

    authServiceSpy.getCustomerId.and.returnValue(42);
    accountServiceSpy.getAccounts.and.returnValue(of(MOCK_ACCOUNTS));

    await TestBed.configureTestingModule({
      imports: [DepositComponent, NoopAnimationsModule],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: AccountService, useValue: accountServiceSpy },
        { provide: TransactionService, useValue: transactionServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DepositComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load accounts for the logged-in customer on init', () => {
    expect(authServiceSpy.getCustomerId).toHaveBeenCalled();
    expect(accountServiceSpy.getAccounts).toHaveBeenCalledWith(42);
    expect(component.accounts()).toEqual(MOCK_ACCOUNTS);
  });

  it('should have an invalid form when fields are empty', () => {
    expect(component.form.invalid).toBeTrue();
  });

  it('should have a valid form when accountId and amount are set', () => {
    component.form.setValue({ accountId: 1001, amount: 100 });
    expect(component.form.valid).toBeTrue();
  });

  it('should reject amount of zero', () => {
    component.form.setValue({ accountId: 1001, amount: 0 });
    expect(component.form.get('amount')!.invalid).toBeTrue();
  });

  it('should call deposit service with correct accountId and amount on submit', fakeAsync(() => {
    transactionServiceSpy.deposit.and.returnValue(of(MOCK_DEPOSIT_RESULT));
    component.form.setValue({ accountId: 1001, amount: 250 });

    component.submit();
    tick();

    expect(transactionServiceSpy.deposit).toHaveBeenCalledWith(1001, 250);
  }));

  it('should set the result signal after a successful deposit', fakeAsync(() => {
    transactionServiceSpy.deposit.and.returnValue(of(MOCK_DEPOSIT_RESULT));
    component.form.setValue({ accountId: 1001, amount: 250 });

    component.submit();
    tick();

    expect(component.result()).toEqual(MOCK_DEPOSIT_RESULT);
  }));

  // ── TDD: this test DRIVES the fix ─────────────────────────────────────────
  // Before fix: getAccounts is only called once (ngOnInit). After a successful
  // deposit the account balance in the UI stays stale. The fix is to re-fetch
  // accounts after deposit success so the displayed balance reflects the new total.
  it('should refresh account list after a successful deposit', fakeAsync(() => {
    const updatedAccounts: AccountResponse[] = [
      { ...MOCK_ACCOUNTS[0], balance: 750.00 }, // balance increased after deposit
    ];
    // Return updated accounts on any subsequent call
    accountServiceSpy.getAccounts.and.returnValue(of(updatedAccounts));
    transactionServiceSpy.deposit.and.returnValue(of(MOCK_DEPOSIT_RESULT));

    const callsBefore = accountServiceSpy.getAccounts.calls.count();
    component.form.setValue({ accountId: 1001, amount: 250 });
    component.submit();
    tick();

    // getAccounts must be called at least once more after deposit
    expect(accountServiceSpy.getAccounts.calls.count()).toBeGreaterThan(callsBefore);
    expect(component.accounts()[0].balance).toBe(750.00);
  }));

  it('should show an error message when deposit fails', fakeAsync(() => {
    transactionServiceSpy.deposit.and.returnValue(
      throwError(() => ({ error: { detail: 'Insufficient funds' } }))
    );
    component.form.setValue({ accountId: 1001, amount: 9999 });

    component.submit();
    tick();

    expect(component.error).toBe('Insufficient funds');
    expect(component.result()).toBeNull();
  }));

  it('should set loading to false after deposit completes', fakeAsync(() => {
    // Use a Subject to control when the observable emits so we can assert
    // the intermediate loading=true state before the response arrives.
    const depositSubject = new Subject<TransactionResponse>();
    transactionServiceSpy.deposit.and.returnValue(depositSubject.asObservable());
    component.form.setValue({ accountId: 1001, amount: 250 });

    component.submit();
    expect(component.loading).toBeTrue();

    depositSubject.next(MOCK_DEPOSIT_RESULT);
    depositSubject.complete();
    tick();

    expect(component.loading).toBeFalse();
  }));

  it('should set loading to false after deposit errors', fakeAsync(() => {
    transactionServiceSpy.deposit.and.returnValue(
      throwError(() => ({ error: { detail: 'Server error' } }))
    );
    component.form.setValue({ accountId: 1001, amount: 250 });

    component.submit();
    tick();

    expect(component.loading).toBeFalse();
  }));
});
