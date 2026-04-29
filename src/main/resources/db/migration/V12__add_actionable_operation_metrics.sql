alter table weekly_operation_metric_snapshot
    add column weekly_plan_review_completed_days bigint not null default 0;

alter table weekly_operation_metric_snapshot
    add column weekly_review_viewed_count bigint not null default 0;

alter table weekly_operation_metric_snapshot
    add column exporting_users bigint not null default 0;

alter table weekly_operation_metric_snapshot
    add column new_workspace_activated_users bigint not null default 0;

alter table weekly_operation_metric_snapshot
    add column plan_review_conversion_rate_percent double precision not null default 0;

alter table weekly_operation_metric_snapshot
    add column new_workspace_activation_rate_percent double precision not null default 0;
