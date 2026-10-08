ALTER TABLE detection ADD COLUMN validateur_id BIGINT;
ALTER TABLE detection ADD COLUMN valide_le TIMESTAMP;

ALTER TABLE detection
    ADD CONSTRAINT fk_detection_validateur
    FOREIGN KEY (validateur_id) REFERENCES utilisateur(id);
