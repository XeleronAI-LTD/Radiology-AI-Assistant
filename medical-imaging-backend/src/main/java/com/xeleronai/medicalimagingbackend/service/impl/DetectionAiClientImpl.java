package com.xeleronai.medicalimagingbackend.service.impl;

import com.xeleronai.medicalimagingbackend.service.DetectionAiClient;
import com.xeleronai.medicalimagingbackend.service.DetectionAiIndisponibleException;
import com.xeleronai.medicalimagingbackend.service.PredictionIA;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
@RequiredArgsConstructor
@Slf4j
public class DetectionAiClientImpl implements DetectionAiClient {

    private final RestClient aiDetectionRestClient;

    @Value("${ai.detection.seuil:#{null}}")
    private Double seuil;

    @Override
    public PredictionIA predict(List<byte[]> fichiersDicomOrdonnes, String modalite, String zone) {
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        int index = 0;
        for (byte[] fichier : fichiersDicomOrdonnes) {
            bodyBuilder.part("files", new ByteArrayResource(fichier)).filename("slice" + index + ".dcm");
            index++;
        }
        MultiValueMap<String, HttpEntity<?>> body = bodyBuilder.build();

        try {
            return aiDetectionRestClient.post()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/predict")
                                .queryParam("modalite", modalite.toUpperCase())
                                .queryParam("zone", zone);
                        if (seuil != null) {
                            uriBuilder.queryParam("seuil", seuil);
                        }
                        return uriBuilder.build();
                    })
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(PredictionIA.class);
        } catch (RestClientException e) {
            throw new DetectionAiIndisponibleException(
                    "Échec de l'appel à ai-detection-service /predict", e);
        }
    }
}
