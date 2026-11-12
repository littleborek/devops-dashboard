import assert from 'node:assert/strict';
import test from 'node:test';
import { metricValue, serverProblems } from '../src/app/components/dashboard/dashboard-metrics.ts';

test('only valid percentage data is displayed, including a real zero', () => {
    assert.equal(metricValue(0), 0);
    assert.equal(metricValue(100), 100);
    for (const value of [null, undefined, '42', NaN, Infinity, -1, 101]) {
        assert.equal(metricValue(value), null);
    }
});

test('offline servers report only connectivity and unknown servers report no inferred problems', () => {
    const reading = { cpuUsage: 100, cpuUsageThreshold: 20 };
    assert.deepEqual(serverProblems({ status: 'OFFLINE', ...reading }), ['offline']);
    assert.deepEqual(serverProblems({ status: 'UNKNOWN', ...reading }), []);
});

test('online usage must strictly exceed its own positive configured threshold', () => {
    const reading = { status: 'ONLINE', cpuUsage: 75, ramUsage: 92 };
    assert.deepEqual(serverProblems(reading), []);
    assert.deepEqual(serverProblems({ ...reading, cpuUsageThreshold: 74, ramUsageThreshold: 90 }), ['cpu', 'ram']);
    assert.deepEqual(serverProblems({ ...reading, cpuUsageThreshold: 75, ramUsageThreshold: 0 }), []);
    assert.deepEqual(serverProblems({ ...reading, cpuUsage: null, cpuUsageThreshold: 10, ramUsageThreshold: 91 }), ['ram']);
});
