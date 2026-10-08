package com.xeleronai.medicalimagingbackend.service.impl;

import com.xeleronai.medicalimagingbackend.entity.AuditLog;
import com.xeleronai.medicalimagingbackend.entity.Examen;
import com.xeleronai.medicalimagingbackend.entity.Image;
import com.xeleronai.medicalimagingbackend.entity.Patient;
import com.xeleronai.medicalimagingbackend.entity.Utilisateur;
import com.xeleronai.medicalimagingbackend.entity.enums.AuditActionEnum;
import com.xeleronai.medicalimagingbackend.entity.enums.ExamenZoneEnum;
import com.xeleronai.medicalimagingbackend.repository.AuditLogRepository;
import com.xeleronai.medicalimagingbackend.repository.ExamenRepository;
import com.xeleronai.medicalimagingbackend.service.ExamenPersistenceService;
import com.xeleronai.medicalimagingbackend.service.ImagePreparee;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ExamenPersistenceServiceImpl implements ExamenPersistenceService {

    private final ExamenRepository examenRepository;
    private final AuditLogRepository auditLogRepository;

    @Override
    public Examen persisterExamen(
            Patient patient,
            Utilisateur utilisateurCourant,
            String type,
            LocalDate dateExamen,
            String modalite,
            ExamenZoneEnum zone,
            List<ImagePreparee> images) {

        Examen examen = Examen.builder()
                .institution(utilisateurCourant.getInstitution())
                .patient(patient)
                .creePar(utilisateurCourant)
                .type(type)
                .dateExamen(dateExamen)
                .modalite(modalite)
                .zone(zone)
                .images(new ArrayList<>())
                .build();

        for (ImagePreparee ip : images) {
            examen.getImages().add(Image.builder()
                    .examen(examen)
                    .cheminOriginal(ip.cheminOriginal())
                    .cheminApercu(ip.cheminApercu())
                    .format(ip.format())
                    .ordre(ip.ordre())
                    .build());
        }

        Examen sauvegarde = examenRepository.save(examen);

        auditLogRepository.save(AuditLog.builder()
                .institution(utilisateurCourant.getInstitution())
                .utilisateur(utilisateurCourant)
                .action(AuditActionEnum.UPLOAD_EXAMEN.name())
                .resourceType("EXAMEN")
                .resourceId(sauvegarde.getId())
                .build());

        log.info("Examen créé (id={}, nombreImages={})", sauvegarde.getId(), images.size());

        return sauvegarde;
    }
}
