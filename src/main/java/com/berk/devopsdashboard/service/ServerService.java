package com.berk.devopsdashboard.service;

import com.berk.devopsdashboard.dto.request.ServerRequest;
import com.berk.devopsdashboard.dto.response.ServerResponse;
import com.berk.devopsdashboard.entity.Server;
import com.berk.devopsdashboard.entity.enums.ServerStatus;

import java.util.List;

public interface ServerService {
    ServerResponse createServer(ServerRequest request);
    List<ServerResponse> getAllServers();
	void deleteServer(Long id);
	ServerResponse getServerById(Long id);
	ServerStatus getServerStatus(String address);
	ServerResponse updateServer(Long id, ServerRequest request);
	ServerStatus checkServerStatus(Server server);
}