package dev.vality.alerting.tg.bot.service;

import dev.vality.alerting.tg.bot.config.properties.AlertBotProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MockMetricsMessagesService {
    private final AlertBotProperties properties;
    private final TelegramApiService telegramApiService;

    // Заглушка для отправки метрики "Рост числа 5xx кодов при обращении к API процессинга"
    public void send5xxErrorsMetrics(Long chatId) {
        String messageText = String.format("""
                ```
                Рост числа 5xx кодов при обращении к API процессинга за последние 24h
                
                prov       | term       | count  
                --------------------------------
                %-10d | %-10d | %-10d
                %-10d | %-10d | %-10d
                %-10d | %-10d | %-10d
                ```
                """,
                197, 1435, 35,
                492, 7223, 42,
                545, 9998, 55);

        telegramApiService.sendMessage(chatId, properties.getThreads().getErrors5xx(), messageText, "MarkdownV2");
    }

    // Заглушка для отправки метрики "Рост числа платежей без финального статуса"
    public void sendPendingPaymentsMetrics(Long chatId) {
        String messageText = String.format("""
                ```
                Рост числа платежей без финального статуса за последние 24h
                
                prov       | term       | count  
                --------------------------------
                %-10d | %-10d | %-10d
                %-10d | %-10d | %-10d
                %-10d | %-10d | %-10d
                ```
                """,
                314, 1234, 35,
                244, 7556, 42,
                345, 1129, 55);

        telegramApiService.sendMessage(chatId, properties.getThreads().getPendingPayments(), messageText, "MarkdownV2");
    }

    // Заглушка для отправки метрики "Рост числа упавших машин"
    public void sendFailedMachinesMetrics(Long chatId) {
        String messageText = String.format("""
                ```
                Рост числа упавших машин за последние 24h
                
                prov       | term       | count  
                --------------------------------
                %-10d | %-10d | %-10d
                %-10d | %-10d | %-10d
                %-10d | %-10d | %-10d
                ```
                """,
                221, 1569, 35,
                234, 7034, 42,
                595, 9032, 55);

        telegramApiService.sendMessage(chatId, properties.getThreads().getFailedMachines(), messageText, "MarkdownV2");
    }

    // Заглушка для отправки метрики "Конверсия альтернативных платежей"
    public void sendAltPayConversionMetrics(Long chatId) {
        String messageText = String.format("""
                        ```
                        Конверсия альтернативных платежей за последние 24h
                        
                        prov       | term       | state  | curr  | avg  
                        -----------------------------------------------
                        %-10d | %-10d | %-6s | %-5.2f | %-5.2f  
                        %-10d | %-10d | %-6s | %-5.2f | %-5.2f  
                        %-10d | %-10d | %-6s | %-5.2f | %-5.2f  
                        ```
                        """,
                160, 1456, "alive", 70.23, 61.56,
                240, 7234, "dead", 15.85, 90.02,
                538, 9456, "alive", 50.10, 40.20);

        telegramApiService.sendMessage(chatId, properties.getThreads().getAltpayConversion(),
                messageText, "MarkdownV2");
    }

    // Заглушка для отправки алерта по 5xx кодам для одного провайдера и терминала
    public String send5xxAlert() {
        return String.format("""
            ```
            Рост числа 5xx кодов при обращении к API процессинга за последние %dh

            Количество 5xx: %d
            ```
            """,
                24, 87);
    }
}
