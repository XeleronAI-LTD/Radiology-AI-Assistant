package com.xeleronai.medicalimagingbackend.service;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    /**
     * Stocke un fichier uploadé sous la clé donnée.
     * @throws UploadEchoueException si le stockage échoue.
     */
    void uploader(String cle, MultipartFile fichier);

    /**
     * Stocke un contenu généré en mémoire (ex. aperçu PNG) sous la clé donnée.
     * @throws UploadEchoueException si le stockage échoue.
     */
    void uploader(String cle, byte[] contenu, String contentType);

    /**
     * Supprime les objets aux clés données, en compensation d'un batch qui a
     * échoué. Best-effort : chaque suppression est indépendante, un échec est
     * loggé (sans PHI) mais ne lève jamais d'exception — ne doit jamais
     * masquer l'erreur d'origine remontée à l'appelant.
     */
    void supprimerSilencieux(List<String> cles);

    /**
     * Lit le contenu d'un objet MinIO.
     * @throws LectureImpossibleException si l'objet est absent ou MinIO injoignable.
     */
    byte[] lire(String cle);
}
