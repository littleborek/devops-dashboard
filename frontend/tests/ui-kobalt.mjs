import assert from 'node:assert/strict';

const { chromium } = await import(process.env.PLAYWRIGHT_MODULE || 'playwright');
const browser = await chromium.launch({ channel: 'chrome' });
const base = 'http://127.0.0.1:4317';
const server = { id: 1, name: 'Kobalt QA fixture', ipAddress: '192.0.2.1', operatingSystem: 'Ubuntu', location: 'Antalya', category: 'QA', status: 'ONLINE', cpuUsage: 27, ramUsage: 64, diskUsage: 42 };
const context = await browser.newContext({ viewport: { width: 1440, height: 960 }, permissions: ['clipboard-read', 'clipboard-write'] });
const page = await context.newPage();
page.setDefaultTimeout(3500);
const errors = [], diagnostics = [], failed = [];
page.on('pageerror', e => errors.push(e.message));
page.on('console', m => { if (['warning', 'error'].includes(m.type())) diagnostics.push(m.text()); });
let login = 401, inventory = 'normal', chat = 'error', sends = 0, queued = 0, query, detailStatus = 200, logAttempts = 0;
await context.route('**/*', async r => {
  const url = new URL(r.request().url());
  if (url.origin !== base) return r.abort();
  const p = url.pathname, method = r.request().method();
  if (p === '/api/auth/login') return r.fulfill({ status: login, body: '' });
  if (/\/tasks(?:\/|$)/.test(p)) { queued++; return r.fulfill({ status: 403 }); }
  if (p === '/api/v1/agent/logs/stream/1') {
    if (++logAttempts === 1) return r.fulfill({ status: 503 });
    return r.fulfill({ contentType: 'text/event-stream', body: 'data: {"serverId":"1","content":"Fixture log received","level":"INFO","timestamp":"now"}\n\n' });
  }
  if (p.endsWith('/ai/chat/stream')) {
    sends++; query = r.request().postDataJSON().query;
    if (chat === 'hold') return;
    const body = 'data: <b>literal</b>\n\ndata: \n' +
      'data: ```html\ndata: <b>code</b>\ndata: ```\n\n' + (chat === 'error' ? 'data: [STREAM_ERROR] fixture\n\n' : '');
    return r.fulfill({ contentType: 'text/event-stream', body });
  }
  if (p === '/api/v1/servers' && method === 'GET') {
    const edges = [{ ...server, id: 2, name: 'Unknown host', status: 'UNKNOWN' },
      { ...server, id: 3, name: 'Offline host', status: 'OFFLINE' },
      { ...server, id: 4, name: 'Hot host', cpuUsageThreshold: 20 }];
    return inventory === 'error' ? r.fulfill({ status: 500 }) : r.fulfill({ json: inventory === 'empty' ? [] : inventory === 'edges' ? edges : [server] });
  }
  if (p === '/api/v1/servers/1') return method === 'GET' && detailStatus === 200 ? r.fulfill({ json: server }) : r.fulfill({ status: method === 'GET' ? detailStatus : 500 });
  if (p === '/api/v1/deployments/server/1') return r.fulfill({ status: 500 });
  if (p.startsWith('/api/v1/settings/')) return r.fulfill({ body: p.endsWith('TOKEN') ? 'fixture-token' : '42' });
  if (p === '/actuator/health') return r.fulfill({ json: { status: 'UP' } });
  if (p.startsWith('/api/') || p.startsWith('/actuator/')) return r.fulfill({ json: [] });
  return r.continue();
});

