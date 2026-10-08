package com.xeleronai.medicalimagingbackend.dto.examen;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadStandardRequest {

    @NotEmpty
    private List<MultipartFile> files;

    @NotBlank
    private String mrn;

    @NotBlank
    private String nom;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateNaissance;

    private String sexe;

    private String type;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateExamen;

    private String modalite;
}
