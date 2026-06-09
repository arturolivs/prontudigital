package com.prontudigital.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ProntudigitalApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProntudigitalApplication.class, args);
	}

}
