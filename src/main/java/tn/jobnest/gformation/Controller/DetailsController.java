package tn.jobnest.gformation.Controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import tn.jobnest.gformation.model.Formation;

public class DetailsController {
    @FXML private Label labTitre, labObjectifs, labDates, labLieu, labStatut;
    @FXML private Label labDescription, labPrix, labFormateur, labPlaces, labDuree, labNiveau;

    public void setFormation(Formation f) {
        if (f == null) return;

        // Informations principales
        labTitre.setText(nonVide(f.getTitre()));
        labDescription.setText(nonVide(f.getDescription()));
        labObjectifs.setText(nonVide(f.getObjectifs()));

        // Métadonnées
        labFormateur.setText(nonVide(f.getNomFormateur()));
        labPrix.setText(String.format("%.2f DT", f.getPrix()));
        labDuree.setText(f.getDuree_heures() + " Heures");
        labNiveau.setText("Niveau " + nonVide(f.getNiveau()));
        labPlaces.setText(f.getNb_places_occupees() + " / " + f.getNb_places() + " inscrits");

        // Planning et Lieu
        labDates.setText("Du " + f.getDate_debut() + " au " + f.getDate_fin());
        labLieu.setText("📍 " + nonVide(f.getLieu()));

        // Style dynamique du Statut
        configurerStatut(f.getStatut());
    }

    private void configurerStatut(String statut) {
        if (labStatut == null) return;

        // --- SÉCURITÉ : GESTION DU NULL ---
        if (statut == null || statut.trim().isEmpty()) {
            statut = "OUVERT"; // Valeur par défaut
        }

        labStatut.setText(statut.toUpperCase());

        // --- COULEURS DEMANDÉES ---
        String backgroundColor;
        String textColor = "white";

        switch (statut.toLowerCase()) {
            case "terminé":
            case "terminée":
                backgroundColor = "#E53E3E"; // Rouge
                break;
            case "ouvert":
                backgroundColor = "#38A169"; // Vert
                break;
            case "programmée":
            case "en cours":
                backgroundColor = "#3182CE"; // Bleu
                break;
            case "complet":
                backgroundColor = "#DD6B20"; // Orange
                break;
            default:
                backgroundColor = "#718096"; // Gris par défaut
                break;
        }

        // Application du style visuel
        labStatut.setStyle("-fx-background-color: " + backgroundColor + "; " +
                "-fx-text-fill: " + textColor + "; " +
                "-fx-padding: 5 12; " +
                "-fx-background-radius: 15; " +
                "-fx-font-size: 11; " +
                "-fx-font-weight: bold;");
    }

    private String nonVide(String str) {
        return (str == null || str.trim().isEmpty()) ? "Information non disponible" : str;
    }

    @FXML
    private void fermer(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}