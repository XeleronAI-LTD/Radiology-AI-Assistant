package com.xeleronai.medicalimagingbackend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xeleronai.medicalimagingbackend.entity.AuditLog;
import com.xeleronai.medicalimagingbackend.entity.Examen;
import com.xeleronai.medicalimagingbackend.entity.Institution;
import com.xeleronai.medicalimagingbackend.entity.Patient;
import com.xeleronai.medicalimagingbackend.entity.Utilisateur;
import com.xeleronai.medicalimagingbackend.entity.enums.FormatImageEnum;
import com.xeleronai.medicalimagingbackend.entity.enums.RoleUtilisateur;
import com.xeleronai.medicalimagingbackend.repository.AuditLogRepository;
import com.xeleronai.medicalimagingbackend.repository.ExamenRepository;
import com.xeleronai.medicalimagingbackend.service.ImagePreparee;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExamenPersistenceServiceImplTest {

    @Mock
    private ExamenRepository examenRepository;
    @Mock
    private AuditLogRepository auditLogRepository;

    private ExamenPersistenceServiceImpl service;

    private Institution institution;
    private Utilisateur utilisateurCourant;
    private Patient patient;

    @BeforeEach
    void setUp() {
        service = new ExamenPersistenceServiceImpl(examenRepository, auditLogRepository);

        institution = Institution.builder().id(7L).nom("Institution par défaut").code("DEFAULT").build();
        utilisateurCourant = Utilisateur.builder()
                .id(1L).email("radio@test.com").role(RoleUtilisateur.RADIOLOGUE).institution(institution).build();
        patient = Patient.builder().id(1L).institution(institution).mrn("MRN-1").build();
    }

    @Test
    void persisterExamen_poseInstitutionDeUtilisateurCourant_surExamenEtAuditLog() {
        when(examenRepository.save(any())).thenAnswer(invocation -> {
            Examen examen = invocation.getArgument(0);
            examen.setId(42L);
            return examen;
        });

        List<ImagePreparee> images = List.of(new ImagePreparee("a.png", "a.png", FormatImageEnum.PNG, 0));

        service.persisterExamen(
                patient, utilisateurCourant, "Radio", LocalDate.now(), "CR", null, images);

        ArgumentCaptor<Examen> examenCaptor = ArgumentCaptor.forClass(Examen.class);
        verify(examenRepository).save(examenCaptor.capture());
        assertThat(examenCaptor.getValue().getInstitution()).isEqualTo(institution);

        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditLogCaptor.capture());
        assertThat(auditLogCaptor.getValue().getInstitution()).isEqualTo(institution);
        assertThat(auditLogCaptor.getValue().getAction()).isEqualTo("UPLOAD_EXAMEN");
    }
}
