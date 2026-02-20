package com.berk.devopsdashboard.repository;

import com.berk.devopsdashboard.entity.Deployment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface DeploymentRepository extends JpaRepository<Deployment, Long> {

    List<Deployment> findByServerIdOrderByCreatedAtDesc(Long serverId);

    @Transactional
    @Modifying
    @Query("DELETE FROM Deployment d WHERE d.server.id = :serverId")
    void deleteByServerId(@Param("serverId") Long serverId);
}