package com.berk.devopsdashboard.service;

import com.berk.devopsdashboard.dto.TaskDTO;
import com.berk.devopsdashboard.entity.RemoteTask;
import com.berk.devopsdashboard.entity.Server;
import com.berk.devopsdashboard.repository.RemoteTaskRepository;
import com.berk.devopsdashboard.repository.ServerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskQueueService {

    private final RemoteTaskRepository remoteTaskRepository;
    private final ServerRepository serverRepository;

    @Transactional(readOnly = true)
    public List<TaskDTO> getPendingTasksForServer(Long serverId, String clientIp) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new IllegalArgumentException("Server not found"));

        // Perform IP validation here.
        if (!server.getIpAddress().equals(clientIp)) {
            log.warn("Unauthorized task poll from IP {} for Server IP {}", clientIp, server.getIpAddress());
            throw new SecurityException("IP Address mismatch");
        }

        List<RemoteTask> tasks = remoteTaskRepository.findByServerAndStatusOrderByCreatedAtAsc(server, "PENDING");
        return tasks.stream()
                .map(t -> new TaskDTO(t.getId(), t.getCommand()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskDTO> getPendingTasksByIp(String clientIp) {
        Server server = serverRepository.findByIpAddress(clientIp)
                .orElseThrow(() -> new IllegalArgumentException("Server not found for IP: " + clientIp));

        List<RemoteTask> tasks = remoteTaskRepository.findByServerAndStatusOrderByCreatedAtAsc(server, "PENDING");
        return tasks.stream()
                .map(t -> new TaskDTO(t.getId(), t.getCommand()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void reportTaskResult(Long taskId, String result, String status) {
        RemoteTask task = remoteTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        task.setResult(result);
        task.setStatus(status != null ? status.toUpperCase() : "EXECUTED");
        task.setExecutedAt(LocalDateTime.now());

        remoteTaskRepository.save(task);
        log.info("Task {} result updated. Status: {}", taskId, status);
    }

    @Transactional
    public Long queueTask(Long serverId, String command) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new IllegalArgumentException("Server not found"));

        RemoteTask task = new RemoteTask(server, command);
        task = remoteTaskRepository.save(task);
        log.info("Queued task {} for server {}", task.getId(), serverId);
        return task.getId();
    }

    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getTaskStatus(Long taskId) {
        RemoteTask task = remoteTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));
        return java.util.Map.of(
                "id", task.getId(),
                "status", task.getStatus(),
                "result", task.getResult() != null ? task.getResult() : "");
    }
}
