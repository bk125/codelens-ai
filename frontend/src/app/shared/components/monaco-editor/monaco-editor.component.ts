import {
  Component, ElementRef, EventEmitter, Input,
  OnChanges, OnDestroy, OnInit, Output, SimpleChanges, ViewChild
} from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-monaco-editor',
  standalone: true,
  imports: [CommonModule],
  template: `<div #editorContainer class="monaco-container"></div>`,
  styles: [`
    .monaco-container {
      width: 100%;
      height: 100%;
    }
  `]
})
export class MonacoEditorComponent implements OnInit, OnChanges, OnDestroy {
  @ViewChild('editorContainer', { static: true }) editorContainer!: ElementRef;

  @Input() value = '';
  @Input() language = 'plaintext';
  @Input() readOnly = false;
  @Input() height = '420px';
  @Output() valueChange = new EventEmitter<string>();

  private editor: any = null;
  private isUpdatingValue = false;

  ngOnInit() {
    this.editorContainer.nativeElement.style.height = this.height;
    this.loadMonaco();
  }

  ngOnChanges(changes: SimpleChanges) {
    if (!this.editor) return;

    // Update language (for syntax highlighting changes as user types)
    if (changes['language'] && !changes['language'].firstChange) {
      const monaco = (window as any).monaco;
      if (monaco) {
        const model = this.editor.getModel();
        if (model) {
          monaco.editor.setModelLanguage(model, this.language || 'plaintext');
        }
      }
    }

    // Update value externally (e.g. file upload, re-review)
    if (changes['value'] && !changes['value'].firstChange) {
      const current = this.editor.getValue();
      if (current !== this.value && !this.isUpdatingValue) {
        this.isUpdatingValue = true;
        this.editor.setValue(this.value || '');
        this.isUpdatingValue = false;
      }
    }

    // Update height
    if (changes['height'] && !changes['height'].firstChange) {
      this.editorContainer.nativeElement.style.height = this.height;
      this.editor.layout();
    }
  }

  ngOnDestroy() {
    if (this.editor) {
      this.editor.dispose();
      this.editor = null;
    }
  }

  private loadMonaco() {
    if ((window as any).monaco) {
      this.initEditor();
      return;
    }

    const win = window as any;
    win.require = { paths: { vs: 'assets/monaco/vs' } };

    // Avoid duplicate script tags
    if (document.querySelector('script[src="assets/monaco/vs/loader.js"]')) {
      const interval = setInterval(() => {
        if ((win).monaco) { clearInterval(interval); this.initEditor(); }
      }, 100);
      return;
    }

    const script = document.createElement('script');
    script.src = 'assets/monaco/vs/loader.js';
    script.onload = () => {
      win.require(['vs/editor/editor.main'], () => this.initEditor());
    };
    document.head.appendChild(script);
  }

  private initEditor() {
    const monaco = (window as any).monaco;

    // Define theme once globally
    if (!(window as any).__codeLensThemeDefined) {
      monaco.editor.defineTheme('codeLensDark', {
        base: 'vs-dark',
        inherit: true,
        rules: [
          { token: 'comment',    foreground: '6b7190', fontStyle: 'italic' },
          { token: 'keyword',    foreground: '00e5cc' },
          { token: 'string',     foreground: '4ade80' },
          { token: 'number',     foreground: 'fbbf24' },
          { token: 'type',       foreground: '60a5fa' },
          { token: 'class',      foreground: '60a5fa' },
          { token: 'function',   foreground: 'a78bfa' },
          { token: 'variable',   foreground: 'e8eaf0' },
          { token: 'operator',   foreground: 'f87171' },
          { token: 'annotation', foreground: 'fb923c' },
        ],
        colors: {
          'editor.background':                '#13161d',
          'editor.foreground':                '#e8eaf0',
          'editorLineNumber.foreground':      '#3d4160',
          'editorLineNumber.activeForeground':'#00e5cc',
          'editor.lineHighlightBackground':   '#1a1e2888',
          'editor.selectionBackground':       '#00e5cc33',
          'editor.inactiveSelectionBackground':'#00e5cc1a',
          'editorCursor.foreground':          '#00e5cc',
          'editorWidget.background':          '#1a1e28',
          'editorSuggestWidget.background':   '#1a1e28',
          'editorSuggestWidget.border':       '#252935',
          'editorSuggestWidget.selectedBackground': '#252935',
          'scrollbarSlider.background':       '#252935',
          'scrollbarSlider.hoverBackground':  '#3d4160',
          'editorIndentGuide.background':     '#252935',
          'editorBracketMatch.background':    '#00e5cc22',
          'editorBracketMatch.border':        '#00e5cc',
        }
      });

      monaco.editor.defineTheme('codeLensLight', {
        base: 'vs',
        inherit: true,
        rules: [
          { token: 'comment',    foreground: '6b7280', fontStyle: 'italic' },
          { token: 'keyword',    foreground: '0891b2' },
          { token: 'string',     foreground: '16a34a' },
          { token: 'number',     foreground: 'd97706' },
          { token: 'type',       foreground: '2563eb' },
          { token: 'function',   foreground: '7c3aed' },
          { token: 'annotation', foreground: 'ea580c' },
        ],
        colors: {
          'editor.background':                '#ffffff',
          'editor.foreground':                '#111827',
          'editorLineNumber.foreground':      '#d1d5db',
          'editorLineNumber.activeForeground':'#0891b2',
          'editor.lineHighlightBackground':   '#f8f9fb',
          'editor.selectionBackground':       '#0891b233',
          'editorCursor.foreground':          '#0891b2',
        }
      });

      (window as any).__codeLensThemeDefined = true;
    }

    const isDark = document.documentElement.getAttribute('data-theme') !== 'light';
    const theme = isDark ? 'codeLensDark' : 'codeLensLight';

    this.editor = monaco.editor.create(this.editorContainer.nativeElement, {
      value: this.value || '',
      language: this.language || 'plaintext',
      theme,
      readOnly: this.readOnly,
      automaticLayout: true,
      minimap: { enabled: false },
      fontSize: 13,
      fontFamily: "'Space Mono', 'Cascadia Code', 'Fira Code', 'Consolas', monospace",
      fontLigatures: true,
      lineHeight: 22,
      padding: { top: 16, bottom: 16 },
      scrollBeyondLastLine: false,
      wordWrap: 'on',
      tabSize: 2,
      renderWhitespace: 'selection',
      smoothScrolling: true,
      cursorBlinking: 'smooth',
      cursorSmoothCaretAnimation: 'on',
      renderLineHighlight: 'line',
      bracketPairColorization: { enabled: true },
      suggest: { showMethods: true, showFunctions: true, showKeywords: true },
      scrollbar: {
        verticalScrollbarSize: 6,
        horizontalScrollbarSize: 6,
      }
    });

    if (!this.readOnly) {
      this.editor.onDidChangeModelContent(() => {
        if (!this.isUpdatingValue) {
          this.valueChange.emit(this.editor.getValue());
        }
      });
    }

    // Watch for theme changes on the html element
    const observer = new MutationObserver(() => {
      const dark = document.documentElement.getAttribute('data-theme') !== 'light';
      monaco.editor.setTheme(dark ? 'codeLensDark' : 'codeLensLight');
    });
    observer.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] });
  }
}
