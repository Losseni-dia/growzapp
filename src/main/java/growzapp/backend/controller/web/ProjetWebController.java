package growzapp.backend.controller.web;

import growzapp.backend.model.dto.projetDTO.ProjetDTO;
import growzapp.backend.service.LocalisationService;
import growzapp.backend.service.LocaliteService;
import growzapp.backend.service.ProjetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
@RequestMapping("/projets")
@RequiredArgsConstructor
public class ProjetWebController {

    private final ProjetService projetService;
    private final LocalisationService localisationService;
    private final LocaliteService localiteService;

    @GetMapping
    public String list(Model model) {
        System.out.println("PROJETS CONTROLLER CHARGÉ !");
        List<ProjetDTO> projets = projetService.getAll();
        model.addAttribute("projets", projets);
        model.addAttribute("title", "Liste des projets");
        return "projet/index";
    }
    @GetMapping("/{id}")
    public String show(@PathVariable Long id, Model model) {
        ProjetDTO projet = projetService.getById(id);
        model.addAttribute("projet", projet);
        model.addAttribute("title", projet.libelle());
        return "projet/show";
    }

    // === FORMULAIRE CRÉATION ===
    @GetMapping("/create")
    public String createForm(Model model) {
        ProjetDTO dto = new ProjetDTO(null, null, null,
                null, null, 0, 0,
                0, 0, 0, 0, 0, null, null, 0, null, null,
                null, null, null, null,
                 null, null, null, null, null, null, null, null);
        model.addAttribute("projet", dto);
        model.addAttribute("localites", localiteService.getAll());
        model.addAttribute("sites", localisationService.getAll());
        model.addAttribute("title", "Créer un projet");
        return "projet/form";
    }

    // === SAUVEGARDE (create + edit) ===
    @PostMapping(value = { "/create", "/{id}/edit" })
    public String save(
            @PathVariable(required = false) Long id,
            @ModelAttribute("projet") ProjetDTO projetForm,
            @RequestParam(value = "files", required = false) MultipartFile[] files) {

        ProjetDTO dto = projetForm;

        if (id != null) {
            ProjetDTO existing = projetService.getById(id);
            dto = new ProjetDTO(
                    id,
                    projetForm.poster(),
                    projetForm.reference(),
                    projetForm.libelle(),
                    projetForm.description(),
                    projetForm.valuation(),
                    projetForm.roiProjete(),
                    projetForm.partsDisponible(),
                    projetForm.partsPrises(),
                    projetForm.prixUnePart(),
                    projetForm.objectifFinancement(),
                    projetForm.montantCollecte(),
                    projetForm.dateDebut(),
                    projetForm.dateFin(),
                    projetForm.valeurTotalePartsEnPourcent(),
                    projetForm.statutProjet(),
                    existing.createdAt(), // conserve createdAt
                    projetForm.localiteId(),
                    projetForm.porteurId(),
                    projetForm.siteId(),
                    projetForm.secteurId(),
                    projetForm.paysId(),
                    projetForm.paysNom(),
                    projetForm.localiteNom(),
                    projetForm.porteurNom(),
                    projetForm.siteNom(),
                    projetForm.secteurNom(),
                    existing.documents(), // conserve les documents existants
                    existing.investissements() // conserve les investissements
            );
        }

        projetService.save(dto, files != null ? files : new MultipartFile[0]);
        return "redirect:/projets";
    }

    // === FORMULAIRE ÉDITION ===
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        ProjetDTO projet = projetService.getById(id);
        model.addAttribute("projet", projet);
        model.addAttribute("localites", localiteService.getAll());
        model.addAttribute("sites", localisationService.getAll());
        model.addAttribute("title", "Modifier " + projet.libelle());
        return "projet/form";
    }

    // === SUPPRESSION ===
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        projetService.deleteById(id);
        return "redirect:/projets";
    }

    @GetMapping("/{id}/investissements")
    public String showInvestissements(
            @PathVariable Long id,
            Model model) {

        ProjetDTO projet = projetService.getProjetWithInvestissements(id);
        model.addAttribute("projet", projet);

        return "projet/investissements";
    }
}