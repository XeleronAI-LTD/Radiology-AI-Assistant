package com.xeleronai.medicalimagingbackend.dto.examen;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageResponse {

    private Long imageId;
    private String format;
    private Boolean apercuDisponible;
    private Integer ordre;
}
