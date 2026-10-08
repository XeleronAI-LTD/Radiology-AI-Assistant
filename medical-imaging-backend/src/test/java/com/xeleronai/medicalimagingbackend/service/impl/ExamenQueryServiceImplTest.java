package com.xeleronai.medicalimagingbackend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xeleronai.medicalimagingbackend.dto.common.PageResponse;
import com.xeleronai.medicalimagingbackend.dto.examen.ExamenSummaryResponse;
import com.xeleronai.medicalimagingbackend.entity.Examen;
import com.xeleronai.medicalimagingbackend.entity.Image;
import com.xeleronai.medicalimagingbackend.entity.Institution;
import com.xeleronai.medicalimagingbackend.entity.Utilisateur;
import com.xeleronai.medicalimagingbackend.entity.enums.RoleUtilisateur;
import com.xeleronai.medicalimagingbackend.repository.AuditLogRepository;
import com.xeleronai.medicalimagingbackend.repository.ExamenRepository;
import com.xeleronai.medicalimagingbackend.repository.ImageRepository;
import com.xeleronai.medicalimagingbackend.service.LectureImpossibleException;
import com.xeleronai.medicalimagingbackend.service.RessourceIntrouvableException;
import com.xeleronai.medicalimagingbackend.service.StorageService;
import com.xeleronai.medicalimagingbackend.service.mapper.ExamenMapper;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ExamenQueryServiceImplTest {

    @Mock
    private ExamenRepository examenRepository;
    @Mock
    private ImageRepository imageRepository;
    @Mock
    private StorageService storageService;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private ExamenMapper examenMapper;

    private ExamenQueryServiceImpl service;

    private Utilisateur utilisateur;
    private Examen examen;
    private Image image;

    @BeforeEach
    void setUp() {
        service = new ExamenQueryServiceImpl(
                examenRepository, imageRepository, storageService, auditLogRepository, examenMapper);

        utilisateur = Utilisateur.builder().id(1L).email("radio@test.com").role(RoleUtilisateur.RADIOLOGUE)
                .institution(Institution.builder().id(7L).build()).build();
        examen = Examen.builder().id(42L).build();
        image = Image.builder().id(101L).examen(examen).cheminApercu("examens/42/x_apercu.png").build();
    }

    @Test
    void detail_examenInexistant_leveRessourceIntrouvable() {
        when(examenRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.detail(99L)).isInstanceOf(RessourceIntrouvableException.class);
    }

    @Test
    void lireApercu_imageInexistanteOuAutreExamen_leveRessourceIntrouvable_storageJamaisAppele() {
        when(imageRepository.findByIdAndExamenId(101L, 42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.lireApercu(42L, 101L, utilisateur))
                .isInstanceOf(RessourceIntrouvableException.class);

        verify(storageService, never()).lire(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void lireApercu_apercuAbsent_leveRessourceIntrouvable_storageJamaisAppele() {
        Image imageSansApercu = Image.builder().id(102L).examen(examen).cheminApercu(null).build();
        when(imageRepository.findByIdAndExamenId(102L, 42L)).thenReturn(Optional.of(imageSansApercu));

        assertThatThrownBy(() -> service.lireApercu(42L, 102L, utilisateur))
                .isInstanceOf(RessourceIntrouvableException.class);

        verify(storageService, never()).lire(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void lireApercu_succes_ecritUneLigneAuditApresLecture() {
        when(imageRepository.findByIdAndExamenId(101L, 42L)).thenReturn(Optional.of(image));
        when(storageService.lire("examens/42/x_apercu.png")).thenReturn(new byte[] {1, 2, 3});

        byte[] resultat = service.lireApercu(42L, 101L, utilisateur);

        assertThat(resultat).containsExactly(1, 2, 3);

        var captor = ArgumentCaptor.forClass(com.xeleronai.medicalimagingbackend.entity.AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo("VIEW_IMAGE");
        assertThat(captor.getValue().getResourceType()).isEqualTo("IMAGE");
        assertThat(captor.getValue().getResourceId()).isEqualTo(101L);
        assertThat(captor.getValue().getUtilisateur()).isEqualTo(utilisateur);
    }

    @Test
    void lireApercu_echecLectureMinio_neCriveAucuneLigneAudit() {
        when(imageRepository.findByIdAndExamenId(101L, 42L)).thenReturn(Optional.of(image));
        when(storageService.lire("examens/42/x_apercu.png"))
                .thenThrow(new LectureImpossibleException("panne minio", new RuntimeException()));

        assertThatThrownBy(() -> service.lireApercu(42L, 101L, utilisateur))
                .isInstanceOf(LectureImpossibleException.class);

        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void lister_sansMrn_appelleFindAllAvecPatient() {
        Page<Examen> page = new PageImpl<>(java.util.List.of(examen), PageRequest.of(0, 20), 1);
        when(examenRepository.findAllAvecPatient(eq(7L), any())).thenReturn(page);
        when(examenMapper.toSummaryResponse(examen)).thenReturn(ExamenSummaryResponse.builder().examenId(42L).build());

        PageResponse<ExamenSummaryResponse> resultat = service.lister(null, 0, 20, utilisateur);

        assertThat(resultat.getContent()).hasSize(1);
        assertThat(resultat.getTotalElements()).isEqualTo(1);
        verify(examenRepository, never()).findByPatientMrnAvecPatient(any(), any(), any());
    }

    @Test
    void lister_avecMrn_appelleFindByPatientMrnAvecPatient() {
        Page<Examen> page = new PageImpl<>(java.util.List.of(examen), PageRequest.of(0, 20), 1);
        when(examenRepository.findByPatientMrnAvecPatient(eq("MRN-1"), eq(7L), any())).thenReturn(page);
        when(examenMapper.toSummaryResponse(examen)).thenReturn(ExamenSummaryResponse.builder().examenId(42L).build());

        service.lister("MRN-1", 0, 20, utilisateur);

        verify(examenRepository).findByPatientMrnAvecPatient(eq("MRN-1"), eq(7L), any());
        verify(examenRepository, never()).findAllAvecPatient(any(), any());
    }
}
