package com.berk.devopsdashboard.controller;

import com.berk.devopsdashboard.service.DeploymentService;
import com.berk.devopsdashboard.service.ServerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
@RequiredArgsConstructor
public class DashboardController {

    private final ServerService serverService;
    private final DeploymentService deploymentService;

    @GetMapping
    public String showDashboard(Model model) {
        model.addAttribute("servers", serverService.getAllServers());
        return "dashboard"; 
    }


    @GetMapping("/server/{id}")
    public String showServerDetail(@PathVariable Long id, Model model) {
        model.addAttribute("server", serverService.getServerById(id));
        model.addAttribute("deployments", deploymentService.getDeploymentsByServerId(id));
        
        return "server-detail"; 
    }
}