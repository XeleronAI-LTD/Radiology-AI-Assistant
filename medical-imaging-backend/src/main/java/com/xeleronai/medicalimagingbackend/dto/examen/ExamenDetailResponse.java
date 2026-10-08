package com.xeleronai.medicalimagingbackend.dto.examen;

import com.xeleronai.medicalimagingbackend.dto.patient.PatientResponse;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamenDetailResponse {

    private Long examenId;
    private PatientResponse patient;
    private String type;
    private LocalDate dateExamen;
    private String modalite;
    private String zone;
    private String creePar;
    private List<ImageResponse> images;
}
