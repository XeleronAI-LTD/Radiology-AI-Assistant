package com.xeleronai.medicalimagingbackend.service.impl;

import com.xeleronai.medicalimagingbackend.dto.common.PageResponse;
import com.xeleronai.medicalimagingbackend.dto.examen.ExamenDetailResponse;
import com.xeleronai.medicalimagingbackend.dto.examen.ExamenSummaryResponse;
import com.xeleronai.medicalimagingbackend.entity.AuditLog;
import com.xeleronai.medicalimagingbackend.entity.Examen;
import com.xeleronai.medicalimagingbackend.entity.Image;
import com.xeleronai.medicalimagingbackend.entity.Utilisateur;
import com.xeleronai.medicalimagingbackend.entity.enums.AuditActionEnum;
import com.xeleronai.medicalimagingbackend.repository.AuditLogRepository;
import com.xeleronai.medicalimagingbackend.repository.ExamenRepository;
import com.xeleronai.medicalimagingbackend.repository.ImageRepository;
import com.xeleronai.medicalimagingbackend.service.ExamenQueryService;
import com.xeleronai.medicalimagingbackend.service.RessourceIntrouvableException;
import com.xeleronai.medicalimagingbackend.service.StorageService;
import com.xeleronai.medicalimagingbackend.service.mapper.ExamenMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExamenQueryServiceImpl implements ExamenQueryService {

    private final ExamenRepository examenRepository;
    private final ImageRepository imageRepository;
    private final StorageService storageService;
    private final AuditLogRepository auditLogRepository;
    private final ExamenMapper examenMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ExamenSummaryResponse> lister(String mrn, int page, int size, Utilisateur utilisateurCourant) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "dateExamen"));
        Long institutionId = utilisateurCourant.getInstitution().getId();

        Page<Examen> resultat = StringUtils.hasText(mrn)
                ? examenRepository.findByPatientMrnAvecPatient(mrn, institutionId, pageable)
                : examenRepository.findAllAvecPatient(institutionId, pageable);

        return PageResponse.<ExamenSummaryResponse>builder()
                .content(resultat.getContent().stream().map(examenMapper::toSummaryResponse).toList())
                .page(resultat.getNumber())
                .size(resultat.getSize())
                .totalElements(resultat.getTotalElements())
                .totalPages(resultat.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ExamenDetailResponse detail(Long examenId) {
        Examen examen = examenRepository.findById(examenId)
                .orElseThrow(() -> new RessourceIntrouvableException("Examen introuvable"));
        return examenMapper.toDetailResponse(examen);
    }

    @Override
    @Transactional
    public byte[] lireApercu(Long examenId, Long imageId, Utilisateur utilisateurCourant) {
        Image image = imageRepository.findByIdAndExamenId(imageId, examenId)
                .orElseThrow(() -> new RessourceIntrouvableException("Image introuvable"));

        if (image.getCheminApercu() == null) {
            throw new RessourceIntrouvableException("Aucun aperçu disponible pour cette image");
        }

        byte[] contenu = storageService.lire(image.getCheminApercu());

        auditLogRepository.save(AuditLog.builder()
                .institution(utilisateurCourant.getInstitution())
                .utilisateur(utilisateurCourant)
                .action(AuditActionEnum.VIEW_IMAGE.name())
                .resourceType("IMAGE")
                .resourceId(imageId)
                .build());

        log.info("Image consultée (imageId={}, examenId={})", imageId, examenId);

        return contenu;
    }
}
