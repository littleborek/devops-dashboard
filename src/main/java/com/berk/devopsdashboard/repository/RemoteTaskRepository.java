package com.berk.devopsdashboard.repository;

import com.berk.devopsdashboard.entity.RemoteTask;
import com.berk.devopsdashboard.entity.Server;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RemoteTaskRepository extends JpaRepository<RemoteTask, Long> {

    List<RemoteTask> findByServerAndStatusOrderByCreatedAtAsc(Server server, String status);
}
