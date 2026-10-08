package com.xeleronai.medicalimagingbackend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestToUriTemplate;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.xeleronai.medicalimagingbackend.service.DetectionAiIndisponibleException;
import com.xeleronai.medicalimagingbackend.service.PredictionIA;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class DetectionAiClientImplTest {

    private static final String BASE_URL = "http://localhost:8000";

    private MockRestServiceServer server;
    private DetectionAiClientImpl client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        client = new DetectionAiClientImpl(restClient);
    }

    @Test
    void predict_reponseValide_retourneObjetParse() {
        server.expect(requestToUriTemplate(BASE_URL + "/predict?modalite=CT&zone=THORAX"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        """
                        {"type":"BOX","detections":[
                            {"label":"nodule","bbox":{"x":1.0,"y":2.0,"w":3.0,"h":4.0},"confiance":0.9,"coupe":0}
                        ]}
                        """,
                        MediaType.APPLICATION_JSON));

        PredictionIA resultat = client.predict(List.of(new byte[] {1, 2, 3}), "ct", "THORAX");

        assertThat(resultat.type()).isEqualTo("BOX");
        assertThat(resultat.detections()).hasSize(1);
        assertThat(resultat.detections().get(0).label()).isEqualTo("nodule");
        assertThat(resultat.detections().get(0).coupe()).isEqualTo(0);

        server.verify();
    }

    @Test
    void predict_reponseErreurServeur_leveDetectionAiIndisponible() {
        server.expect(requestToUriTemplate(BASE_URL + "/predict?modalite=CT&zone=THORAX"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.predict(List.of(new byte[] {1}), "CT", "THORAX"))
                .isInstanceOf(DetectionAiIndisponibleException.class);
    }

    @Test
    void predict_reponseMasque_desserialiseMasqueBase64DepuisLeChampSnakeCase() {
        server.expect(requestToUriTemplate(BASE_URL + "/predict?modalite=CT&zone=ABDOMEN"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        """
                        {"type":"MASQUE","detections":[
                            {"label":"rate","masque_base64":"iVBORw0KG==","confiance":0.93,"coupe":42}
                        ]}
                        """,
                        MediaType.APPLICATION_JSON));

        PredictionIA resultat = client.predict(List.of(new byte[] {1, 2, 3}), "CT", "ABDOMEN");

        assertThat(resultat.type()).isEqualTo("MASQUE");
        assertThat(resultat.detections()).hasSize(1);
        assertThat(resultat.detections().get(0).masqueBase64()).isEqualTo("iVBORw0KG==");
        assertThat(resultat.detections().get(0).label()).isEqualTo("rate");
        assertThat(resultat.detections().get(0).coupe()).isEqualTo(42);
        assertThat(resultat.detections().get(0).bbox()).isNull();

        server.verify();
    }

    @Test
    void predict_modaliteEnMinuscule_envoyeeEnMajuscule() {
        server.expect(requestToUriTemplate(BASE_URL + "/predict?modalite=CT&zone=THORAX"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"type":"BOX","detections":[]}
                        """, MediaType.APPLICATION_JSON));

        client.predict(List.of(new byte[] {1}), "ct", "THORAX");

        server.verify();
    }
}
