package growzapp.backend.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    // Personnalise l'ObjectMapper auto-configuré par Spring Boot au lieu de le
    // remplacer, pour conserver ses modules par défaut (JavaTimeModule, etc.)
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer hibernateJacksonCustomizer() {
        return builder -> {
            Hibernate6Module hibernateModule = new Hibernate6Module();
            hibernateModule.disable(Hibernate6Module.Feature.USE_TRANSIENT_ANNOTATION);
            hibernateModule.configure(Hibernate6Module.Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS, true);
            hibernateModule.configure(Hibernate6Module.Feature.FORCE_LAZY_LOADING, false);
            hibernateModule.configure(Hibernate6Module.Feature.REPLACE_PERSISTENT_COLLECTIONS, true);

            builder.modulesToInstall(hibernateModule);
            builder.mixIn(Object.class, IgnoreHibernatePropertiesMixin.class);
            builder.failOnUnknownProperties(false);
            builder.serializationInclusion(JsonInclude.Include.NON_NULL);
            builder.featuresToDisable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        };
    }
}