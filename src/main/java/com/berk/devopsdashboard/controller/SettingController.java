package com.berk.devopsdashboard.controller;

import com.berk.devopsdashboard.entity.SystemSetting;
import com.berk.devopsdashboard.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class SettingController {

    private final SystemSettingRepository settingRepository;

    @GetMapping("/{key}")
    public ResponseEntity<String> getSetting(@PathVariable String key) {
        return ResponseEntity.ok(
            settingRepository.findBySettingKey(key)
                .map(SystemSetting::getSettingValue)
                .orElse("")
        );
    }

    @PostMapping
    public ResponseEntity<?> saveSetting(@RequestBody Map<String, String> payload) {
        String key = payload.get("key");
        String value = payload.get("value");

        SystemSetting setting = settingRepository.findBySettingKey(key)
                .orElse(SystemSetting.builder().settingKey(key).build());
        
        setting.setSettingValue(value);
        settingRepository.save(setting);
        
        return ResponseEntity.ok().build();
    }
}