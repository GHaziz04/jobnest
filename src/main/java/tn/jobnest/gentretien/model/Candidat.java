package tn.jobnest.gentretien.model;

public class Candidat {
    private int id;
    private String nom, prenom, email, telephone, ville, titrePro, bio, imagePath, dateNaissance;

    public Candidat(int id, String nom, String prenom, String email, String telephone,
                    String ville, String titrePro, String bio, String imagePath, String dateNaissance) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.telephone = telephone;
        this.ville = ville;
        this.titrePro = titrePro;
        this.bio = bio;
        this.imagePath = imagePath;
        this.dateNaissance = dateNaissance;
    }

    // Getters
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
    public String getNomComplet() { return prenom + " " + nom; }
    public String getEmail() { return email; }
    public String getTelephone() { return telephone; }
    public String getVille() { return ville; }
    public String getTitrePro() { return titrePro; }
    public String getImagePath() { return imagePath; }
    public String getDateNaissance() { return dateNaissance; }
}