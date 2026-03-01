package tn.jobnest.gentretien.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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

    // ── Champs FXML ──────────────────────────────────────────────────────────
    @FXML private DatePicker        dateEntretien;
    @FXML private TextField         heureDebut;
    @FXML private TextField         heureFin;
    @FXML private ComboBox<String>  typeEntretien;
    @FXML private TextField         lieu;
    @FXML private TextField         lienVisio;
    @FXML private ComboBox<String>  statut;
    @FXML private TextField         noteRecruteur;
    @FXML private Button            btnGenererMeet;
    @FXML private Button            btnChoisirSurCarte;
    @FXML private Button            btnOuvrirMeet;   // ← NOUVEAU : bouton "Rejoindre le Meet"

    // ── Sections conditionnelles ─────────────────────────────────────────────
    @FXML private VBox              sectionLieu;      // ← section lieu (présentiel)
    @FXML private VBox              sectionVisio;     // ← section visio
    @FXML private HBox              hintTypeSection;  // ← message "choisissez un type"

    // ── Labels d'info dynamiques (affichés en haut du formulaire) ────────────
    @FXML private Label             lblInfoCandidat;
    @FXML private Label             lblInfoOffre;

    // ── État interne ─────────────────────────────────────────────────────────
    private Entretien               entretien;
    private boolean                 isReorganisation  = false;
    private final Entretienservice  service           = new Entretienservice();

    // ── Contexte dynamique ───────────────────────────────────────────────────
    private int    idCandidatALier   = -1;
    private int    idOffreLiee       = -1;
    private String nomCandidat       = "";
    private String titreOffreLiee    = "";
    private static final int ID_RECRUTEUR_COURANT = 1;

    // ─────────────────────────────────────────────────────────────────────────
    //  INJECTER LE CONTEXTE CANDIDATURE
    // ─────────────────────────────────────────────────────────────────────────
    public void setContextCandidature(int idCandidat, int idOffre,
                                      String nomCandidat, String titreOffre) {
        this.idCandidatALier = idCandidat;
        this.idOffreLiee     = idOffre;
        this.nomCandidat     = nomCandidat != null ? nomCandidat : "";
        this.titreOffreLiee  = titreOffre  != null ? titreOffre  : "";

        if (lblInfoCandidat != null)
            lblInfoCandidat.setText("Candidat : " + this.nomCandidat);
        if (lblInfoOffre != null)
            lblInfoOffre.setText("Offre : " + this.titreOffreLiee);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  INITIALIZE
    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    private void initialize() {
        typeEntretien.getItems().addAll("présentiel", "visio");

        statut.getItems().addAll("proposé", "confirmé");
        statut.setValue("proposé");
        statut.setDisable(true);

        Tooltip tooltipStatut = new Tooltip(
                "Le statut est géré automatiquement.\n" +
                        "• 'proposé'  : à la création\n" +
                        "• 'confirmé' : confirmé par le candidat\n" +
                        "• 'réalisé'  : après que le candidat a rejoint\n" +
                        "• 'annulé'   : si la date est passée sans action"
        );
        tooltipStatut.setStyle("-fx-font-size: 12px;");
        Tooltip.install(statut, tooltipStatut);

        // ── État initial : tout désactivé jusqu'au choix du type ─────────────
        sectionLieu.setDisable(true);
        sectionLieu.setOpacity(0.4);
        sectionVisio.setDisable(true);
        sectionVisio.setOpacity(0.4);
        hintTypeSection.setVisible(true);
        hintTypeSection.setManaged(true);

        // ── Listener sur le type d'entretien ─────────────────────────────────
        typeEntretien.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean isPresential = "présentiel".equals(newVal);
            boolean isVisio      = "visio".equals(newVal);

            // Masquer le hint une fois un type sélectionné
            hintTypeSection.setVisible(false);
            hintTypeSection.setManaged(false);

            // Section LIEU
            sectionLieu.setDisable(!isPresential);
            sectionLieu.setOpacity(isPresential ? 1.0 : 0.4);
            if (!isPresential) lieu.clear();

            // Section VISIO
            sectionVisio.setDisable(!isVisio);
            sectionVisio.setOpacity(isVisio ? 1.0 : 0.4);
            if (!isVisio) {
                lienVisio.clear();
                // Masquer le bouton "Rejoindre"
                if (btnOuvrirMeet != null) {
                    btnOuvrirMeet.setVisible(false);
                    btnOuvrirMeet.setManaged(false);
                }
            }
        });

        // ── Afficher le bouton "Rejoindre" dès qu'un lien est généré ─────────
        lienVisio.textProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasLink = newVal != null && newVal.startsWith("http");
            if (btnOuvrirMeet != null) {
                btnOuvrirMeet.setVisible(hasLink);
                btnOuvrirMeet.setManaged(hasLink);
            }
        });

        // Labels d'info
        if (lblInfoCandidat != null && !nomCandidat.isEmpty())
            lblInfoCandidat.setText("Candidat : " + nomCandidat);
        if (lblInfoOffre != null && !titreOffreLiee.isEmpty())
            lblInfoOffre.setText("Offre : " + titreOffreLiee);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  OUVRIR LE LIEN MEET DANS LE NAVIGATEUR  ← NOUVEAU
    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    private void ouvrirLienMeet() {
        String lien = lienVisio.getText().trim();
        if (lien.isEmpty() || !lien.startsWith("http")) {
            showAlert(Alert.AlertType.WARNING, "Lien invalide",
                    "Aucun lien Meet valide n'est disponible.\nVeuillez d'abord générer le lien.");
            return;
        }
        try {
            Desktop.getDesktop().browse(new URI(lien));
        } catch (Exception ex) {
            // Fallback : afficher le lien
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Lien Meet");
            alert.setHeaderText("Ouvrez ce lien dans votre navigateur :");
            alert.setContentText(lien);

            // Permettre la copie
            javafx.scene.control.TextField tfLink = new javafx.scene.control.TextField(lien);
            tfLink.setEditable(false);
            tfLink.setStyle("-fx-font-size: 11px;");
            alert.getDialogPane().setExpandableContent(tfLink);
            alert.getDialogPane().setExpanded(true);
            alert.showAndWait();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  OUVRIR LE LOCATION PICKER
    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    private void ouvrirLocationPicker() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/tn/jobnest/gentretien/location-picker-view.fxml"));
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
            pickerStage.initModality(Modality.APPLICATION_MODAL);
            pickerStage.initOwner(lieu.getScene().getWindow());
            pickerStage.showAndWait();

            String adresseChoisie = pickerCtrl.getAdresseChoisie();
            if (adresseChoisie != null && !adresseChoisie.isEmpty()) {
                lieu.setText(adresseChoisie);
                lieu.requestFocus();
                lieu.setStyle(lieu.getStyle() + "; -fx-border-color: #00C37A; -fx-border-width: 1.5;");
                javafx.animation.PauseTransition pause =
                        new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
                pause.setOnFinished(e -> lieu.setStyle(
                        "-fx-font-size: 13px; -fx-border-color: #E2E8F0;" +
                                " -fx-border-radius: 10; -fx-background-radius: 10;" +
                                " -fx-border-width: 1.5; -fx-padding: 9 12;"));
                pause.play();
            }
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'ouvrir le sélecteur de lieu : " + ex.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SET ENTRETIEN (mode édition)
    // ─────────────────────────────────────────────────────────────────────────
    public void setEntretien(Entretien e) {
        this.entretien        = e;
        this.isReorganisation = false;
        if (e != null) {
            dateEntretien.setValue(e.getDateEntretien() != null
                    ? e.getDateEntretien().toLocalDate() : null);
            heureDebut.setText(e.getHeureDebut() != null
                    ? e.getHeureDebut().toLocalTime().toString() : "");
            heureFin.setText(e.getHeureFin() != null
                    ? e.getHeureFin().toLocalTime().toString() : "");
            typeEntretien.setValue(e.getTypeEntretien()); // déclenche le listener automatiquement
            lieu.setText(e.getLieu());
            lienVisio.setText(e.getLienVisio());
            String currentStatut = e.getStatut();
            if (currentStatut != null && !statut.getItems().contains(currentStatut))
                statut.getItems().add(currentStatut);
            statut.setValue(currentStatut);
            statut.setDisable(true);
            noteRecruteur.setText(e.getNoteRecruteur());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SET ENTRETIEN POUR RÉORGANISATION
    // ─────────────────────────────────────────────────────────────────────────
    public void setEntretienPourReorganisation(Entretien e) {
        this.entretien        = e;
        this.isReorganisation = true;
        if (e != null) {
            dateEntretien.setValue(null);
            dateEntretien.setPromptText("Choisissez une nouvelle date");
            heureDebut.setText(e.getHeureDebut() != null
                    ? e.getHeureDebut().toLocalTime().toString() : "");
            heureFin.setText(e.getHeureFin() != null
                    ? e.getHeureFin().toLocalTime().toString() : "");
            typeEntretien.setValue(e.getTypeEntretien()); // déclenche le listener
            if ("présentiel".equals(e.getTypeEntretien())) {
                lieu.setText(e.getLieu());
                lienVisio.clear();
            } else {
                lieu.clear();
                lienVisio.clear();
                lienVisio.setPromptText("Générez un nouveau lien Meet pour la nouvelle date");
            }
            if (!statut.getItems().contains("proposé")) statut.getItems().add("proposé");
            statut.setValue("proposé");
            statut.setDisable(true);
            noteRecruteur.setText(e.getNoteRecruteur());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  GÉNÉRER LIEN MEET
    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    private void genererLienMeet() {
        if (dateEntretien.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Champ requis",
                    "Veuillez sélectionner une date avant de générer le lien Meet."); return;
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

            String meetLink = GoogleMeetService.creerMeetingLink(
                    "Entretien JobNest", "Entretien d'embauche planifié via JobNest",
                    dateTimeDebut.format(fmt), dateTimeFin.format(fmt));
            lienVisio.setText(meetLink);  // déclenche le listener → affiche btnOuvrirMeet

            // Proposer d'ouvrir le lien immédiatement
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Lien Meet généré !");
            confirm.setHeaderText("✅ Lien Google Meet créé avec succès");
            confirm.setContentText(meetLink + "\n\nVoulez-vous rejoindre la réunion maintenant ?");
            ButtonType btnOuvrir  = new ButtonType("🔗 Rejoindre");
            ButtonType btnFermer  = new ButtonType("Continuer la saisie");
            confirm.getButtonTypes().setAll(btnOuvrir, btnFermer);
            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == btnOuvrir) {
                ouvrirLienMeet();
            }

        } catch (DateTimeParseException ex) {
            showAlert(Alert.AlertType.ERROR, "Format invalide",
                    "Format d'heure invalide. Utilisez HH:mm (ex: 14:30)");
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur Meet",
                    "Impossible de générer le lien Meet : " + ex.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  VALIDER ADRESSE (ouvre Google Maps)
    // ─────────────────────────────────────────────────────────────────────────
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
            confirmation.setContentText("Adresse : " + adresse + "\n\nVoulez-vous ouvrir Google Maps pour vérifier ?");
            ButtonType btnOui = new ButtonType("Oui, vérifier");
            ButtonType btnNon = new ButtonType("Non, continuer");
            confirmation.getButtonTypes().setAll(btnOui, btnNon);
            Optional<ButtonType> result = confirmation.showAndWait();
            if (result.isPresent() && result.get() == btnOui)
                Desktop.getDesktop().browse(new URI(searchUrl));
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'ouvrir Google Maps : " + ex.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SAUVEGARDER L'ENTRETIEN
    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    private void save() {
        // ── Validation : date ────────────────────────────────────────────────
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

        // ── Validation : heures ──────────────────────────────────────────────
        String hDebutStr = heureDebut.getText().trim();
        String hFinStr   = heureFin.getText().trim();
        if (hDebutStr.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champ obligatoire",
                    "Veuillez renseigner l'heure de début (format HH:mm).");
            heureDebut.requestFocus(); return;
        }
        if (hFinStr.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champ obligatoire",
                    "Veuillez renseigner l'heure de fin (format HH:mm).");
            heureFin.requestFocus(); return;
        }

        // ── Validation : type ────────────────────────────────────────────────
        if (typeEntretien.getValue() == null || typeEntretien.getValue().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champ obligatoire",
                    "Veuillez sélectionner un type d'entretien.");
            typeEntretien.requestFocus(); return;
        }

        // ── Validation conditionnelle : lieu ou lien visio ───────────────────
        if ("présentiel".equals(typeEntretien.getValue())) {
            if (lieu.getText() == null || lieu.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Champ obligatoire",
                        "Pour un entretien présentiel, le lieu est obligatoire.\n" +
                                "Utilisez le bouton carte pour sélectionner un lieu.");
                lieu.requestFocus(); return;
            }
        } else if ("visio".equals(typeEntretien.getValue())) {
            if (lienVisio.getText() == null || lienVisio.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Champ obligatoire",
                        "Pour un entretien en visio, le lien de conférence est obligatoire.\n" +
                                "Cliquez sur '📹 Meet' pour générer un lien Google Meet.");
                lienVisio.requestFocus(); return;
            }
        }

        // ── Parse des heures ─────────────────────────────────────────────────
        LocalTime debut, fin;
        try {
            debut = LocalTime.parse(hDebutStr);
            fin   = LocalTime.parse(hFinStr);
        } catch (DateTimeParseException ex) {
            showAlert(Alert.AlertType.ERROR, "Format invalide",
                    "Format d'heure invalide. Utilisez HH:mm (ex: 14:30)"); return;
        }
        if (fin.isBefore(debut) || fin.equals(debut)) {
            showAlert(Alert.AlertType.WARNING, "Incohérence horaire",
                    "L'heure de fin doit être strictement après l'heure de début.");
            heureFin.requestFocus(); return;
        }

        // ── Vérification conflits horaires ───────────────────────────────────
        try {
            List<Entretien> tousEntretiens = service.afficher();
            for (Entretien e : tousEntretiens) {
                if (entretien != null && e.getIdEntretien() == entretien.getIdEntretien()) continue;
                if (e.getDateEntretien() != null
                        && e.getDateEntretien().toLocalDate().equals(dateEntretien.getValue())) {
                    if (e.getHeureDebut() != null && e.getHeureFin() != null) {
                        LocalTime autreDebut = e.getHeureDebut().toLocalTime();
                        LocalTime autreFin   = e.getHeureFin().toLocalTime();
                        boolean conflit =
                                (debut.isAfter(autreDebut) || debut.equals(autreDebut)) && debut.isBefore(autreFin) ||
                                        fin.isAfter(autreDebut) && (fin.isBefore(autreFin) || fin.equals(autreFin)) ||
                                        (debut.isBefore(autreDebut) || debut.equals(autreDebut)) && (fin.isAfter(autreFin) || fin.equals(autreFin));
                        if (conflit) {
                            String titreOffre = service.getOffreTitre(e.getIdOffre());
                            showAlert(Alert.AlertType.ERROR, "Conflit d'horaire",
                                    "Un entretien existe déjà à cette date et heure :\n\n" +
                                            "Offre : " + titreOffre + "\n" +
                                            "Date : " + e.getDateEntretien().toLocalDate()
                                            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "\n" +
                                            "Horaire : " + autreDebut.format(DateTimeFormatter.ofPattern("HH:mm")) +
                                            " - " + autreFin.format(DateTimeFormatter.ofPattern("HH:mm")) +
                                            "\n\nVeuillez choisir une autre date ou un autre horaire.");
                            return;
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de vérifier les conflits : " + ex.getMessage()); return;
        }

        // ── Construction de l'objet Entretien ────────────────────────────────
        if (entretien == null) entretien = new Entretien();
        entretien.setDateEntretien(java.sql.Date.valueOf(dateEntretien.getValue()));
        entretien.setHeureDebut(Time.valueOf(debut));
        entretien.setHeureFin(Time.valueOf(fin));
        entretien.setTypeEntretien(typeEntretien.getValue());
        entretien.setLieu(lieu.getText() != null ? lieu.getText().trim() : "");
        entretien.setLienVisio(lienVisio.getText() != null ? lienVisio.getText().trim() : "");
        entretien.setNoteRecruteur(noteRecruteur.getText() != null ? noteRecruteur.getText().trim() : "");
        entretien.setDateCreation(new Timestamp(System.currentTimeMillis()));
        entretien.setIdRecruteur(ID_RECRUTEUR_COURANT);

        if (idOffreLiee > 0) {
            entretien.setIdOffre(idOffreLiee);
        } else if (entretien.getIdOffre() == 0) {
            entretien.setIdOffre(10); // fallback
        }

        if (entretien.getIdEntretien() == 0 || isReorganisation) {
            entretien.setStatut("proposé");
        }

        // ── Enregistrement ───────────────────────────────────────────────────
        try {
            if (entretien.getIdEntretien() == 0) {
                int idNouvelEntretien = service.ajouterEtRetournerId(entretien);
                if (idNouvelEntretien > 0 && idCandidatALier > 0) {
                    service.ajouterParticipant(idNouvelEntretien, idCandidatALier);
                }
                showAlert(Alert.AlertType.INFORMATION, "Succès",
                        "Entretien créé avec succès !\n" +
                                (idCandidatALier > 0 ? "Candidat « " + nomCandidat + " » ajouté comme participant." : ""));
            } else if (isReorganisation) {
                service.update(entretien);
                showAlert(Alert.AlertType.INFORMATION, "Réorganisation réussie",
                        "L'entretien a été réorganisé avec succès !\nLe statut est repassé à 'proposé'.");
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

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPER
    // ─────────────────────────────────────────────────────────────────────────
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}