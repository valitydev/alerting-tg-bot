package dev.vality.alerting.tg.bot.util;

import dev.vality.alerting.tg.bot.model.Webhook;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class WebhookUtil {
    public static final String FIRING = "firing";
    public static final String RESOLVED = "resolved";

    public static String extractAlertname(Webhook webhook) {
        if (webhook.getCommonLabels() != null) {
            return webhook.getCommonLabels().getAlertname();
        } else if (webhook.getGroupLabels() != null) {
            return webhook.getGroupLabels().getAlertname();
        } else if (webhook.getAlerts().getFirst().getLabels() != null) {
            return webhook.getAlerts().getFirst().getLabels().getAlertname();
        } else {
            return null;
        }
    }

    public static String formatWebhook(Webhook webhook) {

        Webhook.Annotation annotation = webhook.getCommonAnnotations();

        if (annotation == null) {
            log.error("Отсутствует описание алерта,  webhook.getCommonAnnotations() is null. {}", webhook);
            throw new IllegalStateException("Отсутствует описание алерта:  webhook.getCommonAnnotations() is null");
        }

        if (FIRING.equals(webhook.getStatus())) {
            return """
                ```
                АЛЕРТ СРАБОТАЛ ❗
                
                %s
                ```
                """.formatted(annotation.getDescription());
        } else if (RESOLVED.equals(webhook.getStatus())) {
            return """
                ```
                Ситуация пришла в норму ✅
                
                %s
                ```
                """.formatted(annotation.getSummary());
        } else {
            log.error("Отсутствует статус алерта,  webhook.getStatus() is null. {}", webhook);
            throw new IllegalStateException("Отсутствует статус алерта:  webhook.getStatus() is null");
        }
    }
}
