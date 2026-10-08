package com.xeleronai.medicalimagingbackend.service;

import com.xeleronai.medicalimagingbackend.entity.Examen;
import com.xeleronai.medicalimagingbackend.entity.Patient;
import com.xeleronai.medicalimagingbackend.entity.Utilisateur;
import com.xeleronai.medicalimagingbackend.entity.enums.ExamenZoneEnum;
import java.time.LocalDate;
import java.util.List;

public interface ExamenPersistenceService {

    /**
     * Persiste l'Examen, ses Image et la ligne AuditLog UPLOAD_EXAMEN dans une
     * seule transaction. Le Patient doit déjà être résolu (find-or-create fait
     * en amont, voir PatientLookupService) — cette méthode ne fait plus aucun
     * find-or-create.
     */
    Examen persisterExamen(
            Patient patient,
            Utilisateur utilisateurCourant,
            String type,
            LocalDate dateExamen,
            String modalite,
            ExamenZoneEnum zone,
            List<ImagePreparee> images);
}
