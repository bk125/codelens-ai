export interface RepoScanRequest {
  repoUrl: string;
  branch?: string;
}

export interface SecurityFinding {
  file: string;
  type: string;
  severity: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';
  line: number;
  cwe: string;
  description: string;
  exploitScenario: string;
  fix: string;
  fixedCodeSnippet: string;
}

export interface RepoScanStatusResponse {
  id: number;
  repoUrl: string;
  repoName: string;
  branch: string;
  status: 'PENDING' | 'FETCHING' | 'SCANNING' | 'COMPLETED' | 'FAILED';
  totalFiles: number;
  processedFiles: number;
  progressPercent: number;
  currentFile: string;
  overallRiskScore: number;
  criticalCount: number;
  highCount: number;
  mediumCount: number;
  lowCount: number;
  summary: string;
  findings: SecurityFinding[];
  errorMessage: string;
  reportReady: boolean;
  createdAt: string;
  completedAt: string;
}

export interface RepoScanHistorySummary {
  id: number;
  repoUrl: string;
  repoName: string;
  status: string;
  totalFiles: number;
  overallRiskScore: number;
  criticalCount: number;
  highCount: number;
  createdAt: string;
  completedAt: string;
}

export interface GithubStatusResponse {
  connected: boolean;
  githubUsername: string;
}

export const CWE_INFO: Record<string, string> = {
  'CWE-89':  'SQL Injection',
  'CWE-78':  'OS Command Injection',
  'CWE-798': 'Hardcoded Credentials',
  'CWE-502': 'Insecure Deserialization',
  'CWE-287': 'Improper Authentication',
  'CWE-284': 'Improper Access Control',
  'CWE-79':  'Cross-Site Scripting (XSS)',
  'CWE-601': 'Open Redirect',
  'CWE-22':  'Path Traversal',
  'CWE-918': 'SSRF',
  'CWE-611': 'XXE',
  'CWE-732': 'Security Misconfiguration',
  'CWE-200': 'Sensitive Data Exposure',
  'CWE-327': 'Weak Cryptography',
  'CWE-330': 'Insecure Randomness',
};
