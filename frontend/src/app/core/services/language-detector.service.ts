import { Injectable } from '@angular/core';

/**
 * Client-side language detector for Monaco syntax highlighting.
 * Runs instantly as the user types — no API call needed.
 * This is ONLY for Monaco's display language (colors/highlighting).
 * The actual language detection for review is done by the AI on the backend.
 */
@Injectable({ providedIn: 'root' })
export class LanguageDetectorService {

  /**
   * Returns a Monaco language ID based on code content patterns.
   * Falls back to 'plaintext' if unsure.
   */
  detect(code: string): string {
    if (!code || code.trim().length < 10) return 'plaintext';

    const sample = code.substring(0, 2000); // Only scan first 2000 chars for performance

    // --- Java ---
    if (
      /\b(public|private|protected)\s+(class|interface|enum|record)\s+\w+/.test(sample) ||
      /\b(import\s+java\.|import\s+org\.|import\s+com\.)/.test(sample) ||
      /\b(System\.out\.println|@Override|@SpringBootApplication|@RestController)\b/.test(sample) ||
      /\bvoid\s+main\s*\(\s*String\[\]\s*args\s*\)/.test(sample)
    ) return 'java';

    // --- TypeScript (check before JS because TS is a superset) ---
    if (
      /\b(interface|type|enum)\s+\w+\s*[\{<]/.test(sample) ||
      /:\s*(string|number|boolean|void|any|never|unknown)\b/.test(sample) ||
      /\b(readonly|abstract|implements|declare|namespace)\b/.test(sample) ||
      /\bas\s+\w+/.test(sample)
    ) return 'typescript';

    // --- JavaScript ---
    if (
      /\b(const|let|var)\s+\w+\s*=/.test(sample) ||
      /\b(function|async\s+function|=>\s*\{)/.test(sample) ||
      /\b(require\(|module\.exports|import\s+.*\s+from\s+['"])/.test(sample) ||
      /\b(document\.|window\.|console\.|Promise\.|fetch\()/.test(sample) ||
      /\bexport\s+(default|const|function|class)/.test(sample)
    ) return 'javascript';

    // --- Python ---
    if (
      /^(def |class |import |from |if __name__|@|\s{4})/.test(sample) ||
      /\bdef\s+\w+\s*\(/.test(sample) ||
      /\bprint\s*\(/.test(sample) ||
      /\b(elif|lambda|None|True|False)\b/.test(sample) ||
      /\bself\.\w+/.test(sample) ||
      /"""[\s\S]*?"""/.test(sample) ||
      /#\s*\w+/.test(sample)
    ) return 'python';

    // --- Go ---
    if (
      /\bpackage\s+\w+/.test(sample) ||
      /\bfunc\s+\w+\s*\(/.test(sample) ||
      /\b(fmt\.|goroutine|go\s+func|chan\s+)/.test(sample) ||
      /\bimport\s+\([\s\S]*?\)/.test(sample)
    ) return 'go';

    // --- Rust ---
    if (
      /\bfn\s+\w+\s*\(/.test(sample) ||
      /\b(let\s+mut|impl\s+\w+|pub\s+fn|use\s+std::)/.test(sample) ||
      /\b(println!|vec!|Option<|Result<|unwrap\(\))/.test(sample)
    ) return 'rust';

    // --- C# ---
    if (
      /\busing\s+System/.test(sample) ||
      /\b(namespace|Console\.Write|\.cs$)/.test(sample) ||
      /\b(public|private)\s+(static\s+)?(void|int|string|bool)\s+\w+/.test(sample)
    ) return 'csharp';

    // --- C++ ---
    if (
      /#include\s*</.test(sample) ||
      /\bstd::(cout|cin|vector|string|endl)\b/.test(sample) ||
      /\b(int\s+main\s*\(\s*(void|int\s+argc)?)/.test(sample)
    ) return 'cpp';

    // --- PHP ---
    if (
      /<\?php/.test(sample) ||
      /\$\w+\s*=/.test(sample) ||
      /\b(echo|isset|array\(|\$_GET|\$_POST)\b/.test(sample)
    ) return 'php';

    // --- Ruby ---
    if (
      /\bdef\s+\w+[\s\n]/.test(sample) && /\bend\b/.test(sample) ||
      /\b(puts|attr_accessor|require\s+'|gem\s+')/.test(sample)
    ) return 'ruby';

    // --- Kotlin ---
    if (
      /\bfun\s+\w+\s*\(/.test(sample) ||
      /\b(val|var)\s+\w+\s*:\s*\w+/.test(sample) ||
      /\b(data class|object\s+\w+|companion object)\b/.test(sample)
    ) return 'kotlin';

    // --- Swift ---
    if (
      /\bvar\s+\w+\s*:\s*\w+/.test(sample) ||
      /\b(func|guard|let|var)\s+\w+/.test(sample) && /\bimport\s+(Foundation|UIKit|SwiftUI)\b/.test(sample)
    ) return 'swift';

    // --- SQL ---
    if (
      /\b(SELECT|INSERT|UPDATE|DELETE|CREATE|ALTER|DROP)\b/i.test(sample) &&
      /\b(FROM|WHERE|TABLE|INTO|VALUES)\b/i.test(sample)
    ) return 'sql';

    // --- Shell / Bash ---
    if (
      /^(#!\/bin\/(bash|sh)|#!\/usr\/bin\/env\s+(bash|sh))/.test(sample) ||
      /\b(echo\s+"|grep\s+|awk\s+|sed\s+|chmod\s+|export\s+\w+=)/.test(sample)
    ) return 'shell';

    // --- HTML ---
    if (/<(!DOCTYPE|html|head|body|div|span|script)\b/i.test(sample)) return 'html';

    // --- CSS ---
    if (/[\w\-\.#*]+\s*\{[\s\S]*?\}/.test(sample) && /:\s*[\w\-#%px]+;/.test(sample)) return 'css';

    // --- JSON ---
    if (/^\s*[\{\[]/.test(sample.trim())) {
      try { JSON.parse(code); return 'json'; } catch {}
    }

    // --- YAML ---
    if (/^[\w\-]+:\s*.+/m.test(sample) && !/[;{}()]/.test(sample)) return 'yaml';

    return 'plaintext';
  }

  /**
   * Maps file extension to Monaco language ID.
   * Used when a file is uploaded.
   */
  fromExtension(filename: string): string {
    const ext = filename.split('.').pop()?.toLowerCase() ?? '';
    const map: Record<string, string> = {
      java: 'java',
      js: 'javascript', mjs: 'javascript', cjs: 'javascript',
      ts: 'typescript', tsx: 'typescript',
      py: 'python', pyw: 'python',
      go: 'go',
      rs: 'rust',
      cs: 'csharp',
      cpp: 'cpp', cc: 'cpp', cxx: 'cpp', c: 'cpp',
      php: 'php',
      rb: 'ruby',
      kt: 'kotlin', kts: 'kotlin',
      swift: 'swift',
      sql: 'sql',
      sh: 'shell', bash: 'shell',
      html: 'html', htm: 'html',
      css: 'css', scss: 'css', less: 'css',
      json: 'json',
      yaml: 'yaml', yml: 'yaml',
      xml: 'xml',
      md: 'markdown',
    };
    return map[ext] ?? 'plaintext';
  }
}
