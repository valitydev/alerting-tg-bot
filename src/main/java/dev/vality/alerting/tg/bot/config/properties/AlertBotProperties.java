package dev.vality.alerting.tg.bot.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.validation.constraints.NotNull;

@Data
@Configuration
@ConfigurationProperties(prefix = "bot")
public class AlertBotProperties {

    @NotNull
    private String token;
    @NotNull
    private String name;
}
