package com.berk.devopsdashboard.scheduler;

import com.berk.devopsdashboard.entity.Server;
import com.berk.devopsdashboard.entity.ServerHistory;
import com.berk.devopsdashboard.entity.enums.ServerStatus;
import com.berk.devopsdashboard.repository.ServerHistoryRepository;
import com.berk.devopsdashboard.repository.ServerRepository;
import com.berk.devopsdashboard.service.NotificationService;
import com.berk.devopsdashboard.service.ServerService;
import com.berk.devopsdashboard.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ServerStatusMonitor {

    private final ServerRepository serverRepository;
    private final ServerHistoryRepository historyRepository;
    private final ServerService serverService;
    private final NotificationService notificationService;
    private final SseService sseService;
    private final com.berk.devopsdashboard.repository.DockerContainerRepository containerRepository;

    @Scheduled(fixedRate = 15000)
    public void updateServerStatus() {
        List<Server> servers = serverRepository.findAll();
        if (servers.isEmpty())
            return;

        log.info("Paralel izleme başlatılıyor: {} sunucu", servers.size());

        List<CompletableFuture<Void>> futures = servers.stream()
                .map(server -> CompletableFuture.runAsync(() -> checkSingleServer(server)))
                .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        sseService.broadcast("server-update", "updated");
        log.info("İzleme döngüsü tamamlandı.");
    }

    private void checkSingleServer(Server server) {
        try {
            ServerStatus oldStatus = server.getStatus();
            ServerStatus newStatus = serverService.checkServerStatus(server);

            server.setStatus(newStatus);
            serverRepository.save(server);

            ServerHistory history = ServerHistory.builder()
                    .server(server)
                    .status(newStatus)
                    .responseTime(server.getLastResponseTime())
                    .checkTime(LocalDateTime.now())
                    .build();
            historyRepository.save(history);

            if (oldStatus != newStatus) {
                log.info("DURUM DEĞİŞTİ: {} -> {} (Eski: {})", server.getName(), newStatus, oldStatus);

                if (newStatus == ServerStatus.OFFLINE && oldStatus != ServerStatus.UNKNOWN) {
                    if (!server.isMaintenanceMode()) {
                        notificationService.sendOfflineAlert(server);
                    }
                }
            }

            // Sunucu OFFLINE ise containerları da offline yap
            if (newStatus == ServerStatus.OFFLINE) {
                markContainersOffline(server.getId());
            }

            if (server.getStatus() == ServerStatus.ONLINE && !server.isMaintenanceMode()) {
                checkThresholds(server);
            }

        } catch (Exception e) {
            log.error("Sunucu kontrol hatası ({}): {}", server.getName(), e.getMessage());
        }
    }

    private void markContainersOffline(Long serverId) {
        var containers = containerRepository.findByServerId(serverId);
        for (var c : containers) {
            if (!"offline".equals(c.getState())) {
                c.setState("offline");
                c.setStatus("Server Offline");
                c.setLastUpdated(LocalDateTime.now());
                containerRepository.save(c);
            }
        }
    }

    private void checkThresholds(Server server) {
        if (server.getCpuUsageThreshold() != null && server.getCpuUsageThreshold() > 0
                && server.getCpuUsage() != null && server.getCpuUsage() > server.getCpuUsageThreshold()) {
            notificationService.sendResourceAlert(server, "CPU", server.getCpuUsage(), server.getCpuUsageThreshold());
        }

        if (server.getRamUsageThreshold() != null && server.getRamUsageThreshold() > 0
                && server.getRamUsage() != null && server.getRamUsage() > server.getRamUsageThreshold()) {
            notificationService.sendResourceAlert(server, "RAM", server.getRamUsage(), server.getRamUsageThreshold());
        }
    }
}