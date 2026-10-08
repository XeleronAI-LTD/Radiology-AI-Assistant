package com.xeleronai.medicalimagingbackend.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Vérifie l'application de @PreAuthorize sur ExamenController (voir
 * @EnableMethodSecurity ajoutée à SecurityConfig pour ce ticket — sans elle,
 * ces deux tests échoueraient silencieusement en laissant passer ADMIN).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ExamenControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void sansAuthentification_retourne401() throws Exception {
        mockMvc.perform(post("/api/v1/examens/upload/dicom"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void roleAdmin_retourne403() throws Exception {
        // Requête multipart valide avec un fichier bidon : il faut que le
        // binding de @RequestParam("files") réussisse pour que l'exécution
        // atteigne @PreAuthorize (le binding d'arguments a lieu avant que le
        // proxy AOP de @PreAuthorize ne s'exécute) — sinon Spring MVC
        // répondrait 415/400 avant même d'atteindre le contrôle de rôle.
        MockMultipartFile fichier = new MockMultipartFile("files", "a.dcm", "application/dicom", new byte[] {1});
        mockMvc.perform(multipart("/api/v1/examens/upload/dicom")
                        .file(fichier)
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void listerExamens_sansAuthentification_retourne401() throws Exception {
        mockMvc.perform(get("/api/v1/examens"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listerExamens_roleAdmin_estAutorise() throws Exception {
        // Contrairement à l'upload, ADMIN a accès en lecture (voir specify.md).
        // lister() résout désormais l'Utilisateur authentifié via SecurityUtils
        // (pour filtrer par institution) : on réutilise le compte seedé
        // radio@test.com (le seul qui existe réellement en base) avec le rôle
        // ADMIN mocké — @PreAuthorize évalue ce rôle mocké, indépendamment du
        // rôle RADIOLOGUE réellement stocké sur ce compte.
        mockMvc.perform(get("/api/v1/examens")
                        .with(user("radio@test.com").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void listerExamens_sizeInvalide_retourne400() throws Exception {
        mockMvc.perform(get("/api/v1/examens?size=0")
                        .with(user("radio@test.com").roles("RADIOLOGUE")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listerExamens_pageNegative_retourne400() throws Exception {
        mockMvc.perform(get("/api/v1/examens?page=-1")
                        .with(user("radio@test.com").roles("RADIOLOGUE")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void detailExamen_idInexistant_retourne404() throws Exception {
        mockMvc.perform(get("/api/v1/examens/999999999")
                        .with(user("radio@test.com").roles("RADIOLOGUE")))
                .andExpect(status().isNotFound());
    }

    @Test
    void analyser_sansAuthentification_retourne401() throws Exception {
        mockMvc.perform(post("/api/v1/examens/1/detections/analyser"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void analyser_roleAdmin_retourne403() throws Exception {
        mockMvc.perform(post("/api/v1/examens/1/detections/analyser")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void analyser_idInexistant_retourne404() throws Exception {
        mockMvc.perform(post("/api/v1/examens/999999999/detections/analyser")
                        .with(user("radio@test.com").roles("RADIOLOGUE")))
                .andExpect(status().isNotFound());
    }

    @Test
    void statutAnalyse_sansAuthentification_retourne401() throws Exception {
        mockMvc.perform(get("/api/v1/examens/1/detections/statut"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void statutAnalyse_roleTechnicien_idInexistant_retourne404() throws Exception {
        // Le rôle doit être accepté (pas de 403) avant que l'examen introuvable
        // ne produise un 404 — vérifie l'ordre autorisation puis résolution.
        mockMvc.perform(get("/api/v1/examens/999999999/detections/statut")
                        .with(user("technicien@test.com").roles("TECHNICIEN")))
                .andExpect(status().isNotFound());
    }

    @Test
    void masque_sansAuthentification_retourne401() throws Exception {
        mockMvc.perform(get("/api/v1/examens/1/detections/1/masque"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void masque_detectionInexistante_retourne404() throws Exception {
        mockMvc.perform(get("/api/v1/examens/1/detections/999999999/masque")
                        .with(user("radio@test.com").roles("RADIOLOGUE")))
                .andExpect(status().isNotFound());
    }

    @Test
    void validerStatutDetection_sansAuthentification_retourne401() throws Exception {
        mockMvc.perform(patch("/api/v1/examens/1/detections/1/statut")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statut\":\"ACCEPTEE\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validerStatutDetection_roleTechnicien_retourne403() throws Exception {
        mockMvc.perform(patch("/api/v1/examens/1/detections/1/statut")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statut\":\"ACCEPTEE\"}")
                        .with(user("technicien@test.com").roles("TECHNICIEN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void validerStatutDetection_roleAdmin_retourne403() throws Exception {
        mockMvc.perform(patch("/api/v1/examens/1/detections/1/statut")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statut\":\"ACCEPTEE\"}")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void validerStatutDetection_roleRadiologue_detectionInexistante_retourne404() throws Exception {
        mockMvc.perform(patch("/api/v1/examens/1/detections/999999999/statut")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statut\":\"ACCEPTEE\"}")
                        .with(user("radio@test.com").roles("RADIOLOGUE")))
                .andExpect(status().isNotFound());
    }

    @Test
    void validerStatutDetection_roleRadiologue_statutInvalide_retourne400() throws Exception {
        mockMvc.perform(patch("/api/v1/examens/1/detections/1/statut")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statut\":\"EN_ATTENTE\"}")
                        .with(user("radio@test.com").roles("RADIOLOGUE")))
                .andExpect(status().isBadRequest());
    }
}
