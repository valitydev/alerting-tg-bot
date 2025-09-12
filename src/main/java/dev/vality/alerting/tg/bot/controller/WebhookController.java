package dev.vality.alerting.tg.bot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.vality.alerting.tg.bot.config.properties.AlertmanagerWebhookProperties;
import dev.vality.alerting.tg.bot.model.Webhook;
import dev.vality.alerting.tg.bot.service.AlertBot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpServletRequest;

import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/alertmanager")
@RequiredArgsConstructor
public class WebhookController {
    private final AlertmanagerWebhookProperties alertmanagerWebhookProperties;
    private final ObjectMapper objectMapper;
    private final AlertBot alertBot;

    @PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> processWebhook(HttpServletRequest servletRequest) {
        try {
            var webhookBody = servletRequest.getReader().lines().collect(Collectors.joining(" "));
            log.info("Received webhook from alertmanager: {}", webhookBody);
            var webhook = objectMapper.readValue(webhookBody, Webhook.class);
            alertBot.sendAlertMessage(webhook);
        } catch (Exception e) {
            log.error("Unexpected error during webhook parsing:", e);
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok().build();
    }
}
