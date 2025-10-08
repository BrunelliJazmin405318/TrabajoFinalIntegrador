package ar.edu.utn.tfi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "ar.edu.utn.tfi")
public class TfiApplication {

	public static void main(String[] args) {
		SpringApplication.run(TfiApplication.class, args);
	}

}
