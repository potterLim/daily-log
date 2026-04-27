create table operation_usage_event (
    id bigint not null auto_increment,
    user_account_id bigint null,
    event_type varchar(80) not null,
    event_date date not null,
    occurred_at datetime(6) not null,
    constraint pk_operation_usage_event primary key (id),
    constraint fk_operation_usage_event_user_account foreign key (user_account_id) references user_account (id)
);

create index idx_operation_usage_event_occurred_at
    on operation_usage_event (occurred_at);

create index idx_operation_usage_event_event_type_occurred_at
    on operation_usage_event (event_type, occurred_at);

create index idx_operation_usage_event_user_account_id_occurred_at
    on operation_usage_event (user_account_id, occurred_at);

create table weekly_operation_metric_snapshot (
    id bigint not null auto_increment,
    week_start_date date not null,
    week_end_date date not null,
    generated_at datetime(6) not null,
    total_registered_users bigint not null,
    newly_registered_users bigint not null,
    weekly_active_users bigint not null,
    weekly_writing_users bigint not null,
    weekly_writing_days bigint not null,
    weekly_morning_entries bigint not null,
    weekly_evening_entries bigint not null,
    sign_in_succeeded_count bigint not null,
    sign_in_failed_count bigint not null,
    email_verification_mail_sent_count bigint not null,
    email_verification_mail_failed_count bigint not null,
    email_verified_count bigint not null,
    password_reset_requested_count bigint not null,
    password_reset_mail_sent_count bigint not null,
    password_reset_mail_failed_count bigint not null,
    password_reset_completed_count bigint not null,
    record_library_viewed_count bigint not null,
    markdown_exported_count bigint not null,
    pdf_export_viewed_count bigint not null,
    average_writing_days_per_active_user double precision not null,
    average_entry_completions_per_active_user double precision not null,
    goal_completion_rate_percent double precision not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    constraint pk_weekly_operation_metric_snapshot primary key (id),
    constraint uk_weekly_operation_metric_snapshot_week unique (week_start_date, week_end_date)
);

create index idx_weekly_operation_metric_snapshot_week_start_date
    on weekly_operation_metric_snapshot (week_start_date);

create index idx_user_account_created_at
    on user_account (created_at);

create index idx_daymark_entry_entry_date
    on daymark_entry (entry_date);
