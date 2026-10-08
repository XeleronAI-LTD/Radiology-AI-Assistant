package com.xeleronai.medicalimagingbackend.service;

import com.xeleronai.medicalimagingbackend.dto.detection.AnalyseStatutResponse;
import com.xeleronai.medicalimagingbackend.dto.detection.DetectionResponse;
import com.xeleronai.medicalimagingbackend.entity.Utilisateur;
import java.util.List;

public interface DetectionQueryService {

    /**
     * Liste les détections de toutes les images d'un examen, triées par
     * ordre d'image puis par id de détection.
     *
     * @throws RessourceIntrouvableException si l'examen n'existe pas.
     */
    List<DetectionResponse> listerParExamen(Long examenId);

    /**
     * Statut courant de l'analyse IA d'un examen (pour polling).
     *
     * @throws RessourceIntrouvableException si l'examen n'existe pas.
     */
    AnalyseStatutResponse statutAnalyse(Long examenId);

    /**
     * Lit le PNG d'un masque de segmentation et écrit l'AuditLog VIEW_MASQUE
     * (seulement après succès de la lecture MinIO).
     *
     * @throws RessourceIntrouvableException si la détection n'existe pas,
     *         n'appartient pas à l'examen, n'est pas de type MASQUE, ou n'a
     *         pas de masque stocké.
     * @throws LectureImpossibleException si MinIO est injoignable ou l'objet absent.
     */
    byte[] lireMasque(Long examenId, Long detectionId, Utilisateur utilisateurCourant);
}
