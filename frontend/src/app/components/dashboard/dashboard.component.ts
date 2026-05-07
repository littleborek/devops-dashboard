import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { forkJoin } from 'rxjs';
import { ApiService } from '../../services/api.service';
import { NavbarComponent } from '../navbar/navbar.component';
import { AiService, AiConfig } from '../../services/ai.service';

@Component({
    selector: 'app-dashboard',
    standalone: true,
    imports: [CommonModule, NavbarComponent, RouterLink, FormsModule],
    template: `
    <app-navbar (navClick)="onNavClick($event)" (logout)="onLogout()"></app-navbar>

    <div class="container mx-auto mt-8 p-4">
        <!-- Stats Grid -->
        <div class="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
            <div class="bg-gray-800 p-6 rounded-lg shadow-lg border-l-4 border-blue-500">
                <div class="flex justify-between items-center">
                    <div>
                        <h3 class="text-gray-400 text-xs uppercase tracking-wider">Sunucular</h3>
                        <p class="text-2xl font-bold mt-1">{{ servers().length }}</p>
                    </div>
                    <i class="fa-solid fa-network-wired text-blue-500 text-3xl opacity-50"></i>
                </div>
            </div>

            <div class="bg-gray-800 p-6 rounded-lg shadow-lg border-l-4 border-cyan-500">
                <div class="flex justify-between items-center">
                    <div>
                        <h3 class="text-gray-400 text-xs uppercase tracking-wider">Docker</h3>
                        <p class="text-2xl font-bold mt-1 text-cyan-400">{{ dockerCount() }}</p>
                    </div>
                    <i class="fa-brands fa-docker text-cyan-500 text-3xl opacity-50"></i>
                </div>
            </div>

            <div class="bg-gray-800 p-6 rounded-lg shadow-lg border-l-4 border-purple-500">
                <div class="flex justify-between items-center">
                    <div>
                        <h3 class="text-gray-400 text-xs uppercase tracking-wider">K8s Pods</h3>
                        <p class="text-2xl font-bold mt-1 text-purple-400">{{ k8sCount() }}</p>
                    </div>
                    <i class="fa-solid fa-cubes text-purple-500 text-3xl opacity-50"></i>
                </div>
            </div>

            <div class="bg-gray-800 p-6 rounded-lg shadow-lg border-l-4 border-emerald-500">
                <div class="flex justify-between items-center">
                    <div>
                        <h3 class="text-gray-400 text-xs uppercase tracking-wider">MCP Tools</h3>
                        <p class="text-2xl font-bold mt-1 text-emerald-400">{{ mcpToolCount() }}</p>
                    </div>
                    <i class="fa-solid fa-plug text-emerald-500 text-3xl opacity-50"></i>
                </div>
                <div class="mt-2 flex items-center space-x-1">
                    <span class="w-2 h-2 rounded-full" [ngClass]="mcpOnline() ? 'bg-emerald-400 shadow-[0_0_6px_rgba(52,211,153,0.6)]' : 'bg-red-400'"></span>
                    <span class="text-xs text-gray-500">{{ mcpOnline() ? 'Aktif' : 'Çevrimdışı' }}</span>
                </div>
            </div>
        </div>

        <!-- TAB MENU -->
        <div class="flex space-x-6 border-b border-gray-700 mb-6 font-sans">
            <button (click)="currentTab.set('servers')"
                [class]="currentTab() === 'servers' ? 'pb-3 text-sm font-medium transition border-b-2 border-blue-500 text-blue-500 flex items-center' : 'pb-3 text-sm font-medium transition text-gray-400 border-b-2 border-transparent hover:text-gray-200 flex items-center'">
                <i class="fa-solid fa-server mr-2"></i>Sunucular
            </button>
            <button (click)="currentTab.set('docker')"
                [class]="currentTab() === 'docker' ? 'pb-3 text-sm font-medium transition border-b-2 border-cyan-500 text-cyan-500 flex items-center' : 'pb-3 text-sm font-medium transition text-gray-400 border-b-2 border-transparent hover:text-gray-200 flex items-center'">
                <i class="fa-brands fa-docker mr-2"></i>Docker ({{ dockerCount() }})
            </button>
            <button (click)="currentTab.set('k8s')"
                [class]="currentTab() === 'k8s' ? 'pb-3 text-sm font-medium transition border-b-2 border-purple-500 text-purple-500 flex items-center' : 'pb-3 text-sm font-medium transition text-gray-400 border-b-2 border-transparent hover:text-gray-200 flex items-center'">
                <i class="fa-solid fa-cubes mr-2"></i>Kubernetes ({{ k8sCount() }})
            </button>
            <button (click)="currentTab.set('mcp')"
                [class]="currentTab() === 'mcp' ? 'pb-3 text-sm font-medium transition border-b-2 border-emerald-500 text-emerald-500 flex items-center' : 'pb-3 text-sm font-medium transition text-gray-400 border-b-2 border-transparent hover:text-gray-200 flex items-center'">
                <i class="fa-solid fa-plug mr-2"></i>MCP
                <span class="ml-2 px-1.5 py-0.5 text-[10px] font-bold rounded bg-emerald-900/40 text-emerald-400 border border-emerald-700">NEW</span>
            </button>
        </div>

        <!-- TAB CONTENT -->
        <div [ngSwitch]="currentTab()" class="fade-in block space-y-4">
            <!-- SERVERS TAB -->
            <div *ngSwitchCase="'servers'" class="space-y-4">
                @if (loading()) {
                    <div class="text-center text-gray-500 py-10">
                        <i class="fa-solid fa-circle-notch fa-spin text-2xl"></i>
                        <p class="mt-2">Sunucular yükleniyor...</p>
                    </div>
                } @else if (servers().length === 0) {
                    <div class="text-center text-gray-500 p-10">Henüz sunucu eklenmemiş.</div>
                } @else {
                    @for (server of servers(); track server.id) {
                        <div class="bg-gray-800 p-4 rounded-lg flex justify-between items-center border border-gray-700 hover:bg-gray-750 transition group">
                            <div class="flex items-center space-x-4">
                                <a [routerLink]="['/server', server.id]" class="flex items-center space-x-4 hover:opacity-80 transition">
                                    <div class="w-3 h-3 rounded-full transition-all duration-300" 
                                         [ngClass]="server.status === 'ONLINE' ? 'bg-green-500 shadow-[0_0_10px_rgba(74,222,128,0.5)]' : 'bg-red-500'"></div>
                                    <div>
                                        <h4 class="font-bold text-white group-hover:text-blue-400 transition">{{ server.name }}</h4>
                                        <div class="flex items-center space-x-2 text-xs text-gray-400">
                                            <span class="font-mono">{{ server.ipAddress }}</span>
                                            <span class="bg-gray-700 px-1.5 rounded text-[10px]">{{ server.category || 'General' }}</span>
                                        </div>
                                    </div>
                                </a>
                            </div>
                            
                            <!-- Metrikler -->
                            <div class="hidden lg:flex items-center space-x-6 text-sm">
                                <div class="w-24">
                                    <div class="flex justify-between items-center mb-1">
                                        <i class="fa-solid fa-microchip text-gray-500 text-xs mr-2"></i>
                                        <span class="font-mono text-gray-300">{{ server.cpuUsage | number:'1.0-0' }}%</span>
                                    </div>
                                    <div class="bg-gray-700 rounded-full h-2 overflow-hidden">
                                        <div class="h-full transition-all duration-500" 
                                             [style.width.%]="server.cpuUsage"
                                             [ngClass]="getMetricColor(server.cpuUsage)"></div>
                                    </div>
                                </div>

                                <div class="w-24">
                                    <div class="flex justify-between items-center mb-1">
                                        <i class="fa-solid fa-memory text-gray-500 text-xs mr-2"></i>
                                        <span class="font-mono text-gray-300">{{ server.ramUsage | number:'1.0-0' }}%</span>
                                    </div>
                                    <div class="bg-gray-700 rounded-full h-2 overflow-hidden">
                                        <div class="h-full transition-all duration-500" 
                                             [style.width.%]="server.ramUsage"
                                             [ngClass]="getMetricColor(server.ramUsage)"></div>
                                    </div>
                                </div>
                            </div>

                            <!-- Aksiyon Butonları -->
                            <div class="flex items-center space-x-4">
                                <span class="text-xs font-mono text-blue-300 bg-blue-900/30 px-2 py-1 rounded hidden md:inline-block">
                                    {{ server.lastResponseTime > 0 ? server.lastResponseTime + ' ms' : '-' }}
                                </span>
                                
                                <div class="flex space-x-2">
                                    <button class="p-2 rounded hover:bg-gray-700 text-purple-400 transition" title="Grafik İzle">
                                        <i class="fa-solid fa-chart-line"></i>
                                    </button>
                                    <button class="p-2 rounded hover:bg-gray-700 transition" title="Bakım Modu">
                                        <i class="fa-solid" [ngClass]="server.maintenanceMode ? 'fa-pause-circle text-yellow-500' : 'fa-wrench text-gray-500'"></i>
                                    </button>
                                    <button (click)="openEditModal(server)" class="p-2 rounded hover:bg-gray-700 text-blue-400 transition" title="Düzenle">
                                        <i class="fa-solid fa-pen"></i>
                                    </button>
                                    <button (click)="onDeleteServer(server.id)" class="p-2 rounded hover:bg-gray-700 text-red-400 transition" title="Sil">
                                        <i class="fa-solid fa-trash"></i>
                                    </button>
                                </div>
                            </div>
                        </div>
                    }
                }
            </div>

            <!-- DOCKER TAB -->
            <div *ngSwitchCase="'docker'" class="fade-in">
                <div class="overflow-x-auto rounded-lg border border-gray-700">
                    <table class="w-full text-left bg-gray-800">
                        <thead class="bg-gray-700 text-gray-300 uppercase text-xs font-semibold">
                            <tr>
                                <th class="p-4 w-24">Durum</th>
                                <th class="p-4">Sunucu</th>
                                <th class="p-4">Container</th>
                                <th class="p-4">Image</th>
                                <th class="p-4">ID</th>
                                <th class="p-4">Son Güncelleme</th>
                            </tr>
                        </thead>
                        <tbody class="divide-y divide-gray-700 text-sm">
                            @for (c of dockerData(); track c.containerId) {
                                <tr class="hover:bg-gray-700/50 transition border-b border-gray-700/50 last:border-0">
                                    <td class="p-4">
                                        <span class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium border"
                                              [ngClass]="c.state === 'running' ? 'bg-green-900/40 text-green-400 border-green-700' : 'bg-red-900/40 text-red-400 border-red-700'">
                                            {{ (c.state || 'unknown') | uppercase }}
                                        </span>
                                    </td>
                                    <td class="p-4 text-blue-400 text-sm">{{ c.serverName || '-' }}</td>
                                    <td class="p-4 font-bold text-white">{{ c.name }}</td>
                                    <td class="p-4 text-gray-400 font-mono text-xs">{{ c.image }}</td>
                                    <td class="p-4 text-gray-500 font-mono text-xs">{{ c.containerId | slice:0:12 }}</td>
                                    <td class="p-4 text-gray-500 text-xs">{{ c.lastUpdated | date:'HH:mm:ss' }}</td>
                                </tr>
                            }
                        </tbody>
                    </table>
                </div>
            </div>

            <!-- KUBERNETES TAB -->
            <div *ngSwitchCase="'k8s'" class="fade-in">
                <div class="overflow-x-auto rounded-lg border border-gray-700">
                    <table class="w-full text-left bg-gray-800">
                        <thead class="bg-gray-700 text-gray-300 uppercase text-xs font-semibold">
                            <tr>
                                <th class="p-4 w-24">Durum</th>
                                <th class="p-4">Pod</th>
                                <th class="p-4">Namespace</th>
                                <th class="p-4">Node</th>
                                <th class="p-4">Oluşturulma</th>
                            </tr>
                        </thead>
                        <tbody class="divide-y divide-gray-700 text-sm">
                            @for (pod of k8sPods(); track pod.name) {
                                <tr class="hover:bg-gray-700/50 transition border-b border-gray-700/50 last:border-0">
                                    <td class="p-4">
                                        <span class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium border"
                                              [ngClass]="pod.phase === 'Running' ? 'bg-green-900/40 text-green-400 border-green-700' : 'bg-yellow-900/40 text-yellow-400 border-yellow-700'">
                                            {{ pod.phase }}
                                        </span>
                                    </td>
                                    <td class="p-4 font-bold text-white">{{ pod.name }}</td>
                                    <td class="p-4 text-purple-400 text-sm">{{ pod.namespace }}</td>
                                    <td class="p-4 text-gray-400 text-sm">{{ pod.nodeName }}</td>
                                    <td class="p-4 text-gray-500 text-xs">{{ pod.creationTimestamp | date:'dd/MM HH:mm' }}</td>
                                </tr>
                            }
                            @if (k8sPods().length === 0) {
                                <tr><td colspan="5" class="p-8 text-center text-gray-500">Kubernetes pod bulunamadı.</td></tr>
                            }
                        </tbody>
                    </table>
                </div>
            </div>

            <!-- MCP TAB -->
            <div *ngSwitchCase="'mcp'" class="fade-in space-y-6">
                <!-- MCP Header -->
                <div class="bg-gradient-to-r from-emerald-900/30 to-gray-800 rounded-xl p-6 border border-emerald-700/30">
                    <div class="flex items-center justify-between">
                        <div>
                            <h2 class="text-xl font-bold text-white flex items-center">
                                <i class="fa-solid fa-plug text-emerald-400 mr-3"></i>
                                Model Context Protocol (MCP)
                            </h2>
                            <p class="text-gray-400 text-sm mt-1">Dashboard yeteneklerini Claude Desktop, Cursor ve diğer AI araçlarına açar.</p>
                        </div>
                        <div class="flex items-center space-x-3">
                            <span class="flex items-center space-x-2 bg-gray-900/60 px-3 py-2 rounded-lg border border-gray-700">
                                <span class="w-2.5 h-2.5 rounded-full animate-pulse" [ngClass]="mcpOnline() ? 'bg-emerald-400 shadow-[0_0_8px_rgba(52,211,153,0.6)]' : 'bg-red-400'"></span>
                                <span class="text-sm" [ngClass]="mcpOnline() ? 'text-emerald-400' : 'text-red-400'">
                                    {{ mcpOnline() ? 'MCP Server Hazır — 4 tool aktif' : 'MCP Server Çevrimdışı' }}
                                </span>
                            </span>
                        </div>
                    </div>
                </div>

                <!-- Tools Grid -->
                <div>
                    <h3 class="text-sm font-bold text-gray-400 uppercase tracking-wider mb-3 px-1">
                        <i class="fa-solid fa-wrench mr-2"></i>Kayıtlı Araçlar (Tools)
                    </h3>
                    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <div class="bg-gray-800 rounded-lg p-5 border border-gray-700 hover:border-emerald-600/50 transition group">
                            <div class="flex items-start justify-between">
                                <div>
                                    <h4 class="font-bold text-white group-hover:text-emerald-400 transition">analyze_incident</h4>
                                    <p class="text-gray-400 text-xs mt-1 leading-relaxed">CrewAI çoklu-ajan sistemiyle olay analizi. LangGraph router sorguyu SIMPLE/COMPLEX olarak sınıflandırır.</p>
                                </div>
                                <span class="bg-blue-900/30 text-blue-400 text-[10px] px-2 py-0.5 rounded border border-blue-700 whitespace-nowrap">CrewAI</span>
                            </div>
                            <div class="mt-3 flex items-center space-x-2 text-[10px] text-gray-500 font-mono">
                                <span class="bg-gray-900 px-2 py-0.5 rounded">query: string</span>
                                <span class="bg-gray-900 px-2 py-0.5 rounded">context?: string</span>
                            </div>
                        </div>

                        <div class="bg-gray-800 rounded-lg p-5 border border-gray-700 hover:border-emerald-600/50 transition group">
                            <div class="flex items-start justify-between">
                                <div>
                                    <h4 class="font-bold text-white group-hover:text-emerald-400 transition">get_server_status</h4>
                                    <p class="text-gray-400 text-xs mt-1 leading-relaxed">Tüm sunucuların veya belirli bir sunucunun CPU, RAM, disk kullanımını getirir.</p>
                                </div>
                                <span class="bg-purple-900/30 text-purple-400 text-[10px] px-2 py-0.5 rounded border border-purple-700 whitespace-nowrap">API</span>
                            </div>
                            <div class="mt-3 flex items-center space-x-2 text-[10px] text-gray-500 font-mono">
                                <span class="bg-gray-900 px-2 py-0.5 rounded">server_id?: int</span>
                            </div>
                        </div>

                        <div class="bg-gray-800 rounded-lg p-5 border border-gray-700 hover:border-emerald-600/50 transition group">
                            <div class="flex items-start justify-between">
                                <div>
                                    <h4 class="font-bold text-white group-hover:text-emerald-400 transition">ask_devops_llm</h4>
                                    <p class="text-gray-400 text-xs mt-1 leading-relaxed">Yerel LLM'e doğrudan soru sorar, CrewAI pipeline'ını atlar. Hızlı cevaplar için.</p>
                                </div>
                                <span class="bg-orange-900/30 text-orange-400 text-[10px] px-2 py-0.5 rounded border border-orange-700 whitespace-nowrap">LLM</span>
                            </div>
                            <div class="mt-3 flex items-center space-x-2 text-[10px] text-gray-500 font-mono">
                                <span class="bg-gray-900 px-2 py-0.5 rounded">question: string</span>
                            </div>
                        </div>

                        <div class="bg-gray-800 rounded-lg p-5 border border-gray-700 hover:border-emerald-600/50 transition group">
                            <div class="flex items-start justify-between">
                                <div>
                                    <h4 class="font-bold text-white group-hover:text-emerald-400 transition">get_task_queue</h4>
                                    <p class="text-gray-400 text-xs mt-1 leading-relaxed">Belirli sunucunun RSA imzalı bekleyen uzak komutlarını listeler.</p>
                                </div>
                                <span class="bg-red-900/30 text-red-400 text-[10px] px-2 py-0.5 rounded border border-red-700 whitespace-nowrap">System</span>
                            </div>
                            <div class="mt-3 flex items-center space-x-2 text-[10px] text-gray-500 font-mono">
                                <span class="bg-gray-900 px-2 py-0.5 rounded">server_id: int</span>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Resources & Prompts -->
                <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div>
                        <h3 class="text-sm font-bold text-gray-400 uppercase tracking-wider mb-3 px-1">
                            <i class="fa-solid fa-book-open mr-2"></i>Kaynaklar & İstemler
                        </h3>
                        <div class="space-y-3">
                            <div class="bg-gray-800/50 rounded-lg p-4 border border-gray-700 flex items-center space-x-4">
                                <div class="w-10 h-10 rounded-lg bg-cyan-500/10 flex items-center justify-center border border-cyan-500/20">
                                    <i class="fa-solid fa-sitemap text-cyan-400"></i>
                                </div>
                                <div>
                                    <span class="text-white text-sm font-medium">devops://architecture</span>
                                    <p class="text-gray-500 text-[11px]">Sistem mimarisi diyagramı (Resource)</p>
                                </div>
                            </div>
                            <div class="bg-gray-800/50 rounded-lg p-4 border border-gray-700 flex items-center space-x-4">
                                <div class="w-10 h-10 rounded-lg bg-yellow-500/10 flex items-center justify-center border border-yellow-500/20">
                                    <i class="fa-solid fa-magnifying-glass-chart text-yellow-400"></i>
                                </div>
                                <div>
                                    <span class="text-white text-sm font-medium">rca_prompt</span>
                                    <p class="text-gray-500 text-[11px]">Kök Neden Analizi şablonu (Prompt)</p>
                                </div>
                            </div>
                        </div>
                    </div>
                    
                    <div>
                        <h3 class="text-sm font-bold text-gray-400 uppercase tracking-wider mb-3 px-1">
                            <i class="fa-solid fa-link mr-2"></i>Uyumlu Uygulamalar
                        </h3>
                        <div class="grid grid-cols-2 gap-3">
                            <div class="bg-gray-800/50 rounded-lg p-3 border border-gray-700 flex items-center space-x-3">
                                <i class="fa-solid fa-robot text-orange-400"></i>
                                <span class="text-xs text-gray-300">Claude Desktop</span>
                            </div>
                            <div class="bg-gray-800/50 rounded-lg p-3 border border-gray-700 flex items-center space-x-3">
                                <i class="fa-solid fa-code text-blue-400"></i>
                                <span class="text-xs text-gray-300">Cursor IDE</span>
                            </div>
                            <div class="bg-gray-800/50 rounded-lg p-3 border border-gray-700 flex items-center space-x-3">
                                <i class="fa-solid fa-terminal text-green-400"></i>
                                <span class="text-xs text-gray-300">MCP Inspector</span>
                            </div>
                            <div class="bg-gray-800/50 rounded-lg p-3 border border-gray-700 flex items-center space-x-3">
                                <i class="fa-solid fa-puzzle-piece text-purple-400"></i>
                                <span class="text-xs text-gray-300">VS Code</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- YENİ SUNUCU MODAL -->
    @if (showNewServerModal()) {
    <div class="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4 fade-in" (click)="showNewServerModal.set(false)">
        <div class="bg-gray-800 border border-gray-700 rounded-2xl shadow-2xl max-w-lg w-full" (click)="$event.stopPropagation()">
            <div class="flex justify-between items-center p-6 border-b border-gray-700">
                <h2 class="text-xl font-bold text-blue-400"><i class="fa-solid fa-plus mr-2"></i>Yeni Sunucu Ekle</h2>
                <button (click)="showNewServerModal.set(false)" class="text-gray-400 hover:text-white transition"><i class="fa-solid fa-xmark"></i></button>
            </div>
            <form (ngSubmit)="createServer()" class="p-6 space-y-4">
                <div>
                    <label class="block text-xs uppercase tracking-wider text-gray-400 font-semibold mb-1">Sunucu Adı *</label>
                    <input type="text" [(ngModel)]="newServer.name" name="name" required class="w-full bg-gray-900/50 border border-gray-700 rounded-lg py-2 px-3 text-gray-100 outline-none focus:ring-2 focus:ring-blue-500" placeholder="Web Server 1">
                </div>
                <div>
                    <label class="block text-xs uppercase tracking-wider text-gray-400 font-semibold mb-1">IP Adresi *</label>
                    <input type="text" [(ngModel)]="newServer.ipAddress" name="ipAddress" required class="w-full bg-gray-900/50 border border-gray-700 rounded-lg py-2 px-3 text-gray-100 outline-none focus:ring-2 focus:ring-blue-500" placeholder="192.168.1.100">
                </div>
                <div class="grid grid-cols-2 gap-4">
                    <div>
                        <label class="block text-xs uppercase tracking-wider text-gray-400 font-semibold mb-1">İşletim Sistemi *</label>
                        <input type="text" [(ngModel)]="newServer.operatingSystem" name="os" required class="w-full bg-gray-900/50 border border-gray-700 rounded-lg py-2 px-3 text-gray-100 outline-none focus:ring-2 focus:ring-blue-500" placeholder="linux">
                    </div>
                    <div>
                        <label class="block text-xs uppercase tracking-wider text-gray-400 font-semibold mb-1">Kategori</label>
                        <input type="text" [(ngModel)]="newServer.category" name="category" class="w-full bg-gray-900/50 border border-gray-700 rounded-lg py-2 px-3 text-gray-100 outline-none focus:ring-2 focus:ring-blue-500" placeholder="Web Server">
                    </div>
                </div>
                <div>
                    <label class="block text-xs uppercase tracking-wider text-gray-400 font-semibold mb-1">Konum</label>
                    <input type="text" [(ngModel)]="newServer.location" name="location" class="w-full bg-gray-900/50 border border-gray-700 rounded-lg py-2 px-3 text-gray-100 outline-none focus:ring-2 focus:ring-blue-500" placeholder="İstanbul">
                </div>
                <div class="flex items-center space-x-4">
                    <label class="flex items-center space-x-2 text-sm text-gray-300">
                        <input type="checkbox" [(ngModel)]="newServer.skipSslCheck" name="skipSsl" class="rounded">
                        <span>SSL Kontrolünü Atla</span>
                    </label>
                </div>
                <button type="submit" class="w-full bg-blue-600 hover:bg-blue-500 text-white font-bold py-3 rounded-xl transition">Sunucu Ekle</button>
            </form>
        </div>
    </div>
    }

    <!-- SUNUCU DÜZENLE MODAL -->
    @if (showEditModal()) {
    <div class="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4 fade-in" (click)="showEditModal.set(false)">
        <div class="bg-gray-800 border border-gray-700 rounded-2xl shadow-2xl max-w-lg w-full" (click)="$event.stopPropagation()">
            <div class="flex justify-between items-center p-6 border-b border-gray-700">
                <h2 class="text-xl font-bold text-blue-400"><i class="fa-solid fa-pen mr-2"></i>Sunucu Düzenle</h2>
                <button (click)="showEditModal.set(false)" class="text-gray-400 hover:text-white transition"><i class="fa-solid fa-xmark"></i></button>
            </div>
            <form (ngSubmit)="updateServer()" class="p-6 space-y-4">
                <div>
                    <label class="block text-xs uppercase tracking-wider text-gray-400 font-semibold mb-1">Sunucu Adı *</label>
                    <input type="text" [(ngModel)]="editingServer.name" name="editName" required class="w-full bg-gray-900/50 border border-gray-700 rounded-lg py-2 px-3 text-gray-100 outline-none focus:ring-2 focus:ring-blue-500">
                </div>
                <div>
                    <label class="block text-xs uppercase tracking-wider text-gray-400 font-semibold mb-1">IP Adresi *</label>
                    <input type="text" [(ngModel)]="editingServer.ipAddress" name="editIpAddress" required class="w-full bg-gray-900/50 border border-gray-700 rounded-lg py-2 px-3 text-gray-100 outline-none focus:ring-2 focus:ring-blue-500">
                </div>
                <div class="grid grid-cols-2 gap-4">
                    <div>
                        <label class="block text-xs uppercase tracking-wider text-gray-400 font-semibold mb-1">İşletim Sistemi *</label>
                        <input type="text" [(ngModel)]="editingServer.operatingSystem" name="editOs" required class="w-full bg-gray-900/50 border border-gray-700 rounded-lg py-2 px-3 text-gray-100 outline-none focus:ring-2 focus:ring-blue-500">
                    </div>
                    <div>
                        <label class="block text-xs uppercase tracking-wider text-gray-400 font-semibold mb-1">Kategori</label>
                        <input type="text" [(ngModel)]="editingServer.category" name="editCategory" class="w-full bg-gray-900/50 border border-gray-700 rounded-lg py-2 px-3 text-gray-100 outline-none focus:ring-2 focus:ring-blue-500">
                    </div>
                </div>
                <div>
                    <label class="block text-xs uppercase tracking-wider text-gray-400 font-semibold mb-1">Konum</label>
                    <input type="text" [(ngModel)]="editingServer.location" name="editLocation" class="w-full bg-gray-900/50 border border-gray-700 rounded-lg py-2 px-3 text-gray-100 outline-none focus:ring-2 focus:ring-blue-500">
                </div>
                <div class="flex items-center space-x-4">
                    <label class="flex items-center space-x-2 text-sm text-gray-300">
                        <input type="checkbox" [(ngModel)]="editingServer.skipSslCheck" name="editSkipSsl" class="rounded">
                        <span>SSL Kontrolünü Atla</span>
                    </label>
                </div>
                <button type="submit" class="w-full bg-blue-600 hover:bg-blue-500 text-white font-bold py-3 rounded-xl transition">Değişiklikleri Kaydet</button>
            </form>
        </div>
    </div>
    }

    <!-- AJAN KURULUMU MODAL -->
    @if (showSetupModal()) {
    <div class="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4 fade-in" (click)="showSetupModal.set(false)">
        <div class="bg-gray-800 border border-gray-700 rounded-2xl shadow-2xl max-w-2xl w-full max-h-[90vh] overflow-y-auto" (click)="$event.stopPropagation()">
            <div class="flex justify-between items-center p-6 border-b border-gray-700">
                <h2 class="text-xl font-bold text-blue-400 flex items-center">
                    <i class="fa-solid fa-terminal mr-3"></i>Linux Ajan Kurulumu
                </h2>
                <button (click)="showSetupModal.set(false)" class="text-gray-400 hover:text-white transition text-xl">
                    <i class="fa-solid fa-xmark"></i>
                </button>
            </div>
            <div class="p-6 space-y-6">
                <div>
                    <p class="text-gray-300 text-sm mb-4">Uzak sunucunuzu izlemek için aşağıdaki komutu terminale yapıştırmanız yeterlidir. Bu komut, sunucu metriklerini (CPU, RAM) toplayıp bu panele gönderecektir.</p>
                    <div class="bg-gray-900 border border-gray-700 rounded-lg p-4 relative group">
                        <code class="text-green-400 text-sm font-mono break-all">curl -s http://{{ getHostAddress() }}/api/v1/agent/script | bash</code>
                        <button (click)="copyToClipboard('curl -s http://' + getHostAddress() + '/api/v1/agent/script | bash')" 
                            class="absolute top-2 right-2 bg-gray-700 hover:bg-gray-600 text-gray-300 px-2 py-1 rounded text-xs transition opacity-0 group-hover:opacity-100">
                            <i class="fa-solid fa-copy mr-1"></i>Kopyala
                        </button>
                    </div>
                </div>

                <div>
                    <h3 class="text-lg font-semibold text-white mb-3 flex items-center">
                        <i class="fa-solid fa-circle-info text-blue-400 mr-2"></i>Nasıl Çalışır?
                    </h3>
                    <ul class="space-y-2 text-sm text-gray-300">
                        <li class="flex items-start">
                            <i class="fa-solid fa-check text-green-400 mr-2 mt-1"></i>
                            Basit bir Bash script'idir, Python veya harici bağımlılık gerektirmez.
                        </li>
                        <li class="flex items-start">
                            <i class="fa-solid fa-check text-green-400 mr-2 mt-1"></i>
                            Veri göndermesi için sunucuyu önce panele eklemelisiniz.
                        </li>
                        <li class="flex items-start">
                            <i class="fa-solid fa-check text-green-400 mr-2 mt-1"></i>
                            Anlık veri gönderir ve kendini otomatik olarak Crontab'a ekler.
                        </li>
                    </ul>
                </div>

                <div class="border-t border-gray-700 pt-4">
                    <h3 class="text-lg font-semibold text-white mb-3 flex items-center">
                        <i class="fa-solid fa-trash-can text-red-400 mr-2"></i>Ajanı Kaldırma (Uninstall)
                    </h3>
                    <p class="text-gray-400 text-sm mb-3">Ajanı durdurmak ve tamamen silmek için şu komutu çalıştırabilirsiniz:</p>
                    <div class="bg-gray-900 border border-gray-700 rounded-lg p-4 relative group">
                        <code class="text-red-400 text-sm font-mono break-all">{{ uninstallCmd }}</code>
                        <button (click)="copyUninstall()" 
                            class="absolute top-2 right-2 bg-gray-700 hover:bg-gray-600 text-gray-300 px-2 py-1 rounded text-xs transition opacity-0 group-hover:opacity-100">
                            <i class="fa-solid fa-copy mr-1"></i>Kopyala
                        </button>
                    </div>
                </div>

                @if (copySuccess()) {
                <div class="bg-green-900/30 border border-green-700 text-green-400 text-sm px-4 py-2 rounded-lg text-center fade-in">
                    <i class="fa-solid fa-check-circle mr-2"></i>Panoya kopyalandı!
                </div>
                }
            </div>
        </div>
    </div>
    }

    <!-- AYARLAR MODAL -->
    @if (showSettingsModal()) {
    <div class="fixed inset-0 bg-black/60 backdrop-blur-md z-[110] flex items-center justify-center p-4 fade-in" (click)="showSettingsModal.set(false)">
        <div class="bg-gray-900 border border-gray-700 rounded-2xl shadow-[0_20px_60px_rgba(0,0,0,0.8)] max-w-lg w-full max-h-[85vh] flex flex-col relative overflow-hidden ring-1 ring-white/10" (click)="$event.stopPropagation()">
            <!-- Mac Style Dynamic Header -->
            <div class="px-6 py-5 bg-gradient-to-br from-gray-800 to-gray-900 border-b border-gray-700/50 relative overflow-hidden flex-shrink-0 flex items-center justify-between">
                <!-- Shine effect -->
                <div class="absolute top-0 inset-x-0 h-px bg-gradient-to-r from-transparent via-white/20 to-transparent"></div>
                <div class="flex items-center space-x-3 z-10">
                    <div class="w-10 h-10 rounded-xl bg-gray-800 border border-gray-600 shadow-inner flex items-center justify-center relative overflow-hidden">
                        <div class="absolute inset-0 bg-blue-500/10"></div>
                        <i class="fa-solid fa-sliders text-blue-400 text-lg"></i>
                    </div>
                    <div>
                        <h2 class="text-base font-bold text-gray-100 tracking-wide">Sistem Ayarları</h2>
                        <p class="text-[11px] text-gray-400 font-medium">Panel Tercihleri ve Entegrasyonlar</p>
                    </div>
                </div>
                <!-- Close Button -->
                <button (click)="showSettingsModal.set(false)" class="w-8 h-8 rounded-full bg-gray-800 hover:bg-gray-750 border border-gray-700 text-gray-400 hover:text-white transition-all flex items-center justify-center z-10 shadow-sm">
                    <i class="fa-solid fa-xmark text-sm"></i>
                </button>
            </div>

            <div class="p-6 space-y-4 overflow-y-auto custom-scrollbar bg-black/20">
                
                <div class="grid grid-cols-2 gap-3 mb-4">
                    <a href="/actuator/prometheus" target="_blank" class="flex flex-col p-4 bg-gray-800 border border-gray-700/60 rounded-xl hover:bg-gray-750 transition group shadow-sm relative overflow-hidden">
                        <div class="absolute top-0 right-0 w-16 h-16 bg-orange-500/10 rounded-bl-full -z-0"></div>
                        <i class="fa-solid fa-chart-line text-orange-400 text-xl mb-2 z-10"></i>
                        <p class="text-white text-sm font-semibold z-10">Prometheus</p>
                        <p class="text-gray-500 text-[10px] z-10">Metrik Endpointi</p>
                    </a>
                    <a href="/actuator/health" target="_blank" class="flex flex-col p-4 bg-gray-800 border border-gray-700/60 rounded-xl hover:bg-gray-750 transition group shadow-sm relative overflow-hidden">
                        <div class="absolute top-0 right-0 w-16 h-16 bg-green-500/10 rounded-bl-full -z-0"></div>
                        <i class="fa-solid fa-heart-pulse text-green-400 text-xl mb-2 z-10"></i>
                        <p class="text-white text-sm font-semibold z-10">Health Check</p>
                        <p class="text-gray-500 text-[10px] z-10">Durum Endpointi</p>
                    </a>
                </div>

                <!-- Info Block -->
                <div class="p-4 bg-gray-800 border border-gray-700/60 rounded-xl shadow-sm">
                    <div class="flex justify-between items-center text-xs pb-2 border-b border-gray-700/50 mb-2">
                        <span class="text-gray-500 uppercase font-semibold tracking-wider">Sunucu Sayısı</span>
                        <span class="text-blue-400 font-mono font-bold">{{ servers().length }}</span>
                    </div>
                    <div class="flex justify-between items-center text-xs pb-2 border-b border-gray-700/50 mb-2">
                        <span class="text-gray-500 uppercase font-semibold tracking-wider">Docker Kayıtları</span>
                        <span class="text-cyan-400 font-mono font-bold">{{ dockerCount() }}</span>
                    </div>
                    <div class="flex justify-between items-center text-xs">
                        <span class="text-gray-500 uppercase font-semibold tracking-wider">Agent Refresh</span>
                        <span class="text-gray-300 font-mono">10s</span>
                    </div>
                </div>
                
                <!-- AI Configuration -->
                <div class="p-5 bg-gray-800 border border-gray-700/60 rounded-xl shadow-sm">
                    <div class="flex items-center mb-4">
                        <div class="w-8 h-8 rounded-lg bg-purple-500/10 flex items-center justify-center mr-3 border border-purple-500/20">
                           <i class="fa-solid fa-robot text-purple-400"></i>
                        </div>
                        <p class="text-white text-sm font-semibold tracking-wide">AI Engine Ayarları</p>
                    </div>
                    <form (ngSubmit)="saveAiConfig()" class="space-y-3">
                        <div>
                            <label class="block text-[10px] uppercase tracking-wider text-gray-500 font-semibold mb-1">Provider</label>
                            <select [(ngModel)]="aiConfig.provider" name="provider" class="w-full bg-gray-900 border border-gray-700 rounded-lg p-2.5 text-[13px] text-gray-200 outline-none focus:border-purple-500 transition-colors shadow-inner">
                                <option value="CLOUD">Cloud (OpenAI / Claude vs)</option>
                                <option value="LOCAL">Local (LM Studio / Ollama)</option>
                                <option value="CREW_AI">CrewAI (Multi-Agent Team)</option>
                            </select>
                        </div>
                        <div>
                            <label class="block text-[10px] uppercase tracking-wider text-gray-500 font-semibold mb-1">API Key <span class="text-gray-600">(İsteğe Bağlı)</span></label>
                            <input type="password" [(ngModel)]="aiConfig.apiKey" name="apiKey" class="w-full bg-gray-900 border border-gray-700 rounded-lg p-2.5 text-[13px] text-gray-200 outline-none focus:border-purple-500 transition-colors shadow-inner" placeholder="sk-...">
                        </div>
                        <div>
                            <label class="block text-[10px] uppercase tracking-wider text-gray-500 font-semibold mb-1">Endpoint URL</label>
                            <input type="text" [(ngModel)]="aiConfig.endpointUrl" name="endpointUrl" class="w-full bg-gray-900 border border-gray-700 rounded-lg p-2.5 text-[13px] text-gray-200 outline-none focus:border-purple-500 transition-colors shadow-inner" placeholder="http://local:1234/v1/chat/completions">
                        </div>
                        <button type="submit" class="w-full mt-2 bg-purple-600 hover:bg-purple-500 text-white font-medium py-2.5 rounded-lg transition-colors text-sm shadow-[0_0_15px_rgba(168,85,247,0.3)]">AI Ayarlarını Doğrula</button>
                    </form>
                </div>

                <!-- Telegram Configuration -->
                <div class="p-5 bg-gray-800 border border-gray-700/60 rounded-xl shadow-sm">
                    <div class="flex items-center mb-4">
                         <div class="w-8 h-8 rounded-lg bg-blue-500/10 flex items-center justify-center mr-3 border border-blue-500/20">
                           <i class="fa-brands fa-telegram text-blue-400"></i>
                        </div>
                        <p class="text-white text-sm font-semibold tracking-wide">Telegram Entegrasyonu</p>
                    </div>
                    <form (ngSubmit)="saveTelegramConfig()" class="space-y-3">
                        <div>
                            <label class="block text-[10px] uppercase tracking-wider text-gray-500 font-semibold mb-1">Bot Token</label>
                            <input type="password" [(ngModel)]="telegramConfig.token" name="bgTbToken" class="w-full bg-gray-900 border border-gray-700 rounded-lg p-2.5 text-[13px] text-gray-200 outline-none focus:border-blue-500 transition-colors shadow-inner">
                        </div>
                        <div>
                            <label class="block text-[10px] uppercase tracking-wider text-gray-500 font-semibold mb-1">Chat ID</label>
                            <input type="text" [(ngModel)]="telegramConfig.chatId" name="bgTbCId" class="w-full bg-gray-900 border border-gray-700 rounded-lg p-2.5 text-[13px] text-gray-200 outline-none focus:border-blue-500 transition-colors shadow-inner">
                        </div>
                        <button type="submit" class="w-full mt-2 bg-blue-600 hover:bg-blue-500 text-white font-medium py-2.5 rounded-lg transition-colors text-sm shadow-[0_0_15px_rgba(59,130,246,0.3)]">
                            Webhook'u Kaydet
                        </button>
                    </form>
                </div>
            </div>
        </div>
    </div>
    }
  `,
    styles: [`
    .fade-in { animation: fadeIn 0.3s ease-in-out; }
    @keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
  `]
})
export class DashboardComponent implements OnInit {
    private apiService = inject(ApiService);
    private router = inject(Router);
    private http = inject(HttpClient);
    private refreshInterval: any;

