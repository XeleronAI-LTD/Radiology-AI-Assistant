package com.xeleronai.medicalimagingbackend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xeleronai.medicalimagingbackend.entity.AuditLog;
import com.xeleronai.medicalimagingbackend.entity.Examen;
import com.xeleronai.medicalimagingbackend.entity.Image;
import com.xeleronai.medicalimagingbackend.entity.Utilisateur;
import com.xeleronai.medicalimagingbackend.entity.enums.AnalyseStatutEnum;
import com.xeleronai.medicalimagingbackend.entity.enums.ExamenZoneEnum;
import com.xeleronai.medicalimagingbackend.entity.enums.RoleUtilisateur;
import com.xeleronai.medicalimagingbackend.repository.AuditLogRepository;
import com.xeleronai.medicalimagingbackend.repository.ExamenRepository;
import com.xeleronai.medicalimagingbackend.repository.ImageRepository;
import com.xeleronai.medicalimagingbackend.service.AnalyseEnCoursException;
import com.xeleronai.medicalimagingbackend.service.AnalyseNonLancableException;
import com.xeleronai.medicalimagingbackend.service.BboxIA;
import com.xeleronai.medicalimagingbackend.service.DetectionAiClient;
import com.xeleronai.medicalimagingbackend.service.DetectionAiIndisponibleException;
import com.xeleronai.medicalimagingbackend.service.DetectionIA;
import com.xeleronai.medicalimagingbackend.service.DetectionRunPersistenceService;
import com.xeleronai.medicalimagingbackend.service.LectureImpossibleException;
import com.xeleronai.medicalimagingbackend.service.MasqueDetecte;
import com.xeleronai.medicalimagingbackend.service.PredictionIA;
import com.xeleronai.medicalimagingbackend.service.RessourceIntrouvableException;
import com.xeleronai.medicalimagingbackend.service.StorageService;
import com.xeleronai.medicalimagingbackend.service.UploadEchoueException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DetectionAnalyseServiceImplTest {

    @Mock
    private ExamenRepository examenRepository;
    @Mock
    private ImageRepository imageRepository;
    @Mock
    private StorageService storageService;
    @Mock
    private DetectionAiClient detectionAiClient;
    @Mock
    private DetectionRunPersistenceService detectionRunPersistenceService;
    @Mock
    private AuditLogRepository auditLogRepository;

    private DetectionAnalyseServiceImpl service;

    private Utilisateur utilisateur;

    @BeforeEach
    void setUp() {
        service = new DetectionAnalyseServiceImpl(
                examenRepository,
                imageRepository,
                storageService,
                detectionAiClient,
                detectionRunPersistenceService,
                auditLogRepository);
        utilisateur = Utilisateur.builder().id(1L).email("radio@test.com").role(RoleUtilisateur.RADIOLOGUE).build();
    }

    private Examen examenValide(Long id) {
        return Examen.builder()
                .id(id)
                .modalite("CT")
                .zone(ExamenZoneEnum.THORAX)
                .statutAnalyse(AnalyseStatutEnum.EN_ATTENTE)
                .images(List.of(Image.builder().id(10L).ordre(0).build()))
                .build();
    }

    @Test
    void lancerAnalyse_examenValide_passeEnCoursEtSauvegarde() {
        Examen examen = examenValide(42L);
        when(examenRepository.findById(42L)).thenReturn(Optional.of(examen));

        service.lancerAnalyse(42L, utilisateur);

        ArgumentCaptor<Examen> captor = ArgumentCaptor.forClass(Examen.class);
        verify(examenRepository).save(captor.capture());
        assertThat(captor.getValue().getStatutAnalyse()).isEqualTo(AnalyseStatutEnum.EN_COURS);
    }

    @Test
    void lancerAnalyse_examenValide_ecritAuditAnalyseDemandee() {
        Examen examen = examenValide(42L);
        when(examenRepository.findById(42L)).thenReturn(Optional.of(examen));

        service.lancerAnalyse(42L, utilisateur);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo("ANALYSE_DEMANDEE");
        assertThat(captor.getValue().getResourceType()).isEqualTo("EXAMEN");
        assertThat(captor.getValue().getResourceId()).isEqualTo(42L);
        assertThat(captor.getValue().getUtilisateur()).isEqualTo(utilisateur);
    }

    @Test
    void lancerAnalyse_examenIntrouvable_leveRessourceIntrouvable() {
        when(examenRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.lancerAnalyse(99L, utilisateur))
                .isInstanceOf(RessourceIntrouvableException.class);
    }

    @Test
    void lancerAnalyse_modaliteManquante_leveAnalyseNonLancable() {
        Examen examen = examenValide(42L);
        examen.setModalite(null);
        when(examenRepository.findById(42L)).thenReturn(Optional.of(examen));

        assertThatThrownBy(() -> service.lancerAnalyse(42L, utilisateur))
                .isInstanceOf(AnalyseNonLancableException.class);
    }

    @Test
    void lancerAnalyse_zoneNulle_leveAnalyseNonLancable() {
        Examen examen = examenValide(42L);
        examen.setZone(null);
        when(examenRepository.findById(42L)).thenReturn(Optional.of(examen));

        assertThatThrownBy(() -> service.lancerAnalyse(42L, utilisateur))
                .isInstanceOf(AnalyseNonLancableException.class);
    }

    @Test
    void lancerAnalyse_zoneUnknown_leveAnalyseNonLancable() {
        Examen examen = examenValide(42L);
        examen.setZone(ExamenZoneEnum.UNKNOWN);
        when(examenRepository.findById(42L)).thenReturn(Optional.of(examen));

        assertThatThrownBy(() -> service.lancerAnalyse(42L, utilisateur))
                .isInstanceOf(AnalyseNonLancableException.class);
    }

    @Test
    void lancerAnalyse_aucuneImage_leveAnalyseNonLancable() {
        Examen examen = examenValide(42L);
        examen.setImages(List.of());
        when(examenRepository.findById(42L)).thenReturn(Optional.of(examen));

        assertThatThrownBy(() -> service.lancerAnalyse(42L, utilisateur))
                .isInstanceOf(AnalyseNonLancableException.class);
    }

    @Test
    void lancerAnalyse_dejaEnCours_leveAnalyseEnCours() {
        Examen examen = examenValide(42L);
        examen.setStatutAnalyse(AnalyseStatutEnum.EN_COURS);
        when(examenRepository.findById(42L)).thenReturn(Optional.of(examen));

        assertThatThrownBy(() -> service.lancerAnalyse(42L, utilisateur))
                .isInstanceOf(AnalyseEnCoursException.class);
    }

    @Test
    void analyserEnArrierePlan_succes_appelleEnregistrerSucces() {
        Examen examen = examenValide(42L);
        Image image0 = Image.builder().id(10L).ordre(0).cheminOriginal("examens/a/0.dcm").build();
        Image image1 = Image.builder().id(11L).ordre(1).cheminOriginal("examens/a/1.dcm").build();
        List<Image> images = List.of(image0, image1);

        when(examenRepository.findById(42L)).thenReturn(Optional.of(examen));
        when(imageRepository.findByExamenIdOrderByOrdreAsc(42L)).thenReturn(images);
        when(storageService.lire("examens/a/0.dcm")).thenReturn(new byte[] {1});
        when(storageService.lire("examens/a/1.dcm")).thenReturn(new byte[] {2});

        PredictionIA prediction = new PredictionIA("BOX", List.of(
                new DetectionIA("nodule", new BboxIA(1.0, 2.0, 3.0, 4.0), 0.9, 0, null)));
        when(detectionAiClient.predict(any(), eq("CT"), eq("THORAX"))).thenReturn(prediction);

        service.analyserEnArrierePlan(42L, utilisateur.getId());

        ArgumentCaptor<List<byte[]>> fichiersCaptor = ArgumentCaptor.forClass(List.class);
        verify(detectionAiClient).predict(fichiersCaptor.capture(), eq("CT"), eq("THORAX"));
        assertThat(fichiersCaptor.getValue()).hasSize(2);
        assertThat(fichiersCaptor.getValue().get(0)).isEqualTo(new byte[] {1});
        assertThat(fichiersCaptor.getValue().get(1)).isEqualTo(new byte[] {2});

        verify(detectionRunPersistenceService)
                .enregistrerSucces(42L, images, prediction, utilisateur.getId());
        verify(detectionRunPersistenceService, never())
                .enregistrerEchec(any(), any(), any());
    }

    @Test
    void analyserEnArrierePlan_erreurAiClient_appelleEnregistrerEchec() {
        Examen examen = examenValide(42L);
        Image image0 = Image.builder().id(10L).ordre(0).cheminOriginal("examens/a/0.dcm").build();

        when(examenRepository.findById(42L)).thenReturn(Optional.of(examen));
        when(imageRepository.findByExamenIdOrderByOrdreAsc(42L)).thenReturn(List.of(image0));
        when(storageService.lire("examens/a/0.dcm")).thenReturn(new byte[] {1});
        when(detectionAiClient.predict(any(), eq("CT"), eq("THORAX")))
                .thenThrow(new DetectionAiIndisponibleException("panne", new RuntimeException()));

        service.analyserEnArrierePlan(42L, utilisateur.getId());

        verify(detectionRunPersistenceService, times(1))
                .enregistrerEchec(eq(42L), any(), eq(utilisateur.getId()));
        verify(detectionRunPersistenceService, never())
                .enregistrerSucces(any(), any(), any(), any());
    }

    @Test
    void analyserEnArrierePlan_masque_uploadeChaqueMasqueEtAppelleEnregistrerSuccesMasques() {
        Examen examen = examenValide(42L);
        Image image0 = Image.builder().id(10L).ordre(0).cheminOriginal("examens/a/0.dcm").build();
        Image image1 = Image.builder().id(11L).ordre(1).cheminOriginal("examens/a/1.dcm").build();
        List<Image> images = List.of(image0, image1);

        when(examenRepository.findById(42L)).thenReturn(Optional.of(examen));
        when(imageRepository.findByExamenIdOrderByOrdreAsc(42L)).thenReturn(images);
        when(storageService.lire("examens/a/0.dcm")).thenReturn(new byte[] {1});
        when(storageService.lire("examens/a/1.dcm")).thenReturn(new byte[] {2});

        String base64Masque0 = Base64.getEncoder().encodeToString(new byte[] {9, 9});
        String base64Masque1 = Base64.getEncoder().encodeToString(new byte[] {8, 8});
        PredictionIA prediction = new PredictionIA("MASQUE", List.of(
                new DetectionIA("rate", null, 0.8, 0, base64Masque0),
                new DetectionIA("rate", null, 0.7, 1, base64Masque1)));
        when(detectionAiClient.predict(any(), eq("CT"), eq("THORAX"))).thenReturn(prediction);

        service.analyserEnArrierePlan(42L, utilisateur.getId());

        ArgumentCaptor<String> cleCaptor = ArgumentCaptor.forClass(String.class);
        verify(storageService, times(2)).uploader(cleCaptor.capture(), any(byte[].class), eq("image/png"));
        List<String> cles = cleCaptor.getAllValues();
        assertThat(cles).hasSize(2);
        assertThat(cles.get(0)).startsWith("examens/42/masques/");
        assertThat(cles.get(1)).startsWith("examens/42/masques/");
        assertThat(cles.get(0)).isNotEqualTo(cles.get(1));

        ArgumentCaptor<List<MasqueDetecte>> masquesCaptor = ArgumentCaptor.forClass(List.class);
        verify(detectionRunPersistenceService)
                .enregistrerSuccesMasques(eq(42L), eq(images), masquesCaptor.capture(), eq(utilisateur.getId()));
        List<MasqueDetecte> masques = masquesCaptor.getValue();
        assertThat(masques).hasSize(2);
        assertThat(masques.get(0).coupe()).isEqualTo(0);
        assertThat(masques.get(0).label()).isEqualTo("rate");
        assertThat(masques.get(0).confiance()).isEqualTo(0.8);
        assertThat(masques.get(0).cheminMasque()).isEqualTo(cles.get(0));
        assertThat(masques.get(1).coupe()).isEqualTo(1);
        assertThat(masques.get(1).cheminMasque()).isEqualTo(cles.get(1));

        verify(detectionRunPersistenceService, never())
                .enregistrerSucces(any(), any(), any(), any());
        verify(detectionRunPersistenceService, never())
                .enregistrerEchec(any(), any(), any());
    }

    @Test
    void analyserEnArrierePlan_masque_echecUploadMinio_appelleEnregistrerEchec_pasDePersistancePartielle() {
        Examen examen = examenValide(42L);
        Image image0 = Image.builder().id(10L).ordre(0).cheminOriginal("examens/a/0.dcm").build();

        when(examenRepository.findById(42L)).thenReturn(Optional.of(examen));
        when(imageRepository.findByExamenIdOrderByOrdreAsc(42L)).thenReturn(List.of(image0));
        when(storageService.lire("examens/a/0.dcm")).thenReturn(new byte[] {1});

        String base64Masque = Base64.getEncoder().encodeToString(new byte[] {9, 9});
        PredictionIA prediction = new PredictionIA("MASQUE", List.of(
                new DetectionIA("rate", null, 0.8, 0, base64Masque)));
        when(detectionAiClient.predict(any(), eq("CT"), eq("THORAX"))).thenReturn(prediction);
        doThrow(new UploadEchoueException("échec stockage masque", new RuntimeException()))
                .when(storageService).uploader(any(), any(byte[].class), any());

        service.analyserEnArrierePlan(42L, utilisateur.getId());

        verify(detectionRunPersistenceService, times(1))
                .enregistrerEchec(eq(42L), any(), eq(utilisateur.getId()));
        verify(detectionRunPersistenceService, never())
                .enregistrerSuccesMasques(any(), any(), any(), any());
        verify(detectionRunPersistenceService, never())
                .enregistrerSucces(any(), any(), any(), any());
    }

    @Test
    void analyserEnArrierePlan_erreurLectureMinio_appelleEnregistrerEchec() {
        Examen examen = examenValide(42L);
        Image image0 = Image.builder().id(10L).ordre(0).cheminOriginal("examens/a/0.dcm").build();

        when(examenRepository.findById(42L)).thenReturn(Optional.of(examen));
        when(imageRepository.findByExamenIdOrderByOrdreAsc(42L)).thenReturn(List.of(image0));
        when(storageService.lire("examens/a/0.dcm"))
                .thenThrow(new LectureImpossibleException("minio down", new RuntimeException()));

        service.analyserEnArrierePlan(42L, utilisateur.getId());

        verify(detectionRunPersistenceService, times(1))
                .enregistrerEchec(eq(42L), any(), eq(utilisateur.getId()));
        verify(detectionRunPersistenceService, never())
                .enregistrerSucces(any(), any(), any(), any());
        verify(detectionAiClient, never()).predict(any(), any(), any());
    }
}
