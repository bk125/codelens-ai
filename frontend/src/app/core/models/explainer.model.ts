export interface ExplainRequest {
  code: string;
}

export interface FunctionBreakdown {
  name: string;
  purpose: string;
  parameters: string;
  returns: string;
}

export interface ExplainResponse {
  id: number;
  language: string;
  originalCode: string;
  overview: string;
  howItWorks: string;
  functions: FunctionBreakdown[];
  complexity: string;
  useCases: string[];
  keyConcepts: string[];
  createdAt: string;
}

export interface ExplainHistorySummary {
  id: number;
  language: string;
  overview: string;
  createdAt: string;
}
