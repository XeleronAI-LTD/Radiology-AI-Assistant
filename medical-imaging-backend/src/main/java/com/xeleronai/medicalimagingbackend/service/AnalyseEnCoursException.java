package com.xeleronai.medicalimagingbackend.service;

/**
 * Levée par DetectionAnalyseServiceImpl#lancerAnalyse quand statutAnalyse
 * vaut déjà EN_COURS pour l'examen visé (évite le double lancement).
 * Mappée en 409 par ExamenController.
 */
public class AnalyseEnCoursException extends RuntimeException {

    public AnalyseEnCoursException(String message) {
        super(message);
    }
}
