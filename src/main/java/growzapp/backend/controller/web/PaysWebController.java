package growzapp.backend.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import growzapp.backend.model.dto.paysDTO.PaysDTO;
import growzapp.backend.service.PaysService;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/pays")
@RequiredArgsConstructor
public class PaysWebController {

    private final PaysService paysService;

    // === LISTE ===
    @GetMapping
    public String list(Model model) {
        model.addAttribute("paysList", paysService.getAll());
        model.addAttribute("title", "Liste des pays");
        return "pays/index";
    }

    // === SHOW ===
    @GetMapping("/{id}")
    public String show(@PathVariable Long id, Model model) {
        PaysDTO pays = paysService.getById(id);
        model.addAttribute("pays", pays);
        model.addAttribute("title", pays.nom());
        return "pays/show";
    }

    // === CRÉER ===
    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("pays", new PaysDTO(null, "", null));
        model.addAttribute("title", "Créer un pays");
        return "pays/form";
    }

    // === ÉDITER ===
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        PaysDTO pays = paysService.getById(id);
        model.addAttribute("pays", pays);
        model.addAttribute("title", "Modifier " + pays.nom());
        return "pays/form";
    }

    // === SAUVEGARDER ===
    @PostMapping(value = { "/create", "/{id}/edit" })
    public String save(@PathVariable(required = false) Long id,
            @ModelAttribute PaysDTO pays) {
        if (id != null) {
            pays = new PaysDTO(id, pays.nom(), pays.localites());
        }
        paysService.save(pays);
        return "redirect:/pays";
    }

    // === SUPPRIMER ===
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        paysService.deleteById(id);
        return "redirect:/pays";
    }
}
