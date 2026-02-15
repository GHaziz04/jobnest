package tn.jobnest.gentretien.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.jobnest.gentretien.model.Entretien;
import tn.jobnest.gentretien.service.Entretienservice;
import tn.jobnest.gentretien.service.GoogleMeetService;

import java.awt.Desktop;
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

    @FXML private DatePicker dateEntretien;
    @FXML private TextField heureDebut;
    @FXML private TextField heureFin;
    @FXML private ComboBox<String> typeEntretien;
    @FXML private TextField lieu;
    @FXML private TextField lienVisio;
    @FXML private ComboBox<String> statut;
    @FXML private TextField noteRecruteur;
    @FXML private Button btnGenererMeet;

    private Entretien entretien;
    private final Entretienservice service = new Entretienservice();

    @FXML
    private void initialize() {
        typeEntretien.getItems().addAll("présentiel", "visio");
        statut.getItems().addAll("proposé", "confirmé", "réalisé", "annulé");

        // Listener pour afficher/masquer les champs selon le type
        typeEntretien.valueProperty().addListener((obs, oldVal, newVal) -> {
            if ("visio".equals(newVal)) {
                lieu.setDisable(true);
                lieu.clear();
                lienVisio.setDisable(false);
            } else {
                lieu.setDisable(false);
                lienVisio.setDisable(true);
                lienVisio.clear();
            }
        });
    }

    public void setEntretien(Entretien e) {
        this.entretien = e;
        if (e != null) {
            dateEntretien.setValue(e.getDateEntretien() != null ? e.getDateEntretien().toLocalDate() : null);
            heureDebut.setText(e.getHeureDebut() != null ? e.getHeureDebut().toLocalTime().toString() : "");
            heureFin.setText(e.getHeureFin() != null ? e.getHeureFin().toLocalTime().toString() : "");
            typeEntretien.setValue(e.getTypeEntretien());
            lieu.setText(e.getLieu());
            lienVisio.setText(e.getLienVisio());
            statut.setValue(e.getStatut());
            noteRecruteur.setText(e.getNoteRecruteur());
        }
    }

    @FXML
    private void genererLienMeet() {
        // Validation des champs requis
        if (dateEntretien.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Champ requis", "Veuillez sélectionner une date.");
            return;
        }

        // Vérifier que la date n'est pas dans le passé
        if (dateEntretien.getValue().isBefore(LocalDate.now())) {
            showAlert(Alert.AlertType.WARNING, "Date invalide",
                    "Impossible de créer un entretien dans le passé.");
            return;
        }

        String hDebutStr = heureDebut.getText().trim();
        String hFinStr = heureFin.getText().trim();

        if (hDebutStr.isEmpty() || hFinStr.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champs requis",
                    "Les heures de début et fin sont requises pour générer le lien Meet.");
            return;
        }

        try {
            LocalTime debut = LocalTime.parse(hDebutStr);
            LocalTime fin = LocalTime.parse(hFinStr);

            if (fin.isBefore(debut) || fin.equals(debut)) {
                showAlert(Alert.AlertType.WARNING, "Incohérence horaire",
                        "L'heure de fin doit être après l'heure de début.");
                return;
            }

            // Construire les dates complètes
            LocalDateTime dateTimeDebut = LocalDateTime.of(dateEntretien.getValue(), debut);
            LocalDateTime dateTimeFin = LocalDateTime.of(dateEntretien.getValue(), fin);

            // Format ISO 8601 pour Google Calendar
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            String dateDebutISO = dateTimeDebut.format(formatter);
            String dateFinISO = dateTimeFin.format(formatter);

            // Afficher un message de chargement
            showAlert(Alert.AlertType.INFORMATION, "Génération en cours",
                    "Veuillez patienter pendant la génération du lien Google Meet...");

            // Générer le lien Meet
            String meetLink = GoogleMeetService.creerMeetingLink(
                    "Entretien JobNest",
                    "Entretien d'embauche planifié via JobNest",
                    dateDebutISO,
                    dateFinISO
            );

            // Remplir le champ lien_visio
            lienVisio.setText(meetLink);

            showAlert(Alert.AlertType.INFORMATION, "Succès",
                    "Lien Google Meet généré avec succès !\n\n" + meetLink);

        } catch (DateTimeParseException ex) {
            showAlert(Alert.AlertType.ERROR, "Format invalide",
                    "Format d'heure invalide. Utilisez HH:mm (ex: 14:30)");
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de générer le lien Meet : " + ex.getMessage());
        }
    }

    @FXML
    private void validerAdresse() {
        String adresse = lieu.getText().trim();

        if (adresse.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champ vide",
                    "Veuillez saisir une adresse.");
            return;
        }

        try {
            // Ouvrir directement sur Google Maps pour vérifier
            String searchUrl = "https://www.google.com/maps/search/?api=1&query="
                    + java.net.URLEncoder.encode(adresse, "UTF-8");

            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Valider l'adresse");
            confirmation.setHeaderText("Vérifier l'adresse sur Google Maps");
            confirmation.setContentText("📍 Adresse : " + adresse + "\n\nVoulez-vous ouvrir Google Maps pour vérifier cette adresse ?");

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

    @FXML
    private void save() {
        // ===== VALIDATION 1 : Aucun champ obligatoire vide =====
        if (dateEntretien.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Champ obligatoire",
                    "Veuillez sélectionner une date d'entretien.");
            dateEntretien.requestFocus();
            return;
        }

        // ===== VALIDATION 2 : Pas d'entretien dans le passé =====
        if (dateEntretien.getValue().isBefore(LocalDate.now())) {
            showAlert(Alert.AlertType.WARNING, "Date invalide",
                    "Impossible de créer un entretien dans le passé.\nVeuillez choisir une date future.");
            dateEntretien.requestFocus();
            return;
        }

        String hDebutStr = heureDebut.getText().trim();
        String hFinStr = heureFin.getText().trim();

        if (hDebutStr.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champ obligatoire",
                    "Veuillez renseigner l'heure de début.");
            heureDebut.requestFocus();
            return;
        }

        if (hFinStr.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champ obligatoire",
                    "Veuillez renseigner l'heure de fin.");
            heureFin.requestFocus();
            return;
        }

        // Validation du type d'entretien
        if (typeEntretien.getValue() == null || typeEntretien.getValue().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champ obligatoire",
                    "Veuillez sélectionner un type d'entretien (présentiel ou visio).");
            typeEntretien.requestFocus();
            return;
        }

        // Validation selon le type
        if ("présentiel".equals(typeEntretien.getValue())) {
            if (lieu.getText() == null || lieu.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Champ obligatoire",
                        "Veuillez renseigner le lieu pour un entretien présentiel.");
                lieu.requestFocus();
                return;
            }
        } else if ("visio".equals(typeEntretien.getValue())) {
            if (lienVisio.getText() == null || lienVisio.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Champ obligatoire",
                        "Veuillez renseigner le lien visio ou cliquer sur 'Générer Meet'.");
                lienVisio.requestFocus();
                return;
            }
        }

        // Validation du statut
        if (statut.getValue() == null || statut.getValue().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champ obligatoire",
                    "Veuillez sélectionner un statut.");
            statut.requestFocus();
            return;
        }

        // ===== VALIDATION 3 : Format des heures =====
        LocalTime debut;
        LocalTime fin;
        try {
            debut = LocalTime.parse(hDebutStr);
            fin = LocalTime.parse(hFinStr);
        } catch (DateTimeParseException ex) {
            showAlert(Alert.AlertType.ERROR, "Format invalide",
                    "Format d'heure invalide. Utilisez le format HH:mm\nExemple : 14:30");
            return;
        }

        // ===== VALIDATION 4 : Heure fin > Heure début =====
        if (fin.isBefore(debut) || fin.equals(debut)) {
            showAlert(Alert.AlertType.WARNING, "Incohérence horaire",
                    "L'heure de fin doit être strictement après l'heure de début.");
            heureFin.requestFocus();
            return;
        }

        // ===== VALIDATION 5 : Pas de conflit avec d'autres entretiens =====
        try {
            List<Entretien> tousEntretiens = service.afficher();

            for (Entretien e : tousEntretiens) {
                // Ignorer l'entretien en cours de modification
                if (entretien != null && e.getIdEntretien() == entretien.getIdEntretien()) {
                    continue;
                }

                // Vérifier si même date
                if (e.getDateEntretien() != null &&
                        e.getDateEntretien().toLocalDate().equals(dateEntretien.getValue())) {

                    if (e.getHeureDebut() != null && e.getHeureFin() != null) {
                        LocalTime autreDebut = e.getHeureDebut().toLocalTime();
                        LocalTime autreFin = e.getHeureFin().toLocalTime();

                        // Vérifier le chevauchement
                        boolean conflit = false;

                        // Cas 1 : Le nouvel entretien commence pendant un autre entretien
                        if ((debut.isAfter(autreDebut) || debut.equals(autreDebut)) && debut.isBefore(autreFin)) {
                            conflit = true;
                        }

                        // Cas 2 : Le nouvel entretien se termine pendant un autre entretien
                        if (fin.isAfter(autreDebut) && (fin.isBefore(autreFin) || fin.equals(autreFin))) {
                            conflit = true;
                        }

                        // Cas 3 : Le nouvel entretien englobe complètement un autre
                        if ((debut.isBefore(autreDebut) || debut.equals(autreDebut)) &&
                                (fin.isAfter(autreFin) || fin.equals(autreFin))) {
                            conflit = true;
                        }

                        if (conflit) {
                            String titreOffre = service.getOffreTitre(e.getIdOffre());
                            showAlert(Alert.AlertType.ERROR, "Conflit d'horaire",
                                    "Un entretien existe déjà à cette date et heure :\n\n" +
                                            "Offre : " + titreOffre + "\n" +
                                            "Date : " + e.getDateEntretien().toLocalDate().format(
                                            DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "\n" +
                                            "Horaire : " + autreDebut.format(DateTimeFormatter.ofPattern("HH:mm")) +
                                            " - " + autreFin.format(DateTimeFormatter.ofPattern("HH:mm")) + "\n\n" +
                                            "Veuillez choisir une autre date ou un autre horaire.");
                            return;
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de vérifier les conflits : " + ex.getMessage());
            return;
        }

        // ===== ENREGISTREMENT =====
        if (entretien == null) {
            entretien = new Entretien();
        }

        entretien.setDateEntretien(java.sql.Date.valueOf(dateEntretien.getValue()));
        entretien.setHeureDebut(Time.valueOf(debut));
        entretien.setHeureFin(Time.valueOf(fin));
        entretien.setTypeEntretien(typeEntretien.getValue());
        entretien.setLieu(lieu.getText().trim());
        entretien.setLienVisio(lienVisio.getText().trim());
        entretien.setStatut(statut.getValue());
        entretien.setNoteRecruteur(noteRecruteur.getText().trim());
        entretien.setDateCreation(new Timestamp(System.currentTimeMillis()));
        entretien.setIdRecruteur(1);
        entretien.setIdOffre(10);

        try {
            if (entretien.getIdEntretien() == 0) {
                service.ajouter(entretien);
                showAlert(Alert.AlertType.INFORMATION, "Succès",
                        "Entretien créé avec succès !");
            } else {
                service.update(entretien);
                showAlert(Alert.AlertType.INFORMATION, "Succès",
                        "Entretien modifié avec succès !");
            }

            Stage stage = (Stage) dateEntretien.getScene().getWindow();
            stage.close();

        } catch (SQLException ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur BD",
                    "Impossible d'enregistrer l'entretien : " + ex.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}