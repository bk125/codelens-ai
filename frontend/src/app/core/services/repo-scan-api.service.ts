import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/review.model';
import {
  RepoScanRequest, RepoScanStatusResponse, RepoScanHistorySummary
} from '../models/security-scan.model';

@Injectable({ providedIn: 'root' })
export class RepoScanApiService {
  private readonly base = environment.apiBaseUrl + '/security-scan';

  constructor(private http: HttpClient) {}

  startScan(request: RepoScanRequest): Observable<RepoScanStatusResponse> {
    return this.http.post<ApiResponse<RepoScanStatusResponse>>(`${this.base}/start`, request)
      .pipe(map(r => r.data));
  }

  getStatus(id: number): Observable<RepoScanStatusResponse> {
    return this.http.get<ApiResponse<RepoScanStatusResponse>>(`${this.base}/${id}/status`)
      .pipe(map(r => r.data));
  }

  getHistory(): Observable<RepoScanHistorySummary[]> {
    return this.http.get<ApiResponse<RepoScanHistorySummary[]>>(`${this.base}/history`)
      .pipe(map(r => r.data));
  }

  delete(id: number): Observable<void> {
    return this.http.delete<ApiResponse<void>>(`${this.base}/${id}`)
      .pipe(map(() => void 0));
  }
}
