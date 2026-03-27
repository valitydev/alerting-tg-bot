package dev.vality.alerting.tg.bot.service;

import dev.vality.alerting.tg.bot.config.properties.AlertBotProperties;
import dev.vality.alerting.tg.bot.dao.ProviderThreadDao;
import dev.vality.alerting.tg.bot.exception.TelegramThreadCreationException;
import dev.vality.alerting.tg.bot.model.Webhook;
import dev.vality.alerting.tg.bot.pojo.ProviderThread;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderThreadService {
    private final ProviderThreadDao providerThreadDao;
    private final TelegramApiService telegramApiService;
    private final AlertBotProperties properties;

    public Integer getOrCreateThreadId(Webhook.Alert alert) {
        var labels = alert.getLabels();
        String providerId = labels.getProviderId();
        String providerName = labels.getProviderName();

        var existing = providerThreadDao.findByProvider(providerId);
        if (existing.isPresent() && existing.get().getThreadId() != null) {
            return existing.get().getThreadId();
        }

        String threadName = providerId + ": " + providerName;
        Integer threadId;
        try {
            threadId = telegramApiService.createTopicAndReturnThreadId(threadName);
        } catch (TelegramThreadCreationException e) {
            log.warn("Не удалось получить или создать threadId для алерта. Алерт будет отправлен в командный топик. {}",
                    e.getMessage(), e);
            threadId = properties.getThreads().getCommands();
        }

        var entity = new ProviderThread(
                null,
                threadId,
                providerId,
                labels.getProviderName(),
                threadName
        );
        providerThreadDao.upsert(entity);

        return threadId;
    }

    public Map<Integer, List<Webhook.Alert>> groupAlertsByThreadId(List<Webhook.Alert> alerts) {
        Map<Integer, List<Webhook.Alert>> alertsByThreadId = new HashMap<>();
        if (alerts == null || alerts.isEmpty()) {
            return alertsByThreadId;
        }

        for (Webhook.Alert alert : alerts) {
            Integer threadId = getOrCreateThreadId(alert);
            alertsByThreadId.computeIfAbsent(threadId, key -> new ArrayList<>()).add(alert);
        }

        return alertsByThreadId;
    }
}
