package growzapp.backend.controller.web;

import growzapp.backend.model.dto.localisationDTO.LocalisationDTO;
import growzapp.backend.service.LocalisationService;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/localisations")
@RequiredArgsConstructor
public class LocalisationWebController {

    private final LocalisationService localisationService;


    // === LISTE ===
    @GetMapping
    public String list(Model model) {
        List<LocalisationDTO> localisations = localisationService.getAll();
        model.addAttribute("localisations", localisations);
        model.addAttribute("title", "Liste des sites");
        return "localisation/index";
    }

    // === SHOW ===
    @GetMapping("/{id}")
    public String show(@PathVariable Long id, Model model) {
        LocalisationDTO localisation = localisationService.getById(id);
        model.addAttribute("localisation", localisation);
        model.addAttribute("title", localisation.nom());
        return "localisation/show";
    }

    // === CRÉER ===
    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("localisation", new LocalisationDTO(null, null, null, null, null, null, null, null, null, null));
        model.addAttribute("title", "Créer un site");
        return "localisation/form";
    }


    // === ÉDITER ===
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        LocalisationDTO localisation = localisationService.getById(id);
        model.addAttribute("localisation", localisation);
        model.addAttribute("title", "Modifier " + localisation.nom());
        return "localisation/form";
    }

    // === SAUVEGARDER ===
    @PostMapping(value = { "/create", "/{id}/edit" })
    public String save(@PathVariable(required = false) Long id,
            @ModelAttribute LocalisationDTO localisation) {
        if (id != null) {
            localisation = new LocalisationDTO(
                    id,
                    localisation.nom(),
                    localisation.adresse(),
                    localisation.contact(),
                    localisation.responsable(),
                    localisation.createdAt(),
                    localisation.localiteNom(),
                    localisation.localiteId(),
                    localisation.paysNom(),
                    localisation.projets());
        }
        localisationService.save(localisation);
        return "redirect:/localisations";
    }

    // === SUPPRIMER ===
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        localisationService.deleteById(id);
        return "redirect:/localisations";
    }
}