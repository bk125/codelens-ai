import { Component, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ExplainerApiService } from '../../core/services/explainer-api.service';
import { ExplainResponse, ExplainHistorySummary, FunctionBreakdown } from '../../core/models/explainer.model';
import { MonacoEditorComponent } from '../../shared/components/monaco-editor/monaco-editor.component';
import { FileUploadComponent, FileReadResult } from '../../shared/components/file-upload/file-upload.component';
import { LanguageDetectorService } from '../../core/services/language-detector.service';

@Component({
  selector: 'app-explainer',
  standalone: true,
  imports: [CommonModule, MonacoEditorComponent, FileUploadComponent],
  templateUrl: './explainer.component.html',
  styleUrls: ['./explainer.component.scss']
})
export class ExplainerComponent {
  code = signal('');
  monacoLang = signal('plaintext');
  inputMode = signal<'editor' | 'file'>('editor');
  loadedFileName = signal<string | null>(null);
  isLoading = signal(false);
  error = signal<string | null>(null);
  result = signal<ExplainResponse | null>(null);

  history = signal<ExplainHistorySummary[]>([]);
  showHistory = signal(false);
  isLoadingHistory = signal(false);
  copiedField = signal<string | null>(null);

  charCount = computed(() => this.code().length);
  canExplain = computed(() => this.code().trim().length > 0 && !this.isLoading());

  private detectTimer: any = null;

  constructor(
    private apiService: ExplainerApiService,
    private languageDetector: LanguageDetectorService
  ) {}

  onCodeChange(value: string) {
    this.code.set(value);
    clearTimeout(this.detectTimer);
    this.detectTimer = setTimeout(() => {
      const detected = this.languageDetector.detect(value);
      if (detected !== this.monacoLang()) this.monacoLang.set(detected);
    }, 400);
  }

  onFileLoaded(file: FileReadResult) {
    this.code.set(file.content);
    this.loadedFileName.set(file.fileName);
    this.error.set(null);
    this.monacoLang.set(this.languageDetector.fromExtension(file.fileName));
  }

  onFileError(msg: string) { this.error.set(msg); }

  switchInputMode(mode: 'editor' | 'file') {
    this.inputMode.set(mode);
    this.error.set(null);
    if (mode === 'editor') this.loadedFileName.set(null);
  }

  explain() {
    if (!this.canExplain()) return;
    this.isLoading.set(true);
    this.error.set(null);
    this.result.set(null);

    this.apiService.explain({ code: this.code() }).subscribe({
      next: (res) => {
        this.result.set(res);
        this.isLoading.set(false);
        // Update Monaco with AI-confirmed language
        if (res.language) {
          const detected = this.languageDetector.fromExtension(
            res.language.toLowerCase().replace('#', 's')
          );
          if (detected !== 'plaintext') this.monacoLang.set(detected);
        }
      },
      error: (err: Error) => { this.error.set(err.message); this.isLoading.set(false); }
    });
  }

  clearAll() {
    this.code.set('');
    this.result.set(null);
    this.error.set(null);
    this.loadedFileName.set(null);
    this.monacoLang.set('plaintext');
    clearTimeout(this.detectTimer);
  }

  toggleHistory() {
    this.showHistory.update(v => !v);
    if (this.showHistory() && this.history().length === 0) this.loadHistory();
  }

  private loadHistory() {
    this.isLoadingHistory.set(true);
    this.apiService.getHistory().subscribe({
      next: (h) => { this.history.set(h); this.isLoadingHistory.set(false); },
      error: () => this.isLoadingHistory.set(false)
    });
  }

  openHistoryItem(id: number) {
    this.apiService.getById(id).subscribe({
      next: (res) => {
        this.result.set(res);
        this.code.set(res.originalCode);
        this.showHistory.set(false);
        this.monacoLang.set(this.languageDetector.detect(res.originalCode));
      },
      error: () => {}
    });
  }

  deleteHistoryItem(id: number, event: Event) {
    event.stopPropagation();
    if (!confirm('Delete this explanation?')) return;
    this.apiService.delete(id).subscribe({
      next: () => this.history.update(h => h.filter(i => i.id !== id)),
      error: () => {}
    });
  }

  copyOverview() {
    const r = this.result();
    if (!r) return;
    navigator.clipboard.writeText(r.overview + '\n\n' + r.howItWorks).then(() => {
      this.copiedField.set('overview');
      setTimeout(() => this.copiedField.set(null), 2000);
    });
  }

  trackByFn(_: number, fn: FunctionBreakdown) { return fn.name; }
}
