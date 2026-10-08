package com.xeleronai.medicalimagingbackend.config;

import com.xeleronai.medicalimagingbackend.entity.Institution;
import com.xeleronai.medicalimagingbackend.entity.Utilisateur;
import com.xeleronai.medicalimagingbackend.entity.enums.RoleUtilisateur;
import com.xeleronai.medicalimagingbackend.repository.InstitutionRepository;
import com.xeleronai.medicalimagingbackend.repository.UtilisateurRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final String SEED_EMAIL = "radio@test.com";
    private static final String SEED_PASSWORD = "azerty123";

    private final UtilisateurRepository utilisateurRepository;
    private final InstitutionRepository institutionRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(
            UtilisateurRepository utilisateurRepository,
            InstitutionRepository institutionRepository,
            PasswordEncoder passwordEncoder) {
        this.utilisateurRepository = utilisateurRepository;
        this.institutionRepository = institutionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (utilisateurRepository.findByEmail(SEED_EMAIL).isPresent()) {
            return;
        }

        Institution institutionDefaut = institutionRepository.findByCode(Institution.CODE_DEFAUT)
                .orElseThrow(() -> new IllegalStateException(
                        "Institution par défaut introuvable — migration V12 non appliquée ?"));

        Utilisateur utilisateur = Utilisateur.builder()
                .email(SEED_EMAIL)
                .motDePasseHash(passwordEncoder.encode(SEED_PASSWORD))
                .role(RoleUtilisateur.RADIOLOGUE)
                .institution(institutionDefaut)
                .build();

        utilisateurRepository.save(utilisateur);
    }
}
