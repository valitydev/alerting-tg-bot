package dev.vality.alerting.tg.bot;

import dev.vality.alerting.tg.bot.config.PostgresqlSpringBootITest;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

@PostgresqlSpringBootITest
@ActiveProfiles("test")
class DataBaseMigrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private DSLContext dslContext;

    @BeforeEach
    void setUp() {
        dslContext.execute("DROP SCHEMA IF EXISTS alert_tg_bot CASCADE");
        dslContext.execute("CREATE SCHEMA alert_tg_bot");
    }

    @Test
    void shouldApplyV2MigrationWithoutExceptionAfterDataFromV1() {
        runMigration("db/migration/V1__init.sql");

        dslContext.insertInto(table(name("alert_tg_bot", "provider_terminal_thread")))
                .columns(
                        field(name("provider_id")),
                        field(name("terminal_id")),
                        field(name("thread_id")),
                        field(name("provider_name")),
                        field(name("terminal_name")),
                        field(name("name"))
                )
                .values("provider-1", "terminal-1", 1, "Provider 1", "Terminal 1", "name-1")
                .execute();

        dslContext.insertInto(table(name("alert_tg_bot", "provider_terminal_thread")))
                .columns(
                        field(name("provider_id")),
                        field(name("terminal_id")),
                        field(name("thread_id")),
                        field(name("provider_name")),
                        field(name("terminal_name")),
                        field(name("name"))
                )
                .values("provider-1", "terminal-2", 2, "Provider 1", "Terminal 2", "name-2")
                .execute();

        assertThatCode(() -> runMigration("db/migration/V2__change_table.sql"))
                .doesNotThrowAnyException();

        int providersCount = dslContext.fetchCount(
                table(name("alert_tg_bot", "provider_thread")),
                field(name("provider_id")).eq("provider-1")
        );

        assertThat(providersCount).isEqualTo(1);
    }

    private void runMigration(String classpathMigrationFile) {
        ResourceDatabasePopulator populator =
                new ResourceDatabasePopulator(new ClassPathResource(classpathMigrationFile));
        populator.execute(dataSource);
    }
}

