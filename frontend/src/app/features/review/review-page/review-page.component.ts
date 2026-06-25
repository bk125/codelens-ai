import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ReviewApiService } from '../../../core/services/review-api.service';
import { CodeReviewResponse, CodeReviewRequest } from '../../../core/models/review.model';
import { ReviewResultComponent } from '../review-result/review-result.component';
import { MonacoEditorComponent } from '../../../shared/components/monaco-editor/monaco-editor.component';
import { FileUploadComponent, FileReadResult } from '../../../shared/components/file-upload/file-upload.component';
import { LanguageDetectorService } from '../../../core/services/language-detector.service';

@Component({
  selector: 'app-review-page',
  standalone: true,
  imports: [CommonModule, ReviewResultComponent, MonacoEditorComponent, FileUploadComponent],
  templateUrl: './review-page.component.html',
  styleUrls: ['./review-page.component.scss']
})
export class ReviewPageComponent implements OnInit {
  code = signal('');
  monacoLang = signal('plaintext'); // local display only — drives Monaco highlighting
  isLoading = signal(false);
  error = signal<string | null>(null);
  result = signal<CodeReviewResponse | null>(null);
  activeTab = signal<'editor' | 'result'>('editor');
  inputMode = signal<'editor' | 'file'>('editor');
  loadedFileName = signal<string | null>(null);

  charCount = computed(() => this.code().length);
  canSubmit = computed(() => this.code().trim().length > 0 && !this.isLoading());

  // Debounce timer for language detection while typing
  private detectTimer: any = null;

  constructor(
    private apiService: ReviewApiService,
    private router: Router,
    private languageDetector: LanguageDetectorService
  ) {}

  ngOnInit() {
    const nav = this.router.getCurrentNavigation();
    const state = nav?.extras?.state as { code?: string; language?: string } | undefined;
    if (state?.code) {
      this.code.set(state.code);
      // Detect language from pasted code immediately
      this.monacoLang.set(this.languageDetector.detect(state.code));
    }
  }

  onCodeChange(value: string) {
    this.code.set(value);

    // Debounce: detect language 400ms after user stops typing
    // so Monaco doesn't re-highlight on every keypress
    clearTimeout(this.detectTimer);
    this.detectTimer = setTimeout(() => {
      const detected = this.languageDetector.detect(value);
      if (detected !== this.monacoLang()) {
        this.monacoLang.set(detected);
      }
    }, 400);
  }

  onFileLoaded(file: FileReadResult) {
    this.code.set(file.content);
    this.loadedFileName.set(file.fileName);
    this.error.set(null);
    // File extension gives instant accurate detection
    this.monacoLang.set(this.languageDetector.fromExtension(file.fileName));
  }

  onFileError(msg: string) { this.error.set(msg); }

  switchMode(mode: 'editor' | 'file') {
    this.inputMode.set(mode);
    this.error.set(null);
    if (mode === 'editor') {
      this.loadedFileName.set(null);
    }
  }

  submitReview() {
    if (!this.canSubmit()) return;
    this.isLoading.set(true);
    this.error.set(null);
    this.result.set(null);

    // No language sent to backend — AI detects it
    const request: CodeReviewRequest = { code: this.code() };

    this.apiService.submitReview(request).subscribe({
      next: (res) => {
        this.result.set(res);
        this.isLoading.set(false);
        this.activeTab.set('result');
        // Update Monaco to AI-confirmed language after review
        if (res.language) {
          const monacoId = this.languageDetector.fromExtension(
            res.language.toLowerCase().replace('#', 's') // C# -> cs
          );
          if (monacoId !== 'plaintext') this.monacoLang.set(monacoId);
        }
      },
      error: (err: Error) => {
        this.error.set(err.message);
        this.isLoading.set(false);
      }
    });
  }

  clearAll() {
    this.code.set('');
    this.result.set(null);
    this.error.set(null);
    this.activeTab.set('editor');
    this.loadedFileName.set(null);
    this.monacoLang.set('plaintext');
    clearTimeout(this.detectTimer);
  }

  onReReview(updatedCode: string) {
    this.code.set(updatedCode);
    this.monacoLang.set(this.languageDetector.detect(updatedCode));
    this.activeTab.set('editor');
  }

  getScoreClass(score: number): string {
    if (score >= 8) return 'high';
    if (score >= 5) return 'mid';
    return 'low';
  }
}
