alter table weekly_operation_metric_snapshot
    drop column email_verification_mail_sent_count;

alter table weekly_operation_metric_snapshot
    drop column email_verification_mail_failed_count;

alter table weekly_operation_metric_snapshot
    drop column email_verified_count;

alter table weekly_operation_metric_snapshot
    drop column password_reset_requested_count;

alter table weekly_operation_metric_snapshot
    drop column password_reset_mail_sent_count;

alter table weekly_operation_metric_snapshot
    drop column password_reset_mail_failed_count;

alter table weekly_operation_metric_snapshot
    drop column password_reset_completed_count;
