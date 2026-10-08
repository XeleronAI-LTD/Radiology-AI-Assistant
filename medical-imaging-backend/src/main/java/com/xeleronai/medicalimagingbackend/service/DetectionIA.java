package com.xeleronai.medicalimagingbackend.service;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Une détection brute reçue de ai-detection-service (POST /predict).
 * `coupe` est l'index 0-based dans la série triée par position z — la même
 * base que Image.ordre, donc directement comparable sans décalage.
 * `bbox` n'est renseigné que pour une entrée de type BOX ; `masqueBase64`
 * (PNG encodé base64) n'est renseigné que pour une entrée de type MASQUE —
 * les deux ne sont jamais renseignés simultanément (voir PredictionIA.type).
 * `masque_base64` est le nom du champ JSON côté service IA (snake_case,
 * voir ai-detection-service/CLAUDE.md) — @JsonProperty obligatoire, Jackson
 * ne convertit pas snake_case → camelCase sans config globale (absente ici).
 * Type interne, jamais exposé tel quel via l'API.
 */
public record DetectionIA(
        String label,
        BboxIA bbox,
        Double confiance,
        Integer coupe,
        @JsonProperty("masque_base64") String masqueBase64) {
}
