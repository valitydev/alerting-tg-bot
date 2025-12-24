package dev.vality.alerting.tg.bot.service;

import dev.vality.alerting.tg.bot.config.properties.AlertBotProperties;
import dev.vality.alerting.tg.bot.dao.ProviderTerminalThreadDao;
import dev.vality.alerting.tg.bot.exception.TelegramThreadCreationException;
import dev.vality.alerting.tg.bot.model.Webhook;
import dev.vality.alerting.tg.bot.pojo.ProviderTerminalThread;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FailedMachinesAlertService {
    private final ProviderTerminalThreadDao providerTerminalThreadDao;
    private final TelegramApiService telegramApiService;
    private final AlertBotProperties properties;

    public Integer getOrCreateTopicIdForFailedMachinesAlert(Webhook.Alert alert) {
        var labels = alert.getLabels();
        String providerId = labels.getProviderId();
        String providerName = labels.getProviderName();
        String terminalId = labels.getTerminalId();
        String terminalName = labels.getTerminalName();

        var existing = providerTerminalThreadDao.findByProviderAndTerminal(providerId, terminalId);
        if (existing.isPresent() && existing.get().getThreadId() != null) {
            return existing.get().getThreadId();
        }

        String threadName = "(" + providerId + ") " + providerName + " - (" + terminalId + ") " + terminalName;
        Integer threadId;
        try {
            threadId = telegramApiService.createTopicAndReturnThreadId(threadName);
        } catch (TelegramThreadCreationException e) {
            log.warn("Не удалось получить или создать threadId для алерта. Алерт будет отправлен в командный топик. {}",
                    e.getMessage(), e);
            threadId = properties.getThreads().getCommands();
        }

        var entity = new ProviderTerminalThread(
                null,
                threadId,
                providerId,
                terminalId,
                labels.getProviderName(),
                labels.getTerminalName(),
                threadName
        );
        providerTerminalThreadDao.upsert(entity);

        return threadId;
    }
}
