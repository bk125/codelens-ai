import { Injectable } from '@angular/core';
import { HttpClient, HttpEvent, HttpRequest } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/review.model';
import { MigrationStatusResponse, MigrationHistorySummary } from '../models/migration.model';

@Injectable({ providedIn: 'root' })
export class MigrationApiService {
  private readonly base = environment.apiBaseUrl + '/migration';

  constructor(private http: HttpClient) {}

  startMigration(file: File, targetLanguage: string): Observable<MigrationStatusResponse> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('targetLanguage', targetLanguage);
    return this.http.post<ApiResponse<MigrationStatusResponse>>(`${this.base}/start`, formData)
      .pipe(map(r => r.data));
  }

  getStatus(id: number): Observable<MigrationStatusResponse> {
    return this.http.get<ApiResponse<MigrationStatusResponse>>(`${this.base}/${id}/status`)
      .pipe(map(r => r.data));
  }

  getHistory(): Observable<MigrationHistorySummary[]> {
    return this.http.get<ApiResponse<MigrationHistorySummary[]>>(`${this.base}/history`)
      .pipe(map(r => r.data));
  }

  getDownloadUrl(id: number): string {
    return `${this.base}/${id}/download`;
  }

  delete(id: number): Observable<void> {
    return this.http.delete<ApiResponse<void>>(`${this.base}/${id}`)
      .pipe(map(() => void 0));
  }
}
