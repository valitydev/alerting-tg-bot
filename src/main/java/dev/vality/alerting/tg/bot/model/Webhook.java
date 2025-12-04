package dev.vality.alerting.tg.bot.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
        @JsonProperty("api_type")
        private String apiType;
        private String code;
        private String prometheus;
        private String team;
        private String url;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Annotation {
        private String description;
        private String summary;
    }
}
