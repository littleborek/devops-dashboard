package com.berk.devopsdashboard.service;

import com.berk.devopsdashboard.entity.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Service
public class NotificationService {

    @Value("${discord.webhook-url}")
    private String webhookUrl;

    public void sendOfflineAlert(Server server) {
        if (webhookUrl == null || webhookUrl.isBlank() || webhookUrl.contains("BURAYA_KOPYALA")) {
            System.out.println("Discord Webhook URL ayarlanmamış, bildirim atılmadı.");
            return;
        }

        try {

            String jsonPayload = "{"
                    + "\"content\": \"🚨 **ALARM: SUNUCU ÇÖKTÜ!** 🚨\\n"
                    + "**Sunucu:** " + server.getName() + "\\n"
                    + "**IP:** " + server.getIpAddress() + "\\n"
                    + "**Lokasyon:** " + server.getLocation() + "\\n"
                    + "**Zaman:** " + java.time.LocalDateTime.now() + "\""
                    + "}";

            URL url = new URL(webhookUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "Java-Discord-Webhook");
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int code = connection.getResponseCode();
            System.out.println("Discord Bildirimi Gönderildi! Kod: " + code);

        } catch (Exception e) {
            System.err.println("Discord Bildirim Hatası: " + e.getMessage());
        }
    }
}