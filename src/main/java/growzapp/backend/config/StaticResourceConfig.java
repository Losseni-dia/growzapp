// src/main/java/growzapp/backend/config/StaticResourceConfig.java
// VERSION FINALE – 07 DÉCEMBRE 2025
// Tous les dossiers uploads sont servis + création automatique au démarrage

package growzapp.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    private static final String BASE_UPLOAD_DIR = System.getProperty("user.dir") + "/uploads";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Liste complète des sous-dossiers utilisés dans l'application
        String[] subfolders = {
                "posters",
                "contrats",
                "factures",
                "avatars",
                "documents" // ← ajouté comme demandé
        };

        for (String folder : subfolders) {
            String absolutePath = BASE_UPLOAD_DIR + "/" + folder;
            File directory = new File(absolutePath);

            // Création du dossier s'il n'existe pas
            if (!directory.exists()) {
                boolean created = directory.mkdirs();
                System.out.println(created
                        ? "Dossier créé : " + absolutePath
                        : "Échec création dossier : " + absolutePath);
            }

            // Exposition des fichiers statiques
            registry.addResourceHandler("/uploads/" + folder + "/**")
                    .addResourceLocations("file:" + absolutePath + "/")
                    .setCachePeriod(3600); // cache 1 heure

            System.out.println("Serving /uploads/" + folder + "/** → file:" + absolutePath + "/");
        }
    }
}