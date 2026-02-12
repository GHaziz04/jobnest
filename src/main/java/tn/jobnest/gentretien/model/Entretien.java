package tn.jobnest.gentretien.model;



import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

public class Entretien {
    private int idEntretien;
    private Date dateEntretien;
    private Time heureDebut;
    private Time heureFin;
    private String typeEntretien;
    private String lieu;
    private String lienVisio;
    private String statut;
    private String noteRecruteur;
    private Timestamp dateCreation;
    private int idRecruteur;
    private int idOffre;

    public Entretien() {
    }

    public int getIdEntretien() {
        return idEntretien;
    }

    public void setIdEntretien(int idEntretien) {
        this.idEntretien = idEntretien;
    }

    public Date getDateEntretien() {
        return dateEntretien;
    }

    public void setDateEntretien(Date dateEntretien) {
        this.dateEntretien = dateEntretien;
    }

    public Time getHeureDebut() {
        return heureDebut;
    }

    public void setHeureDebut(Time heureDebut) {
        this.heureDebut = heureDebut;
    }

    public Time getHeureFin() {
        return heureFin;
    }

    public void setHeureFin(Time heureFin) {
        this.heureFin = heureFin;
    }

    public String getTypeEntretien() {
        return typeEntretien;
    }

    public void setTypeEntretien(String typeEntretien) {
        this.typeEntretien = typeEntretien;
    }

    public String getLieu() {
        return lieu;
    }

    public void setLieu(String lieu) {
        this.lieu = lieu;
    }

    public String getLienVisio() {
        return lienVisio;
    }

    public void setLienVisio(String lienVisio) {
        this.lienVisio = lienVisio;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getNoteRecruteur() {
        return noteRecruteur;
    }

    public void setNoteRecruteur(String noteRecruteur) {
        this.noteRecruteur = noteRecruteur;
    }

    public Timestamp getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(Timestamp dateCreation) {
        this.dateCreation = dateCreation;
    }

    public int getIdRecruteur() {
        return idRecruteur;
    }

    public void setIdRecruteur(int idRecruteur) {
        this.idRecruteur = idRecruteur;
    }

    public int getIdOffre() {
        return idOffre;
    }

    public void setIdOffre(int idOffre) {
        this.idOffre = idOffre;
    }
}
