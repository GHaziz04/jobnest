package tn.jobnest.gentretien.model;

import javafx.beans.property.*;

public class Experience {

    // ================= PROPERTIES =================

    private final IntegerProperty idExperience;
    private final StringProperty nom;
    private final StringProperty categorie;
    private final StringProperty description;
    private final StringProperty niveauRequis;

    // 🔥 CHECKBOX PROPERTY
    private final BooleanProperty selected = new SimpleBooleanProperty(false);

    // ================= CONSTRUCTORS =================

    public Experience() {
        this.idExperience = new SimpleIntegerProperty();
        this.nom = new SimpleStringProperty();
        this.categorie = new SimpleStringProperty();
        this.description = new SimpleStringProperty();
        this.niveauRequis = new SimpleStringProperty();
    }

    public Experience(int id, String nom, String categorie,
                      String description, String niveauRequis) {
        this();
        this.idExperience.set(id);
        this.nom.set(nom);
        this.categorie.set(categorie);
        this.description.set(description);
        this.niveauRequis.set(niveauRequis);
    }

    // ================= GETTERS =================

    public int getIdExperience() {
        return idExperience.get();
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

    // 🔥 CHECKBOX GETTER
    public boolean isSelected() {
        return selected.get();
    }

    // ================= SETTERS =================

    public void setIdExperience(int idExperience) {
        this.idExperience.set(idExperience);
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

    // 🔥 CHECKBOX SETTER
    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }

    // ================= PROPERTY METHODS =================

    public IntegerProperty idExperienceProperty() {
        return idExperience;
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

    // 🔥 IMPORTANT POUR CheckBoxListCell
    public BooleanProperty selectedProperty() {
        return selected;
    }

    // ================= DEBUG =================

    @Override
    public String toString() {
        return nom.get() + " (" + categorie.get() + ")";
    }
}
