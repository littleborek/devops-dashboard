import { Component, DestroyRef, ElementRef, OnInit, computed, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { forkJoin, finalize } from 'rxjs';
import { ApiService } from '../../services/api.service';
import { NavbarComponent } from '../navbar/navbar.component';
import { AiService, AiConfig } from '../../services/ai.service';
import { UiService } from '../../services/ui.service';
import { exceedsThreshold, metricValue, serverProblems } from './dashboard-metrics';
import { windowsAgentCommands } from './windows-agent-setup';

@Component({
    selector: 'app-dashboard', standalone: true,
    imports: [CommonModule, NavbarComponent, RouterLink, FormsModule],
    templateUrl: './dashboard.component.html', styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {
    public ui = inject(UiService);
    private api = inject(ApiService);
    private http = inject(HttpClient);
    private router = inject(Router);
    private ai = inject(AiService);
    private destroyRef = inject(DestroyRef);
    private timer?: ReturnType<typeof setInterval>;
    private previousFocus?: HTMLElement;
    dialog = viewChild<ElementRef<HTMLDialogElement>>('modal');
    servers = signal<any[]>([]);
    dockerData = signal<any[]>([]);
    k8sPods = signal<any[]>([]);
    loading = signal(true);
    dockerLoading = signal(true);
    k8sLoading = signal(true);
    serverError = signal(false);
    dockerError = signal(false);
    k8sError = signal(false);
    lastUpdated = signal<Date | null>(null);
    backendStatus = signal<'loading' | 'up' | 'error'>('loading');
    currentTab = signal('servers');
    modalKind = signal('');
    busy = signal(false);
    feedback = signal<'saved' | 'error' | 'copied' | 'copy-error' | 'settings-error' | ''>('');
    serverToDelete: any = null;
    formServer: any = this.emptyServer();
    aiConfig: AiConfig = { provider: 'CLOUD' };
    telegramConfig = { token: '', chatId: '' };
    telegramLoading = signal(false);
    telegramError = signal(false);
    problems = computed(() => this.servers().flatMap(server => serverProblems(server).map(kind => ({ server, kind }))));
    metric = metricValue;
    exceedsThreshold = exceedsThreshold;
    readonly tabs = [
        { id: 'servers', tr: 'Sunucular', en: 'Servers' },
        { id: 'docker', tr: 'Docker', en: 'Docker' },
        { id: 'k8s', tr: 'Kubernetes', en: 'Kubernetes' },
        { id: 'mcp', tr: 'MCP', en: 'MCP' },
    ];
    readonly mcpTools = [
        { name: 'analyze_incident', tr: 'Olay analizi', en: 'Incident analysis' },
        { name: 'get_server_status', tr: 'Sunucu metrikleri', en: 'Server metrics' },
        { name: 'ask_devops_llm', tr: 'Yerel modele soru sorar', en: 'Queries the local model' },
        { name: 'get_task_queue', tr: 'Bekleyen sunucu görevlerini listeler', en: 'Lists pending server tasks' },
        { name: 'devops://architecture', tr: 'Mimari kaynağı', en: 'Architecture resource' },
        { name: 'rca_prompt', tr: 'Kök neden analizi istemi', en: 'Root cause analysis prompt' },
    ];
    statusLabel(status: unknown) {
        switch (String(status ?? '').toLowerCase()) {
            case 'online': return this.ui.text('Çevrimiçi', 'Online');
            case 'offline': return this.ui.text('Çevrimdışı', 'Offline');
            case 'running': return this.ui.text('Çalışıyor', 'Running');
            case 'pending': return this.ui.text('Bekliyor', 'Pending');
            case 'failed': return this.ui.text('Başarısız', 'Failed');
            case 'succeeded': return this.ui.text('Tamamlandı', 'Succeeded');
            case 'exited': return this.ui.text('Durdu', 'Exited');
            case 'created': return this.ui.text('Oluşturuldu', 'Created');
            case 'paused': return this.ui.text('Duraklatıldı', 'Paused');
            case 'restarting': return this.ui.text('Yeniden başlıyor', 'Restarting');
            case 'dead': return this.ui.text('Sonlandı', 'Dead');
            default: return this.ui.text('Bilinmiyor', 'Unknown');
        }
    }
    uninstallCmd = 'crontab -l | sed "/devops_agent[.]sh/d" | crontab -';
    setupOs = 'linux';
    windowsSetupHost = '';
    windowsSetupUser = '';
    get windowsSetup() { return windowsAgentCommands(this.windowsSetupHost, this.windowsSetupUser); }
    get installCmd() { return `installer=$(mktemp) && curl -fsS '${window.location.origin}/api/v1/agent/script' -o "$installer"`; }
    emptyServer() { return { name: '', ipAddress: '', operatingSystem: 'linux', location: '', category: '', skipSslCheck: false }; }
    ngOnInit() {
        this.aiConfig = this.ai.getConfig(); this.loadServers();
        this.timer = setInterval(() => this.loadServers(false), 10000);
        this.destroyRef.onDestroy(() => clearInterval(this.timer));
    }
    loadServers(initial = true) {
        if (initial) {
            this.loading.set(true);
            this.dockerLoading.set(true);
            this.k8sLoading.set(true);
        }
        this.api.getServers().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
            next: data => { this.servers.set(data); this.serverError.set(false); this.loading.set(false); this.lastUpdated.set(new Date()); this.loadDockerData(data); },
            error: () => { this.serverError.set(true); this.loading.set(false); this.dockerLoading.set(false); this.dockerError.set(true); }
        });
        this.api.getK8sPods().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
            next: pods => { this.k8sPods.set(pods); this.k8sError.set(false); this.k8sLoading.set(false); },
            error: () => { this.k8sError.set(true); this.k8sLoading.set(false); }
        });
        this.http.get<{ status: string }>('/actuator/health').pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
            next: data => this.backendStatus.set(data.status === 'UP' ? 'up' : 'error'), error: () => this.backendStatus.set('error')
        });
    }
    loadDockerData(servers: any[]) {
        if (!servers.length) { this.dockerData.set([]); this.dockerError.set(false); this.dockerLoading.set(false); return; }
        forkJoin(servers.map(s => this.api.getDockerContainers(s.id))).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
            next: results => { this.dockerData.set(results.flatMap((containers, i) => containers.map(c => ({ ...c, serverName: c.serverName || servers[i].name })))); this.dockerError.set(false); this.dockerLoading.set(false); },
            error: () => { this.dockerError.set(true); this.dockerLoading.set(false); }
        });
    }
    onNavClick(target: string) {
        if (target === 'servers') this.currentTab.set('servers');
        else if (target === 'grafana') window.open('/actuator/prometheus', '_blank', 'noopener');
        else if (['setup', 'new-server', 'settings'].includes(target)) this.openModal(target);
    }
    openModal(kind: string, server?: any) {
        if (kind === 'setup' && !this.windowsSetupHost) {
            const windows = this.servers().find(s => String(s.operatingSystem ?? '').toLowerCase().startsWith('windows'));
            if (windows?.ipAddress) { this.setupOs = 'windows'; this.windowsSetupHost = windows.ipAddress; }
        }
        this.feedback.set(''); this.formServer = server ? { ...server } : this.emptyServer(); this.serverToDelete = server;
        this.previousFocus = document.activeElement as HTMLElement; this.modalKind.set(kind); this.dialog()?.nativeElement.showModal();
        this.dialog()?.nativeElement.querySelector<HTMLElement>('[autofocus]')?.focus();
        if (kind === 'settings') this.loadTelegramConfig();
    }
    closeModal() {
        if (this.busy()) return;
        this.dialog()?.nativeElement.close();
        this.modalKind.set('');
        this.serverToDelete = null;
        this.previousFocus?.focus();
    }
    cancelModal(event: Event) { event.preventDefault(); this.closeModal(); }
    saveServer() {
        if (this.busy() || !this.formServer.name?.trim() || !this.formServer.ipAddress?.trim() || !this.formServer.operatingSystem?.trim()) return;
        this.busy.set(true); this.feedback.set('');
        const request = this.modalKind() === 'edit' ? this.api.updateServer(this.formServer.id, this.formServer) : this.api.createServer(this.formServer);
        request.pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.busy.set(false))).subscribe({
            next: () => { this.busy.set(false); this.closeModal(); this.loadServers(); }, error: () => this.feedback.set('error')
        });
    }
    confirmDeleteServer() {
        if (!this.serverToDelete || this.busy()) return;
        this.busy.set(true); this.feedback.set('');
        this.api.deleteServer(this.serverToDelete.id).pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.busy.set(false))).subscribe({
            next: () => { this.busy.set(false); this.closeModal(); this.loadServers(); }, error: () => this.feedback.set('error')
        });
    }
    saveAiConfig() {
        try { this.ai.saveConfig(this.aiConfig); this.feedback.set('saved'); }
        catch { this.feedback.set('error'); }
    }
    loadTelegramConfig() {
        this.telegramLoading.set(true); this.telegramError.set(false);
        this.telegramConfig = { token: '', chatId: '' };
        if (this.feedback() === 'settings-error') this.feedback.set('');
        forkJoin([this.http.get('/api/v1/settings/TELEGRAM_BOT_TOKEN', { responseType: 'text' }), this.http.get('/api/v1/settings/TELEGRAM_CHAT_ID', { responseType: 'text' })])
            .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.telegramLoading.set(false))).subscribe({
                next: ([token, chatId]) => this.telegramConfig = { token, chatId }, error: () => { this.telegramError.set(true); this.feedback.set('settings-error'); }
            });
    }
    saveTelegramConfig() {
        if (this.busy() || this.telegramLoading() || !this.telegramConfig.token.trim() || !this.telegramConfig.chatId.trim()) return;
        this.busy.set(true); this.feedback.set('');
        forkJoin([this.http.post('/api/v1/settings', { key: 'TELEGRAM_BOT_TOKEN', value: this.telegramConfig.token }), this.http.post('/api/v1/settings', { key: 'TELEGRAM_CHAT_ID', value: this.telegramConfig.chatId })])
            .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.busy.set(false))).subscribe({ next: () => this.feedback.set('saved'), error: () => this.feedback.set('error') });
    }
    async copyToClipboard(text: string) { try { await navigator.clipboard.writeText(text); this.feedback.set('copied'); } catch { this.feedback.set('copy-error'); } }
    onLogout() { this.http.post('/api/auth/logout', {}).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({ next: () => this.router.navigate(['/login']), error: () => this.router.navigate(['/login']) }); }
}
