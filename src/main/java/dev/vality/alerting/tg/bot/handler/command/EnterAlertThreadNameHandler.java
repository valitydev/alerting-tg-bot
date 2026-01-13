package dev.vality.alerting.tg.bot.handler.command;

import dev.vality.alerting.tg.bot.config.properties.AlertBotProperties;
import dev.vality.alerting.tg.bot.exception.TelegramThreadCreationException;
import dev.vality.alerting.tg.bot.service.CommandStateService;
import dev.vality.alerting.tg.bot.service.TelegramApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.forum.CreateForumTopic;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(10)
public class EnterAlertThreadNameHandler implements TelegramCommandHandler {
    private final AlertBotProperties properties;
    private final CommandStateService commandStateService;
    private final TelegramApiService telegramApiService;
    private final TelegramClient telegramClient;

    @Override
    public boolean filter(Update update) {
        var message = update.getMessage();

        Integer threadId = message.getMessageThreadId();
        if (threadId == null || !threadId.equals(properties.getThreads().getCommands())) {
            return false;
        }

        Long chatId = message.getChatId();
        if (chatId == null) {
            return false;
        }

        return commandStateService.isWaitingForNewThreadName(chatId);
    }

    @Override
    public void handle(Update update) {
        var message = update.getMessage();
        Long chatId = message.getChatId();

        commandStateService.clearWaitingForNewThreadName(chatId);

        String threadName = message.getText() != null ? message.getText().trim() : "";
        if (threadName.isBlank()) {
            telegramApiService.sendMessage(chatId, properties.getThreads().getCommands(),
                    "Название треда не может быть пустым. Повторите /create_alert_topic.", null);
            return;
        }

        if (threadName.startsWith("/")) {
            telegramApiService.sendMessage(chatId, properties.getThreads().getCommands(),
                    "Ожидалось название треда (обычный текст), а не команда. Повторите /create_alert_topic.", null);
            return;
        }

        try {
            CreateForumTopic createForumTopic = CreateForumTopic.builder()
                    .chatId(chatId.toString())
                    .name(threadName)
                    .build();

            Integer messageThreadId = telegramClient.execute(createForumTopic).getMessageThreadId();
            if (messageThreadId == null) {
                throw new TelegramThreadCreationException("Telegram вернул null messageThreadId: threadName='" +
                        threadName + "'");
            }

            telegramApiService.sendMessage(chatId, properties.getThreads().getCommands(),
                    "✅ Тред '" + threadName + "' создан.", null);
        } catch (TelegramApiException e) {
            log.error("Ошибка при создании треда", e);
            telegramApiService.sendMessage(chatId, properties.getThreads().getCommands(),
                    "❌ Ошибка при создании треда.", null);
        }
    }
}
