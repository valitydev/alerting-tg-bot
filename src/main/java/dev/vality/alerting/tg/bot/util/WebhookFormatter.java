package dev.vality.alerting.tg.bot.util;

import dev.vality.alerting.tg.bot.model.Webhook;

public final class WebhookFormatter {
    public static String formatWebhook(Webhook webhook) {
        StringBuilder sb = new StringBuilder();

        sb.append("```").append("\n");

        appendLine(sb, "", "receiver", webhook.getReceiver());
        appendLine(sb, "", "status", webhook.getStatus());
        appendLine(sb, "", "version", webhook.getVersion());
        appendLine(sb, "", "external URL", webhook.getExternalURL());

        sb.append("\n");

        sb.append("group labels:").append("\n");
        appendLabelBlock(sb, webhook.getGroupLabels(), "  ");

        sb.append("\n");

        sb.append("common labels:").append("\n");
        appendLabelBlock(sb, webhook.getCommonLabels(), "  ");

        sb.append("\n");

        sb.append("common annotations:").append("\n");
        appendAnnotationBlock(sb, webhook.getCommonAnnotations(), "  ");

        sb.append("\n");

        if (webhook.getAlerts() == null || webhook.getAlerts().isEmpty()) {
            sb.append("alerts: []").append("\n");
        } else {
            sb.append("alerts:").append("\n");
            int index = 0;
            for (Webhook.Alert alert : webhook.getAlerts()) {
                sb.append("  [").append(index++).append("]").append("\n");
                appendAlertBlock(sb, alert, "    ");
                sb.append("\n");
            }
        }

        sb.append("```");

        return sb.toString();
    }

    private static boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static void appendLine(StringBuilder sb, String indent, String name, String value) {
        if (isNotBlank(value)) {
            sb.append(indent)
                    .append(name)
                    .append(": ")
                    .append(value)
                    .append("\n");
        }
    }

    private static void appendAlertBlock(StringBuilder sb, Webhook.Alert alert, String indent) {
        if (alert == null) {
            sb.append(indent).append("<null alert>").append("\n");
            return;
        }

        appendLine(sb, indent, "status", alert.getStatus());
        appendLine(sb, indent, "starts at", alert.getStartsAt());
        appendLine(sb, indent, "ends at", alert.getEndsAt());
        appendLine(sb, indent, "generator URL", alert.getGeneratorURL());
        appendLine(sb, indent, "fingerprint", alert.getFingerprint());

        sb.append("\n");
        sb.append(indent).append("labels:").append("\n");
        appendLabelBlock(sb, alert.getLabels(), indent + "  ");

        sb.append("\n");
        sb.append(indent).append("annotations:").append("\n");
        appendAnnotationBlock(sb, alert.getAnnotations(), indent + "  ");
    }

    private static void appendLabelBlock(StringBuilder sb, Webhook.Label label, String indent) {
        if (label == null) {
            sb.append(indent).append("<null>").append("\n");
            return;
        }

        appendLine(sb, indent, "alertname", label.getAlertname());
        appendLine(sb, indent, "api_type", label.getApiType());
        appendLine(sb, indent, "code", label.getCode());
        appendLine(sb, indent, "prometheus", label.getPrometheus());
        appendLine(sb, indent, "team", label.getTeam());
        appendLine(sb, indent, "url", label.getUrl());
    }

    private static void appendAnnotationBlock(StringBuilder sb, Webhook.Annotation ann, String indent) {
        if (ann == null) {
            sb.append(indent).append("<null>").append("\n");
            return;
        }

        appendLine(sb, indent, "description", ann.getDescription());
        appendLine(sb, indent, "summary", ann.getSummary());
    }
}
