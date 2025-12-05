package com.berk.devopsdashboard.repository;

import com.berk.devopsdashboard.entity.KubernetesPod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface KubernetesPodRepository extends JpaRepository<KubernetesPod, Long> {
    
    Optional<KubernetesPod> findByUid(String uid);
    @Transactional
    void deleteByServerId(Long serverId);
}