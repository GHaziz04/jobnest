package tn.jobnest.gentretien.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.jobnest.gentretien.model.Recruteur;
import tn.jobnest.gentretien.service.RecruteurService;

import java.io.File;
import java.io.IOException;

public class ProfileRecruteurController {

    @FXML private Circle  circleAvatar;
    @FXML private Label   lblNomComplet;
    @FXML private Label   lblPoste;
    @FXML private Label   lblEmailCard;
    @FXML private Label   lblTelCard;
    @FXML private Label   lblAdresseCard;
    @FXML private Label   lblStatutBadge;
    @FXML private Label   lblBalance;
    @FXML private Label   lblDateInscription;

    @FXML private TextField txtPrenom;
    @FXML private TextField txtNom;
    @FXML private TextField txtEmail;
    @FXML private TextField txtTel;
    @FXML private TextField txtAdresse;
    @FXML private TextArea  txaBio;
    @FXML private TextField txtGitHub;

    @FXML private TextField txtNomEntreprise;
    @FXML private TextField txtSecteur;
    @FXML private TextField txtSiteWeb;
    @FXML private TextArea  txaDescEntreprise;

    @FXML private Label lblSaveStatus;

    private final RecruteurService service = new RecruteurService();
    private Recruteur recruteurCourant;
    private String   cheminPhotoTemp;

    @FXML
    public void initialize() {
        chargerProfil(1);
    }

    private void chargerProfil(int id) {
        recruteurCourant = service.getRecruteurById(id);
        if (recruteurCourant == null) {
            lblNomComplet.setText("Recruteur introuvable");
            return;
        }

        lblNomComplet.setText(recruteurCourant.getNomComplet());
        lblPoste.setText(
                (recruteurCourant.getNomEntreprise() != null && !recruteurCourant.getNomEntreprise().isEmpty())
                        ? "Recruteur – " + recruteurCourant.getNomEntreprise()
                        : "Recruteur");
        lblEmailCard.setText(safe(recruteurCourant.getEmail()));
        lblTelCard.setText(safe(recruteurCourant.getTelephone()));
        lblAdresseCard.setText(safe(recruteurCourant.getAdresse()));
        lblDateInscription.setText(safe(recruteurCourant.getDateInscription()));

        String statut = recruteurCourant.getStatut();
        if (statut == null || statut.isEmpty()) statut = "actif";
        lblStatutBadge.setText(statut.toUpperCase());
        switch (statut.toLowerCase()) {
            case "actif":
                lblStatutBadge.setStyle(
                        "-fx-background-color:#D1FAE5; -fx-text-fill:#065F46;" +
                                "-fx-font-weight:700; -fx-padding:4 14 4 14; -fx-background-radius:20px;");
                break;
            case "bloqué":
                lblStatutBadge.setStyle(
                        "-fx-background-color:#FEE2E2; -fx-text-fill:#991B1B;" +
                                "-fx-font-weight:700; -fx-padding:4 14 4 14; -fx-background-radius:20px;");
                break;
            default:
                lblStatutBadge.setStyle(
                        "-fx-background-color:#F3F4F6; -fx-text-fill:#6B7280;" +
                                "-fx-font-weight:700; -fx-padding:4 14 4 14; -fx-background-radius:20px;");
        }

        lblBalance.setText(String.format("%.2f TND", recruteurCourant.getBalance()));

        chargerAvatar(recruteurCourant.getPhotoDeProfil());

        txtPrenom.setText(safe(recruteurCourant.getPrenomRecruteur()));
        txtNom.setText(safe(recruteurCourant.getNomRecruteur()));
        txtEmail.setText(safe(recruteurCourant.getEmail()));
        txtTel.setText(safe(recruteurCourant.getTelephone()));
        txtAdresse.setText(safe(recruteurCourant.getAdresse()));
        txaBio.setText(safe(recruteurCourant.getBio()));
        txtGitHub.setText(safe(recruteurCourant.getGitHub()));

        txtNomEntreprise.setText(safe(recruteurCourant.getNomEntreprise()));
        txtSecteur.setText(safe(recruteurCourant.getSecteur()));
        txtSiteWeb.setText(safe(recruteurCourant.getSiteWeb()));
        txaDescEntreprise.setText(safe(recruteurCourant.getDescriptionEntreprise()));

        lblSaveStatus.setText("");
    }

    private void chargerAvatar(String path) {
        if (path != null && !path.isEmpty()) {
            File f = new File(path);
            if (f.exists()) {
                circleAvatar.setFill(new ImagePattern(new Image(f.toURI().toString())));
                return;
            }
        }
        circleAvatar.setStyle("-fx-fill: linear-gradient(to bottom right, #1e3a8a, #3b82f6);");
    }

