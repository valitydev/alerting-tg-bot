package dev.vality.alerting.tg.bot.handler.alert;

import dev.vality.alerting.tg.bot.config.properties.AlertBotProperties;
import dev.vality.alerting.tg.bot.model.Webhook;
import dev.vality.alerting.tg.bot.service.TelegramApiService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static dev.vality.alerting.tg.bot.constant.AlertThreadName.*;
import static dev.vality.alerting.tg.bot.constant.AlertThreadName.PENDING_PAYMENTS;
import static dev.vality.alerting.tg.bot.util.WebhookUtil.formatWebhook;

@Slf4j
@Component
@RequiredArgsConstructor
public class UnknownAlertHandler implements AlertHandler {
    private final AlertBotProperties properties;
    private final TelegramApiService telegramApiService;
    private Map<String, Integer> alertThreads;

    @PostConstruct
    public void init() {
        alertThreads = buildAlertThreadMap();
    }

    @Override
    public boolean filter(String alertName) {
        return false;
    }

    @Override
    public void handle(Webhook webhook, List<Webhook.Alert> alerts) {
        if (alerts == null || alerts.isEmpty()) {
            return;
        }

        String alertName = alerts.get(0).getLabels() != null ? alerts.get(0).getLabels().getAlertname() : null;

        Integer threadId = alertThreads.get(alertName);
        if (threadId == null) {
            log.error("Неизвестный тип алерта, alertName=" + alertName);
            threadId = properties.getThreads().getCommands();
        }

        telegramApiService.sendMessage(
                properties.getChatId(),
                threadId,
                formatWebhook(webhook),
                "MarkdownV2"
        );
    }

    private Map<String, Integer> buildAlertThreadMap() {
        return Map.of(
                API_ERROR_HTTP_CODE_INCREASE, properties.getThreads().getErrors5xx(),
                ALT_PAY_CONVERSION, properties.getThreads().getAltpayConversion(),
                FAILED_MACHINES, properties.getThreads().getFailedMachines(),
                PENDING_PAYMENTS, properties.getThreads().getPendingPayments()
        );
    }
}
