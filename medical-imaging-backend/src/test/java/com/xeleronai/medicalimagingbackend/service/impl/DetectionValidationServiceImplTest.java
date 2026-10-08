package com.xeleronai.medicalimagingbackend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xeleronai.medicalimagingbackend.dto.detection.DetectionResponse;
import com.xeleronai.medicalimagingbackend.entity.AuditLog;
import com.xeleronai.medicalimagingbackend.entity.Detection;
import com.xeleronai.medicalimagingbackend.entity.Utilisateur;
import com.xeleronai.medicalimagingbackend.entity.enums.DetectionStatutEnum;
import com.xeleronai.medicalimagingbackend.entity.enums.DetectionTypeEnum;
import com.xeleronai.medicalimagingbackend.entity.enums.RoleUtilisateur;
import com.xeleronai.medicalimagingbackend.repository.AuditLogRepository;
import com.xeleronai.medicalimagingbackend.repository.DetectionRepository;
import com.xeleronai.medicalimagingbackend.service.RessourceIntrouvableException;
import com.xeleronai.medicalimagingbackend.service.StatutDetectionInvalideException;
import com.xeleronai.medicalimagingbackend.service.mapper.DetectionMapper;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DetectionValidationServiceImplTest {

    @Mock
    private DetectionRepository detectionRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private DetectionMapper detectionMapper;

    private DetectionValidationServiceImpl service;

    private Utilisateur radiologue;

    @BeforeEach
    void setUp() {
        service = new DetectionValidationServiceImpl(detectionRepository, auditLogRepository, detectionMapper);
        radiologue = Utilisateur.builder().id(1L).email("radio@test.com").role(RoleUtilisateur.RADIOLOGUE).build();
    }

    private Detection detectionEnAttente(Long id) {
        return Detection.builder()
                .id(id)
                .type(DetectionTypeEnum.BOX)
                .statut(DetectionStatutEnum.EN_ATTENTE)
                .build();
    }

    @Test
    void validerStatut_detectionExistante_metAJourStatutValidateurEtDate() {
        Detection detection = detectionEnAttente(8L);
        when(detectionRepository.findByIdAndExamenId(8L, 42L)).thenReturn(Optional.of(detection));
        when(detectionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(detectionMapper.toResponse(any())).thenReturn(DetectionResponse.builder().id(8L).build());

        service.validerStatut(42L, 8L, "ACCEPTEE", radiologue);

        ArgumentCaptor<Detection> captor = ArgumentCaptor.forClass(Detection.class);
        verify(detectionRepository).save(captor.capture());
        Detection sauvegarde = captor.getValue();
        assertThat(sauvegarde.getStatut()).isEqualTo(DetectionStatutEnum.ACCEPTEE);
        assertThat(sauvegarde.getValidateur()).isEqualTo(radiologue);
        assertThat(sauvegarde.getValideLe()).isNotNull();
    }

    @Test
    void validerStatut_changementAvis_accepteeVersRejetee_autorise() {
        Detection detection = Detection.builder()
                .id(8L)
                .type(DetectionTypeEnum.BOX)
                .statut(DetectionStatutEnum.ACCEPTEE)
                .build();
        when(detectionRepository.findByIdAndExamenId(8L, 42L)).thenReturn(Optional.of(detection));
        when(detectionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(detectionMapper.toResponse(any())).thenReturn(DetectionResponse.builder().id(8L).build());

        service.validerStatut(42L, 8L, "REJETEE", radiologue);

        ArgumentCaptor<Detection> captor = ArgumentCaptor.forClass(Detection.class);
        verify(detectionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatut()).isEqualTo(DetectionStatutEnum.REJETEE);
    }

    @Test
    void validerStatut_statutInvalide_leveStatutDetectionInvalideException() {
        assertThatThrownBy(() -> service.validerStatut(42L, 8L, "EN_ATTENTE", radiologue))
                .isInstanceOf(StatutDetectionInvalideException.class);

        verify(detectionRepository, never()).findByIdAndExamenId(any(), any());
        verify(detectionRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void validerStatut_statutInconnu_leveStatutDetectionInvalideException() {
        assertThatThrownBy(() -> service.validerStatut(42L, 8L, "INCONNU", radiologue))
                .isInstanceOf(StatutDetectionInvalideException.class);
    }

    @Test
    void validerStatut_detectionHorsExamen_leveRessourceIntrouvableException() {
        when(detectionRepository.findByIdAndExamenId(8L, 42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validerStatut(42L, 8L, "ACCEPTEE", radiologue))
                .isInstanceOf(RessourceIntrouvableException.class);

        verify(detectionRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void validerStatut_ecritAuditDetectionValidationAvecTransitionEncodee() {
        Detection detection = detectionEnAttente(8L);
        when(detectionRepository.findByIdAndExamenId(8L, 42L)).thenReturn(Optional.of(detection));
        when(detectionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(detectionMapper.toResponse(any())).thenReturn(DetectionResponse.builder().id(8L).build());

        service.validerStatut(42L, 8L, "ACCEPTEE", radiologue);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo("DETECTION_VALIDATION:EN_ATTENTE->ACCEPTEE");
        assertThat(captor.getValue().getResourceType()).isEqualTo("DETECTION");
        assertThat(captor.getValue().getResourceId()).isEqualTo(8L);
        assertThat(captor.getValue().getUtilisateur()).isEqualTo(radiologue);
    }
}
