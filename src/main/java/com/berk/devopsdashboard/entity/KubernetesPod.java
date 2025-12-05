package com.berk.devopsdashboard.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "kubernetes_pods")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KubernetesPod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String uid;

    private String name;
    
    private String namespace;
    
    private String nodeName;

    private String status;

    private Integer restartCount;

    private LocalDateTime lastUpdated;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id")
    @JsonIgnore
    private Server server;
}