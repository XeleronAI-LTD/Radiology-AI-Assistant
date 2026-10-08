package com.xeleronai.medicalimagingbackend.repository;

import com.xeleronai.medicalimagingbackend.entity.Image;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageRepository extends JpaRepository<Image, Long> {

    Optional<Image> findByIdAndExamenId(Long id, Long examenId);

    /**
     * Charge les images d'un examen déjà triées par ordre croissant, sans
     * passer par la collection lazy Examen.images — nécessaire pour la
     * lire depuis un thread @Async où aucune session Hibernate liée à la
     * requête HTTP n'est active (voir DetectionAnalyseServiceImpl).
     */
    List<Image> findByExamenIdOrderByOrdreAsc(Long examenId);
}
