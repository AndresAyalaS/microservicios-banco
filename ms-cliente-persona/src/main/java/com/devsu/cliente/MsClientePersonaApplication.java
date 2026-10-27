package com.devsu.cliente;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.devsu.cliente")
public class MsClientePersonaApplication {
	public static void main(String[] args) {
		SpringApplication.run(MsClientePersonaApplication.class, args);
	}
}