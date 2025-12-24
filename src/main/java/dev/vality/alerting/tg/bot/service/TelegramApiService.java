package dev.vality.alerting.tg.bot.service;

import dev.vality.alerting.tg.bot.config.properties.AlertBotProperties;
import dev.vality.alerting.tg.bot.exception.TelegramThreadCreationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.forum.CreateForumTopic;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramApiService {
    private final TelegramClient telegramClient;
    private final AlertBotProperties properties;

    public void sendMessage(Long chatId, Integer threadId, String messageText, String parseMode) {
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

    public Integer createTopicAndReturnThreadId(String threadName) {
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
            sendMessage(properties.getChatId(), properties.getThreads().getCommands(),
                    "✅ Тред '" + threadName + "' создан.", null);
            return threadId;
        } catch (TelegramApiException e) {
            log.error("Ошибка при создании треда threadName " + threadName, e);
            sendMessage(properties.getChatId(), properties.getThreads().getCommands(),
                    "❌ Ошибка при создании треда.", null);
            throw new TelegramThreadCreationException("Ошибка при создании треда threadName=" + threadName);
        }
    }
}
