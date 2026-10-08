-- Supprime la contrainte UNIQUE existante sur patient.mrn (contrainte
-- inline non nommée explicitement dans V2, donc nom généré par PostgreSQL —
-- recherchée dynamiquement ici plutôt que supposée, pour ne pas dépendre
-- d'un nom par défaut qui peut varier selon l'environnement).
DO $$
DECLARE
    nom_contrainte TEXT;
BEGIN
    SELECT conname INTO nom_contrainte
    FROM pg_constraint
    WHERE conrelid = 'patient'::regclass
      AND contype = 'u'
      AND conkey = ARRAY[
          (SELECT attnum FROM pg_attribute WHERE attrelid = 'patient'::regclass AND attname = 'mrn')
      ];

    IF nom_contrainte IS NOT NULL THEN
        EXECUTE format('ALTER TABLE patient DROP CONSTRAINT %I', nom_contrainte);
    END IF;
END $$;

ALTER TABLE patient
    ADD CONSTRAINT uq_patient_institution_mrn UNIQUE (institution_id, mrn);
