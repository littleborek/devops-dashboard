package com.berk.devopsdashboard.scheduler;

import com.berk.devopsdashboard.entity.Server;
import com.berk.devopsdashboard.entity.enums.ServerStatus;
import com.berk.devopsdashboard.repository.ServerRepository;
import com.berk.devopsdashboard.service.NotificationService;
import com.berk.devopsdashboard.service.ServerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

import com.berk.devopsdashboard.entity.ServerHistory;
import com.berk.devopsdashboard.repository.ServerHistoryRepository; 
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class ServerStatusMonitor {

    private final ServerRepository serverRepository;
    private final ServerHistoryRepository historyRepository;
    private final ServerService serverService;
    private final NotificationService notificationService;

    @Scheduled(fixedRate = 10000)
    public void updateServerStatus() {
        List<Server> servers = serverRepository.findAll();

        for (Server server : servers) {
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
                    server.setStatus(newStatus);
                    serverRepository.save(server);
                    log.info("DURUM DEĞİŞTİ: {} -> {} (Eski: {})", server.getName(), newStatus, oldStatus);

                    if (newStatus == ServerStatus.OFFLINE && oldStatus != ServerStatus.UNKNOWN) {
                        
                        if (!server.isMaintenanceMode()) {
                            log.warn("🚨 ALARM GÖNDERİLİYOR: {}", server.getName());
                            notificationService.sendOfflineAlert(server);
                        } else {
                            log.warn("Sunucu bakıma alındığı için bildirim atılmadı: {}", server.getName());
                        }
                    }
                }

            } catch (Exception e) {
                log.error("Hata: {}", server.getName(), e);
            }
        }
    }
}