import assert from 'node:assert/strict';
import { test } from 'node:test';
import { formatAiReply } from '../src/app/services/ai-format.ts';

test('separates closed code fences without interpreting HTML', () => {
    assert.deepEqual(formatAiReply('<img src=x>\n```bash\r\necho "<script>"\r\n```\r\nend'), [
        { text: '<img src=x>\n', code: false },
        { text: 'echo "<script>"', code: true },
        { text: '\nend', code: false },
    ]);
});

test('does not expose copy for incomplete streaming fences', () => {
    const partial = 'Try:\n```sh\nls -l';
    assert.deepEqual(formatAiReply(partial), [{ text: partial, code: false }]);
});
