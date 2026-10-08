package com.xeleronai.medicalimagingbackend.repository;

import com.xeleronai.medicalimagingbackend.entity.Institution;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstitutionRepository extends JpaRepository<Institution, Long> {

    Optional<Institution> findByCode(String code);
}
