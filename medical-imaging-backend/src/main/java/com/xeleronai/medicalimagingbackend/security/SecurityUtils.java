package com.xeleronai.medicalimagingbackend.security;

import com.xeleronai.medicalimagingbackend.entity.Institution;
import com.xeleronai.medicalimagingbackend.entity.Utilisateur;
import com.xeleronai.medicalimagingbackend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Résout l'{@link Utilisateur} authentifié à partir du contexte de sécurité.
 * JwtAuthFilter pose l'email comme principal (authentication.getName()) ;
 * ce helper relit l'entité complète en base pour les cas où le controller a
 * besoin de l'utilisateur (ex. creePar, AuditLog.utilisateur).
 */
@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UtilisateurRepository utilisateurRepository;

    public Utilisateur getUtilisateurCourant() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Utilisateur authentifié introuvable en base"));
    }

    public Institution getInstitutionCourante() {
        return getUtilisateurCourant().getInstitution();
    }
}
