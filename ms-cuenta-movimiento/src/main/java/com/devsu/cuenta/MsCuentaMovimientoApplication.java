package com.devsu.cuenta;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class MsCuentaMovimientoApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsCuentaMovimientoApplication.class, args);
	}

}
