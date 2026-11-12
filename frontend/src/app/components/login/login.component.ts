import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { UiService } from '../../services/ui.service';
import { PreferencesComponent } from '../preferences/preferences.component';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, PreferencesComponent],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  readonly ui = inject(UiService);
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  username = '';
  password = '';
  error = signal<'credentials' | 'connection' | null>(null);
  isLoading = signal(false);

  onLogin(): void {
    if (this.isLoading() || !this.username.trim() || !this.password) return;
    this.isLoading.set(true);
    this.error.set(null);
    const formData = new URLSearchParams({ username: this.username.trim(), password: this.password });
    this.http.post('/api/auth/login', formData.toString(), {
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      responseType: 'text',
      observe: 'response'
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => { this.isLoading.set(false); this.router.navigate(['/']); },
      error: err => { this.isLoading.set(false); this.error.set(err.status === 401 ? 'credentials' : 'connection'); }
    });
  }
}
