package com.berk.devopsdashboard.dto.request;

import com.berk.devopsdashboard.entity.KubernetesPod;
import lombok.Data;
import java.util.List;

@Data
public class AgentK8sSyncRequest {
    private String serverIp;   
    private String serverName;
    private List<KubernetesPod> pods; 
}