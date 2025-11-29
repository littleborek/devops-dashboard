package com.berk.devopsdashboard.controller;



import com.berk.devopsdashboard.dto.request.ServerRequest;
import com.berk.devopsdashboard.dto.response.ServerResponse;
import com.berk.devopsdashboard.service.ServerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.berk.devopsdashboard.entity.ServerHistory;
import com.berk.devopsdashboard.repository.ServerHistoryRepository;

import java.util.List;

@RestController
@RequestMapping("/api/v1/servers")
@RequiredArgsConstructor
public class ServerController {

    private final ServerService serverService;
    private final ServerHistoryRepository historyRepository;

    @PostMapping
    public ResponseEntity<ServerResponse> createServer(@Valid @RequestBody ServerRequest request) {
        return new ResponseEntity<>(serverService.createServer(request), HttpStatus.CREATED);
    }


    @GetMapping
    public ResponseEntity<List<ServerResponse>> getAllServers() {
        return ResponseEntity.ok(serverService.getAllServers());
    }
    


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteServer(@PathVariable Long id) {
        serverService.deleteServer(id);
        return ResponseEntity.noContent().build(); 
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ServerResponse> updateServer(@PathVariable Long id, @Valid @RequestBody ServerRequest request) {
        return ResponseEntity.ok(serverService.updateServer(id, request));
    }
    
    @GetMapping("/{id}/history")
    public ResponseEntity<List<ServerHistory>> getServerHistory(@PathVariable Long id) {
        return ResponseEntity.ok(historyRepository.findTop50ByServerIdOrderByCheckTimeDesc(id));
    }
}
