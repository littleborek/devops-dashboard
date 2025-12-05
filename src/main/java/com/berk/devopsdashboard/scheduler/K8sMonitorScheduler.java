package com.berk.devopsdashboard.scheduler;

import com.berk.devopsdashboard.dto.request.AgentK8sSyncRequest;
import com.berk.devopsdashboard.entity.KubernetesPod;
import com.berk.devopsdashboard.repository.KubernetesPodRepository;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
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
public class K8sMonitorScheduler {

    private final KubernetesClient k8sClient;
    private final KubernetesPodRepository k8sRepository;

    // --- AYARLAR ---
    @Value("${app.mode:server}")
    private String appMode;

    @Value("${agent.master-url:http://localhost:8080}")
    private String masterUrl;

    @Value("${agent.my-ip:127.0.0.1}")
    private String myIp;

    @Value("${agent.server-name:Localhost}")
    private String serverName;

    @Scheduled(fixedRate = 15000) // 15 Saniyede bir
    public void syncKubernetesPods() {
        try {
            // Kubernetes API'ye erişim yoksa işlem yapma
            if (k8sClient == null) return;

            // Tüm podları çek
            List<Pod> podList = k8sClient.pods().inAnyNamespace().list().getItems();

            if ("agent".equalsIgnoreCase(appMode)) {
                // AGENT MODU: Gönder
                sendDataToMaster(podList);
            } else {
                // SERVER MODU: Kaydet
                processLocalPods(podList);
            }
        } catch (Exception e) {
            log.error("K8s tarama hatası: {}", e.getMessage());
        }
    }

    private void sendDataToMaster(List<Pod> remotePods) {
        RestTemplate restTemplate = new RestTemplate();
        List<KubernetesPod> dtoList = new ArrayList<>();

        for (Pod pod : remotePods) {
            dtoList.add(mapToEntity(pod));
        }

        AgentK8sSyncRequest request = new AgentK8sSyncRequest();
        request.setServerIp(myIp);
        request.setServerName(serverName);
        request.setPods(dtoList);

        try {
            restTemplate.postForEntity(masterUrl + "/api/v1/agent/k8s-sync", request, String.class);
            log.info("K8s verisi gönderildi. Adet: {}", dtoList.size());
        } catch (Exception e) {
            log.error("K8s verisi gönderilemedi: {}", e.getMessage());
        }
    }

    private void processLocalPods(List<Pod> pods) {
        log.info("Local K8s taranıyor... Bulunan Pod: {}", pods.size());
        for (Pod remotePod : pods) {
            KubernetesPod entity = mapToEntity(remotePod);
            
            Optional<KubernetesPod> existing = k8sRepository.findByUid(entity.getUid());
            if (existing.isPresent()) {
                KubernetesPod dbPod = existing.get();
                dbPod.setStatus(entity.getStatus());
                dbPod.setRestartCount(entity.getRestartCount());
                dbPod.setLastUpdated(LocalDateTime.now());
                k8sRepository.save(dbPod);
            } else {
                k8sRepository.save(entity);
            }
        }
    }

    // Fabric8 Pod objesini Bizim Entity'ye çevirir
    private KubernetesPod mapToEntity(Pod pod) {
        String uid = pod.getMetadata().getUid();
        String name = pod.getMetadata().getName();
        String namespace = pod.getMetadata().getNamespace();
        String status = pod.getStatus().getPhase();
        String nodeName = pod.getSpec().getNodeName();
        
        int restartCount = 0;
        if (pod.getStatus().getContainerStatuses() != null) {
            restartCount = pod.getStatus().getContainerStatuses().stream()
                    .mapToInt(cs -> cs.getRestartCount() != null ? cs.getRestartCount() : 0)
                    .sum();
        }

        return KubernetesPod.builder()
                .uid(uid)
                .name(name)
                .namespace(namespace)
                .nodeName(nodeName)
                .status(status)
                .restartCount(restartCount)
                .lastUpdated(LocalDateTime.now())
                .build();
    }
}