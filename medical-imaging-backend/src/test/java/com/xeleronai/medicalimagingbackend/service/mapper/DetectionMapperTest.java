package com.xeleronai.medicalimagingbackend.service.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeleronai.medicalimagingbackend.dto.detection.DetectionResponse;
import com.xeleronai.medicalimagingbackend.entity.Detection;
import com.xeleronai.medicalimagingbackend.entity.Image;
import com.xeleronai.medicalimagingbackend.entity.enums.DetectionStatutEnum;
import com.xeleronai.medicalimagingbackend.entity.enums.DetectionTypeEnum;
import org.junit.jupiter.api.Test;

class DetectionMapperTest {

    private final DetectionMapper mapper = new DetectionMapperImpl();

    @Test
    void toResponse_detectionBox_mappeEnumsEnStringEtBbox() {
        Image image = Image.builder().id(7L).build();
        Detection detection = Detection.builder()
                .id(1L)
                .image(image)
                .type(DetectionTypeEnum.BOX)
                .anomalie("nodule")
                .confiance(0.87)
                .statut(DetectionStatutEnum.EN_ATTENTE)
                .coupe(12)
                .x(10).y(20).largeur(30).hauteur(40)
                .build();

        DetectionResponse response = mapper.toResponse(detection);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getImageId()).isEqualTo(7L);
        assertThat(response.getType()).isEqualTo("BOX");
        assertThat(response.getStatut()).isEqualTo("EN_ATTENTE");
        assertThat(response.getAnomalie()).isEqualTo("nodule");
        assertThat(response.getConfiance()).isEqualTo(0.87);
        assertThat(response.getBbox()).isNotNull();
        assertThat(response.getBbox().getX()).isEqualTo(10);
        assertThat(response.getBbox().getY()).isEqualTo(20);
        assertThat(response.getBbox().getLargeur()).isEqualTo(30);
        assertThat(response.getBbox().getHauteur()).isEqualTo(40);
        assertThat(response.isApercuMasqueDisponible()).isFalse();
    }

    @Test
    void toResponse_detectionMasque_bboxNullEtApercuMasqueDisponible() {
        Image image = Image.builder().id(7L).build();
        Detection detection = Detection.builder()
                .id(2L)
                .image(image)
                .type(DetectionTypeEnum.MASQUE)
                .statut(DetectionStatutEnum.EN_ATTENTE)
                .confiance(0.7)
                .cheminMasque("examens/x/masque.png")
                .build();

        DetectionResponse response = mapper.toResponse(detection);

        assertThat(response.getType()).isEqualTo("MASQUE");
        assertThat(response.getBbox()).isNull();
        assertThat(response.isApercuMasqueDisponible()).isTrue();
    }

    @Test
    void toResponse_detectionMasqueSansCheminMasque_apercuMasqueIndisponible() {
        Image image = Image.builder().id(7L).build();
        Detection detection = Detection.builder()
                .id(3L)
                .image(image)
                .type(DetectionTypeEnum.MASQUE)
                .statut(DetectionStatutEnum.EN_ATTENTE)
                .confiance(0.7)
                .cheminMasque(null)
                .build();

        DetectionResponse response = mapper.toResponse(detection);

        assertThat(response.isApercuMasqueDisponible()).isFalse();
    }
}
