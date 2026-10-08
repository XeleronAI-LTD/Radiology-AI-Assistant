package com.xeleronai.medicalimagingbackend.service;

/**
 * Levée pour tout batch d'upload invalide : batch vide, extension de fichier
 * non conforme à l'endpoint appelé, fichier DICOM illisible, incohérence
 * PatientID/StudyInstanceUID entre fichiers du même batch, mrn manquant.
 * Mappée en 400 par ExamenController.
 */
public class UploadValidationException extends RuntimeException {

    public UploadValidationException(String message) {
        super(message);
    }
}
