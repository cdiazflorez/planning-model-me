package com.mercadolibre.planning_model_me;

import com.mercadolibre.planning_model_me.config.SpringConfig;
import com.mercadolibre.planning_model_me.util.ScopeUtils;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class Application {
	public static void main(String[] args) {
		ScopeUtils.calculateScopeSuffix();
		new SpringApplicationBuilder(SpringConfig.class).registerShutdownHook(true)
				.run(args);
	}
}
