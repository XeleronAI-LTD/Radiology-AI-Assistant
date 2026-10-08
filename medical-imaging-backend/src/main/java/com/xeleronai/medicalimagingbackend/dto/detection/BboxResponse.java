package com.xeleronai.medicalimagingbackend.dto.detection;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BboxResponse {

    private Integer x;
    private Integer y;
    private Integer largeur;
    private Integer hauteur;
}
