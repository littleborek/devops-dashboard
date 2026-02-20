package com.berk.devopsdashboard.service;

import com.github.dockerjava.api.model.Container;
import java.util.List;

public interface LogService {
    String getLogs(Long serverId, String containerId);

    List<Container> getRunningContainers(Long serverId);
}
