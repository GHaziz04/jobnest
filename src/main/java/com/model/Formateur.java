package com.model;

public class Formateur extends User {

    private String specialite;
    private int experience;

    public String getSpecialite() { return specialite; }
    public void setSpecialite(String specialite) {
        this.specialite = specialite;
    }

    public int getExperience() { return experience; }
    public void setExperience(int experience) {
        this.experience = experience;
    }
}
