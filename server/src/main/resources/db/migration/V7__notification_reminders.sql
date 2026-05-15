alter table notifications
    add column if not exists type varchar(64) not null default 'GENERAL';

alter table notifications
    add column if not exists household_id uuid;

alter table notifications
    add column if not exists product_id uuid;

alter table notifications
    add column if not exists dedupe_key varchar(255);

create unique index if not exists idx_notifications_user_dedupe_key
    on notifications (user_id, dedupe_key);
