# Local monitoring

With the dashboard already running in the same Compose project, enable the
optional monitoring services. Keep any Compose override used for the dashboard
in the command.

```sh
docker compose --profile monitoring up -d prometheus grafana
```

- Grafana: http://127.0.0.1:3000/d/devops-overview/
- Prometheus targets: http://127.0.0.1:9090/targets

Grafana provisions the DevOps Overview and its Prometheus data source at
startup. Change Grafana's initial admin password on first login. Both UIs are
bound to loopback; do not publish them directly to a network.

Prometheus scrapes `devops-dashboard:15000/actuator/prometheus` over the Compose
network. Allow one scrape interval (15 seconds) for data to appear. The Docker
panel counts known containers, including stopped ones but not removed or
invalid inventory records. Prometheus and Grafana use separate named volumes.

## Windows telemetry over SSH

Register the Windows host with its LAN address and OS set to Windows. With
fresh telemetry (less than 45 seconds old), the host is online. Otherwise the
backend checks TCP port 22 for Windows hosts registered by address or hostname;
if it cannot connect, the host status is unknown. A successful TCP check does
not make the last CPU/RAM sample fresh.

For a local test, use **Agent setup → Windows** in the dashboard. It generates
the SSH tunnel command and an interactive PowerShell loop. This path does not
require changing the Windows script execution policy.

Alternatively, if scripts are permitted, copy `scripts/windows-telemetry.ps1`
to your Windows user directory using your verified SSH connection. Open the
SSH session with the reverse forwarding:

```sh
ssh -o ExitOnForwardFailure=yes -R 127.0.0.1:15001:127.0.0.1:15000 USER@WINDOWS_IP
```

In that session, open PowerShell and run:

```powershell
.\windows-telemetry.ps1 -ServerIp 'WINDOWS_IP'
```

Use `-Once` for a single sample, or Ctrl+C to stop the foreground loop. It
does not install a service, run remote tasks, or replace Docker inventory.
Keep the SSH session open while sending samples; both dashboard and tunnel
listeners remain on loopback.
