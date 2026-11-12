export function windowsAgentCommands(address: string, username: string) {
    const host = address.trim();
    const user = username.trim();
    if (!/^[a-zA-Z0-9][a-zA-Z0-9.-]{0,252}$/.test(host) || !/^[a-zA-Z0-9_][a-zA-Z0-9_.-]{0,63}$/.test(user)) return null;

    return {
        tunnel: `ssh -o ExitOnForwardFailure=yes -R 127.0.0.1:15001:127.0.0.1:15000 ${user}@${host}`,
        telemetry: `while ($true) {
    try {
        $os = Get-CimInstance Win32_OperatingSystem -ErrorAction Stop
        $cpu = (Get-CimInstance Win32_Processor -ErrorAction Stop | Measure-Object LoadPercentage -Average).Average
        $ram = [math]::Round(100 * (1 - $os.FreePhysicalMemory / $os.TotalVisibleMemorySize), 1)
        $body = @{serverIp='${host}'; serverName=$env:COMPUTERNAME; cpuUsage=$cpu; ramUsage=$ram; metricsOnly=$true} | ConvertTo-Json
        Invoke-RestMethod -Method Post -ContentType 'application/json' -Body $body -Uri 'http://127.0.0.1:15001/api/v1/agent/sync' -TimeoutSec 8 -ErrorAction Stop
    } catch {
        Write-Warning $_.Exception.Message
    }
    Start-Sleep -Seconds 15
}`
    };
}
