package dev.vality.alerting.tg.bot.util;

import dev.vality.alerting.tg.bot.model.Webhook;

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
        StringBuilder sb = new StringBuilder();

        sb.append("```").append("\n");

        Webhook.Annotation annotation = webhook.getCommonAnnotations();

        if (webhook.getStatus().equals(FIRING)) {
            sb.append("АЛЕРТ СРАБОТАЛ ❗").append("\n");
            if (annotation != null) {
                appendLine(sb, "", annotation.getDescription());
            }
        } else if (webhook.getStatus().equals(RESOLVED)) {
            sb.append("Ситуация пришла в норму ✅").append("\n");
            if (annotation != null) {
                appendLine(sb, "", annotation.getSummary());
            }
        }

        sb.append("```");

        return sb.toString();
    }

    private static boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static void appendLine(StringBuilder sb, String indent, String value) {
        if (isNotBlank(value)) {
            sb.append(indent)
                    .append(value)
                    .append("\n");
        }
    }
}
