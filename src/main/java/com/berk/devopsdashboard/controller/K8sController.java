package com.berk.devopsdashboard.controller;

import com.berk.devopsdashboard.dto.response.K8sPodDTO;
import com.berk.devopsdashboard.service.K8sService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/k8s")
@RequiredArgsConstructor
public class K8sController {

    private final K8sService k8sService;

    @GetMapping("/pods")
    public List<K8sPodDTO> getPods() {
        return k8sService.listAllPods();
    }
}