CREATE TABLE examen (
    id          BIGSERIAL PRIMARY KEY,
    patient_id  BIGINT       NOT NULL,
    cree_par    BIGINT,
    type        VARCHAR(100),
    date_examen DATE,
    modalite    VARCHAR(50),
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_examen_patient  FOREIGN KEY (patient_id) REFERENCES patient(id)     ON DELETE RESTRICT,
    CONSTRAINT fk_examen_cree_par FOREIGN KEY (cree_par)   REFERENCES utilisateur(id) ON DELETE SET NULL
);

CREATE INDEX idx_examen_patient_id ON examen(patient_id);
