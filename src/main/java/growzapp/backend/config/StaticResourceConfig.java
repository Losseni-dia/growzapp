// src/main/java/growzapp/backend/config/StaticResourceConfig.java

package growzapp.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Chemin ABSOLU + création du dossier s'il n'existe pas
        String uploadDir = System.getProperty("user.dir") + "/uploads/posters";
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs(); // ← CRÉE LE DOSSIER AU DÉMARRAGE
            System.out.println("Dossier créé : " + uploadDir);
        }

        registry.addResourceHandler("/uploads/posters/**")
                .addResourceLocations("file:" + uploadDir + "/")
                .setCachePeriod(3600);

        System.out.println("Serving posters from: file:" + uploadDir + "/");
    }
}