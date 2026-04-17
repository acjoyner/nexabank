import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { TransferComponent } from './transfer.component';
import { AuthService } from '../../../core/services/auth.service';
import { AccountService, AccountResponse } from '../../../core/services/account.service';
import { TransactionService, TransactionResponse } from '../../../core/services/transaction.service';

const MOCK_ACCOUNTS: AccountResponse[] = [
  {
    id: 1001,
    accountNumber: 'CHK-0000001001',
    accountType: 'CHECKING',
    balance: 2000,
    status: 'ACTIVE',
    openedAt: '2025-01-01T00:00:00Z',
    customerId: 42,
    customerEmail: 'alice@nexabank.com',
  },
  {
    id: 1002,
    accountNumber: 'SAV-0000001002',
    accountType: 'SAVINGS',
    balance: 5000,
    status: 'ACTIVE',
    openedAt: '2025-01-01T00:00:00Z',
    customerId: 42,
    customerEmail: 'alice@nexabank.com',
  },
];

const MOCK_TRANSFER_RESPONSE: TransactionResponse = {
  id: 1,
  referenceNumber: 'ref-transfer-001',
  sourceAccountId: 1001,
  destAccountId: 1002,
  transactionType: 'TRANSFER',
  amount: 300,
  currency: 'USD',
  status: 'COMPLETED',
  createdAt: new Date().toISOString(),
};

describe('TransferComponent', () => {
  let fixture: ComponentFixture<TransferComponent>;
  let component: TransferComponent;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let accountServiceSpy: jasmine.SpyObj<AccountService>;
  let transactionServiceSpy: jasmine.SpyObj<TransactionService>;

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['getCustomerId']);
    accountServiceSpy = jasmine.createSpyObj('AccountService', ['getAccounts']);
    transactionServiceSpy = jasmine.createSpyObj('TransactionService', ['transfer']);

    authServiceSpy.getCustomerId.and.returnValue(42);
    accountServiceSpy.getAccounts.and.returnValue(of(MOCK_ACCOUNTS));

    await TestBed.configureTestingModule({
      imports: [TransferComponent, NoopAnimationsModule],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: AccountService, useValue: accountServiceSpy },
        { provide: TransactionService, useValue: transactionServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TransferComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load accounts on init', () => {
    expect(accountServiceSpy.getAccounts).toHaveBeenCalledWith(42);
    expect(component.accounts().length).toBe(2);
  });

  it('should not submit when form is invalid', () => {
    component.submit();
    expect(transactionServiceSpy.transfer).not.toHaveBeenCalled();
  });

  it('should call transfer service with form values', fakeAsync(() => {
    transactionServiceSpy.transfer.and.returnValue(of(MOCK_TRANSFER_RESPONSE));

    component['form'].setValue({
      sourceAccountId: 1001,
      destAccountId: 1002,
      amount: 300,
      description: 'Rent payment',
    });

    component.submit();
    tick();

    expect(transactionServiceSpy.transfer).toHaveBeenCalledWith(
      1001, 1002, 300, 'Rent payment'
    );
  }));

  it('should set result after successful transfer', fakeAsync(() => {
    transactionServiceSpy.transfer.and.returnValue(of(MOCK_TRANSFER_RESPONSE));

    component['form'].setValue({
      sourceAccountId: 1001,
      destAccountId: 1002,
      amount: 300,
      description: 'Test',
    });

    component.submit();
    tick();

    expect(component.result()).toBeTruthy();
    expect(component.result()!.status).toBe('COMPLETED');
  }));

  it('should set error message on service failure', fakeAsync(() => {
    transactionServiceSpy.transfer.and.returnValue(
      throwError(() => ({ error: { detail: 'Insufficient funds' } }))
    );

    component['form'].setValue({
      sourceAccountId: 1001,
      destAccountId: 1002,
      amount: 99999,
      description: 'Test',
    });

    component.submit();
    tick();

    expect(component.error).toBeTruthy();
  }));

  it('should set loading true during transfer and false after', fakeAsync(() => {
    transactionServiceSpy.transfer.and.returnValue(of(MOCK_TRANSFER_RESPONSE));

    component['form'].setValue({
      sourceAccountId: 1001,
      destAccountId: 1002,
      amount: 300,
      description: 'Test',
    });

    component.submit();
    expect(component.loading).toBeTrue();
    tick();
    expect(component.loading).toBeFalse();
  }));
});
