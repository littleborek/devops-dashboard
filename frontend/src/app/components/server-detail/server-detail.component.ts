import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { AiService } from '../../services/ai.service';

@Component({
    selector: 'app-server-detail',
    standalone: true,
    imports: [CommonModule, RouterLink, FormsModule],
    template: `
    <nav class="bg-gray-800 border-b border-gray-700 p-4 sticky top-0 z-50">
        <div class="container mx-auto flex items-center">
            <a routerLink="/" class="text-blue-400 hover:text-blue-300 transition mr-4 flex items-center">
                <i class="fa-solid fa-arrow-left mr-2"></i> Geri
            </a>
            <h1 class="text-xl font-bold text-white flex items-center">
                <i class="fa-solid fa-server mr-2 text-gray-500"></i>
                <span>{{ server() ? server()?.name : 'Sunucu Bulunamadı' }}</span>
            </h1>
            <span class="ml-4 px-2 py-1 rounded text-xs bg-gray-700 text-gray-300">
                {{ server()?.ipAddress || '-' }}
            </span>
        </div>
    </nav>

    <div class="container mx-auto mt-8 p-4 grid grid-cols-1 lg:grid-cols-3 gap-8" *ngIf="server()">
        <div class="space-y-6">
            <div class="bg-gray-800 rounded-lg shadow-lg p-6 border border-gray-700">
                <h2 class="text-lg font-semibold mb-4 text-blue-400">Sunucu Bilgileri</h2>
                <div class="space-y-3 text-sm">
                    <div class="flex justify-between">
                        <span class="text-gray-400">Durum:</span>
                        <span class="font-bold text-white">{{ server()?.status }}</span>
                    </div>
                    <div class="flex justify-between">
                        <span class="text-gray-400">Lokasyon:</span>
                        <span class="text-white">{{ server()?.location }}</span>
                    </div>
                    <div class="flex justify-between">
                        <span class="text-gray-400">OS:</span>
                        <span class="text-white">{{ server()?.operatingSystem }}</span>
                    </div>
                    <div class="flex justify-between">
                        <span class="text-gray-400">IP:</span>
                        <span class="text-white">{{ server()?.ipAddress }}</span>
                    </div>

                    <hr class="border-gray-700 my-2" />

                    <div class="flex items-center justify-between">
                        <span class="text-gray-400">CPU Alarm Eşiği (%):</span>
                        <input type="number" [value]="server()?.cpuUsageThreshold"
                            class="bg-gray-900 border border-gray-600 rounded px-2 py-1 w-20 text-right text-white focus:outline-none focus:ring-1 focus:ring-blue-500">
                    </div>
                    <div class="flex items-center justify-between">
                        <span class="text-gray-400">RAM Alarm Eşiği (%):</span>
                        <input type="number" [value]="server()?.ramUsageThreshold"
                            class="bg-gray-900 border border-gray-600 rounded px-2 py-1 w-20 text-right text-white focus:outline-none focus:ring-1 focus:ring-blue-500">
                    </div>
                </div>
            </div>
            <div class="bg-gray-800 rounded-lg shadow-lg p-6 border border-gray-700">
                <h2 class="text-lg font-semibold mb-4 text-purple-400">Hızlı İşlemler</h2>
                <button (click)="openTerminal()"
                    class="w-full bg-slate-700 hover:bg-slate-600 border border-slate-600 text-white font-semibold py-3 rounded mb-3 shadow-lg transition flex justify-center items-center transform active:scale-95">
                    <i class="fa-solid fa-terminal mr-2"></i> Uzak Terminal (Görev Gönder)
                </button>
                <button class="w-full bg-slate-700/50 text-gray-400 cursor-not-allowed py-2 rounded transition text-sm mb-3">
                    <i class="fa-solid fa-rocket mr-2"></i> CI/CD Pipeline (Yakında)
                </button>
                <button (click)="analyzeServer()" [disabled]="analyzing()"
                    class="w-full bg-purple-600 hover:bg-purple-700 text-white font-semibold py-3 rounded shadow-lg shadow-purple-600/20 transition flex justify-center items-center transform active:scale-95 disabled:opacity-50">
                    <i class="fa-solid fa-robot mr-2" [class.fa-spin]="analyzing()"></i> 
                    {{ analyzing() ? 'Analiz Ediliyor...' : 'AI ile Analiz Et' }}
                </button>

                <!-- AI Analysis Modal -->
                @if (aiAnalysisResult()) {
                  <div class="fixed inset-0 bg-slate-900/80 backdrop-blur-md z-[100] flex items-center justify-center p-4 fade-in" (click)="aiAnalysisResult.set(null)">
                      <div class="bg-slate-900 border border-purple-500/30 rounded-2xl shadow-[0_0_50px_rgba(168,85,247,0.15)] max-w-3xl w-full max-h-[85vh] flex flex-col relative overflow-hidden" (click)="$event.stopPropagation()">
                          <!-- Decorative gradient top border -->
                          <div class="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-purple-600 via-indigo-500 to-blue-500"></div>
                          
                          <div class="flex justify-between items-center p-6 border-b border-slate-800 bg-slate-900/50">
                              <div class="flex items-center">
                                  <div class="w-10 h-10 rounded-full bg-purple-500/10 flex items-center justify-center mr-4">
                                      <i class="fa-solid fa-wand-magic-sparkles text-xl text-purple-400"></i>
                                  </div>
                                  <div>
                                      <h2 class="text-xl font-bold text-white tracking-wide">Root Cause Analysis</h2>
                                      <p class="text-xs text-slate-400 mt-0.5">Powered by AI-Ops Engine</p>
                                  </div>
                              </div>
                              <button (click)="aiAnalysisResult.set(null)" class="w-8 h-8 rounded-full bg-slate-800 flex items-center justify-center text-slate-400 hover:text-white hover:bg-slate-700 transition">
                                <i class="fa-solid fa-xmark"></i>
                              </button>
                          </div>
                          
                          <!-- Modal Body -->
                          <div class="p-8 overflow-y-auto bg-slate-900/80 prose prose-invert max-w-none custom-scrollbar">
                              <div class="text-slate-300 text-[15px] leading-relaxed whitespace-pre-wrap font-sans">
                                  {{ aiAnalysisResult() }}
                              </div>
                          </div>
                          
                          <div class="p-4 bg-slate-800/50 border-t border-slate-800 flex justify-end">
                              <button class="px-5 py-2 rounded-lg bg-slate-700 hover:bg-slate-600 text-white font-medium text-sm transition" (click)="aiAnalysisResult.set(null)">Kapat</button>
                          </div>
                      </div>
                  </div>
                }

                <!-- Remote Terminal Modal -->
                @if (showTerminalModal()) {
                  <div class="fixed inset-0 bg-slate-900/80 backdrop-blur-md z-[100] flex items-center justify-center p-4 fade-in" (click)="closeTerminal()">
                      <div class="bg-gray-900 border border-slate-700 rounded-xl shadow-2xl max-w-2xl w-full flex flex-col relative overflow-hidden font-mono" (click)="$event.stopPropagation()">
                          <!-- Mac Style Header -->
                          <div class="flex items-center px-4 py-3 border-b border-gray-800 bg-gray-900">
                             <div class="flex space-x-2 mr-4">
                               <div class="w-3 h-3 rounded-full bg-red-500 cursor-pointer hover:bg-red-600" (click)="closeTerminal()"></div>
                               <div class="w-3 h-3 rounded-full bg-yellow-500"></div>
                               <div class="w-3 h-3 rounded-full bg-green-500"></div>
                             </div>
                             <div class="text-xs text-gray-400 font-sans mx-auto mr-12">{{ server()?.name }} - Terminal</div>
                          </div>
                          
                          <!-- Modal Body -->
                          <div class="p-6 overflow-y-auto bg-black h-[350px] custom-scrollbar text-sm text-gray-300 leading-relaxed font-mono whitespace-pre-wrap break-words" #terminalScroll>
                              <div class="text-blue-400 mb-4">DevOps Dashboard Secure Terminal v1.0<br>Bağlanılan Sunucu: {{ server()?.ipAddress }}<br>Yetki: Ajan Root</div>
                              @for (line of terminalOutput(); track $index) {
                                  <div><span [innerHTML]="line"></span></div>
                              }
                          </div>
                          
                          <div class="px-4 py-3 bg-gray-900 border-t border-gray-800 flex items-center">
                              <span class="text-green-500 mr-2 font-bold">root&#64;agent:~$</span>
                              <input type="text" [(ngModel)]="terminalInput" (keyup.enter)="sendTerminalCommand()"
                                    class="bg-transparent text-gray-200 outline-none w-full placeholder-gray-600 font-mono text-sm"
                                    [disabled]="terminalWaiting()"
                                    placeholder="Enter bash command (e.g. ls -la, free -m, ps aux)"
                                    autofocus>
                              <i *ngIf="terminalWaiting()" class="fa-solid fa-circle-notch fa-spin text-gray-500 ml-2"></i>
                          </div>
                      </div>
                  </div>
                }
            </div>
        </div>
        <div class="lg:col-span-2">
            <div class="bg-gray-800 rounded-lg shadow-lg border border-gray-700 overflow-hidden">
                <div class="p-4 border-b border-gray-700 flex justify-between items-center bg-gray-800/50">
                    <h2 class="text-lg font-semibold text-white">Deploy Geçmişi</h2>
                    <span class="text-xs text-gray-500">Son 50 işlem</span>
                </div>

                <div class="overflow-x-auto">
                  <table class="w-full text-left">
                      <thead class="bg-gray-900/50 text-gray-400 text-xs uppercase font-semibold">
                          <tr>
                              <th class="p-4">Uygulama</th>
                              <th class="p-4">Versiyon</th>
                              <th class="p-4">Durum</th>
                              <th class="p-4">Tarih</th>
                              <th class="p-4 text-right">Log</th>
                          </tr>
                      </thead>
                      <tbody class="divide-y divide-gray-700 text-sm">
                          @for (deploy of deployments(); track deploy.id) {
                            <tr class="hover:bg-gray-700/30 transition">
                                <td class="p-4 font-medium text-white">{{ deploy.applicationName }}</td>
                                <td class="p-4 text-blue-300 font-mono">{{ deploy.version }}</td>
                                <td class="p-4">
                                    <span 
                                        [ngClass]="deploy.status === 'SUCCESS' ? 'bg-green-900/40 text-green-400 border border-green-800' : 'bg-red-900/40 text-red-400 border border-red-800'"
                                        class="px-2 py-0.5 rounded text-[10px] font-bold">
                                        {{ deploy.status || 'PENDING' }}
                                    </span>
                                </td>
                                <td class="p-4 text-gray-400">{{ deploy.createdAt | date:'short' }}</td>
                                <td class="p-4 text-right">
                                    <button class="text-gray-400 hover:text-blue-400 transition cursor-pointer" title="Logları İncele">
                                        <i class="fa-solid fa-file-code text-lg"></i>
                                    </button>
                                </td>
                            </tr>
                          }
                          @if (deployments().length === 0) {
                              <tr>
                                  <td colspan="5" class="p-12 text-center text-gray-500">
                                      <i class="fa-solid fa-box-open mb-3 text-3xl opacity-20 block"></i>
                                      <p>Henüz deploy kaydı yok.</p>
                                  </td>
                              </tr>
                          }
                      </tbody>
                  </table>
                </div>
            </div>
        </div>
    </div>

    <div class="container mx-auto mt-20 text-center" *ngIf="!server() && !loading()">
        <i class="fa-solid fa-triangle-exclamation text-4xl text-yellow-500 mb-4"></i>
        <h2 class="text-2xl font-bold text-white">Sunucu Bulunamadı</h2>
        <p class="text-gray-400 mt-2">Aradığınız ID'ye sahip bir sunucu kaydı yok.</p>
        <a routerLink="/" class="mt-6 inline-block bg-blue-600 px-6 py-2 rounded text-white hover:bg-blue-700 font-semibold transition shadow-lg shadow-blue-600/20">Ana Sayfaya Dön</a>
    </div>
  `,
    styles: [`
    .fade-in { animation: modalFadeIn 0.3s cubic-bezier(0.16, 1, 0.3, 1); }
    @keyframes modalFadeIn { 
      0% { opacity: 0; transform: scale(0.95) translateY(10px); } 
      100% { opacity: 1; transform: scale(1) translateY(0); } 
    }
    .custom-scrollbar::-webkit-scrollbar { width: 8px; }
    .custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
    .custom-scrollbar::-webkit-scrollbar-thumb { background: rgba(51, 65, 85, 0.6); border-radius: 4px; }
    .custom-scrollbar::-webkit-scrollbar-thumb:hover { background: rgba(71, 85, 105, 0.8); }
  `]
})
export class ServerDetailComponent implements OnInit {
    private route = inject(ActivatedRoute);
    private apiService = inject(ApiService);
    private aiService = inject(AiService);

