package com.mercadolibre.planning.model.me.utils;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadolibre.json.JsonUtils;
import com.mercadolibre.json_jackson.JsonJackson;
import com.mercadolibre.restclient.MockResponse;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;

import static com.mercadolibre.restclient.http.ContentType.APPLICATION_JSON;
import static com.mercadolibre.restclient.http.ContentType.HEADER_NAME;
import static com.mercadolibre.restclient.http.HttpMethod.POST;

public class TestUtils {
    public static final String WAREHOUSE_ID = "ARTW01";

    public static ObjectMapper objectMapper() {
        return ((JsonJackson) JsonUtils.INSTANCE.getEngine())
                .getMapper()
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
    }

    public static void mockPostUrlSuccess(final String url, final JSONObject request) {
        MockResponse.builder()
                .withMethod(POST)
                .withURL(url)
                .withRequestBody(request.toString())
                .withStatusCode(HttpStatus.OK.value())
                .withResponseHeader(HEADER_NAME, APPLICATION_JSON.toString())
                .build();
    }

}
