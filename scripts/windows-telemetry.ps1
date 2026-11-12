param(
    [Parameter(Mandatory = $true)][string]$ServerIp,
    [uri]$Endpoint = 'http://127.0.0.1:15001/api/v1/agent/sync',
    [switch]$Once
)

do {
    try {
        $os = Get-CimInstance Win32_OperatingSystem -ErrorAction Stop
        $cpu = (Get-CimInstance Win32_Processor -ErrorAction Stop | Measure-Object LoadPercentage -Average).Average
        $body = @{
            serverIp = $ServerIp
            serverName = $env:COMPUTERNAME
            cpuUsage = $cpu
            ramUsage = [math]::Round(100 * (1 - $os.FreePhysicalMemory / $os.TotalVisibleMemorySize), 1)
            metricsOnly = $true
        } | ConvertTo-Json
        Invoke-RestMethod -Method Post -Uri $Endpoint -ContentType 'application/json' -Body $body -TimeoutSec 8 -ErrorAction Stop
    } catch {
        if ($Once) { throw }
        Write-Warning $_.Exception.Message
    }
    if (-not $Once) { Start-Sleep -Seconds 15 }
} while (-not $Once)
