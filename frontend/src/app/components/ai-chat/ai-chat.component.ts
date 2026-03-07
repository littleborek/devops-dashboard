import { Component, inject, signal, ElementRef, ViewChild, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AiService } from '../../services/ai.service';

@Component({
  selector: 'app-ai-chat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <!-- Launch Button -->
    <button *ngIf="!isOpen()" (click)="toggleChat()" 
            class="fixed bottom-8 right-8 bg-emerald-600 p-4 rounded-lg shadow-2xl border border-emerald-400 hover:bg-emerald-500 transition-all text-black font-bold flex items-center gap-2"
            style="z-index: 999;">
      <i class="fa-solid fa-brain text-xl"></i> <span class="tracking-wider">AI ASSISTANT</span>
    </button>

    <!-- Backdrop -->
    <div *ngIf="isOpen()" 
         (click)="closeChat()" 
         class="fixed inset-0 bg-black/80 backdrop-blur-sm transition-opacity"
         style="z-index: 1000;">
    </div>

    <!-- Side Drawer Panel -->
    <div *ngIf="isOpen()" 
         class="fixed inset-y-0 right-0 flex flex-col animate-drawer-in"
         style="z-index: 1001; width: 100%; max-width: 450px; background-color: #0b0e14; border-left: 1px solid rgba(16, 185, 129, 0.3); box-shadow: -10px 0 30px rgba(0,0,0,0.8);">
      
      <!-- Drawer Header -->
      <div class="p-6 border-b border-gray-800 flex justify-between items-center shrink-0" style="background-color: #0b0e14;">
        <div>
          <h2 class="text-emerald-500 font-mono font-bold tracking-tighter text-xl flex items-center gap-2">
            <i class="fa-solid fa-terminal text-base"></i> DEV_OPS_TERMINAL
          </h2>
          <p class="text-[10px] text-gray-500 font-mono mt-1 w-full flex items-center gap-2">
            <span class="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span> STATUS: ENCRYPTED_LINK_ESTABLISHED
          </p>
        </div>
        <button (click)="closeChat()" class="text-gray-500 hover:text-emerald-500 transition-colors p-2" title="Close">
          <i class="fa-solid fa-chevron-right text-xl"></i>
        </button>
      </div>

      <!-- Messages Area -->
      <div class="flex-1 min-h-0 overflow-y-auto p-6 space-y-6 scrollbar-thin" style="background-color: #0b0e14;" #scrollContainer>
        <div *ngFor="let msg of messages()" [ngClass]="{'text-right': msg.role === 'user'}">
          <div [ngClass]="msg.role === 'user' ? 'bg-emerald-900/20 border-emerald-500/30 ml-auto' : 'border-gray-800'"
               class="inline-block max-w-[95%] p-4 border rounded-sm text-sm font-mono text-gray-300 text-left shadow-sm"
               [style.background-color]="msg.role === 'ai' ? '#11151c' : ''">
            <span class="block text-[10px] mb-2 opacity-50 uppercase tracking-widest flex items-center gap-2 font-bold"
                  [ngClass]="msg.role === 'user' ? 'text-emerald-400 justify-end' : 'text-purple-400'">
               <i class="fa-solid fa-user" *ngIf="msg.role === 'user'"></i>
               <i class="fa-solid fa-robot" *ngIf="msg.role === 'ai'"></i>
              {{ msg.role === 'user' ? 'sysadmin' : 'ai-core' }}
            </span>
            <div class="chat-html break-words" [innerHTML]="formatMessage(msg.content)"></div>
          </div>
        </div>
        
        <div *ngIf="isLoading()" class="text-emerald-500 font-mono text-xs animate-pulse p-2">
          > EXECUTING NEURAL_QUERY...<span class="w-1.5 h-3 bg-emerald-500 inline-block align-middle ml-1 animate-ping"></span>
        </div>
      </div>

      <!-- Input Area -->
      <div class="p-5 border-t border-gray-800 shrink-0" style="background-color: #0b0e14;">
        <form (ngSubmit)="sendMessage()" class="relative flex items-center">
          <span class="absolute left-4 font-mono text-emerald-600 font-bold">></span>
          <input type="text" [(ngModel)]="currentInput" name="input" autocomplete="off"
                 placeholder="Enter query..."
                 class="w-full border border-gray-700 rounded-sm p-4 pl-8 pr-12 text-emerald-400 font-mono text-sm focus:border-emerald-500 outline-none transition-all shadow-inner"
                 style="background-color: #11151c;"
                 [disabled]="isLoading()" #chatInput>
          <button type="submit" [disabled]="!currentInput.trim() || isLoading()" 
                  class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-emerald-500 disabled:opacity-50 transition-colors p-2">
            <i class="fa-solid fa-share"></i>
          </button>
        </form>
      </div>
    </div>
  `,
  styles: [`
    .animate-drawer-in { animation: slideIn 0.3s cubic-bezier(0.2, 0.8, 0.2, 1); }
    @keyframes slideIn { from { transform: translateX(100%); } to { transform: translateX(0); } }

    .scrollbar-thin::-webkit-scrollbar { width: 6px; }
    .scrollbar-thin::-webkit-scrollbar-track { background: rgba(0, 0, 0, 0.2); }
    .scrollbar-thin::-webkit-scrollbar-thumb { background: #059669; border-radius: 2px; }
    .scrollbar-thin::-webkit-scrollbar-thumb:hover { background: #10b981; }

    ::ng-deep .chat-html p { margin-bottom: 0.8rem; line-height: 1.5; }
    ::ng-deep .chat-html p:last-child { margin-bottom: 0; }
    ::ng-deep .chat-html code { color: #34d399; background: rgba(0,0,0,0.5); padding: 2px 6px; border-radius: 3px; border: 1px solid rgba(16,185,129,0.2); }
    ::ng-deep .chat-html strong { color: #10b981; text-shadow: 0 0 10px rgba(16,185,129,0.2); font-weight: bold; }
  `]
})
export class AiChatComponent implements AfterViewChecked {
  @ViewChild('scrollContainer') private scrollContainer!: ElementRef;
  @ViewChild('chatInput') private chatInput!: ElementRef;

  private aiService = inject(AiService);
  isOpen = signal(false);
  isLoading = signal(false);
  messages = signal<{ role: string, content: string }[]>([]);
  currentInput = '';

  ngAfterViewChecked() {
    this.scrollToBottom();
  }

  closeChat() {
    this.isOpen.set(false);
  }

  toggleChat() {
    this.isOpen.set(!this.isOpen());
    if (this.isOpen()) {
      setTimeout(() => this.chatInput?.nativeElement.focus(), 200);
    }
  }

  private scrollToBottom() {
    if (this.scrollContainer) {
      this.scrollContainer.nativeElement.scrollTop = this.scrollContainer.nativeElement.scrollHeight;
    }
  }

  async sendMessage() {
    if (!this.currentInput.trim() || this.isLoading()) return;
    const val = this.currentInput;
    this.messages.update(m => [...m, { role: 'user', content: val }]);
    this.currentInput = '';
    this.isLoading.set(true);

    // Initial empty message to start streaming into
    this.messages.update(m => [...m, { role: 'ai', content: '' }]);

    try {
      const stream = this.aiService.chatQueryStream(val);
      for await (const chunk of stream) {
        this.isLoading.set(false); // Stop loader as soon as stream starts
        this.messages.update(m => {
          const newMessages = [...m];
          const lastMsg = newMessages[newMessages.length - 1];
          if (lastMsg.role === 'ai') {
            lastMsg.content += chunk;
          }
          return newMessages;
        });

        // Small delay to ensure scroll happens after Angular updates view
        setTimeout(() => this.scrollToBottom(), 50);
      }
    } catch (error) {
      this.messages.update(m => {
        const newMessages = [...m];
        const lastMsg = newMessages[newMessages.length - 1];
        if (lastMsg.role === 'ai' && !lastMsg.content) {
          lastMsg.content = '⚠️ Bağlantı hatası: Sisteme ulaşılamadı.';
        }
        return newMessages;
      });
      console.error('Stream error:', error);
    } finally {
      this.isLoading.set(false);
    }
  }

  formatMessage(content: string): string {
    if (!content) return '';
    let t = content.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>').replace(/`(.*?)`/g, '<code>$1</code>');
    return t.split('\n').map(l => l.trim() ? `<p>${l}</p>` : '').join('');
  }
}