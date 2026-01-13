package dev.vality.alerting.tg.bot.handler.command;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface TelegramCommandHandler {

    boolean filter(final Update update);

    void handle(Update update);
}
