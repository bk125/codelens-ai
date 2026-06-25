export interface MigrationStatusResponse {
  id: number;
  projectName: string;
  sourceLanguage: string;
  targetLanguage: string;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  totalFiles: number;
  processedFiles: number;
  progressPercent: number;
  errorMessage?: string;
  migrationPlan?: string;
  fileSummaryJson?: string;
  downloadReady: boolean;
  createdAt: string;
  completedAt?: string;
}

export interface MigrationHistorySummary {
  id: number;
  projectName: string;
  sourceLanguage: string;
  targetLanguage: string;
  status: string;
  totalFiles: number;
  processedFiles: number;
  createdAt: string;
  completedAt?: string;
}

export interface FileSummaryEntry {
  original: string;
  converted: string;
  status: 'migrated' | 'copied';
}

export interface TargetLang {
  value: string;
  label: string;
  icon: string;
}

export const MIGRATION_TARGET_LANGUAGES: TargetLang[] = [
  { value: 'Python',     label: 'Python',     icon: '🐍' },
  { value: 'Java',       label: 'Java',       icon: '☕' },
  { value: 'JavaScript', label: 'JavaScript', icon: '⚡' },
  { value: 'TypeScript', label: 'TypeScript', icon: '🔷' },
  { value: 'Go',         label: 'Go',         icon: '🔵' },
  { value: 'Rust',       label: 'Rust',       icon: '🦀' },
  { value: 'C#',         label: 'C#',         icon: '💜' },
  { value: 'C++',        label: 'C++',        icon: '⚙️' },
  { value: 'Kotlin',     label: 'Kotlin',     icon: '🟣' },
  { value: 'Swift',      label: 'Swift',      icon: '🍎' },
  { value: 'PHP',        label: 'PHP',        icon: '🐘' },
  { value: 'Ruby',       label: 'Ruby',       icon: '💎' },
];
