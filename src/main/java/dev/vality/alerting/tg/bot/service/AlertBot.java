package dev.vality.alerting.tg.bot.service;

import dev.vality.alerting.tg.bot.config.properties.AlertBotProperties;
import dev.vality.alerting.tg.bot.dao.ProviderTerminalTopicDao;
import dev.vality.alerting.tg.bot.model.Webhook;
import dev.vality.alerting.tg.bot.pojo.ProviderTerminalTopic;
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

import static dev.vality.alerting.tg.bot.constant.AlertTopicName.*;
import static dev.vality.alerting.tg.bot.util.WebhookUtil.extractAlertname;
import static dev.vality.alerting.tg.bot.util.WebhookUtil.formatWebhook;

@Slf4j
@Service
@RequiredArgsConstructor
@EnableScheduling
public class AlertBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private final AlertBotProperties properties;
    private final TelegramClient telegramClient;
    private final ProviderTerminalTopicDao providerTerminalTopicDao;
    private static final Map<Long, List<String>> activeTopics = new HashMap<>();
    private static final Set<Long> waitingForTopicName = new HashSet<>();
    private Map<String, Integer> alertTopics;

    @PostConstruct
    public void init() {
        alertTopics = buildAlertTopicMap();
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
            if (!threadId.equals(properties.getTopics().getCommands())) {
                return;
            }

            String text = message.getText();
            if (text.startsWith("/create_alert_topic")) {
                promptForTopicName(chatId);
            } else if (waitingForTopicName.contains(chatId)) {
                createTopic(chatId, text);
            } else if (text.startsWith("/delete_alert_topic")) {
                deleteTopic(chatId);
            } else {
                sendResponse(chatId, properties.getTopics().getCommands(), "Неизвестная команда.", null);
            }
        }
    }

    public void sendAlertMessage(Webhook webhook) {
        if (webhook.getAlerts() == null || webhook.getAlerts().isEmpty()) {
            return;
        }

        Optional<String> alertNameOpt = extractAlertname(webhook);

        if (alertNameOpt.isEmpty()) {
            log.error("Alertname is null: {}", webhook);
            return;
        }

        String alertName = alertNameOpt.get();
        Integer threadId;
        if (alertName.equals(FAILED_MACHINES)) {
            threadId = getFailedMachinesThreadId(webhook);
        } else {
            threadId = alertTopics.get(alertName);
        }

        sendResponse(
                properties.getChatId(),
                threadId,
                formatWebhook(webhook),
                "MarkdownV2"
        );
    }

    public void sendScheduledMetrics() {
        send5xxErrorsMetrics(properties.getChatId());
        sendFailedMachinesMetrics(properties.getChatId());
        sendPendingPaymentsMetrics(properties.getChatId());
        sendAltPayConversionMetrics(properties.getChatId());
        sendMessageToLastTopic(properties.getChatId());
    }

    private Map<String, Integer> buildAlertTopicMap() {
        return Map.of(
                API_ERROR_HTTP_CODE_INCREASE, properties.getTopics().getErrors5xx(),
                ALT_PAY_CONVERSION, properties.getTopics().getAltpayConversion(),
                FAILED_MACHINES, properties.getTopics().getFailedMachines(),
                PENDING_PAYMENTS, properties.getTopics().getPendingPayments()
        );
    }

    // Просим ввести название топика (только в командном топике)
    private void promptForTopicName(Long chatId) {
        waitingForTopicName.add(chatId);
        sendResponse(chatId, properties.getTopics().getCommands(), "Введите название для нового топика:", null);
    }

    // Создание топика по введённому названию
    private void createTopic(Long chatId, String topicName) {
        try {
            waitingForTopicName.remove(chatId);

            CreateForumTopic createForumTopic = CreateForumTopic.builder()
                    .chatId(chatId.toString())
                    .name(topicName)
                    .build();

            Integer messageThreadId = telegramClient.execute(createForumTopic).getMessageThreadId();
//            activeTopics.put(chatId, String.valueOf(messageThreadId));

            // Добавляем топик в список, если у чата уже есть созданные топики
            activeTopics.computeIfAbsent(chatId, k -> new ArrayList<>()).add(String.valueOf(messageThreadId));

            sendResponse(chatId, properties.getTopics().getCommands(), "✅ Топик '" + topicName + "' создан.", null);
            sendResponse(chatId, null, "✅ Топик '" + topicName + "' создан.", null);
        } catch (TelegramApiException e) {
            log.error("Ошибка при создании топика", e);
            sendResponse(chatId, properties.getTopics().getCommands(), "❌ Ошибка при создании топика.", null);
        }
    }

    // Удаление топика (пока API не поддерживает удаление)
    private void deleteTopic(Long chatId) {
        sendResponse(chatId, properties.getTopics().getCommands(),
                "🗑 Топик удалён (на самом деле, нет, API не поддерживает).", null);
    }

    private void sendMessageToLastTopic(Long chatId) {
        List<String> topics = activeTopics.get(chatId);

        if (topics != null && !topics.isEmpty()) {
            String lastTopic = topics.get(topics.size() - 1);
            String messageText = send5xxAlert();
            sendResponse(chatId, Integer.parseInt(lastTopic), messageText, "MarkdownV2");
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

        sendResponse(chatId, properties.getTopics().getErrors5xx(), messageText, "MarkdownV2");
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

        sendResponse(chatId, properties.getTopics().getPendingPayments(), messageText, "MarkdownV2");
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

        sendResponse(chatId, properties.getTopics().getFailedMachines(), messageText, "MarkdownV2");
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

        sendResponse(chatId, properties.getTopics().getAltpayConversion(), messageText, "MarkdownV2");
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


    // Отправка сообщения в командный топик
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

    private Integer getFailedMachinesThreadId(Webhook webhook) {
        //достаю из лейблов поля providerId и terminalId
        //проверяю, есть ли по сочетанию таких providerId и terminalId запись
        // в таблице alrt_tg_bot.provider_terminal_topic
        //если есть, то возвращаю поле threadId этой записи
        //если нет, то создаю топик для такого сочетания providerId и terminalId, формируя название
        // из providerName и terminalName
        //получаю threadId
        //и создаю в таблице alrt_tg_bot.provider_terminal_topic запись для такого сочетания providerId и terminalId
        //затем возвращаю threadId этого топика

        var alert = webhook.getAlerts().stream()
                .filter(a -> a.getLabels() != null)
                .filter(a -> FAILED_MACHINES.equals(a.getLabels().getAlertname()))
                .findFirst()
                .orElseThrow();

        var labels = alert.getLabels();
        String providerId = labels.getProviderId();
        String terminalId = labels.getTerminalId();

        var existing = providerTerminalTopicDao.findByProviderAndTerminal(providerId, terminalId);
        if (existing.isPresent() && existing.get().getThreadId() != null) {
            return existing.get().getThreadId();
        }

        String topicName = labels.getProviderName() + " - " + labels.getTerminalName();
        Integer threadId = createTopicAndReturnThreadId(topicName);

        var entity = new ProviderTerminalTopic(
                threadId,
                providerId,
                terminalId,
                labels.getProviderName(),
                labels.getTerminalName(),
                topicName
        );
        providerTerminalTopicDao.upsert(entity);

        return threadId;
    }

    private Integer createTopicAndReturnThreadId(String topicName) {
        try {
            CreateForumTopic createForumTopic = CreateForumTopic.builder()
                    .chatId(properties.getChatId())
                    .name(topicName)
                    .build();
            Integer threadId = telegramClient.execute(createForumTopic).getMessageThreadId();
            if (threadId == null) {
                throw new IllegalStateException("Telegram вернул null messageThreadId: topicName='" + topicName + "'");
            }
            sendResponse(properties.getChatId(), properties.getTopics().getCommands(),
                    "✅ Топик '" + topicName + "' создан.", null);
            return threadId;
        } catch (TelegramApiException e) {
            log.error("Ошибка при создании топика topicName " + topicName, e);
            sendResponse(properties.getChatId(), properties.getTopics().getCommands(),
                    "❌ Ошибка при создании топика.", null);
            throw new IllegalStateException("Ошибка при создании топика topicName=" + topicName);
        }
    }

}

