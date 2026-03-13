package dev.vality.alerting.tg.bot.dao;

import dev.vality.alerting.tg.bot.pojo.ProviderThread;
import dev.vality.dao.impl.AbstractGenericDao;
import dev.vality.mapper.RecordRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Optional;

import static dev.vality.alerting.tg.bot.domain.Tables.PROVIDER_THREAD;

@Component
public class ProviderThreadDao extends AbstractGenericDao {

    private final RowMapper<ProviderThread> rowMapper;

    public ProviderThreadDao(DataSource dataSource) {
        super(dataSource);
        this.rowMapper = new RecordRowMapper<>(PROVIDER_THREAD, ProviderThread.class);
    }

    public Optional<ProviderThread> findByProvider(String providerId) {
        var query = getDslContext()
                .selectFrom(PROVIDER_THREAD)
                .where(PROVIDER_THREAD.PROVIDER_ID.eq(providerId));

        return Optional.ofNullable(fetchOne(query, rowMapper));
    }

    public int insert(ProviderThread thread) {
        var record = getDslContext().newRecord(PROVIDER_THREAD, thread);
        var query = getDslContext()
                .insertInto(PROVIDER_THREAD)
                .set(record);

        return execute(query);
    }

    public int upsert(ProviderThread thread) {
        var query = getDslContext()
                .insertInto(PROVIDER_THREAD)
                .set(PROVIDER_THREAD.PROVIDER_ID, thread.getProviderId())
                .set(PROVIDER_THREAD.THREAD_ID, thread.getThreadId())
                .set(PROVIDER_THREAD.PROVIDER_NAME, thread.getProviderName())
                .set(PROVIDER_THREAD.NAME, thread.getName())
                .onConflict(PROVIDER_THREAD.PROVIDER_ID)
                .doUpdate()
                .set(PROVIDER_THREAD.THREAD_ID, thread.getThreadId())
                .set(PROVIDER_THREAD.PROVIDER_NAME, thread.getProviderName())
                .set(PROVIDER_THREAD.NAME, thread.getName());

        return execute(query);
    }
}
