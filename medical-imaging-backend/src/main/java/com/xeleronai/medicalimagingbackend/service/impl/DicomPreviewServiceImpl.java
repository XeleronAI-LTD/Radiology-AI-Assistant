package com.xeleronai.medicalimagingbackend.service.impl;

import com.xeleronai.medicalimagingbackend.service.DicomPreviewService;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Optional;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class DicomPreviewServiceImpl implements DicomPreviewService {

    @Override
    public Optional<byte[]> genererApercuPng(MultipartFile fichierDicom) {
        try (InputStream in = fichierDicom.getInputStream()) {
            BufferedImage image = ImageIO.read(in);
            if (image == null) {
                log.warn("Aucun lecteur ImageIO n'a pu décoder les pixels du fichier : {}",
                        fichierDicom.getOriginalFilename());
                return Optional.empty();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return Optional.of(out.toByteArray());
        } catch (Exception e) {
            log.warn("Échec de génération de l'aperçu DICOM pour {}", fichierDicom.getOriginalFilename());
            return Optional.empty();
        }
    }
}
