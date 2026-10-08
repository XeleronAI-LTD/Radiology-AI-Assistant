package com.xeleronai.medicalimagingbackend.service.impl;

import com.xeleronai.medicalimagingbackend.entity.AuditLog;
import com.xeleronai.medicalimagingbackend.entity.Examen;
import com.xeleronai.medicalimagingbackend.entity.Image;
import com.xeleronai.medicalimagingbackend.entity.Utilisateur;
import com.xeleronai.medicalimagingbackend.entity.enums.AnalyseStatutEnum;
import com.xeleronai.medicalimagingbackend.entity.enums.AuditActionEnum;
import com.xeleronai.medicalimagingbackend.entity.enums.ExamenZoneEnum;
import com.xeleronai.medicalimagingbackend.repository.AuditLogRepository;
import com.xeleronai.medicalimagingbackend.repository.ExamenRepository;
import com.xeleronai.medicalimagingbackend.repository.ImageRepository;
import com.xeleronai.medicalimagingbackend.service.AnalyseEnCoursException;
import com.xeleronai.medicalimagingbackend.service.AnalyseNonLancableException;
import com.xeleronai.medicalimagingbackend.service.DetectionAiClient;
import com.xeleronai.medicalimagingbackend.service.DetectionAiIndisponibleException;
import com.xeleronai.medicalimagingbackend.service.DetectionAnalyseService;
import com.xeleronai.medicalimagingbackend.service.DetectionIA;
import com.xeleronai.medicalimagingbackend.service.DetectionRunPersistenceService;
import com.xeleronai.medicalimagingbackend.service.LectureImpossibleException;
import com.xeleronai.medicalimagingbackend.service.MasqueDetecte;
import com.xeleronai.medicalimagingbackend.service.PredictionIA;
import com.xeleronai.medicalimagingbackend.service.RessourceIntrouvableException;
import com.xeleronai.medicalimagingbackend.service.StorageService;
import com.xeleronai.medicalimagingbackend.service.UploadEchoueException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Volontairement PAS @Transactional sur cette classe : analyserEnArrierePlan
 * orchestre une lecture MinIO (non transactionnelle) et un appel HTTP long
 * (jusqu'à ~31 min) puis délègue la persistance à
 * DetectionRunPersistenceService, bean séparé et strictement transactionnel
 * — même principe que ExamenUploadServiceImpl / ExamenPersistenceService.
 * Séparer les deux évite aussi le piège du self-invocation Spring AOP :
 * @Async et @Transactional ne sont interceptés par le proxy Spring que sur
 * un appel externe à ce bean, jamais sur un appel interne (this.xxx()).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DetectionAnalyseServiceImpl implements DetectionAnalyseService {

    private final ExamenRepository examenRepository;
    private final ImageRepository imageRepository;
    private final StorageService storageService;
    private final DetectionAiClient detectionAiClient;
    private final DetectionRunPersistenceService detectionRunPersistenceService;
    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public void lancerAnalyse(Long examenId, Utilisateur utilisateurCourant) {
        Examen examen = examenRepository.findById(examenId)
                .orElseThrow(() -> new RessourceIntrouvableException("Examen introuvable"));

        if (examen.getModalite() == null
                || examen.getZone() == null
                || examen.getZone() == ExamenZoneEnum.UNKNOWN) {
            throw new AnalyseNonLancableException("Modalité ou zone non renseignée, routage impossible");
        }
        if (examen.getImages().isEmpty()) {
            throw new AnalyseNonLancableException("Aucune image dans cet examen");
        }
        if (examen.getStatutAnalyse() == AnalyseStatutEnum.EN_COURS) {
            throw new AnalyseEnCoursException("Une analyse est déjà en cours pour cet examen");
        }

        examen.setStatutAnalyse(AnalyseStatutEnum.EN_COURS);
        examenRepository.save(examen);

        auditLogRepository.save(AuditLog.builder()
                .institution(utilisateurCourant.getInstitution())
                .utilisateur(utilisateurCourant)
                .action(AuditActionEnum.ANALYSE_DEMANDEE.name())
                .resourceType("EXAMEN")
                .resourceId(examenId)
                .build());
    }

    @Override
    @Async("detectionTaskExecutor")
    public void analyserEnArrierePlan(Long examenId, Long utilisateurId) {
        try {
            Examen examen = examenRepository.findById(examenId)
                    .orElseThrow(() -> new IllegalStateException("Examen disparu pendant l'analyse : " + examenId));
            List<Image> images = imageRepository.findByExamenIdOrderByOrdreAsc(examenId);

            List<byte[]> fichiers = images.stream()
                    .map(image -> storageService.lire(image.getCheminOriginal()))
                    .toList();

            PredictionIA prediction = detectionAiClient.predict(
                    fichiers, examen.getModalite(), examen.getZone().name());

            if ("MASQUE".equals(prediction.type())) {
                List<MasqueDetecte> masques = new ArrayList<>();
                for (DetectionIA d : prediction.detections()) {
                    byte[] pngMasque = Base64.getDecoder().decode(d.masqueBase64());
                    String cle = "examens/" + examenId + "/masques/" + UUID.randomUUID() + ".png";
                    storageService.uploader(cle, pngMasque, "image/png");
                    masques.add(new MasqueDetecte(d.coupe(), d.label(), d.confiance(), cle));
                }
                detectionRunPersistenceService.enregistrerSuccesMasques(examenId, images, masques, utilisateurId);
            } else {
                detectionRunPersistenceService.enregistrerSucces(examenId, images, prediction, utilisateurId);
            }
        } catch (Exception e) {
            log.error("Échec de l'analyse IA pour l'examen {}", examenId, e);
            detectionRunPersistenceService.enregistrerEchec(examenId, messageLisible(e), utilisateurId);
        }
    }

    private String messageLisible(Exception e) {
        String base = switch (e) {
            case DetectionAiIndisponibleException ignored -> "Service de détection IA injoignable ou en erreur.";
            case LectureImpossibleException ignored -> "Échec de lecture d'une image depuis le stockage.";
            case UploadEchoueException ignored -> "Échec du stockage d'un masque de segmentation.";
            default -> "Erreur inattendue pendant l'analyse.";
        };
        return base.length() > 500 ? base.substring(0, 500) : base;
    }
}
