export function metricValue(value: unknown): number | null {
    return typeof value === 'number' && Number.isFinite(value) && value >= 0 && value <= 100 ? value : null;
}

export function exceedsThreshold(value: unknown, threshold: unknown): boolean {
    const usage = metricValue(value);
    const limit = metricValue(threshold);
    return usage !== null && limit !== null && limit > 0 && usage > limit;
}

export function serverProblems(server: {
    status?: string;
    telemetryStatus?: string;
    cpuUsage?: unknown;
    ramUsage?: unknown;
    cpuUsageThreshold?: unknown;
    ramUsageThreshold?: unknown;
}): ('offline' | 'cpu' | 'ram')[] {
    if (server.status === 'OFFLINE') return ['offline'];
    if (server.status !== 'ONLINE' || (server.telemetryStatus && server.telemetryStatus !== 'FRESH')) return [];
    const result: ('offline' | 'cpu' | 'ram')[] = [];
    if (exceedsThreshold(server.cpuUsage, server.cpuUsageThreshold)) result.push('cpu');
    if (exceedsThreshold(server.ramUsage, server.ramUsageThreshold)) result.push('ram');
    return result;
}
