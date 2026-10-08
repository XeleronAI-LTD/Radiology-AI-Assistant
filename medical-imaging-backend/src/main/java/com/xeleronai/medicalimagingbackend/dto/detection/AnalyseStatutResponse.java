package com.xeleronai.medicalimagingbackend.dto.detection;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyseStatutResponse {

    private Long examenId;
    private String statut;
    private String message;
    private LocalDateTime finieLe;
}
