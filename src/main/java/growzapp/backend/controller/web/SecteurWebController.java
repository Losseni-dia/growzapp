package growzapp.backend.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import growzapp.backend.model.dto.secteurDTO.SecteurDTO;
import growzapp.backend.service.SecteurService;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("web/secteurs")
@RequiredArgsConstructor
public class SecteurWebController {

    private final SecteurService secteurService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("secteurs", secteurService.getAll());
        model.addAttribute("title", "Liste des secteurs");
        return "secteur/index";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("secteur", new SecteurDTO(null, "", null));
        model.addAttribute("title", "Créer un secteur");
        return "secteur/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        SecteurDTO secteur = secteurService.getById(id);
        model.addAttribute("secteur", secteur);
        model.addAttribute("title", "Modifier " + secteur.nom());
        return "secteur/form";
    }

    @PostMapping(value = { "/create", "/{id}/edit" })
    public String save(@PathVariable(required = false) Long id,
            @ModelAttribute SecteurDTO secteur) {
        if (id != null) {
            secteur = new SecteurDTO(id, secteur.nom(), secteur.projets());
        }
        secteurService.save(secteur);
        return "redirect:/secteurs";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        secteurService.deleteById(id);
        return "redirect:/secteurs";
    }
}