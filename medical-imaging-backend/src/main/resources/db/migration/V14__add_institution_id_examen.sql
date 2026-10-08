ALTER TABLE examen ADD COLUMN institution_id BIGINT;

UPDATE examen
SET institution_id = (SELECT id FROM institution WHERE code = 'DEFAULT');

ALTER TABLE examen ALTER COLUMN institution_id SET NOT NULL;

ALTER TABLE examen
    ADD CONSTRAINT fk_examen_institution
    FOREIGN KEY (institution_id) REFERENCES institution(id);

CREATE INDEX idx_examen_institution_id ON examen(institution_id);
