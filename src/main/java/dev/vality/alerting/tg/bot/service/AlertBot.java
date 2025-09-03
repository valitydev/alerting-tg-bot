package dev.vality.alerting.tg.bot.service;

import dev.vality.alerting.tg.bot.config.properties.AlertBotProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.forum.CreateForumTopic;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@EnableScheduling
public class AlertBot extends TelegramLongPollingBot {

    private final AlertBotProperties properties;
    private static final Map<Long, List<String>> activeTopics = new HashMap<>();
    private static final Set<Long> waitingForTopicName = new HashSet<>();
    private static final int COMMANDS_TOPIC_ID = 3;
    private static final int ERRORS_5XX_TOPIC_ID = 9;
    private static final int ALTPAY_CONVERSION_TOPIC_ID = 11;
    private static final int FAILED_MACHINES_TOPIC_ID = 7;
    private static final int PENDING_PAYMENTS_TOPIC_ID = 17;

    @Override
    public String getBotUsername() {
        return properties.getName();
    }

    @Override
    public String getBotToken() {
        return properties.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
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
            if (!threadId.equals(COMMANDS_TOPIC_ID)) {
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
                sendResponse(chatId, COMMANDS_TOPIC_ID, "Неизвестная команда.", null);
            }
        }
    }

    public void sendScheduledMetrics() {
        send5xxErrorsMetrics(properties.getChatId());
        sendFailedMachinesMetrics(properties.getChatId());
        sendPendingPaymentsMetrics(properties.getChatId());
        sendAltPayConversionMetrics(properties.getChatId());
        sendMessageToLastTopic(properties.getChatId());
    }

    // Просим ввести название топика (только в командном топике)
    private void promptForTopicName(Long chatId) {
        waitingForTopicName.add(chatId);
        sendResponse(chatId, COMMANDS_TOPIC_ID, "Введите название для нового топика:", null);
    }

    // Создание топика по введённому названию
    private void createTopic(Long chatId, String topicName) {
        try {
            waitingForTopicName.remove(chatId);

            CreateForumTopic createForumTopic = CreateForumTopic.builder()
                    .chatId(chatId.toString())
                    .name(topicName)
                    .build();

            Integer messageThreadId = execute(createForumTopic).getMessageThreadId();
//            activeTopics.put(chatId, String.valueOf(messageThreadId));

            // Добавляем топик в список, если у чата уже есть созданные топики
            activeTopics.computeIfAbsent(chatId, k -> new ArrayList<>()).add(String.valueOf(messageThreadId));

            sendResponse(chatId, COMMANDS_TOPIC_ID, "✅ Топик '" + topicName + "' создан.", null);
            sendResponse(chatId, null, "✅ Топик '" + topicName + "' создан.", null);
        } catch (TelegramApiException e) {
            log.error("Ошибка при создании топика", e);
            sendResponse(chatId, COMMANDS_TOPIC_ID, "❌ Ошибка при создании топика.", null);
        }
    }

    // Удаление топика (пока API не поддерживает удаление)
    private void deleteTopic(Long chatId) {
        sendResponse(chatId, COMMANDS_TOPIC_ID, "🗑 Топик удалён (на самом деле, нет, API не поддерживает).", null);
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

        sendResponse(chatId, ERRORS_5XX_TOPIC_ID, messageText, "MarkdownV2");
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

        sendResponse(chatId, PENDING_PAYMENTS_TOPIC_ID, messageText, "MarkdownV2");
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

        sendResponse(chatId, FAILED_MACHINES_TOPIC_ID, messageText, "MarkdownV2");
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

        sendResponse(chatId, ALTPAY_CONVERSION_TOPIC_ID, messageText, "MarkdownV2");
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
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения", e);
        }
    }
}

