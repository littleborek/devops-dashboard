package com.berk.devopsdashboard.repository;

import com.berk.devopsdashboard.entity.Server;
import com.berk.devopsdashboard.entity.enums.ServerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ServerRepository extends JpaRepository<Server, Long> {
    
    Optional<Server> findByIpAddress(String ipAddress);

    Optional<Server> findByName(String name);

    @Transactional
    @Modifying
    @Query("UPDATE Server s SET s.status = :status, s.lastResponseTime = :latency "
            + "WHERE s.id = :id AND s.updatedAt = :observedUpdate")
    int updateProbeResult(Long id, LocalDateTime observedUpdate, ServerStatus status, Integer latency);
}
