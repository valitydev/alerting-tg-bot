package dev.vality.alerting.tg.bot.dao;

import dev.vality.alerting.tg.bot.pojo.ProviderTerminalTopic;
import dev.vality.dao.impl.AbstractGenericDao;
import dev.vality.mapper.RecordRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Optional;

import static dev.vality.alerting.tg.bot.domain.Tables.PROVIDER_TERMINAL_TOPIC;

@Component
public class ProviderTerminalTopicDao extends AbstractGenericDao {

    private final RowMapper<ProviderTerminalTopic> rowMapper;

    public ProviderTerminalTopicDao(DataSource dataSource, RowMapper<ProviderTerminalTopic> rowMapper) {
        super(dataSource);
        this.rowMapper = new RecordRowMapper<>(PROVIDER_TERMINAL_TOPIC, ProviderTerminalTopic.class);
    }

    public Optional<ProviderTerminalTopic> findByProviderAndTerminal(String providerId, String terminalId) {
        var query = getDslContext()
                .selectFrom(PROVIDER_TERMINAL_TOPIC)
                .where(PROVIDER_TERMINAL_TOPIC.PROVIDER_ID.eq(providerId))
                .and(PROVIDER_TERMINAL_TOPIC.TERMINAL_ID.eq(terminalId));

        return Optional.ofNullable(fetchOne(query, rowMapper));
    }

    public int insert(ProviderTerminalTopic topic) {
        var record = getDslContext().newRecord(PROVIDER_TERMINAL_TOPIC, topic);
        var query = getDslContext()
                .insertInto(PROVIDER_TERMINAL_TOPIC)
                .set(record);

        return execute(query);
    }

    public int upsert(ProviderTerminalTopic topic) {
        var query = getDslContext()
                .insertInto(PROVIDER_TERMINAL_TOPIC)
                .set(PROVIDER_TERMINAL_TOPIC.PROVIDER_ID, topic.getProviderId())
                .set(PROVIDER_TERMINAL_TOPIC.TERMINAL_ID, topic.getTerminalId())
                .set(PROVIDER_TERMINAL_TOPIC.THREAD_ID, topic.getThreadId())
                .set(PROVIDER_TERMINAL_TOPIC.PROVIDER_NAME, topic.getProviderName())
                .set(PROVIDER_TERMINAL_TOPIC.TERMINAL_NAME, topic.getTerminalName())
                .set(PROVIDER_TERMINAL_TOPIC.NAME, topic.getName())
                .onConflict(PROVIDER_TERMINAL_TOPIC.PROVIDER_ID, PROVIDER_TERMINAL_TOPIC.TERMINAL_ID)
                .doUpdate()
                .set(PROVIDER_TERMINAL_TOPIC.THREAD_ID, topic.getThreadId())
                .set(PROVIDER_TERMINAL_TOPIC.PROVIDER_NAME, topic.getProviderName())
                .set(PROVIDER_TERMINAL_TOPIC.TERMINAL_NAME, topic.getTerminalName())
                .set(PROVIDER_TERMINAL_TOPIC.NAME, topic.getName());

        return execute(query);
    }
}
