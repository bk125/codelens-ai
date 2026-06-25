import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/review.model';
import { ExplainRequest, ExplainResponse, ExplainHistorySummary } from '../models/explainer.model';

@Injectable({ providedIn: 'root' })
export class ExplainerApiService {
  private readonly base = environment.apiBaseUrl + '/explainer';

  constructor(private http: HttpClient) {}

  explain(request: ExplainRequest): Observable<ExplainResponse> {
    return this.http.post<ApiResponse<ExplainResponse>>(this.base, request)
      .pipe(map(r => r.data));
  }

  getHistory(): Observable<ExplainHistorySummary[]> {
    return this.http.get<ApiResponse<ExplainHistorySummary[]>>(`${this.base}/history`)
      .pipe(map(r => r.data));
  }

  getById(id: number): Observable<ExplainResponse> {
    return this.http.get<ApiResponse<ExplainResponse>>(`${this.base}/${id}`)
      .pipe(map(r => r.data));
  }

  delete(id: number): Observable<void> {
    return this.http.delete<ApiResponse<void>>(`${this.base}/${id}`)
      .pipe(map(() => void 0));
  }
}
