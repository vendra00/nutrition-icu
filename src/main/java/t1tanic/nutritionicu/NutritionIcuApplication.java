package t1tanic.nutritionicu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class NutritionIcuApplication {
    static void main(String[] args) {
        SpringApplication.run(NutritionIcuApplication.class, args);
    }
}
