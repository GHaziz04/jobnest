package tn.jobnest.gentretien.model;

import java.sql.Timestamp; // N'oubliez pas cet import !

public class Document {
    private int idDocument;
    private int idCandidature;
    private String nomDocument;
    private String typeDocument;
    private String cheminFichier;
    private Timestamp dateUpload; // <--- Nouveau champ

    public Document(int idDocument, int idCandidature, String nomDocument, String typeDocument, String cheminFichier, Timestamp dateUpload) {
        this.idDocument = idDocument;
        this.idCandidature = idCandidature;
        this.nomDocument = nomDocument;
        this.typeDocument = typeDocument;
        this.cheminFichier = cheminFichier;
        this.dateUpload = dateUpload; // <--- Ajout ici
    }

    // Getters
    public String getNomDocument() { return nomDocument; }
    public String getTypeDocument() { return typeDocument; }
    public String getCheminFichier() { return cheminFichier; }
    public Timestamp getDateUpload() { return dateUpload; } // <--- Nouveau Getter
}