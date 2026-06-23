package com.buscam2.Buscam2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Buscam2Application {

	public static void main(String[] args) {

		SpringApplication.run(Buscam2Application.class, args);
		System.out.println("""
                
                  BusCam API démarrée avec succès !
                  http://localhost:8080/api/v1
                  Swagger : http://localhost:8080/swagger-ui.html
                ================================================
                """);
	}

}
