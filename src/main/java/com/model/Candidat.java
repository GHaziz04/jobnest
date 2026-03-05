package com.model;

public class Candidat extends User {

    private String cv;
    private String niveau;

    public String getCv() { return cv; }
    public void setCv(String cv) { this.cv = cv; }

    public String getNiveau() { return niveau; }
    public void setNiveau(String niveau) { this.niveau = niveau; }
}
