package com.berk.devopsdashboard.service.impl;

import com.berk.devopsdashboard.entity.Server;
import com.berk.devopsdashboard.repository.ServerRepository;
import com.berk.devopsdashboard.service.LogService;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class LogServiceImpl implements LogService {

    private final ServerRepository serverRepository;

    @Override
    public String getLogs(Long serverId, String containerId) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new RuntimeException("Sunucu bulunamadı: " + serverId));

        // Şimdilik sadece Docker destekleyelim, K8s için pod id gerekir.
        // ContainerId, deploy geçmişinden veya canlı konteyner listesinden gelebilir.
        // Eğer containerId boş ise, belki server'da çalışan ana uygulamayı bulmaya
        // çalışabiliriz
        // veya bu özellik şimdilik "son yapılan deploy'un loglarını" getiriyor
        // olabilir.

        // Basitlik adına: IP adresinden Docker Client oluşturup log çekmeyi deneyelim.
        // NOT: Gerçek hayatta Docker TCP bağlantısı veya SSH tüneli gerekir.
        // Projenin "Agentless" yapısına uygun olarak, TCP üzerinden deniyoruz.

        try {
            String dockerHost = "tcp://" + server.getIpAddress() + ":2375";
            DockerClient dockerClient = DockerClientBuilder.getInstance(dockerHost).build();

            List<String> logs = new ArrayList<>();

            dockerClient.logContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withTail(100) // Son 100 satır
                    .withTimestamps(true)
                    .exec(new LogContainerResultCallback() {
                        @Override
                        public void onNext(Frame item) {
                            logs.add(item.toString());
                        }
                    }).awaitCompletion(5, TimeUnit.SECONDS);

            StringBuilder sb = new StringBuilder();
            for (String line : logs) {
                sb.append(line).append("\n");
            }
            return sb.toString();

        } catch (Exception e) {
            return "Loglar alınamadı: " + e.getMessage() + "\n\n(Docker Daemon 2375 portunda açık mı?)";
        }
    }

    @Override
    public java.util.List<com.github.dockerjava.api.model.Container> getRunningContainers(Long serverId) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new RuntimeException("Sunucu bulunamadı: " + serverId));

        try {
            String dockerHost = "tcp://" + server.getIpAddress() + ":2375";
            DockerClient dockerClient = DockerClientBuilder.getInstance(dockerHost).build();

            return dockerClient.listContainersCmd().withShowAll(true).exec();
        } catch (Exception e) {
            throw new RuntimeException("Konteyner listesi alınamadı: " + e.getMessage());
        }
    }
}
