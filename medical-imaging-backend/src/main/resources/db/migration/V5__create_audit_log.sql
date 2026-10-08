CREATE TABLE audit_log (
    id             BIGSERIAL PRIMARY KEY,
    utilisateur_id BIGINT,
    action         VARCHAR(100) NOT NULL,
    resource_type  VARCHAR(50),
    resource_id    BIGINT,
    date_action    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_log_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES utilisateur(id) ON DELETE SET NULL
);

CREATE INDEX idx_audit_log_utilisateur_id ON audit_log(utilisateur_id);
