package com.berk.devopsdashboard.controller;

import com.berk.devopsdashboard.entity.DockerContainer;

//import com.berk.devopsdashboard.entity.KubernetesPod;
import com.berk.devopsdashboard.repository.DockerContainerRepository;
//import com.berk.devopsdashboard.repository.KubernetesPodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Collections;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class OrchestrationController {

    private final DockerContainerRepository dockerRepository;
//    private final KubernetesPodRepository k8sRepository; 

    @GetMapping("/docker")
    public ResponseEntity<List<DockerContainer>> getDockerContainers() {
        List<DockerContainer> containers = dockerRepository.findAll();
        return ResponseEntity.ok(containers);
    }
    
    @GetMapping("/docker/server/{serverId}")
    public ResponseEntity<List<DockerContainer>> getDockerContainersByServer(@PathVariable Long serverId) {
        return ResponseEntity.ok(dockerRepository.findByServerId(serverId));
    }
}

    /*
    @GetMapping("/k8s")
    public ResponseEntity<List<KubernetesPod>> getK8sPods() {
        return ResponseEntity.ok(k8sRepository.findAll());
    }
}
    */