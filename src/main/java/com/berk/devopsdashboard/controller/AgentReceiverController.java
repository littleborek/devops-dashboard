package com.berk.devopsdashboard.controller;

import com.berk.devopsdashboard.dto.request.AgentK8sSyncRequest;
import com.berk.devopsdashboard.dto.request.AgentSyncRequest;
import com.berk.devopsdashboard.entity.DockerContainer;
import com.berk.devopsdashboard.entity.KubernetesPod;
import com.berk.devopsdashboard.entity.Server;
import com.berk.devopsdashboard.entity.enums.ServerStatus;
import com.berk.devopsdashboard.repository.DockerContainerRepository;
import com.berk.devopsdashboard.repository.KubernetesPodRepository;
import com.berk.devopsdashboard.repository.ServerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/agent")
@RequiredArgsConstructor
@Slf4j
public class AgentReceiverController {

    private final ServerRepository serverRepository;
    private final DockerContainerRepository containerRepository;
    private final KubernetesPodRepository k8sRepository;
    private final com.berk.devopsdashboard.service.PrometheusMetricsService metricsService;

    @PostMapping("/sync")
    @Transactional
    public ResponseEntity<String> receiveDockerData(@RequestBody AgentSyncRequest request) {
        log.info("Agent'dan Docker/Metrik verisi geldi: {} ({}) - CPU: %{}, RAM: %{}, Disk: %{}, Load: {}",
                request.getServerName(), request.getServerIp(), request.getCpuUsage(), request.getRamUsage(),
                request.getDiskUsage(), request.getLoadAvg());
        Optional<Server> serverOpt = serverRepository.findByIpAddress(request.getServerIp());
        if (serverOpt.isEmpty()) {
            log.warn("Tanımsız Agent girişimi: {}", request.getServerIp());
            return ResponseEntity.status(403).body("Sunucu kayıtlı değil! Lütfen önce panelden sunucuyu ekleyin.");
        }

        Server server = serverOpt.get();
        server.setStatus(ServerStatus.ONLINE);
        server.setCpuUsage(request.getCpuUsage());
        server.setRamUsage(request.getRamUsage());
        server.setDiskUsage(request.getDiskUsage());
        server.setLoadAvg(request.getLoadAvg());
        server.setTotalRam(request.getTotalRam());

        serverRepository.save(server);

        // Prometheus Metriklerini Kaydet/Güncelle
        metricsService.registerServerMetrics(server);

        for (DockerContainer incomingContainer : request.getContainers()) {
            Optional<DockerContainer> existing = containerRepository
                    .findByContainerId(incomingContainer.getContainerId());

            if (existing.isPresent()) {
                DockerContainer dbContainer = existing.get();
                dbContainer.setState(incomingContainer.getState());
                dbContainer.setStatus(incomingContainer.getStatus());
                dbContainer.setLastUpdated(LocalDateTime.now());
                dbContainer.setServer(server);
                containerRepository.save(dbContainer);
            } else {
                incomingContainer.setServer(server);
                containerRepository.save(incomingContainer);
            }
        }

        return ResponseEntity.ok("Docker & Metrics Sync Başarılı");
    }

