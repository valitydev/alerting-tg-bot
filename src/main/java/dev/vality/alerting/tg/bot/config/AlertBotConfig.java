package dev.vality.alerting.tg.bot.config;

import dev.vality.alerting.tg.bot.service.AlertBot;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
@RequiredArgsConstructor
public class AlertBotConfig {

    private final AlertBot alertBot;

    @Bean
    public TelegramBotsApi telegramBotsApi() throws Exception {
        TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
        api.registerBot(alertBot);
        return api;
    }
}
