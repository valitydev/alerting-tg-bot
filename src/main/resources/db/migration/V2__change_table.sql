ALTER TABLE IF EXISTS alert_tg_bot.provider_terminal_thread
    RENAME TO provider_thread;

ALTER TABLE IF EXISTS alert_tg_bot.provider_thread
    DROP CONSTRAINT IF EXISTS provider_terminal_thread_provider_terminal_uk;

ALTER TABLE IF EXISTS alert_tg_bot.provider_thread
    DROP COLUMN IF EXISTS terminal_id,
    DROP COLUMN IF EXISTS terminal_name;

DELETE FROM alert_tg_bot.provider_thread current_row
    USING alert_tg_bot.provider_thread duplicate_row
WHERE current_row.provider_id = duplicate_row.provider_id
    AND current_row.id > duplicate_row.id;

ALTER TABLE IF EXISTS alert_tg_bot.provider_thread
    ADD CONSTRAINT provider_thread_provider_uk UNIQUE (provider_id);