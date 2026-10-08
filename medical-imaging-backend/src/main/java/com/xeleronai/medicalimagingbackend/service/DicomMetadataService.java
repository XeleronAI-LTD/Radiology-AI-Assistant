package com.xeleronai.medicalimagingbackend.service;

import org.springframework.web.multipart.MultipartFile;

public interface DicomMetadataService {

    /**
     * Extrait les métadonnées d'un fichier DICOM (tags PatientID, PatientName,
     * PatientBirthDate, PatientSex, StudyDate, Modality, StudyDescription,
     * StudyInstanceUID).
     *
     * @throws UploadValidationException si le fichier est illisible (structure
     *         DICOM invalide) ou si le tag PatientID est absent/vide.
     */
    DicomMetadata extraire(MultipartFile fichierDicom);
}
