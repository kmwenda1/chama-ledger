package ke.co.chamaledger.chamalegder.mpesa.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "mpesa")
@Data
public class MpesaConfig {
    private String consumerKey;
    private String consumerSecret;
    private String authUrl;
    private String stkPushUrl;
    private String shortcode;
    private String passkey;
    private String callbackUrl;
}