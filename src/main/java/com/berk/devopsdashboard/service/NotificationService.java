package com.berk.devopsdashboard.service;

import com.berk.devopsdashboard.entity.Server;
import com.berk.devopsdashboard.repository.SystemSettingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final SystemSettingRepository settingRepository;
    private final ObjectMapper objectMapper;
    private final TelegramNotificationService telegramNotificationService;

    @Async
    public void sendOfflineAlert(Server server) {
        String webhookUrl = settingRepository.findBySettingKey("discord_webhook_url")
                .map(setting -> setting.getSettingValue())
                .orElse("");
        if (webhookUrl == null || webhookUrl.length() < 10) {
            log.warn("Discord Webhook URL veritabanında bulunamadı veya geçersiz.");
            return;
        }

        try {
            String time = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
            String content = String.format(
                    "🚨 **ALARM: SUNUCU ÇÖKTÜ!** 🚨\n**Sunucu:** %s\n**IP:** %s\n**Kategori:** %s\n**Zaman:** %s",
                    server.getName(), server.getIpAddress(),
                    (server.getCategory() != null ? server.getCategory() : "Genel"), time);

            Map<String, String> payload = new HashMap<>();
            payload.put("content", content);
            String jsonPayload = objectMapper.writeValueAsString(payload);

            sendWebhook(webhookUrl, jsonPayload);
            log.info("Discord Bildirimi Gönderildi: {}", server.getName());

            telegramNotificationService.sendNotification(
                    "SERVER DOWN: " + server.getName(),
                    "IP: " + server.getIpAddress() + "\nCategory: "
                            + (server.getCategory() != null ? server.getCategory() : "General"),
                    "CRITICAL");

        } catch (Exception e) {
            log.error("Bildirim Gönderilemedi: {}", e.getMessage());
        }
    }

    @Async
    public void sendResourceAlert(Server server, String resourceType, double usage, double threshold) {
        String webhookUrl = settingRepository.findBySettingKey("discord_webhook_url")
                .map(setting -> setting.getSettingValue())
                .orElse("");

        if (webhookUrl == null || webhookUrl.length() < 10)
            return;

        try {
            String time = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
            String content = String.format(
                    "⚠️ **UYARI: YÜKSEK KAYNAK KULLANIMI** ⚠️\n**Sunucu:** %s\n**IP:** %s\n**Kaynak:** %s\n**Kullanım:** %%%.2f (Eşik: %%%.2f)\n**Zaman:** %s",
                    server.getName(), server.getIpAddress(), resourceType, usage, threshold, time);

            Map<String, String> payload = new HashMap<>();
            payload.put("content", content);
            String jsonPayload = objectMapper.writeValueAsString(payload);

            sendWebhook(webhookUrl, jsonPayload);
            log.info("Discord Kaynak Uyarısı Gönderildi: {}/{}", server.getName(), resourceType);

            telegramNotificationService.sendNotification(
                    "HIGH RESOURCE USAGE: " + server.getName(),
                    "Resource: " + resourceType + "\nUsage: " + String.format("%.2f%%", usage) + " (Threshold: "
                            + String.format("%.2f%%", threshold) + ")",
                    "WARNING");

        } catch (Exception e) {
            log.error("Resource Bildirimi Gönderilemedi: {}", e.getMessage());
        }
    }

    private void sendWebhook(String webhookUrl, String jsonPayload) throws Exception {
        URL url = new URL(webhookUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("User-Agent", "DevOpsDashboard");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int code = connection.getResponseCode();
        if (code != 204 && code != 200) {
            log.error("Discord API Hatası. Kod: {}", code);
        }
    }
}