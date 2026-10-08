package com.xeleronai.medicalimagingbackend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeleronai.medicalimagingbackend.entity.Detection;
import com.xeleronai.medicalimagingbackend.entity.Examen;
import com.xeleronai.medicalimagingbackend.entity.Image;
import com.xeleronai.medicalimagingbackend.entity.Institution;
import com.xeleronai.medicalimagingbackend.entity.Patient;
import com.xeleronai.medicalimagingbackend.entity.enums.DetectionStatutEnum;
import com.xeleronai.medicalimagingbackend.entity.enums.DetectionTypeEnum;
import com.xeleronai.medicalimagingbackend.entity.enums.FormatImageEnum;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Test d'intégration contre la vraie base Postgres locale, même pattern que
 * ExamenRepositoryQueryTest.
 */
@SpringBootTest
class DetectionRepositoryQueryTest {

    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private ExamenRepository examenRepository;
    @Autowired
    private ImageRepository imageRepository;
    @Autowired
    private DetectionRepository detectionRepository;
    @Autowired
    private InstitutionRepository institutionRepository;

    private Patient patient;
    private Examen examen;
    private Image image;

    @BeforeEach
    void setUp() {
        Institution institutionParDefaut = institutionRepository.findByCode(Institution.CODE_DEFAUT).orElseThrow();

        String suffixe = UUID.randomUUID().toString();
        patient = patientRepository.save(Patient.builder()
                .institution(institutionParDefaut).mrn("TEST-DET-" + suffixe).nom("Dupont").build());
        examen = examenRepository.save(Examen.builder()
                .institution(institutionParDefaut).patient(patient).type("CT").modalite("CT").build());
        image = imageRepository.save(Image.builder()
                .examen(examen).cheminOriginal("examens/x/1.dcm").format(FormatImageEnum.DICOM).ordre(0).build());
    }

    @AfterEach
    void nettoyer() {
        // deleteById recharge une instance managée fraîche (avec sa collection
        // d'images à jour) dans la transaction du delete, contrairement à
        // delete(examen) qui merge l'instance détachée de setUp() — dont la
        // collection en mémoire ne reflète pas l'image ajoutée entre-temps.
        examenRepository.deleteById(examen.getId());
        patientRepository.deleteById(patient.getId());
    }

    @Test
    void persisterDetectionBox_liaisonImageEtStatutParDefaut() {
        Detection detection = detectionRepository.save(Detection.builder()
                .image(image)
                .type(DetectionTypeEnum.BOX)
                .anomalie("nodule")
                .confiance(0.87)
                .coupe(12)
                .x(10).y(20).largeur(30).hauteur(40)
                .build());

        Detection relue = detectionRepository.findById(detection.getId()).orElseThrow();

        assertThat(relue.getImage().getId()).isEqualTo(image.getId());
        assertThat(relue.getType()).isEqualTo(DetectionTypeEnum.BOX);
        assertThat(relue.getAnomalie()).isEqualTo("nodule");
        assertThat(relue.getConfiance()).isEqualTo(0.87);
        assertThat(relue.getStatut()).isEqualTo(DetectionStatutEnum.EN_ATTENTE);
        assertThat(relue.getCreatedAt()).isNotNull();
        assertThat(relue.getCheminMasque()).isNull();
    }

    @Test
    void plusieursDetectionsSurLaMemeImage_relationUnPourN() {
        Detection d1 = detectionRepository.save(Detection.builder()
                .image(image).type(DetectionTypeEnum.BOX).anomalie("nodule").confiance(0.9)
                .x(1).y(1).largeur(5).hauteur(5).build());
        Detection d2 = detectionRepository.save(Detection.builder()
                .image(image).type(DetectionTypeEnum.BOX).anomalie("opacite").confiance(0.6)
                .x(50).y(50).largeur(15).hauteur(15).build());

        List<Detection> detections = detectionRepository.findByExamenId(examen.getId());

        assertThat(detections).extracting(Detection::getId).containsExactlyInAnyOrder(d1.getId(), d2.getId());
        assertThat(detections).allSatisfy(d -> assertThat(d.getImage().getId()).isEqualTo(image.getId()));
    }

    @Test
    void findByIdAndExamenId_detectionDeLExamen_estRetrouvee() {
        Detection masque = detectionRepository.save(Detection.builder()
                .image(image).type(DetectionTypeEnum.MASQUE).anomalie("rate").confiance(0.95)
                .coupe(0).cheminMasque("examens/x/masques/uuid.png").build());

        Detection trouvee = detectionRepository.findByIdAndExamenId(masque.getId(), examen.getId()).orElseThrow();

        assertThat(trouvee.getId()).isEqualTo(masque.getId());
        assertThat(trouvee.getCheminMasque()).isEqualTo("examens/x/masques/uuid.png");
    }

    @Test
    void findByIdAndExamenId_detectionDuneAutreExamen_estVide() {
        Detection masque = detectionRepository.save(Detection.builder()
                .image(image).type(DetectionTypeEnum.MASQUE).anomalie("rate").confiance(0.95)
                .coupe(0).cheminMasque("examens/x/masques/uuid.png").build());

        assertThat(detectionRepository.findByIdAndExamenId(masque.getId(), examen.getId() + 1_000_000L)).isEmpty();
    }

    @Test
    void statutNonRenseigne_prendLaValeurParDefautEnAttente() {
        Detection detection = Detection.builder()
                .image(image).type(DetectionTypeEnum.BOX).confiance(0.5)
                .x(0).y(0).largeur(1).hauteur(1).build();

        assertThat(detection.getStatut()).isEqualTo(DetectionStatutEnum.EN_ATTENTE);

        Detection sauvegardee = detectionRepository.save(detection);
        Detection relue = detectionRepository.findById(sauvegardee.getId()).orElseThrow();
        assertThat(relue.getStatut()).isEqualTo(DetectionStatutEnum.EN_ATTENTE);
    }
}
