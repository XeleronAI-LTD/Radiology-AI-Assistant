package com.xeleronai.medicalimagingbackend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.xeleronai.medicalimagingbackend.entity.enums.ExamenZoneEnum;
import com.xeleronai.medicalimagingbackend.service.DicomMetadata;
import com.xeleronai.medicalimagingbackend.service.UploadValidationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.data.VR;
import org.dcm4che3.io.DicomOutputStream;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class DicomMetadataServiceImplTest {

    private final DicomMetadataServiceImpl service = new DicomMetadataServiceImpl();

    @Test
    void extraire_bodyPartExaminedChest_ressortZoneThorax() {
        MockMultipartFile fichier = fichierDicom(attrs -> {
            attrs.setString(Tag.PatientID, VR.LO, "MRN-1");
            attrs.setString(Tag.Modality, VR.CS, "CT");
            attrs.setString(Tag.BodyPartExamined, VR.CS, "CHEST");
        });

        DicomMetadata metadata = service.extraire(fichier);

        assertThat(metadata.zone()).isEqualTo(ExamenZoneEnum.THORAX);
        assertThat(metadata.modalite()).isEqualTo("CT");
    }

    @Test
    void extraire_bodyPartExaminedAbsent_replieSurStudyDescription() {
        MockMultipartFile fichier = fichierDicom(attrs -> {
            attrs.setString(Tag.PatientID, VR.LO, "MRN-1");
            attrs.setString(Tag.StudyDescription, VR.LO, "CT ABDOMEN SANS INJECTION");
        });

        DicomMetadata metadata = service.extraire(fichier);

        assertThat(metadata.zone()).isEqualTo(ExamenZoneEnum.ABDOMEN);
    }

    @Test
    void extraire_bodyPartEtStudyDescriptionAbsents_replieSurSeriesDescription() {
        MockMultipartFile fichier = fichierDicom(attrs -> {
            attrs.setString(Tag.PatientID, VR.LO, "MRN-1");
            attrs.setString(Tag.SeriesDescription, VR.LO, "IRM CRANE T1");
        });

        DicomMetadata metadata = service.extraire(fichier);

        assertThat(metadata.zone()).isEqualTo(ExamenZoneEnum.CRANE);
    }

    @Test
    void extraire_studyDescriptionEchocardiogram_ressortZoneCoeur() {
        MockMultipartFile fichier = fichierDicom(attrs -> {
            attrs.setString(Tag.PatientID, VR.LO, "MRN-1");
            attrs.setString(Tag.Modality, VR.CS, "US");
            attrs.setString(Tag.StudyDescription, VR.LO, "Echocardiogram");
        });

        DicomMetadata metadata = service.extraire(fichier);

        assertThat(metadata.zone()).isEqualTo(ExamenZoneEnum.COEUR);
    }

    @Test
    void extraire_aucunTagZoneRenseigne_ressortZoneNullSansPlanter() {
        MockMultipartFile fichier = fichierDicom(attrs -> attrs.setString(Tag.PatientID, VR.LO, "MRN-1"));

        DicomMetadata metadata = service.extraire(fichier);

        assertThat(metadata.zone()).isNull();
    }

    @Test
    void extraire_bodyPartExaminedNonReconnu_ressortZoneUnknown() {
        MockMultipartFile fichier = fichierDicom(attrs -> {
            attrs.setString(Tag.PatientID, VR.LO, "MRN-1");
            attrs.setString(Tag.BodyPartExamined, VR.CS, "FOOT");
        });

        DicomMetadata metadata = service.extraire(fichier);

        assertThat(metadata.zone()).isEqualTo(ExamenZoneEnum.UNKNOWN);
    }

    @Test
    void extraire_patientIdManquant_leveUploadValidationException() {
        MockMultipartFile fichier = fichierDicom(attrs -> attrs.setString(Tag.Modality, VR.CS, "CT"));

        assertThatThrownBy(() -> service.extraire(fichier)).isInstanceOf(UploadValidationException.class);
    }

    private MockMultipartFile fichierDicom(java.util.function.Consumer<Attributes> remplisseur) {
        Attributes dataset = new Attributes();
        dataset.setString(Tag.SOPClassUID, VR.UI, UID.SecondaryCaptureImageStorage);
        dataset.setString(Tag.SOPInstanceUID, VR.UI, "1.2.3.4.5");
        remplisseur.accept(dataset);
        return new MockMultipartFile("files", "test.dcm", "application/dicom", ecrireDicom(dataset));
    }

    private byte[] ecrireDicom(Attributes dataset) {
        Attributes fmi = dataset.createFileMetaInformation(UID.ExplicitVRLittleEndian);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DicomOutputStream dos = new DicomOutputStream(baos, UID.ExplicitVRLittleEndian)) {
            dos.writeDataset(fmi, dataset);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return baos.toByteArray();
    }
}
