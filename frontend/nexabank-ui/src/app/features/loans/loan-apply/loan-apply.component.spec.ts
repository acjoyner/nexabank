import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { LoanApplyComponent } from './loan-apply.component';
import { AuthService } from '../../../core/services/auth.service';
import { AccountService, AccountResponse } from '../../../core/services/account.service';
import { LoanService, LoanApplicationResponse } from '../../../core/services/loan.service';

const MOCK_ACCOUNTS: AccountResponse[] = [
  {
    id: 1001,
    accountNumber: 'CHK-0000001001',
    accountType: 'CHECKING',
    balance: 5000,
    status: 'ACTIVE',
    openedAt: '2025-01-01T00:00:00Z',
    customerId: 42,
    customerEmail: 'alice@nexabank.com',
  },
];

const MOCK_ELIGIBLE_RESPONSE: LoanApplicationResponse = {
  id: 1,
  customerId: 42,
  loanType: 'AUTO',
  requestedAmount: 20000,
  termMonths: 60,
  annualIncome: 80000,
  creditScore: 750,
  status: 'AI_REVIEW',
  aiDecision: 'ELIGIBLE',
  aiReason: 'Strong application. AI confidence score: 88/100.',
  strategyUsed: 'AI',
  appliedAt: new Date().toISOString(),
};

describe('LoanApplyComponent', () => {
  let fixture: ComponentFixture<LoanApplyComponent>;
  let component: LoanApplyComponent;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let accountServiceSpy: jasmine.SpyObj<AccountService>;
  let loanServiceSpy: jasmine.SpyObj<LoanService>;

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['getCustomerId']);
    accountServiceSpy = jasmine.createSpyObj('AccountService', ['getAccounts']);
    loanServiceSpy = jasmine.createSpyObj('LoanService', ['apply']);

    authServiceSpy.getCustomerId.and.returnValue(42);
    accountServiceSpy.getAccounts.and.returnValue(of(MOCK_ACCOUNTS));

    await TestBed.configureTestingModule({
      imports: [LoanApplyComponent, NoopAnimationsModule],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: AccountService, useValue: accountServiceSpy },
        { provide: LoanService, useValue: loanServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LoanApplyComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load accounts on init', () => {
    expect(accountServiceSpy.getAccounts).toHaveBeenCalledWith(42);
    expect(component.accounts().length).toBe(1);
  });

  it('should not submit when forms are invalid', () => {
    component.submit();
    expect(loanServiceSpy.apply).not.toHaveBeenCalled();
  });

  it('should submit with valid loan and financial forms', fakeAsync(() => {
    loanServiceSpy.apply.and.returnValue(of(MOCK_ELIGIBLE_RESPONSE));

    component['loanForm'].setValue({
      loanType: 'AUTO',
      requestedAmount: 20000,
      termMonths: 60,
    });
    component['financialForm'].setValue({
      annualIncome: 80000,
      creditScore: 750,
      accountId: 1001,
    });

    component.submit();
    tick();

    expect(loanServiceSpy.apply).toHaveBeenCalled();
  }));

  it('should display AI decision after successful submission', fakeAsync(() => {
    loanServiceSpy.apply.and.returnValue(of(MOCK_ELIGIBLE_RESPONSE));

    component['loanForm'].setValue({
      loanType: 'AUTO',
      requestedAmount: 20000,
      termMonths: 60,
    });
    component['financialForm'].setValue({
      annualIncome: 80000,
      creditScore: 750,
      accountId: 1001,
    });

    component.submit();
    tick();

    expect(component.result()).toBeTruthy();
    expect(component.result()!.aiDecision).toBe('ELIGIBLE');
  }));

  it('getDecisionColor() returns green for ELIGIBLE', () => {
    expect(component.getDecisionColor('ELIGIBLE')).toBe('green');
  });

  it('getDecisionColor() returns red for INELIGIBLE', () => {
    expect(component.getDecisionColor('INELIGIBLE')).toBe('red');
  });

  it('getDecisionColor() returns orange for REVIEW', () => {
    expect(component.getDecisionColor('REVIEW')).toBe('orange');
  });

  it('should set error on service failure', fakeAsync(() => {
    loanServiceSpy.apply.and.returnValue(
      throwError(() => ({ error: { detail: 'Service error' } }))
    );

    component['loanForm'].setValue({
      loanType: 'AUTO',
      requestedAmount: 20000,
      termMonths: 60,
    });
    component['financialForm'].setValue({
      annualIncome: 80000,
      creditScore: 750,
      accountId: 1001,
    });

    component.submit();
    tick();

    expect(component.error).toBeTruthy();
  }));
});
