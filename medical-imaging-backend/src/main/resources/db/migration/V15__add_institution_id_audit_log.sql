ALTER TABLE audit_log ADD COLUMN institution_id BIGINT;

UPDATE audit_log
SET institution_id = (SELECT id FROM institution WHERE code = 'DEFAULT');

ALTER TABLE audit_log ALTER COLUMN institution_id SET NOT NULL;

ALTER TABLE audit_log
    ADD CONSTRAINT fk_audit_log_institution
    FOREIGN KEY (institution_id) REFERENCES institution(id);

CREATE INDEX idx_audit_log_institution_id ON audit_log(institution_id);
