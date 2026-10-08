package com.xeleronai.medicalimagingbackend.service;

/**
 * Levée quand une ressource demandée n'existe pas (examen, image, ou image
 * n'appartenant pas à l'examen indiqué), ou quand un aperçu n'a jamais été
 * généré (chemin_apercu vide). Mappée en 404 par ExamenController.
 */
public class RessourceIntrouvableException extends RuntimeException {

    public RessourceIntrouvableException(String message) {
        super(message);
    }
}
