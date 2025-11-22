package growzapp.backend.controller.web;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import growzapp.backend.model.dto.localisationDTO.LocalisationDTO;
import growzapp.backend.model.dto.localiteDTO.LocaliteDTO;
import growzapp.backend.service.LocaliteService;

import java.util.List;

@Controller
@RequestMapping("web/localites")
@RequiredArgsConstructor
public class LocaliteWebController {

    private final LocaliteService localiteService;

    // === LISTE ===
    @GetMapping
    public String list(Model model) {
        List<LocaliteDTO> localites = localiteService.getAll();
        model.addAttribute("localites", localites);
        model.addAttribute("title", "Liste des localités");
        return "localite/index";
    }

    // === SHOW ===
    @GetMapping("/{id}")
    public String show(@PathVariable Long id, Model model) {
        LocaliteDTO localite = localiteService.getById(id);
        model.addAttribute("localite", localite);
        model.addAttribute("title", localite.nom() + " (" + localite.codePostal() + ")");
        return "localite/show";
    }

    // === CRÉER ===
    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("localite", new LocaliteDTO(null, "", "", "", null, null));
        model.addAttribute("title", "Créer une localité");
        return "localite/form";
    }

    // === ÉDITER ===
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        LocaliteDTO localite = localiteService.getById(id);
        model.addAttribute("localite", localite);
        model.addAttribute("title", "Modifier " + localite.nom());
        return "localite/form";
    }

    // === SAUVEGARDER ===
    @PostMapping(value = { "/create", "/{id}/edit" })
    public String save(@PathVariable(required = false) Long id,
            @ModelAttribute LocaliteDTO localite) {
        if (id != null) {
            localite = new LocaliteDTO(
                    id,
                    localite.codePostal(),
                    localite.nom(),
                    localite.paysNom(),
                    localite.users(),
                    localite.localisations());
        }
        localiteService.save(localite);
        return "redirect:/localites";
    }

    // === SUPPRIMER ===
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        localiteService.deleteById(id);
        return "redirect:/localites";
    }

    @GetMapping("/{id}/create-localisation")
public String createLocalisationForm(@PathVariable Long id, Model model) {
    LocaliteDTO localite = localiteService.getById(id);

    LocalisationDTO dto = new LocalisationDTO(id, null, null, null, null, null, null, id, null, null);

    model.addAttribute("localisation", dto);
    model.addAttribute("title", "Créer une localisation à " + localite.nom());
    return "localisation/form";
}
}