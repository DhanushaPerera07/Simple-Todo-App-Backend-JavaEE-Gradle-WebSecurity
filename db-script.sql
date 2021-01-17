create table user
(
    username varchar(50)  not null
        primary key,
    password varchar(255) not null
);

create table todo_item
(
    id       int auto_increment
        primary key,
    text     varchar(100)                                                                  null,
    priority enum ('PRIORITY1', 'PRIORITY2', 'PRIORITY3', 'PRIORITY4') default 'PRIORITY1' null,
    status   enum ('COMPLETED', 'NOT_COMPLETED')                                           null,
    username varchar(50)                                                                   null,
    constraint todo_item_ibfk_1
        foreign key (username) references user (username)
);

create index username
    on todo_item (username);


