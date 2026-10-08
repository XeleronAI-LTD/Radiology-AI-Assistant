CREATE TABLE institution (
    id         BIGSERIAL PRIMARY KEY,
    nom        VARCHAR(255) NOT NULL,
    code       VARCHAR(50)  NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_institution_code UNIQUE (code)
);
