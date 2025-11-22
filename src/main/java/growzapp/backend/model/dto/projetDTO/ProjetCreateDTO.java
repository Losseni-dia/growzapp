package growzapp.backend.model.dto.projetDTO;

public record ProjetCreateDTO(
                String libelle,
                String description,
                String secteurNom, // ← ON ENVOIE LE NOM, PAS L'ID
                String localiteNom, // ← ON ENVOIE LE NOM, PAS L'ID
                String paysNom, // ← Optionnel, pour la localité si elle n'existe pas
                String poster) {
}