package com.xeleronai.medicalimagingbackend.service;

import com.xeleronai.medicalimagingbackend.entity.enums.ExamenZoneEnum;
import java.time.LocalDate;

/**
 * Métadonnées extraites d'un fichier DICOM. Type interne, jamais exposé tel
 * quel via l'API (voir ExamenUploadResponse / dto/examen).
 */
public record DicomMetadata(
        String mrn,
        String nom,
        LocalDate dateNaissance,
        String sexe,
        LocalDate dateExamen,
        String modalite,
        ExamenZoneEnum zone,
        String type,
        String studyInstanceUid
) {
}
