import { Component, EventEmitter, Output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface FileReadResult {
  content: string;
  language: string; // soft hint from extension
  fileName: string;
}

// Extension → language hint (sent to backend as soft hint only)
const EXT_TO_LANG: Record<string, string> = {
  java: 'java',
  js: 'javascript', mjs: 'javascript', cjs: 'javascript',
  ts: 'typescript',
  py: 'python', pyw: 'python',
  go: 'go',
  rb: 'ruby',
  cs: 'csharp',
  cpp: 'cpp', cc: 'cpp', cxx: 'cpp',
  c: 'c',
  php: 'php',
  rs: 'rust',
  kt: 'kotlin',
  swift: 'swift',
  sql: 'sql',
  sh: 'shell', bash: 'shell',
};

const ALLOWED_EXTENSIONS = Object.keys(EXT_TO_LANG);
const MAX_SIZE_BYTES = 500 * 1024; // 500 KB

@Component({
  selector: 'app-file-upload',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './file-upload.component.html',
  styleUrls: ['./file-upload.component.scss']
})
export class FileUploadComponent {
  @Output() fileLoaded = new EventEmitter<FileReadResult>();
  @Output() uploadError = new EventEmitter<string>();

  isDragging = signal(false);
  isReading  = signal(false);
  loadedFile = signal<string | null>(null);
  error      = signal<string | null>(null);

  onDragOver(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging.set(true);
  }

  onDragLeave(event: DragEvent) {
    event.preventDefault();
    this.isDragging.set(false);
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging.set(false);
    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      this.processFile(files[0]); // single file only
    }
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.processFile(input.files[0]);
      input.value = '';
    }
  }

  private processFile(file: File) {
    this.error.set(null);
    this.loadedFile.set(null);

    // Reject folder drops
    if (file.size === 0 && !file.type) {
      this.setError('Folder upload is not allowed. Please select a single file.');
      return;
    }

    const ext = file.name.split('.').pop()?.toLowerCase() ?? '';

    if (!ALLOWED_EXTENSIONS.includes(ext)) {
      this.setError(
        `".${ext}" is not supported. Supported: ${ALLOWED_EXTENSIONS.map(e => '.' + e).join(', ')}`
      );
      return;
    }

    if (file.size > MAX_SIZE_BYTES) {
      this.setError(`File too large (${(file.size / 1024).toFixed(0)} KB). Max allowed: 500 KB.`);
      return;
    }

    this.isReading.set(true);
    const reader = new FileReader();

    reader.onload = (e) => {
      const content = e.target?.result as string;
      this.loadedFile.set(file.name);
      this.isReading.set(false);
      this.fileLoaded.emit({
        content,
        language: EXT_TO_LANG[ext] ?? ext,
        fileName: file.name
      });
    };

    reader.onerror = () => {
      this.isReading.set(false);
      this.setError('Failed to read file. Please try again.');
    };

    reader.readAsText(file);
  }

  private setError(msg: string) {
    this.error.set(msg);
    this.uploadError.emit(msg);
  }

  clear() {
    this.loadedFile.set(null);
    this.error.set(null);
  }

  get supportedExtensions(): string {
    return ALLOWED_EXTENSIONS.slice(0, 8).map(e => '.' + e).join(', ') + ' and more';
  }
}
