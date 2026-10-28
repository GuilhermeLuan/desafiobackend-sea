CREATE TABLE tb_users (
    id            BIGSERIAL    PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    CONSTRAINT uk_tb_users_email UNIQUE (email),
    CONSTRAINT ck_tb_users_role CHECK (role IN ('CLIENT', 'ANALYST', 'ADMIN'))
);
