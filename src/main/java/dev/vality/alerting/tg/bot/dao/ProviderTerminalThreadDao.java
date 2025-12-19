package dev.vality.alerting.tg.bot.dao;

import dev.vality.alerting.tg.bot.pojo.ProviderTerminalThread;
import dev.vality.dao.impl.AbstractGenericDao;
import dev.vality.mapper.RecordRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Optional;

import static dev.vality.alerting.tg.bot.domain.Tables.PROVIDER_TERMINAL_THREAD;

@Component
public class ProviderTerminalThreadDao extends AbstractGenericDao {

    private final RowMapper<ProviderTerminalThread> rowMapper;

    public ProviderTerminalThreadDao(DataSource dataSource, RowMapper<ProviderTerminalThread> rowMapper) {
        super(dataSource);
        this.rowMapper = new RecordRowMapper<>(PROVIDER_TERMINAL_THREAD, ProviderTerminalThread.class);
    }

    public Optional<ProviderTerminalThread> findByProviderAndTerminal(String providerId, String terminalId) {
        var query = getDslContext()
                .selectFrom(PROVIDER_TERMINAL_THREAD)
                .where(PROVIDER_TERMINAL_THREAD.PROVIDER_ID.eq(providerId))
                .and(PROVIDER_TERMINAL_THREAD.TERMINAL_ID.eq(terminalId));

        return Optional.ofNullable(fetchOne(query, rowMapper));
    }

    public int insert(ProviderTerminalThread thread) {
        var record = getDslContext().newRecord(PROVIDER_TERMINAL_THREAD, thread);
        var query = getDslContext()
                .insertInto(PROVIDER_TERMINAL_THREAD)
                .set(record);

        return execute(query);
    }

    public int upsert(ProviderTerminalThread thread) {
        var query = getDslContext()
                .insertInto(PROVIDER_TERMINAL_THREAD)
                .set(PROVIDER_TERMINAL_THREAD.PROVIDER_ID, thread.getProviderId())
                .set(PROVIDER_TERMINAL_THREAD.TERMINAL_ID, thread.getTerminalId())
                .set(PROVIDER_TERMINAL_THREAD.THREAD_ID, thread.getThreadId())
                .set(PROVIDER_TERMINAL_THREAD.PROVIDER_NAME, thread.getProviderName())
                .set(PROVIDER_TERMINAL_THREAD.TERMINAL_NAME, thread.getTerminalName())
                .set(PROVIDER_TERMINAL_THREAD.NAME, thread.getName())
                .onConflict(PROVIDER_TERMINAL_THREAD.PROVIDER_ID, PROVIDER_TERMINAL_THREAD.TERMINAL_ID)
                .doUpdate()
                .set(PROVIDER_TERMINAL_THREAD.THREAD_ID, thread.getThreadId())
                .set(PROVIDER_TERMINAL_THREAD.PROVIDER_NAME, thread.getProviderName())
                .set(PROVIDER_TERMINAL_THREAD.TERMINAL_NAME, thread.getTerminalName())
                .set(PROVIDER_TERMINAL_THREAD.NAME, thread.getName());

        return execute(query);
    }
}
