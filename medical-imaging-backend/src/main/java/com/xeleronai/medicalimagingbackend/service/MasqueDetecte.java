package com.xeleronai.medicalimagingbackend.service;

/**
 * Un masque de segmentation déjà décodé et déjà stocké dans MinIO, prêt à
 * être persisté. Contrairement à DetectionIA (données brutes reçues de
 * l'IA, `masqueBase64` en mémoire), `cheminMasque` est la clé MinIO de
 * l'objet déjà uploadé — ce type marque la frontière entre le service async
 * (accès MinIO) et le service de persistance (DB uniquement, jamais
 * d'accès MinIO), voir DetectionAnalyseServiceImpl.
 */
public record MasqueDetecte(Integer coupe, String label, Double confiance, String cheminMasque) {
}
