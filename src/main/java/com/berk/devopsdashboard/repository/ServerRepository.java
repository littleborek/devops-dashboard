package com.berk.devopsdashboard.repository;

import com.berk.devopsdashboard.entity.Server;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ServerRepository extends JpaRepository<Server, Long> {
    
    Optional<Server> findByIpAddress(String ipAddress);

    Optional<Server> findByName(String name);
}