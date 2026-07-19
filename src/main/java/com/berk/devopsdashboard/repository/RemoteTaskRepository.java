package com.berk.devopsdashboard.repository;

import com.berk.devopsdashboard.entity.RemoteTask;
import com.berk.devopsdashboard.entity.Server;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface RemoteTaskRepository extends JpaRepository<RemoteTask, Long> {

    List<RemoteTask> findByServerAndStatusOrderByCreatedAtAsc(Server server, String status);

    @Transactional
    @Modifying
    @Query("DELETE FROM RemoteTask r WHERE r.server.id = :serverId")
    void deleteByServerId(@Param("serverId") Long serverId);
}
