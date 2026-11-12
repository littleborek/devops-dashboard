import assert from 'node:assert/strict';
import test from 'node:test';
import { windowsAgentCommands } from '../src/app/components/dashboard/windows-agent-setup.ts';

test('rejects unsafe Windows setup hosts', () => {
    for (const host of ['', ' ', '-host', 'host name', 'host\nname', 'host;name', "host'name", 'host$(name)', 'https://host', 'host:22', 'user@host', 'a'.repeat(254)]) {
        assert.equal(windowsAgentCommands(host, 'berk'), null, JSON.stringify(host));
    }
});

test('rejects unsafe Windows setup usernames', () => {
    for (const user of ['', ' ', '-user', 'user name', 'user\nname', 'user;name', "user'name", 'user$(name)', 'user@host', 'a'.repeat(65)]) {
        assert.equal(windowsAgentCommands('dashboard.example.test', user), null, JSON.stringify(user));
    }
});

test('generates a loopback SSH tunnel and a retrying metrics-only PowerShell loop', () => {
    for (const host of ['dashboard-1.example.test', '192.0.2.10']) {
        const { tunnel, telemetry } = windowsAgentCommands(` ${host} `, ' ops_user-1 ');
        assert.equal(tunnel, `ssh -o ExitOnForwardFailure=yes -R 127.0.0.1:15001:127.0.0.1:15000 ops_user-1@${host}`);

        assert.match(telemetry, /^while \(\$true\) \{\s+try \{/);
        assert.match(telemetry, /Get-CimInstance Win32_OperatingSystem/);
        assert.match(telemetry, /Get-CimInstance Win32_Processor.*Measure-Object LoadPercentage -Average/);
        assert.match(telemetry, /\$os\.FreePhysicalMemory \/ \$os\.TotalVisibleMemorySize/);
        assert.ok(telemetry.includes(`@{serverIp='${host}'; serverName=$env:COMPUTERNAME; cpuUsage=$cpu; ramUsage=$ram; metricsOnly=$true} | ConvertTo-Json`));
        assert.match(telemetry, /Invoke-RestMethod -Method Post -ContentType 'application\/json' -Body \$body -Uri 'http:\/\/127\.0\.0\.1:15001\/api\/v1\/agent\/sync'.* -ErrorAction Stop/);
        assert.match(telemetry, /\} catch \{\s+Write-Warning \$_\.Exception\.Message\s+\}\s+Start-Sleep -Seconds 15\s+\}$/);
    }
});
