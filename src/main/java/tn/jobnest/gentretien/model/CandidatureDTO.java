package tn.jobnest.gentretien.model;

public class CandidatureDTO {
    private int idCandidature;
    private int idCandidat;
    private String nomComplet;
    private String titreOffre;
    private String titrePro;
    private String statut;
    private boolean isBoosted;
    private int idOffre;   // ← NOUVEAU : nécessaire pour planifier l'entretien dynamiquement

    public CandidatureDTO(int idCandidature, int idCandidat, String nomComplet,
                          String titreOffre, String titrePro, String statut,
                          boolean isBoosted, int idOffre) {
        this.idCandidature = idCandidature;
        this.idCandidat    = idCandidat;
        this.nomComplet    = nomComplet;
        this.titreOffre    = titreOffre;
        this.titrePro      = titrePro;
        this.statut        = statut;
        this.isBoosted     = isBoosted;
        this.idOffre       = idOffre;
    }

    // ── Getters ──
    public int    getIdCandidature() { return idCandidature; }
    public int    getIdCandidat()    { return idCandidat;    }
    public String getNomComplet()    { return nomComplet;    }
    public String getTitreOffre()    { return titreOffre;    }
    public String getTitrePro()      { return titrePro;      }
    public String getStatut()        { return statut;        }
    public boolean isBoosted()       { return isBoosted;     }
    public int    getIdOffre()       { return idOffre;       }
}