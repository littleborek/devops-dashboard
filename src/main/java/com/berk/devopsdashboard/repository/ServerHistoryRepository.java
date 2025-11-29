package com.berk.devopsdashboard.repository;

import com.berk.devopsdashboard.entity.ServerHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ServerHistoryRepository extends JpaRepository<ServerHistory, Long> {
    List<ServerHistory> findTop50ByServerIdOrderByCheckTimeDesc(Long serverId);
}