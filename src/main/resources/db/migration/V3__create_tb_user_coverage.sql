CREATE TABLE tb_user_coverage (
    user_id BIGINT  NOT NULL,
    uf      CHAR(2) NOT NULL,
    PRIMARY KEY (user_id, uf),
    CONSTRAINT fk_tb_user_coverage_user FOREIGN KEY (user_id) REFERENCES tb_users (id) ON DELETE CASCADE,
    CONSTRAINT ck_tb_user_coverage_uf CHECK (uf IN (
        'AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES', 'GO',
        'MA', 'MT', 'MS', 'MG', 'PA', 'PB', 'PR', 'PE', 'PI',
        'RJ', 'RN', 'RS', 'RO', 'RR', 'SC', 'SP', 'SE', 'TO'
    ))
);
