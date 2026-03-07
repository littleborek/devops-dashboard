import { Routes } from '@angular/router';
import { LoginComponent } from './components/login/login.component';
import { DashboardComponent } from './components/dashboard/dashboard.component';

export const routes: Routes = [
    { path: 'login', component: LoginComponent },
    { path: '', component: DashboardComponent },
    { path: 'server/:id', loadComponent: () => import('./components/server-detail/server-detail.component').then(m => m.ServerDetailComponent) },
    { path: '**', redirectTo: '' }
];
