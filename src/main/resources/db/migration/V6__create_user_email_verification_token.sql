create table user_email_verification_token (
    id bigint not null auto_increment,
    user_account_id bigint not null,
    token_hash varchar(64) not null,
    expires_at datetime(6) not null,
    consumed_at datetime(6) null,
    created_at datetime(6) not null,
    constraint pk_user_email_verification_token primary key (id),
    constraint fk_user_email_verification_token_user_account foreign key (user_account_id) references user_account (id),
    constraint uk_user_email_verification_token_token_hash unique (token_hash)
);
