package tn.jobnest.gformation.Controller;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.jobnest.gformation.model.Formation;
import tn.jobnest.gformation.services.ServiceFormation;

import java.io.File;
import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class AjoutController {
    @FXML private TextField txtTitre, txtPrix, txtDuree, txtLieu, txtPlaces, txtUrlImage;
    @FXML private TextArea txtDescription, txtObjectifs;
    @FXML private ComboBox<String> comboNiveau, comboStatut;
    @FXML private DatePicker dateDebut, dateFin;

    private Formation formationAModifier = null;
    private Runnable onRefreshCallback;

    @FXML
    public void initialize() {
        // Remplissage des ComboBox
        comboNiveau.setItems(FXCollections.observableArrayList("Débutant", "Intermédiaire", "Avancé", "Expert"));
        comboStatut.setItems(FXCollections.observableArrayList("Programmée", "Ouvert", "Complet", "Terminé"));

        // Par défaut (Mode Ajout), on cache le choix du statut car il est automatisé
        comboStatut.setVisible(false);
        comboStatut.setManaged(false);

        // --- ÉCOUTEURS EN TEMPS RÉEL ---

        txtTitre.textProperty().addListener((obs, old, newVal) -> validerChamp(txtTitre, !newVal.trim().isEmpty()));
        txtLieu.textProperty().addListener((obs, old, newVal) -> validerChamp(txtLieu, !newVal.trim().isEmpty()));
        txtDescription.textProperty().addListener((obs, old, newVal) -> validerChamp(txtDescription, !newVal.trim().isEmpty()));
        txtObjectifs.textProperty().addListener((obs, old, newVal) -> validerChamp(txtObjectifs, !newVal.trim().isEmpty()));
        txtUrlImage.textProperty().addListener((obs, old, newVal) -> validerChamp(txtUrlImage, !newVal.trim().isEmpty()));

        txtPrix.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d*)?")) txtPrix.setText(old);
            validerChamp(txtPrix, inputValidePrix(txtPrix.getText()));
        });

        txtPlaces.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.matches("\\d*")) txtPlaces.setText(old);
            validerChamp(txtPlaces, inputValideEntier(txtPlaces.getText()));
        });

        comboNiveau.valueProperty().addListener((obs, old, newVal) -> validerChamp(comboNiveau, newVal != null));

        // On n'ajoute l'écouteur du statut que s'il est visible (mode modif)
        comboStatut.valueProperty().addListener((obs, old, newVal) -> {
            if (comboStatut.isVisible()) validerChamp(comboStatut, newVal != null);
        });

        dateDebut.valueProperty().addListener((obs, old, newVal) -> verifierCoherenceDatesEtDuree());
        dateFin.valueProperty().addListener((obs, old, newVal) -> verifierCoherenceDatesEtDuree());
        txtDuree.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.matches("\\d*")) txtDuree.setText(old);
            verifierCoherenceDatesEtDuree();
        });
    }

    private void validerChamp(Control field, boolean estValide) {
        field.getStyleClass().removeAll("field-success", "field-error");
        if (estValide) {
            field.getStyleClass().add("field-success");
        } else {
            field.getStyleClass().add("field-error");
        }
    }

    private void verifierCoherenceDatesEtDuree() {
        LocalDate debut = dateDebut.getValue();
        LocalDate fin = dateFin.getValue();
        // Note: On autorise la date de début aujourd'hui
        boolean debutOk = debut != null && !debut.isBefore(LocalDate.now());
        boolean finOk = fin != null && debut != null && !fin.isBefore(debut);
        boolean dureeOk = false;
        if (debutOk && finOk && !txtDuree.getText().isEmpty()) {
            try {
                long nbJours = ChronoUnit.DAYS.between(debut, fin) + 1;
                int heuresSaisies = Integer.parseInt(txtDuree.getText());
                dureeOk = (heuresSaisies > 0 && heuresSaisies <= (nbJours * 24));
            } catch (NumberFormatException e) { dureeOk = false; }
        }
        validerChamp(dateDebut, debutOk);
        validerChamp(dateFin, finOk);
        validerChamp(txtDuree, dureeOk);
    }

    private boolean inputValidePrix(String s) {
        try { return !s.isEmpty() && Double.parseDouble(s) > 0; } catch (Exception e) { return false; }
    }

    private boolean inputValideEntier(String s) {
        try { return !s.isEmpty() && Integer.parseInt(s) > 0; } catch (Exception e) { return false; }
    }

    public void chargerDonnees(Formation f) {
        this.formationAModifier = f;

        // On rend le statut visible car on est en mode modification
        comboStatut.setVisible(true);
        comboStatut.setManaged(true);

        txtTitre.setText(f.getTitre());
        txtDescription.setText(f.getDescription());
        txtPrix.setText(String.valueOf(f.getPrix()));
        txtDuree.setText(String.valueOf(f.getDuree_heures()));
        txtPlaces.setText(String.valueOf(f.getNb_places()));
        txtUrlImage.setText(f.getUrl_image());
        txtLieu.setText(f.getLieu());
        txtObjectifs.setText(f.getObjectifs());
        if (f.getDate_debut() != null) dateDebut.setValue(f.getDate_debut().toLocalDate());
        if (f.getDate_fin() != null) dateFin.setValue(f.getDate_fin().toLocalDate());
        comboNiveau.setValue(f.getNiveau());
        comboStatut.setValue(f.getStatut());

        validerToutLeFormulaire();
    }

    private void validerToutLeFormulaire() {
        validerChamp(txtTitre, !txtTitre.getText().trim().isEmpty());
        validerChamp(txtLieu, !txtLieu.getText().trim().isEmpty());
        validerChamp(txtDescription, !txtDescription.getText().trim().isEmpty());
        validerChamp(txtObjectifs, !txtObjectifs.getText().trim().isEmpty());
        validerChamp(txtUrlImage, !txtUrlImage.getText().trim().isEmpty());
        validerChamp(txtPrix, inputValidePrix(txtPrix.getText()));
        validerChamp(txtPlaces, inputValideEntier(txtPlaces.getText()));
        validerChamp(comboNiveau, comboNiveau.getValue() != null);

        if (comboStatut.isVisible()) {
            validerChamp(comboStatut, comboStatut.getValue() != null);
        }
        verifierCoherenceDatesEtDuree();
    }

    @FXML
    private void enregistrer() {
        validerToutLeFormulaire();

        List<Control> champsAVerifier = new ArrayList<>(List.of(
                txtTitre, txtPrix, txtDuree, txtPlaces, txtLieu,
                dateDebut, dateFin, comboNiveau, txtDescription, txtObjectifs, txtUrlImage
        ));

        if (comboStatut.isVisible()) champsAVerifier.add(comboStatut);

        for (Control c : champsAVerifier) {
            if (c.getStyleClass().contains("field-error")) {
                new Alert(Alert.AlertType.WARNING, "Veuillez corriger les champs invalides.").show();
                return;
            }
        }

        try {
            Formation f = (formationAModifier == null) ? new Formation() : formationAModifier;
            f.setTitre(txtTitre.getText());
            f.setDescription(txtDescription.getText());
            f.setPrix(Double.parseDouble(txtPrix.getText()));
            f.setDuree_heures(Integer.parseInt(txtDuree.getText()));
            f.setNb_places(Integer.parseInt(txtPlaces.getText()));
            f.setUrl_image(txtUrlImage.getText());
            f.setNiveau(comboNiveau.getValue());
            f.setLieu(txtLieu.getText());
            f.setObjectifs(txtObjectifs.getText());
            f.setDate_debut(Date.valueOf(dateDebut.getValue()));
            f.setDate_fin(Date.valueOf(dateFin.getValue()));

            // --- LOGIQUE DE STATUT AUTOMATISÉE ---
            LocalDate aujourdhui = LocalDate.now();
            LocalDate debut = dateDebut.getValue();
            LocalDate fin = dateFin.getValue();

            if (fin != null && fin.isBefore(aujourdhui)) {
                // Règle Prioritaire : Si la date de fin est passée, le statut est "Terminé"
                f.setStatut("Terminé");
            } else if (formationAModifier == null) {
                // Mode AJOUT : Calcul entre Programmée et Ouvert
                f.setNb_places_occupees(0);
                if (debut != null && debut.isAfter(aujourdhui)) {
                    f.setStatut("Programmée");
                } else {
                    f.setStatut("Ouvert");
                }
            } else {
                // Mode MODIFICATION : On prend la valeur du ComboBox (sauf si finie, géré au dessus)
                f.setStatut(comboStatut.getValue());
            }

            ServiceFormation sf = new ServiceFormation();
            if (formationAModifier == null) sf.ajouter(f); else sf.modifier(f);

            if (onRefreshCallback != null) onRefreshCallback.run();
            annuler();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Erreur : " + e.getMessage()).show();
        }
    }

    @FXML
    private void choisirImage(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File f = fc.showOpenDialog(((Node) event.getSource()).getScene().getWindow());
        if (f != null) {
            txtUrlImage.setText(f.toURI().toString());
        }
    }

    @FXML
    private void annuler() {
        if (txtTitre.getScene() != null) {
            ((Stage) txtTitre.getScene().getWindow()).close();
        }
    }

    public void setOnRefreshCallback(Runnable r) { this.onRefreshCallback = r; }
}