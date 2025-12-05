package com.berk.devopsdashboard.repository;

import com.berk.devopsdashboard.entity.DockerContainer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface DockerContainerRepository extends JpaRepository<DockerContainer, Long> {
    
    Optional<DockerContainer> findByContainerId(String containerId);
    
    List<DockerContainer> findByServerId(Long serverId);
    @Transactional
    void deleteByServerId(Long serverId);
}