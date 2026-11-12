import { Component, inject } from '@angular/core';
import { UiService } from '../../services/ui.service';

@Component({
  selector: 'app-preferences',
  standalone: true,
  template: `
    <div class="preferences">
      <button type="button" class="ops-button-secondary theme-toggle" (click)="ui.toggleTheme()"
        [attr.aria-label]="ui.theme() === 'light' ? ui.text('Koyu temaya geç', 'Switch to dark theme') : ui.text('Açık temaya geç', 'Switch to light theme')"
        [title]="ui.text('Temayı değiştir', 'Change theme')">
        <span aria-hidden="true">{{ ui.theme() === 'light' ? '◐' : '◑' }}</span>
      </button>
      <label class="language-picker">
        <span class="sr-only">{{ ui.text('Arayüz dili', 'Interface language') }}</span>
        <select class="ops-input" [value]="ui.language()" (change)="changeLanguage($event)">
          <option value="tr">Türkçe</option>
          <option value="en">English</option>
        </select>
      </label>
    </div>
  `,
  styles: [`
    .preferences { display:flex; align-items:center; gap:.5rem; }
    .theme-toggle { min-width:2.75rem; font-size:1.4rem; padding:.25rem .6rem; }
    .language-picker select { min-height:2.75rem; padding:.4rem 1.6rem .4rem .6rem; font-size:.8rem; }
  `]
})
export class PreferencesComponent {
  readonly ui = inject(UiService);
  changeLanguage(event: Event): void {
    this.ui.setLanguage((event.target as HTMLSelectElement).value === 'en' ? 'en' : 'tr');
  }
}
