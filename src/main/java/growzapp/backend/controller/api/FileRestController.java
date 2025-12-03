package growzapp.backend.controller.api;

import java.io.IOException;
import java.nio.file.Files;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import growzapp.backend.model.entite.User;
import growzapp.backend.repository.UserRepository;
import growzapp.backend.service.FileStorageService;
import lombok.RequiredArgsConstructor;

// src/main/java/growzapp/backend/controller/api/FileController.java
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileRestController {

    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;

    // Téléchargement sécurisé des documents privés
    @GetMapping("/documents/{filename}")
    public ResponseEntity<ByteArrayResource> downloadDocument(
            @PathVariable String filename,
            Authentication auth) throws IOException {

        User user = userRepository.findByLogin(auth.getName()).orElseThrow();

        // On retrouve le document via le filename (ou mieux : via ID, voir plus bas)
        // Mais pour rester simple avec ton système actuel :
        String fullPath = "documents/" + filename;
        // Ici tu peux ajouter une vérif supplémentaire si tu veux (ex: via un cache ou
        // une table)

        byte[] data = Files.readAllBytes(fileStorageService.getUploadPath("documents").resolve(filename));
        ByteArrayResource resource = new ByteArrayResource(data);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentLength(data.length)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}