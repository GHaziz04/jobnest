package tn.jobnest.gformation.model;

public class Module {
    private int id_module;
    private int id_formation;
    private String titre;
    private int ordre;
    private String description;
    private String type;
    private String contenu; // NOUVEAU : Pour le texte du drop-down
    private Formation formationSelectionnee;
    // Constructeur vide
    public Module() {}

    // Constructeur complet (mis à jour)
    public Module(int id_module, int id_formation, String titre, int ordre, String description, String type) {
        this.id_module = id_module;
        this.id_formation = id_formation;
        this.titre = titre;
        this.ordre = ordre;
        this.description = description;
        this.type = type;
    }

    // Getters et Setters
    public int getId_module() { return id_module; }
    public void setId_module(int id_module) { this.id_module = id_module; }

    public int getId_formation() { return id_formation; }
    public void setId_formation(int id_formation) { this.id_formation = id_formation; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public int getOrdre() { return ordre; }
    public void setOrdre(int ordre) { this.ordre = ordre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    // AJOUT DES MÉTHODES POUR LE CONTENU
    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    @Override
    public String toString() {
        return "Module{" + "titre='" + titre + '\'' + ", ordre=" + ordre + '}';
    }
}