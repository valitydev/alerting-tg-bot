package dev.vality.alerting.tg.bot.handler.command;

import dev.vality.alerting.tg.bot.config.properties.AlertBotProperties;
import dev.vality.alerting.tg.bot.service.CommandStateService;
import dev.vality.alerting.tg.bot.service.TelegramApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(20)
public class CreateAlertThreadCommandHandler implements TelegramCommandHandler {

    private static final String COMMAND = "/create_alert_topic";

    private final AlertBotProperties properties;
    private final CommandStateService commandStateService;
    private final TelegramApiService telegramApiService;

    @Override
    public boolean filter(Update update) {
        var message = update.getMessage();

        Integer threadId = message.getMessageThreadId();
        if (threadId == null || !threadId.equals(properties.getThreads().getCommands())) {
            return false;
        }

        String text = message.getText();
        if (text == null) {
            return false;
        }

        return text.trim().startsWith(COMMAND);
    }

    @Override
    public void handle(Update update) {
        var message = update.getMessage();
        Long chatId = message.getChatId();

        commandStateService.markWaitingForNewThreadName(chatId);

        telegramApiService.sendMessage(chatId, properties.getThreads().getCommands(),
                "Введите название для нового треда:", null);

        log.info("Create alert thread command accepted. chatId={}, user=@{}",
                chatId,
                message.getFrom() != null ? message.getFrom().getUserName() : null
        );
    }
}
