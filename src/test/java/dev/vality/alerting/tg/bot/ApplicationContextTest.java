package dev.vality.alerting.tg.bot;

import dev.vality.alerting.tg.bot.config.PostgresqlSpringBootITest;
import dev.vality.alerting.tg.bot.dao.ProviderThreadDao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@PostgresqlSpringBootITest
@ActiveProfiles("test")
public class ApplicationContextTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void startApplicationContextTest() {
        assertThat(applicationContext).isNotNull();

        assertThat(applicationContext.getBean(ProviderThreadDao.class)).isNotNull();
    }
}
