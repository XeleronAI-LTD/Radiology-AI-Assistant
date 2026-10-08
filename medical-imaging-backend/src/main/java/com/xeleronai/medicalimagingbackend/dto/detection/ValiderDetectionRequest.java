package com.xeleronai.medicalimagingbackend.dto.detection;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValiderDetectionRequest {

    @NotBlank
    private String statut;
}
