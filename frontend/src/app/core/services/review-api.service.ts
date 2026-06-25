import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import {
  ApiResponse,
  CodeReviewRequest,
  CodeReviewResponse,
  ReviewHistorySummary,
  SessionStats
} from '../models/review.model';

@Injectable({ providedIn: 'root' })
export class ReviewApiService {
  private readonly base = environment.apiBaseUrl + '/reviews';

  constructor(private http: HttpClient) {}

  submitReview(request: CodeReviewRequest): Observable<CodeReviewResponse> {
    return this.http.post<ApiResponse<CodeReviewResponse>>(this.base, request)
      .pipe(map(r => r.data));
  }

  reReview(id: number, request: CodeReviewRequest): Observable<CodeReviewResponse> {
    return this.http.post<ApiResponse<CodeReviewResponse>>(`${this.base}/${id}/re-review`, request)
      .pipe(map(r => r.data));
  }

  getReview(id: number): Observable<CodeReviewResponse> {
    return this.http.get<ApiResponse<CodeReviewResponse>>(`${this.base}/${id}`)
      .pipe(map(r => r.data));
  }

  getHistory(): Observable<ReviewHistorySummary[]> {
    return this.http.get<ApiResponse<ReviewHistorySummary[]>>(`${this.base}/history`)
      .pipe(map(r => r.data));
  }

  getStats(): Observable<SessionStats> {
    return this.http.get<ApiResponse<SessionStats>>(`${this.base}/stats`)
      .pipe(map(r => r.data));
  }

  deleteReview(id: number): Observable<void> {
    return this.http.delete<ApiResponse<void>>(`${this.base}/${id}`)
      .pipe(map(() => void 0));
  }
}
