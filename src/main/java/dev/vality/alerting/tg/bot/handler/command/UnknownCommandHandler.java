package dev.vality.alerting.tg.bot.handler.command;

import dev.vality.alerting.tg.bot.config.properties.AlertBotProperties;
import dev.vality.alerting.tg.bot.service.TelegramApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(1000)
public class UnknownCommandHandler implements TelegramCommandHandler {
    private final AlertBotProperties properties;
    private final TelegramApiService telegramApiService;


    @Override
    public boolean filter(Update update) {
        var message = update.getMessage();
        Integer threadId = message.getMessageThreadId();
        if (threadId == null || !threadId.equals(properties.getThreads().getCommands())) {
            return false;
        }

        String text = message.getText();
        return text != null && text.trim().startsWith("/");
    }

    @Override
    public void handle(Update update) {
        var message = update.getMessage();
        Long chatId = message.getChatId();

        telegramApiService.sendMessage(chatId, properties.getThreads().getCommands(), "Неизвестная команда.", null);
        log.info("Введена неизвестная команда message:" + message);
    }
}
