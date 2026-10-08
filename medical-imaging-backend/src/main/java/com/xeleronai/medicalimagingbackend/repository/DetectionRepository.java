package com.xeleronai.medicalimagingbackend.repository;

import com.xeleronai.medicalimagingbackend.entity.Detection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DetectionRepository extends JpaRepository<Detection, Long> {

    @Query("SELECT d FROM Detection d JOIN FETCH d.image i WHERE i.examen.id = :examenId ORDER BY i.ordre ASC, d.id ASC")
    List<Detection> findByExamenId(@Param("examenId") Long examenId);

    @Query("SELECT d FROM Detection d JOIN FETCH d.image i WHERE d.id = :detectionId AND i.examen.id = :examenId")
    Optional<Detection> findByIdAndExamenId(@Param("detectionId") Long detectionId, @Param("examenId") Long examenId);
}
