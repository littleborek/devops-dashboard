import { Component, inject } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { filter, map } from 'rxjs';
import { UiService } from './services/ui.service';
import { AiChatComponent } from './components/ai-chat/ai-chat.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, AiChatComponent],
  template: `
    <router-outlet />
    @if (!isLogin()) { <app-ai-chat></app-ai-chat> }
  `,
  styles: [],
})
export class App {
  private readonly ui = inject(UiService);
  private readonly router = inject(Router);
  readonly isLogin = toSignal(this.router.events.pipe(
    filter(event => event instanceof NavigationEnd),
    map(event => (event as NavigationEnd).urlAfterRedirects.split(/[?#]/)[0] === '/login')
  ), { initialValue: true });
}
