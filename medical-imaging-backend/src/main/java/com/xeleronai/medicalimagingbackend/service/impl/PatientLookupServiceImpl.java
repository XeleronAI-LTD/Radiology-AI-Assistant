package com.xeleronai.medicalimagingbackend.service.impl;

import com.xeleronai.medicalimagingbackend.entity.Institution;
import com.xeleronai.medicalimagingbackend.entity.Patient;
import com.xeleronai.medicalimagingbackend.repository.PatientRepository;
import com.xeleronai.medicalimagingbackend.service.PatientLookupService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Volontairement PAS @Transactional sur cette classe : voir le Javadoc de
 * PatientLookupService. Chaque appel à patientRepository.save()/findByInstitutionIdAndMrn()
 * s'exécute dans sa propre transaction implicite (comportement par défaut de
 * Spring Data JPA), à condition qu'aucune transaction ne soit déjà ouverte
 * autour de l'appel à trouverOuCreerPatient().
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PatientLookupServiceImpl implements PatientLookupService {

    private final PatientRepository patientRepository;

    @Override
    public Patient trouverOuCreerPatient(
            String mrn, String nom, LocalDate dateNaissance, String sexe, Institution institution) {
        return patientRepository.findByInstitutionIdAndMrn(institution.getId(), mrn).orElseGet(() -> {
            try {
                return patientRepository.save(Patient.builder()
                        .institution(institution)
                        .mrn(mrn)
                        .nom(nom)
                        .dateNaissance(dateNaissance)
                        .sexe(sexe)
                        .build());
            } catch (DataIntegrityViolationException e) {
                log.info("Course détectée sur la contrainte unique (institution_id, mrn), repêchage du patient existant");
                return patientRepository.findByInstitutionIdAndMrn(institution.getId(), mrn).orElseThrow(() -> e);
            }
        });
    }
}
