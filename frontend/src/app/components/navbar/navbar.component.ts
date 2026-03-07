import { Component, output } from '@angular/core';

@Component({
    selector: 'app-navbar',
    imports: [],
    template: `
    <nav class="bg-gray-800 border-b border-gray-700 p-4 sticky top-0 z-50 shadow-lg">
        <div class="container mx-auto flex justify-between items-center">
            <h1 class="text-xl font-bold text-blue-400 flex items-center cursor-pointer" (click)="onNavClick('servers')">
                <i class="fa-solid fa-server mr-2"></i>
                DevOps<span class="text-white">Dashboard</span>
            </h1>
            <div class="flex items-center space-x-3">
                <button (click)="onNavClick('setup')"
                    class="bg-indigo-600 hover:bg-indigo-700 text-white px-3 py-2 rounded shadow transition text-sm flex items-center">
                    <i class="fa-solid fa-terminal mr-2"></i> Ajan Kurulumu
                </button>
                <a href="/actuator/prometheus" target="_blank"
                    class="bg-orange-600/20 hover:bg-orange-600/40 text-orange-400 px-3 py-2 rounded text-sm transition flex items-center"
                    title="Prometheus Metrikleri">
                    <i class="fa-solid fa-chart-line mr-2"></i> Grafana/Prom
                </a>
                <button (click)="onNavClick('settings')"
                    class="bg-gray-700 hover:bg-gray-600 px-3 py-2 rounded text-sm text-gray-300 transition"
                    title="Ayarlar">
                    <i class="fa-solid fa-gear"></i>
                </button>
                <button (click)="onNavClick('new-server')"
                    class="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded shadow transition text-sm flex items-center">
                    <i class="fa-solid fa-plus mr-2"></i> <span class="hidden md:inline">Yeni Sunucu</span>
                </button>
                <button (click)="onLogout()"
                    class="bg-red-600/20 hover:bg-red-600/40 text-red-400 px-3 py-2 rounded text-sm transition"
                    title="Çıkış Yap">
                    <i class="fa-solid fa-right-from-bracket"></i>
                </button>
            </div>
        </div>
    </nav>
  `,
    styles: []
})
export class NavbarComponent {
    navClick = output<string>();
    logout = output<void>();

    onNavClick(target: string) {
        this.navClick.emit(target);
    }

    onLogout() {
        this.logout.emit();
    }
}
