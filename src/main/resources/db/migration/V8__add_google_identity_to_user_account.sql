alter table user_account
    add column google_subject varchar(255) null;

alter table user_account
    add column google_connected_at datetime(6) null;

create unique index uk_user_account_google_subject
    on user_account (google_subject);
