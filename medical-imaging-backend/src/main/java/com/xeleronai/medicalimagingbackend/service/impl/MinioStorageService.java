package com.xeleronai.medicalimagingbackend.service.impl;

import com.xeleronai.medicalimagingbackend.service.LectureImpossibleException;
import com.xeleronai.medicalimagingbackend.service.StorageService;
import com.xeleronai.medicalimagingbackend.service.UploadEchoueException;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioStorageService implements StorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    @PostConstruct
    public void assurerBucketExiste() {
        try {
            boolean existe = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!existe) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Bucket MinIO créé : {}", bucket);
            }
        } catch (Exception e) {
            log.error("Impossible de vérifier/créer le bucket MinIO '{}' au démarrage", bucket, e);
        }
    }

    @Override
    public void uploader(String cle, MultipartFile fichier) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(cle)
                    .stream(fichier.getInputStream(), fichier.getSize(), -1)
                    .contentType(fichier.getContentType())
                    .build());
        } catch (Exception e) {
            throw new UploadEchoueException("Échec du stockage MinIO pour la clé " + cle, e);
        }
    }

    @Override
    public void uploader(String cle, byte[] contenu, String contentType) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(cle)
                    .stream(new ByteArrayInputStream(contenu), contenu.length, -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            throw new UploadEchoueException("Échec du stockage MinIO pour la clé " + cle, e);
        }
    }

    @Override
    public byte[] lire(String cle) {
        try (GetObjectResponse reponse = minioClient.getObject(
                GetObjectArgs.builder().bucket(bucket).object(cle).build())) {
            return reponse.readAllBytes();
        } catch (Exception e) {
            throw new LectureImpossibleException("Échec de lecture MinIO pour la clé " + cle, e);
        }
    }

    @Override
    public void supprimerSilencieux(List<String> cles) {
        for (String cle : cles) {
            try {
                minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(cle).build());
            } catch (Exception e) {
                log.error("Échec de la suppression de compensation pour la clé MinIO : {}", cle, e);
            }
        }
    }
}
