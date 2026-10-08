package com.xeleronai.medicalimagingbackend.dto.examen;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamenSummaryResponse {

    private Long examenId;
    private String mrn;
    private String patientNom;
    private String type;
    private LocalDate dateExamen;
    private String modalite;
    private String zone;
    private Integer nombreImages;
}
