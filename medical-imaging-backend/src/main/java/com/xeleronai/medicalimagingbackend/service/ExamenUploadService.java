package com.xeleronai.medicalimagingbackend.service;

import com.xeleronai.medicalimagingbackend.dto.examen.ExamenUploadResponse;
import com.xeleronai.medicalimagingbackend.dto.examen.UploadStandardRequest;
import com.xeleronai.medicalimagingbackend.entity.Utilisateur;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface ExamenUploadService {

    ExamenUploadResponse uploadStandard(UploadStandardRequest request, Utilisateur utilisateurCourant);

    ExamenUploadResponse uploadDicom(List<MultipartFile> files, Utilisateur utilisateurCourant);
}
