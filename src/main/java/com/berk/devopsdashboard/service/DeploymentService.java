package com.berk.devopsdashboard.service;

import com.berk.devopsdashboard.dto.request.DeploymentRequest;
import com.berk.devopsdashboard.entity.Deployment;
import java.util.List;

public interface DeploymentService {
    Deployment createDeployment(DeploymentRequest request);
    List<Deployment> getDeploymentsByServerId(Long serverId);
}