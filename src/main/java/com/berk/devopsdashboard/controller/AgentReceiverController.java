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
    @PostMapping("/sync")
    @Transactional
    public ResponseEntity<String> receiveDockerData(@RequestBody AgentSyncRequest request) {
        log.info("Agent'dan Docker/Metrik verisi geldi: {} ({}) - CPU: %{}, RAM: %{}", 
                request.getServerName(), request.getServerIp(), request.getCpuUsage(), request.getRamUsage());
        Optional<Server> serverOpt = serverRepository.findByIpAddress(request.getServerIp());
        if (serverOpt.isEmpty()) {
            log.warn("Tanımsız Agent girişimi: {}", request.getServerIp());
            return ResponseEntity.status(403).body("Sunucu kayıtlı değil! Lütfen önce panelden sunucuyu ekleyin.");
        }

        Server server = serverOpt.get();
        server.setStatus(ServerStatus.ONLINE);
        server.setCpuUsage(request.getCpuUsage());
        server.setRamUsage(request.getRamUsage());
        server.setTotalRam(request.getTotalRam());
        
        serverRepository.save(server);
        for (DockerContainer incomingContainer : request.getContainers()) {
            Optional<DockerContainer> existing = containerRepository.findByContainerId(incomingContainer.getContainerId());
            
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
                k8sRepository.save(dbPod);
            } else {
                k8sRepository.save(incomingPod);
            }
        }

        return ResponseEntity.ok("K8s Sync Başarılı");
    }
}