    server = signal<any>(null);
    deployments = signal<any[]>([]);
    loading = signal(true);
    analyzing = signal(false);
    aiAnalysisResult = signal<string | null>(null);

    // Terminal State
    showTerminalModal = signal(false);
    terminalInput = '';
    terminalOutput = signal<string[]>([]);
    terminalWaiting = signal(false);
    private intervalId: any = null;

    ngOnInit() {
        this.route.params.subscribe(params => {
            const id = +params['id'];
            if (id) {
                this.loadServer(id);
                this.loadDeployments(id);
            }
        });
    }

    loadServer(id: number) {
        this.apiService.getServer(id).subscribe({
            next: (data) => {
                this.server.set(data);
                this.loading.set(false);
            },
            error: () => this.loading.set(false)
        });
    }

    loadDeployments(id: number) {
        this.apiService.getDeployments(id).subscribe(data => {
            this.deployments.set(data);
        });
    }

    analyzeServer() {
        const s = this.server();
        if (!s) return;

        this.analyzing.set(true);
        this.aiAnalysisResult.set(null);

        let eventContext = '';
        if (s.cpuUsage > 80) eventContext += 'High CPU Usage detected. ';
        if (s.ramUsage > 80) eventContext += 'High RAM Usage detected. ';
        if (s.status === 'OFFLINE') eventContext += 'Server is OFFLINE. ';

        this.aiService.analyzeIssue(s.id, eventContext, 'No recent log traces provided.').subscribe({
            next: (res) => {
                this.aiAnalysisResult.set(res.analysis);
                this.analyzing.set(false);
            },
            error: (err) => {
                this.aiAnalysisResult.set('Error communicating with AI Assistant: ' + (err.error?.error || err.message));
                this.analyzing.set(false);
            }
        });
    }

