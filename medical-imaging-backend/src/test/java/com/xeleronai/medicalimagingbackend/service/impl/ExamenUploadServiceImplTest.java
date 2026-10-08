package com.xeleronai.medicalimagingbackend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.xeleronai.medicalimagingbackend.dto.examen.ExamenUploadResponse;
import com.xeleronai.medicalimagingbackend.dto.examen.UploadStandardRequest;
import com.xeleronai.medicalimagingbackend.entity.Examen;
import com.xeleronai.medicalimagingbackend.entity.Institution;
import com.xeleronai.medicalimagingbackend.entity.Patient;
import com.xeleronai.medicalimagingbackend.entity.Utilisateur;
import com.xeleronai.medicalimagingbackend.entity.enums.ExamenZoneEnum;
import com.xeleronai.medicalimagingbackend.entity.enums.FormatImageEnum;
import com.xeleronai.medicalimagingbackend.entity.enums.RoleUtilisateur;
import com.xeleronai.medicalimagingbackend.service.DicomMetadata;
import com.xeleronai.medicalimagingbackend.service.DicomMetadataService;
import com.xeleronai.medicalimagingbackend.service.DicomPreviewService;
import com.xeleronai.medicalimagingbackend.service.ExamenPersistenceService;
import com.xeleronai.medicalimagingbackend.service.FileTypeValidator;
import com.xeleronai.medicalimagingbackend.service.ImagePreparee;
import com.xeleronai.medicalimagingbackend.service.PatientLookupService;
import com.xeleronai.medicalimagingbackend.service.StorageService;
import com.xeleronai.medicalimagingbackend.service.UploadEchoueException;
import com.xeleronai.medicalimagingbackend.service.UploadValidationException;
import com.xeleronai.medicalimagingbackend.service.mapper.ExamenMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class ExamenUploadServiceImplTest {

    @Mock
    private PatientLookupService patientLookupService;
    @Mock
    private DicomMetadataService dicomMetadataService;
    @Mock
    private DicomPreviewService dicomPreviewService;
    @Mock
    private StorageService storageService;
    @Mock
    private ExamenPersistenceService examenPersistenceService;
    @Mock
    private ExamenMapper examenMapper;

    private final FileTypeValidator fileTypeValidator = new FileTypeValidator();

    private ExamenUploadServiceImpl service;

    private Patient patient;
    private Utilisateur utilisateur;
    private Examen examenSauvegarde;
    private ExamenUploadResponse reponseAttendue;

    @BeforeEach
    void setUp() {
        service = new ExamenUploadServiceImpl(
                fileTypeValidator,
                patientLookupService,
                dicomMetadataService,
                dicomPreviewService,
                storageService,
                examenPersistenceService,
                examenMapper);

        patient = Patient.builder().id(1L).mrn("MRN-1").build();
        utilisateur = Utilisateur.builder().id(1L).email("radio@test.com").role(RoleUtilisateur.RADIOLOGUE)
                .institution(Institution.builder().id(7L).build()).build();
        examenSauvegarde = Examen.builder().id(42L).patient(patient).build();
        reponseAttendue = ExamenUploadResponse.builder().examenId(42L).build();
    }

    @Test
    void uploadStandard_nominal_cheminApercu_egal_cheminOriginal() {
        MockMultipartFile f1 = new MockMultipartFile("files", "a.png", "image/png", new byte[] {1});
        MockMultipartFile f2 = new MockMultipartFile("files", "b.jpg", "image/jpeg", new byte[] {1});
        UploadStandardRequest request = UploadStandardRequest.builder()
                .files(List.of(f1, f2))
                .mrn("MRN-1")
                .nom("Dupont")
                .build();

        when(patientLookupService.trouverOuCreerPatient("MRN-1", "Dupont", null, null, utilisateur.getInstitution()))
                .thenReturn(patient);
        when(examenPersistenceService.persisterExamen(any(), any(), any(), any(), any(), any(), anyList()))
                .thenReturn(examenSauvegarde);
        when(examenMapper.toResponse(examenSauvegarde)).thenReturn(reponseAttendue);

        ExamenUploadResponse resultat = service.uploadStandard(request, utilisateur);

        assertThat(resultat).isEqualTo(reponseAttendue);

        ArgumentCaptor<List<ImagePreparee>> imagesCaptor = ArgumentCaptor.forClass(List.class);
        verify(examenPersistenceService).persisterExamen(
                eq(patient), eq(utilisateur), any(), eq(LocalDate.now()), any(), isNull(), imagesCaptor.capture());

        List<ImagePreparee> images = imagesCaptor.getValue();
        assertThat(images).hasSize(2);
        assertThat(images.get(0).cheminApercu()).isEqualTo(images.get(0).cheminOriginal());
        assertThat(images.get(0).format()).isEqualTo(FormatImageEnum.PNG);
        assertThat(images.get(1).format()).isEqualTo(FormatImageEnum.JPEG);
        assertThat(images.get(0).ordre()).isZero();
        assertThat(images.get(1).ordre()).isEqualTo(1);

        verify(storageService).uploader(eq(images.get(0).cheminOriginal()), eq(f1));
        verify(storageService).uploader(eq(images.get(1).cheminOriginal()), eq(f2));
    }

    @Test
    void uploadStandard_echec_stockage_declenche_compensation() {
        MockMultipartFile f1 = new MockMultipartFile("files", "a.png", "image/png", new byte[] {1});
        MockMultipartFile f2 = new MockMultipartFile("files", "b.png", "image/png", new byte[] {1});
        UploadStandardRequest request = UploadStandardRequest.builder()
                .files(List.of(f1, f2))
                .mrn("MRN-1")
                .nom("Dupont")
                .build();

        when(patientLookupService.trouverOuCreerPatient("MRN-1", "Dupont", null, null, utilisateur.getInstitution()))
                .thenReturn(patient);
        // Le premier fichier s'uploade correctement, le second échoue.
        doNothing().when(storageService).uploader(anyString(), eq(f1));
        doThrow(new UploadEchoueException("panne minio", new RuntimeException()))
                .when(storageService).uploader(anyString(), eq(f2));

        assertThatThrownBy(() -> service.uploadStandard(request, utilisateur))
                .isInstanceOf(UploadEchoueException.class);

        ArgumentCaptor<List<String>> clesCaptor = ArgumentCaptor.forClass(List.class);
        verify(storageService).supprimerSilencieux(clesCaptor.capture());
        assertThat(clesCaptor.getValue()).hasSize(1);

        verify(examenPersistenceService, never()).persisterExamen(any(), any(), any(), any(), any(), any(), anyList());
    }

    @Test
    void uploadDicom_nominal_avec_echec_apercu_sur_un_fichier() {
        MockMultipartFile f1 = new MockMultipartFile("files", "a.dcm", "application/dicom", new byte[] {1});
        MockMultipartFile f2 = new MockMultipartFile("files", "b.dcm", "application/dicom", new byte[] {1});

        DicomMetadata meta1 =
                new DicomMetadata("MRN-1", "Dupont", null, "M", null, "CT", ExamenZoneEnum.THORAX, "Thorax", "STUDY-UID-1");
        DicomMetadata meta2 =
                new DicomMetadata("MRN-1", "Dupont", null, "M", null, "CT", ExamenZoneEnum.THORAX, "Thorax", "STUDY-UID-1");
        when(dicomMetadataService.extraire(f1)).thenReturn(meta1);
        when(dicomMetadataService.extraire(f2)).thenReturn(meta2);
        when(patientLookupService.trouverOuCreerPatient("MRN-1", "Dupont", null, "M", utilisateur.getInstitution()))
                .thenReturn(patient);
        when(dicomPreviewService.genererApercuPng(f1)).thenReturn(Optional.of(new byte[] {9, 9}));
        when(dicomPreviewService.genererApercuPng(f2)).thenReturn(Optional.empty());
        when(examenPersistenceService.persisterExamen(any(), any(), any(), any(), any(), any(), anyList()))
                .thenReturn(examenSauvegarde);
        when(examenMapper.toResponse(examenSauvegarde)).thenReturn(reponseAttendue);

        ExamenUploadResponse resultat = service.uploadDicom(List.of(f1, f2), utilisateur);

        assertThat(resultat).isEqualTo(reponseAttendue);

        ArgumentCaptor<List<ImagePreparee>> imagesCaptor = ArgumentCaptor.forClass(List.class);
        verify(examenPersistenceService).persisterExamen(
                eq(patient), eq(utilisateur), any(), any(), eq("CT"), eq(ExamenZoneEnum.THORAX), imagesCaptor.capture());

        List<ImagePreparee> images = imagesCaptor.getValue();
        assertThat(images).hasSize(2);
        assertThat(images.get(0).format()).isEqualTo(FormatImageEnum.DICOM);
        assertThat(images.get(0).cheminApercu()).isNotNull();
        assertThat(images.get(1).cheminApercu()).isNull();
    }

    @Test
    void uploadDicom_incoherence_patientId_rejette_sans_toucher_au_stockage() {
        MockMultipartFile f1 = new MockMultipartFile("files", "a.dcm", "application/dicom", new byte[] {1});
        MockMultipartFile f2 = new MockMultipartFile("files", "b.dcm", "application/dicom", new byte[] {1});

        DicomMetadata meta1 =
                new DicomMetadata("MRN-1", "Dupont", null, "M", null, "CT", ExamenZoneEnum.THORAX, "Thorax", "STUDY-UID-1");
        DicomMetadata meta2 =
                new DicomMetadata("MRN-2", "Martin", null, "F", null, "CT", ExamenZoneEnum.THORAX, "Thorax", "STUDY-UID-1");
        when(dicomMetadataService.extraire(f1)).thenReturn(meta1);
        when(dicomMetadataService.extraire(f2)).thenReturn(meta2);

        assertThatThrownBy(() -> service.uploadDicom(List.of(f1, f2), utilisateur))
                .isInstanceOf(UploadValidationException.class);

        verifyNoInteractions(storageService);
        verifyNoInteractions(patientLookupService);
        verifyNoInteractions(examenPersistenceService);
    }
}
