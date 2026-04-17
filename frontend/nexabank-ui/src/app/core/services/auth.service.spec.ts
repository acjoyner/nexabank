import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { AuthService, AuthResponse } from './auth.service';

const MOCK_RESPONSE: AuthResponse = {
  token: 'test-jwt-token',
  tokenType: 'Bearer',
  expiresAt: '2099-01-01T00:00:00Z',
  customerId: 42,
  email: 'alice@nexabank.com',
  fullName: 'Alice Smith',
};

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(() => {
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        AuthService,
        { provide: Router, useValue: routerSpy },
      ],
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    localStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('login() sends POST to /api/auth/login', () => {
    service.login('alice@nexabank.com', 'Password1!').subscribe();

    const req = httpMock.expectOne('/api/auth/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      email: 'alice@nexabank.com',
      password: 'Password1!',
    });
    req.flush(MOCK_RESPONSE);
  });

  it('login() stores token and customer in localStorage', () => {
    service.login('alice@nexabank.com', 'Password1!').subscribe();

    const req = httpMock.expectOne('/api/auth/login');
    req.flush(MOCK_RESPONSE);

    expect(localStorage.getItem('nexabank_token')).toBe('test-jwt-token');
    expect(JSON.parse(localStorage.getItem('nexabank_customer')!).email)
      .toBe('alice@nexabank.com');
  });

  it('register() sends POST to /api/auth/register', () => {
    const registerData = {
      email: 'bob@nexabank.com',
      password: 'Pass1!',
      firstName: 'Bob',
      lastName: 'Jones',
      phone: '555-1234',
    };
    service.register(registerData).subscribe();

    const req = httpMock.expectOne('/api/auth/register');
    expect(req.request.method).toBe('POST');
    req.flush(MOCK_RESPONSE);
  });

  it('isLoggedIn() returns true when token exists', () => {
    localStorage.setItem('nexabank_token', 'some-token');
    expect(service.isLoggedIn()).toBeTrue();
  });

  it('isLoggedIn() returns false when no token', () => {
    expect(service.isLoggedIn()).toBeFalse();
  });

  it('getCustomer() returns parsed customer from localStorage', () => {
    localStorage.setItem('nexabank_customer', JSON.stringify(MOCK_RESPONSE));
    const customer = service.getCustomer();
    expect(customer?.email).toBe('alice@nexabank.com');
    expect(customer?.customerId).toBe(42);
  });

  it('getCustomerId() returns customerId', () => {
    localStorage.setItem('nexabank_customer', JSON.stringify(MOCK_RESPONSE));
    expect(service.getCustomerId()).toBe(42);
  });

  it('logout() clears localStorage and redirects to login', () => {
    localStorage.setItem('nexabank_token', 'some-token');
    localStorage.setItem('nexabank_customer', JSON.stringify(MOCK_RESPONSE));

    service.logout();

    expect(localStorage.getItem('nexabank_token')).toBeNull();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);
  });
});
