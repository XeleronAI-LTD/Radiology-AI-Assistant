ALTER TABLE utilisateur ADD COLUMN institution_id BIGINT;

UPDATE utilisateur
SET institution_id = (SELECT id FROM institution WHERE code = 'DEFAULT');

ALTER TABLE utilisateur ALTER COLUMN institution_id SET NOT NULL;

ALTER TABLE utilisateur
    ADD CONSTRAINT fk_utilisateur_institution
    FOREIGN KEY (institution_id) REFERENCES institution(id);

CREATE INDEX idx_utilisateur_institution_id ON utilisateur(institution_id);
