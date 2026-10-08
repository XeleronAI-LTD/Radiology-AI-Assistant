package com.xeleronai.medicalimagingbackend.entity;

import com.xeleronai.medicalimagingbackend.entity.enums.DetectionStatutEnum;
import com.xeleronai.medicalimagingbackend.entity.enums.DetectionTypeEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Résultat d'une détection IA sur une coupe. En v1 seul le type BOX est
 * produit (par ai-detection-service, Sprint 2) ; les champs MASQUE sont déjà
 * portés par le schéma mais non alimentés (voir ai-detection-service/CLAUDE.md).
 */
@Entity
@Table(name = "detection")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Detection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id", nullable = false)
    private Image image;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private DetectionTypeEnum type;

    @Column(name = "anomalie", length = 100)
    private String anomalie;

    @Column(name = "confiance", nullable = false)
    private Double confiance;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    @Builder.Default
    private DetectionStatutEnum statut = DetectionStatutEnum.EN_ATTENTE;

    @Column(name = "coupe")
    private Integer coupe;

    // Champs BOX (renseignés si type = BOX).
    @Column(name = "x")
    private Integer x;

    @Column(name = "y")
    private Integer y;

    @Column(name = "largeur")
    private Integer largeur;

    @Column(name = "hauteur")
    private Integer hauteur;

    // Champ MASQUE (renseigné si type = MASQUE) : clé MinIO du masque.
    @Column(name = "chemin_masque", length = 255)
    private String cheminMasque;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validateur_id")
    private Utilisateur validateur;

    @Column(name = "valide_le")
    private LocalDateTime valideLe;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (statut == null) {
            statut = DetectionStatutEnum.EN_ATTENTE;
        }
    }
}
