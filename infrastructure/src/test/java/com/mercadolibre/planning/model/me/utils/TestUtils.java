package com.mercadolibre.planning.model.me.utils;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadolibre.json.JsonUtils;
import com.mercadolibre.json_jackson.JsonJackson;
import com.mercadolibre.restclient.MockResponse;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static com.mercadolibre.restclient.http.ContentType.APPLICATION_JSON;
import static com.mercadolibre.restclient.http.ContentType.HEADER_NAME;
import static com.mercadolibre.restclient.http.HttpMethod.POST;

public class TestUtils {
    public static final String WAREHOUSE_ID = "ARTW01";
    public static final Long USER_ID = 1234L;
    public static final ZonedDateTime A_DATE = ZonedDateTime.of(2020, 8, 19, 17, 40, 0, 0,
            ZoneId.of("UTC"));

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

    public static String getResourceAsString(final String resourceName) throws IOException {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        final InputStream resource = classLoader.getResourceAsStream(resourceName);

        try {
            return IOUtils.toString(resource, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            resource.close();
        }
    }

}
