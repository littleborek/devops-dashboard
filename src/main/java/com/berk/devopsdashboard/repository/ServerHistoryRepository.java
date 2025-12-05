package com.berk.devopsdashboard.repository;

import com.berk.devopsdashboard.entity.ServerHistory;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional; // Toplu silme için

@Repository
public interface ServerHistoryRepository extends JpaRepository<ServerHistory, Long> {
    @Transactional
    void deleteByServerId(Long serverId);
    
    List<ServerHistory> findTop50ByServerIdOrderByCheckTimeDesc(Long serverId);
}