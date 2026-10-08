CREATE TABLE image (
    id            BIGSERIAL PRIMARY KEY,
    examen_id     BIGINT NOT NULL,
    chemin_dcm    VARCHAR(2048),
    chemin_apercu VARCHAR(2048),
    format        VARCHAR(50),
    ordre         INTEGER,
    CONSTRAINT fk_image_examen FOREIGN KEY (examen_id) REFERENCES examen(id) ON DELETE CASCADE
);

CREATE INDEX idx_image_examen_id ON image(examen_id);
