package com.xeleronai.medicalimagingbackend.service;

import com.xeleronai.medicalimagingbackend.entity.enums.FormatImageEnum;

/**
 * Représente une image déjà stockée dans MinIO, prête à être persistée en
 * base. Type interne, jamais exposé tel quel via l'API.
 */
public record ImagePreparee(
        String cheminOriginal,
        String cheminApercu,
        FormatImageEnum format,
        int ordre
) {
}
