package com.berk.devopsdashboard.service.impl;

import com.berk.devopsdashboard.dto.request.DeploymentRequest;
import com.berk.devopsdashboard.entity.Deployment;
import com.berk.devopsdashboard.entity.Server;
import com.berk.devopsdashboard.entity.enums.DeploymentStatus;
import com.berk.devopsdashboard.repository.DeploymentRepository;
import com.berk.devopsdashboard.repository.ServerRepository;
import com.berk.devopsdashboard.service.DeploymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeploymentServiceImpl implements DeploymentService {

    private final DeploymentRepository deploymentRepository;
    private final ServerRepository serverRepository;

    @Override
    public Deployment createDeployment(DeploymentRequest request) {
        Server server = serverRepository.findById(request.getServerId())
                .orElseThrow(() -> new RuntimeException("Sunucu bulunamadı ID: " + request.getServerId()));

        Deployment deployment = Deployment.builder()
                .applicationName(request.getApplicationName())
                .version(request.getVersion())
                .logs(request.getLogs())
                .status(DeploymentStatus.SUCCESS)
                .triggeredBy("Admin")
                .server(server)
                .build();

 
        return deploymentRepository.save(deployment);
    }

    @Override
    public List<Deployment> getDeploymentsByServerId(Long serverId) {
        return deploymentRepository.findByServerIdOrderByCreatedAtDesc(serverId);
    }
}