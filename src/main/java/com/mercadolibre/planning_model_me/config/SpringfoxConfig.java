package com.mercadolibre.planning_model_me.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SpringfoxConfig {
	@Bean
	public Docket swaggerDocket() {
		return new Docket(DocumentationType.SWAGGER_2).select()
				.apis(RequestHandlerSelectors.basePackage("com.mercadolibre"))
				.paths(PathSelectors.any())
				.build()
				.apiInfo(this.apiInfo());
	}

	private ApiInfo apiInfo() {
		return new ApiInfoBuilder().title("planning-model-me")
				.description("You can change this description in SpringfoxConfig class")
				.contact(new Contact("candelaria campos", null, "candelaria.campos@mercadolibre.com"))
				.build();
	}

}
