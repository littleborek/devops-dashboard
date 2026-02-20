package com.berk.devopsdashboard.repository;

import com.berk.devopsdashboard.entity.ServerHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ServerHistoryRepository extends JpaRepository<ServerHistory, Long> {

    @Transactional
    @Modifying
    @Query("DELETE FROM ServerHistory h WHERE h.server.id = :serverId")
    void deleteByServerId(@Param("serverId") Long serverId);

    List<ServerHistory> findTop50ByServerIdOrderByCheckTimeDesc(Long serverId);
}