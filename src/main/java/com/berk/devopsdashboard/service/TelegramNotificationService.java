package com.berk.devopsdashboard.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.berk.devopsdashboard.repository.SystemSettingRepository;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class TelegramNotificationService {

    @Value("${telegram.bot.token:}")
    private String defaultBotToken;

    @Value("${telegram.chat.id:}")
    private String defaultChatId;

    private final RestTemplate restTemplate = new RestTemplate();
    private final SystemSettingRepository settingRepository;

    public TelegramNotificationService(SystemSettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    public void sendNotification(String title, String message, String level) {
        String token = settingRepository.findBySettingKey("TELEGRAM_BOT_TOKEN")
                .map(s -> s.getSettingValue()).orElse(defaultBotToken);
        String chatId = settingRepository.findBySettingKey("TELEGRAM_CHAT_ID")
                .map(s -> s.getSettingValue()).orElse(defaultChatId);

        if (token == null || token.trim().isEmpty() || chatId == null || chatId.trim().isEmpty()) {
            log.debug("Telegram configuration is missing. Skipping notification.");
            return;
        }

        String url = String.format("https://api.telegram.org/bot%s/sendMessage", token);

        String icon = switch (level.toUpperCase()) {
            case "CRITICAL" -> "🚨";
            case "WARNING" -> "⚠️";
            case "INFO" -> "ℹ️";
            case "SUCCESS" -> "✅";
            default -> "🔔";
        };

        String formattedMessage = String.format("%s *%s*\n\n%s", icon, title, message);

        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", formattedMessage);
        body.put("parse_mode", "Markdown");

        try {
            restTemplate.postForEntity(url, body, String.class);
            log.info("Telegram notification sent successfully: {}", title);
        } catch (Exception e) {
            log.error("Failed to send Telegram notification", e);
        }
    }
}
