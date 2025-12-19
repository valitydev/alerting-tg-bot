package dev.vality.alerting.tg.bot.exception;

public class TelegramThreadCreationException extends RuntimeException {

    public TelegramThreadCreationException(String message) {
        super(message);
    }

    public TelegramThreadCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