const button = name => page.getByRole('button', { name, exact: true });
const visible = locator => locator.waitFor({ state: 'visible' });
const focus = async locator => {
  await locator.and(page.locator(':focus')).waitFor({ state: 'attached' });
};
const check = async (name, fn) => {
  try { await fn(); console.log('PASS', name); }
  catch (e) { failed.push(name); console.error('FAIL', name, e.message); }
};
const overflow = async () => assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth));
const go = path => page.goto(base + path);
const prefs = async (theme, lang) => {
  const root = page.locator('html');
  assert.equal(await root.getAttribute('data-theme'), theme);
  assert.equal(await root.getAttribute('lang'), lang);
};
const dashboard = () => visible(page.getByRole('heading', { name: 'Infrastructure watch' }));
const detail = async () => {
  await page.getByRole('link', { name: server.name }).click();
  await visible(page.getByRole('heading', { name: server.name }));
};
try {
  await check('login preferences, hidden AI, 401/500/success and navigation', async () => {
    await go('/login');
    assert.equal(await page.locator('app-ai-chat').count(), 0);
    await button('Koyu temaya geç').click();
    await page.getByRole('combobox').selectOption('en');
    await page.reload();
    await prefs('dark', 'en');
    await page.locator('input[name=username]').fill('fixture');
    await page.locator('input[name=password]').fill('fixture');
    const signIn = button('Sign in to dashboard');
    await signIn.click();
    await visible(page.getByText('Incorrect username or password.'));
    login = 500;
    await signIn.click();
    await visible(page.getByText('Could not connect. Please try again.'));
    login = 200;
    await signIn.click();
    await dashboard();
  });
  await check('desktop light EN and mobile dark TR inventory screenshots', async () => {
    await go('/');
    await dashboard();
    await button('Switch to light theme').click();
    await visible(page.getByRole('link', { name: server.name }));
    await prefs('light', 'en');
    await overflow();
    await page.screenshot({ path: '/tmp/kobalt-desktop-light.png', fullPage: true });
    await page.setViewportSize({ width: 390, height: 844 });
    await button('Switch to dark theme').click();
    await page.getByRole('combobox').selectOption('tr');
    await prefs('dark', 'tr');
    await visible(page.locator('meter[aria-label="CPU"]'));
    await visible(page.locator('meter[aria-label="RAM"]'));
    await overflow();
    await page.screenshot({ path: '/tmp/kobalt-mobile-dark.png', fullPage: true });
    await page.getByRole('combobox').selectOption('en');
  });
  await check('inventory empty and error with recovery', async () => {
    inventory = 'empty'; await go('/'); await visible(button('Add your first server'));
    inventory = 'error'; await go('/'); await visible(page.getByRole('alert'));
    inventory = 'normal'; await button('Retry').click();
    await visible(page.getByRole('link', { name: server.name }));
  });
  await check('tabs and settings keyboard close/restore', async () => {
    await go('/');
    await visible(page.getByRole('link', { name: server.name }));
    for (const name of ['Docker', 'Kubernetes', 'MCP', 'Servers']) {
      await button(name).click();
      await visible(button(name).and(page.locator('[aria-current="true"]')));
      await overflow();
    }
    await button('Settings').click();
    await focus(button('Close'));
    await visible(button('Save AI settings'));
    await page.keyboard.press('Escape');
    await focus(button('Settings'));
  });
  await check('failed server edit retains values; Escape restores focus', async () => {
    await go('/');
    const edit = button(`Edit: ${server.name}`);
    await edit.click(); await focus(button('Close'));
    const name = page.getByLabel('Server name');
    assert.equal(await name.inputValue(), server.name);
    await name.fill('Retain this fixture value');
    await button('Save').click();
    await visible(page.getByText(/could not|failed/i).filter({ visible: true }));
    assert.equal(await name.inputValue(), 'Retain this fixture value');
    await page.keyboard.press('Escape'); await focus(edit);
  });
  await check('unknown/offline metrics and threshold problem', async () => {
    inventory = 'edges';
    try {
      await go('/');
      for (const name of ['Unknown host', 'Offline host', 'Hot host']) {
        const row = page.locator('.server-row').filter({ has: page.getByRole('link', { name }) });
        await visible(row);
        assert.equal(await row.locator('meter').count(), name === 'Hot host' ? 2 : 0);
      }
      await visible(page.locator('meter.high[aria-label="CPU"]'));
      await visible(page.getByText('Server offline', { exact: true }));
    } finally { inventory = 'normal'; }
  });
  await check('detail 404, 500, retry and SSE log reconnect', async () => {
    detailStatus = 404;
    try {
      await go('/server/1');
      await visible(page.getByText('Server not found.'));
      detailStatus = 500;
      await go('/server/1');
      await visible(page.getByText('Could not load server details.'));
      detailStatus = 200;
      await button('Retry').click();
      await visible(page.getByText('Could not load deployments.'));
      await button('Live logs').click();
      await visible(page.getByText(/Log stream connection failed/));
      await button('Reconnect').click();
      await visible(page.getByText(/Fixture log received/));
      assert.equal(logAttempts, 2);
      await page.keyboard.press('Escape');
      await focus(button('Live logs'));
    } finally { detailStatus = 200; }
  });
  await check('detail preferences, persistence, overflow and first AI focus/cancel', async () => {
    await go('/'); await detail();
    await button('Switch to light theme').click();
    await page.getByRole('combobox').selectOption('tr');
    await page.reload(); await prefs('light', 'tr');
    await visible(page.getByRole('heading', { name: server.name }));
    await overflow();
    await page.screenshot({ path: '/tmp/kobalt-detail-mobile-light.png', fullPage: true });
    await button('Koyu temaya geç').click();
    await page.getByRole('combobox').selectOption('en');
    await overflow();
    await button('Ask AI').click(); await focus(button('Close'));
    await page.screenshot({ path: '/tmp/kobalt-ai-mobile-dark.png', fullPage: true });
    assert.equal(sends, 0);
    await page.keyboard.press('Escape'); await focus(button('Ask AI'));
    assert.equal(sends, 0);
  });
  await check('AI safe HTML/code/copy, partial error, no queue', async () => {
    await visible(button('Ask AI'));
    if (!(await page.locator('app-ai-chat dialog').evaluate(e => e.open))) await button('Ask AI').click();
    await page.locator('#ai-question').fill('Fixture question'); await button('Send').click();
    const panel = page.locator('app-ai-chat dialog');
    await visible(panel.getByText('<b>literal</b>', { exact: false }).last());
    await visible(panel.locator('pre').getByText('<b>code</b>'));
    assert.equal(await panel.locator('b').count(), 0);
    assert.match(query, /Kobalt QA fixture/);
    await visible(panel.getByText(/interrupt|fail|could not/i).last());
    await button('Copy').click();
    assert.equal((await page.evaluate(() => navigator.clipboard.readText())).trim(), '<b>code</b>');
    assert.equal(queued, 0); await overflow();
  });
  await check('AI context removal and stop cancel in-flight requests', async () => {
    chat = 'hold';
    const submit = async () => {
      await page.locator('#ai-question').fill('Held fixture');
      const request = page.waitForRequest('**/api/v1/ai/chat/stream');
      await button('Send').click(); await request;
    };
    const canceled = () => page.waitForEvent('requestfailed', { predicate: r => r.url().endsWith('/ai/chat/stream') });
    await submit(); let failure = canceled(); await button('Remove context').click(); await failure;
    await visible(page.getByText(/No server context/));
    assert.equal(await page.getByText('Fixture question', { exact: true }).count(), 0);
    await submit(); failure = canceled(); await button('Stop').click(); await failure;
    await focus(page.locator('#ai-question')); assert.equal(queued, 0);
    assert.doesNotMatch(query, /Server snapshot/);
    await page.keyboard.press('Escape');
    await button('Ask AI').click();
    await visible(page.getByText('Fixture question', { exact: true }));
    await visible(page.getByText(/Request stopped/));
  });
} finally {
  console.log('Uncaught page errors:', JSON.stringify(errors));
  console.log('Console warning/error messages:', JSON.stringify([...new Set(diagnostics)]));
  console.log('Task API requests:', queued);
  console.log('Failed checks:', JSON.stringify(failed));
  await browser.close();
  if (failed.length || errors.length || queued) process.exitCode = 1;
}
