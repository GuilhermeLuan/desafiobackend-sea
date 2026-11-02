CREATE TABLE audit_logs
(
    id            BIGSERIAL PRIMARY KEY,
    occurred_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actor         VARCHAR(255) NOT NULL,
    action        VARCHAR(30)  NOT NULL,
    resource_type VARCHAR(30)  NOT NULL,
    resource_id   BIGINT,
    outcome       VARCHAR(10)  NOT NULL,
    error_type    VARCHAR(255),

    CONSTRAINT ck_audit_logs_action CHECK (action IN ('REGISTER', 'CREATE_USER', 'SUBMIT', 'START', 'DECIDE')
) ,
    CONSTRAINT ck_audit_logs_resource_type CHECK (resource_type IN ('USER', 'SOLICITATION')),
    CONSTRAINT ck_audit_logs_outcome CHECK (outcome IN ('SUCCESS', 'FAILURE')),
    CONSTRAINT ck_audit_logs_error_type CHECK ((outcome = 'FAILURE') = (error_type IS NOT NULL))
);

CREATE INDEX ix_audit_logs_resource ON audit_logs (resource_type, resource_id);

CREATE INDEX ix_audit_logs_actor ON audit_logs (actor, occurred_at);
