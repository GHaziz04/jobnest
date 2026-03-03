package tn.jobnest.gentretien.model;

import javafx.beans.property.*;

public class Competence {

    // ================= PROPERTIES =================
    private final IntegerProperty idCompetence = new SimpleIntegerProperty();
    private final StringProperty nom = new SimpleStringProperty();
    private final StringProperty categorie = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();
    private final StringProperty niveauRequis = new SimpleStringProperty();

    // 🔐 OTP Secret (persisté en DB)
    private String otpSecret;

    // ✅ Pour CheckBoxListCell
    private final BooleanProperty selected = new SimpleBooleanProperty(false);

    // ================= CONSTRUCTORS =================

    // 🔹 Constructor complet (lecture DB)
    public Competence(int id, String nom, String categorie,
                      String description, String niveauRequis,
                      String otpSecret) {

        this.idCompetence.set(id);
        this.nom.set(nom);
        this.categorie.set(categorie);
        this.description.set(description);
        this.niveauRequis.set(niveauRequis);
        this.otpSecret = otpSecret;
    }

    // 🔹 Constructor sans secret
    public Competence(int id, String nom, String categorie,
                      String description, String niveauRequis) {

        this(id, nom, categorie, description, niveauRequis, null);
    }

    // 🔹 Constructor ajout avec secret
    public Competence(String nom, String categorie,
                      String description, String niveauRequis,
                      String otpSecret) {

        this(0, nom, categorie, description, niveauRequis, otpSecret);
    }
    public Competence() {
    }

    // 🔹 Constructor ajout sans secret
    public Competence(String nom, String categorie,
                      String description, String niveauRequis) {

        this(0, nom, categorie, description, niveauRequis, null);
    }

    // ================= GETTERS =================

    public int getIdCompetence() {
        return idCompetence.get();
    }

    public String getNom() {
        return nom.get();
    }

    public String getCategorie() {
        return categorie.get();
    }

    public String getDescription() {
        return description.get();
    }

    public String getNiveauRequis() {
        return niveauRequis.get();
    }

    public boolean isSelected() {
        return selected.get();
    }

    public String getOtpSecret() {
        return otpSecret;
    }

    // ================= SETTERS =================

    public void setIdCompetence(int id) {
        this.idCompetence.set(id);
    }

    public void setNom(String nom) {
        this.nom.set(nom);
    }

    public void setCategorie(String categorie) {
        this.categorie.set(categorie);
    }

    public void setDescription(String description) {
        this.description.set(description);
    }

    public void setNiveauRequis(String niveauRequis) {
        this.niveauRequis.set(niveauRequis);
    }

    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }

    public void setOtpSecret(String otpSecret) {
        this.otpSecret = otpSecret;
    }

    // ================= PROPERTY METHODS =================

    public IntegerProperty idCompetenceProperty() {
        return idCompetence;
    }

    public StringProperty nomProperty() {
        return nom;
    }

    public StringProperty categorieProperty() {
        return categorie;
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    public StringProperty niveauRequisProperty() {
        return niveauRequis;
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }

    // ================= UTILS =================

    @Override
    public String toString() {
        return getNom() + " (" + getCategorie() + ")";
    }
}

