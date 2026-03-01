package tn.jobnest.gentretien.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.jobnest.gentretien.model.Entretien;
import tn.jobnest.gentretien.service.Entretienservice;
import tn.jobnest.gentretien.service.GoogleMeetService;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

public class EntretienFormController {

    @FXML private DatePicker        dateEntretien;
    @FXML private TextField         heureDebut;
    @FXML private TextField         heureFin;
    @FXML private ComboBox<String>  typeEntretien;
    @FXML private TextField         lieu;
    @FXML private TextField         lienVisio;
    @FXML private ComboBox<String>  statut;
    @FXML private TextField         noteRecruteur;
    @FXML private Button            btnGenererMeet;

    /* ── Bouton "Choisir sur la carte" ajouté dans le FXML ── */
    @FXML private Button            btnChoisirSurCarte;

    private Entretien          entretien;
    private boolean            isReorganisation = false;
    private final Entretienservice service = new Entretienservice();

    // ─────────────────────────────────────────────────────────────────
    @FXML
    private void initialize() {
        typeEntretien.getItems().addAll("présentiel", "visio");

        statut.getItems().addAll("proposé", "confirmé");
        statut.setValue("proposé");
        statut.setDisable(true);

        Tooltip tooltipStatut = new Tooltip(
                "Le statut est géré automatiquement.\n"
                        + "• 'proposé' : à la création\n"
                        + "• 'confirmé' : confirmé par le candidat\n"
                        + "• 'réalisé' : après que le candidat a rejoint l'entretien\n"
                        + "• 'annulé'  : si la date est passée sans action du candidat"
        );
        tooltipStatut.setStyle("-fx-font-size: 12px;");
        Tooltip.install(statut, tooltipStatut);

        /* ── Listener type d'entretien ── */
        typeEntretien.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean isPresential = "présentiel".equals(newVal);
            lieu.setDisable(!isPresential);
            btnChoisirSurCarte.setDisable(!isPresential);
            if (!isPresential) {
                lieu.clear();
                lienVisio.setDisable(false);
            } else {
                lienVisio.setDisable(true);
                lienVisio.clear();
            }
        });

        /* ── Tooltip sur le champ lieu ── */
        Tooltip tipLieu = new Tooltip(
                "Cliquez sur le bouton \"Choisir sur la carte\"\n"
                        + "pour sélectionner le lieu visuellement."
        );
        tipLieu.setStyle("-fx-font-size: 12px;");
        Tooltip.install(lieu, tipLieu);
    }

    // ─────────────────────────────────────────────────────────────────
    /**
     * ═══════════════════════════════════════════════════════════════
     *  NOUVELLE MÉTHODE — Ouvrir le Location Picker
     *
     *  Déclenchée par le bouton "Choisir sur la carte" dans le FXML.
     *  Ouvre la fenêtre location-picker-view.fxml en mode modal (showAndWait).
     *  Quand l'utilisateur valide, l'adresse est injectée dans le champ lieu.
     * ═══════════════════════════════════════════════════════════════
     */
    @FXML
    private void ouvrirLocationPicker() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/tn/jobnest/gentretien/location-picker-view.fxml")
            );
            Parent root = loader.load();

            LocationPickerController pickerCtrl = loader.getController();

            Stage pickerStage = new Stage();
            pickerStage.setTitle("JobNest - Choisir le lieu de l'entretien");
            pickerStage.setScene(new Scene(root));
            pickerStage.setMinWidth(640);
            pickerStage.setMinHeight(480);
            pickerStage.setWidth(860);
            pickerStage.setHeight(620);
            pickerStage.centerOnScreen();

            /* Modal — bloque le formulaire pendant la sélection */
            pickerStage.initModality(Modality.APPLICATION_MODAL);
            pickerStage.initOwner(lieu.getScene().getWindow());

            /* Attendre que l'utilisateur ferme la fenêtre */
            pickerStage.showAndWait();

            /* Récupérer l'adresse choisie */
            String adresseChoisie = pickerCtrl.getAdresseChoisie();

            if (adresseChoisie != null && !adresseChoisie.isEmpty()) {
                lieu.setText(adresseChoisie);
                lieu.requestFocus();
                /* Feedback visuel */
                lieu.setStyle(lieu.getStyle()
                        + "; -fx-border-color: #00C37A; -fx-border-width: 1.5;");
                /* Remettre le style normal après 2 secondes */
                javafx.animation.PauseTransition pause =
                        new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
                pause.setOnFinished(e -> lieu.setStyle(
                        "-fx-font-size: 13px; -fx-border-color: #E2E8F0;"
                                + " -fx-border-radius: 10; -fx-background-radius: 10;"
                                + " -fx-border-width: 1.5; -fx-padding: 9 12;"));
                pause.play();
            }

        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'ouvrir le sélecteur de lieu : " + ex.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    public void setEntretien(Entretien e) {
        this.entretien = e;
        this.isReorganisation = false;
        if (e != null) {
            dateEntretien.setValue(e.getDateEntretien() != null
                    ? e.getDateEntretien().toLocalDate() : null);
            heureDebut.setText(e.getHeureDebut() != null
                    ? e.getHeureDebut().toLocalTime().toString() : "");
            heureFin.setText(e.getHeureFin() != null
                    ? e.getHeureFin().toLocalTime().toString() : "");
            typeEntretien.setValue(e.getTypeEntretien());
            lieu.setText(e.getLieu());
            lienVisio.setText(e.getLienVisio());

            String currentStatut = e.getStatut();
            if (currentStatut != null && !statut.getItems().contains(currentStatut)) {
                statut.getItems().add(currentStatut);
            }
            statut.setValue(currentStatut);
            statut.setDisable(true);

            noteRecruteur.setText(e.getNoteRecruteur());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    public void setEntretienPourReorganisation(Entretien e) {
        this.entretien = e;
        this.isReorganisation = true;
        if (e != null) {
            dateEntretien.setValue(null);
            dateEntretien.setPromptText("Choisissez une nouvelle date");
            heureDebut.setText(e.getHeureDebut() != null
                    ? e.getHeureDebut().toLocalTime().toString() : "");
            heureFin.setText(e.getHeureFin() != null
                    ? e.getHeureFin().toLocalTime().toString() : "");
            typeEntretien.setValue(e.getTypeEntretien());
            if ("présentiel".equals(e.getTypeEntretien())) {
                lieu.setText(e.getLieu());
                lienVisio.clear();
            } else {
                lieu.clear();
                lienVisio.clear();
                lienVisio.setPromptText("Generez un nouveau lien Meet pour la nouvelle date");
            }
            if (!statut.getItems().contains("proposé")) statut.getItems().add("proposé");
            statut.setValue("proposé");
            statut.setDisable(true);
            noteRecruteur.setText(e.getNoteRecruteur());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @FXML
    private void genererLienMeet() {
        if (dateEntretien.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Champ requis",
                    "Veuillez sélectionner une date."); return;
        }
        if (dateEntretien.getValue().isBefore(LocalDate.now())) {
            showAlert(Alert.AlertType.WARNING, "Date invalide",
                    "Impossible de créer un entretien dans le passé."); return;
        }
        String hDebutStr = heureDebut.getText().trim();
        String hFinStr   = heureFin.getText().trim();
        if (hDebutStr.isEmpty() || hFinStr.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champs requis",
                    "Les heures de début et fin sont requises pour générer le lien Meet."); return;
        }
        try {
            LocalTime debut = LocalTime.parse(hDebutStr);
            LocalTime fin   = LocalTime.parse(hFinStr);
            if (fin.isBefore(debut) || fin.equals(debut)) {
                showAlert(Alert.AlertType.WARNING, "Incohérence horaire",
                        "L'heure de fin doit être après l'heure de début."); return;
            }
            LocalDateTime dateTimeDebut = LocalDateTime.of(dateEntretien.getValue(), debut);
            LocalDateTime dateTimeFin   = LocalDateTime.of(dateEntretien.getValue(), fin);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            showAlert(Alert.AlertType.INFORMATION, "Génération en cours",
                    "Veuillez patienter pendant la génération du lien Google Meet...");
            String meetLink = GoogleMeetService.creerMeetingLink(
                    "Entretien JobNest", "Entretien d'embauche planifié via JobNest",
                    dateTimeDebut.format(fmt), dateTimeFin.format(fmt));
            lienVisio.setText(meetLink);
            showAlert(Alert.AlertType.INFORMATION, "Succès",
                    "Lien Google Meet généré !\n\n" + meetLink);
        } catch (DateTimeParseException ex) {
            showAlert(Alert.AlertType.ERROR, "Format invalide",
                    "Format d'heure invalide. Utilisez HH:mm (ex: 14:30)");
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de générer le lien Meet : " + ex.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @FXML
    private void validerAdresse() {
        String adresse = lieu.getText().trim();
        if (adresse.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champ vide",
                    "Veuillez saisir ou sélectionner une adresse."); return;
        }
        try {
            String searchUrl = "https://www.google.com/maps/search/?api=1&query="
                    + java.net.URLEncoder.encode(adresse, "UTF-8");
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Valider l'adresse");
            confirmation.setHeaderText("Vérifier l'adresse sur Google Maps");
            confirmation.setContentText("Adresse : " + adresse
                    + "\n\nVoulez-vous ouvrir Google Maps pour vérifier ?");
            ButtonType btnOui = new ButtonType("Oui, vérifier");
            ButtonType btnNon = new ButtonType("Non, continuer");
            confirmation.getButtonTypes().setAll(btnOui, btnNon);
            Optional<ButtonType> result = confirmation.showAndWait();
            if (result.isPresent() && result.get() == btnOui) {
                Desktop.getDesktop().browse(new URI(searchUrl));
            }
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'ouvrir Google Maps : " + ex.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @FXML
    private void save() {
        if (dateEntretien.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Champ obligatoire",
                    "Veuillez sélectionner une date d'entretien.");
            dateEntretien.requestFocus(); return;
        }
        if (dateEntretien.getValue().isBefore(LocalDate.now())) {
            showAlert(Alert.AlertType.WARNING, "Date invalide",
                    "Impossible de créer un entretien dans le passé.\nVeuillez choisir une date future.");
            dateEntretien.requestFocus(); return;
        }
        String hDebutStr = heureDebut.getText().trim();
        String hFinStr   = heureFin.getText().trim();
        if (hDebutStr.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champ obligatoire",
                    "Veuillez renseigner l'heure de début.");
            heureDebut.requestFocus(); return;
        }
        if (hFinStr.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champ obligatoire",
                    "Veuillez renseigner l'heure de fin.");
            heureFin.requestFocus(); return;
        }
        if (typeEntretien.getValue() == null || typeEntretien.getValue().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champ obligatoire",
                    "Veuillez sélectionner un type d'entretien.");
            typeEntretien.requestFocus(); return;
        }
        if ("présentiel".equals(typeEntretien.getValue())) {
            if (lieu.getText() == null || lieu.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Champ obligatoire",
                        "Veuillez renseigner le lieu.\n"
                                + "Utilisez le bouton \"Choisir sur la carte\" pour sélectionner un lieu.");
                lieu.requestFocus(); return;
            }
        } else if ("visio".equals(typeEntretien.getValue())) {
            if (lienVisio.getText() == null || lienVisio.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Champ obligatoire",
                        "Veuillez renseigner le lien visio ou cliquer sur 'Générer Meet'.");
                lienVisio.requestFocus(); return;
            }
        }
        LocalTime debut, fin;
        try {
            debut = LocalTime.parse(hDebutStr);
            fin   = LocalTime.parse(hFinStr);
        } catch (DateTimeParseException ex) {
            showAlert(Alert.AlertType.ERROR, "Format invalide",
                    "Format d'heure invalide. Utilisez le format HH:mm\nExemple : 14:30"); return;
        }
        if (fin.isBefore(debut) || fin.equals(debut)) {
            showAlert(Alert.AlertType.WARNING, "Incohérence horaire",
                    "L'heure de fin doit être strictement après l'heure de début.");
            heureFin.requestFocus(); return;
        }
        /* Vérification conflits */
        try {
            List<Entretien> tousEntretiens = service.afficher();
            for (Entretien e : tousEntretiens) {
                if (entretien != null && e.getIdEntretien() == entretien.getIdEntretien()) continue;
                if (e.getDateEntretien() != null
                        && e.getDateEntretien().toLocalDate().equals(dateEntretien.getValue())) {
                    if (e.getHeureDebut() != null && e.getHeureFin() != null) {
                        LocalTime autreDebut = e.getHeureDebut().toLocalTime();
                        LocalTime autreFin   = e.getHeureFin().toLocalTime();
                        boolean conflit = false;
                        if ((debut.isAfter(autreDebut) || debut.equals(autreDebut)) && debut.isBefore(autreFin)) conflit = true;
                        if (fin.isAfter(autreDebut) && (fin.isBefore(autreFin) || fin.equals(autreFin))) conflit = true;
                        if ((debut.isBefore(autreDebut) || debut.equals(autreDebut))
                                && (fin.isAfter(autreFin) || fin.equals(autreFin))) conflit = true;
                        if (conflit) {
                            String titreOffre = service.getOffreTitre(e.getIdOffre());
                            showAlert(Alert.AlertType.ERROR, "Conflit d'horaire",
                                    "Un entretien existe déjà à cette date et heure :\n\n"
                                            + "Offre : " + titreOffre + "\n"
                                            + "Date : " + e.getDateEntretien().toLocalDate()
                                            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "\n"
                                            + "Horaire : " + autreDebut.format(DateTimeFormatter.ofPattern("HH:mm"))
                                            + " - " + autreFin.format(DateTimeFormatter.ofPattern("HH:mm"))
                                            + "\n\nVeuillez choisir une autre date ou un autre horaire.");
                            return;
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de vérifier les conflits : " + ex.getMessage()); return;
        }
        /* Enregistrement */
        if (entretien == null) entretien = new Entretien();
        entretien.setDateEntretien(java.sql.Date.valueOf(dateEntretien.getValue()));
        entretien.setHeureDebut(Time.valueOf(debut));
        entretien.setHeureFin(Time.valueOf(fin));
        entretien.setTypeEntretien(typeEntretien.getValue());
        entretien.setLieu(lieu.getText().trim());
        entretien.setLienVisio(lienVisio.getText().trim());
        entretien.setNoteRecruteur(noteRecruteur.getText().trim());
        entretien.setDateCreation(new Timestamp(System.currentTimeMillis()));
        entretien.setIdRecruteur(1);
        entretien.setIdOffre(10);
        if (entretien.getIdEntretien() == 0 || isReorganisation) {
            entretien.setStatut("proposé");
        }
        try {
            if (entretien.getIdEntretien() == 0) {
                service.ajouter(entretien);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Entretien créé avec succès !");
            } else if (isReorganisation) {
                service.update(entretien);
                showAlert(Alert.AlertType.INFORMATION, "Réorganisation réussie",
                        "L'entretien a été réorganisé avec succès !\n"
                                + "Le statut est repassé à 'proposé'.");
            } else {
                service.update(entretien);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Entretien modifié avec succès !");
            }
            Stage stage = (Stage) dateEntretien.getScene().getWindow();
            stage.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur BD",
                    "Impossible d'enregistrer l'entretien : " + ex.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}