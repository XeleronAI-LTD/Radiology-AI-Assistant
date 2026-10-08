package com.xeleronai.medicalimagingbackend.service;

/**
 * Levée quand un objet attendu en base n'a pas pu être lu depuis MinIO
 * (absent malgré la garantie d'atomicité de l'upload, ou MinIO injoignable).
 * Mappée en 503 par ExamenController.
 */
public class LectureImpossibleException extends RuntimeException {

    public LectureImpossibleException(String message, Throwable cause) {
        super(message, cause);
    }
}
