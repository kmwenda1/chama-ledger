package ke.co.chamaledger.chamalegder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ChamalegderApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChamalegderApplication.class, args);
    }
}