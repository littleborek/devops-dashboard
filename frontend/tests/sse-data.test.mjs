import assert from 'node:assert/strict';
import { test } from 'node:test';
import { readSseData } from '../src/app/services/sse-data.ts';

test('preserves multiline SSE data split across network and UTF-8 boundaries', async () => {
    const input = new TextEncoder().encode(
        ': heartbeat\r\nevent: message\r\ndata:  first\r\ndata: ü\r\n\r\ndata:\n\n'
        + 'data: last\n\ndata: incomplete\n',
    );
    const stream = new ReadableStream({
        start(controller) {
            for (const byte of input) controller.enqueue(Uint8Array.of(byte));
            controller.close();
        },
    });

    const events = [];
    for await (const data of readSseData(stream)) events.push(data);
    assert.deepEqual(events, [' first\nü', '', 'last']);
    assert.equal(stream.locked, false);
});
