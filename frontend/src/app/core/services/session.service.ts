import { Injectable } from '@angular/core';

const SESSION_KEY = 'acr_session_id';

@Injectable({ providedIn: 'root' })
export class SessionService {
  private sessionId: string;

  constructor() {
    const stored = localStorage.getItem(SESSION_KEY);
    if (stored) {
      this.sessionId = stored;
    } else {
      this.sessionId = this.generateId();
      localStorage.setItem(SESSION_KEY, this.sessionId);
    }
  }

  getSessionId(): string {
    return this.sessionId;
  }

  reset(): void {
    this.sessionId = this.generateId();
    localStorage.setItem(SESSION_KEY, this.sessionId);
  }

  private generateId(): string {
    // Native UUID v4 — no external dependency needed
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
      const r = Math.random() * 16 | 0;
      const v = c === 'x' ? r : (r & 0x3 | 0x8);
      return v.toString(16);
    });
  }
}
