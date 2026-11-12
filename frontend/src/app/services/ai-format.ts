export function formatAiReply(reply: string): { text: string; code: boolean }[] {
    const parts: { text: string; code: boolean }[] = [];
    const fences = /^ {0,3}```[\w+-]*[ \t]*\r?\n([\s\S]*?)^ {0,3}```[ \t]*\r?$/gm;
    let start = 0;
    for (const match of reply.matchAll(fences)) {
        if (match.index > start) parts.push({ text: reply.slice(start, match.index), code: false });
        parts.push({ text: match[1].replace(/\r?\n$/, ''), code: true });
        start = match.index + match[0].length;
    }
    if (start < reply.length) parts.push({ text: reply.slice(start), code: false });
    return parts;
}
