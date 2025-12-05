package com.berk.devopsdashboard.service;

import com.berk.devopsdashboard.dto.response.K8sPodDTO;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class K8sService {

    private final CoreV1Api coreV1Api;

    public List<K8sPodDTO> listAllPods() {
        if (coreV1Api == null) {
            log.warn("Kubernetes API Client yapılandırılmamış. Pod listesi boş dönüyor.");
            return Collections.emptyList();
        }

        try {
            // Tüm namespace'lerdeki Pod'ları listele
            V1PodList podList = coreV1Api.listPodForAllNamespaces(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null 
            );
            return podList.getItems().stream()
                .map(this::mapToPodDTO)
                .collect(Collectors.toList());

        } catch (ApiException e) {
            log.error("K8s API hatası (listPods): Kodu: {}, Mesaj: {}", e.getCode(), e.getResponseBody());
            return Collections.emptyList();
        }
    }
    private K8sPodDTO mapToPodDTO(V1Pod pod) {
        return K8sPodDTO.builder()
            .name(pod.getMetadata().getName())
            .namespace(pod.getMetadata().getNamespace())
            .phase(pod.getStatus().getPhase())
            .nodeName(pod.getSpec().getNodeName())
            .hostIP(pod.getStatus().getHostIP())
            .creationTimestamp(pod.getMetadata().getCreationTimestamp())
            .build();
    }
}