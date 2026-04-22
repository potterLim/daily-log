alter table user_account
    add column email varchar(255);

update user_account
set email = concat(user_name, '@local.invalid')
where email is null
   or email = '';

alter table user_account
    modify column email varchar(255) not null;

alter table user_account
    add constraint uk_user_account_email unique (email);
