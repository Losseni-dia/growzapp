package growzapp.backend.model.dto.projetDTO;

public record ProjetCreateDTO(
        String libelle,
        String description,
        String secteurNom,
        String localiteNom,
        String paysNom
// → poster supprimé ici ! On ne l’envoie plus dans le JSON
) {
}