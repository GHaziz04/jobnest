package tn.jobnest.gentretien.model;

public class Recruteur {
    private int idUser;
    private String nomRecruteur;
    private String prenomRecruteur;
    private String email;
    private String motDePasse;
    private String telephone;
    private String adresse;
    private String photoDeProfil;
    private String bio;
    private String gitHub;
    private String statut;
    private String dateInscription;
    private String nomEntreprise;
    private String secteur;
    private String siteWeb;
    private String descriptionEntreprise;
    private double balance;

    public Recruteur(int idUser, String nomRecruteur, String prenomRecruteur, String email,
                     String motDePasse, String telephone, String adresse, String photoDeProfil,
                     String bio, String gitHub, String statut, String dateInscription,
                     String nomEntreprise, String secteur, String siteWeb,
                     String descriptionEntreprise, double balance) {
        this.idUser = idUser;
        this.nomRecruteur = nomRecruteur;
        this.prenomRecruteur = prenomRecruteur;
        this.email = email;
        this.motDePasse = motDePasse;
        this.telephone = telephone;
        this.adresse = adresse;
        this.photoDeProfil = photoDeProfil;
        this.bio = bio;
        this.gitHub = gitHub;
        this.statut = statut;
        this.dateInscription = dateInscription;
        this.nomEntreprise = nomEntreprise;
        this.secteur = secteur;
        this.siteWeb = siteWeb;
        this.descriptionEntreprise = descriptionEntreprise;
        this.balance = balance;
    }

    // Getters
    public int getIdUser() { return idUser; }
    public String getNomRecruteur() { return nomRecruteur; }
    public String getPrenomRecruteur() { return prenomRecruteur; }
    public String getNomComplet() { return prenomRecruteur + " " + nomRecruteur; }
    public String getEmail() { return email; }
    public String getMotDePasse() { return motDePasse; }
    public String getTelephone() { return telephone; }
    public String getAdresse() { return adresse; }
    public String getPhotoDeProfil() { return photoDeProfil; }
    public String getBio() { return bio; }
    public String getGitHub() { return gitHub; }
    public String getStatut() { return statut; }
    public String getDateInscription() { return dateInscription; }
    public String getNomEntreprise() { return nomEntreprise; }
    public String getSecteur() { return secteur; }
    public String getSiteWeb() { return siteWeb; }
    public String getDescriptionEntreprise() { return descriptionEntreprise; }
    public double getBalance() { return balance; }

    // Setters
    public void setNomRecruteur(String nomRecruteur) { this.nomRecruteur = nomRecruteur; }
    public void setPrenomRecruteur(String prenomRecruteur) { this.prenomRecruteur = prenomRecruteur; }
    public void setEmail(String email) { this.email = email; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    public void setAdresse(String adresse) { this.adresse = adresse; }
    public void setPhotoDeProfil(String photoDeProfil) { this.photoDeProfil = photoDeProfil; }
    public void setBio(String bio) { this.bio = bio; }
    public void setGitHub(String gitHub) { this.gitHub = gitHub; }
    public void setNomEntreprise(String nomEntreprise) { this.nomEntreprise = nomEntreprise; }
    public void setSecteur(String secteur) { this.secteur = secteur; }
    public void setSiteWeb(String siteWeb) { this.siteWeb = siteWeb; }
    public void setDescriptionEntreprise(String descriptionEntreprise) { this.descriptionEntreprise = descriptionEntreprise; }
}