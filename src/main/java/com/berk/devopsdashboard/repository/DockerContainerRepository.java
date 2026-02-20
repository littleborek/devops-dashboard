package com.berk.devopsdashboard.repository;

import com.berk.devopsdashboard.entity.DockerContainer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface DockerContainerRepository extends JpaRepository<DockerContainer, Long> {

    Optional<DockerContainer> findByContainerId(String containerId);

    List<DockerContainer> findByServerId(Long serverId);

    @Transactional
    @Modifying
    @Query("DELETE FROM DockerContainer d WHERE d.server.id = :serverId")
    void deleteByServerId(@Param("serverId") Long serverId);

    long countByServer_IpAddress(String ipAddress);
}