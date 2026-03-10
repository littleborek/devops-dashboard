package com.berk.devopsdashboard.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "remote_tasks")
@Data
@NoArgsConstructor
public class RemoteTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id", nullable = false)
    private Server server;

    @Column(nullable = false)
    private String command;

    @Column(nullable = false)
    private String status; // PENDING, EXECUTED, FAILED

    @Column(columnDefinition = "TEXT")
    private String result;

    @Column(columnDefinition = "TEXT")
    private String signature;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime executedAt;

    public RemoteTask(Server server, String command) {
        this.server = server;
        this.command = command;
        this.status = "PENDING";
        this.createdAt = LocalDateTime.now();
    }
}
