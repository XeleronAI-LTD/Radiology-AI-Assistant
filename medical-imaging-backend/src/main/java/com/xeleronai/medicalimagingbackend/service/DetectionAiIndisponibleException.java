package com.xeleronai.medicalimagingbackend.service;

/**
 * Levée par DetectionAiClient pour toute erreur réseau, timeout, réponse
 * HTTP non-2xx, ou corps de réponse non parsable en provenance de
 * ai-detection-service. Traitée par DetectionAnalyseServiceImpl comme un
 * échec de run (statutAnalyse = ECHOUEE), jamais comme une 503 exposée
 * telle quelle à l'appelant (l'appel est asynchrone, personne n'attend la
 * requête HTTP d'origine).
 */
public class DetectionAiIndisponibleException extends RuntimeException {

    public DetectionAiIndisponibleException(String message, Throwable cause) {
        super(message, cause);
    }
}
