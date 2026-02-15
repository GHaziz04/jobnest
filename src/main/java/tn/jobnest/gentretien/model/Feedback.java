package tn.jobnest.gentretien.model;

import java.sql.Timestamp;

public class Feedback {
    private int idFeedback;
    private int idEntretien;
    private int competenceTechniques;
    private int competenceCommunication;
    private int motivation;
    private int adequationAuPoste;
    private String commentaire;
    private String competenceManquantes;
    private boolean suggestionFormation;
    private Timestamp dateFeedback;

    public Feedback() {
    }

    public int getIdFeedback() {
        return idFeedback;
    }

    public void setIdFeedback(int idFeedback) {
        this.idFeedback = idFeedback;
    }

    public int getIdEntretien() {
        return idEntretien;
    }

    public void setIdEntretien(int idEntretien) {
        this.idEntretien = idEntretien;
    }

    public int getCompetenceTechniques() {
        return competenceTechniques;
    }

    public void setCompetenceTechniques(int competenceTechniques) {
        this.competenceTechniques = competenceTechniques;
    }

    public int getCompetenceCommunication() {
        return competenceCommunication;
    }

    public void setCompetenceCommunication(int competenceCommunication) {
        this.competenceCommunication = competenceCommunication;
    }

    public int getMotivation() {
        return motivation;
    }

    public void setMotivation(int motivation) {
        this.motivation = motivation;
    }

    public int getAdequationAuPoste() {
        return adequationAuPoste;
    }

    public void setAdequationAuPoste(int adequationAuPoste) {
        this.adequationAuPoste = adequationAuPoste;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }

    public String getCompetenceManquantes() {
        return competenceManquantes;
    }

    public void setCompetenceManquantes(String competenceManquantes) {
        this.competenceManquantes = competenceManquantes;
    }

    public boolean isSuggestionFormation() {
        return suggestionFormation;
    }

    public void setSuggestionFormation(boolean suggestionFormation) {
        this.suggestionFormation = suggestionFormation;
    }

    public Timestamp getDateFeedback() {
        return dateFeedback;
    }

    public void setDateFeedback(Timestamp dateFeedback) {
        this.dateFeedback = dateFeedback;
    }
}