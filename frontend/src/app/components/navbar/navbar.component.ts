import { Component, inject, output } from '@angular/core';
import { UiService } from '../../services/ui.service';
import { PreferencesComponent } from '../preferences/preferences.component';

@Component({
  selector: 'app-navbar',
  imports: [PreferencesComponent],
  template: `
    <nav class="navigation" [attr.aria-label]="ui.text('Ana menü', 'Main navigation')">
      <div class="nav-inner">
        <button type="button" class="brand" (click)="navClick.emit('servers')" aria-label="DevOps Dashboard">
          <span class="brand-mark" aria-hidden="true">K.</span>
          <span>DevOps <strong>Dashboard</strong><small>{{ ui.text('KOBALT NÖBET', 'COBALT WATCH') }}</small></span>
        </button>
        <app-preferences />
        <div class="nav-actions">
          <button type="button" class="ops-button-secondary" (click)="navClick.emit('setup')"><i class="fa-solid fa-terminal" aria-hidden="true"></i>{{ ui.text('Ajan kurulumu', 'Agent setup') }}</button>
          <a href="/actuator/prometheus" target="_blank" rel="noopener" class="ops-button-secondary">{{ ui.text('Metrikler', 'Metrics') }}<span aria-hidden="true">↗</span></a>
          <button type="button" class="ops-button-secondary" (click)="navClick.emit('settings')"><i class="fa-solid fa-gear" aria-hidden="true"></i>{{ ui.text('Ayarlar', 'Settings') }}</button>
          <button type="button" class="ops-button" (click)="navClick.emit('new-server')"><span aria-hidden="true">+</span>{{ ui.text('Sunucu ekle', 'Add server') }}</button>
          <button type="button" class="ops-button-secondary" (click)="logout.emit()" [attr.aria-label]="ui.text('Çıkış yap', 'Sign out')" [title]="ui.text('Çıkış yap', 'Sign out')"><i class="fa-solid fa-right-from-bracket" aria-hidden="true"></i></button>
        </div>
      </div>
    </nav>
  `,
  styles: [`
    .navigation { background:var(--surface); border-bottom:1px solid var(--border); }
    .nav-inner { max-width:1440px; margin:auto; padding:1rem clamp(1rem,3vw,3rem); display:flex; flex-wrap:wrap; align-items:center; gap:1rem; }
    .brand { display:flex; align-items:center; gap:.7rem; text-align:left; margin-right:auto; color:var(--ink); background:none; border:0; }
    .brand-mark { width:2.7rem; height:2.7rem; display:grid; place-items:center; background:var(--accent); color:var(--accent-contrast); border-radius:.65rem; font-family:'Archivo Black',sans-serif; font-size:1.4rem; }
    .brand strong { font-weight:700; }
    .brand small { display:block; margin-top:.2rem; color:var(--muted); font-size:.55rem; letter-spacing:.18em; font-family:'IBM Plex Mono',monospace; }
    .nav-actions { display:flex; flex-wrap:wrap; align-items:center; gap:.5rem; }
    @media(max-width:1150px) { .nav-actions { width:100%; } }
    @media(max-width:480px) {
      .nav-inner { gap:.75rem; } .brand { font-size:.8rem; } .brand-mark { display:none; }
      .nav-actions { display:grid; grid-template-columns:1fr 1fr; }
      .nav-actions > :last-child { grid-column:2; grid-row:2; justify-self:end; }
      .nav-actions > :nth-child(3) { grid-column:1; grid-row:2; }
      .nav-actions > :nth-child(4) { grid-column:1 / -1; }
    }
  `]
})
export class NavbarComponent {
  readonly ui = inject(UiService);
  navClick = output<string>();
  logout = output<void>();
}
