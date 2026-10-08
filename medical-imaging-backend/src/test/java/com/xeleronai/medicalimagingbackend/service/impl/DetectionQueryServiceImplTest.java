package com.xeleronai.medicalimagingbackend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xeleronai.medicalimagingbackend.entity.AuditLog;
import com.xeleronai.medicalimagingbackend.entity.Detection;
import com.xeleronai.medicalimagingbackend.entity.Image;
import com.xeleronai.medicalimagingbackend.entity.Utilisateur;
import com.xeleronai.medicalimagingbackend.entity.enums.DetectionStatutEnum;
import com.xeleronai.medicalimagingbackend.entity.enums.DetectionTypeEnum;
import com.xeleronai.medicalimagingbackend.entity.enums.RoleUtilisateur;
import com.xeleronai.medicalimagingbackend.repository.AuditLogRepository;
import com.xeleronai.medicalimagingbackend.repository.DetectionRepository;
import com.xeleronai.medicalimagingbackend.repository.ExamenRepository;
import com.xeleronai.medicalimagingbackend.service.LectureImpossibleException;
import com.xeleronai.medicalimagingbackend.service.RessourceIntrouvableException;
import com.xeleronai.medicalimagingbackend.service.StorageService;
import com.xeleronai.medicalimagingbackend.service.mapper.DetectionMapper;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DetectionQueryServiceImplTest {

    @Mock
    private ExamenRepository examenRepository;
    @Mock
    private DetectionRepository detectionRepository;
    @Mock
    private DetectionMapper detectionMapper;
    @Mock
    private StorageService storageService;
    @Mock
    private AuditLogRepository auditLogRepository;

    private DetectionQueryServiceImpl service;

    private Utilisateur utilisateur;

    @BeforeEach
    void setUp() {
        service = new DetectionQueryServiceImpl(
                examenRepository, detectionRepository, detectionMapper, storageService, auditLogRepository);
        utilisateur = Utilisateur.builder().id(1L).email("radio@test.com").role(RoleUtilisateur.RADIOLOGUE).build();
    }

    private Detection detectionMasque(Long id, String cheminMasque) {
        return Detection.builder()
                .id(id)
                .type(DetectionTypeEnum.MASQUE)
                .statut(DetectionStatutEnum.EN_ATTENTE)
                .cheminMasque(cheminMasque)
                .image(Image.builder().id(101L).build())
                .build();
    }

    @Test
    void lireMasque_detectionInexistanteOuAutreExamen_leveRessourceIntrouvable_storageJamaisAppele() {
        when(detectionRepository.findByIdAndExamenId(5L, 42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.lireMasque(42L, 5L, utilisateur))
                .isInstanceOf(RessourceIntrouvableException.class);

        verify(storageService, never()).lire(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void lireMasque_typeBox_leveRessourceIntrouvable_storageJamaisAppele() {
        Detection detectionBox = Detection.builder()
                .id(6L)
                .type(DetectionTypeEnum.BOX)
                .image(Image.builder().id(101L).build())
                .build();
        when(detectionRepository.findByIdAndExamenId(6L, 42L)).thenReturn(Optional.of(detectionBox));

        assertThatThrownBy(() -> service.lireMasque(42L, 6L, utilisateur))
                .isInstanceOf(RessourceIntrouvableException.class);

        verify(storageService, never()).lire(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void lireMasque_cheminMasqueNull_leveRessourceIntrouvable_storageJamaisAppele() {
        Detection detectionSansMasque = detectionMasque(7L, null);
        when(detectionRepository.findByIdAndExamenId(7L, 42L)).thenReturn(Optional.of(detectionSansMasque));

        assertThatThrownBy(() -> service.lireMasque(42L, 7L, utilisateur))
                .isInstanceOf(RessourceIntrouvableException.class);

        verify(storageService, never()).lire(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void lireMasque_succes_ecritUneLigneAuditApresLecture() {
        Detection detection = detectionMasque(8L, "examens/42/masques/uuid-1.png");
        when(detectionRepository.findByIdAndExamenId(8L, 42L)).thenReturn(Optional.of(detection));
        when(storageService.lire("examens/42/masques/uuid-1.png")).thenReturn(new byte[] {1, 2, 3});

        byte[] resultat = service.lireMasque(42L, 8L, utilisateur);

        assertThat(resultat).containsExactly(1, 2, 3);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo("VIEW_MASQUE");
        assertThat(captor.getValue().getResourceType()).isEqualTo("DETECTION");
        assertThat(captor.getValue().getResourceId()).isEqualTo(8L);
        assertThat(captor.getValue().getUtilisateur()).isEqualTo(utilisateur);
    }

    @Test
    void lireMasque_echecLectureMinio_neCriveAucuneLigneAudit() {
        Detection detection = detectionMasque(9L, "examens/42/masques/uuid-1.png");
        when(detectionRepository.findByIdAndExamenId(9L, 42L)).thenReturn(Optional.of(detection));
        when(storageService.lire("examens/42/masques/uuid-1.png"))
                .thenThrow(new LectureImpossibleException("panne minio", new RuntimeException()));

        assertThatThrownBy(() -> service.lireMasque(42L, 9L, utilisateur))
                .isInstanceOf(LectureImpossibleException.class);

        verify(auditLogRepository, never()).save(any());
    }
}
