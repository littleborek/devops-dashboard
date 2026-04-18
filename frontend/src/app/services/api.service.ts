import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private http = inject(HttpClient);
  private baseUrl = '/api/v1';

  getServers(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/servers`);
  }

  getServer(id: number): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/servers/${id}`);
  }

  getServerHistory(id: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/servers/${id}/history`);
  }

  getContainers(serverId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/servers/${serverId}/containers`);
  }

  getContainerLogs(serverId: number, containerId: string): Observable<string> {
    return this.http.get(`${this.baseUrl}/servers/${serverId}/logs`, {
      params: { containerId },
      responseType: 'text'
    });
  }

  getDeployments(serverId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/deployments/server/${serverId}`);
  }

  createServer(server: any): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/servers`, server);
  }

  deleteServer(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/servers/${id}`);
  }

  updateServer(id: number, server: any): Observable<any> {
    return this.http.put<any>(`${this.baseUrl}/servers/${id}`, server);
  }

  getDockerContainers(serverId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/servers/${serverId}/containers`);
  }

  getK8sPods(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/k8s/pods`);
  }

  queueTask(serverId: number, command: string): Observable<{ taskId: number }> {
    return this.http.post<{ taskId: number }>(`${this.baseUrl}/tasks/queue/${serverId}`, { command });
  }

  getTaskResult(taskId: number): Observable<{ id: number, status: string, result: string }> {
    return this.http.get<{ id: number, status: string, result: string }>(`${this.baseUrl}/tasks/result/${taskId}`);
  }
}
