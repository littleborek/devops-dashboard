package com.berk.devopsdashboard.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuditLogService {

    public void logCommand(String username, String action, String details) {
        log.info("[AUDIT] User: {} | Action: {} | Details: {}", username, action, details);
        // İlerleyen aşamalarda bu kayıtları veritabanına da yazacak şekilde genişletebiliriz.
    }
}
