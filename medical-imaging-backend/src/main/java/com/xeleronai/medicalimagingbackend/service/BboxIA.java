package com.xeleronai.medicalimagingbackend.service;

/**
 * Rectangle 2D brut reçu de ai-detection-service (pixels de la coupe).
 * Type interne, jamais exposé tel quel via l'API (voir
 * dto/detection/BboxResponse pour la version exposée, en Integer).
 */
public record BboxIA(Double x, Double y, Double w, Double h) {
}
