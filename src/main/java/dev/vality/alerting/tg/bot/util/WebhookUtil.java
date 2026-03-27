package dev.vality.alerting.tg.bot.util;

import dev.vality.alerting.tg.bot.model.Webhook;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public final class WebhookUtil {
    public static final String FIRING = "firing";
    public static final String RESOLVED = "resolved";

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

    public static String formatWebhook(Webhook webhook, List<Webhook.Alert> alerts) {
        if (alerts == null || alerts.isEmpty()) {
            log.error("Список алертов пуст. {}", webhook);
            throw new IllegalStateException("Список алертов пуст");
        }

        List<Webhook.Alert> firingAlerts = alerts.stream()
                .filter(alert -> FIRING.equals(alert.getStatus()))
                .toList();

        List<Webhook.Alert> resolvedAlerts = alerts.stream()
                .filter(alert -> RESOLVED.equals(alert.getStatus()))
                .toList();

        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append("```\n");

        if (!firingAlerts.isEmpty()) {
            messageBuilder.append("АЛЕРТЫ СРАБОТАЛИ ❗\n\n");

            String firingText = firingAlerts.stream()
                    .map(Webhook.Alert::getAnnotations)
                    .filter(Objects::nonNull)
                    .map(Webhook.Annotation::getDescription)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.joining("\n\n"));

            messageBuilder.append(firingText);
        }

        if (!firingAlerts.isEmpty() && !resolvedAlerts.isEmpty()) {
            messageBuilder.append("\n\n---\n\n");
        }

        if (!resolvedAlerts.isEmpty()) {
            messageBuilder.append("Ситуации пришли в норму ✅\n\n");

            String resolvedText = resolvedAlerts.stream()
                    .map(Webhook.Alert::getAnnotations)
                    .filter(Objects::nonNull)
                    .map(Webhook.Annotation::getSummary)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.joining("\n\n"));

            messageBuilder.append(resolvedText);
        }

        messageBuilder.append("\n```");
        String result = messageBuilder.toString();

        if (result.isBlank()) {
            log.error("Не удалось сформировать текст алертов. {}", webhook);
            throw new IllegalStateException("Не удалось сформировать текст алертов");
        }

        return result;
    }

    public static String formatPaymentConversionAlert(Webhook.Alert alert) {
        if (alert == null || alert.getLabels() == null) {
            return null;
        }

        Webhook.Label labels = alert.getLabels();
        final String statusMark = FIRING.equals(alert.getStatus()) ? "🛑" : "✅";
        final String terminal = joinNotBlank(labels.getTerminalId(), labels.getTerminalName());
        final String provider = joinNotBlank(labels.getProviderId(), labels.getProviderName());
        String errorDescription = null;
        String currentConversion = null;
        String uniqueUsersRatio = null;

        if (alert.getAnnotations() != null) {
            String source = firstNonBlank(
                    alert.getAnnotations().getDescription(),
                    alert.getAnnotations().getSummary()
            );
            if (source != null) {
                List<String> lines = source.lines()
                        .map(String::trim)
                        .filter(line -> !line.isBlank())
                        .toList();

                Function<String, String> normalize = line -> line
                        .toLowerCase(Locale.ROOT)
                        .replace(" ", "")
                        .replace("_", "");

                currentConversion = lines.stream()
                        .filter(line -> normalize.apply(line).startsWith("currentconversion"))
                        .findFirst()
                        .orElse(null);
                uniqueUsersRatio = lines.stream()
                        .filter(line -> normalize.apply(line).startsWith("uniqueusersratio"))
                        .findFirst()
                        .orElse(null);
                errorDescription = lines.stream()
                        .filter(line -> !normalize.apply(line).startsWith("currentconversion"))
                        .filter(line -> !normalize.apply(line).startsWith("uniqueusersratio"))
                        .findFirst()
                        .orElse(null);
            }
        }

        StringBuilder builder = new StringBuilder();
        builder.append("```\n")
                .append(statusMark);

        if (!terminal.isBlank()) {
            builder.append(" ").append(terminal);
        }
        builder.append("\n");

        if (!provider.isBlank()) {
            builder.append("provider: ").append(provider).append("\n");
        }
        if (errorDescription != null && !errorDescription.isBlank()) {
            builder.append(errorDescription).append("\n");
        }
        if (currentConversion != null && !currentConversion.isBlank()) {
            builder.append(currentConversion).append("\n");
        }
        if (uniqueUsersRatio != null && !uniqueUsersRatio.isBlank()) {
            builder.append(uniqueUsersRatio).append("\n");
        }
        builder.append("```");
        return builder.toString();
    }

    private static String firstNonBlank(String... values) {
        return Arrays.stream(values)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
    }

    private static String joinNotBlank(String left, String right) {
        return Stream.of(left, right)
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(" "));
    }
}
