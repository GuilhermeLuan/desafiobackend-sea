CREATE TABLE tb_solicitations
(
    id             BIGSERIAL PRIMARY KEY,
    owner_id       BIGINT      NOT NULL,
    status         VARCHAR(20) NOT NULL,
    current_step   SMALLINT    NOT NULL DEFAULT 1,

    -- etapa 1: dados básicos
    service_type     VARCHAR(20),
    title            VARCHAR(80),
    description      VARCHAR(1000),

    -- etapa 2: endereço (state é a UF vinda do ViaCEP e liga a solicitação à cobertura)
    cep              VARCHAR(9),
    street           VARCHAR(255),
    neighborhood     VARCHAR(255),
    city             VARCHAR(255),
    state            CHAR(2),
    number           VARCHAR(20),
    complement       VARCHAR(255),

    -- etapa 3: confirmação
    priority         VARCHAR(10),
    preferred_date   DATE,
    estimated_value  NUMERIC(12, 2),
    terms_accepted BOOLEAN     NOT NULL DEFAULT FALSE,

    -- decisão do analista
    decided_by       BIGINT,
    decision_comment VARCHAR(1000),

    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    submitted_at     TIMESTAMPTZ,

    CONSTRAINT fk_tb_solicitations_owner FOREIGN KEY (owner_id) REFERENCES tb_users (id),
    CONSTRAINT fk_tb_solicitations_decided_by FOREIGN KEY (decided_by) REFERENCES tb_users (id),

    CONSTRAINT ck_tb_solicitations_status CHECK (status IN ('DRAFT', 'SUBMITTED', 'IN_REVIEW', 'APPROVED', 'REJECTED')),
    CONSTRAINT ck_tb_solicitations_current_step CHECK (current_step BETWEEN 1 AND 3),
    CONSTRAINT ck_tb_solicitations_service_type CHECK (service_type IN ('INSTALLATION', 'MAINTENANCE', 'INSPECTION')),
    CONSTRAINT ck_tb_solicitations_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT ck_tb_solicitations_estimated_value CHECK (estimated_value >= 0),
    CONSTRAINT ck_tb_solicitations_state CHECK (state IN (
                                                          'AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES', 'GO',
                                                          'MA', 'MT', 'MS', 'MG', 'PA', 'PB', 'PR', 'PE', 'PI',
                                                          'RJ', 'RN', 'RS', 'RO', 'RR', 'SC', 'SP', 'SE', 'TO'
        )),
    -- quem decidiu e o comentário andam juntos: ou os dois existem, ou nenhum
    CONSTRAINT ck_tb_solicitations_decision CHECK ((decided_by IS NULL) = (decision_comment IS NULL))
);

-- listagem das solicitações de um cliente e checagem de dono
CREATE INDEX ix_tb_solicitations_owner ON tb_solicitations (owner_id);

-- consultas do analista por UF e status
CREATE INDEX ix_tb_solicitations_state_status ON tb_solicitations (state, status);
