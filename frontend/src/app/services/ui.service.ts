import { DOCUMENT } from '@angular/common';
import { Injectable, inject, signal } from '@angular/core';

export type Language = 'tr' | 'en';
export type Theme = 'light' | 'dark';

@Injectable({ providedIn: 'root' })
export class UiService {
  private readonly document = inject(DOCUMENT);
  readonly language = signal<Language>(this.read('ops-language') === 'en' ? 'en' : 'tr');
  readonly theme = signal<Theme>(this.read('ops-theme') === 'dark' ? 'dark' : 'light');

  constructor() { this.apply(); }
  text(tr: string, en: string): string { return this.language() === 'tr' ? tr : en; }
  setLanguage(language: Language): void {
    this.language.set(language);
    this.persist('ops-language', language);
    this.apply();
  }
  toggleTheme(): void {
    const theme = this.theme() === 'light' ? 'dark' : 'light';
    this.theme.set(theme);
    this.persist('ops-theme', theme);
    this.apply();
  }
  private apply(): void {
    this.document.documentElement.lang = this.language();
    this.document.documentElement.dataset['theme'] = this.theme();
    this.document.documentElement.style.colorScheme = this.theme();
  }
  private read(key: string): string | null {
    try { return this.document.defaultView?.localStorage.getItem(key) ?? null; }
    catch { return null; }
  }
  private persist(key: string, value: string): void {
    try { this.document.defaultView?.localStorage.setItem(key, value); }
    catch { /* Preferences still apply when browser storage is unavailable. */ }
  }
}
