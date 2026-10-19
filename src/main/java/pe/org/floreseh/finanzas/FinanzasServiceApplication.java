package pe.org.floreseh.finanzas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FinanzasServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(FinanzasServiceApplication.class, args);
	}

}
