package dev.vality.alerting.tg.bot.config;

import dev.vality.alerting.tg.bot.config.properties.AlertBotProperties;
import dev.vality.alerting.tg.bot.service.AlertBot;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Configuration
@EnableConfigurationProperties(AlertBotProperties.class)
@RequiredArgsConstructor
public class AlertBotConfig {

    private final AlertBot alertBot;

    @Bean
    public TelegramClient telegramClient(AlertBotProperties properties) {
        return new OkHttpTelegramClient(properties.getToken());
    }
}
