package growzapp.backend.controller.web;

import growzapp.backend.model.dto.userDTO.UserDTO;
import growzapp.backend.model.enumeration.Sexe;
import growzapp.backend.service.LocaliteService;
import growzapp.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserWebController {

    private final UserService userService;
    private final LocaliteService localiteService;

    // LISTE
    @GetMapping
    public String index(Model model) {
       // model.addAttribute("users", userService.getAll());
        model.addAttribute("title", "Liste des utilisateurs");
        return "user/index"; // → user/index.html
    }

    // DÉTAIL
    @GetMapping("/{id}")
    public String show(@PathVariable Long id, Model model) {
       // UserDTO user = userService.getById(id);
        //model.addAttribute("user", user);
       // model.addAttribute("title", user.getPrenom() + " " + user.getNom());
        return "user/show"; // → user/show.html
    }

    // FORMULAIRE CRÉATION
    @GetMapping("/create")
    public String createForm(Model model) {
        UserDTO user = new UserDTO();
        user.setSexe(Sexe.M);
        model.addAttribute("user", user);
        model.addAttribute("localites", localiteService.getAll());
        model.addAttribute("title", "Créer un utilisateur");
        return "user/form"; // → user/form.html
    }

    // FORMULAIRE ÉDITION
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
       // UserDTO user = userService.getById(id);
       // model.addAttribute("user", user);
        model.addAttribute("localites", localiteService.getAll());
       // model.addAttribute("title", "Modifier " + user.getPrenom() + " " + user.getNom());
        return "user/form"; // même vue
    }

    // SAUVEGARDE
   // @PostMapping(value = {"/create", "/{id}/edit"})
   /*  public String save(
            @PathVariable(required = false) Long id,
            @ModelAttribute("user") UserDTO userForm,
            RedirectAttributes ra) 

        try {
           // UserDTO saved = id != null 
              //  ? userService.update(id, userForm)
               // : userService.save(userForm);
            
            ra.addFlashAttribute("successMessage", 
                id != null ? "Utilisateur mis à jour !" : "Utilisateur créé !");
           // return "redirect:/users/" + saved.getId();
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Erreur : " + e.getMessage());
            return id != null 
                ? "redirect:/users/" + id + "/edit"
                : "redirect:/users/create";
        }
    } */

    // SUPPRESSION
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
           // userService.deleteById(id);
            ra.addFlashAttribute("successMessage", "Utilisateur supprimé.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Impossible de supprimer.");
        }
        return "redirect:/users";
    }

}