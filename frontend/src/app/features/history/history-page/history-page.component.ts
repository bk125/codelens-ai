import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ReviewApiService } from '../../../core/services/review-api.service';
import { AuthService } from '../../../core/services/auth.service';
import { ReviewHistorySummary, SessionStats, CodeReviewResponse } from '../../../core/models/review.model';

@Component({
  selector: 'app-history-page',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './history-page.component.html',
  styleUrls: ['./history-page.component.scss']
})
export class HistoryPageComponent implements OnInit {
  history = signal<ReviewHistorySummary[]>([]);
  stats = signal<SessionStats | null>(null);
  isLoading = signal(true);
  error = signal<string | null>(null);
  deletingId = signal<number | null>(null);

  selectedReview = signal<CodeReviewResponse | null>(null);
  isLoadingDetail = signal(false);
  viewMode = signal<'diff' | 'original' | 'optimized'>('diff');
  copiedField = signal<string | null>(null);
  isMaximized = signal(false);

  constructor(
    private apiService: ReviewApiService,
    public authService: AuthService,
    private router: Router
  ) {}

  ngOnInit() { this.loadAll(); }

  private loadAll() {
    this.isLoading.set(true);
    this.apiService.getHistory().subscribe({
      next: (items) => { this.history.set(items); this.isLoading.set(false); },
      error: (err: Error) => { this.error.set(err.message); this.isLoading.set(false); }
    });
    this.apiService.getStats().subscribe({
      next: (s) => this.stats.set(s),
      error: () => {}
    });
  }

  openReview(id: number) {
    this.isLoadingDetail.set(true);
    this.selectedReview.set(null);
    this.viewMode.set('diff');
    this.apiService.getReview(id).subscribe({
      next: (review) => { this.selectedReview.set(review); this.isLoadingDetail.set(false); },
      error: (err: Error) => { this.error.set(err.message); this.isLoadingDetail.set(false); }
    });
  }

  closeDetail() { this.selectedReview.set(null); this.isMaximized.set(false); }

  toggleMaximize() { this.isMaximized.update(v => !v); }

  deleteReview(id: number, event: Event) {
    event.stopPropagation();
    if (!confirm('Delete this review?')) return;
    this.deletingId.set(id);
    this.apiService.deleteReview(id).subscribe({
      next: () => {
        this.history.update(h => h.filter(r => r.id !== id));
        if (this.selectedReview()?.id === id) this.selectedReview.set(null);
        this.deletingId.set(null);
      },
      error: (err: Error) => { this.error.set(err.message); this.deletingId.set(null); }
    });
  }

  copyOptimized() {
    const review = this.selectedReview();
    if (!review) return;
    navigator.clipboard.writeText(review.optimized_code).then(() => {
      this.copiedField.set('code');
      setTimeout(() => this.copiedField.set(null), 2000);
    });
  }

  reReviewSelected() {
    const review = this.selectedReview();
    if (!review) return;
    this.router.navigate(['/review'], {
      state: { code: review.optimized_code || review.originalCode, language: review.language }
    });
  }

  goToReview() { this.router.navigate(['/review']); }

  getScoreClass(score: number): string {
    if (score >= 8) return 'high';
    if (score >= 5) return 'mid';
    return 'low';
  }
  getLangIcon(lang: string): string {
    return ({ java: '☕', javascript: '⚡', python: '🐍' } as Record<string,string>)[lang] ?? '◈';
  }
  getMonacoLang(lang: string): string {
    return ({ java: 'java', javascript: 'javascript', python: 'python' } as Record<string,string>)[lang] ?? 'java';
  }
  getSeverityIcon(severity: string): string {
    return ({ critical: 'X', warning: '!', info: 'i' } as Record<string,string>)[severity] ?? 'i';
  }
  trackById(_: number, item: ReviewHistorySummary) { return item.id; }
}
