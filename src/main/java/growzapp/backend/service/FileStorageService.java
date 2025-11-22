package growzapp.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private Path getUploadPath() {
        Path path = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(path);
        } catch (Exception e) {
            throw new RuntimeException("Impossible de créer le dossier d'upload : " + uploadDir, e);
        }
        return path;
    }

    /**
     * Sauvegarde un fichier (byte[] ou MultipartFile)
     */
    public String save(byte[] content, String originalFilename, String contentType) throws IOException {
        String filename = UUID.randomUUID() + "_" + StringUtils.cleanPath(originalFilename);
        Path target = getUploadPath().resolve(filename);
        Files.write(target, content);
        return "/files/download/" + filename;
    }

    public String save(MultipartFile file) throws IOException {
        String filename = UUID.randomUUID() + "_" + StringUtils.cleanPath(file.getOriginalFilename());
        Path target = getUploadPath().resolve(filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return "/files/download/" + filename;
    }

    /**
     * Récupère un fichier pour téléchargement
     */
    public Path load(String filename) {
        return getUploadPath().resolve(filename).normalize();
    }
}
