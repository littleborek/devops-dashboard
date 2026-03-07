import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';

@Component({
    selector: 'app-login',
    standalone: true,
    imports: [CommonModule, FormsModule],
    template: `
    <div class="absolute top-0 left-0 w-full h-full overflow-hidden z-0 bg-[#0f172a]">
        <div class="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] bg-blue-600/20 rounded-full blur-[120px]"></div>
        <div class="absolute bottom-[-10%] right-[-10%] w-[40%] h-[40%] bg-purple-600/10 rounded-full blur-[120px]"></div>
    </div>

    <div class="flex items-center justify-center min-h-screen relative z-10">
      <div class="w-full max-w-md p-8 bg-gray-800/70 backdrop-blur-xl border border-gray-700/50 rounded-2xl shadow-2xl fade-in">
          <div class="text-center mb-10">
              <h1 class="text-3xl font-bold text-blue-400 flex justify-center items-center mb-2">
                  <i class="fa-solid fa-server mr-3"></i>
                  DevOps<span class="text-white">Dashboard</span>
              </h1>
              <p class="text-gray-400 text-sm">Altyapını yönetmeye başlamak için giriş yap</p>
          </div>

          @if (errorMessage()) {
              <div class="mb-4 p-3 bg-red-900/30 border border-red-700 rounded-lg text-red-400 text-sm text-center">
                  <i class="fa-solid fa-circle-exclamation mr-2"></i>{{ errorMessage() }}
              </div>
          }

          <form (ngSubmit)="onLogin()" class="space-y-6">
              <div>
                  <label class="block text-xs uppercase tracking-wider text-gray-400 font-semibold mb-2 ml-1">Kullanıcı Adı</label>
                  <div class="relative">
                      <span class="absolute inset-y-0 left-0 pl-3 flex items-center text-gray-500">
                          <i class="fa-solid fa-user"></i>
                      </span>
                      <input type="text" [(ngModel)]="username" name="username" required autocomplete="username"
                          class="w-full bg-gray-900/50 border border-gray-700 rounded-xl py-3 pl-10 pr-4 focus:ring-2 focus:ring-blue-500 focus:border-transparent transition outline-none text-gray-100"
                          placeholder="admin">
                  </div>
              </div>

              <div>
                  <label class="block text-xs uppercase tracking-wider text-gray-400 font-semibold mb-2 ml-1">Şifre</label>
                  <div class="relative">
                      <span class="absolute inset-y-0 left-0 pl-3 flex items-center text-gray-500">
                          <i class="fa-solid fa-lock"></i>
                      </span>
                      <input type="password" [(ngModel)]="password" name="password" required autocomplete="current-password"
                          class="w-full bg-gray-900/50 border border-gray-700 rounded-xl py-3 pl-10 pr-4 focus:ring-2 focus:ring-blue-500 focus:border-transparent transition outline-none text-gray-100"
                          placeholder="••••••••">
                  </div>
              </div>

              <button type="submit" [disabled]="isLoading()"
                  class="w-full bg-blue-600 hover:bg-blue-500 text-white font-bold py-3 px-4 rounded-xl shadow-lg shadow-blue-600/20 transform transition hover:-translate-y-0.5 active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed">
                  @if (isLoading()) {
                      <i class="fa-solid fa-circle-notch fa-spin mr-2"></i>Giriş yapılıyor...
                  } @else {
                      Giriş Yap
                  }
              </button>
          </form>

          <div class="mt-8 pt-6 border-t border-gray-800 text-center">
              <p class="text-xs text-gray-500">
                  &copy; 2026 DevOps Dashboard. Tüm hakları saklıdır.
              </p>
          </div>
      </div>
    </div>
  `,
    styles: [`
    .fade-in { animation: fadeIn 0.5s ease-out; }
    @keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
  `]
})
export class LoginComponent {
    username = '';
    password = '';
    errorMessage = signal('');
    isLoading = signal(false);

    constructor(private http: HttpClient, private router: Router) { }

    onLogin() {
        this.isLoading.set(true);
        this.errorMessage.set('');

        const formData = new URLSearchParams();
        formData.set('username', this.username);
        formData.set('password', this.password);

        this.http.post('/api/auth/login', formData.toString(), {
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            responseType: 'text',
            observe: 'response'
        }).subscribe({
            next: () => {
                this.router.navigate(['/']);
            },
            error: (err) => {
                this.isLoading.set(false);
                if (err.status === 401) {
                    this.errorMessage.set('Kullanıcı adı veya şifre hatalı!');
                } else {
                    this.errorMessage.set('Bağlantı hatası. Lütfen tekrar deneyin.');
                }
            }
        });
    }
}
