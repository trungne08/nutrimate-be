package com.nutrimate;

import com.nutrimate.config.DotEnvConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NutrimateApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(NutrimateApplication.class);
		app.addInitializers(new DotEnvConfig());
		app.run(args);
	}

}
