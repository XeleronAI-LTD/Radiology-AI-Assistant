package com.xeleronai.medicalimagingbackend.service.impl;

import com.xeleronai.medicalimagingbackend.entity.AuditLog;
import com.xeleronai.medicalimagingbackend.entity.Detection;
import com.xeleronai.medicalimagingbackend.entity.Examen;
import com.xeleronai.medicalimagingbackend.entity.Image;
import com.xeleronai.medicalimagingbackend.entity.Utilisateur;
import com.xeleronai.medicalimagingbackend.entity.enums.AnalyseStatutEnum;
import com.xeleronai.medicalimagingbackend.entity.enums.AuditActionEnum;
import com.xeleronai.medicalimagingbackend.entity.enums.DetectionStatutEnum;
import com.xeleronai.medicalimagingbackend.entity.enums.DetectionTypeEnum;
import com.xeleronai.medicalimagingbackend.repository.AuditLogRepository;
import com.xeleronai.medicalimagingbackend.repository.DetectionRepository;
import com.xeleronai.medicalimagingbackend.repository.ExamenRepository;
import com.xeleronai.medicalimagingbackend.repository.UtilisateurRepository;
import com.xeleronai.medicalimagingbackend.service.DetectionIA;
import com.xeleronai.medicalimagingbackend.service.DetectionRunPersistenceService;
import com.xeleronai.medicalimagingbackend.service.MasqueDetecte;
import com.xeleronai.medicalimagingbackend.service.PredictionIA;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DetectionRunPersistenceServiceImpl implements DetectionRunPersistenceService {

    private static final int ANALYSE_MESSAGE_MAX_LENGTH = 500;

    private final ExamenRepository examenRepository;
    private final DetectionRepository detectionRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public void enregistrerSucces(
            Long examenId, List<Image> imagesOrdonnees, PredictionIA prediction, Long utilisateurId) {

        Map<Integer, Image> parOrdre = new HashMap<>();
        for (Image image : imagesOrdonnees) {
            parOrdre.put(image.getOrdre(), image);
        }

        List<Detection> detections = new ArrayList<>();
        for (DetectionIA d : prediction.detections()) {
            Image image = parOrdre.get(d.coupe());
            if (image == null) {
                log.warn("Détection ignorée : coupe {} hors de la plage des images de l'examen {}",
                        d.coupe(), examenId);
                continue;
            }
            detections.add(Detection.builder()
                    .image(image)
                    .type(DetectionTypeEnum.BOX)
                    .anomalie(d.label())
                    .confiance(d.confiance())
                    .statut(DetectionStatutEnum.EN_ATTENTE)
                    .coupe(d.coupe())
                    .x((int) Math.round(d.bbox().x()))
                    .y((int) Math.round(d.bbox().y()))
                    .largeur((int) Math.round(d.bbox().w()))
                    .hauteur((int) Math.round(d.bbox().h()))
                    .build());
        }
        detectionRepository.saveAll(detections);

        marquerAnalyseTerminee(examenId, detections.size(), utilisateurId);
    }

    @Override
    @Transactional
    public void enregistrerSuccesMasques(
            Long examenId, List<Image> imagesOrdonnees, List<MasqueDetecte> masques, Long utilisateurId) {

        Map<Integer, Image> parOrdre = new HashMap<>();
        for (Image image : imagesOrdonnees) {
            parOrdre.put(image.getOrdre(), image);
        }

        List<Detection> detections = new ArrayList<>();
        for (MasqueDetecte m : masques) {
            Image image = parOrdre.get(m.coupe());
            if (image == null) {
                log.warn("Masque ignoré : coupe {} hors de la plage des images de l'examen {}",
                        m.coupe(), examenId);
                continue;
            }
            detections.add(Detection.builder()
                    .image(image)
                    .type(DetectionTypeEnum.MASQUE)
                    .anomalie(m.label())
                    .confiance(m.confiance())
                    .statut(DetectionStatutEnum.EN_ATTENTE)
                    .coupe(m.coupe())
                    .cheminMasque(m.cheminMasque())
                    .build());
        }
        detectionRepository.saveAll(detections);

        marquerAnalyseTerminee(examenId, detections.size(), utilisateurId);
    }

    private void marquerAnalyseTerminee(Long examenId, int nombreDetections, Long utilisateurId) {
        Examen examen = examenRepository.findById(examenId)
                .orElseThrow(() -> new IllegalStateException("Examen disparu pendant l'analyse : " + examenId));
        examen.setStatutAnalyse(AnalyseStatutEnum.TERMINEE);
        examen.setAnalyseFinieLe(LocalDateTime.now());
        examen.setAnalyseMessage(null);
        examenRepository.save(examen);

        ecrireAudit(examenId, utilisateurId);

        log.info("Analyse IA terminée (examenId={}, nombreDetections={})", examenId, nombreDetections);
    }

    @Override
    @Transactional
    public void enregistrerEchec(Long examenId, String message, Long utilisateurId) {
        Examen examen = examenRepository.findById(examenId)
                .orElseThrow(() -> new IllegalStateException("Examen disparu pendant l'analyse : " + examenId));
        examen.setStatutAnalyse(AnalyseStatutEnum.ECHOUEE);
        examen.setAnalyseMessage(tronquer(message));
        examen.setAnalyseFinieLe(LocalDateTime.now());
        examenRepository.save(examen);

        ecrireAudit(examenId, utilisateurId);

        log.warn("Analyse IA échouée (examenId={}, message={})", examenId, message);
    }

    private void ecrireAudit(Long examenId, Long utilisateurId) {
        Utilisateur utilisateur = utilisateurRepository.getReferenceById(utilisateurId);
        auditLogRepository.save(AuditLog.builder()
                .institution(utilisateur.getInstitution())
                .utilisateur(utilisateur)
                .action(AuditActionEnum.DETECTION_RUN.name())
                .resourceType("EXAMEN")
                .resourceId(examenId)
                .build());
    }

    private String tronquer(String message) {
        if (message != null && message.length() > ANALYSE_MESSAGE_MAX_LENGTH) {
            return message.substring(0, ANALYSE_MESSAGE_MAX_LENGTH);
        }
        return message;
    }
}
