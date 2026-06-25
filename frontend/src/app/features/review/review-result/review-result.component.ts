import { Component, Input, Output, EventEmitter, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CodeReviewResponse } from '../../../core/models/review.model';
import { DiffViewerComponent } from '../../../shared/components/diff-viewer/diff-viewer.component';
import { MonacoEditorComponent } from '../../../shared/components/monaco-editor/monaco-editor.component';

@Component({
  selector: 'app-review-result',
  standalone: true,
  imports: [CommonModule, DiffViewerComponent, MonacoEditorComponent],
  templateUrl: './review-result.component.html',
  styleUrls: ['./review-result.component.scss']
})
export class ReviewResultComponent {
  @Input() review!: CodeReviewResponse;
  @Output() reReview = new EventEmitter<string>();

  viewMode = signal<'diff' | 'original' | 'optimized'>('diff');
  copiedField = signal<string | null>(null);

  get scoreLabel(): string {
    const s = this.review.score;
    if (s >= 9) return 'Excellent';
    if (s >= 7) return 'Good';
    if (s >= 5) return 'Average';
    if (s >= 3) return 'Needs Work';
    return 'Poor';
  }
  get scoreClass(): string {
    const s = this.review.score;
    if (s >= 8) return 'high';
    if (s >= 5) return 'mid';
    return 'low';
  }
  get scorePercent(): number { return (this.review.score / 10) * 100; }
  get issuesCount(): number { return this.review.issues?.length ?? 0; }
  get improvementsCount(): number { return this.review.improvements?.length ?? 0; }
  get bestPracticesCount(): number { return this.review.best_practices?.length ?? 0; }

  get monacoLang(): string {
    const map: Record<string, string> = { java: 'java', javascript: 'javascript', python: 'python' };
    return map[this.review.language] ?? 'java';
  }

  getSeverityIcon(severity: string): string {
    return ({ critical: 'X', warning: '!', info: 'i' })[severity?.toLowerCase()] ?? 'i';
  }

  copyCode() {
    const text = this.viewMode() === 'original'
      ? this.review.originalCode : this.review.optimized_code;
    this.copyToClipboard(text, 'code');
  }

  copyOptimized() { this.copyToClipboard(this.review.optimized_code, 'all'); }

  private copyToClipboard(text: string, field: string) {
    navigator.clipboard.writeText(text).then(() => {
      this.copiedField.set(field);
      setTimeout(() => this.copiedField.set(null), 2000);
    });
  }

  triggerReReview() {
    this.reReview.emit(this.review.optimized_code || this.review.originalCode);
  }
}
