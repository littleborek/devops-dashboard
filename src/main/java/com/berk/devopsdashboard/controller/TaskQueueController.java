package com.berk.devopsdashboard.controller;

import com.berk.devopsdashboard.dto.TaskDTO;
import com.berk.devopsdashboard.service.TaskQueueService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskQueueController {

    private final TaskQueueService taskQueueService;

    // Agent endpoints
    @GetMapping("/pending/{serverId}")
    public ResponseEntity<List<TaskDTO>> getPendingTasks(@PathVariable Long serverId, HttpServletRequest request) {
        String clientIp = getClientIp(request);
        return ResponseEntity.ok(taskQueueService.getPendingTasksForServer(serverId, clientIp));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<TaskDTO>> getPendingTasksByIp(HttpServletRequest request) {
        String clientIp = getClientIp(request);
        return ResponseEntity.ok(taskQueueService.getPendingTasksByIp(clientIp));
    }

    @PostMapping("/result/{taskId}")
    public ResponseEntity<Void> reportTaskResult(
            @PathVariable Long taskId,
            @RequestBody Map<String, String> payload) {

        String status = payload.getOrDefault("status", "EXECUTED");
        String result = payload.getOrDefault("result", "");

        taskQueueService.reportTaskResult(taskId, result, status);
        return ResponseEntity.ok().build();
    }

    // Dashboard endpoints
    @PostMapping("/queue/{serverId}")
    public ResponseEntity<Map<String, Long>> queueTask(
            @PathVariable Long serverId,
            @RequestBody Map<String, String> payload) {
        String command = payload.get("command");
        if (command == null || command.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        Long taskId = taskQueueService.queueTask(serverId, command);
        return ResponseEntity.ok(Map.of("taskId", taskId));
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @GetMapping("/result/{taskId}")
    public ResponseEntity<Map<String, Object>> getTaskStatus(@PathVariable Long taskId) {
        return ResponseEntity.ok(taskQueueService.getTaskStatus(taskId));
    }
}
