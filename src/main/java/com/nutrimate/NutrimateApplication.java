package com.nutrimate;

import com.nutrimate.config.DotEnvConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NutrimateApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(NutrimateApplication.class);
		app.addInitializers(new DotEnvConfig());
		app.run(args);
	}

}
