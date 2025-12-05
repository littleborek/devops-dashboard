package com.berk.devopsdashboard.dto.request;

import com.berk.devopsdashboard.entity.DockerContainer;
import lombok.Data;
import java.util.List;

@Data
public class AgentSyncRequest {
    private String serverIp;
    private String serverName;
    private Double cpuUsage;
    private Double ramUsage;
    private String totalRam;
    
    private List<DockerContainer> containers;
}