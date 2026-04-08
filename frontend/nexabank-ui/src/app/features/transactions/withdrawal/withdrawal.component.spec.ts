import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { WithdrawalComponent } from './withdrawal.component';
import { AuthService } from '../../../core/services/auth.service';
import { AccountService, AccountResponse } from '../../../core/services/account.service';
import { TransactionService, TransactionResponse } from '../../../core/services/transaction.service';

const MOCK_ACCOUNTS: AccountResponse[] = [
  {
    id: 1001,
    accountNumber: 'CHK-0000001001',
    accountType: 'CHECKING',
    balance: 1000.00,
    status: 'ACTIVE',
    openedAt: '2025-01-01T00:00:00Z',
    customerId: 42,
    customerEmail: 'alice@nexabank.com',
  },
];

const MOCK_WITHDRAWAL_RESULT: TransactionResponse = {
  id: 2,
  referenceNumber: 'def456',
  sourceAccountId: 1001,
  transactionType: 'WITHDRAWAL',
  amount: 200.00,
  currency: 'USD',
  status: 'COMPLETED',
  createdAt: '2025-06-01T11:00:00Z',
};

describe('WithdrawalComponent', () => {
  let fixture: ComponentFixture<WithdrawalComponent>;
  let component: WithdrawalComponent;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let accountServiceSpy: jasmine.SpyObj<AccountService>;
  let transactionServiceSpy: jasmine.SpyObj<TransactionService>;

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['getCustomerId']);
    accountServiceSpy = jasmine.createSpyObj('AccountService', ['getAccounts']);
    transactionServiceSpy = jasmine.createSpyObj('TransactionService', ['withdrawal']);

    authServiceSpy.getCustomerId.and.returnValue(42);
    accountServiceSpy.getAccounts.and.returnValue(of(MOCK_ACCOUNTS));

    await TestBed.configureTestingModule({
      imports: [WithdrawalComponent, NoopAnimationsModule],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: AccountService, useValue: accountServiceSpy },
        { provide: TransactionService, useValue: transactionServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(WithdrawalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load accounts for the logged-in customer on init', () => {
    expect(accountServiceSpy.getAccounts).toHaveBeenCalledWith(42);
    expect(component.accounts()).toEqual(MOCK_ACCOUNTS);
  });

  it('should call withdrawal service with correct args on submit', fakeAsync(() => {
    transactionServiceSpy.withdrawal.and.returnValue(of(MOCK_WITHDRAWAL_RESULT));
    component.form.setValue({ accountId: 1001, amount: 200 });

    component.submit();
    tick();

    expect(transactionServiceSpy.withdrawal).toHaveBeenCalledWith(1001, 200);
  }));

  it('should set the result signal after a successful withdrawal', fakeAsync(() => {
    transactionServiceSpy.withdrawal.and.returnValue(of(MOCK_WITHDRAWAL_RESULT));
    component.form.setValue({ accountId: 1001, amount: 200 });

    component.submit();
    tick();

    expect(component.result()).toEqual(MOCK_WITHDRAWAL_RESULT);
  }));

  // ── TDD: this test DRIVES the fix ─────────────────────────────────────────
  // After a withdrawal the account balance shown in the UI does not update
  // because getAccounts is never called again. The fix re-fetches accounts
  // after success so the balance in the accounts signal reflects the deduction.
  it('should refresh account list after a successful withdrawal', fakeAsync(() => {
    const updatedAccounts: AccountResponse[] = [
      { ...MOCK_ACCOUNTS[0], balance: 800.00 }, // balance reduced after withdrawal
    ];
    accountServiceSpy.getAccounts.and.returnValue(of(updatedAccounts));
    transactionServiceSpy.withdrawal.and.returnValue(of(MOCK_WITHDRAWAL_RESULT));

    const callsBefore = accountServiceSpy.getAccounts.calls.count();
    component.form.setValue({ accountId: 1001, amount: 200 });
    component.submit();
    tick();

    expect(accountServiceSpy.getAccounts.calls.count()).toBeGreaterThan(callsBefore);
    expect(component.accounts()[0].balance).toBe(800.00);
  }));

  it('should show an error message when withdrawal fails', fakeAsync(() => {
    transactionServiceSpy.withdrawal.and.returnValue(
      throwError(() => ({ error: { detail: 'Insufficient funds' } }))
    );
    component.form.setValue({ accountId: 1001, amount: 9999 });

    component.submit();
    tick();

    expect(component.error).toBe('Insufficient funds');
  }));
});
