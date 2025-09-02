package dev.vality.alerting.tg.bot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.vality.alerting.tg.bot.config.properties.AlertmanagerWebhookProperties;
import dev.vality.alerting.tg.bot.constant.PrometheusRuleLabel;
import dev.vality.alerting.tg.bot.model.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import dev.vality.alerting.tg_bot.Notification;
import dev.vality.alerting.tg_bot.NotifierServiceSrv;
import dev.vality.alerting.tg_bot.ReceiverNotFound;
import org.apache.thrift.TException;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpServletRequest;

import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/alertmanager")
@RequiredArgsConstructor
public class WebhookController {
    private final AlertmanagerWebhookProperties alertmanagerWebhookProperties;
    private final NotifierServiceSrv.Iface telegramBotClient;
    private final Converter<Webhook.Alert, Notification> webhookAlertToNotificationConverter;
    private final ObjectMapper objectMapper;
    public static final String FIRING = "firing";

    @PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> processWebhook(HttpServletRequest servletRequest) {
        try {
            var webhookBody = servletRequest.getReader().lines().collect(Collectors.joining(" "));
            log.info("Received webhook from alertmanager: {}", webhookBody);
            var webhook = objectMapper.readValue(webhookBody, Webhook.class);
            for (Webhook.Alert alert : webhook.getAlerts()) {
                try {
                    String userId = alert.getLabels().get(PrometheusRuleLabel.USERNAME);
                    String alertName = alert.getLabels().get(PrometheusRuleLabel.ALERT_NAME);
                    // Алертменеджер может прислать нотификацию уже после того, как пользователь удалил алерт, т.к
                    // обновления в конфигурации применяются не моментально. Поэтому нужна доп.фильтрация здесь.
                    if (isResolvedNotificationPermitted(alert)) {
                        var notification = webhookAlertToNotificationConverter.convert(alert);
                        telegramBotClient.notify(notification);
                        log.info("Alert from alertmanager webhook processed successfully: {}", alert);
                    }
                } catch (ReceiverNotFound receiverNotFound) {
                    log.error("Unable to find notification receiver '{}':", webhook.getReceiver(), receiverNotFound);
                } catch (TException e) {
                    log.error("Unexpected error during notification delivery:", e);
                    return ResponseEntity.internalServerError().build();
                }
            }
        } catch (Exception e) {
            log.error("Unexpected error during webhook parsing:", e);
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok().build();
    }

    private boolean isResolvedNotificationPermitted(Webhook.Alert alert) {
        return FIRING.equals(alert.getStatus()) || alertmanagerWebhookProperties.getSendResolved();
    }
}