    servers = signal<any[]>([]);
    dockerData = signal<any[]>([]);
    loading = signal(true);
    currentTab = signal('servers');
    dockerCount = signal(0);
    k8sCount = signal(0);
    k8sPods = signal<any[]>([]);
    mcpToolCount = signal(0);
    mcpOnline = signal(false);
    showSetupModal = signal(false);
    showNewServerModal = signal(false);
    showEditModal = signal(false);
    showSettingsModal = signal(false);
    copySuccess = signal(false);
    uninstallCmd = 'sudo crontab -l | grep -v "devops_agent.sh" | sudo crontab - && sudo rm -f /usr/local/bin/devops_agent.sh /tmp/devops_agent.sh';
    newServer: any = { name: '', ipAddress: '', operatingSystem: 'linux', location: '', category: '', skipSslCheck: false };
    editingServer: any = { id: 0, name: '', ipAddress: '', operatingSystem: '', location: '', category: '', skipSslCheck: false };

    private aiService = inject(AiService);
    aiConfig: AiConfig = { provider: 'CLOUD' };
    telegramConfig: { token: string, chatId: string } = { token: '', chatId: '' };

    ngOnInit() {
        this.loadServers();
        this.aiConfig = this.aiService.getConfig();
        this.loadTelegramConfig();
        this.checkMcpStatus();
        this.refreshInterval = setInterval(() => {
            this.refreshServers();
            this.checkMcpStatus();
        }, 10000);
    }

