package ke.co.chamaledger.chamalegder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableScheduling
public class ChamalegderApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChamalegderApplication.class, args);
    }

    // Add this Bean so your MpesaService can use it to talk to Safaricom
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
