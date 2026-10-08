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
public class DetectionResponse {

    private Long id;
    private Long imageId;
    private String type;
    private String anomalie;
    private Double confiance;
    private String statut;
    private Integer coupe;
    private BboxResponse bbox;
    private boolean apercuMasqueDisponible;
    private String validateurEmail;
    private LocalDateTime valideLe;
}
