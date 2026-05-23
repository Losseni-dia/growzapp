package growzapp.backend.module.employe.service;

import growzapp.backend.module.employe.model.Employe;
import growzapp.backend.module.employe.model.EmployeFonction;
import growzapp.backend.module.employe.model.EmployeFonctionId;
import growzapp.backend.module.employe.model.Fonction;
import growzapp.backend.module.employe.repository.EmployeRepository;
import growzapp.backend.module.employe.repository.FonctionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeService {

    private final EmployeRepository employeRepository;
    private final FonctionRepository fonctionRepository;
    private final FonctionService fonctionService;

    @Transactional
    public Employe save(Employe employe) {
        if (employe.getFonctions() != null && !employe.getFonctions().isEmpty()) {
            List<EmployeFonction> fonctionsToSave = new ArrayList<>();

            for (EmployeFonction ef : employe.getFonctions()) {
                Fonction fonction = fonctionRepository.findByNomIgnoreCase(ef.getFonction().getNom())
                        .orElseGet(() -> {
                            Fonction newF = new Fonction();
                            newF.setNom(ef.getFonction().getNom());
                            return fonctionRepository.save(newF);
                        });

                EmployeFonction savedEF = new EmployeFonction();
                savedEF.setEmploye(employe);
                savedEF.setFonction(fonction);
                savedEF.setDatePriseFonction(
                        ef.getDatePriseFonction() != null ? ef.getDatePriseFonction() : LocalDate.now());

                fonctionsToSave.add(savedEF);
            }

            employe.getFonctions().clear();
            employe.getFonctions().addAll(fonctionsToSave);
        }
        return employeRepository.save(employe);
    }

    public List<Employe> getAll() {
        return employeRepository.findAll();
    }

    public Employe getById(Long id) {
        return employeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé"));
    }

    public void deleteById(long id) {
        employeRepository.deleteById(id);
    }

    @Transactional
    public void addFonctionToEmploye(Long employeId, Long fonctionId, LocalDate datePriseFonction) {
        Employe employe = getById(employeId);
        Fonction fonction = fonctionService.getById(fonctionId);

        boolean exists = employe.getFonctions().stream()
                .anyMatch(ef -> ef.getFonction().getId().equals(fonctionId));
        if (exists) {
            throw new IllegalArgumentException("Cette fonction est déjà attribuée à cet employé.");
        }

        EmployeFonctionId id = new EmployeFonctionId(employeId, fonctionId);

        EmployeFonction ef = new EmployeFonction();
        ef.setId(id);
        ef.setEmploye(employe);
        ef.setFonction(fonction);
        ef.setDatePriseFonction(datePriseFonction);

        employe.getFonctions().add(ef);
        employeRepository.save(employe);
    }

    public long count() {
        return employeRepository.count();
    }
}
