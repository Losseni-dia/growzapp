package growzapp.backend.module.kyc.VoveIdConfig;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class VoveIdConfig {

    @Value("${vove.api-key}")
    private String apiKey;

    @Value("${vove.base-url}")
    private String baseUrl;

    @Bean
    public WebClient voveIdWebClient() {
        return WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("Content-Type", "application/json")
            .defaultHeader("x-api-key", apiKey)
            .build();
    }
}