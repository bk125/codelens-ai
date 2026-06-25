import { Component, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MigrationApiService } from '../../core/services/migration-api.service';
import {
  MigrationStatusResponse,
  MigrationHistorySummary,
  FileSummaryEntry,
  MIGRATION_TARGET_LANGUAGES,
  TargetLang
} from '../../core/models/migration.model';

@Component({
  selector: 'app-migration',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './migration.component.html',
  styleUrls: ['./migration.component.scss']
})
export class MigrationComponent implements OnDestroy {
  // Upload state
  selectedFile = signal<File | null>(null);
  selectedTarget = signal<TargetLang>(MIGRATION_TARGET_LANGUAGES[0]);
  isDragging = signal(false);
  uploadError = signal<string | null>(null);

  // Migration state
  isStarting = signal(false);
  currentMigration = signal<MigrationStatusResponse | null>(null);
  activeTab = signal<'progress' | 'plan' | 'files'>('progress');

  // History
  history = signal<MigrationHistorySummary[]>([]);
  showHistory = signal(false);
  isLoadingHistory = signal(false);

  readonly languages = MIGRATION_TARGET_LANGUAGES;
  readonly MAX_SIZE = 50 * 1024 * 1024; // 50MB

  private pollInterval: any = null;

  constructor(private apiService: MigrationApiService) {}

  ngOnDestroy() { this.stopPolling(); }

  // ── File Selection ────────────────────────────────────────────────────────

  onDragOver(e: DragEvent) { e.preventDefault(); this.isDragging.set(true); }
  onDragLeave(e: DragEvent) { e.preventDefault(); this.isDragging.set(false); }

  onDrop(e: DragEvent) {
    e.preventDefault();
    this.isDragging.set(false);
    const file = e.dataTransfer?.files?.[0];
    if (file) this.validateAndSetFile(file);
  }

  onFileSelected(e: Event) {
    const file = (e.target as HTMLInputElement).files?.[0];
    if (file) this.validateAndSetFile(file);
    (e.target as HTMLInputElement).value = '';
  }

  private validateAndSetFile(file: File) {
    this.uploadError.set(null);
    if (!file.name.toLowerCase().endsWith('.zip')) {
      this.uploadError.set('Only ZIP files are supported. Please upload a .zip archive of your project.');
      return;
    }
    if (file.size > this.MAX_SIZE) {
      this.uploadError.set(`File too large (${this.formatSize(file.size)}). Maximum allowed is 50 MB.`);
      return;
    }
    this.selectedFile.set(file);
  }

  removeFile() { this.selectedFile.set(null); this.uploadError.set(null); }

  selectTarget(lang: TargetLang) { this.selectedTarget.set(lang); }

  // ── Start Migration ────────────────────────────────────────────────────────

  startMigration() {
    const file = this.selectedFile();
    if (!file) return;

    this.isStarting.set(true);
    this.uploadError.set(null);

    this.apiService.startMigration(file, this.selectedTarget().value).subscribe({
      next: (status) => {
        this.currentMigration.set(status);
        this.isStarting.set(false);
        this.activeTab.set('progress');
        this.startPolling(status.id);
      },
      error: (err: Error) => {
        this.uploadError.set(err.message);
        this.isStarting.set(false);
      }
    });
  }

  // ── Polling ────────────────────────────────────────────────────────────────

  private startPolling(id: number) {
    this.stopPolling();
    this.pollInterval = setInterval(() => {
      this.apiService.getStatus(id).subscribe({
        next: (status) => {
          this.currentMigration.set(status);
          if (status.status === 'COMPLETED' || status.status === 'FAILED') {
            this.stopPolling();
            if (status.status === 'COMPLETED') this.activeTab.set('plan');
          }
        },
        error: () => this.stopPolling()
      });
    }, 3000); // Poll every 3 seconds
  }

  private stopPolling() {
    if (this.pollInterval) {
      clearInterval(this.pollInterval);
      this.pollInterval = null;
    }
  }

  // ── Download ───────────────────────────────────────────────────────────────

  cancelMigration() {
    const m = this.currentMigration();
    if (!m) return;

    if (confirm('Are you sure you want to cancel this migration? All progress will be lost.')) {
      this.apiService.delete(m.id).subscribe({
        next: () => {
          this.resetAll();
        },
        error: (err) => {
          this.uploadError.set('Failed to cancel migration: ' + err.message);
        }
      });
    }
  }

