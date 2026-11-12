import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { firstValueFrom, fromEvent, Observable, takeUntil } from 'rxjs';
import { readSseData } from './sse-data';

export interface AiConfig {
    provider: 'LOCAL' | 'CLOUD' | 'CREW_AI';
    apiKey?: string;
    endpointUrl?: string;
}

export interface AiServerContext { id: number; name: string; context: string }

@Injectable({
    providedIn: 'root'
})
export class AiService {
    readonly serverContext = signal<AiServerContext | null>(null);
    readonly panelOpen = signal(false);

    setServerContext(context: AiServerContext | null): void {
        this.serverContext.update(current =>
            current?.id === context?.id && current?.name === context?.name && current?.context === context?.context
                ? current : context
        );
    }

    openForServer(context: AiServerContext): void {
        this.setServerContext(context);
        this.panelOpen.set(true);
    }
    private http = inject(HttpClient);
    private baseUrl = '/api/v1/ai';

    private readonly CONFIG_KEY = 'ai_ops_config';

    getConfig(): AiConfig {
        try {
            const config = JSON.parse(localStorage.getItem(this.CONFIG_KEY) ?? 'null');
            if (config && ['LOCAL', 'CLOUD', 'CREW_AI'].includes(config.provider)) {
                const header = (value: unknown) => typeof value === 'string' && /^[\x20-\x7e]*$/.test(value) ? value : undefined;
                return { provider: config.provider, apiKey: header(config.apiKey), endpointUrl: header(config.endpointUrl) };
            }
        } catch { /* Storage can be corrupt or disabled. */ }
        return { provider: 'CLOUD' };
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

    async *chatQueryStream(query: string, signal: AbortSignal): AsyncGenerator<string, void, unknown> {
        signal.throwIfAborted();
        const config = this.getConfig();
        if (config.provider === 'CREW_AI') {
            const { response } = await firstValueFrom(this.chatQuery(query).pipe(takeUntil(fromEvent(signal, 'abort'))));
            yield response;
            return;
        }
        const headers: Record<string, string> = {
            'Content-Type': 'application/json',
            'X-AI-Provider': config.provider
        };
        if (config.apiKey) headers['X-AI-Key'] = config.apiKey;
        if (config.endpointUrl) headers['X-AI-Endpoint'] = config.endpointUrl;

        const response = await fetch(`${this.baseUrl}/chat/stream`, {
            method: 'POST',
            signal,
            headers,
            body: JSON.stringify({ query })
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        if (!response.body) throw new Error('Missing chat response body');
        for await (const chunk of readSseData(response.body)) {
            if (chunk.startsWith('[STREAM_ERROR]')) throw new Error('AI provider stream failed');
            yield chunk;
        }
    }
}
