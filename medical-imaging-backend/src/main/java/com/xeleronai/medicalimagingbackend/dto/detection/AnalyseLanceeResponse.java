package com.xeleronai.medicalimagingbackend.dto.detection;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyseLanceeResponse {

    private Long examenId;
    private String statut;
}
