package tn.jobnest.gformation.model;

import java.sql.Date;

public class Formation {
    private int id_formation, id_user, duree_heures, nb_places, nb_places_occupees;
    private double prix;
    private String titre, description, objectifs, niveau, statut, lieu, url_image, nomFormateur;
    private Date date_debut, date_fin;

    // --- CONSTRUCTEURS ---

    public Formation() {}

    public Formation(int id_formation, int id_user, String titre, String description, String objectifs,
                     String niveau, int duree_heures, double prix, int nb_places, int nb_places_occupees,
                     Date date_debut, Date date_fin, String statut, String lieu, String url_image, String nomFormateur) {
        this.id_formation = id_formation;
        this.id_user = id_user;
        this.titre = titre;
        this.description = description;
        this.objectifs = objectifs;
        this.niveau = niveau;
        this.duree_heures = duree_heures;
        this.prix = prix;
        this.nb_places = nb_places;
        this.nb_places_occupees = nb_places_occupees;
        this.date_debut = date_debut;
        this.date_fin = date_fin;
        this.statut = statut;
        this.lieu = lieu;
        this.url_image = url_image;
        this.nomFormateur = nomFormateur;
    }

    // --- GETTERS & SETTERS (Synchronisés avec les Controllers) ---

    public int getId_formation() { return id_formation; }
    public void setId_formation(int id_formation) { this.id_formation = id_formation; }

    public int getId_user() { return id_user; }
    public void setId_user(int id_user) { this.id_user = id_user; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getObjectifs() { return objectifs; }
    public void setObjectifs(String objectifs) { this.objectifs = objectifs; }

    public String getNiveau() { return niveau; }
    public void setNiveau(String niveau) { this.niveau = niveau; }

    public int getDuree_heures() { return duree_heures; }
    public void setDuree_heures(int duree_heures) { this.duree_heures = duree_heures; }

    public double getPrix() { return prix; }
    public void setPrix(double prix) { this.prix = prix; }

    public int getNb_places() { return nb_places; }
    public void setNb_places(int nb_places) { this.nb_places = nb_places; }

    public int getNb_places_occupees() { return nb_places_occupees; }
    public void setNb_places_occupees(int nb_places_occupees) { this.nb_places_occupees = nb_places_occupees; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getLieu() { return lieu; }
    public void setLieu(String lieu) { this.lieu = lieu; }

    public String getUrl_image() { return url_image; }
    public void setUrl_image(String url_image) { this.url_image = url_image; }

    public String getNomFormateur() { return nomFormateur; }
    public void setNomFormateur(String nomFormateur) { this.nomFormateur = nomFormateur; }

    // --- GESTION DES DATES (Double Getters pour éviter les erreurs) ---

    public Date getDate_debut() { return date_debut; }
    public void setDate_debut(Date date_debut) { this.date_debut = date_debut; }

    public Date getDate_fin() { return date_fin; }
    public void setDate_fin(Date date_fin) { this.date_fin = date_fin; }

    /** Alias pour les contrôleurs qui appellent getDateDebut au lieu de getDate_debut */
    public Date getDateDebut() { return date_debut; }

    /** Alias pour les contrôleurs qui appellent getDateFin au lieu de getDate_fin */
    public Date getDateFin() { return date_fin; }

    // --- MÉTHODES UTILES ---

    @Override
    public String toString() {
        return "Formation{" + "titre='" + titre + '\'' + ", formateur='" + nomFormateur + '\'' + '}';
    }
}