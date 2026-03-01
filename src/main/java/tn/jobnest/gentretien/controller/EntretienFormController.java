package tn.jobnest.gentretien.controller;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
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
    @FXML private Button            btnChoisirSurCarte;
    @FXML private Button            btnOuvrirMeet;
    @FXML private VBox              sectionLieu;
    @FXML private VBox              sectionVisio;
    @FXML private HBox              hintTypeSection;
    @FXML private Label             lblInfoCandidat;
    @FXML private Label             lblInfoOffre;

    private Entretien              entretien;
    private boolean                isReorganisation  = false;
    private final Entretienservice service           = new Entretienservice();
    private int    idCandidatALier  = -1;
    private int    idOffreLiee      = -1;
    private String nomCandidat      = "";
    private String titreOffreLiee   = "";
    private static final int ID_RECRUTEUR_COURANT = 1;

    // ─────────────────────────────────────────────────────────────────────────
    public void setContextCandidature(int idCandidat, int idOffre, String nomCandidat, String titreOffre) {
        this.idCandidatALier  = idCandidat;
        this.idOffreLiee      = idOffre;
        this.nomCandidat      = nomCandidat  != null ? nomCandidat  : "";
        this.titreOffreLiee   = titreOffre   != null ? titreOffre   : "";
        if (lblInfoCandidat != null) lblInfoCandidat.setText("Candidat : " + this.nomCandidat);
        if (lblInfoOffre    != null) lblInfoOffre   .setText("Offre : "    + this.titreOffreLiee);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    private void initialize() {
        typeEntretien.getItems().addAll("présentiel", "visio");
        statut.getItems().addAll("proposé", "confirmé");
        statut.setValue("proposé");
        statut.setDisable(true);
        Tooltip.install(statut, new Tooltip(
                "• 'proposé'  : à la création\n• 'confirmé' : confirmé par le candidat\n" +
                        "• 'réalisé'  : après que le candidat a rejoint\n• 'annulé'   : date passée sans action"));

        sectionLieu .setDisable(true);  sectionLieu .setOpacity(0.4);
        sectionVisio.setDisable(true);  sectionVisio.setOpacity(0.4);
        hintTypeSection.setVisible(true); hintTypeSection.setManaged(true);

        typeEntretien.valueProperty().addListener((obs, ov, nv) -> {
            boolean isP = "présentiel".equals(nv), isV = "visio".equals(nv);
            hintTypeSection.setVisible(false); hintTypeSection.setManaged(false);
            sectionLieu .setDisable(!isP); sectionLieu .setOpacity(isP ? 1.0 : 0.4);
            sectionVisio.setDisable(!isV); sectionVisio.setOpacity(isV ? 1.0 : 0.4);
            if (!isP) lieu.clear();
            if (!isV) { lienVisio.clear(); hideMeetBtn(); }
        });

        lienVisio.textProperty().addListener((obs, ov, nv) -> {
            boolean has = nv != null && nv.startsWith("http");
            if (btnOuvrirMeet != null) { btnOuvrirMeet.setVisible(has); btnOuvrirMeet.setManaged(has); }
        });

        if (lblInfoCandidat != null && !nomCandidat.isEmpty())
            lblInfoCandidat.setText("Candidat : " + nomCandidat);
        if (lblInfoOffre != null && !titreOffreLiee.isEmpty())
            lblInfoOffre.setText("Offre : " + titreOffreLiee);
    }

    private void hideMeetBtn() {
        if (btnOuvrirMeet != null) { btnOuvrirMeet.setVisible(false); btnOuvrirMeet.setManaged(false); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    private void ouvrirLienMeet() {
        String lien = lienVisio.getText().trim();
        if (lien.isEmpty() || !lien.startsWith("http")) {
            showStyledAlert("warning", "Lien invalide", "Aucun lien Meet valide",
                    "Veuillez d'abord générer le lien en cliquant sur '📹 Meet'.");
            return;
        }
        try { Desktop.getDesktop().browse(new URI(lien)); }
        catch (Exception ex) { showStyledAlert("error", "Erreur", "Impossible d'ouvrir le navigateur", lien); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    private void ouvrirLocationPicker() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/tn/jobnest/gentretien/location-picker-view.fxml"));
            Parent root = loader.load();
            LocationPickerController ctrl = loader.getController();
            Stage s = new Stage();
            s.setTitle("JobNest - Choisir le lieu");
            s.setScene(new Scene(root));
            s.setMinWidth(640); s.setMinHeight(480); s.setWidth(860); s.setHeight(620);
            s.centerOnScreen(); s.initModality(Modality.APPLICATION_MODAL);
            s.initOwner(lieu.getScene().getWindow()); s.showAndWait();
            String addr = ctrl.getAdresseChoisie();
            if (addr != null && !addr.isEmpty()) {
                lieu.setText(addr);
                lieu.setStyle("-fx-font-size:13px;-fx-border-color:#00C37A;-fx-border-radius:10;" +
                        "-fx-background-radius:10;-fx-border-width:1.5;-fx-padding:9 12;");
                javafx.animation.PauseTransition p =
                        new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
                p.setOnFinished(e -> lieu.setStyle("-fx-font-size:13px;-fx-border-color:#E2E8F0;" +
                        "-fx-border-radius:10;-fx-background-radius:10;-fx-border-width:1.5;-fx-padding:9 12;"));
                p.play();
            }
        } catch (IOException ex) {
            showStyledAlert("error", "Erreur", "Impossible d'ouvrir le sélecteur", ex.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    public void setEntretien(Entretien e) {
        this.entretien = e; this.isReorganisation = false;
        if (e == null) return;
        dateEntretien.setValue(e.getDateEntretien() != null ? e.getDateEntretien().toLocalDate() : null);
        heureDebut.setText(e.getHeureDebut() != null ? e.getHeureDebut().toLocalTime().toString() : "");
        heureFin  .setText(e.getHeureFin()   != null ? e.getHeureFin()  .toLocalTime().toString() : "");
        typeEntretien.setValue(e.getTypeEntretien());
        lieu     .setText(e.getLieu());
        lienVisio.setText(e.getLienVisio());
        String cs = e.getStatut();
        if (cs != null && !statut.getItems().contains(cs)) statut.getItems().add(cs);
        statut.setValue(cs); statut.setDisable(true);
        noteRecruteur.setText(e.getNoteRecruteur());
    }

    // ─────────────────────────────────────────────────────────────────────────
    public void setEntretienPourReorganisation(Entretien e) {
        this.entretien = e; this.isReorganisation = true;
        if (e == null) return;
        dateEntretien.setValue(null); dateEntretien.setPromptText("Choisissez une nouvelle date");
        heureDebut.setText(e.getHeureDebut() != null ? e.getHeureDebut().toLocalTime().toString() : "");
        heureFin  .setText(e.getHeureFin()   != null ? e.getHeureFin()  .toLocalTime().toString() : "");
        typeEntretien.setValue(e.getTypeEntretien());
        if ("présentiel".equals(e.getTypeEntretien())) { lieu.setText(e.getLieu()); lienVisio.clear(); }
        else { lieu.clear(); lienVisio.clear(); lienVisio.setPromptText("Générez un nouveau lien Meet"); }
        if (!statut.getItems().contains("proposé")) statut.getItems().add("proposé");
        statut.setValue("proposé"); statut.setDisable(true);
        noteRecruteur.setText(e.getNoteRecruteur());
    }

    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    private void genererLienMeet() {
        if (dateEntretien.getValue() == null) {
            showStyledAlert("warning", "Champ requis", "Date manquante",
                    "Veuillez sélectionner une date avant de générer le lien Meet."); return; }
        if (dateEntretien.getValue().isBefore(LocalDate.now())) {
            showStyledAlert("warning", "Date invalide", "La date est dans le passé",
                    "Impossible de créer un entretien dans le passé."); return; }
        String hD = heureDebut.getText().trim(), hF = heureFin.getText().trim();
        if (hD.isEmpty() || hF.isEmpty()) {
            showStyledAlert("warning", "Champs requis", "Heures manquantes",
                    "Les heures de début et fin sont requises pour générer le lien Meet."); return; }
        try {
            LocalTime debut = LocalTime.parse(hD), fin = LocalTime.parse(hF);
            if (fin.isBefore(debut) || fin.equals(debut)) {
                showStyledAlert("warning", "Incohérence", "Heure de fin invalide",
                        "L'heure de fin doit être après l'heure de début."); return; }
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            String meetLink = GoogleMeetService.creerMeetingLink("Entretien JobNest",
                    "Entretien d'embauche planifié via JobNest",
                    LocalDateTime.of(dateEntretien.getValue(), debut).format(fmt),
                    LocalDateTime.of(dateEntretien.getValue(), fin).format(fmt));
            lienVisio.setText(meetLink);
            showMeetSuccessDialog(meetLink);
        } catch (DateTimeParseException ex) {
            showStyledAlert("error", "Format invalide", "Format d'heure incorrect", "Utilisez HH:mm (ex: 14:30)");
        } catch (Exception ex) {
            showStyledAlert("error", "Erreur Meet", "Impossible de générer le lien", ex.getMessage()); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    private void validerAdresse() {
        String adresse = lieu.getText().trim();
        if (adresse.isEmpty()) {
            showStyledAlert("warning", "Champ vide", "Aucune adresse saisie",
                    "Veuillez saisir ou sélectionner une adresse."); return; }
        try {
            String url = "https://www.google.com/maps/search/?api=1&query="
                    + java.net.URLEncoder.encode(adresse, "UTF-8");
            if (showStyledConfirm("Vérifier", "Ouvrir Google Maps ?", "Adresse : " + adresse,
                    "Oui, vérifier", "Non, continuer"))
                Desktop.getDesktop().browse(new URI(url));
        } catch (Exception ex) {
            showStyledAlert("error", "Erreur", "Impossible d'ouvrir Google Maps", ex.getMessage()); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SAVE — popup récap → si confirmé → enregistrement → fermeture directe
    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    private void save() {
        if (dateEntretien.getValue() == null) {
            showStyledAlert("warning", "Champ obligatoire", "Date manquante",
                    "Veuillez sélectionner une date d'entretien.");
            dateEntretien.requestFocus(); return; }
        if (dateEntretien.getValue().isBefore(LocalDate.now())) {
            showStyledAlert("warning", "Date invalide", "La date est dans le passé",
                    "Veuillez choisir une date future.");
            dateEntretien.requestFocus(); return; }
        String hD = heureDebut.getText().trim(), hF = heureFin.getText().trim();
        if (hD.isEmpty()) {
            showStyledAlert("warning", "Champ obligatoire", "Heure de début manquante",
                    "Veuillez renseigner l'heure de début (HH:mm).");
            heureDebut.requestFocus(); return; }
        if (hF.isEmpty()) {
            showStyledAlert("warning", "Champ obligatoire", "Heure de fin manquante",
                    "Veuillez renseigner l'heure de fin (HH:mm).");
            heureFin.requestFocus(); return; }
        if (typeEntretien.getValue() == null || typeEntretien.getValue().isEmpty()) {
            showStyledAlert("warning", "Champ obligatoire", "Type non sélectionné",
                    "Veuillez sélectionner un type d'entretien.");
            typeEntretien.requestFocus(); return; }
        if ("présentiel".equals(typeEntretien.getValue()) &&
                (lieu.getText() == null || lieu.getText().trim().isEmpty())) {
            showStyledAlert("warning", "Champ obligatoire", "Lieu manquant",
                    "Pour un entretien présentiel, le lieu est obligatoire.");
            lieu.requestFocus(); return; }
        if ("visio".equals(typeEntretien.getValue()) &&
                (lienVisio.getText() == null || lienVisio.getText().trim().isEmpty())) {
            showStyledAlert("warning", "Champ obligatoire", "Lien visio manquant",
                    "Cliquez sur '📹 Meet' pour générer un lien.");
            lienVisio.requestFocus(); return; }

        LocalTime debut, fin;
        try { debut = LocalTime.parse(hD); fin = LocalTime.parse(hF); }
        catch (DateTimeParseException ex) {
            showStyledAlert("error", "Format invalide", "Format d'heure incorrect",
                    "Utilisez HH:mm (ex: 14:30)"); return; }
        if (fin.isBefore(debut) || fin.equals(debut)) {
            showStyledAlert("warning", "Incohérence", "Heure de fin invalide",
                    "L'heure de fin doit être après l'heure de début.");
            heureFin.requestFocus(); return; }

        try {
            for (Entretien e : service.afficher()) {
                if (entretien != null && e.getIdEntretien() == entretien.getIdEntretien()) continue;
                if (e.getDateEntretien() != null
                        && e.getDateEntretien().toLocalDate().equals(dateEntretien.getValue())
                        && e.getHeureDebut() != null && e.getHeureFin() != null) {
                    LocalTime ad = e.getHeureDebut().toLocalTime(), af = e.getHeureFin().toLocalTime();
                    boolean conflit =
                            ((debut.isAfter(ad) || debut.equals(ad)) && debut.isBefore(af)) ||
                                    (fin.isAfter(ad) && (fin.isBefore(af) || fin.equals(af))) ||
                                    ((debut.isBefore(ad) || debut.equals(ad)) && (fin.isAfter(af) || fin.equals(af)));
                    if (conflit) {
                        showStyledAlert("error", "Conflit", "Créneau déjà occupé",
                                "Offre : " + service.getOffreTitre(e.getIdOffre()) + "\n" +
                                        "Horaire : " + ad.format(DateTimeFormatter.ofPattern("HH:mm")) +
                                        " → " + af.format(DateTimeFormatter.ofPattern("HH:mm")));
                        return; }
                }
            }
        } catch (SQLException ex) {
            showStyledAlert("error", "Erreur BD", "Vérification impossible", ex.getMessage()); return; }

        // Infos pour le récapitulatif
        boolean estVisio = "visio".equals(typeEntretien.getValue());
        String dateStr   = dateEntretien.getValue()
                .format(DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy"));
        String detailStr = estVisio ? lienVisio.getText().trim() : lieu.getText().trim();
        String mode = (entretien != null && entretien.getIdEntretien() != 0)
                ? (isReorganisation ? "réorganiser" : "modifier") : "créer";

        // ── Afficher le popup récapitulatif ───────────────────────────────
        boolean confirme = showEntretienConfirmDialog(
                nomCandidat.isEmpty()    ? "Candidat" : nomCandidat,
                titreOffreLiee.isEmpty() ? "—"        : titreOffreLiee,
                dateStr,
                hD + " → " + hF,
                estVisio ? "💻 Visioconférence" : "🏢 Présentiel",
                detailStr.isEmpty() ? "—" : detailStr,
                mode);

        // ✅ Si l'utilisateur a annulé dans le popup → on reste sur le formulaire
        if (!confirme) return;

        // ── Enregistrement en base ────────────────────────────────────────
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
        if (idOffreLiee > 0) entretien.setIdOffre(idOffreLiee);
        else if (entretien.getIdOffre() == 0) entretien.setIdOffre(10);
        if (entretien.getIdEntretien() == 0 || isReorganisation) entretien.setStatut("proposé");

        try {
            if (entretien.getIdEntretien() == 0) {
                int idNew = service.ajouterEtRetournerId(entretien);
                if (idNew > 0 && idCandidatALier > 0)
                    service.ajouterParticipant(idNew, idCandidatALier);
            } else {
                service.update(entretien);
            }
            // ✅ Fermer le formulaire directement — aucun popup supplémentaire
            Stage stage = (Stage) dateEntretien.getScene().getWindow();
            stage.close();
        } catch (SQLException ex) {
            showStyledAlert("error", "Erreur BD", "Enregistrement impossible", ex.getMessage()); }
    }

    // =========================================================================
    //  POPUP RÉCAPITULATIF — design system JobNest (#1E3A5F / #2563EB / #F97316)
    //  ✅ CORRECTION : utilisation de showAndWait() UNIQUEMENT (sans show())
    //     L'ancien code faisait dialog.show() + dialog.showAndWait() ce qui
    //     causait un conflit : result[0] restait false → save() retournait
    //     sans sauvegarder et le formulaire restait ouvert.
    // =========================================================================
    private boolean showEntretienConfirmDialog(
            String candidat, String offre,
            String date, String horaire, String type, String detail, String mode) {

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setResizable(false);

        // ── Conteneur principal ───────────────────────────────────────────
        VBox root = new VBox(0);
        root.setPrefWidth(520);
        root.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 18px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(15,36,68,0.38), 52, 0, 0, 14);");

        // ═══ Bande dégradée orange→bleu (identique au formulaire)
        Region accentBar = new Region();
        accentBar.setPrefHeight(5);
        accentBar.setStyle(
                "-fx-background-color: linear-gradient(to right, #F97316, #2563EB);" +
                        "-fx-background-radius: 18px 18px 0 0;");

        // ═══ Header bleu marine (identique au formulaire)
        VBox header = new VBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #1E3A5F; -fx-padding: 24 30 22 30;");

        // Ligne titre : icône ronde + texte
        HBox titleRow = new HBox(14);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        StackPane iconWrapper = new StackPane();
        iconWrapper.setPrefSize(48, 48);
        iconWrapper.setMaxSize(48, 48);
        Circle iconCircleOuter = new Circle(24);
        iconCircleOuter.setStyle("-fx-fill: rgba(37,99,235,0.35);");
        Circle iconCircleInner = new Circle(18);
        iconCircleInner.setStyle("-fx-fill: rgba(37,99,235,0.55);");
        Label iconEmoji = new Label("📋");
        iconEmoji.setStyle("-fx-font-size: 20px;");
        iconWrapper.getChildren().addAll(iconCircleOuter, iconCircleInner, iconEmoji);

        VBox titleTexts = new VBox(5);
        String cap = mode.substring(0, 1).toUpperCase() + mode.substring(1);
        Label titleMain = new Label("Confirmation de l'entretien");
        titleMain.setStyle("-fx-font-size: 19px; -fx-font-weight: 800; -fx-text-fill: white;");
        Label titleSub = new Label("Vérifiez attentivement avant de " + mode);
        titleSub.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.55);");
        titleTexts.getChildren().addAll(titleMain, titleSub);
        titleRow.getChildren().addAll(iconWrapper, titleTexts);

        // Bandeau CANDIDAT / OFFRE
        HBox infoStrip = new HBox(0);
        infoStrip.setAlignment(Pos.CENTER_LEFT);
        infoStrip.setStyle(
                "-fx-background-color: rgba(255,255,255,0.09);" +
                        "-fx-background-radius: 10px;" +
                        "-fx-padding: 12 18 12 18;" +
                        "-fx-border-color: rgba(255,255,255,0.12);" +
                        "-fx-border-radius: 10px;" +
                        "-fx-border-width: 1;");
        VBox.setMargin(infoStrip, new Insets(4, 0, 0, 0));

        VBox colCand = new VBox(3);
        HBox.setHgrow(colCand, Priority.ALWAYS);
        Label kCand = new Label("CANDIDAT");
        kCand.setStyle("-fx-font-size: 9px; -fx-font-weight: 700; -fx-text-fill: rgba(255,255,255,0.45);");
        Label vCand = new Label(candidat);
        vCand.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: white;");
        colCand.getChildren().addAll(kCand, vCand);

        Region divV = new Region();
        divV.setPrefWidth(1);
        divV.setPrefHeight(36);
        divV.setStyle("-fx-background-color: rgba(255,255,255,0.18);");
        HBox.setMargin(divV, new Insets(0, 18, 0, 18));

        VBox colOffre = new VBox(3);
        HBox.setHgrow(colOffre, Priority.ALWAYS);
        Label kOffre = new Label("OFFRE");
        kOffre.setStyle("-fx-font-size: 9px; -fx-font-weight: 700; -fx-text-fill: rgba(255,255,255,0.45);");
        Label vOffre = new Label(offre);
        vOffre.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #60A5FA;");
        colOffre.getChildren().addAll(kOffre, vOffre);

        infoStrip.getChildren().addAll(colCand, divV, colOffre);
        header.getChildren().addAll(titleRow, infoStrip);

        // ═══ Corps récapitulatif
        VBox body = new VBox(6);
        body.setStyle("-fx-background-color: #F8FAFC; -fx-padding: 24 30 20 30;");

        HBox sectionHeader = new HBox(8);
        sectionHeader.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(sectionHeader, new Insets(0, 0, 8, 0));
        Region sectionAccent = new Region();
        sectionAccent.setPrefWidth(3);
        sectionAccent.setPrefHeight(16);
        sectionAccent.setStyle("-fx-background-color: #2563EB; -fx-background-radius: 2px;");
        Label sectionLabel = new Label("RÉCAPITULATIF DES INFORMATIONS");
        sectionLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #94A3B8;");
        sectionHeader.getChildren().addAll(sectionAccent, sectionLabel);
        body.getChildren().add(sectionHeader);

        // Grille 2×2
        HBox row1 = new HBox(6);
        row1.getChildren().addAll(
                buildRecapCard("📅", "Date",    date,    true),
                buildRecapCard("⏱", "Horaire", horaire, false)
        );
        HBox row2 = new HBox(6);
        row2.getChildren().addAll(
                buildRecapCard("🏷", "Type",        type,   false),
                buildRecapCard("📍", "Lieu / Lien", detail, true)
        );
        body.getChildren().addAll(row1, row2);

        // Note optionnelle
        if (noteRecruteur != null && noteRecruteur.getText() != null
                && !noteRecruteur.getText().trim().isEmpty()) {
            HBox noteRow = new HBox(0);
            noteRow.setAlignment(Pos.CENTER_LEFT);
            VBox.setMargin(noteRow, new Insets(4, 0, 0, 0));
            noteRow.setStyle(
                    "-fx-background-color: #FFFBEB;" +
                            "-fx-background-radius: 10px; -fx-padding: 10 14 10 14;" +
                            "-fx-border-color: #FDE68A; -fx-border-radius: 10px; -fx-border-width: 1;");
            Label noteIcon = new Label("📝  ");
            noteIcon.setStyle("-fx-font-size: 13px;");
            Label noteKey = new Label("Note : ");
            noteKey.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #92400E;");
            Label noteVal = new Label(noteRecruteur.getText().trim());
            noteVal.setStyle("-fx-font-size: 12px; -fx-text-fill: #78350F;");
            noteVal.setWrapText(true);
            noteVal.setMaxWidth(420);
            noteRow.getChildren().addAll(noteIcon, noteKey, noteVal);
            body.getChildren().add(noteRow);
        }

        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setStyle("-fx-background-color: #E2E8F0;");
        VBox.setMargin(divider, new Insets(12, 0, 0, 0));
        body.getChildren().add(divider);

        // ═══ Footer boutons
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle(
                "-fx-background-color: white;" +
                        "-fx-padding: 16 30 22 30;" +
                        "-fx-background-radius: 0 0 18px 18px;");

        Label infoTxt = new Label("⚠  Cette action est irréversible.");
        infoTxt.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8; -fx-font-style: italic;");
        HBox.setHgrow(infoTxt, Priority.ALWAYS);

        Button btnAnnuler = new Button("✕  Annuler");
        btnAnnuler.setPrefWidth(120);
        btnAnnuler.setPrefHeight(44);
        btnAnnuler.setStyle(
                "-fx-background-color: #F1F5F9; -fx-text-fill: #475569;" +
                        "-fx-font-size: 13px; -fx-font-weight: 700; -fx-background-radius: 10px;" +
                        "-fx-cursor: hand; -fx-border-color: #CBD5E1;" +
                        "-fx-border-width: 1.5; -fx-border-radius: 10px;");
        btnAnnuler.setOnMouseEntered(e -> btnAnnuler.setStyle(
                "-fx-background-color: #E2E8F0; -fx-text-fill: #334155;" +
                        "-fx-font-size: 13px; -fx-font-weight: 700; -fx-background-radius: 10px;" +
                        "-fx-cursor: hand; -fx-border-color: #94A3B8;" +
                        "-fx-border-width: 1.5; -fx-border-radius: 10px;"));
        btnAnnuler.setOnMouseExited(e -> btnAnnuler.setStyle(
                "-fx-background-color: #F1F5F9; -fx-text-fill: #475569;" +
                        "-fx-font-size: 13px; -fx-font-weight: 700; -fx-background-radius: 10px;" +
                        "-fx-cursor: hand; -fx-border-color: #CBD5E1;" +
                        "-fx-border-width: 1.5; -fx-border-radius: 10px;"));

        String confirmBaseStyle =
                "-fx-background-color: linear-gradient(from 0% 0% to 100% 0%, #1E3A5F, #2563EB);" +
                        "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: 800;" +
                        "-fx-background-radius: 10px; -fx-cursor: hand; -fx-padding: 0 24;" +
                        "-fx-effect: dropshadow(gaussian, rgba(37,99,235,0.45), 14, 0, 0, 4);";
        String confirmHoverStyle =
                "-fx-background-color: linear-gradient(from 0% 0% to 100% 0%, #162D4A, #1D4ED8);" +
                        "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: 800;" +
                        "-fx-background-radius: 10px; -fx-cursor: hand; -fx-padding: 0 24;" +
                        "-fx-effect: dropshadow(gaussian, rgba(37,99,235,0.62), 18, 0, 0, 6);";

        Button btnConfirmer = new Button("✅  " + cap + " l'entretien");
        btnConfirmer.setPrefHeight(44);
        btnConfirmer.setStyle(confirmBaseStyle);
        btnConfirmer.setOnMouseEntered(e -> btnConfirmer.setStyle(confirmHoverStyle));
        btnConfirmer.setOnMouseExited(e  -> btnConfirmer.setStyle(confirmBaseStyle));

        final boolean[] result = {false};
        btnAnnuler  .setOnAction(e -> dialog.close());
        btnConfirmer.setOnAction(e -> { result[0] = true; dialog.close(); });

        footer.getChildren().addAll(infoTxt, btnAnnuler, btnConfirmer);

        // ═══ Assemblage
        root.getChildren().addAll(accentBar, header, body, footer);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);

        // ✅ Animation fade-in SANS dialog.show() — on applique l'opacité
        //    initiale à 0, puis on démarre le fade APRÈS showAndWait()
        //    via un listener sur la propriété showing du Stage.
        root.setOpacity(0);
        dialog.showingProperty().addListener((obs, wasShowing, isNowShowing) -> {
            if (isNowShowing) {
                FadeTransition ft = new FadeTransition(Duration.millis(200), root);
                ft.setFromValue(0);
                ft.setToValue(1);
                ft.play();
            }
        });

        makeDraggable(dialog, root);

        // ✅ showAndWait() UNIQUEMENT — bloque jusqu'à ce que l'utilisateur
        //    clique sur Confirmer ou Annuler, puis retourne result[0]
        dialog.showAndWait();

        return result[0];
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Carte individuelle du récapitulatif
    // ─────────────────────────────────────────────────────────────────────────
    private VBox buildRecapCard(String emoji, String labelTxt, String valueTxt, boolean wide) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.TOP_LEFT);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12px;" +
                        "-fx-padding: 14 16 14 16;" +
                        "-fx-border-color: #EEF2FF;" +
                        "-fx-border-radius: 12px;" +
                        "-fx-border-width: 1;" +
                        "-fx-effect: dropshadow(gaussian, rgba(30,58,95,0.06), 8, 0, 0, 2);");
        if (wide) HBox.setHgrow(card, Priority.ALWAYS);
        card.setPrefWidth(wide ? 260 : 210);

        HBox iconRow = new HBox(8);
        iconRow.setAlignment(Pos.CENTER_LEFT);

        StackPane iconBox = new StackPane();
        iconBox.setPrefSize(32, 32);
        iconBox.setMaxSize(32, 32);
        Region iconBg = new Region();
        iconBg.setMinSize(32, 32);
        iconBg.setMaxSize(32, 32);
        iconBg.setStyle("-fx-background-color: #EFF6FF; -fx-background-radius: 8px;");
        Label iconLbl = new Label(emoji);
        iconLbl.setStyle("-fx-font-size: 14px;");
        iconBox.getChildren().addAll(iconBg, iconLbl);

        Label lbl = new Label(labelTxt.toUpperCase());
        lbl.setStyle("-fx-font-size: 9px; -fx-font-weight: 800; -fx-text-fill: #94A3B8;");
        iconRow.getChildren().addAll(iconBox, lbl);

        Label val = new Label(valueTxt);
        val.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #1E3A5F;");
        val.setWrapText(true);
        val.setMaxWidth(220);

        card.getChildren().addAll(iconRow, val);
        return card;
    }

    // =========================================================================
    //  POPUP MEET
    // =========================================================================
    private void showMeetSuccessDialog(String meetLink) {
        Stage d = new Stage();
        d.initModality(Modality.APPLICATION_MODAL); d.initStyle(StageStyle.UNDECORATED);
        VBox root = new VBox(0); root.setPrefWidth(430);
        root.setStyle("-fx-background-color:white;-fx-background-radius:16px;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.25),32,0,0,6);");
        Region ab = new Region(); ab.setPrefHeight(4);
        ab.setStyle("-fx-background-color: linear-gradient(to right,#F97316,#2563EB);" +
                "-fx-background-radius:16px 16px 0 0;");
        VBox hdr = new VBox(8); hdr.setAlignment(Pos.CENTER);
        hdr.setStyle("-fx-background-color:#1E3A5F;-fx-padding:22 28 18 28;");
        hdr.getChildren().addAll(
                new Label("📹") {{ setStyle("-fx-font-size:34px;"); }},
                new Label("Lien Google Meet généré !") {{
                    setStyle("-fx-font-size:17px;-fx-font-weight:800;-fx-text-fill:white;"); }});
        VBox body = new VBox(10);
        body.setStyle("-fx-padding:18 28 12 28;-fx-background-color:#F8FAFC;");
        body.getChildren().add(new Label("Votre lien de réunion :") {{
            setStyle("-fx-font-size:12px;-fx-font-weight:700;-fx-text-fill:#475569;"); }});
        HBox lb = new HBox(8); lb.setAlignment(Pos.CENTER_LEFT);
        lb.setStyle("-fx-background-color:white;-fx-border-color:#BFDBFE;-fx-border-radius:10;" +
                "-fx-border-width:1.5;-fx-background-radius:10;-fx-padding:10 12;");
        TextField lf = new TextField(meetLink); lf.setEditable(false);
        lf.setStyle("-fx-background-color:transparent;-fx-border-color:transparent;" +
                "-fx-font-size:11px;-fx-text-fill:#1E3A5F;");
        HBox.setHgrow(lf, Priority.ALWAYS);
        lb.getChildren().addAll(new Label("🔗") {{ setStyle("-fx-font-size:14px;"); }}, lf);
        body.getChildren().add(lb);
        HBox footer = new HBox(10); footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle("-fx-padding:12 28 20 28;-fx-background-color:white;" +
                "-fx-background-radius:0 0 16px 16px;");
        Button bc = new Button("📋 Copier"); bc.setPrefHeight(40);
        bc.setStyle("-fx-background-color:#EFF6FF;-fx-text-fill:#2563EB;-fx-font-weight:700;" +
                "-fx-font-size:12px;-fx-background-radius:10;-fx-cursor:hand;" +
                "-fx-border-color:#BFDBFE;-fx-border-width:1.5;-fx-border-radius:10;");
        bc.setOnAction(ev -> {
            javafx.scene.input.Clipboard cb = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent cc2 = new javafx.scene.input.ClipboardContent();
            cc2.putString(meetLink); cb.setContent(cc2);
            bc.setText("✅ Copié !");
            bc.setStyle("-fx-background-color:#D1FAE5;-fx-text-fill:#059669;" +
                    "-fx-font-weight:700;-fx-font-size:12px;-fx-background-radius:10;-fx-cursor:hand;");
        });
        Button bo = new Button("🔗 Rejoindre"); bo.setPrefHeight(40);
        bo.setStyle("-fx-background-color:#2563EB;-fx-text-fill:white;-fx-font-weight:700;" +
                "-fx-font-size:13px;-fx-background-radius:10;-fx-cursor:hand;" +
                "-fx-effect:dropshadow(gaussian,rgba(37,99,235,0.4),8,0,0,3);");
        bo.setOnAction(ev -> { try { Desktop.getDesktop().browse(new URI(meetLink)); } catch (Exception ignored) {} });
        Button bco = new Button("Continuer"); bco.setPrefHeight(40);
        bco.setStyle("-fx-background-color:#F1F5F9;-fx-text-fill:#475569;" +
                "-fx-font-weight:600;-fx-font-size:12px;-fx-background-radius:10;-fx-cursor:hand;");
        bco.setOnAction(ev -> d.close());
        footer.getChildren().addAll(bc, bo, bco);
        root.getChildren().addAll(ab, hdr, body, footer);
        Scene sc = new Scene(root); sc.setFill(Color.TRANSPARENT); d.setScene(sc);
        makeDraggable(d, root); d.showAndWait();
    }

    // =========================================================================
    //  POPUPS UTILITAIRES
    // =========================================================================
    private void showStyledAlert(String type, String title, String header, String body) {
        Stage d = new Stage();
        d.initModality(Modality.APPLICATION_MODAL); d.initStyle(StageStyle.UNDECORATED);
        String accent, icon, bg;
        switch (type) {
            case "warning": accent = "#F97316"; icon = "⚠"; bg = "#FFF7ED"; break;
            case "error":   accent = "#DC2626"; icon = "✕"; bg = "#FEF2F2"; break;
            default:        accent = "#2563EB"; icon = "ℹ"; bg = "#EFF6FF"; break;
        }
        VBox root = new VBox(0); root.setPrefWidth(380);
        root.setStyle("-fx-background-color:white;-fx-background-radius:14px;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.22),28,0,0,5);");
        Region tb = new Region(); tb.setPrefHeight(5);
        tb.setStyle("-fx-background-color:" + accent + ";-fx-background-radius:14px 14px 0 0;");
        HBox ct = new HBox(16); ct.setAlignment(Pos.TOP_LEFT);
        ct.setStyle("-fx-padding:20 24 16 24;-fx-background-color:" + bg + ";");
        Label il = new Label(icon);
        il.setStyle("-fx-font-size:22px;-fx-text-fill:" + accent + ";-fx-min-width:36;-fx-alignment:CENTER;");
        VBox tc = new VBox(5);
        Label hl = new Label(header);
        hl.setStyle("-fx-font-size:14px;-fx-font-weight:800;-fx-text-fill:#1E3A5F;-fx-wrap-text:true;");
        hl.setMaxWidth(280); hl.setWrapText(true);
        Label bl = new Label(body);
        bl.setStyle("-fx-font-size:12px;-fx-text-fill:#64748B;-fx-wrap-text:true;");
        bl.setMaxWidth(280); bl.setWrapText(true);
        tc.getChildren().addAll(hl, bl); ct.getChildren().addAll(il, tc);
        HBox ft = new HBox(); ft.setAlignment(Pos.CENTER_RIGHT);
        ft.setStyle("-fx-padding:10 24 16 24;-fx-background-color:white;" +
                "-fx-background-radius:0 0 14px 14px;");
        Button ok = new Button("Compris"); ok.setPrefWidth(100); ok.setPrefHeight(38);
        ok.setStyle("-fx-background-color:" + accent + ";-fx-text-fill:white;-fx-font-weight:700;" +
                "-fx-font-size:13px;-fx-background-radius:9px;-fx-cursor:hand;");
        ok.setOnAction(ev -> d.close()); ft.getChildren().add(ok);
        root.getChildren().addAll(tb, ct, ft);
        Scene sc = new Scene(root); sc.setFill(Color.TRANSPARENT); d.setScene(sc);
        makeDraggable(d, root); d.showAndWait();
    }

    private boolean showStyledConfirm(String title, String header, String body,
                                      String labelOui, String labelNon) {
        Stage d = new Stage();
        d.initModality(Modality.APPLICATION_MODAL); d.initStyle(StageStyle.UNDECORATED);
        VBox root = new VBox(0); root.setPrefWidth(380);
        root.setStyle("-fx-background-color:white;-fx-background-radius:14px;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.22),28,0,0,5);");
        Region tb = new Region(); tb.setPrefHeight(5);
        tb.setStyle("-fx-background-color:#2563EB;-fx-background-radius:14px 14px 0 0;");
        HBox ct = new HBox(16); ct.setAlignment(Pos.TOP_LEFT);
        ct.setStyle("-fx-padding:20 24 16 24;-fx-background-color:#EFF6FF;");
        Label il = new Label("?");
        il.setStyle("-fx-font-size:18px;-fx-font-weight:900;-fx-text-fill:#2563EB;" +
                "-fx-background-color:#DBEAFE;-fx-background-radius:50%;" +
                "-fx-min-width:36;-fx-min-height:36;-fx-alignment:CENTER;");
        VBox tc = new VBox(5);
        Label hl = new Label(header);
        hl.setStyle("-fx-font-size:14px;-fx-font-weight:800;-fx-text-fill:#1E3A5F;");
        Label bl = new Label(body);
        bl.setStyle("-fx-font-size:12px;-fx-text-fill:#64748B;-fx-wrap-text:true;");
        bl.setMaxWidth(280); bl.setWrapText(true);
        tc.getChildren().addAll(hl, bl); ct.getChildren().addAll(il, tc);
        HBox ft = new HBox(10); ft.setAlignment(Pos.CENTER_RIGHT);
        ft.setStyle("-fx-padding:10 24 16 24;-fx-background-color:white;" +
                "-fx-background-radius:0 0 14px 14px;");
        Button no  = new Button(labelNon); no.setPrefHeight(38);
        no.setStyle("-fx-background-color:#F1F5F9;-fx-text-fill:#475569;" +
                "-fx-font-weight:600;-fx-font-size:12px;-fx-background-radius:9px;-fx-cursor:hand;");
        Button yes = new Button(labelOui); yes.setPrefHeight(38);
        yes.setStyle("-fx-background-color:#2563EB;-fx-text-fill:white;" +
                "-fx-font-weight:700;-fx-font-size:13px;-fx-background-radius:9px;-fx-cursor:hand;");
        final boolean[] res = {false};
        no.setOnAction(ev -> d.close());
        yes.setOnAction(ev -> { res[0] = true; d.close(); });
        ft.getChildren().addAll(no, yes);
        root.getChildren().addAll(tb, ct, ft);
        Scene sc = new Scene(root); sc.setFill(Color.TRANSPARENT); d.setScene(sc);
        makeDraggable(d, root); d.showAndWait();
        return res[0];
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void makeDraggable(Stage stage, VBox root) {
        final double[] delta = new double[2];
        root.setOnMousePressed(ev  -> { delta[0] = stage.getX() - ev.getScreenX(); delta[1] = stage.getY() - ev.getScreenY(); });
        root.setOnMouseDragged(ev  -> { stage.setX(ev.getScreenX() + delta[0]);    stage.setY(ev.getScreenY() + delta[1]); });
    }
}