package com.xeleronai.medicalimagingbackend.service;

/**
 * Levée quand l'upload échoue après le début du stockage MinIO : échec de
 * stockage lui-même, ou échec de la transaction base après stockage réussi.
 * Dans les deux cas, la réponse et la mécanique de compensation (suppression
 * des objets déjà poussés pour ce batch) sont identiques. Mappée en 503 par
 * ExamenController.
 */
public class UploadEchoueException extends RuntimeException {

    public UploadEchoueException(String message, Throwable cause) {
        super(message, cause);
    }
}
