package com.xeleronai.medicalimagingbackend.service.impl;

import com.xeleronai.medicalimagingbackend.entity.enums.ExamenZoneEnum;
import com.xeleronai.medicalimagingbackend.service.DicomMetadata;
import com.xeleronai.medicalimagingbackend.service.DicomMetadataService;
import com.xeleronai.medicalimagingbackend.service.UploadValidationException;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class DicomMetadataServiceImpl implements DicomMetadataService {

    private static final DateTimeFormatter FORMAT_DICOM_DA = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * Table de correspondance tolérante zone → mots-clés. Recherche par
     * "contains" sur BodyPartExamined (terme contrôlé) ou sur
     * StudyDescription/SeriesDescription (texte libre, ex. "CT CHEST W/O
     * CONTRAST"). Ordre = priorité en cas de mots-clés concurrents.
     */
    private static final Map<String, ExamenZoneEnum> MOTS_CLES_ZONE = new LinkedHashMap<>();

    static {
        MOTS_CLES_ZONE.put("THORAX", ExamenZoneEnum.THORAX);
        MOTS_CLES_ZONE.put("THORACIC", ExamenZoneEnum.THORAX);
        MOTS_CLES_ZONE.put("CHEST", ExamenZoneEnum.THORAX);
        MOTS_CLES_ZONE.put("LUNG", ExamenZoneEnum.THORAX);
        MOTS_CLES_ZONE.put("ABDOMEN", ExamenZoneEnum.ABDOMEN);
        MOTS_CLES_ZONE.put("ABDO", ExamenZoneEnum.ABDOMEN);
        MOTS_CLES_ZONE.put("CRANE", ExamenZoneEnum.CRANE);
        MOTS_CLES_ZONE.put("HEAD", ExamenZoneEnum.CRANE);
        MOTS_CLES_ZONE.put("BRAIN", ExamenZoneEnum.CRANE);
        MOTS_CLES_ZONE.put("SKULL", ExamenZoneEnum.CRANE);
        MOTS_CLES_ZONE.put("PELVIS", ExamenZoneEnum.PELVIS);
        MOTS_CLES_ZONE.put("PELVIC", ExamenZoneEnum.PELVIS);
        MOTS_CLES_ZONE.put("SEIN", ExamenZoneEnum.SEIN);
        MOTS_CLES_ZONE.put("BREAST", ExamenZoneEnum.SEIN);
        MOTS_CLES_ZONE.put("MAMMO", ExamenZoneEnum.SEIN);
        MOTS_CLES_ZONE.put("HEART", ExamenZoneEnum.COEUR);
        MOTS_CLES_ZONE.put("CARDIAC", ExamenZoneEnum.COEUR);
        MOTS_CLES_ZONE.put("ECHOCARDIOGRAM", ExamenZoneEnum.COEUR);
    }

    @Override
    public DicomMetadata extraire(MultipartFile fichierDicom) {
        Attributes attrs;
        try (DicomInputStream dis = new DicomInputStream(fichierDicom.getInputStream())) {
            attrs = dis.readDataset();
        } catch (IOException e) {
            log.warn("Fichier DICOM illisible : {}", fichierDicom.getOriginalFilename());
            throw new UploadValidationException("Fichier DICOM illisible : " + fichierDicom.getOriginalFilename());
        }

        String mrn = attrs.getString(Tag.PatientID);
        if (!StringUtils.hasText(mrn)) {
            throw new UploadValidationException(
                    "Tag PatientID manquant dans le fichier DICOM : " + fichierDicom.getOriginalFilename());
        }

        String nomFichier = fichierDicom.getOriginalFilename();
        return new DicomMetadata(
                mrn,
                attrs.getString(Tag.PatientName),
                parseDateDicom(attrs.getString(Tag.PatientBirthDate)),
                attrs.getString(Tag.PatientSex),
                parseDateDicom(attrs.getString(Tag.StudyDate)),
                lireTagSecurise(attrs, Tag.Modality, nomFichier),
                resoudreZone(attrs, nomFichier),
                attrs.getString(Tag.StudyDescription),
                attrs.getString(Tag.StudyInstanceUID));
    }

    private LocalDate parseDateDicom(String valeurDA) {
        if (!StringUtils.hasText(valeurDA)) {
            return null;
        }
        try {
            return LocalDate.parse(valeurDA, FORMAT_DICOM_DA);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * BodyPartExamined, StudyDescription et SeriesDescription sont des tags
     * de niveau Series, identiques sur toutes les coupes d'une même série.
     * L'appelant (ExamenUploadServiceImpl) n'utilise donc que la valeur du
     * fichier de référence du batch, jamais une agrégation coupe par coupe.
     */
    private ExamenZoneEnum resoudreZone(Attributes attrs, String nomFichier) {
        String candidat = lireTagSecurise(attrs, Tag.BodyPartExamined, nomFichier);
        if (!StringUtils.hasText(candidat)) {
            candidat = lireTagSecurise(attrs, Tag.StudyDescription, nomFichier);
        }
        if (!StringUtils.hasText(candidat)) {
            candidat = lireTagSecurise(attrs, Tag.SeriesDescription, nomFichier);
        }
        if (!StringUtils.hasText(candidat)) {
            return null;
        }
        return normaliserZone(candidat);
    }

    private ExamenZoneEnum normaliserZone(String valeur) {
        String normalise = valeur.trim().toUpperCase();
        for (Map.Entry<String, ExamenZoneEnum> motCle : MOTS_CLES_ZONE.entrySet()) {
            if (normalise.contains(motCle.getKey())) {
                return motCle.getValue();
            }
        }
        return ExamenZoneEnum.UNKNOWN;
    }

    /**
     * Lecture best-effort : un tag illisible ne doit jamais faire échouer
     * tout l'upload (contrairement à PatientID, contrôlé plus haut).
     */
    private String lireTagSecurise(Attributes attrs, int tag, String nomFichier) {
        try {
            return attrs.getString(tag);
        } catch (Exception e) {
            log.warn("Lecture du tag DICOM {} impossible sur {} : {}", tag, nomFichier, e.getMessage());
            return null;
        }
    }
}
