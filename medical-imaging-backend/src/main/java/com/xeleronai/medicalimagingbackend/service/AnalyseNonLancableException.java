package com.xeleronai.medicalimagingbackend.service;

/**
 * Levée par DetectionAnalyseServiceImpl#lancerAnalyse quand l'examen n'a
 * pas de modalité/zone renseignée (zone UNKNOWN incluse — pas de routage
 * possible vers un modèle côté ai-detection-service), ou n'a aucune image.
 * Mappée en 422 par ExamenController.
 */
public class AnalyseNonLancableException extends RuntimeException {

    public AnalyseNonLancableException(String message) {
        super(message);
    }
}
