import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { createRequire } from 'node:module';
import { test } from 'node:test';
import ts from 'typescript';

const require = createRequire(import.meta.url);
const source = readFileSync(new URL('../src/app/services/log-stream.service.ts', import.meta.url), 'utf8');
const { outputText } = ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022, experimentalDecorators: true },
});
test('log connection distinguishes failure, reconnect, and disconnect and ignores stale events', () => {
    const sources = [];
    const EventSource = class {
        constructor(url) { this.url = url; sources.push(this); }
        close() { this.closed = true; }
    };
    const module = { exports: {} };
    new Function('require', 'exports', 'EventSource', outputText)(require, module.exports, EventSource);
    const { LogStreamService } = module.exports;
    const service = new LogStreamService();
    assert.equal(service.connectionStatus(), 'idle');
    service.connect(7);
    const [first] = sources;
    assert.equal(first.url, '/api/v1/agent/logs/stream/7');
    assert.equal(service.connectionStatus(), 'connecting');
    first.onerror();
    assert.equal(first.closed, true);
    assert.equal(service.connectionStatus(), 'error');
    service.connect(7);
    const second = sources[1];
    first.onopen();
    assert.equal(service.connectionStatus(), 'connecting');
    second.onopen();
    assert.equal(service.connectionStatus(), 'open');
    first.onerror();
    assert.equal(service.connectionStatus(), 'open');
    service.disconnect();
    assert.equal(second.closed, true);
    assert.equal(service.connectionStatus(), 'idle');
});
