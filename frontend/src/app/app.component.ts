import { Component } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from './core/services/auth.service';
import { ThemeService } from './core/services/theme.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule],
  template: `
    <div class="app-shell">
      <nav class="navbar" *ngIf="authService.isLoggedIn()">
        <div class="nav-brand">
          <div class="nav-hex">CL</div>
          <span class="brand-name">CodeLens <span class="brand-ai">AI</span></span>
        </div>

        <div class="nav-links">
          <a routerLink="/review"        routerLinkActive="active" class="nav-link">&#x25C8; Review</a>
          <a routerLink="/history"       routerLinkActive="active" class="nav-link">&#x25CE; History</a>
          <a routerLink="/explainer"     routerLinkActive="active" class="nav-link">&#x1F4A1; Explainer</a>
          <a routerLink="/migration"     routerLinkActive="active" class="nav-link">&#x1F680; Migration</a>
          <a routerLink="/security-scan" routerLinkActive="active" class="nav-link nav-link-security">
            &#x1F6E1; Security
          </a>
        </div>

        <div class="nav-right">
          <button class="theme-toggle" (click)="themeService.toggle()"
            [title]="themeService.isDark() ? 'Switch to light mode' : 'Switch to dark mode'">
            <span *ngIf="themeService.isDark()">&#9728;</span>
            <span *ngIf="!themeService.isDark()">&#9790;</span>
          </button>
          <span class="nav-user">
            <span class="user-dot"></span>
            {{ authService.currentUser()?.name }}
          </span>
          <button class="btn-logout" (click)="authService.logout()">Sign out</button>
        </div>
      </nav>

      <main class="main-content">
        <router-outlet />
      </main>
    </div>
  `,
  styles: [`
    .app-shell { min-height: 100vh; display: flex; flex-direction: column; }
    .main-content { flex: 1; }

    .nav-hex {
      width: 30px; height: 30px; flex-shrink: 0;
      background: linear-gradient(135deg, var(--accent), #7c5cfc);
      clip-path: polygon(50% 0%,100% 25%,100% 75%,50% 100%,0% 75%,0% 25%);
      display: flex; align-items: center; justify-content: center;
      font-size: 0.65rem; font-weight: 800; color: #000;
    }
    .brand-name {
      font-family: 'Space Mono', monospace; font-size: 1rem;
      font-weight: 700; color: var(--text-primary);
    }
    .brand-ai { color: var(--accent); }

    .nav-link-security {
      position: relative;
      &.active { color: #f87171 !important; }
      &:hover { color: #f87171 !important; }
    }

    .nav-right { margin-left: auto; display: flex; align-items: center; gap: 0.75rem; }
    .nav-user {
      display: flex; align-items: center; gap: 0.5rem;
      font-family: 'Space Mono', monospace; font-size: 0.8rem; color: var(--text-muted);
    }
    .user-dot { width: 7px; height: 7px; background: var(--accent); border-radius: 50%; }
    .theme-toggle {
      width: 34px; height: 34px; display: flex; align-items: center; justify-content: center;
      background: var(--surface-raised); border: 1px solid var(--border);
      border-radius: 8px; color: var(--text-muted); font-size: 1rem;
      cursor: pointer; transition: all 0.2s;
      &:hover { border-color: var(--accent); color: var(--accent); }
    }
    .btn-logout {
      padding: 0.3rem 0.85rem; background: transparent; border: 1px solid var(--border);
      border-radius: 6px; color: var(--text-muted); font-size: 0.78rem;
      font-family: 'Space Mono', monospace; cursor: pointer; transition: all 0.2s;
      &:hover { border-color: #f87171; color: #f87171; }
    }
  `]
})
export class AppComponent {
  constructor(public authService: AuthService, public themeService: ThemeService) {}
}
