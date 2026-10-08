package com.xeleronai.medicalimagingbackend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeleronai.medicalimagingbackend.entity.Examen;
import com.xeleronai.medicalimagingbackend.entity.Institution;
import com.xeleronai.medicalimagingbackend.entity.Patient;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * Test d'intégration contre la vraie base Postgres locale (medimg-pg) —
 * cohérent avec PatientLookupServiceImplTest, seul précédent du projet pour
 * ce type de test.
 */
@SpringBootTest
class ExamenRepositoryQueryTest {

    @Autowired
    private ExamenRepository examenRepository;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private InstitutionRepository institutionRepository;

    private Institution institutionParDefaut;
    private Patient patientA;
    private Patient patientB;
    private Examen examenAncien;
    private Examen examenRecent;

    @BeforeEach
    void setUp() {
        institutionParDefaut = institutionRepository.findByCode(Institution.CODE_DEFAUT).orElseThrow();

        String suffixe = UUID.randomUUID().toString();
        patientA = patientRepository.save(Patient.builder()
                .institution(institutionParDefaut).mrn("TEST-A-" + suffixe).nom("Dupont").build());
        patientB = patientRepository.save(Patient.builder()
                .institution(institutionParDefaut).mrn("TEST-B-" + suffixe).nom("Martin").build());

        examenAncien = examenRepository.save(Examen.builder()
                .institution(institutionParDefaut)
                .patient(patientA).type("Radio").dateExamen(LocalDate.of(2026, 1, 1)).modalite("CR").build());
        examenRecent = examenRepository.save(Examen.builder()
                .institution(institutionParDefaut)
                .patient(patientB).type("Echo").dateExamen(LocalDate.of(2026, 6, 1)).modalite("US").build());
    }

    @AfterEach
    void nettoyer() {
        examenRepository.delete(examenRecent);
        examenRepository.delete(examenAncien);
        patientRepository.delete(patientB);
        patientRepository.delete(patientA);
    }

    @Test
    void findAllAvecPatient_trieParDateExamenDecroissant_etPatientAccessibleSansLazyException() {
        Page<Examen> page = examenRepository.findAllAvecPatient(
                institutionParDefaut.getId(), PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "dateExamen")));

        // Comparaison par id : l'entité Examen (@Data) génère equals() sur
        // tous les champs y compris la collection images, peu fiable pour
        // comparer une entité fraîchement rechargée à l'instance de setUp().
        List<Long> ids = page.getContent().stream().map(Examen::getId).toList();
        int indexRecent = ids.indexOf(examenRecent.getId());
        int indexAncien = ids.indexOf(examenAncien.getId());

        assertThat(indexRecent).isGreaterThanOrEqualTo(0);
        assertThat(indexAncien).isGreaterThanOrEqualTo(0);
        assertThat(indexRecent).isLessThan(indexAncien);

        // Accès au patient hors de toute transaction explicite : ne doit pas
        // lever LazyInitializationException grâce au JOIN FETCH.
        assertThat(page.getContent().get(indexRecent).getPatient().getNom()).isEqualTo("Martin");
        assertThat(page.getContent().get(indexAncien).getPatient().getNom()).isEqualTo("Dupont");
    }

    @Test
    void findByPatientMrnAvecPatient_filtreCorrectement() {
        Page<Examen> page = examenRepository.findByPatientMrnAvecPatient(
                patientA.getMrn(), institutionParDefaut.getId(),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "dateExamen")));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getId()).isEqualTo(examenAncien.getId());
    }

    @Test
    void findByPatientMrnAvecPatient_mrnInexistant_retourneVide() {
        Page<Examen> page = examenRepository.findByPatientMrnAvecPatient(
                "MRN-INEXISTANT-" + UUID.randomUUID(), institutionParDefaut.getId(), PageRequest.of(0, 20));

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    void findAllAvecPatient_isoleParInstitution_uneInstitutionNeVoitPasLesExamensDUneAutre() {
        Institution autreInstitution = institutionRepository.save(Institution.builder()
                .nom("Autre hôpital de test").code("TEST-" + UUID.randomUUID()).build());
        Patient patientAutreInstitution = patientRepository.save(Patient.builder()
                .institution(autreInstitution).mrn("TEST-AUTRE-" + UUID.randomUUID()).nom("Durand").build());
        Examen examenAutreInstitution = examenRepository.save(Examen.builder()
                .institution(autreInstitution)
                .patient(patientAutreInstitution).type("Radio").dateExamen(LocalDate.of(2026, 3, 1)).modalite("CR")
                .build());

        try {
            Page<Examen> pageDefaut = examenRepository.findAllAvecPatient(
                    institutionParDefaut.getId(), PageRequest.of(0, 100));
            Page<Examen> pageAutre = examenRepository.findAllAvecPatient(
                    autreInstitution.getId(), PageRequest.of(0, 100));

            List<Long> idsDefaut = pageDefaut.getContent().stream().map(Examen::getId).toList();
            List<Long> idsAutre = pageAutre.getContent().stream().map(Examen::getId).toList();

            assertThat(idsDefaut).doesNotContain(examenAutreInstitution.getId());
            assertThat(idsAutre).containsExactly(examenAutreInstitution.getId());
        } finally {
            examenRepository.delete(examenAutreInstitution);
            patientRepository.delete(patientAutreInstitution);
            institutionRepository.delete(autreInstitution);
        }
    }
}
