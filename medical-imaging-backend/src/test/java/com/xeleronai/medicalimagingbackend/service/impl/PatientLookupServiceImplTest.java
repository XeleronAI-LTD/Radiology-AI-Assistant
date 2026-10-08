package com.xeleronai.medicalimagingbackend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeleronai.medicalimagingbackend.entity.Institution;
import com.xeleronai.medicalimagingbackend.entity.Patient;
import com.xeleronai.medicalimagingbackend.repository.InstitutionRepository;
import com.xeleronai.medicalimagingbackend.repository.PatientRepository;
import com.xeleronai.medicalimagingbackend.service.PatientLookupService;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Test d'intégration contre la vraie base Postgres locale (medimg-pg) — pas
 * de H2, cohérent avec MedicalImagingBackendApplicationTests, seul précédent
 * du projet.
 */
@SpringBootTest
class PatientLookupServiceImplTest {

    @Autowired
    private PatientLookupService patientLookupService;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private InstitutionRepository institutionRepository;

    private String mrnDeTest;
    private Institution autreInstitutionDeTest;

    @AfterEach
    void nettoyer() {
        if (mrnDeTest != null) {
            Institution institutionParDefaut = institutionRepository.findByCode(Institution.CODE_DEFAUT).orElseThrow();
            patientRepository.findByInstitutionIdAndMrn(institutionParDefaut.getId(), mrnDeTest)
                    .ifPresent(patientRepository::delete);
            if (autreInstitutionDeTest != null) {
                patientRepository.findByInstitutionIdAndMrn(autreInstitutionDeTest.getId(), mrnDeTest)
                        .ifPresent(patientRepository::delete);
                institutionRepository.delete(autreInstitutionDeTest);
            }
        }
    }

    @Test
    void mrn_inexistant_cree_un_nouveau_patient() {
        mrnDeTest = "TEST-MRN-" + UUID.randomUUID();
        Institution institutionParDefaut = institutionRepository.findByCode(Institution.CODE_DEFAUT).orElseThrow();

        Patient patient = patientLookupService.trouverOuCreerPatient(
                mrnDeTest, "Dupont", null, "M", institutionParDefaut);

        assertThat(patient.getId()).isNotNull();
        assertThat(patient.getMrn()).isEqualTo(mrnDeTest);
        assertThat(patient.getNom()).isEqualTo("Dupont");
        assertThat(patient.getInstitution().getId()).isEqualTo(institutionParDefaut.getId());
    }

    @Test
    void mrn_existant_est_reutilise_sans_ecrasement() {
        mrnDeTest = "TEST-MRN-" + UUID.randomUUID();
        Institution institutionParDefaut = institutionRepository.findByCode(Institution.CODE_DEFAUT).orElseThrow();

        Patient premier = patientLookupService.trouverOuCreerPatient(
                mrnDeTest, "Dupont", null, "M", institutionParDefaut);
        Patient second = patientLookupService.trouverOuCreerPatient(
                mrnDeTest, "AUTRE_NOM_IGNORE", null, "F", institutionParDefaut);

        assertThat(second.getId()).isEqualTo(premier.getId());
        assertThat(second.getNom()).isEqualTo("Dupont");
        assertThat(second.getSexe()).isEqualTo("M");

        assertThat(patientRepository.findByInstitutionIdAndMrn(institutionParDefaut.getId(), mrnDeTest))
                .hasValueSatisfying(p -> assertThat(p.getNom()).isEqualTo("Dupont"));
    }

    @Test
    void memeMrn_deuxInstitutionsDifferentes_creeDeuxPatientsDistincts() {
        mrnDeTest = "TEST-MRN-" + UUID.randomUUID();
        Institution institutionParDefaut = institutionRepository.findByCode(Institution.CODE_DEFAUT).orElseThrow();
        autreInstitutionDeTest = institutionRepository.save(Institution.builder()
                .nom("Autre hôpital de test").code("TEST-" + UUID.randomUUID()).build());

        Patient patientInstitutionDefaut = patientLookupService.trouverOuCreerPatient(
                mrnDeTest, "Dupont", null, "M", institutionParDefaut);
        Patient patientAutreInstitution = patientLookupService.trouverOuCreerPatient(
                mrnDeTest, "Durand", null, "F", autreInstitutionDeTest);

        assertThat(patientInstitutionDefaut.getId()).isNotEqualTo(patientAutreInstitution.getId());
        assertThat(patientInstitutionDefaut.getMrn()).isEqualTo(patientAutreInstitution.getMrn());
        assertThat(patientAutreInstitution.getNom()).isEqualTo("Durand");
    }
}
