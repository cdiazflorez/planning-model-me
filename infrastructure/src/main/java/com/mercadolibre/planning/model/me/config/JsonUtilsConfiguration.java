package com.mercadolibre.planning.model.me.config;

import com.mercadolibre.json.JsonUtils;
import com.mercadolibre.json_jackson.JsonJackson;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class JsonUtilsConfiguration implements ApplicationRunner {
    @Override
    public void run(final ApplicationArguments args) {
        final JsonJackson engine = (JsonJackson) JsonUtils.INSTANCE.getEngine();
        engine.getMapper()
                .findAndRegisterModules();
    }
}
