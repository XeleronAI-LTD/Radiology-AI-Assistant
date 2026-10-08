package com.xeleronai.medicalimagingbackend.service.mapper;

import com.xeleronai.medicalimagingbackend.dto.detection.BboxResponse;
import com.xeleronai.medicalimagingbackend.dto.detection.DetectionResponse;
import com.xeleronai.medicalimagingbackend.entity.Detection;
import com.xeleronai.medicalimagingbackend.entity.enums.DetectionTypeEnum;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface DetectionMapper {

    @Mapping(target = "imageId", source = "image.id")
    @Mapping(target = "bbox", expression = "java(toBbox(detection))")
    @Mapping(target = "apercuMasqueDisponible", expression = "java(toApercuMasqueDisponible(detection))")
    @Mapping(target = "validateurEmail", expression = "java(toValidateurEmail(detection))")
    DetectionResponse toResponse(Detection detection);

    /**
     * bbox n'a de sens que pour une détection BOX ; pour une détection
     * MASQUE, x/y/largeur/hauteur ne sont jamais renseignés donc bbox reste
     * null.
     */
    default BboxResponse toBbox(Detection detection) {
        if (detection.getType() != DetectionTypeEnum.BOX) {
            return null;
        }
        return BboxResponse.builder()
                .x(detection.getX())
                .y(detection.getY())
                .largeur(detection.getLargeur())
                .hauteur(detection.getHauteur())
                .build();
    }

    /**
     * La clé MinIO du masque (chemin_masque) n'est jamais exposée via
     * l'API ; seule sa disponibilité l'est.
     */
    default boolean toApercuMasqueDisponible(Detection detection) {
        return detection.getType() == DetectionTypeEnum.MASQUE && detection.getCheminMasque() != null;
    }

    /**
     * Utilisateur ne porte pas de nom affichable : seul l'email identifie
     * le validateur côté API.
     */
    default String toValidateurEmail(Detection detection) {
        return detection.getValidateur() != null ? detection.getValidateur().getEmail() : null;
    }
}
