package tn.jobnest.gentretien.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.stage.Stage;
import tn.jobnest.gentretien.service.MatchingService.MatchingResult;

/**
 * Contrôleur de la popup qui affiche le détail du score de matching.
 * Lié au fichier FXML : matching-result.fxml
 */
public class MatchingResultController {

    @FXML private Label  lblNomCandidat;
    @FXML private Label  lblDecision;
    @FXML private Label  lblScoreFinal;
    @FXML private Label  lblScoreComp;
    @FXML private Label  lblScoreExp;
    @FXML private VBox   vboxCompetencesOffre;
    @FXML private VBox   vboxCompetencesCandidat;
    @FXML private VBox   vboxExperiencesOffre;
    @FXML private VBox   vboxExperiencesCandidat;
    @FXML private HBox   hboxDecision;
    @FXML private Region barreCompetences;
    @FXML private Region barreExperiences;
    @FXML private Region barreFinal;

    public void afficherResultat(MatchingResult result, String nomCandidat) {
        if (lblNomCandidat != null) lblNomCandidat.setText(nomCandidat);

        // ─── Score final ─────────────────────────────────────────
        if (lblScoreFinal != null)
            lblScoreFinal.setText(String.format("%.1f%%", result.scoreFinal));

        if (lblScoreComp != null)
            lblScoreComp.setText(String.format("%.1f%%", result.scoreCompetences));

        if (lblScoreExp != null)
            lblScoreExp.setText(String.format("%.1f%%", result.scoreExperiences));

        // ─── Barres de progression ───────────────────────────────
        setBarreLargeur(barreCompetences, result.scoreCompetences);
        setBarreLargeur(barreExperiences, result.scoreExperiences);
        setBarreLargeur(barreFinal,       result.scoreFinal);

        // ─── Décision ────────────────────────────────────────────
        if (lblDecision != null) {
            String txt; String bg; String fg;
            switch (result.decision) {
                case "ACCEPTÉ":
                    txt = "✅  Candidature ACCEPTÉE automatiquement";
                    bg  = "#DCFCE7"; fg = "#15803D"; break;
                case "EN_REVISION":
                    txt = "🔍  Mise EN RÉVISION — votre décision est requise";
                    bg  = "#FEF3C7"; fg = "#D97706"; break;
                default:
                    txt = "❌  Candidature ANNULÉE automatiquement";
                    bg  = "#FEE2E2"; fg = "#DC2626";
            }
            lblDecision.setText(txt);
            lblDecision.setStyle(String.format(
                    "-fx-background-color:%s; -fx-text-fill:%s; " +
                            "-fx-font-weight:800; -fx-font-size:14px; " +
                            "-fx-padding:12 20; -fx-background-radius:12;", bg, fg));
        }

        // ─── Listes ──────────────────────────────────────────────
        remplirListe(vboxCompetencesOffre,     result.competencesRequises,  "#DBEAFE", "#1E40AF", "Aucune compétence requise");
        remplirListe(vboxCompetencesCandidat,  result.competencesCandidat,  "#DCFCE7", "#15803D", "Aucune compétence trouvée");
        remplirListe(vboxExperiencesOffre,     result.experiencesRequises,  "#FEF3C7", "#B45309", "Aucune expérience requise");
        remplirListe(vboxExperiencesCandidat,  result.experiencesCandidat,  "#F0FDF4", "#166534", "Aucune expérience trouvée");
    }

    private void setBarreLargeur(Region barre, double score) {
        if (barre == null) return;
        double pct = Math.min(score, 100.0);
        barre.setPrefWidth(pct * 3); // 300px max = 100%
        String couleur;
        if      (pct >= 80) couleur = "#059669";
        else if (pct >= 40) couleur = "#D97706";
        else                couleur = "#DC2626";
        barre.setStyle("-fx-background-color:" + couleur + "; -fx-background-radius:6;");
    }

    private void remplirListe(VBox box, java.util.List<String> items, String bg, String fg, String vide) {
        if (box == null) return;
        box.getChildren().clear();
        if (items == null || items.isEmpty()) {
            Label l = new Label(vide);
            l.setStyle("-fx-font-size:12px; -fx-text-fill:#94A3B8; -fx-font-style:italic;");
            box.getChildren().add(l);
            return;
        }
        for (String item : items) {
            Label tag = new Label("• " + item);
            tag.setStyle(String.format(
                    "-fx-background-color:%s; -fx-text-fill:%s; " +
                            "-fx-font-size:12px; -fx-font-weight:600; " +
                            "-fx-padding:4 10; -fx-background-radius:8;", bg, fg));
            box.getChildren().add(tag);
        }
    }

    @FXML
    private void fermer() {
        if (lblNomCandidat != null && lblNomCandidat.getScene() != null)
            ((Stage) lblNomCandidat.getScene().getWindow()).close();
    }
}