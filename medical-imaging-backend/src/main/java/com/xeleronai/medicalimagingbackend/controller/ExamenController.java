package com.xeleronai.medicalimagingbackend.controller;

import com.xeleronai.medicalimagingbackend.dto.detection.AnalyseLanceeResponse;
import com.xeleronai.medicalimagingbackend.dto.detection.ValiderDetectionRequest;
import com.xeleronai.medicalimagingbackend.dto.examen.ExamenUploadResponse;
import com.xeleronai.medicalimagingbackend.dto.examen.UploadStandardRequest;
import com.xeleronai.medicalimagingbackend.entity.Utilisateur;
import com.xeleronai.medicalimagingbackend.security.SecurityUtils;
import com.xeleronai.medicalimagingbackend.service.AnalyseEnCoursException;
import com.xeleronai.medicalimagingbackend.service.AnalyseNonLancableException;
import com.xeleronai.medicalimagingbackend.service.DetectionAnalyseService;
import com.xeleronai.medicalimagingbackend.service.DetectionQueryService;
import com.xeleronai.medicalimagingbackend.service.DetectionValidationService;
import com.xeleronai.medicalimagingbackend.service.ExamenQueryService;
import com.xeleronai.medicalimagingbackend.service.ExamenUploadService;
import com.xeleronai.medicalimagingbackend.service.LectureImpossibleException;
import com.xeleronai.medicalimagingbackend.service.RessourceIntrouvableException;
import com.xeleronai.medicalimagingbackend.service.StatutDetectionInvalideException;
import com.xeleronai.medicalimagingbackend.service.UploadEchoueException;
import com.xeleronai.medicalimagingbackend.service.UploadValidationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/examens")
@RequiredArgsConstructor
@Validated
@Tag(name = "Examens", description = "Upload et consultation des examens")
public class ExamenController {

    private final ExamenUploadService examenUploadService;
    private final ExamenQueryService examenQueryService;
    private final DetectionQueryService detectionQueryService;
    private final DetectionAnalyseService detectionAnalyseService;
    private final DetectionValidationService detectionValidationService;
    private final SecurityUtils securityUtils;

