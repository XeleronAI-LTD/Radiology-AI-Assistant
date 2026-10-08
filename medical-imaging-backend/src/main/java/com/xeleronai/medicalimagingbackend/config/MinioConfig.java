package com.xeleronai.medicalimagingbackend.config;

import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import io.minio.SetBucketEncryptionArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.SseConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class MinioConfig {

    @Bean
    public MinioClient minioClient(
            @Value("${minio.endpoint}") String endpoint,
            @Value("${minio.access-key}") String accessKey,
            @Value("${minio.secret-key}") String secretKey) {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    /**
     * Active le chiffrement au repos (SSE-S3) sur le bucket applicatif, de façon
     * transparente pour le code d'upload existant (aucun changement requis côté
     * StorageService/MinioStorageService : le chiffrement est géré au niveau du
     * bucket, pas de l'objet).
     *
     * <p>Exécuté via un {@link CommandLineRunner} plutôt qu'un {@code @PostConstruct}
     * sur cette classe : les CommandLineRunner s'exécutent une fois que tous les
     * beans (et leurs {@code @PostConstruct}) sont initialisés, donc après
     * {@code MinioStorageService#assurerBucketExiste()} qui crée le bucket s'il
     * n'existe pas encore. On est ainsi sûr que le bucket existe déjà quand on
     * tente d'y appliquer la config de chiffrement.
     *
     * <p>Idempotent : redéfinir la même règle SSE-S3 à chaque démarrage est un
     * no-op côté MinIO, donc rejouable sans risque à chaque redémarrage.
     */
    @Bean
    public CommandLineRunner minioBucketEncryptionInitializer(
            MinioClient minioClient, @Value("${minio.bucket}") String bucket) {
        return args -> activerChiffrementBucket(minioClient, bucket);
    }

    private void activerChiffrementBucket(MinioClient minioClient, String bucket) {
        try {
            boolean bucketExiste = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!bucketExiste) {
                log.warn("Bucket MinIO '{}' introuvable au démarrage : chiffrement SSE-S3 non configuré "
                        + "(il sera appliqué au prochain redémarrage une fois le bucket créé).", bucket);
                return;
            }

            minioClient.setBucketEncryption(SetBucketEncryptionArgs.builder()
                    .bucket(bucket)
                    .config(SseConfiguration.newConfigWithSseS3Rule())
                    .build());
            log.info("Chiffrement au repos SSE-S3 activé sur le bucket MinIO '{}'.", bucket);
        } catch (ErrorResponseException e) {
            if ("NotImplemented".equalsIgnoreCase(e.errorResponse().code())) {
                log.warn("Chiffrement SSE-S3 non activé : MINIO_KMS_SECRET_KEY absent côté MinIO. "
                        + "Les fichiers seront stockés en clair. Configurez le KMS avant la mise en production.");
            } else {
                log.warn("Impossible d'activer le chiffrement SSE-S3 sur le bucket MinIO '{}' (code={}) : {}",
                        bucket, e.errorResponse().code(), e.getMessage());
            }
        } catch (Exception e) {
            // Ne jamais faire planter le démarrage de l'application pour un problème
            // de configuration du chiffrement au repos : on dégrade en clair + warning.
            log.warn("Impossible d'activer le chiffrement SSE-S3 sur le bucket MinIO '{}' : {}",
                    bucket, e.getMessage());
        }
    }
}
