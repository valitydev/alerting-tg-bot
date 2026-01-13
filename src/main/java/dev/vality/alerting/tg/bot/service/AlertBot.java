package dev.vality.alerting.tg.bot.service;

import dev.vality.alerting.tg.bot.config.properties.AlertBotProperties;
import dev.vality.alerting.tg.bot.handler.alert.UnknownAlertHandler;
import dev.vality.alerting.tg.bot.handler.command.TelegramCommandHandler;
import dev.vality.alerting.tg.bot.handler.alert.AlertHandler;
import dev.vality.alerting.tg.bot.model.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.*;
import java.util.stream.Collectors;

import static dev.vality.alerting.tg.bot.util.WebhookUtil.formatWebhook;

@Slf4j
@Service
@RequiredArgsConstructor
@EnableScheduling
public class AlertBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private final List<TelegramCommandHandler> eventHandlers;
    private final UnknownAlertHandler unknownAlertHandler;
    private final List<AlertHandler> alertHandlers;
    private final AlertBotProperties properties;
    private final TelegramApiService telegramApiService;

    @Override
    public String getBotToken() {
        return properties.getToken();
    }

    @Override
    public LongPollingSingleThreadUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {
        if (update == null || !update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        log.debug("Получено сообщение: message={}, chatId={}, threadId={}, user=@{}, text='{}'",
                update.getMessage(),
                update.getMessage().getChatId(),
                update.getMessage().getMessageThreadId(),
                update.getMessage().getFrom() != null ? update.getMessage().getFrom().getUserName() : null,
                update.getMessage().getText().replace('\n', ' ')
        );

        try {
            for (TelegramCommandHandler handler : eventHandlers) {
                if (handler.filter(update)) {
                    handler.handle(update);
                    return;
                }
            }
        } catch (Exception e) {
            log.error("Неизвестная ошибка при обработке update: updateId={}",
                    update.getUpdateId(), e);
        }
    }

    public void sendAlertMessages(Webhook webhook) {
        if (webhook == null || webhook.getAlerts() == null || webhook.getAlerts().isEmpty()) {
            return;
        }

        var alertsByAlertName = webhook.getAlerts().stream()
                .filter(a -> a.getLabels() != null)
                .filter(a -> a.getLabels().getAlertname() != null)
                .collect(Collectors.groupingBy(a -> a.getLabels().getAlertname()));

        alertsByAlertName.forEach((alertName, alerts) -> {
            AlertHandler handler = alertHandlers.stream()
                    .filter(h -> h.filter(alertName))
                    .findFirst()
                    .orElse(unknownAlertHandler);

            try {
                handler.handle(webhook, alerts);
            } catch (Exception e) {
                log.warn("Не удалось обработать алерт. alertName={}, alertsCount={}, handler={}",
                        alertName, alerts.size(), handler.getClass().getSimpleName(), e);

                telegramApiService.sendMessage(
                        properties.getChatId(),
                        properties.getThreads().getCommands(),
                        formatWebhook(webhook),
                        "MarkdownV2"
                );
            }
        });
    }
}
