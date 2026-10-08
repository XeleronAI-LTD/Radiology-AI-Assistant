package com.xeleronai.medicalimagingbackend.service;

import com.xeleronai.medicalimagingbackend.entity.Utilisateur;

/**
 * Lance et exécute l'analyse IA d'un examen. Le vrai travail (lecture
 * MinIO, appel ai-detection-service, persistance) s'exécute en tâche de
 * fond : l'inférence locale mesurée prend jusqu'à ~31 min, bien trop long
 * pour une requête HTTP synchrone.
 */
public interface DetectionAnalyseService {

    /**
     * Valide et fait passer l'examen en EN_COURS. Synchrone, appelé dans le
     * thread de la requête HTTP — répond avant que l'inférence ne démarre.
     *
     * @throws RessourceIntrouvableException examen absent.
     * @throws AnalyseNonLancableException modalité/zone manquante (ou UNKNOWN) ou aucune image.
     * @throws AnalyseEnCoursException une analyse est déjà EN_COURS pour cet examen.
     */
    void lancerAnalyse(Long examenId, Utilisateur utilisateurCourant);

    /**
     * Exécute le vrai travail (MinIO + appel IA + persistance), en tâche de
     * fond. Ne lève jamais d'exception vers l'appelant : toute erreur est
     * capturée et se traduit par un passage à statutAnalyse=ECHOUEE.
     */
    void analyserEnArrierePlan(Long examenId, Long utilisateurId);
}
