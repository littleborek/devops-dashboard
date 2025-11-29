package com.berk.devopsdashboard.controller;

import com.berk.devopsdashboard.dto.request.DeploymentRequest;
import com.berk.devopsdashboard.entity.Deployment;
import com.berk.devopsdashboard.service.DeploymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/deployments")
@RequiredArgsConstructor
public class DeploymentController {

    private final DeploymentService deploymentService;

    @PostMapping
    public ResponseEntity<Deployment> createDeployment(@Valid @RequestBody DeploymentRequest request) {
        return new ResponseEntity<>(deploymentService.createDeployment(request), HttpStatus.CREATED);
    }

    @GetMapping("/server/{serverId}")
    public ResponseEntity<List<Deployment>> getDeploymentsByServer(@PathVariable Long serverId) {
        return ResponseEntity.ok(deploymentService.getDeploymentsByServerId(serverId));
    }
}