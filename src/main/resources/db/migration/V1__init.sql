CREATE
EXTENSION IF NOT EXISTS pgcrypto;
CREATE SCHEMA IF NOT EXISTS alrt_tg_bot;

CREATE TABLE IF NOT EXISTS alrt_tg_bot.provider_terminal_topic (
    provider_id    CHARACTER VARYING NOT NULL,
    terminal_id    CHARACTER VARYING NOT NULL,
    thread_id      INTEGER,
    provider_name  CHARACTER VARYING,
    terminal_name  CHARACTER VARYING,
    name           CHARACTER VARYING,

    CONSTRAINT provider_terminal_topic_pk PRIMARY KEY (provider_id, terminal_id)
);