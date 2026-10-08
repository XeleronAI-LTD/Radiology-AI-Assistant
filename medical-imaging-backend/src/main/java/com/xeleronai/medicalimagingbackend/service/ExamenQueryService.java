package com.xeleronai.medicalimagingbackend.service;

import com.xeleronai.medicalimagingbackend.dto.common.PageResponse;
import com.xeleronai.medicalimagingbackend.dto.examen.ExamenDetailResponse;
import com.xeleronai.medicalimagingbackend.dto.examen.ExamenSummaryResponse;
import com.xeleronai.medicalimagingbackend.entity.Utilisateur;

public interface ExamenQueryService {

    PageResponse<ExamenSummaryResponse> lister(String mrn, int page, int size, Utilisateur utilisateurCourant);

    ExamenDetailResponse detail(Long examenId);

    /**
     * Lit l'aperçu PNG d'une image, garde l'accès (existence, appartenance à
     * l'examen, aperçu disponible), puis écrit une ligne d'audit VIEW_IMAGE
     * uniquement après succès de la lecture MinIO.
     */
    byte[] lireApercu(Long examenId, Long imageId, Utilisateur utilisateurCourant);
}
