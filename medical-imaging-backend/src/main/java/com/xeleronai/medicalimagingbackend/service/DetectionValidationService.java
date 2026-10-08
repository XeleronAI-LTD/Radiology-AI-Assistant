package com.xeleronai.medicalimagingbackend.service;

import com.xeleronai.medicalimagingbackend.dto.detection.DetectionResponse;
import com.xeleronai.medicalimagingbackend.entity.Utilisateur;

public interface DetectionValidationService {

    DetectionResponse validerStatut(
            Long examenId, Long detectionId, String statutDemande, Utilisateur utilisateurCourant);
}
