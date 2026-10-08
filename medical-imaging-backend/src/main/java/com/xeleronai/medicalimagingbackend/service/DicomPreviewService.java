package com.xeleronai.medicalimagingbackend.service;

import java.util.Optional;
import org.springframework.web.multipart.MultipartFile;

public interface DicomPreviewService {

    /**
     * Génère un aperçu PNG à partir des pixels d'un fichier DICOM. Ne lève
     * jamais d'exception : retourne Optional.empty() si la génération échoue
     * (image illisible, format de pixel non supporté...) — l'échec ne doit
     * jamais bloquer l'upload (règle métier).
     */
    Optional<byte[]> genererApercuPng(MultipartFile fichierDicom);
}
