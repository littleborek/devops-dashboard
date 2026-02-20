package com.berk.devopsdashboard.service;

import com.berk.devopsdashboard.entity.Server;
import com.berk.devopsdashboard.repository.SystemSettingRepository; // BU İMPORT ÇOK ÖNEMLİ
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SystemSettingRepository settingRepository;

    public void sendOfflineAlert(Server server) {

        String webhookUrl = settingRepository.findBySettingKey("discord_webhook_url")
                .map(setting -> setting.getSettingValue())
                .orElse("");
        if (webhookUrl == null || webhookUrl.length() < 10) {
            System.out.println("Discord Webhook URL veritabanında bulunamadı veya geçersiz.");
            return;
        }

        try {
            String time = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));

            String jsonPayload = "{"
                    + "\"content\": \"🚨 **ALARM: SUNUCU ÇÖKTÜ!** 🚨\\n"
                    + "**Sunucu:** " + server.getName() + "\\n"
                    + "**IP:** " + server.getIpAddress() + "\\n"
                    + "**Kategori:** " + (server.getCategory() != null ? server.getCategory() : "Genel") + "\\n"
                    + "**Zaman:** " + time + "\""
                    + "}";

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
            if (code == 204) {
                System.out.println("Discord Bildirimi Başarılı!");
            } else {
                System.out.println("Discord Hatası. Kod: " + code);
            }

        } catch (Exception e) {
            System.err.println("Bildirim Gönderilemedi: " + e.getMessage());
        }
    }

    public void sendResourceAlert(Server server, String resourceType, double usage, double threshold) {
        String webhookUrl = settingRepository.findBySettingKey("discord_webhook_url")
                .map(setting -> setting.getSettingValue())
                .orElse("");

        if (webhookUrl == null || webhookUrl.length() < 10)
            return;

        try {
            String time = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));

            String jsonPayload = "{"
                    + "\"content\": \"⚠️ **UYARI: YÜKSEK KAYNAK KULLANIMI** ⚠️\\n"
                    + "**Sunucu:** " + server.getName() + "\\n"
                    + "**IP:** " + server.getIpAddress() + "\\n"
                    + "**Kaynak:** " + resourceType + "\\n"
                    + "**Kullanım:** %" + usage + " (Eşik: %" + threshold + ")\\n"
                    + "**Zaman:** " + time + "\""
                    + "}";

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
            if (code != 204) {
                System.out.println("Discord Resource Alert Hatası. Kod: " + code);
            }

        } catch (Exception e) {
            System.err.println("Resource Bildirimi Gönderilemedi: " + e.getMessage());
        }
    }
}