    @GetMapping("/script")
    public ResponseEntity<String> getAgentScript(jakarta.servlet.http.HttpServletRequest request) {
        String masterUrl = request.getRequestURL().toString().replace("/script", "/sync");
        String k8sUrl = request.getRequestURL().toString().replace("/script", "/k8s/sync");

        String script = "#!/bin/bash\n" +
                "AGENT_DEST=\"/tmp/devops_agent.sh\"\n" +
                "if [ \"$EUID\" -eq 0 ]; then AGENT_DEST=\"/usr/local/bin/devops_agent.sh\"; fi\n" +
                "echo \">>> DevOps Dashboard Ajan Kurulumu Başlatılıyor...\"\n" +
                "echo \"Master URL: " + masterUrl + "\"\n" +
                "echo \"Polling Rate: 15 saniye\"\n" +
                "\n" +
                "cat << 'EOF' > $AGENT_DEST\n" +
                "#!/bin/bash\n" +
                "export LC_ALL=C\n" +
                "MASTER_URL=\"" + masterUrl + "\"\n" +
                "K8S_URL=\"" + k8sUrl + "\"\n" +
                "SERVER_NAME=$(hostname)\n" +
                "SERVER_IP=$(hostname -I | awk '{print $1}')\n" +
                "\n" +
                "send_data() {\n" +
                "  # CPU & RAM\n" +
                "  IDLE=$(top -bn1 | grep \"Cpu(s)\" | awk -F',' '{for(i=1;i<=NF;i++) if($i ~ /id/) print $i}' | awk '{print $1}')\n"
                +
                "  CPU_USAGE=$(awk -v idle=\"$IDLE\" 'BEGIN {print (idle==\"\" ? 0 : 100 - idle)}')\n" +
                "  RAM_DATA=$(free -k | grep Mem)\n" +
                "  RAM_TOTAL_KB=$(echo $RAM_DATA | awk '{print $2}')\n" +
                "  RAM_USED_KB=$(echo $RAM_DATA | awk '{print $3}')\n" +
                "  RAM_TOTAL_GB=$(awk -v t=$RAM_TOTAL_KB 'BEGIN {printf \"%.2f\", t/1024/1024}')\n" +
                "  RAM_USAGE_PERCENT=$(awk -v t=$RAM_TOTAL_KB -v u=$RAM_USED_KB 'BEGIN {printf \"%.2f\", (t==0 ? 0 : (u*100)/t)}')\n"
                +
                "\n" +
                "  # Disk & Load\n" +
                "  DISK_USAGE=$(df / | tail -1 | awk '{print $5}' | sed 's/%//')\n" +
                "  LOAD_AVG=$(uptime | awk -F'load average:' '{print $2}' | xargs)\n" +
                "\n" +
                "  # Docker\n" +
                "  CONTAINERS=\"[]\"\n" +
                "  if command -v docker >/dev/null 2>&1; then\n" +
                "    DOCKER_LIST=$(docker ps --format '{\"containerId\":\"{{.ID}}\",\"name\":\"{{.Names}}\",\"image\":\"{{.Image}}\",\"state\":\"{{.State}}\",\"status\":\"{{.Status}}\"}' 2>/dev/null | paste -sd, -)\n"
                +
                "    if [ ! -z \"$DOCKER_LIST\" ]; then CONTAINERS=\"[$DOCKER_LIST]\"; fi\n" +
                "  fi\n" +
                "\n" +
                "  # Kubernetes (kubectl)\n" +
                "  PODS=\"[]\"\n" +
                "  if command -v kubectl >/dev/null 2>&1; then\n" +
                "    POD_LIST=$(kubectl get pods -A --no-headers 2>/dev/null | awk '{printf \"{\\\"namespace\\\":\\\"%s\\\",\\\"name\\\":\\\"%s\\\",\\\"status\\\":\\\"%s\\\",\\\"uid\\\":\\\"%s-%s\\\",\\\"restartCount\\\":%d},\", $1, $2, $4, $1, $2, $5}' | sed 's/,$//')\n"
                +
                "    if [ ! -z \"$POD_LIST\" ]; then PODS=\"[$POD_LIST]\"; fi\n" +
                "  fi\n" +
                "\n" +
                "  PAYLOAD=\"{\\\"serverIp\\\": \\\"$SERVER_IP\\\", \\\"serverName\\\": \\\"$SERVER_NAME\\\", \\\"cpuUsage\\\": $CPU_USAGE, \\\"ramUsage\\\": $RAM_USAGE_PERCENT, \\\"diskUsage\\\": $DISK_USAGE, \\\"loadAvg\\\": \\\"$LOAD_AVG\\\", \\\"totalRam\\\": \\\"${RAM_TOTAL_GB} GB\\\", \\\"containers\\\": $CONTAINERS}\"\n"
                +
                "  curl -s -X POST -H \"Content-Type: application/json\" -d \"$PAYLOAD\" \"$MASTER_URL\" > /dev/null\n"
                +
                "\n" +
                "  if [ \"$PODS\" != \"[]\" ]; then\n" +
                "    K8S_PAYLOAD=\"{\\\"serverIp\\\": \\\"$SERVER_IP\\\", \\\"serverName\\\": \\\"$SERVER_NAME\\\", \\\"pods\\\": $PODS}\"\n"
                +
                "    curl -s -X POST -H \"Content-Type: application/json\" -d \"$K8S_PAYLOAD\" \"$K8S_URL\" > /dev/null\n"
                +
                "  fi\n" +
                "}\n" +
                "\n" +
                "for i in {1..4}; do\n" +
                "  send_data\n" +
                "  if [ $i -lt 4 ]; then sleep 15; fi\n" +
                "done\n" +
                "EOF\n" +
                "\n" +
                "chmod +x $AGENT_DEST\n" +
                "echo \">>> Otomatik çalıştırma (Cron) ayarlanıyor...\"\n" +
                "(crontab -l 2>/dev/null | grep -F \"$AGENT_DEST\") || (crontab -l 2>/dev/null; echo \"* * * * * $AGENT_DEST > /dev/null 2>&1\") | crontab -\n"
                +
                "\n" +
                "if command -v docker >/dev/null 2>&1 && ! docker ps >/dev/null 2>&1; then\n" +
                "  echo \"⚠️ UYARI: Docker erişim yetkisi yok. Daha iyi sonuç için kurulumu 'sudo' ile yapın.\"\n" +
                "fi\n" +
                "bash $AGENT_DEST &\n" +
                "echo \"✔ Kurulum tamamlandı!\"\n";

        return ResponseEntity.ok()
                .header("Content-Type", "text/x-shellscript")
                .body(script);
    }

    @PostMapping("/k8s/sync")
    @Transactional
    public ResponseEntity<String> receiveK8sData(@RequestBody AgentK8sSyncRequest request) {
        log.info("Agent'dan K8s verisi geldi: {} ({})", request.getServerName(), request.getServerIp());
        Optional<Server> serverOpt = serverRepository.findByIpAddress(request.getServerIp());
        if (serverOpt.isEmpty()) {
            return ResponseEntity.status(403).body("Sunucu kayıtlı değil!");
        }

        Server server = serverOpt.get();
        if (server.getStatus() != ServerStatus.ONLINE) {
            server.setStatus(ServerStatus.ONLINE);
            serverRepository.save(server);
        }
        for (KubernetesPod incomingPod : request.getPods()) {
            Optional<KubernetesPod> existing = k8sRepository.findByUid(incomingPod.getUid());

            if (existing.isPresent()) {
                KubernetesPod dbPod = existing.get();
                dbPod.setStatus(incomingPod.getStatus());
                dbPod.setRestartCount(incomingPod.getRestartCount());
                dbPod.setLastUpdated(LocalDateTime.now());
                dbPod.setServer(server);
                k8sRepository.save(dbPod);
            } else {
                incomingPod.setServer(server);
                k8sRepository.save(incomingPod);
            }
        }

        return ResponseEntity.ok("K8s Sync Başarılı");
    }
}