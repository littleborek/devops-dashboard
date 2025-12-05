package com.berk.devopsdashboard.scheduler;

import com.berk.devopsdashboard.dto.request.AgentSyncRequest;
import com.berk.devopsdashboard.entity.DockerContainer;
import com.berk.devopsdashboard.entity.Server;
import com.berk.devopsdashboard.entity.enums.ServerStatus;
import com.berk.devopsdashboard.repository.DockerContainerRepository;
import com.berk.devopsdashboard.repository.ServerRepository;
import com.berk.devopsdashboard.service.SystemMetricsService;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DockerMonitorScheduler {

    private final DockerClient dockerClient;
    private final DockerContainerRepository dockerContainerRepository;
    private final ServerRepository serverRepository;
    private final SystemMetricsService metricsService; 

    @Value("${app.mode:server}")
    private String appMode;

    @Value("${agent.master-url:http://localhost:8080}")
    private String masterUrl;

    @Value("${agent.my-ip:127.0.0.1}")
    private String myIp;
    
    @Value("${agent.server-name:Localhost}")
    private String serverName;

    @Scheduled(fixedRate = 10000)
    public void syncDockerContainers() {
        try {
            List<Container> allContainers = dockerClient.listContainersCmd().withShowAll(true).exec();

            if ("agent".equalsIgnoreCase(appMode)) {
                sendDataToMaster(allContainers);
            } else {
                processLocalContainers(allContainers);
            }
        } catch (Exception e) {
            log.error("Docker tarama hatası: {}", e.getMessage());
        }
    }

    private void sendDataToMaster(List<Container> remoteContainers) {
        RestTemplate restTemplate = new RestTemplate();
        List<DockerContainer> dtoList = new ArrayList<>();

        for (Container c : remoteContainers) {
            dtoList.add(DockerContainer.builder()
                    .containerId(c.getId())
                    .name(c.getNames()[0].replace("/", ""))
                    .image(c.getImage())
                    .state(c.getState())
                    .status(c.getStatus())
                    .lastUpdated(LocalDateTime.now())
                    .build());
        }

        AgentSyncRequest request = new AgentSyncRequest();
        request.setServerIp(myIp);
        request.setServerName(serverName);
        request.setContainers(dtoList);
        request.setCpuUsage(metricsService.getCpuUsage());
        request.setRamUsage(metricsService.getRamUsage());
        request.setTotalRam(metricsService.getTotalRam());
        try {
            restTemplate.postForEntity(masterUrl + "/api/v1/agent/sync", request, String.class);
            log.info("Agent verisi gönderildi (CPU: %{}, RAM: %{})", request.getCpuUsage(), request.getRamUsage());
        } catch (Exception e) {
            log.error("Ana sunucuya bağlanılamadı: {}", e.getMessage());
        }
    }

    private void processLocalContainers(List<Container> containers) {
        Server localServer = getOrCreateLocalServer();
        localServer.setCpuUsage(metricsService.getCpuUsage());
        localServer.setRamUsage(metricsService.getRamUsage());
        localServer.setTotalRam(metricsService.getTotalRam());
        localServer.setLastResponseTime(0);
        serverRepository.save(localServer);
        // ------------------------------------------

        for (Container remoteContainer : containers) {
            String containerId = remoteContainer.getId();
            Optional<DockerContainer> existingOpt = dockerContainerRepository.findByContainerId(containerId);
             if (existingOpt.isPresent()) {
                DockerContainer dbContainer = existingOpt.get();
                dbContainer.setState(remoteContainer.getState());
                dbContainer.setStatus(remoteContainer.getStatus());
                dbContainer.setLastUpdated(LocalDateTime.now());
                dbContainer.setServer(localServer);
                dockerContainerRepository.save(dbContainer);
            } else {
                DockerContainer newContainer = DockerContainer.builder()
                        .containerId(containerId)
                        .name(remoteContainer.getNames()[0].replace("/", ""))
                        .image(remoteContainer.getImage())
                        .state(remoteContainer.getState())
                        .status(remoteContainer.getStatus())
                        .lastUpdated(LocalDateTime.now())
                        .server(localServer)
                        .build();
                dockerContainerRepository.save(newContainer);
            }
        }
    }

    private Server getOrCreateLocalServer() {
        return serverRepository.findByIpAddress("127.0.0.1")
                .orElseGet(() -> {
                    Server s = new Server();
                    s.setName(serverName);
                    s.setIpAddress("127.0.0.1");
                    s.setOperatingSystem(System.getProperty("os.name"));
                    s.setLocation("Local");
                    s.setStatus(ServerStatus.ONLINE);
                    s.setCategory("Master Server");
                    return serverRepository.save(s);
                });
    }
}