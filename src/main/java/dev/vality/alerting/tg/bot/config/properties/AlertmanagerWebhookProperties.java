package dev.vality.alerting.tg.bot.config.properties;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "alertmanager.webhook")
@Validated
@Getter
@Setter
public class AlertmanagerWebhookProperties {
    @NotNull
    private String url;
    private String path;
    private Boolean sendResolved = true;
}
