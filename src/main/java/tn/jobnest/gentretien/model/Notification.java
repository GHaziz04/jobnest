package tn.jobnest.gentretien.model;

import java.time.LocalDateTime;

/**
 * Modèle représentant une notification in-app pour le recruteur JobNest.
 */
public class Notification {

    public enum Type {
        RAPPEL_1H,
        RAPPEL_24H,
        ANNULATION,
        INFO
    }

    private int            id;
    private int            idRecruteur;
    private int            idEntretien;
    private String         titre;
    private String         message;
    private Type           type;
    private LocalDateTime  dateCreation;
    private boolean        lue;

    public Notification() {
        this.dateCreation = LocalDateTime.now();
        this.lue = false;
    }

    public Notification(int idRecruteur, int idEntretien,
                        String titre, String message, Type type) {
        this();
        this.idRecruteur  = idRecruteur;
        this.idEntretien  = idEntretien;
        this.titre        = titre;
        this.message      = message;
        this.type         = type;
    }

    // ── Getters / Setters ────────────────────────────────────────────
    public int            getId()           { return id; }
    public void           setId(int id)     { this.id = id; }

    public int            getIdRecruteur()               { return idRecruteur; }
    public void           setIdRecruteur(int idRecruteur){ this.idRecruteur = idRecruteur; }

    public int            getIdEntretien()               { return idEntretien; }
    public void           setIdEntretien(int idEntretien){ this.idEntretien = idEntretien; }

    public String         getTitre()                 { return titre; }
    public void           setTitre(String titre)     { this.titre = titre; }

    public String         getMessage()               { return message; }
    public void           setMessage(String message) { this.message = message; }

    public Type           getType()             { return type; }
    public void           setType(Type type)    { this.type = type; }

    public LocalDateTime  getDateCreation()               { return dateCreation; }
    public void           setDateCreation(LocalDateTime d){ this.dateCreation = d; }

    public boolean        isLue()            { return lue; }
    public void           setLue(boolean lue){ this.lue = lue; }

    /** Icône emoji selon le type */
    public String getIcon() {
        switch (type) {
            case RAPPEL_1H:  return "⏰";
            case RAPPEL_24H: return "📅";
            case ANNULATION: return "🚫";
            default:         return "ℹ️";
        }
    }

    /** Couleur accent selon le type (format CSS JavaFX) */
    public String getAccentColor() {
        switch (type) {
            case RAPPEL_1H:  return "#F97316";   // Orange – urgence
            case RAPPEL_24H: return "#2563EB";   // Bleu – rappel
            case ANNULATION: return "#EF4444";   // Rouge – alerte
            default:         return "#64748B";   // Gris – neutre
        }
    }
}