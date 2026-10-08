ALTER TABLE patient ADD COLUMN institution_id BIGINT;

UPDATE patient
SET institution_id = (SELECT id FROM institution WHERE code = 'DEFAULT');

ALTER TABLE patient ALTER COLUMN institution_id SET NOT NULL;

ALTER TABLE patient
    ADD CONSTRAINT fk_patient_institution
    FOREIGN KEY (institution_id) REFERENCES institution(id);

CREATE INDEX idx_patient_institution_id ON patient(institution_id);
