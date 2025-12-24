package dev.vality.alerting.tg.bot.handler.alert;

import dev.vality.alerting.tg.bot.model.Webhook;

import java.util.List;

public interface AlertHandler {
    boolean filter(String alertName);

    void handle(Webhook webhook, List<Webhook.Alert> alerts);
}
