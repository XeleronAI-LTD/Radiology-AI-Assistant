package com.xeleronai.medicalimagingbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class FileTypeValidatorTest {

    private final FileTypeValidator validator = new FileTypeValidator();

    @Test
    void validerBatchNonVide_rejette_liste_vide() {
        assertThatThrownBy(() -> validator.validerBatchNonVide(List.of()))
                .isInstanceOf(UploadValidationException.class);
    }

    @Test
    void validerBatchNonVide_rejette_null() {
        assertThatThrownBy(() -> validator.validerBatchNonVide(null))
                .isInstanceOf(UploadValidationException.class);
    }

    @Test
    void validerBatchNonVide_accepte_liste_non_vide() {
        MockMultipartFile fichier = new MockMultipartFile("files", "image.png", "image/png", new byte[] {1, 2, 3});
        validator.validerBatchNonVide(List.of(fichier));
    }

    @Test
    void validerExtensionsStandard_accepte_png_jpg_jpeg() {
        MockMultipartFile png = new MockMultipartFile("files", "a.png", "image/png", new byte[] {1});
        MockMultipartFile jpg = new MockMultipartFile("files", "b.jpg", "image/jpeg", new byte[] {1});
        MockMultipartFile jpeg = new MockMultipartFile("files", "c.JPEG", "image/jpeg", new byte[] {1});
        validator.validerExtensionsStandard(List.of(png, jpg, jpeg));
    }

    @Test
    void validerExtensionsStandard_rejette_dcm() {
        MockMultipartFile dcm = new MockMultipartFile("files", "a.dcm", "application/dicom", new byte[] {1});
        assertThatThrownBy(() -> validator.validerExtensionsStandard(List.of(dcm)))
                .isInstanceOf(UploadValidationException.class);
    }

    @Test
    void validerExtensionsDicom_accepte_dcm() {
        MockMultipartFile dcm = new MockMultipartFile("files", "a.dcm", "application/dicom", new byte[] {1});
        validator.validerExtensionsDicom(List.of(dcm));
    }

    @Test
    void validerExtensionsDicom_rejette_png() {
        MockMultipartFile png = new MockMultipartFile("files", "a.png", "image/png", new byte[] {1});
        assertThatThrownBy(() -> validator.validerExtensionsDicom(List.of(png)))
                .isInstanceOf(UploadValidationException.class);
    }

    @Test
    void extensionDe_rejette_fichier_sans_extension() {
        MockMultipartFile sansExtension = new MockMultipartFile("files", "sansextension", "image/png", new byte[] {1});
        assertThatThrownBy(() -> FileTypeValidator.extensionDe(sansExtension))
                .isInstanceOf(UploadValidationException.class);
    }

    @Test
    void extensionDe_retourne_extension() {
        MockMultipartFile fichier = new MockMultipartFile("files", "image.PNG", "image/png", new byte[] {1});
        assertThat(FileTypeValidator.extensionDe(fichier)).isEqualTo("PNG");
    }
}
