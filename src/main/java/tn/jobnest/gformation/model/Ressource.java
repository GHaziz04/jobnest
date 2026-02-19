package tn.jobnest.gformation.model;

/**
 * Modèle représentant une ressource pédagogique (Texte, PDF, Vidéo, ou Quiz)
 */
public class Ressource {
    private int id_ressource;
    private int id_module;
    private String titre;
    private String type; // TEXTE, VIDEO, PDF, QUIZ
    private String valeur_contenu; // URL ou "DATABASE" pour les Quiz
    private int ordre;

    // Constructeur complet pour la lecture
    public Ressource(int id_ressource, int id_module, String titre, String type, String valeur_contenu, int ordre) {
        this.id_ressource = id_ressource;
        this.id_module = id_module;
        this.titre = titre;
        this.type = type;
        this.valeur_contenu = valeur_contenu;
        this.ordre = ordre;
    }

    // Constructeur pour l'insertion (sans ID)
    public Ressource(int id_module, String titre, String type, String valeur_contenu, int ordre) {
        this.id_module = id_module;
        this.titre = titre;
        this.type = type;
        this.valeur_contenu = valeur_contenu;
        this.ordre = ordre;
    }

    public Ressource() {}

    // Getters et Setters
    public int getId_ressource() { return id_ressource; }
    public void setId_ressource(int id_ressource) { this.id_ressource = id_ressource; }
    public int getId_module() { return id_module; }
    public void setId_module(int id_module) { this.id_module = id_module; }
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getValeur_contenu() { return valeur_contenu; }
    public void setValeur_contenu(String valeur_contenu) { this.valeur_contenu = valeur_contenu; }
    public int getOrdre() { return ordre; }
    public void setOrdre(int ordre) { this.ordre = ordre; }
}