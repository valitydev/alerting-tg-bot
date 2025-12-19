package dev.vality.alerting.tg.bot.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
    private Label groupLabels;
    private Label commonLabels;
    private Annotation commonAnnotations;
    private String externalURL;
    private String version;
    private String groupKey;
    private Integer truncatedAlerts;

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Alert {
        private String status;
        private Label labels;
        private Annotation annotations;
        private String startsAt;
        private String endsAt;
        private String generatorURL;
        private String fingerprint;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Label {
        private String alertname;
        @JsonProperty("provider_id")
        private String providerId;
        @JsonProperty("provider_name")
        private String providerName;
        @JsonProperty("terminal_id")
        private String terminalId;
        @JsonProperty("terminal_name")
        private String terminalName;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Annotation {
        private String description;
        private String summary;
    }
}
