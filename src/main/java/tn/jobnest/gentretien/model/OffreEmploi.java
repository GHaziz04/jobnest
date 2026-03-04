package tn.jobnest.gentretien.model;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;

public class OffreEmploi {

    private int idOffre;
    private int idRecruteur;
    private String titre;
    private String description;
    private String entreprise;
    private String typeContrat;
    private double salaireMin;
    private double salaireMax;
    private String niveauExperience;
    private int nbPostes;
    private Date datePublication;
    private Date dateExpiration;
    private String statut;
    private int nbVues;
    private int nbCandidatures;
    private Timestamp dateCreation;
    private Timestamp dateModification;
    // ✅ matchingScore supprimé

    private List<Competence> competences;
    private List<Experience> experiences;

    public OffreEmploi() {
        this.competences = new ArrayList<>();
        this.experiences = new ArrayList<>();
    }

    public boolean isOpen() {
        if (dateExpiration == null) return false;
        return dateExpiration.toLocalDate().isAfter(LocalDate.now());
    }

    public int getIdOffre() { return idOffre; }
    public void setIdOffre(int idOffre) { this.idOffre = idOffre; }

    public int getIdRecruteur() { return idRecruteur; }
    public void setIdRecruteur(int idRecruteur) { this.idRecruteur = idRecruteur; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getEntreprise() { return entreprise; }
    public void setEntreprise(String entreprise) { this.entreprise = entreprise; }

    public String getTypeContrat() { return typeContrat; }
    public void setTypeContrat(String typeContrat) { this.typeContrat = typeContrat; }

    public double getSalaireMin() { return salaireMin; }
    public void setSalaireMin(double salaireMin) { this.salaireMin = salaireMin; }

    public double getSalaireMax() { return salaireMax; }
    public void setSalaireMax(double salaireMax) { this.salaireMax = salaireMax; }

    public String getNiveauExperience() { return niveauExperience; }
    public void setNiveauExperience(String niveauExperience) { this.niveauExperience = niveauExperience; }

    public int getNbPostes() { return nbPostes; }
    public void setNbPostes(int nbPostes) { this.nbPostes = nbPostes; }

    public Date getDatePublication() { return datePublication; }
    public void setDatePublication(Date datePublication) { this.datePublication = datePublication; }

    public Date getDateExpiration() { return dateExpiration; }
    public void setDateExpiration(Date dateExpiration) { this.dateExpiration = dateExpiration; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public int getNbVues() { return nbVues; }
    public void setNbVues(int nbVues) { this.nbVues = nbVues; }

    public int getNbCandidatures() { return nbCandidatures; }
    public void setNbCandidatures(int nbCandidatures) { this.nbCandidatures = nbCandidatures; }

    public Timestamp getDateCreation() { return dateCreation; }
    public void setDateCreation(Timestamp dateCreation) { this.dateCreation = dateCreation; }

    public Timestamp getDateModification() { return dateModification; }
    public void setDateModification(Timestamp dateModification) { this.dateModification = dateModification; }

    public List<Competence> getCompetences() { return competences; }
    public void setCompetences(List<Competence> competences) { this.competences = competences; }

    public List<Experience> getExperiences() { return experiences; }
    public void setExperiences(List<Experience> experiences) { this.experiences = experiences; }
}