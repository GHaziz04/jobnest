package com.model;

public class Recruteur extends User {

    private String entreprise;
    private String departement;

    public String getEntreprise() { return entreprise; }
    public void setEntreprise(String entreprise) {
        this.entreprise = entreprise;
    }

    public String getDepartement() { return departement; }
    public void setDepartement(String departement) {
        this.departement = departement;
    }
}
