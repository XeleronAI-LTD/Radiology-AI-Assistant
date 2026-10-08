package com.xeleronai.medicalimagingbackend.service.mapper;

import com.xeleronai.medicalimagingbackend.dto.examen.ExamenDetailResponse;
import com.xeleronai.medicalimagingbackend.dto.examen.ExamenSummaryResponse;
import com.xeleronai.medicalimagingbackend.dto.examen.ExamenUploadResponse;
import com.xeleronai.medicalimagingbackend.dto.examen.ImageResponse;
import com.xeleronai.medicalimagingbackend.dto.patient.PatientResponse;
import com.xeleronai.medicalimagingbackend.entity.Examen;
import com.xeleronai.medicalimagingbackend.entity.Image;
import com.xeleronai.medicalimagingbackend.entity.Patient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ExamenMapper {

    @Mapping(target = "examenId", source = "id")
    @Mapping(target = "patientId", source = "patient.id")
    @Mapping(target = "mrn", source = "patient.mrn")
    @Mapping(target = "nombreImages", expression = "java(examen.getImages().size())")
    @Mapping(target = "images", source = "images")
    ExamenUploadResponse toResponse(Examen examen);

    @Mapping(target = "imageId", source = "id")
    @Mapping(target = "apercuDisponible", expression = "java(image.getCheminApercu() != null)")
    ImageResponse toImageResponse(Image image);

    @Mapping(target = "examenId", source = "id")
    @Mapping(target = "mrn", source = "patient.mrn")
    @Mapping(target = "patientNom", source = "patient.nom")
    @Mapping(target = "nombreImages", expression = "java(examen.getImages().size())")
    ExamenSummaryResponse toSummaryResponse(Examen examen);

    @Mapping(target = "examenId", source = "id")
    @Mapping(target = "patient", source = "patient")
    @Mapping(target = "creePar", source = "creePar.email")
    @Mapping(target = "images", source = "images")
    ExamenDetailResponse toDetailResponse(Examen examen);

    @Mapping(target = "patientId", source = "id")
    PatientResponse toPatientResponse(Patient patient);
}