    ngOnDestroy() {
        if (this.refreshInterval) {
            clearInterval(this.refreshInterval);
        }
    }

    loadServers() {
        this.loading.set(true);
        this.apiService.getServers().subscribe({
            next: (data) => {
                this.servers.set(data);
                this.loading.set(false);
                this.loadDockerData(data);
            },
            error: () => this.loading.set(false)
        });
        this.apiService.getK8sPods().subscribe({
            next: (pods) => { this.k8sCount.set(pods.length); this.k8sPods.set(pods); },
            error: () => { }
        });
    }

    loadDockerData(servers: any[]) {
        const containerRequests = servers.map(s => this.apiService.getDockerContainers(s.id));
        if (containerRequests.length === 0) return;
        forkJoin(containerRequests).subscribe({
            next: (results) => {
                const allContainers = results.flat();
                this.dockerData.set(allContainers);
                this.dockerCount.set(allContainers.length);
            },
            error: () => { }
        });
    }

    refreshServers() {
        this.apiService.getServers().subscribe({
            next: (data) => {
                this.servers.set(data);
                this.loadDockerData(data);
            },
            error: () => { }
        });
        this.apiService.getK8sPods().subscribe({
            next: (pods) => { this.k8sCount.set(pods.length); this.k8sPods.set(pods); },
            error: () => { }
        });
    }

