package dev.vality.alerting.tg.bot.config;

import dev.vality.alerting.tg.bot.config.properties.AlertBotProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Configuration
@RequiredArgsConstructor
public class AlertBotConfig {

    @Bean
    public TelegramClient telegramClient(AlertBotProperties properties) {
        return new OkHttpTelegramClient(properties.getToken());
    }
}
