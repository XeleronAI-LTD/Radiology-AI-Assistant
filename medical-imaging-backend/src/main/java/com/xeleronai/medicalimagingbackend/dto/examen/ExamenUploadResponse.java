package com.xeleronai.medicalimagingbackend.dto.examen;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamenUploadResponse {

    private Long examenId;
    private Long patientId;
    private String mrn;
    private Integer nombreImages;
    private List<ImageResponse> images;
}