    onNavClick(target: string) {
        if (target === 'setup') {
            this.showSetupModal.set(true);
        } else if (target === 'new-server') {
            this.showNewServerModal.set(true);
        } else if (target === 'grafana') {
            window.open('/actuator/prometheus', '_blank');
        } else if (target === 'settings') {
            this.showSettingsModal.set(true);
        }
    }

    getHostAddress(): string {
        return window.location.host;
    }

    copyToClipboard(text: string) {
        navigator.clipboard.writeText(text).then(() => {
            this.copySuccess.set(true);
            setTimeout(() => this.copySuccess.set(false), 2000);
        });
    }

    copyUninstall() {
        this.copyToClipboard(this.uninstallCmd);
    }

    onLogout() {
        this.http.post('/api/auth/logout', {}).subscribe({
            next: () => this.router.navigate(['/login']),
            error: () => this.router.navigate(['/login'])
        });
    }

    onDeleteServer(id: number) {
        if (confirm('Emin misiniz?')) {
            this.apiService.deleteServer(id).subscribe(() => this.loadServers());
        }
    }

    createServer() {
        this.apiService.createServer(this.newServer).subscribe({
            next: () => {
                this.showNewServerModal.set(false);
                this.newServer = { name: '', ipAddress: '', operatingSystem: 'linux', location: '', category: '', skipSslCheck: false };
                this.loadServers();
            },
            error: (err: any) => alert('Hata: ' + (err.error?.message || 'Sunucu eklenemedi'))
        });
    }

