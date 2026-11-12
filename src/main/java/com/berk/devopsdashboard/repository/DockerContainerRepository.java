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

    String CURRENT_INVENTORY = "d.containerId IS NOT NULL "
            + "AND function('regexp_replace', d.containerId, '^\\s+$', '') <> '' "
            + "AND (d.state IS NULL OR lower(d.state) <> 'removed')";

    Optional<DockerContainer> findByContainerId(String containerId);

    List<DockerContainer> findByServerId(Long serverId);

    @Query("SELECT d FROM DockerContainer d WHERE " + CURRENT_INVENTORY)
    List<DockerContainer> findCurrent();

    @Query("SELECT d FROM DockerContainer d WHERE d.server.id = :serverId AND " + CURRENT_INVENTORY)
    List<DockerContainer> findCurrentByServerId(@Param("serverId") Long serverId);

    @Transactional
    @Modifying
    @Query("DELETE FROM DockerContainer d WHERE d.server.id = :serverId")
    void deleteByServerId(@Param("serverId") Long serverId);

    @Query("SELECT count(d) FROM DockerContainer d WHERE d.server.ipAddress = :ipAddress AND " + CURRENT_INVENTORY)
    long countCurrentByServerIp(@Param("ipAddress") String ipAddress);
}
