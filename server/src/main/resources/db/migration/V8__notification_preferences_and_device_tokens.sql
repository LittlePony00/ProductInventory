create table if not exists notification_settings (
    user_id uuid primary key,
    expiration_reminders_enabled boolean not null default true,
    low_stock_reminders_enabled boolean not null default true,
    push_enabled boolean not null default true,
    expiration_reminder_days integer not null default 3,
    updated_at timestamp not null
);

create table if not exists notification_device_tokens (
    id uuid primary key,
    user_id uuid not null,
    token varchar(2048) not null unique,
    platform varchar(32) not null,
    active boolean not null default true,
    created_at timestamp not null,
    last_seen_at timestamp not null
);

create index if not exists idx_notification_device_tokens_user_active
    on notification_device_tokens (user_id, active);