  downloadResult() {
    const m = this.currentMigration();
    if (!m || !m.downloadReady) return;

    // Use anchor trick to trigger browser download with JWT auth header
    // We need to fetch with auth then create blob URL
    const token = localStorage.getItem('acr_auth_user')
      ? JSON.parse(localStorage.getItem('acr_auth_user')!).token : null;

    if (!token) return;

    fetch(this.apiService.getDownloadUrl(m.id), {
      headers: { Authorization: `Bearer ${token}` }
    })
    .then(res => res.blob())
    .then(blob => {
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `migrated-${m.projectName}.zip`;
      a.click();
      URL.revokeObjectURL(url);
    })
    .catch(err => console.error('Download failed:', err));
  }

  // ── History ────────────────────────────────────────────────────────────────

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

  loadHistoryItem(id: number) {
    this.apiService.getStatus(id).subscribe({
      next: (status) => {
        this.currentMigration.set(status);
        this.showHistory.set(false);
        this.activeTab.set(status.status === 'COMPLETED' ? 'plan' : 'progress');
        if (status.status === 'PROCESSING' || status.status === 'PENDING') {
          this.startPolling(id);
        }
      },
      error: () => {}
    });
  }

  deleteHistory(id: number, e: Event) {
    e.stopPropagation();
    if (!confirm('Delete this migration?')) return;
    this.apiService.delete(id).subscribe({
      next: () => {
        this.history.update(h => h.filter(i => i.id !== id));
        if (this.currentMigration()?.id === id) {
          this.currentMigration.set(null);
          this.stopPolling();
        }
      },
      error: () => {}
    });
  }

  resetAll() {
    this.stopPolling();
    this.currentMigration.set(null);
    this.selectedFile.set(null);
    this.uploadError.set(null);
  }

  // ── Computed helpers ───────────────────────────────────────────────────────

  getFileSummaries(): FileSummaryEntry[] {
    const m = this.currentMigration();
    if (!m?.fileSummaryJson) return [];
    try { return JSON.parse(m.fileSummaryJson); } catch { return []; }
  }

  getMigratedCount(): number {
    return this.getFileSummaries().filter(f => f.status === 'migrated').length;
  }

  getCopiedCount(): number {
    return this.getFileSummaries().filter(f => f.status === 'copied').length;
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'COMPLETED':  return 'success';
      case 'PROCESSING': return 'processing';
      case 'PENDING':    return 'pending';
      case 'FAILED':     return 'failed';
      default: return '';
    }
  }

  getStatusIcon(status: string): string {
    switch (status) {
      case 'COMPLETED':  return '✓';
      case 'PROCESSING': return '◌';
      case 'PENDING':    return '○';
      case 'FAILED':     return '✕';
      default: return '○';
    }
  }

  getLangIcon(lang: string): string {
    return MIGRATION_TARGET_LANGUAGES.find(
      l => l.value.toLowerCase() === lang?.toLowerCase()
    )?.icon ?? '◈';
  }

  formatSize(bytes: number): string {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  }

  get canStart(): boolean {
    return !!this.selectedFile() && !this.isStarting();
  }

  get isRunning(): boolean {
    const s = this.currentMigration()?.status;
    return s === 'PENDING' || s === 'PROCESSING';
  }

  renderMarkdown(text: string): string {
    if (!text) return '';
    return text
      .replace(/^### (.+)$/gm, '<h3>$1</h3>')
      .replace(/^## (.+)$/gm, '<h2>$1</h2>')
      .replace(/^# (.+)$/gm, '<h1>$1</h1>')
      .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
      .replace(/\*(.+?)\*/g, '<em>$1</em>')
      .replace(/^\| (.+) \|$/gm, (m) => {
        const cells = m.split('|').filter(c => c.trim()).map(c => `<td>${c.trim()}</td>`).join('');
        return `<tr>${cells}</tr>`;
      })
      .replace(/^\|[-| ]+\|$/gm, '')
      .replace(/(<tr>.*<\/tr>)/gs, '<table class="md-table">$1</table>')
      .replace(/^- (.+)$/gm, '<li>$1</li>')
      .replace(/(<li>.*<\/li>)/gs, '<ul>$1</ul>')
      .replace(/^\d+\. (.+)$/gm, '<li>$1</li>')
      .replace(/`(.+?)`/g, '<code>$1</code>')
      .replace(/\n\n/g, '</p><p>')
      .replace(/^(?!<[huptlc])/gm, '')
      .trim();
  }

}