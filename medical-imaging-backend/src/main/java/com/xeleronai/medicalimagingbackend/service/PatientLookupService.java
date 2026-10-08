package com.xeleronai.medicalimagingbackend.service;

import com.xeleronai.medicalimagingbackend.entity.Institution;
import com.xeleronai.medicalimagingbackend.entity.Patient;
import java.time.LocalDate;

public interface PatientLookupService {

    /**
     * Find-or-create par (institution, mrn). Si un Patient avec ce mrn
     * existe déjà dans cette institution, il est retourné tel quel
     * (nom/dateNaissance/sexe fournis sont ignorés, jamais utilisés pour
     * écraser une fiche existante). Sinon, un nouveau Patient est créé avec
     * ces valeurs, rattaché à cette institution. Un même mrn peut exister
     * dans deux institutions différentes sans conflit (unicité DB scopée
     * par institution, voir V17__alter_patient_mrn_unique_per_institution.sql).
     *
     * <p><b>Ne jamais appeler cette méthode depuis un contexte déjà
     * transactionnel</b> (voir ExamenPersistenceService) — elle doit
     * s'exécuter dans sa propre transaction, isolée de la transaction
     * d'écriture de l'examen, pour que le repêchage en cas de violation de
     * contrainte UNIQUE(institution_id, mrn) fonctionne (PostgreSQL passe
     * toute transaction en état "aborted" dès qu'une requête échoue en son
     * sein).
     */
    Patient trouverOuCreerPatient(String mrn, String nom, LocalDate dateNaissance, String sexe, Institution institution);
}
