package growzapp.backend.controller.web;

import growzapp.backend.model.dto.dividendeDTO.DividendeDTO;
import growzapp.backend.repository.InvestissementRepository;
import growzapp.backend.service.DividendeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;



@Controller
@RequestMapping("/dividendes")
@RequiredArgsConstructor
public class DividendeWebController {

    private final DividendeService dividendeService;
    private final InvestissementRepository investissementRepository;

  

    @GetMapping("/{id}")
    public String show(@PathVariable Long id, Model model) {
        DividendeDTO dividende = dividendeService.getById(id);
        model.addAttribute("dividende", dividende);
        model.addAttribute("title", "Dividende #" + id);
        return "dividende/show";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("dividende", new DividendeDTO(
                null, 0.0, null, null, null, null, "", 0.0, null));
        model.addAttribute("investissements", investissementRepository.findAll());
        model.addAttribute("title", "Créer un dividende");
        return "dividende/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        DividendeDTO dividende = dividendeService.getById(id);
        model.addAttribute("dividende", dividende);
        model.addAttribute("investissements", investissementRepository.findAll());
        model.addAttribute("title", "Modifier le dividende");
        return "dividende/form";
    }

    @PostMapping(value = { "/create", "/{id}/edit" })
    public String save(
            @PathVariable(required = false) Long id,
            @ModelAttribute DividendeDTO dividendeForm) {

        // En édition : on conserve l'ID
        DividendeDTO toSave = dividendeForm;
        if (id != null) {
            DividendeDTO existing = dividendeService.getById(id);
            toSave = new DividendeDTO(
                    id,
                    dividendeForm.montantParPart(), // getXxx()
                    dividendeForm.statutDividende(),
                    dividendeForm.moyenPaiement(),
                    dividendeForm.datePaiement(),
                    dividendeForm.investissementId(),
                    dividendeForm.investissementInfo(),
                    dividendeForm.montantTotal(),
                    existing.fileName() // Conserve le fichier
            );
        }

        // Sauvegarde
        dividendeService.save(toSave);
        return "redirect:/dividendes";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        dividendeService.deleteById(id);
        return "redirect:/dividendes";
    }

  


}