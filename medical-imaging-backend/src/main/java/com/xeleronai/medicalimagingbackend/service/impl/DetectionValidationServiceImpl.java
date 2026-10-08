package com.xeleronai.medicalimagingbackend.service.impl;

import com.xeleronai.medicalimagingbackend.dto.detection.DetectionResponse;
import com.xeleronai.medicalimagingbackend.entity.AuditLog;
import com.xeleronai.medicalimagingbackend.entity.Detection;
import com.xeleronai.medicalimagingbackend.entity.Utilisateur;
import com.xeleronai.medicalimagingbackend.entity.enums.AuditActionEnum;
import com.xeleronai.medicalimagingbackend.entity.enums.DetectionStatutEnum;
import com.xeleronai.medicalimagingbackend.repository.AuditLogRepository;
import com.xeleronai.medicalimagingbackend.repository.DetectionRepository;
import com.xeleronai.medicalimagingbackend.service.DetectionValidationService;
import com.xeleronai.medicalimagingbackend.service.RessourceIntrouvableException;
import com.xeleronai.medicalimagingbackend.service.StatutDetectionInvalideException;
import com.xeleronai.medicalimagingbackend.service.mapper.DetectionMapper;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DetectionValidationServiceImpl implements DetectionValidationService {

    private final DetectionRepository detectionRepository;
    private final AuditLogRepository auditLogRepository;
    private final DetectionMapper detectionMapper;

    @Override
    @Transactional
    public DetectionResponse validerStatut(
            Long examenId, Long detectionId, String statutDemande, Utilisateur utilisateurCourant) {

        DetectionStatutEnum nouveauStatut = parserStatut(statutDemande);

        Detection detection = detectionRepository.findByIdAndExamenId(detectionId, examenId)
                .orElseThrow(() -> new RessourceIntrouvableException("Détection introuvable"));

        DetectionStatutEnum ancienStatut = detection.getStatut();

        detection.setStatut(nouveauStatut);
        detection.setValidateur(utilisateurCourant);
        detection.setValideLe(LocalDateTime.now());
        Detection sauvegarde = detectionRepository.save(detection);

        auditLogRepository.save(AuditLog.builder()
                .institution(utilisateurCourant.getInstitution())
                .utilisateur(utilisateurCourant)
                .action(AuditActionEnum.DETECTION_VALIDATION.name() + ":" + ancienStatut + "->" + nouveauStatut)
                .resourceType("DETECTION")
                .resourceId(detectionId)
                .build());

        log.info("Détection validée (detectionId={}, examenId={}, statut={})", detectionId, examenId, nouveauStatut);

        return detectionMapper.toResponse(sauvegarde);
    }

    private DetectionStatutEnum parserStatut(String statutDemande) {
        if (!"ACCEPTEE".equals(statutDemande) && !"REJETEE".equals(statutDemande)) {
            throw new StatutDetectionInvalideException(
                    "Statut invalide, attendu ACCEPTEE ou REJETEE : " + statutDemande);
        }
        return DetectionStatutEnum.valueOf(statutDemande);
    }
}
