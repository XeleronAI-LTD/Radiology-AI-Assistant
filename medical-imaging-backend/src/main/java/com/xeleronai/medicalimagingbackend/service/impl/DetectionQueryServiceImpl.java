package com.xeleronai.medicalimagingbackend.service.impl;

import com.xeleronai.medicalimagingbackend.dto.detection.AnalyseStatutResponse;
import com.xeleronai.medicalimagingbackend.dto.detection.DetectionResponse;
import com.xeleronai.medicalimagingbackend.entity.AuditLog;
import com.xeleronai.medicalimagingbackend.entity.Detection;
import com.xeleronai.medicalimagingbackend.entity.Examen;
import com.xeleronai.medicalimagingbackend.entity.Utilisateur;
import com.xeleronai.medicalimagingbackend.entity.enums.AuditActionEnum;
import com.xeleronai.medicalimagingbackend.entity.enums.DetectionTypeEnum;
import com.xeleronai.medicalimagingbackend.repository.AuditLogRepository;
import com.xeleronai.medicalimagingbackend.repository.DetectionRepository;
import com.xeleronai.medicalimagingbackend.repository.ExamenRepository;
import com.xeleronai.medicalimagingbackend.service.DetectionQueryService;
import com.xeleronai.medicalimagingbackend.service.RessourceIntrouvableException;
import com.xeleronai.medicalimagingbackend.service.StorageService;
import com.xeleronai.medicalimagingbackend.service.mapper.DetectionMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DetectionQueryServiceImpl implements DetectionQueryService {

    private final ExamenRepository examenRepository;
    private final DetectionRepository detectionRepository;
    private final DetectionMapper detectionMapper;
    private final StorageService storageService;
    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional(readOnly = true)
    public List<DetectionResponse> listerParExamen(Long examenId) {
        if (!examenRepository.existsById(examenId)) {
            throw new RessourceIntrouvableException("Examen introuvable");
        }
        return detectionRepository.findByExamenId(examenId).stream()
                .map(detectionMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyseStatutResponse statutAnalyse(Long examenId) {
        Examen examen = examenRepository.findById(examenId)
                .orElseThrow(() -> new RessourceIntrouvableException("Examen introuvable"));
        return AnalyseStatutResponse.builder()
                .examenId(examen.getId())
                .statut(examen.getStatutAnalyse().name())
                .message(examen.getAnalyseMessage())
                .finieLe(examen.getAnalyseFinieLe())
                .build();
    }

    @Override
    @Transactional
    public byte[] lireMasque(Long examenId, Long detectionId, Utilisateur utilisateurCourant) {
        Detection detection = detectionRepository.findByIdAndExamenId(detectionId, examenId)
                .orElseThrow(() -> new RessourceIntrouvableException("Détection introuvable"));

        if (detection.getType() != DetectionTypeEnum.MASQUE || detection.getCheminMasque() == null) {
            throw new RessourceIntrouvableException("Aucun masque disponible pour cette détection");
        }

        byte[] contenu = storageService.lire(detection.getCheminMasque());

        auditLogRepository.save(AuditLog.builder()
                .institution(utilisateurCourant.getInstitution())
                .utilisateur(utilisateurCourant)
                .action(AuditActionEnum.VIEW_MASQUE.name())
                .resourceType("DETECTION")
                .resourceId(detectionId)
                .build());

        log.info("Masque consulté (detectionId={}, examenId={})", detectionId, examenId);

        return contenu;
    }
}
