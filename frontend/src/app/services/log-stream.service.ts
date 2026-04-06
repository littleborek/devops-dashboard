import { Injectable } from '@angular/core';
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
    private logsSubject = new Subject<LogMessage>();
    public logs$: Observable<LogMessage> = this.logsSubject.asObservable();
    private eventSource: EventSource | null = null;

    connect(serverId: number): void {
        this.disconnect();
        
        // Backend'deki SSE (Server-Sent Events) endpoint'ine bağlanıyoruz
        this.eventSource = new EventSource(`/api/v1/agent/logs/stream/${serverId}`);
        
        this.eventSource.onmessage = (event) => {
            try {
                const log: LogMessage = JSON.parse(event.data);
                this.logsSubject.next(log);
            } catch (e) {
                console.error('Log parse error:', e);
            }
        };

        this.eventSource.onerror = (error) => {
            console.error('EventSource failed:', error);
            this.disconnect();
        };
    }

    disconnect(): void {
        if (this.eventSource) {
            this.eventSource.close();
            this.eventSource = null;
        }
    }
}