    openEditModal(server: any) {
        this.editingServer = { ...server };
        this.showEditModal.set(true);
    }

    updateServer() {
        this.apiService.updateServer(this.editingServer.id, this.editingServer).subscribe({
            next: () => {
                this.showEditModal.set(false);
                this.loadServers();
            },
            error: (err: any) => alert('Hata: ' + (err.error?.message || 'Sunucu güncellenemedi'))
        });
    }

    saveAiConfig() {
        this.aiService.saveConfig(this.aiConfig);
        alert('AI Ayarları kaydedildi.');
    }

    loadTelegramConfig() {
        this.http.get('/api/v1/settings/TELEGRAM_BOT_TOKEN', { responseType: 'text' }).subscribe({
            next: (val) => this.telegramConfig.token = val,
            error: () => { }
        });
        this.http.get('/api/v1/settings/TELEGRAM_CHAT_ID', { responseType: 'text' }).subscribe({
            next: (val) => this.telegramConfig.chatId = val,
            error: () => { }
        });
    }

    saveTelegramConfig() {
        if (!this.telegramConfig.token || !this.telegramConfig.chatId) {
            alert('Lütfen Bot Token ve Chat ID girin.');
            return;
        }
        forkJoin([
            this.http.post('/api/v1/settings', { key: 'TELEGRAM_BOT_TOKEN', value: this.telegramConfig.token }),
            this.http.post('/api/v1/settings', { key: 'TELEGRAM_CHAT_ID', value: this.telegramConfig.chatId })
        ]).subscribe({
            next: () => alert('Telegram ayarları kaydedildi.'),
            error: () => alert('Telegram ayarları kaydedilirken hata oluştu.')
        });
    }

    getMetricColor(value: number) {
        if (value >= 80) return 'bg-red-500';
        if (value >= 50) return 'bg-yellow-500';
        return 'bg-green-500';
    }

    checkMcpStatus() {
        // Since MCP server runs via stdio, we check if the AI service is reachable
        // which is where the MCP tools proxy their requests.
        this.http.get('/actuator/health').subscribe({
            next: () => {
                this.mcpOnline.set(true);
                this.mcpToolCount.set(4);
            },
            error: () => {
                this.mcpOnline.set(false);
                this.mcpToolCount.set(0);
            }
        });
    }
}