    @FXML
    private void choisirPhoto(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une photo de profil");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp"));
        File fichier = fc.showOpenDialog(
                ((Node) event.getSource()).getScene().getWindow());
        if (fichier != null) {
            cheminPhotoTemp = fichier.getAbsolutePath();
            circleAvatar.setFill(new ImagePattern(new Image(fichier.toURI().toString())));
            lblSaveStatus.setText("📷 Photo prête — cliquez sur Sauvegarder pour confirmer.");
            lblSaveStatus.setStyle("-fx-text-fill: #D97706; -fx-font-size: 12px;");
        }
    }

    @FXML
    private void sauvegarder(ActionEvent event) {
        if (recruteurCourant == null) return;

        if (txtEmail.getText().trim().isEmpty()) {
            afficherStatut("⚠️ L'email ne peut pas être vide.", "#DC2626");
            return;
        }

        recruteurCourant.setPrenomRecruteur(txtPrenom.getText().trim());
        recruteurCourant.setNomRecruteur(txtNom.getText().trim());
        recruteurCourant.setEmail(txtEmail.getText().trim());
        recruteurCourant.setTelephone(txtTel.getText().trim());
        recruteurCourant.setAdresse(txtAdresse.getText().trim());
        recruteurCourant.setBio(txaBio.getText().trim());
        recruteurCourant.setGitHub(txtGitHub.getText().trim());
        recruteurCourant.setNomEntreprise(txtNomEntreprise.getText().trim());
        recruteurCourant.setSecteur(txtSecteur.getText().trim());
        recruteurCourant.setSiteWeb(txtSiteWeb.getText().trim());
        recruteurCourant.setDescriptionEntreprise(txaDescEntreprise.getText().trim());

        if (cheminPhotoTemp != null)
            recruteurCourant.setPhotoDeProfil(cheminPhotoTemp);

        boolean ok = service.updateRecruteur(recruteurCourant);
        if (ok) {
            afficherStatut("✅ Profil mis à jour avec succès !", "#065F46");
            chargerProfil(recruteurCourant.getIdUser());
            cheminPhotoTemp = null;
        } else {
            afficherStatut("❌ Échec de la mise à jour. Vérifiez la base de données.", "#DC2626");
        }
    }

    @FXML
    private void annuler(ActionEvent event) {
        chargerProfil(recruteurCourant != null ? recruteurCourant.getIdUser() : 1);
        cheminPhotoTemp = null;
        afficherStatut("", "");
    }

    // ────────────────────────────────────────────────────────────────
    //  NAVIGATION SIDEBAR
    // ────────────────────────────────────────────────────────────────
    @FXML
    private void ouvrirEntretiens(ActionEvent event) {
        naviguer(event, "/tn/jobnest/gentretien/entretien-view.fxml",
                "JobNest - Gestion des Entretiens");
    }

    @FXML
    private void ouvrirFeedbacks(ActionEvent event) {
        naviguer(event, "/tn/jobnest/gentretien/feedback-interface.fxml",
                "JobNest - Gestion des Feedbacks");
    }

    @FXML
    private void ouvrirHistorique(ActionEvent event) {
        naviguer(event, "/tn/jobnest/gentretien/historique-entretien.fxml",
                "JobNest - Historique des Entretiens");
    }

    @FXML
    private void ouvrirCandidature(ActionEvent event) {
        naviguer(event, "/tn/jobnest/gentretien/GestionCandidatures.fxml",
                "JobNest - Gestion des Candidatures");
    }

    @FXML
    private void ouvrirOffresEmploi(ActionEvent event) {
        naviguer(event, "/tn/jobnest/gentretien/offre-emploi_view.fxml",
                "JobNest - Offres d'Emploi");
    }

    @FXML
    private void ouvrirMatching(ActionEvent event) {
        naviguer(event, "/tn/jobnest/gentretien/matching-view.fxml",
                "JobNest - Matching");
    }

    private void naviguer(ActionEvent event, String fxmlPath, String titre) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            java.net.URL css = getClass().getResource("/tn/jobnest/gentretien/styles.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            stage.setScene(scene);
            stage.setTitle(titre);
            stage.show();
        } catch (IOException ex) {
            afficherStatut("Impossible de naviguer : " + ex.getMessage(), "#DC2626");
        }
    }

    private String safe(String s) { return s != null ? s : ""; }

    private void afficherStatut(String msg, String color) {
        lblSaveStatus.setText(msg);
        if (!color.isEmpty())
            lblSaveStatus.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 13px; -fx-font-weight: 600;");
    }
}