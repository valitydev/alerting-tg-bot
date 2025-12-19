package dev.vality.alerting.tg.bot.service;

import dev.vality.alerting.tg.bot.config.properties.AlertBotProperties;
import dev.vality.alerting.tg.bot.dao.ProviderTerminalThreadDao;
import dev.vality.alerting.tg.bot.exception.TelegramThreadCreationException;
import dev.vality.alerting.tg.bot.model.Webhook;
import dev.vality.alerting.tg.bot.pojo.ProviderTerminalThread;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.forum.CreateForumTopic;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.*;
import java.util.stream.Collectors;

import static dev.vality.alerting.tg.bot.constant.AlertThreadName.*;
import static dev.vality.alerting.tg.bot.util.WebhookUtil.formatWebhook;

@Slf4j
@Service
@RequiredArgsConstructor
@EnableScheduling
public class AlertBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private final AlertBotProperties properties;
    private final TelegramClient telegramClient;
    private final ProviderTerminalThreadDao providerTerminalThreadDao;
    private static final Map<Long, List<String>> activeThreads = new HashMap<>();
    private static final Set<Long> waitingForTopicName = new HashSet<>();
    private Map<String, Integer> alertThreads;

    @PostConstruct
    public void init() {
        alertThreads = buildAlertThreadMap();
    }

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
        if (update.hasMessage() && update.getMessage().hasText()) {

            log.debug("Получено сообщение: message={}, chatId={}, threadId={}, user=@{}, text='{}'",
                    update.getMessage(),
                    update.getMessage().getChatId(),
                    update.getMessage().getMessageThreadId(),
                    update.getMessage().getFrom() != null ? update.getMessage().getFrom().getUserName() : null,
                    update.getMessage().getText().replace('\n', ' ')
            );

            Message message = update.getMessage();
            Long chatId = message.getChatId();
            Integer threadId = message.getMessageThreadId();

            // ❗ Фильтруем команды — они должны выполняться только в командном топике
            if (!threadId.equals(properties.getThreads().getCommands())) {
                return;
            }

            String text = message.getText();
            if (text.startsWith("/create_alert_topic")) {
                promptForTopicName(chatId);
            } else if (waitingForTopicName.contains(chatId)) {
                createThread(chatId, text);
            } else if (text.startsWith("/delete_alert_topic")) {
                deleteThread(chatId);
            } else {
                sendResponse(chatId, properties.getThreads().getCommands(), "Неизвестная команда.", null);
            }
        }
    }

    public void sendAlertMessages(Webhook webhook) {
        if (webhook.getAlerts() == null || webhook.getAlerts().isEmpty()) {
            return;
        }

        var alertsByAlertName = webhook.getAlerts().stream()
                .filter(a -> a.getLabels() != null)
                .filter(a -> a.getLabels().getAlertname() != null)
                .collect(Collectors.groupingBy(a -> a.getLabels().getAlertname()));

        var failedMachinesAlerts = alertsByAlertName.getOrDefault(FAILED_MACHINES, List.of());
        if (!failedMachinesAlerts.isEmpty()) {
            sendFailedMachinesAlerts(webhook, failedMachinesAlerts);
        }

        alertsByAlertName.forEach((alertName, alerts) -> {
            if (FAILED_MACHINES.equals(alertName)) {
                return;
            }
            Integer threadId = alertThreads.get(alertName);
            if (threadId == null) {
                log.error("Неизвестный тип алерта, alertName=" + alertName);
                throw new IllegalStateException("Неизвестный тип алерта, alertName=" + alertName);
            }
            sendResponse(
                    properties.getChatId(),
                    threadId,
                    formatWebhook(webhook),
                    "MarkdownV2"
            );
        });
    }

    private Map<String, Integer> buildAlertThreadMap() {
        return Map.of(
                API_ERROR_HTTP_CODE_INCREASE, properties.getThreads().getErrors5xx(),
                ALT_PAY_CONVERSION, properties.getThreads().getAltpayConversion(),
                FAILED_MACHINES, properties.getThreads().getFailedMachines(),
                PENDING_PAYMENTS, properties.getThreads().getPendingPayments()
        );
    }

    // Просим ввести название топика (только в командном топике)
    private void promptForTopicName(Long chatId) {
        waitingForTopicName.add(chatId);
        sendResponse(chatId, properties.getThreads().getCommands(), "Введите название для нового топика:", null);
    }

    // Создание топика по введённому названию
    private void createThread(Long chatId, String threadName) {
        try {
            waitingForTopicName.remove(chatId);

            CreateForumTopic createForumTopic = CreateForumTopic.builder()
                    .chatId(chatId.toString())
                    .name(threadName)
                    .build();

            Integer messageThreadId = telegramClient.execute(createForumTopic).getMessageThreadId();
//            activeTopics.put(chatId, String.valueOf(messageThreadId));

            // Добавляем топик в список, если у чата уже есть созданные топики
            activeThreads.computeIfAbsent(chatId, k -> new ArrayList<>()).add(String.valueOf(messageThreadId));

            sendResponse(chatId, properties.getThreads().getCommands(), "✅ Тред '" + threadName + "' создан.", null);
            sendResponse(chatId, null, "✅ Тред '" + threadName + "' создан.", null);
        } catch (TelegramApiException e) {
            log.error("Ошибка при создании треда", e);
            sendResponse(chatId, properties.getThreads().getCommands(), "❌ Ошибка при создании треда.", null);
        }
    }

    // Удаление топика (пока API не поддерживает удаление)
    private void deleteThread(Long chatId) {
        sendResponse(chatId, properties.getThreads().getCommands(),
                "🗑 Топик удалён (на самом деле, нет, API не поддерживает).", null);
    }

    private void sendMessageToLastThread(Long chatId) {
        List<String> threads = activeThreads.get(chatId);

        if (threads != null && !threads.isEmpty()) {
            String lastThread = threads.get(threads.size() - 1);
            String messageText = send5xxAlert();
            sendResponse(chatId, Integer.parseInt(lastThread), messageText, "MarkdownV2");
        }
    }

    // Заглушка для отправки метрики "Рост числа 5xx кодов при обращении к API процессинга"
    private void send5xxErrorsMetrics(Long chatId) {
        String messageText = String.format("""
                ```
                Рост числа 5xx кодов при обращении к API процессинга за последние 24h
                
                prov       | term       | count  
                --------------------------------
                %-10d | %-10d | %-10d
                %-10d | %-10d | %-10d
                %-10d | %-10d | %-10d
                ```
                """,
                197, 1435, 35,
                492, 7223, 42,
                545, 9998, 55);

        sendResponse(chatId, properties.getThreads().getErrors5xx(), messageText, "MarkdownV2");
    }

    // Заглушка для отправки метрики "Рост числа платежей без финального статуса"
    private void sendPendingPaymentsMetrics(Long chatId) {
        String messageText = String.format("""
                ```
                Рост числа платежей без финального статуса за последние 24h
                
                prov       | term       | count  
                --------------------------------
                %-10d | %-10d | %-10d
                %-10d | %-10d | %-10d
                %-10d | %-10d | %-10d
                ```
                """,
                314, 1234, 35,
                244, 7556, 42,
                345, 1129, 55);

        sendResponse(chatId, properties.getThreads().getPendingPayments(), messageText, "MarkdownV2");
    }

    // Заглушка для отправки метрики "Рост числа упавших машин"
    private void sendFailedMachinesMetrics(Long chatId) {
        String messageText = String.format("""
                ```
                Рост числа упавших машин за последние 24h
                
                prov       | term       | count  
                --------------------------------
                %-10d | %-10d | %-10d
                %-10d | %-10d | %-10d
                %-10d | %-10d | %-10d
                ```
                """,
                221, 1569, 35,
                234, 7034, 42,
                595, 9032, 55);

        sendResponse(chatId, properties.getThreads().getFailedMachines(), messageText, "MarkdownV2");
    }

    // Заглушка для отправки метрики "Конверсия альтернативных платежей"
    private void sendAltPayConversionMetrics(Long chatId) {
        String messageText = String.format("""
                        ```
                        Конверсия альтернативных платежей за последние 24h
                        
                        prov       | term       | state  | curr  | avg  
                        -----------------------------------------------
                        %-10d | %-10d | %-6s | %-5.2f | %-5.2f  
                        %-10d | %-10d | %-6s | %-5.2f | %-5.2f  
                        %-10d | %-10d | %-6s | %-5.2f | %-5.2f  
                        ```
                        """,
                160, 1456, "alive", 70.23, 61.56,
                240, 7234, "dead", 15.85, 90.02,
                538, 9456, "alive", 50.10, 40.20);

        sendResponse(chatId, properties.getThreads().getAltpayConversion(), messageText, "MarkdownV2");
    }

    // Заглушка для отправки алерта по 5xx кодам для одного провайдера и терминала
    private String send5xxAlert() {
        return String.format("""
            ```
            Рост числа 5xx кодов при обращении к API процессинга за последние %dh

            Количество 5xx: %d
            ```
            """,
                24, 87);
    }


    private void sendResponse(Long chatId, Integer threadId, String messageText, String parseMode) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId.toString())
                .messageThreadId(threadId)
                .text(messageText)
                .parseMode(parseMode)
                .build();

        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения", e);
        }
    }

    private void sendFailedMachinesAlerts(Webhook webhook, List<Webhook.Alert> alerts) {
        Map<Integer, List<Webhook.Alert>> threadIds = new HashMap<>();

        for (Webhook.Alert alert : alerts) {
            Integer threadId = getOrCreateTopicIdForFailedMachinesAlert(alert);
            threadIds.computeIfAbsent(threadId, key -> new ArrayList<>()).add(alert);
        }

        threadIds.forEach((threadId, threadAlerts) -> {
            sendResponse(
                    properties.getChatId(),
                    threadId,
                    formatWebhook(webhook),
                    "MarkdownV2"
            );
        });
    }

    private Integer getOrCreateTopicIdForFailedMachinesAlert(Webhook.Alert alert) {
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
            threadId = getOrCreateTopicIdForFailedMachinesAlert(alert);
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

    private Integer createTopicAndReturnThreadId(String threadName) {
        try {
            CreateForumTopic createForumTopic = CreateForumTopic.builder()
                    .chatId(properties.getChatId())
                    .name(threadName)
                    .build();
            Integer threadId = telegramClient.execute(createForumTopic).getMessageThreadId();
            if (threadId == null) {
                throw new TelegramThreadCreationException("Telegram вернул null messageThreadId: threadName='" +
                        threadName + "'");
            }
            sendResponse(properties.getChatId(), properties.getThreads().getCommands(),
                    "✅ Тред '" + threadName + "' создан.", null);
            return threadId;
        } catch (TelegramApiException e) {
            log.error("Ошибка при создании треда threadName " + threadName, e);
            sendResponse(properties.getChatId(), properties.getThreads().getCommands(),
                    "❌ Ошибка при создании треда.", null);
            throw new TelegramThreadCreationException("Ошибка при создании треда threadName=" + threadName);
        }
    }
}
