package com.xeleronai.medicalimagingbackend.service.impl;

import com.xeleronai.medicalimagingbackend.dto.examen.ExamenUploadResponse;
import com.xeleronai.medicalimagingbackend.dto.examen.UploadStandardRequest;
import com.xeleronai.medicalimagingbackend.entity.Examen;
import com.xeleronai.medicalimagingbackend.entity.Patient;
import com.xeleronai.medicalimagingbackend.entity.Utilisateur;
import com.xeleronai.medicalimagingbackend.entity.enums.FormatImageEnum;
import com.xeleronai.medicalimagingbackend.service.DicomMetadata;
import com.xeleronai.medicalimagingbackend.service.DicomMetadataService;
import com.xeleronai.medicalimagingbackend.service.DicomPreviewService;
import com.xeleronai.medicalimagingbackend.service.ExamenPersistenceService;
import com.xeleronai.medicalimagingbackend.service.ExamenUploadService;
import com.xeleronai.medicalimagingbackend.service.FileTypeValidator;
import com.xeleronai.medicalimagingbackend.service.ImagePreparee;
import com.xeleronai.medicalimagingbackend.service.PatientLookupService;
import com.xeleronai.medicalimagingbackend.service.StorageService;
import com.xeleronai.medicalimagingbackend.service.UploadEchoueException;
import com.xeleronai.medicalimagingbackend.service.UploadValidationException;
import com.xeleronai.medicalimagingbackend.service.mapper.ExamenMapper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Volontairement PAS @Transactional sur cette classe : elle orchestre du
 * stockage objet MinIO (non transactionnel) et une transaction base séparée
 * (ExamenPersistenceService) — les deux ne doivent pas partager une seule
 * transaction (voir PatientLookupService).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExamenUploadServiceImpl implements ExamenUploadService {

    private final FileTypeValidator fileTypeValidator;
    private final PatientLookupService patientLookupService;
    private final DicomMetadataService dicomMetadataService;
    private final DicomPreviewService dicomPreviewService;
    private final StorageService storageService;
    private final ExamenPersistenceService examenPersistenceService;
    private final ExamenMapper examenMapper;

    @Override
    public ExamenUploadResponse uploadStandard(UploadStandardRequest request, Utilisateur utilisateurCourant) {
        fileTypeValidator.validerBatchNonVide(request.getFiles());
        fileTypeValidator.validerExtensionsStandard(request.getFiles());

        Patient patient = patientLookupService.trouverOuCreerPatient(
                request.getMrn(), request.getNom(), request.getDateNaissance(), request.getSexe(),
                utilisateurCourant.getInstitution());

        UUID batchId = UUID.randomUUID();
        List<String> clesPoussees = new ArrayList<>();
        try {
            List<ImagePreparee> images = new ArrayList<>();
            int ordre = 0;
            for (MultipartFile fichier : request.getFiles()) {
                String extension = FileTypeValidator.extensionDe(fichier);
                String cle = "examens/" + batchId + "/" + UUID.randomUUID() + "." + extension;
                storageService.uploader(cle, fichier);
                clesPoussees.add(cle);

                FormatImageEnum format = "png".equalsIgnoreCase(extension) ? FormatImageEnum.PNG : FormatImageEnum.JPEG;
                images.add(new ImagePreparee(cle, cle, format, ordre++));
            }

            Examen examen = examenPersistenceService.persisterExamen(
                    patient,
                    utilisateurCourant,
                    request.getType(),
                    request.getDateExamen() != null ? request.getDateExamen() : LocalDate.now(),
                    request.getModalite(),
                    null,
                    images);

            return examenMapper.toResponse(examen);
        } catch (Exception e) {
            storageService.supprimerSilencieux(clesPoussees);
            if (e instanceof UploadValidationException uve) {
                throw uve;
            }
            throw new UploadEchoueException("Échec de l'upload de l'examen", e);
        }
    }

    @Override
    public ExamenUploadResponse uploadDicom(List<MultipartFile> files, Utilisateur utilisateurCourant) {
        fileTypeValidator.validerBatchNonVide(files);
        fileTypeValidator.validerExtensionsDicom(files);

        List<DicomMetadata> metadonnees = files.stream().map(dicomMetadataService::extraire).toList();

        DicomMetadata reference = metadonnees.get(0);
        for (DicomMetadata m : metadonnees) {
            if (!Objects.equals(m.mrn(), reference.mrn())
                    || !Objects.equals(m.studyInstanceUid(), reference.studyInstanceUid())) {
                throw new UploadValidationException(
                        "Incohérence PatientID/StudyInstanceUID entre fichiers du batch");
            }
        }

        Patient patient = patientLookupService.trouverOuCreerPatient(
                reference.mrn(), reference.nom(), reference.dateNaissance(), reference.sexe(),
                utilisateurCourant.getInstitution());

        UUID batchId = UUID.randomUUID();
        List<String> clesPoussees = new ArrayList<>();
        try {
            List<ImagePreparee> images = new ArrayList<>();
            int ordre = 0;
            for (MultipartFile fichier : files) {
                String cleOriginal = "examens/" + batchId + "/" + UUID.randomUUID() + ".dcm";
                storageService.uploader(cleOriginal, fichier);
                clesPoussees.add(cleOriginal);

                String cleApercu = null;
                Optional<byte[]> apercu = dicomPreviewService.genererApercuPng(fichier);
                if (apercu.isPresent()) {
                    cleApercu = "examens/" + batchId + "/" + UUID.randomUUID() + "_apercu.png";
                    storageService.uploader(cleApercu, apercu.get(), "image/png");
                    clesPoussees.add(cleApercu);
                }

                images.add(new ImagePreparee(cleOriginal, cleApercu, FormatImageEnum.DICOM, ordre++));
            }

            Examen examen = examenPersistenceService.persisterExamen(
                    patient,
                    utilisateurCourant,
                    reference.type(),
                    reference.dateExamen() != null ? reference.dateExamen() : LocalDate.now(),
                    reference.modalite(),
                    reference.zone(),
                    images);

            return examenMapper.toResponse(examen);
        } catch (Exception e) {
            storageService.supprimerSilencieux(clesPoussees);
            if (e instanceof UploadValidationException uve) {
                throw uve;
            }
            throw new UploadEchoueException("Échec de l'upload de l'examen DICOM", e);
        }
    }
}
