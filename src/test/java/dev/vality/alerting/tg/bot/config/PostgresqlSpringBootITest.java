package dev.vality.alerting.tg.bot.config;

import dev.vality.testcontainers.annotations.postgresql.PostgresqlTestcontainer;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@PostgresqlTestcontainer
public @interface PostgresqlSpringBootITest {
}
