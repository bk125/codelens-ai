export interface CodeIssue {
  line: number;
  severity: 'critical' | 'warning' | 'info';
  problem: string;
}

export interface CodeReviewResponse {
  id: number;
  sessionId: string;
  language: string;
  title: string;
  originalCode: string;
  score: number;
  summary: string;
  issues: CodeIssue[];
  improvements: string[];
  best_practices: string[];
  optimized_code: string;
  createdAt: string;
}

export interface ReviewHistorySummary {
  id: number;
  language: string;
  title: string;
  score: number;
  summary: string;
  createdAt: string;
}

export interface CodeReviewRequest {
  code: string;
  language?: string; // optional hint only — AI will auto-detect
  title?: string;
}

export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  error?: string;
  timestamp: string;
}

export interface SessionStats {
  sessionId: string;
  totalReviews: number;
  averageScore: number;
}

// LanguageOption kept only for file extension mapping
export interface LanguageOption {
  value: string;
  monacoId: string;
}
