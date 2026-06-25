import { Component, OnDestroy, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { GithubOAuthService } from '../../core/services/github-oauth.service';
import { RepoScanApiService } from '../../core/services/repo-scan-api.service';
import {
  GithubStatusResponse, RepoScanStatusResponse,
  RepoScanHistorySummary, SecurityFinding, CWE_INFO
} from '../../core/models/security-scan.model';

@Component({
  selector: 'app-security-scan',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './security-scan.component.html',
  styleUrls: ['./security-scan.component.scss']
})
export class SecurityScanComponent implements OnInit, OnDestroy {

  // GitHub connection state
  githubStatus = signal<GithubStatusResponse | null>(null);
  isConnecting = signal(false);

  // Scan input
  repoUrl = signal('');
  branch = signal('');
  isStarting = signal(false);
  inputError = signal<string | null>(null);

  // Active scan
  currentScan = signal<RepoScanStatusResponse | null>(null);
  activeTab = signal<'progress' | 'report' | 'history'>('progress');

  // History
  history = signal<RepoScanHistorySummary[]>([]);
  showHistory = signal(false);
  isLoadingHistory = signal(false);

  // Report filters
  severityFilter = signal<string>('ALL');
  expandedFinding = signal<number | null>(null);

  private pollInterval: any = null;
  readonly cweInfo = CWE_INFO;

  constructor(
    private githubOAuth: GithubOAuthService,
    private scanApi: RepoScanApiService,
    private route: ActivatedRoute
  ) {}

  ngOnInit() {
    this.loadGithubStatus();

    // Handle OAuth callback redirect
    this.route.queryParams.subscribe(params => {
      if (params['github'] === 'connected') {
        this.loadGithubStatus();
        // Clean the URL param
        window.history.replaceState({}, '', '/security-scan');
      } else if (params['github'] === 'error') {
        this.inputError.set('GitHub connection failed. Please try again.');
        window.history.replaceState({}, '', '/security-scan');
      }
    });
  }

  ngOnDestroy() { this.stopPolling(); }

  // ── GitHub ────────────────────────────────────────────────────────────────

  loadGithubStatus() {
    this.githubOAuth.getStatus().subscribe({
      next: (s) => this.githubStatus.set(s),
      error: () => {}
    });
  }

  connectGithub() {
    this.isConnecting.set(true);
    this.githubOAuth.getAuthorizeUrl().subscribe({
      next: (url) => { window.location.href = url; },
      error: () => this.isConnecting.set(false)
    });
  }

  disconnectGithub() {
    if (!confirm('Disconnect GitHub? Your token will be removed.')) return;
    this.githubOAuth.disconnect().subscribe({
      next: () => this.githubStatus.set({ connected: false, githubUsername: '' }),
      error: () => {}
    });
  }

  // ── Scan ──────────────────────────────────────────────────────────────────

  get isConnected(): boolean {
    return this.githubStatus()?.connected === true;
  }

  get canScan(): boolean {
    return this.isConnected && this.repoUrl().trim().length > 0 && !this.isStarting();
  }

  startScan() {
    if (!this.canScan) return;

    const url = this.repoUrl().trim();
    const githubUrlPattern = /^https:\/\/github\.com\/[\w.-]+\/[\w.-]+\/?$/;
    if (!githubUrlPattern.test(url)) {
      this.inputError.set('Enter a valid GitHub repo URL: https://github.com/owner/repo');
      return;
    }

    this.isStarting.set(true);
    this.inputError.set(null);
    this.currentScan.set(null);

    this.scanApi.startScan({
      repoUrl: url,
      branch: this.branch().trim() || undefined
    }).subscribe({
      next: (scan) => {
        this.currentScan.set(scan);
        this.isStarting.set(false);
        this.activeTab.set('progress');
        this.startPolling(scan.id);
      },
      error: (err: Error) => {
        this.inputError.set(err.message);
        this.isStarting.set(false);
      }
    });
  }

  // ── Polling ───────────────────────────────────────────────────────────────

  private startPolling(id: number) {
    this.stopPolling();
    this.pollInterval = setInterval(() => {
      this.scanApi.getStatus(id).subscribe({
        next: (scan) => {
          this.currentScan.set(scan);
          if (scan.status === 'COMPLETED' || scan.status === 'FAILED') {
            this.stopPolling();
            if (scan.status === 'COMPLETED') this.activeTab.set('report');
          }
        },
        error: () => this.stopPolling()
      });
    }, 3000);
  }

  private stopPolling() {
    if (this.pollInterval) { clearInterval(this.pollInterval); this.pollInterval = null; }
  }

  // ── History ───────────────────────────────────────────────────────────────

  toggleHistory() {
    this.showHistory.update(v => !v);
    if (this.showHistory() && this.history().length === 0) this.loadHistory();
  }

  private loadHistory() {
    this.isLoadingHistory.set(true);
    this.scanApi.getHistory().subscribe({
      next: (h) => { this.history.set(h); this.isLoadingHistory.set(false); },
      error: () => this.isLoadingHistory.set(false)
    });
  }

  loadHistoryItem(id: number) {
    this.scanApi.getStatus(id).subscribe({
      next: (scan) => {
        this.currentScan.set(scan);
        this.showHistory.set(false);
        this.activeTab.set(scan.status === 'COMPLETED' ? 'report' : 'progress');
        if (scan.status === 'PENDING' || scan.status === 'FETCHING' || scan.status === 'SCANNING') {
          this.startPolling(id);
        }
      },
      error: () => {}
    });
  }

  deleteHistoryItem(id: number, e: Event) {
    e.stopPropagation();
    if (!confirm('Delete this scan?')) return;
    this.scanApi.delete(id).subscribe({
      next: () => {
        this.history.update(h => h.filter(i => i.id !== id));
        if (this.currentScan()?.id === id) {
          this.currentScan.set(null);
          this.stopPolling();
        }
      },
      error: () => {}
    });
  }

  resetScan() {
    this.stopPolling();
    this.currentScan.set(null);
    this.repoUrl.set('');
    this.branch.set('');
    this.inputError.set(null);
    this.activeTab.set('progress');
  }

  // ── Report helpers ────────────────────────────────────────────────────────

  get filteredFindings(): SecurityFinding[] {
    const findings = this.currentScan()?.findings ?? [];
    const filter = this.severityFilter();
    if (filter === 'ALL') return findings;
    return findings.filter(f => f.severity === filter);
  }

  get findingsBySeverity(): Record<string, SecurityFinding[]> {
    const findings = this.filteredFindings;
    return {
      CRITICAL: findings.filter(f => f.severity === 'CRITICAL'),
      HIGH:     findings.filter(f => f.severity === 'HIGH'),
      MEDIUM:   findings.filter(f => f.severity === 'MEDIUM'),
      LOW:      findings.filter(f => f.severity === 'LOW'),
    };
  }

  toggleFinding(index: number) {
    this.expandedFinding.update(v => v === index ? null : index);
  }

  getCweLabel(cwe: string): string {
    return this.cweInfo[cwe] ?? cwe;
  }

  getRiskClass(score: number): string {
    if (score >= 7) return 'critical';
    if (score >= 4) return 'high';
    if (score >= 2) return 'medium';
    return 'low';
  }

  getStatusIcon(status: string): string {
    switch (status) {
      case 'COMPLETED':  return '✓';
      case 'FETCHING':   return '⬇';
      case 'SCANNING':   return '◌';
      case 'PENDING':    return '○';
      case 'FAILED':     return '✕';
      default: return '○';
    }
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'PENDING':   return 'Queued';
      case 'FETCHING':  return 'Fetching repository files...';
      case 'SCANNING':  return 'Scanning files for vulnerabilities...';
      case 'COMPLETED': return 'Scan complete';
      case 'FAILED':    return 'Scan failed';
      default: return status;
    }
  }

  get isRunning(): boolean {
    const s = this.currentScan()?.status;
    return s === 'PENDING' || s === 'FETCHING' || s === 'SCANNING';
  }

  getSeverityCount(severity: string): number {
    return this.currentScan()?.findings?.filter(f => f.severity === severity).length ?? 0;
  }
}
