package dev.vality.alerting.tg.bot.pojo;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class ProviderThread implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Integer threadId;
    private String providerId;
    private String providerName;
    private String name;
}
