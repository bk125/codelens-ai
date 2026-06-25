import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/review.model';
import { GithubStatusResponse } from '../models/security-scan.model';

@Injectable({ providedIn: 'root' })
export class GithubOAuthService {
  private readonly base = environment.apiBaseUrl + '/github';

  constructor(private http: HttpClient) {}

  getAuthorizeUrl(): Observable<string> {
    return this.http.get<ApiResponse<string>>(`${this.base}/authorize`)
      .pipe(map(r => r.data));
  }

  getStatus(): Observable<GithubStatusResponse> {
    return this.http.get<ApiResponse<GithubStatusResponse>>(`${this.base}/status`)
      .pipe(map(r => r.data));
  }

  disconnect(): Observable<void> {
    return this.http.delete<ApiResponse<void>>(`${this.base}/disconnect`)
      .pipe(map(() => void 0));
  }
}
