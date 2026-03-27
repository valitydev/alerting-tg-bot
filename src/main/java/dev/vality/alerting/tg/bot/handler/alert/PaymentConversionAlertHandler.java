package dev.vality.alerting.tg.bot.handler.alert;

import dev.vality.alerting.tg.bot.config.properties.AlertBotProperties;
import dev.vality.alerting.tg.bot.model.Webhook;
import dev.vality.alerting.tg.bot.service.ProviderThreadService;
import dev.vality.alerting.tg.bot.service.TelegramApiService;
import dev.vality.alerting.tg.bot.util.WebhookUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static dev.vality.alerting.tg.bot.constant.AlertThreadName.PAYMENT_CONVERSION;

@Component
@RequiredArgsConstructor
public class PaymentConversionAlertHandler implements AlertHandler {
    private final AlertBotProperties properties;
    private final TelegramApiService telegramApiService;
    private final ProviderThreadService providerThreadService;

    @Override
    public boolean filter(String alertName) {
        return PAYMENT_CONVERSION.equals(alertName);
    }

    @Override
    public void handle(Webhook webhook, List<Webhook.Alert> alerts) {
        var alertsByThreadId = providerThreadService.groupAlertsByThreadId(alerts);
        alertsByThreadId.forEach((threadId, threadAlerts) -> {
            String message = buildMessage(threadAlerts);
            if (message.isBlank()) {
                return;
            }

            telegramApiService.sendMessage(
                    properties.getChatId(),
                    threadId,
                    message,
                    "MarkdownV2"
            );
        });
    }

    private String buildMessage(List<Webhook.Alert> threadAlerts) {
        return threadAlerts.stream()
                .map(WebhookUtil::formatPaymentConversionAlert)
                .filter(formattedAlert -> formattedAlert != null && !formattedAlert.isBlank())
                .collect(Collectors.joining("\n\n"));
    }
}