    openTerminal() {
        this.showTerminalModal.set(true);
        this.terminalOutput.set([...this.terminalOutput(), '<span class="text-yellow-500">Hazır. Ajanın poll yapması 15 saniyeye kadar sürebilir.</span>']);
    }

    closeTerminal() {
        this.showTerminalModal.set(false);
        if (this.intervalId) {
            clearInterval(this.intervalId);
            this.terminalWaiting.set(false);
        }
    }

    sendTerminalCommand() {
        if (!this.terminalInput.trim() || this.terminalWaiting()) return;
        const cmd = this.terminalInput;
        this.terminalInput = '';
        this.terminalOutput.set([...this.terminalOutput(), `<span class="text-green-500">root&#64;agent:~$</span> ${cmd}`]);
        this.terminalWaiting.set(true);

        this.apiService.queueTask(this.server().id, cmd).subscribe({
            next: (res) => {
                const taskId = res.taskId;
                this.pollTaskResult(taskId);
            },
            error: () => {
                this.terminalOutput.set([...this.terminalOutput(), '<span class="text-red-500">Sunucuya görev gönderilemedi.</span>']);
                this.terminalWaiting.set(false);
            }
        });
    }

    pollTaskResult(taskId: number, attempt = 0) {
        if (attempt > 30) {
            this.terminalOutput.set([...this.terminalOutput(), '<span class="text-red-500">Timeout: Görev 30 saniye içinde yanıtlanmadı! Sunucu OFFLINE olabilir veya Ajan kurulu değil.</span>']);
            this.terminalWaiting.set(false);
            return;
        }

        this.apiService.getTaskResult(taskId).subscribe({
            next: (task) => {
                if (task.status === 'PENDING') {
                    this.intervalId = setTimeout(() => this.pollTaskResult(taskId, attempt + 1), 2000);
                } else if (task.status === 'EXECUTED') {
                    const decoded = task.result ? task.result.replace(/\\n/g, '<br>') : 'Bitti (Çıktı yok)';
                    this.terminalOutput.set([...this.terminalOutput(), decoded.substring(1, decoded.length - 1)]);
                    this.terminalWaiting.set(false);
                } else {
                    this.terminalOutput.set([...this.terminalOutput(), '<span class="text-red-500">Hata oluştu. Durum: ' + task.status + '</span>']);
                    this.terminalWaiting.set(false);
                }
            },
            error: () => {
                this.terminalOutput.set([...this.terminalOutput(), '<span class="text-red-500">Sonuç getirilirken hata.</span>']);
                this.terminalWaiting.set(false);
            }
        });
    }
}
