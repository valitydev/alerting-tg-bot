package dev.vality.alerting.tg.bot;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.vality.alerting.tg.bot.config.AlertBotConfig;
import dev.vality.alerting.tg.bot.config.properties.AlertmanagerWebhookProperties;
import dev.vality.alerting.tg.bot.controller.WebhookController;
import dev.vality.alerting.tg.bot.model.Webhook;
import dev.vality.alerting.tg.bot.service.AlertBot;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.telegram.telegrambots.longpolling.starter.TelegramBotStarterConfiguration;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ImportAutoConfiguration(exclude = TelegramBotStarterConfiguration.class)
@TestPropertySource(properties = {
        "spring.cloud.vault.enabled=false",
        "spring.mvc.pathmatch.matching-strategy=ant_path_matcher",
        "bot.token=test",
        "bot.name=vality_alerting_bot",
        "bot.chatId=1",
        "bot.topics.commands=1",
        "bot.topics.errors5xx=2",
        "bot.topics.altpay-conversion=3",
        "bot.topics.failed-machines=4",
        "bot.topics.pending-payments=5"
})
public class WebhookControllerTest {

    @MockitoBean
    AlertmanagerWebhookProperties webhookProperties;

    @MockitoBean
    AlertBot alertBot;

    @MockitoBean
    AlertBotConfig alertBotConfig;

    @MockitoBean
    TelegramClient telegramClient;

    @MockitoBean
    TelegramBotStarterConfiguration configuration;

    String webhookJson = """
            {
              "status": "firing",
              "receiver": "telegram",
              "alerts": [
                {
                  "status": "firing",
                  "labels": {
                    "alertname": "Errors5xxHigh",
                    "severity": "critical",
                    "job": "payments",
                    "namespace": "prod",
                    "service": "payments-api",
                    "instance": "payments-api-1",
                    "pod": "payments-api-1-abc123"
                  },
                  "annotations": {
                    "summary": "HTTP 5xx rate is too high",
                    "description": "Payments API is returning >5% 5xx responses for 5m",
                    "runbook_url": "https://runbook.company/alerts/errors5xx"
                  }
                },
                {
                  "status": "resolved",
                  "labels": {
                    "alertname": "AltpayConversionLow",
                    "severity": "warning",
                    "job": "altpay",
                    "namespace": "prod",
                    "service": "altpay-conversion",
                    "pod": "altpay-0-xzy987"
                  },
                  "annotations": {
                    "summary": "Altpay conversion dropped",
                    "description": "Altpay conversion < 2% in last 10m"
                  }
                }
              ]
            }
            """;

    @Test
    public void sendTgMessageTest() {
        ObjectMapper objectMapper = new ObjectMapper();
        WebhookController webhookController = new WebhookController(webhookProperties, objectMapper, alertBot);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setMethod("POST");
        req.setRequestURI("/alertmanager/webhook");
        req.setContentType(MediaType.APPLICATION_JSON_VALUE);
        req.setCharacterEncoding(StandardCharsets.UTF_8.name());
        req.setContent(webhookJson.getBytes(StandardCharsets.UTF_8));

        val response = webhookController.processWebhook(req);
        assertThat(response.getStatusCode().value()).isEqualTo(200);

        ArgumentCaptor<Webhook> webhookCaptor = ArgumentCaptor.forClass(Webhook.class);
        verify(alertBot).sendAlertMessage(webhookCaptor.capture());
        Webhook passed = webhookCaptor.getValue();
        assertThat(passed).isNotNull();
        assertThat(passed.getAlerts()).isNotNull();
    }
}
