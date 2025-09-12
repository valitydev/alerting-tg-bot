package dev.vality.alerting.tg.bot;

import dev.vality.alerting.tg.bot.config.properties.AlertmanagerWebhookProperties;
import dev.vality.alerting.tg.bot.controller.WebhookController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WebhookController.class)
@Import(WebhookControllerWithInterceptorTest.TestMvcConfig.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
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
class WebhookControllerWithInterceptorTest {

    @Autowired MockMvc mvc;

    @MockitoBean
    TelegramSender telegramSender;

    @MockitoBean
    AlertmanagerWebhookProperties alertmanagerWebhookProperties;

    @Autowired
    RequestMappingHandlerMapping mappings;

    @Test
    void mappingExists_sanityCheck() {
        var exists = mappings.getHandlerMethods().keySet().stream()
                .anyMatch(i -> i.getPatternsCondition().getPatterns().stream()
                        .anyMatch(p -> p.equals("/alertmanager/webhook")));
        assertThat(exists)
                .as("Контроллер смапил /alertmanager/webhook")
                .isTrue();
    }

    @Test
    void whenWebhookPosted_thenTestInterceptorInvokesTelegramSender() throws Exception {
        String json = """
                { "status":"firing", "alerts":[ { "status":"firing" } ] }
                """;

        mvc.perform(post("/alertmanager/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        verify(telegramSender, times(1))
                .send(argThat(s -> s.contains("webhook")));
    }

    @TestConfiguration
    static class TestMvcConfig implements WebMvcConfigurer {

        @Autowired TelegramSender telegramSender;

        @Bean
        HandlerInterceptor webhookNotifyInterceptor() {
            return new HandlerInterceptor() {
                @Override
                public void afterCompletion(HttpServletRequest request,
                                            HttpServletResponse response,
                                            Object handler,
                                            @Nullable Exception ex) {
                    if (request.getRequestURI().equals("/alertmanager/webhook") && response.getStatus() == 200) {
                        telegramSender.send("alertmanager webhook handled");
                    }
                }
            };
        }

        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(webhookNotifyInterceptor())
                    .addPathPatterns("/alertmanager/webhook");
        }
    }

    interface TelegramSender {
        void send(String text);
    }
}
