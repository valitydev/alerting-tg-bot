package dev.vality.alerting.tg.bot.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Alertmanager webhook body
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Webhook {

    private String status;
    private String receiver;
    private List<Alert> alerts;

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Alert {

        private String status;
        private Map<String, String> labels;
        private Map<String, String> annotations;

    }
}
