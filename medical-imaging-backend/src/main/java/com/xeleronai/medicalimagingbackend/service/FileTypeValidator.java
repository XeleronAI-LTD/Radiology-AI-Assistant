package com.xeleronai.medicalimagingbackend.service;

import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Validation des fichiers d'un batch d'upload : extension, batch non vide.
 * Classe utilitaire sans état ni accès base — pas de split interface/impl
 * (cette convention ne concerne que les services avec logique métier).
 */
@Component
public class FileTypeValidator {

    private static final Set<String> EXTENSIONS_STANDARD = Set.of("png", "jpg", "jpeg");

    public void validerBatchNonVide(List<MultipartFile> fichiers) {
        if (fichiers == null || fichiers.isEmpty()) {
            throw new UploadValidationException("Le batch ne peut pas être vide");
        }
    }

    public void validerExtensionsStandard(List<MultipartFile> fichiers) {
        for (MultipartFile f : fichiers) {
            if (!EXTENSIONS_STANDARD.contains(extensionDe(f).toLowerCase())) {
                throw new UploadValidationException(
                        "Extension non autorisée pour l'upload standard : " + f.getOriginalFilename());
            }
        }
    }

    public void validerExtensionsDicom(List<MultipartFile> fichiers) {
        for (MultipartFile f : fichiers) {
            if (!"dcm".equalsIgnoreCase(extensionDe(f))) {
                throw new UploadValidationException(
                        "Extension non autorisée pour l'upload DICOM (attendu .dcm) : " + f.getOriginalFilename());
            }
        }
    }

    public static String extensionDe(MultipartFile fichier) {
        String nom = fichier.getOriginalFilename();
        int idx = (nom == null) ? -1 : nom.lastIndexOf('.');
        if (idx < 0 || idx == nom.length() - 1) {
            throw new UploadValidationException("Fichier sans extension : " + nom);
        }
        return nom.substring(idx + 1);
    }
}
