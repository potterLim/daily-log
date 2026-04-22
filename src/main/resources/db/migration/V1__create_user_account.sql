create table user_account (
    id bigint not null auto_increment,
    user_name varchar(100) not null,
    password_hash varchar(255) not null,
    user_role varchar(30) not null,
    enabled bit not null,
    locked bit not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    constraint pk_user_account primary key (id),
    constraint uk_user_account_user_name unique (user_name)
);
