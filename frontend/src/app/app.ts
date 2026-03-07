import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AiChatComponent } from './components/ai-chat/ai-chat.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, AiChatComponent],
  template: `
    <router-outlet />
    <app-ai-chat></app-ai-chat>
  `,
  styles: [],
})
export class App { }
