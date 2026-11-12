import { Injectable, signal } from '@angular/core';
import { Subject, Observable } from 'rxjs';

export interface LogMessage {
    serverId: string;
    content: string;
    level: string;
    timestamp: string;
}

@Injectable({
    providedIn: 'root'
})
export class LogStreamService {
    private readonly status = signal<'idle' | 'connecting' | 'open' | 'error'>('idle');
    readonly connectionStatus = this.status.asReadonly();
    private logsSubject = new Subject<LogMessage>();
    public logs$: Observable<LogMessage> = this.logsSubject.asObservable();
    private eventSource: EventSource | null = null;

    connect(serverId: number): void {
        this.disconnect();
        this.status.set('connecting');
        let source: EventSource;
        try { source = new EventSource(`/api/v1/agent/logs/stream/${serverId}`); }
        catch { this.status.set('error'); return; }
        this.eventSource = source;

        source.onopen = () => {
            if (this.eventSource === source) this.status.set('open');
        };
        source.onmessage = (event) => {
            if (this.eventSource !== source) return;
            try {
                const log: LogMessage = JSON.parse(event.data);
                this.logsSubject.next(log);
            } catch (e) {
                console.error('Log parse error:', e);
            }
        };

        source.onerror = () => {
            if (this.eventSource !== source) return;
            this.disconnect();
            this.status.set('error');
        };
    }

    disconnect(): void {
        if (this.eventSource) {
            this.eventSource.close();
            this.eventSource = null;
        }
        this.status.set('idle');
    }
}
