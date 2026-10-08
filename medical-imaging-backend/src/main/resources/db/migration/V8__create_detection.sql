CREATE TABLE detection (
    id            BIGSERIAL PRIMARY KEY,
    image_id      BIGINT           NOT NULL,
    type          VARCHAR(20)      NOT NULL,
    anomalie      VARCHAR(100),
    confiance     DOUBLE PRECISION NOT NULL,
    statut        VARCHAR(20)      NOT NULL DEFAULT 'EN_ATTENTE',
    coupe         INTEGER,
    x             INTEGER,
    y             INTEGER,
    largeur       INTEGER,
    hauteur       INTEGER,
    chemin_masque VARCHAR(255),
    created_at    TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_detection_image FOREIGN KEY (image_id) REFERENCES image(id) ON DELETE CASCADE
);

CREATE INDEX idx_detection_image_id ON detection(image_id);
