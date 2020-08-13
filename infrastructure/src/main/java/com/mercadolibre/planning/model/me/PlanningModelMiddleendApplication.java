package com.mercadolibre.planning.model.me;

import com.mercadolibre.planning.model.me.config.EnvironmentUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PlanningModelMiddleendApplication {

    public static void main(final String[] args) {
        EnvironmentUtil.setup();
        SpringApplication.run(PlanningModelMiddleendApplication.class, args);
    }
}
