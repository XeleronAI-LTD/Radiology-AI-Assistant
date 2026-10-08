package com.xeleronai.medicalimagingbackend.service;

import java.util.List;

/**
 * Appelle ai-detection-service (POST /predict). Ne fait aucun accès base ni
 * stockage — reçoit les octets déjà lus par l'appelant (le service IA
 * n'accède jamais lui-même à MinIO, voir ai-detection-service/CLAUDE.md).
 * Appelé uniquement depuis un service (DetectionAnalyseServiceImpl), jamais
 * depuis un controller.
 */
public interface DetectionAiClient {

    /**
     * @param fichiersDicomOrdonnes octets bruts des fichiers .dcm de la
     *                              série, dans l'ordre des coupes.
     * @param modalite              ex. "CT" (mis en majuscules avant envoi).
     * @param zone                  ex. "THORAX" (nom de l'enum, déjà en majuscules).
     * @throws DetectionAiIndisponibleException réseau, timeout, HTTP non-2xx, parsing.
     */
    PredictionIA predict(List<byte[]> fichiersDicomOrdonnes, String modalite, String zone);
}
