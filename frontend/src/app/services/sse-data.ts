export async function* readSseData(body: ReadableStream<Uint8Array>): AsyncGenerator<string> {
    const reader = body.getReader();
    const decoder = new TextDecoder();
    let pending = '';
    let data: string[] = [];

    try {
        while (true) {
            const { done, value } = await reader.read();
            if (done) break;
            pending += decoder.decode(value, { stream: true });

            let newline: number;
            while ((newline = pending.indexOf('\n')) !== -1) {
                const line = pending.slice(0, newline).replace(/\r$/, '');
                pending = pending.slice(newline + 1);
                if (line === '') {
                    if (data.length) yield data.join('\n');
                    data = [];
                } else if (line === 'data' || line.startsWith('data:')) {
                    data.push(line.slice(5).replace(/^ /, ''));
                }
            }
        }
    } finally {
        try {
            await reader.cancel();
        } finally {
            reader.releaseLock();
        }
    }
}
