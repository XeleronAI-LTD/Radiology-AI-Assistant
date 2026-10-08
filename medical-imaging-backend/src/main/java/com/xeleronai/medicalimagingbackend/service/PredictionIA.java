package com.xeleronai.medicalimagingbackend.service;

import java.util.List;

/**
 * Réponse brute complète de POST /predict (ai-detection-service). `type`
 * vaut "BOX" (détection) ou "MASQUE" (segmentation) selon le modèle routé
 * côté service IA pour ce (modalite, zone) — jamais les deux dans une même
 * réponse. Gardé en String plutôt que typé en enum, par simplicité.
 * Type interne, jamais exposé tel quel via l'API.
 */
public record PredictionIA(String type, List<DetectionIA> detections) {
}