    @PostMapping(value = "/upload/standard", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('RADIOLOGUE', 'TECHNICIEN')")
    @Operation(summary = "Upload d'un lot PNG/JPEG avec métadonnées en formulaire")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Examen créé"),
        @ApiResponse(responseCode = "400", description = "Batch invalide"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "403", description = "Rôle non autorisé"),
        @ApiResponse(responseCode = "503", description = "Échec de stockage")
    })
    public ResponseEntity<?> uploadStandard(@Valid @ModelAttribute UploadStandardRequest request) {
        Utilisateur utilisateurCourant = securityUtils.getUtilisateurCourant();
        ExamenUploadResponse response = examenUploadService.uploadStandard(request, utilisateurCourant);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/upload/dicom", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('RADIOLOGUE', 'TECHNICIEN')")
    @Operation(summary = "Upload d'un lot DICOM, métadonnées extraites automatiquement")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Examen créé"),
        @ApiResponse(responseCode = "400", description = "Batch invalide"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "403", description = "Rôle non autorisé"),
        @ApiResponse(responseCode = "503", description = "Échec de stockage")
    })
    public ResponseEntity<?> uploadDicom(@RequestParam("files") List<MultipartFile> files) {
        Utilisateur utilisateurCourant = securityUtils.getUtilisateurCourant();
        ExamenUploadResponse response = examenUploadService.uploadDicom(files, utilisateurCourant);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('RADIOLOGUE', 'TECHNICIEN', 'ADMIN')")
    @Operation(summary = "Liste paginée des examens")
    public ResponseEntity<?> lister(
            @RequestParam(required = false) String mrn,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        Utilisateur utilisateurCourant = securityUtils.getUtilisateurCourant();
        return ResponseEntity.ok(examenQueryService.lister(mrn, page, size, utilisateurCourant));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RADIOLOGUE', 'TECHNICIEN', 'ADMIN')")
    @Operation(summary = "Détail d'un examen et de ses images")
    public ResponseEntity<?> detail(@PathVariable Long id) {
        return ResponseEntity.ok(examenQueryService.detail(id));
    }

    @GetMapping("/{examenId}/detections")
    @PreAuthorize("hasAnyRole('RADIOLOGUE', 'TECHNICIEN', 'ADMIN')")
    @Operation(summary = "Liste des détections IA d'un examen (résultats bruts, non validés)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste des détections (vide si aucune)"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "403", description = "Rôle non autorisé"),
        @ApiResponse(responseCode = "404", description = "Examen introuvable")
    })
    public ResponseEntity<?> detections(@PathVariable Long examenId) {
        return ResponseEntity.ok(detectionQueryService.listerParExamen(examenId));
    }

    @PostMapping("/{examenId}/detections/analyser")
    @PreAuthorize("hasAnyRole('RADIOLOGUE', 'TECHNICIEN')")
    @Operation(summary = "Lance l'analyse IA d'un examen (asynchrone, ne bloque pas)")
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Analyse lancée, en cours"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "403", description = "Rôle non autorisé"),
        @ApiResponse(responseCode = "404", description = "Examen introuvable"),
        @ApiResponse(responseCode = "409", description = "Analyse déjà en cours"),
        @ApiResponse(responseCode = "422", description = "Modalité/zone manquante ou aucune image")
    })
    public ResponseEntity<?> analyser(@PathVariable Long examenId) {
        Utilisateur utilisateurCourant = securityUtils.getUtilisateurCourant();
        detectionAnalyseService.lancerAnalyse(examenId, utilisateurCourant);
        detectionAnalyseService.analyserEnArrierePlan(examenId, utilisateurCourant.getId());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(AnalyseLanceeResponse.builder().examenId(examenId).statut("EN_COURS").build());
    }

    @GetMapping("/{examenId}/detections/statut")
    @PreAuthorize("hasAnyRole('RADIOLOGUE', 'TECHNICIEN', 'ADMIN')")
    @Operation(summary = "Statut de l'analyse IA en cours ou terminée (pour polling)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Statut courant"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "403", description = "Rôle non autorisé"),
        @ApiResponse(responseCode = "404", description = "Examen introuvable")
    })
    public ResponseEntity<?> statutAnalyse(@PathVariable Long examenId) {
        return ResponseEntity.ok(detectionQueryService.statutAnalyse(examenId));
    }

    @PatchMapping("/{examenId}/detections/{detectionId}/statut")
    @PreAuthorize("hasRole('RADIOLOGUE')")
    @Operation(summary = "Valide ou rejette une détection IA (acte clinique réservé au radiologue)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Détection mise à jour"),
        @ApiResponse(responseCode = "400", description = "Statut invalide (attendu ACCEPTEE ou REJETEE)"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "403", description = "Rôle non autorisé"),
        @ApiResponse(responseCode = "404", description = "Détection introuvable")
    })
    public ResponseEntity<?> validerStatutDetection(
            @PathVariable Long examenId,
            @PathVariable Long detectionId,
            @Valid @RequestBody ValiderDetectionRequest request) {
        Utilisateur utilisateurCourant = securityUtils.getUtilisateurCourant();
        return ResponseEntity.ok(detectionValidationService.validerStatut(
                examenId, detectionId, request.getStatut(), utilisateurCourant));
    }

    @GetMapping("/{examenId}/images/{imageId}/apercu")
    @PreAuthorize("hasAnyRole('RADIOLOGUE', 'TECHNICIEN', 'ADMIN')")
    @Operation(summary = "Aperçu PNG d'une image")
    public ResponseEntity<byte[]> apercu(@PathVariable Long examenId, @PathVariable Long imageId) {
        Utilisateur utilisateurCourant = securityUtils.getUtilisateurCourant();
        byte[] contenu = examenQueryService.lireApercu(examenId, imageId, utilisateurCourant);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(contenu);
    }

    @GetMapping("/{examenId}/detections/{detectionId}/masque")
    @PreAuthorize("hasAnyRole('RADIOLOGUE', 'TECHNICIEN', 'ADMIN')")
    @Operation(summary = "PNG du masque de segmentation d'une détection")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Masque PNG"),
        @ApiResponse(responseCode = "401", description = "Non authentifié"),
        @ApiResponse(responseCode = "403", description = "Rôle non autorisé"),
        @ApiResponse(responseCode = "404", description = "Détection introuvable ou sans masque"),
        @ApiResponse(responseCode = "503", description = "Fichier temporairement indisponible")
    })
    public ResponseEntity<byte[]> masque(@PathVariable Long examenId, @PathVariable Long detectionId) {
        Utilisateur utilisateurCourant = securityUtils.getUtilisateurCourant();
        byte[] contenu = detectionQueryService.lireMasque(examenId, detectionId, utilisateurCourant);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(contenu);
    }

    @ExceptionHandler(UploadValidationException.class)
    public ResponseEntity<String> handleUploadValidation(UploadValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(UploadEchoueException.class)
    public ResponseEntity<String> handleUploadEchoue(UploadEchoueException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Échec du stockage, réessayez plus tard.");
    }

    @ExceptionHandler(RessourceIntrouvableException.class)
    public ResponseEntity<String> handleRessourceIntrouvable(RessourceIntrouvableException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(LectureImpossibleException.class)
    public ResponseEntity<String> handleLectureImpossible(LectureImpossibleException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Fichier temporairement indisponible, réessayez plus tard.");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<String> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(AnalyseEnCoursException.class)
    public ResponseEntity<String> handleAnalyseEnCours(AnalyseEnCoursException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @ExceptionHandler(AnalyseNonLancableException.class)
    public ResponseEntity<String> handleAnalyseNonLancable(AnalyseNonLancableException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(ex.getMessage());
    }

    @ExceptionHandler(StatutDetectionInvalideException.class)
    public ResponseEntity<String> handleStatutDetectionInvalide(StatutDetectionInvalideException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
