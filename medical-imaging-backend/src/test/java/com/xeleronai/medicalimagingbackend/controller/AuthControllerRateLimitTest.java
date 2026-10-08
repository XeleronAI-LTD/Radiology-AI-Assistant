package com.xeleronai.medicalimagingbackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Vérifie le rate-limiting sur POST /api/v1/auth/login (voir
 * LoginRateLimitFilter) : 5 tentatives/minute/IP, la 6e renvoie 429, et deux
 * IP différentes ont des compteurs indépendants.
 *
 * Les identifiants utilisés sont volontairement invalides : le filtre
 * s'applique avant même que l'authentification échoue (401), donc le
 * comportement testé ici ne dépend pas d'un utilisateur existant en base.
 * Chaque test utilise une IP dédiée (via X-Forwarded-For) pour ne pas
 * interférer avec les autres tests partageant le même filtre en mémoire.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerRateLimitTest {

    private static final String LOGIN_BODY =
            "{\"email\":\"nobody@example.com\",\"password\":\"wrong-password\"}";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void sixiemeTentative_dansLaMemeMinute_retourne429() throws Exception {
        String ip = "203.0.113.11";

        for (int i = 1; i <= 5; i++) {
            MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                            .header("X-Forwarded-For", ip)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(LOGIN_BODY))
                    .andReturn();
            assertThat(result.getResponse().getStatus())
                    .as("tentative %d ne doit pas être limitée", i)
                    .isNotEqualTo(429);
        }

        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Forwarded-For", ip)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void deuxIpDifferentes_ontDesCompteursIndependants() throws Exception {
        String ipA = "203.0.113.21";
        String ipB = "203.0.113.22";

        // Épuise le quota de l'IP A.
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .header("X-Forwarded-For", ipA)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(LOGIN_BODY));
        }
        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Forwarded-For", ipA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isTooManyRequests());

        // L'IP B n'a pas encore été utilisée : elle ne doit pas être bloquée.
        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Forwarded-For", ipB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(429));
    }
}
