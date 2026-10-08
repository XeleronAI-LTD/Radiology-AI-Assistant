CREATE TABLE patient (
    id             BIGSERIAL PRIMARY KEY,
    mrn            VARCHAR(50) NOT NULL UNIQUE,
    nom            VARCHAR(255),
    date_naissance DATE,
    sexe           VARCHAR(10)
);
