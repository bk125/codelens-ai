import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { AuthUser, LoginRequest, RegisterRequest, AuthResponse } from '../models/auth.model';
import { ApiResponse } from '../models/review.model';

const AUTH_KEY = 'acr_auth_user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly base = environment.apiBaseUrl + '/auth';

  currentUser = signal<AuthUser | null>(this.loadFromStorage());

  constructor(private http: HttpClient, private router: Router) {}

  register(request: RegisterRequest): Observable<any> {
    return this.http.post<ApiResponse<any>>(`${this.base}/register`, request).pipe(
      map(r => r.data)
    );
  }

  verifyEmail(email: string, code: string): Observable<AuthUser> {
    return this.http.post<ApiResponse<AuthResponse>>(`${this.base}/verify`, { email, code }).pipe(
      map(r => r.data),
      tap(user => this.setUser(user))
    );
  }

  login(request: LoginRequest): Observable<AuthUser> {
    return this.http.post<ApiResponse<AuthResponse>>(`${this.base}/login`, request).pipe(
      map(r => r.data),
      tap(user => this.setUser(user))
    );
  }

  forgotPassword(email: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.base}/forgot-password`, { email });
  }

  resetPassword(token: string, newPassword: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.base}/reset-password`, { token, newPassword });
  }

  logout(): void {
    localStorage.removeItem(AUTH_KEY);
    this.currentUser.set(null);
    window.location.href = 'http://localhost:8080/landing.html';
  }

  getToken(): string | null {
    return this.currentUser()?.token ?? null;
  }

  isLoggedIn(): boolean {
    return this.currentUser() !== null;
  }

  private setUser(user: AuthUser): void {
    localStorage.setItem(AUTH_KEY, JSON.stringify(user));
    this.currentUser.set(user);
  }

  private loadFromStorage(): AuthUser | null {
    try {
      const stored = localStorage.getItem(AUTH_KEY);
      return stored ? JSON.parse(stored) : null;
    } catch {
      return null;
    }
  }
}
