package com.xeleronai.medicalimagingbackend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xeleronai.medicalimagingbackend.entity.AuditLog;
import com.xeleronai.medicalimagingbackend.entity.Detection;
import com.xeleronai.medicalimagingbackend.entity.Examen;
import com.xeleronai.medicalimagingbackend.entity.Image;
import com.xeleronai.medicalimagingbackend.entity.Utilisateur;
import com.xeleronai.medicalimagingbackend.entity.enums.AnalyseStatutEnum;
import com.xeleronai.medicalimagingbackend.entity.enums.DetectionStatutEnum;
import com.xeleronai.medicalimagingbackend.entity.enums.DetectionTypeEnum;
import com.xeleronai.medicalimagingbackend.repository.AuditLogRepository;
import com.xeleronai.medicalimagingbackend.repository.DetectionRepository;
import com.xeleronai.medicalimagingbackend.repository.ExamenRepository;
import com.xeleronai.medicalimagingbackend.repository.UtilisateurRepository;
import com.xeleronai.medicalimagingbackend.service.BboxIA;
import com.xeleronai.medicalimagingbackend.service.DetectionIA;
import com.xeleronai.medicalimagingbackend.service.MasqueDetecte;
import com.xeleronai.medicalimagingbackend.service.PredictionIA;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DetectionRunPersistenceServiceImplTest {

    @Mock
    private ExamenRepository examenRepository;
    @Mock
    private DetectionRepository detectionRepository;
    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private AuditLogRepository auditLogRepository;

    private DetectionRunPersistenceServiceImpl service;

    private Utilisateur utilisateur;

    @BeforeEach
    void setUp() {
        service = new DetectionRunPersistenceServiceImpl(
                examenRepository, detectionRepository, utilisateurRepository, auditLogRepository);
        utilisateur = Utilisateur.builder().id(1L).build();
        when(utilisateurRepository.getReferenceById(1L)).thenReturn(utilisateur);
    }

    private List<Image> imagesOrdonnees() {
        return List.of(
                Image.builder().id(100L).ordre(0).build(),
                Image.builder().id(101L).ordre(1).build(),
                Image.builder().id(102L).ordre(2).build());
    }

    @Test
    void enregistrerSucces_mappingCoupeVersImage_correspondanceOrdre() {
        Examen examen = Examen.builder().id(42L).build();
        when(examenRepository.findById(42L)).thenReturn(Optional.of(examen));

        PredictionIA prediction = new PredictionIA("BOX", List.of(
                new DetectionIA("nodule", new BboxIA(1.0, 2.0, 3.0, 4.0), 0.9, 1, null)));

        service.enregistrerSucces(42L, imagesOrdonnees(), prediction, 1L);

        ArgumentCaptor<List<Detection>> captor = ArgumentCaptor.forClass(List.class);
        verify(detectionRepository).saveAll(captor.capture());
        List<Detection> detections = captor.getValue();
        assertThat(detections).hasSize(1);
        assertThat(detections.get(0).getImage().getId()).isEqualTo(101L);
        assertThat(detections.get(0).getCoupe()).isEqualTo(1);
    }

    @Test
    void enregistrerSucces_coupeHorsPlage_ignoreeAvecWarningSansExplosion() {
        Examen examen = Examen.builder().id(42L).build();
        when(examenRepository.findById(42L)).thenReturn(Optional.of(examen));

        PredictionIA prediction = new PredictionIA("BOX", List.of(
                new DetectionIA("nodule", new BboxIA(1.0, 2.0, 3.0, 4.0), 0.9, 99, null),
                new DetectionIA("nodule", new BboxIA(1.0, 2.0, 3.0, 4.0), 0.8, 0, null)));

        service.enregistrerSucces(42L, imagesOrdonnees(), prediction, 1L);

        ArgumentCaptor<List<Detection>> captor = ArgumentCaptor.forClass(List.class);
        verify(detectionRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getCoupe()).isEqualTo(0);
    }

    @Test
    void enregistrerSucces_conversionBbox_arrondiCorrect() {
        Examen examen = Examen.builder().id(42L).build();
        when(examenRepository.findById(42L)).thenReturn(Optional.of(examen));

        PredictionIA prediction = new PredictionIA("BOX", List.of(
                new DetectionIA("nodule", new BboxIA(10.6, 20.4, 5.5, 3.5), 0.9, 0, null)));

        service.enregistrerSucces(42L, imagesOrdonnees(), prediction, 1L);

        ArgumentCaptor<List<Detection>> captor = ArgumentCaptor.forClass(List.class);
        verify(detectionRepository).saveAll(captor.capture());
        Detection detection = captor.getValue().get(0);
        assertThat(detection.getX()).isEqualTo(11);
        assertThat(detection.getY()).isEqualTo(20);
        assertThat(detection.getLargeur()).isEqualTo(6);
        assertThat(detection.getHauteur()).isEqualTo(4);
    }

    @Test
    void enregistrerSucces_persisteDetectionsEtPasseTermine() {
        Examen examen = Examen.builder().id(42L).statutAnalyse(AnalyseStatutEnum.EN_COURS).build();
        when(examenRepository.findById(42L)).thenReturn(Optional.of(examen));

        PredictionIA prediction = new PredictionIA("BOX", List.of(
                new DetectionIA("nodule", new BboxIA(1.0, 2.0, 3.0, 4.0), 0.9, 0, null)));

        service.enregistrerSucces(42L, imagesOrdonnees(), prediction, 1L);

        ArgumentCaptor<Examen> examenCaptor = ArgumentCaptor.forClass(Examen.class);
        verify(examenRepository).save(examenCaptor.capture());
        Examen sauvegarde = examenCaptor.getValue();
        assertThat(sauvegarde.getStatutAnalyse()).isEqualTo(AnalyseStatutEnum.TERMINEE);
        assertThat(sauvegarde.getAnalyseFinieLe()).isNotNull();
        assertThat(sauvegarde.getAnalyseMessage()).isNull();

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getAction()).isEqualTo("DETECTION_RUN");
        assertThat(auditCaptor.getValue().getResourceType()).isEqualTo("EXAMEN");
        assertThat(auditCaptor.getValue().getResourceId()).isEqualTo(42L);
        assertThat(auditCaptor.getValue().getUtilisateur()).isEqualTo(utilisateur);
    }

    @Test
    void enregistrerSuccesMasques_persisteDetectionsTypeMasque_bboxNullCheminMasqueRenseigne() {
        Examen examen = Examen.builder().id(42L).build();
        when(examenRepository.findById(42L)).thenReturn(Optional.of(examen));

        List<MasqueDetecte> masques = List.of(
                new MasqueDetecte(0, "rate", 0.95, "examens/42/masques/uuid-1.png"),
                new MasqueDetecte(1, "rate", 0.90, "examens/42/masques/uuid-2.png"));

        service.enregistrerSuccesMasques(42L, imagesOrdonnees(), masques, 1L);

        ArgumentCaptor<List<Detection>> captor = ArgumentCaptor.forClass(List.class);
        verify(detectionRepository).saveAll(captor.capture());
        List<Detection> detections = captor.getValue();
        assertThat(detections).hasSize(2);
        for (Detection detection : detections) {
            assertThat(detection.getType()).isEqualTo(DetectionTypeEnum.MASQUE);
            assertThat(detection.getStatut()).isEqualTo(DetectionStatutEnum.EN_ATTENTE);
            assertThat(detection.getCheminMasque()).isNotNull();
            assertThat(detection.getX()).isNull();
            assertThat(detection.getY()).isNull();
            assertThat(detection.getLargeur()).isNull();
            assertThat(detection.getHauteur()).isNull();
        }
    }

    @Test
    void enregistrerSuccesMasques_coupeHorsPlage_ignoreeSansExplosion() {
        Examen examen = Examen.builder().id(42L).build();
        when(examenRepository.findById(42L)).thenReturn(Optional.of(examen));

        List<MasqueDetecte> masques = List.of(
                new MasqueDetecte(99, "rate", 0.95, "examens/42/masques/uuid-1.png"),
                new MasqueDetecte(0, "rate", 0.90, "examens/42/masques/uuid-2.png"));

        service.enregistrerSuccesMasques(42L, imagesOrdonnees(), masques, 1L);

        ArgumentCaptor<List<Detection>> captor = ArgumentCaptor.forClass(List.class);
        verify(detectionRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getCoupe()).isEqualTo(0);
    }

    @Test
    void enregistrerSuccesMasques_passeExamenTermineEtEcritAuditDetectionRun() {
        Examen examen = Examen.builder().id(42L).statutAnalyse(AnalyseStatutEnum.EN_COURS).build();
        when(examenRepository.findById(42L)).thenReturn(Optional.of(examen));

        List<MasqueDetecte> masques = List.of(new MasqueDetecte(0, "rate", 0.95, "examens/42/masques/uuid-1.png"));

        service.enregistrerSuccesMasques(42L, imagesOrdonnees(), masques, 1L);

        ArgumentCaptor<Examen> examenCaptor = ArgumentCaptor.forClass(Examen.class);
        verify(examenRepository).save(examenCaptor.capture());
        Examen sauvegarde = examenCaptor.getValue();
        assertThat(sauvegarde.getStatutAnalyse()).isEqualTo(AnalyseStatutEnum.TERMINEE);
        assertThat(sauvegarde.getAnalyseFinieLe()).isNotNull();
        assertThat(sauvegarde.getAnalyseMessage()).isNull();

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getAction()).isEqualTo("DETECTION_RUN");
        assertThat(auditCaptor.getValue().getResourceType()).isEqualTo("EXAMEN");
        assertThat(auditCaptor.getValue().getResourceId()).isEqualTo(42L);
    }

    @Test
    void enregistrerEchec_statutEchoueeEtMessage_zeroDetectionCreee() {
        Examen examen = Examen.builder().id(42L).statutAnalyse(AnalyseStatutEnum.EN_COURS).build();
        when(examenRepository.findById(42L)).thenReturn(Optional.of(examen));

        service.enregistrerEchec(42L, "Service de détection IA injoignable ou en erreur.", 1L);

        verify(detectionRepository, never()).saveAll(any());
        verify(detectionRepository, never()).save(any());

        ArgumentCaptor<Examen> examenCaptor = ArgumentCaptor.forClass(Examen.class);
        verify(examenRepository).save(examenCaptor.capture());
        Examen sauvegarde = examenCaptor.getValue();
        assertThat(sauvegarde.getStatutAnalyse()).isEqualTo(AnalyseStatutEnum.ECHOUEE);
        assertThat(sauvegarde.getAnalyseMessage()).isEqualTo("Service de détection IA injoignable ou en erreur.");
        assertThat(sauvegarde.getAnalyseFinieLe()).isNotNull();

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getAction()).isEqualTo("DETECTION_RUN");
    }
}
