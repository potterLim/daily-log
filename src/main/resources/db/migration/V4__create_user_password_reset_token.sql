create table user_password_reset_token (
    id bigint not null auto_increment,
    user_account_id bigint not null,
    token_hash varchar(64) not null,
    expires_at datetime(6) not null,
    used_at datetime(6) null,
    created_at datetime(6) not null,
    constraint pk_user_password_reset_token primary key (id),
    constraint fk_user_password_reset_token_user_account foreign key (user_account_id) references user_account (id),
    constraint uk_user_password_reset_token_token_hash unique (token_hash)
);
