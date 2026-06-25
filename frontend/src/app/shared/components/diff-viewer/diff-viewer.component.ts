import {
  Component, ElementRef, Input, OnChanges,
  OnDestroy, OnInit, SimpleChanges, ViewChild
} from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-diff-viewer',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="diff-wrapper">
      <div class="diff-header">
        <div class="diff-col-label original-label">
          <span class="dot red"></span> Original
        </div>
        <div class="diff-col-label optimized-label">
          <span class="dot green"></span> Optimized
        </div>
      </div>
      <div #diffContainer class="diff-container"></div>
    </div>
  `,
  styles: [`
    .diff-wrapper {
      width: 100%;
      border-radius: 8px;
      overflow: hidden;
      border: 1px solid var(--border, #252935);
    }
    .diff-header {
      display: flex;
      background: #1a1e28;
      border-bottom: 1px solid #252935;
    }
    .diff-col-label {
      flex: 1;
      display: flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.5rem 1rem;
      font-family: 'Space Mono', monospace;
      font-size: 0.75rem;
      color: #6b7190;
      &:first-child { border-right: 1px solid #252935; }
    }
    .dot {
      width: 8px; height: 8px; border-radius: 50%;
      &.red   { background: #f87171; }
      &.green { background: #4ade80; }
    }
    .diff-container { width: 100%; height: 500px; }
  `]
})
export class DiffViewerComponent implements OnInit, OnChanges, OnDestroy {
  @ViewChild('diffContainer', { static: true }) diffContainer!: ElementRef;

  @Input() original = '';
  @Input() modified = '';
  @Input() language = 'java';

  private diffEditor: any = null;

  ngOnInit() { this.loadMonacoAndInit(); }

  ngOnChanges(changes: SimpleChanges) {
    if ((changes['original'] || changes['modified']) && this.diffEditor) {
      this.updateModels();
    }
    if (changes['language'] && !changes['language'].firstChange && this.diffEditor) {
      this.updateModels();
    }
  }

  ngOnDestroy() {
    if (this.diffEditor) { this.diffEditor.dispose(); this.diffEditor = null; }
  }

  private loadMonacoAndInit() {
    if ((window as any).monaco) { this.initDiff(); return; }

    const win = window as any;
    if (!win.require) win.require = { paths: { vs: 'assets/monaco/vs' } };

    const existing = document.querySelector('script[src="assets/monaco/vs/loader.js"]');
    if (existing) {
      // Wait for monaco to be available
      const interval = setInterval(() => {
        if ((window as any).monaco) { clearInterval(interval); this.initDiff(); }
      }, 100);
      return;
    }

    const script = document.createElement('script');
    script.src = 'assets/monaco/vs/loader.js';
    script.onload = () => {
      win.require(['vs/editor/editor.main'], () => { this.initDiff(); });
    };
    document.head.appendChild(script);
  }

  private initDiff() {
    const monaco = (window as any).monaco;

    if (!monaco.editor.getEditorTheme || monaco.editor.getEditorTheme() !== 'codeLensDark') {
      try {
        monaco.editor.defineTheme('codeLensDark', {
          base: 'vs-dark', inherit: true, rules: [
            { token: 'comment', foreground: '6b7190', fontStyle: 'italic' },
            { token: 'keyword', foreground: '00e5cc' },
            { token: 'string', foreground: '4ade80' },
            { token: 'number', foreground: 'fbbf24' },
          ],
          colors: {
            'editor.background': '#13161d',
            'editor.foreground': '#e8eaf0',
            'editorLineNumber.foreground': '#3d4160',
            'diffEditor.insertedTextBackground': '#4ade8022',
            'diffEditor.removedTextBackground': '#f8717122',
            'diffEditor.insertedLineBackground': '#4ade8011',
            'diffEditor.removedLineBackground': '#f8717111',
            'diffEditor.diagonalFill': '#252935',
          }
        });
      } catch (_) {}
    }

    this.diffEditor = monaco.editor.createDiffEditor(this.diffContainer.nativeElement, {
      theme: 'codeLensDark',
      automaticLayout: true,
      readOnly: true,
      renderSideBySide: true,
      minimap: { enabled: false },
      fontSize: 12,
      fontFamily: "'Space Mono', monospace",
      lineHeight: 20,
      padding: { top: 12, bottom: 12 },
      scrollBeyondLastLine: false,
      wordWrap: 'on',
      renderWhitespace: 'none',
      ignoreTrimWhitespace: false,
      diffWordWrap: 'on',
    });

    this.updateModels();
  }

  private updateModels() {
    const monaco = (window as any).monaco;
    if (!monaco || !this.diffEditor) return;

    const originalModel = monaco.editor.createModel(this.original || '', this.language);
    const modifiedModel = monaco.editor.createModel(this.modified || '', this.language);

    const old = this.diffEditor.getModel();
    this.diffEditor.setModel({ original: originalModel, modified: modifiedModel });
    if (old?.original) old.original.dispose();
    if (old?.modified) old.modified.dispose();
  }
}
