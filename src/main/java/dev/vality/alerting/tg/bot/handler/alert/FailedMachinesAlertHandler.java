package dev.vality.alerting.tg.bot.handler.alert;

import dev.vality.alerting.tg.bot.config.properties.AlertBotProperties;
import dev.vality.alerting.tg.bot.model.Webhook;
import dev.vality.alerting.tg.bot.service.ProviderThreadService;
import dev.vality.alerting.tg.bot.service.TelegramApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.vality.alerting.tg.bot.constant.AlertThreadName.FAILED_MACHINES;
import static dev.vality.alerting.tg.bot.util.WebhookUtil.formatWebhook;

@Slf4j
@Component
@RequiredArgsConstructor
public class FailedMachinesAlertHandler implements AlertHandler {
    private final AlertBotProperties properties;
    private final TelegramApiService telegramApiService;
    private final ProviderThreadService service;

    @Override
    public boolean filter(String alertName) {
        return FAILED_MACHINES.equals(alertName);
    }

    @Override
    public void handle(Webhook webhook, List<Webhook.Alert> alerts) {
        Map<Integer, List<Webhook.Alert>> threadIds = new HashMap<>();

        for (Webhook.Alert alert : alerts) {
            Integer threadId = service.getOrCreateTopicId(alert);
            threadIds.computeIfAbsent(threadId, key -> new ArrayList<>()).add(alert);
        }

        threadIds.forEach((threadId, threadAlerts) -> {
            telegramApiService.sendMessage(
                    properties.getChatId(),
                    threadId,
                    formatWebhook(webhook, threadAlerts),
                    "MarkdownV2"
            );
        });
    }
}
