package com.xeleronai.medicalimagingbackend.dto.patient;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientResponse {

    private Long patientId;
    private String mrn;
    private String nom;
    private LocalDate dateNaissance;
    private String sexe;
}
