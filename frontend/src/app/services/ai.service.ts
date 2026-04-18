import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface AiConfig {
    provider: 'LOCAL' | 'CLOUD' | 'CREW_AI';
    apiKey?: string;
    endpointUrl?: string;
}

@Injectable({
    providedIn: 'root'
})
export class AiService {
    private http = inject(HttpClient);
    private baseUrl = '/api/v1/ai';

    private readonly CONFIG_KEY = 'ai_ops_config';

    getConfig(): AiConfig {
        const stored = localStorage.getItem(this.CONFIG_KEY);
        if (stored) {
            return JSON.parse(stored) as AiConfig;
        }
        return { provider: 'CLOUD' }; // default
    }

    saveConfig(config: AiConfig): void {
        localStorage.setItem(this.CONFIG_KEY, JSON.stringify(config));
    }

    private getHeaders(): HttpHeaders {
        const config = this.getConfig();
        let headers = new HttpHeaders({
            'X-AI-Provider': config.provider
        });
        if (config.apiKey) {
            headers = headers.set('X-AI-Key', config.apiKey);
        }
        if (config.endpointUrl) {
            headers = headers.set('X-AI-Endpoint', config.endpointUrl);
        }
        return headers;
    }

    analyzeIssue(serverId: number, eventContext: string, recentLogs: string): Observable<{ analysis: string }> {
        const headers = this.getHeaders();
        return this.http.post<{ analysis: string }>(`${this.baseUrl}/analyze`, {
            serverId,
            eventContext,
            recentLogs
        }, { headers });
    }

    chatQuery(query: string): Observable<{ response: string }> {
        const headers = this.getHeaders();
        return this.http.post<{ response: string }>(`${this.baseUrl}/chat`, { query }, { headers });
    }

    async *chatQueryStream(query: string): AsyncGenerator<string, void, unknown> {
        const config = this.getConfig();
        const headers: Record<string, string> = {
            'Content-Type': 'application/json',
            'X-AI-Provider': config.provider
        };
        if (config.apiKey) headers['X-AI-Key'] = config.apiKey;
        if (config.endpointUrl) headers['X-AI-Endpoint'] = config.endpointUrl;

        const response = await fetch(`${this.baseUrl}/chat/stream`, {
            method: 'POST',
            headers,
            body: JSON.stringify({ query })
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const reader = response.body?.getReader();
        const decoder = new TextDecoder();

        if (!reader) return;

        while (true) {
            const { value, done } = await reader.read();
            if (done) break;

            const decoded = decoder.decode(value, { stream: true });

            // Reconstruct potentially fragmented SSE frames
            const lines = decoded.split('\n');
            for (const line of lines) {
                const trimmedLine = line.trim();
                if (!trimmedLine) continue;

                if (trimmedLine.startsWith('data:')) {
                    yield trimmedLine.replace('data:', '').trim();
                } else {
                    // If it's not standard SSE format but we got text, yield it anyway
                    yield trimmedLine;
                }
            }
        }
    }
}
