package growzapp.backend.model.enumeration;

public enum Sexe {
    M("Masculin"), F("Feminin"), X("x");

    private String sexe;

    Sexe(String sexe) {
        this.sexe = sexe;
    }

    public String getSexe() {
        return sexe;
    }

}
