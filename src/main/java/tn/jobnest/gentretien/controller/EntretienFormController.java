package tn.jobnest.gentretien.controller;

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
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
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

    public void setContextCandidature(int idCandidat, int idOffre, String nomCandidat, String titreOffre) {
        this.idCandidatALier = idCandidat; this.idOffreLiee = idOffre;
        this.nomCandidat = nomCandidat != null ? nomCandidat : "";
        this.titreOffreLiee = titreOffre != null ? titreOffre : "";
        if (lblInfoCandidat != null) lblInfoCandidat.setText("Candidat : " + this.nomCandidat);
        if (lblInfoOffre != null) lblInfoOffre.setText("Offre : " + this.titreOffreLiee);
    }

    @FXML
    private void initialize() {
        typeEntretien.getItems().addAll("présentiel", "visio");
        statut.getItems().addAll("proposé", "confirmé");
        statut.setValue("proposé"); statut.setDisable(true);
        Tooltip.install(statut, new Tooltip(
                "• 'proposé'  : à la création\n• 'confirmé' : confirmé par le candidat\n" +
                        "• 'réalisé'  : après que le candidat a rejoint\n• 'annulé'   : date passée sans action"));
        sectionLieu.setDisable(true);  sectionLieu.setOpacity(0.4);
        sectionVisio.setDisable(true); sectionVisio.setOpacity(0.4);
        hintTypeSection.setVisible(true); hintTypeSection.setManaged(true);

        typeEntretien.valueProperty().addListener((obs, ov, nv) -> {
            boolean isP = "présentiel".equals(nv), isV = "visio".equals(nv);
            hintTypeSection.setVisible(false); hintTypeSection.setManaged(false);
            sectionLieu.setDisable(!isP);  sectionLieu.setOpacity(isP ? 1.0 : 0.4);
            sectionVisio.setDisable(!isV); sectionVisio.setOpacity(isV ? 1.0 : 0.4);
            if (!isP) lieu.clear();
            if (!isV) { lienVisio.clear(); hideMeetBtn(); }
        });
        lienVisio.textProperty().addListener((obs, ov, nv) -> {
            boolean has = nv != null && nv.startsWith("http");
            if (btnOuvrirMeet != null) { btnOuvrirMeet.setVisible(has); btnOuvrirMeet.setManaged(has); }
        });
        if (lblInfoCandidat != null && !nomCandidat.isEmpty()) lblInfoCandidat.setText("Candidat : " + nomCandidat);
        if (lblInfoOffre != null && !titreOffreLiee.isEmpty()) lblInfoOffre.setText("Offre : " + titreOffreLiee);
    }

    private void hideMeetBtn() {
        if (btnOuvrirMeet != null) { btnOuvrirMeet.setVisible(false); btnOuvrirMeet.setManaged(false); }
    }

    @FXML
    private void ouvrirLienMeet() {
        String lien = lienVisio.getText().trim();
        if (lien.isEmpty() || !lien.startsWith("http")) {
            showStyledAlert("warning","Lien invalide","Aucun lien Meet valide",
                    "Veuillez d'abord générer le lien en cliquant sur '📹 Meet'."); return; }
        try { Desktop.getDesktop().browse(new URI(lien)); }
        catch (Exception ex) { showStyledAlert("error","Erreur","Impossible d'ouvrir le navigateur", lien); }
    }

    @FXML
    private void ouvrirLocationPicker() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/tn/jobnest/gentretien/location-picker-view.fxml"));
            Parent root = loader.load();
            LocationPickerController ctrl = loader.getController();
            Stage s = new Stage();
            s.setTitle("JobNest - Choisir le lieu"); s.setScene(new Scene(root));
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
            showStyledAlert("error","Erreur","Impossible d'ouvrir le sélecteur", ex.getMessage()); }
    }

    public void setEntretien(Entretien e) {
        this.entretien = e; this.isReorganisation = false;
        if (e == null) return;
        dateEntretien.setValue(e.getDateEntretien() != null ? e.getDateEntretien().toLocalDate() : null);
        heureDebut.setText(e.getHeureDebut() != null ? e.getHeureDebut().toLocalTime().toString() : "");
        heureFin.setText(e.getHeureFin() != null ? e.getHeureFin().toLocalTime().toString() : "");
        typeEntretien.setValue(e.getTypeEntretien());
        lieu.setText(e.getLieu()); lienVisio.setText(e.getLienVisio());
        String cs = e.getStatut();
        if (cs != null && !statut.getItems().contains(cs)) statut.getItems().add(cs);
        statut.setValue(cs); statut.setDisable(true); noteRecruteur.setText(e.getNoteRecruteur());
    }

    public void setEntretienPourReorganisation(Entretien e) {
        this.entretien = e; this.isReorganisation = true;
        if (e == null) return;
        dateEntretien.setValue(null); dateEntretien.setPromptText("Choisissez une nouvelle date");
        heureDebut.setText(e.getHeureDebut() != null ? e.getHeureDebut().toLocalTime().toString() : "");
        heureFin.setText(e.getHeureFin() != null ? e.getHeureFin().toLocalTime().toString() : "");
        typeEntretien.setValue(e.getTypeEntretien());
        if ("présentiel".equals(e.getTypeEntretien())) { lieu.setText(e.getLieu()); lienVisio.clear(); }
        else { lieu.clear(); lienVisio.clear(); lienVisio.setPromptText("Générez un nouveau lien Meet"); }
        if (!statut.getItems().contains("proposé")) statut.getItems().add("proposé");
        statut.setValue("proposé"); statut.setDisable(true); noteRecruteur.setText(e.getNoteRecruteur());
    }

    @FXML
    private void genererLienMeet() {
        if (dateEntretien.getValue() == null) {
            showStyledAlert("warning","Champ requis","Date manquante",
                    "Veuillez sélectionner une date avant de générer le lien Meet."); return; }
        if (dateEntretien.getValue().isBefore(LocalDate.now())) {
            showStyledAlert("warning","Date invalide","La date est dans le passé",
                    "Impossible de créer un entretien dans le passé."); return; }
        String hD = heureDebut.getText().trim(), hF = heureFin.getText().trim();
        if (hD.isEmpty() || hF.isEmpty()) {
            showStyledAlert("warning","Champs requis","Heures manquantes",
                    "Les heures de début et fin sont requises pour générer le lien Meet."); return; }
        try {
            LocalTime debut = LocalTime.parse(hD), fin = LocalTime.parse(hF);
            if (fin.isBefore(debut) || fin.equals(debut)) {
                showStyledAlert("warning","Incohérence","Heure de fin invalide",
                        "L'heure de fin doit être après l'heure de début."); return; }
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            String meetLink = GoogleMeetService.creerMeetingLink("Entretien JobNest",
                    "Entretien d'embauche planifié via JobNest",
                    LocalDateTime.of(dateEntretien.getValue(), debut).format(fmt),
                    LocalDateTime.of(dateEntretien.getValue(), fin).format(fmt));
            lienVisio.setText(meetLink);
            showMeetSuccessDialog(meetLink);
        } catch (DateTimeParseException ex) {
            showStyledAlert("error","Format invalide","Format d'heure incorrect","Utilisez HH:mm (ex: 14:30)");
        } catch (Exception ex) {
            showStyledAlert("error","Erreur Meet","Impossible de générer le lien", ex.getMessage()); }
    }

    @FXML
    private void validerAdresse() {
        String adresse = lieu.getText().trim();
        if (adresse.isEmpty()) {
            showStyledAlert("warning","Champ vide","Aucune adresse saisie",
                    "Veuillez saisir ou sélectionner une adresse."); return; }
        try {
            String url = "https://www.google.com/maps/search/?api=1&query="
                    + java.net.URLEncoder.encode(adresse, "UTF-8");
            if (showStyledConfirm("Vérifier","Ouvrir Google Maps ?","Adresse : " + adresse,
                    "Oui, vérifier","Non, continuer"))
                Desktop.getDesktop().browse(new URI(url));
        } catch (Exception ex) {
            showStyledAlert("error","Erreur","Impossible d'ouvrir Google Maps", ex.getMessage()); }
    }

    @FXML
    private void save() {
        if (dateEntretien.getValue() == null) {
            showStyledAlert("warning","Champ obligatoire","Date manquante",
                    "Veuillez sélectionner une date d'entretien."); dateEntretien.requestFocus(); return; }
        if (dateEntretien.getValue().isBefore(LocalDate.now())) {
            showStyledAlert("warning","Date invalide","La date est dans le passé",
                    "Veuillez choisir une date future."); dateEntretien.requestFocus(); return; }
        String hD = heureDebut.getText().trim(), hF = heureFin.getText().trim();
        if (hD.isEmpty()) { showStyledAlert("warning","Champ obligatoire","Heure de début manquante",
                "Veuillez renseigner l'heure de début (HH:mm)."); heureDebut.requestFocus(); return; }
        if (hF.isEmpty()) { showStyledAlert("warning","Champ obligatoire","Heure de fin manquante",
                "Veuillez renseigner l'heure de fin (HH:mm)."); heureFin.requestFocus(); return; }
        if (typeEntretien.getValue() == null || typeEntretien.getValue().isEmpty()) {
            showStyledAlert("warning","Champ obligatoire","Type non sélectionné",
                    "Veuillez sélectionner un type d'entretien."); typeEntretien.requestFocus(); return; }
        if ("présentiel".equals(typeEntretien.getValue()) &&
                (lieu.getText() == null || lieu.getText().trim().isEmpty())) {
            showStyledAlert("warning","Champ obligatoire","Lieu manquant",
                    "Pour un entretien présentiel, le lieu est obligatoire."); lieu.requestFocus(); return; }
        if ("visio".equals(typeEntretien.getValue()) &&
                (lienVisio.getText() == null || lienVisio.getText().trim().isEmpty())) {
            showStyledAlert("warning","Champ obligatoire","Lien visio manquant",
                    "Cliquez sur '📹 Meet' pour générer un lien."); lienVisio.requestFocus(); return; }

        LocalTime debut, fin;
        try { debut = LocalTime.parse(hD); fin = LocalTime.parse(hF); }
        catch (DateTimeParseException ex) {
            showStyledAlert("error","Format invalide","Format d'heure incorrect","Utilisez HH:mm (ex: 14:30)"); return; }
        if (fin.isBefore(debut) || fin.equals(debut)) {
            showStyledAlert("warning","Incohérence","Heure de fin invalide",
                    "L'heure de fin doit être après l'heure de début."); heureFin.requestFocus(); return; }

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
                        showStyledAlert("error","Conflit","Créneau déjà occupé",
                                "Offre : " + service.getOffreTitre(e.getIdOffre()) + "\n" +
                                        "Horaire : " + ad.format(DateTimeFormatter.ofPattern("HH:mm")) +
                                        " → " + af.format(DateTimeFormatter.ofPattern("HH:mm")));
                        return; }
                }
            }
        } catch (SQLException ex) {
            showStyledAlert("error","Erreur BD","Vérification impossible", ex.getMessage()); return; }

        String dateStr = dateEntretien.getValue().format(DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy"));
        String typeStr = "présentiel".equals(typeEntretien.getValue()) ? "🏢 Présentiel" : "💻 Visio";
        String detailStr = "présentiel".equals(typeEntretien.getValue())
                ? lieu.getText().trim() : lienVisio.getText().trim();
        String mode = (entretien != null && entretien.getIdEntretien() != 0)
                ? (isReorganisation ? "réorganiser" : "modifier") : "créer";

        if (!showEntretienConfirmDialog(
                nomCandidat.isEmpty() ? "Candidat" : nomCandidat,
                titreOffreLiee.isEmpty() ? "—" : titreOffreLiee,
                dateStr, hD + " → " + hF, typeStr,
                detailStr.isEmpty() ? "—" : detailStr, mode)) return;

        if (entretien == null) entretien = new Entretien();
        entretien.setDateEntretien(java.sql.Date.valueOf(dateEntretien.getValue()));
        entretien.setHeureDebut(Time.valueOf(debut)); entretien.setHeureFin(Time.valueOf(fin));
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
                if (idNew > 0 && idCandidatALier > 0) service.ajouterParticipant(idNew, idCandidatALier);
            } else { service.update(entretien); }
            Stage stage = (Stage) dateEntretien.getScene().getWindow();
            if (showSuccessDialog(mode, nomCandidat)) resetForm();
            else stage.close();
        } catch (SQLException ex) {
            showStyledAlert("error","Erreur BD","Enregistrement impossible", ex.getMessage()); }
    }

    private void resetForm() {
        dateEntretien.setValue(null); heureDebut.clear(); heureFin.clear();
        typeEntretien.setValue(null); lieu.clear(); lienVisio.clear();
        noteRecruteur.clear(); statut.setValue("proposé");
        entretien = null; isReorganisation = false;
        sectionLieu.setDisable(true);  sectionLieu.setOpacity(0.4);
        sectionVisio.setDisable(true); sectionVisio.setOpacity(0.4);
        hintTypeSection.setVisible(true); hintTypeSection.setManaged(true); hideMeetBtn();
    }

    // =========================================================================
    //  POPUPS STYLISÉS
    // =========================================================================

    private boolean showEntretienConfirmDialog(String candidat, String offre,
                                               String date, String horaire, String type, String detail, String mode) {
        Stage d = new Stage();
        d.initModality(Modality.APPLICATION_MODAL); d.initStyle(StageStyle.UNDECORATED); d.setResizable(false);
        VBox root = new VBox(0); root.setPrefWidth(480);
        root.setStyle("-fx-background-color:white;-fx-background-radius:18px;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.28),40,0,0,8);");

        VBox header = new VBox(10);
        header.setStyle("-fx-background-color:linear-gradient(135deg,#1E3A5F 0%,#2563EB 100%);" +
                "-fx-background-radius:18px 18px 0 0;-fx-padding:24 28 20 28;");
        HBox tr = new HBox(12); tr.setAlignment(Pos.CENTER_LEFT);
        StackPane iw = new StackPane();
        Circle ic = new Circle(22); ic.setFill(Color.web("rgba(255,255,255,0.18)"));
        Label il = new Label("📋"); il.setStyle("-fx-font-size:20px;");
        iw.getChildren().addAll(ic, il);
        String cap = mode.substring(0,1).toUpperCase() + mode.substring(1);
        VBox tv = new VBox(3);
        Label t1 = new Label("Confirmer l'entretien"); t1.setStyle("-fx-font-size:18px;-fx-font-weight:800;-fx-text-fill:white;");
        Label t2 = new Label("Vérifiez les informations avant de " + mode); t2.setStyle("-fx-font-size:12px;-fx-text-fill:rgba(255,255,255,0.65);");
        tv.getChildren().addAll(t1, t2); tr.getChildren().addAll(iw, tv); header.getChildren().add(tr);

        HBox strip = new HBox(20); strip.setAlignment(Pos.CENTER_LEFT);
        strip.setStyle("-fx-background-color:rgba(255,255,255,0.12);-fx-background-radius:10px;-fx-padding:10 14 10 14;");
        VBox.setMargin(strip, new Insets(8,0,0,0));
        VBox vc = new VBox(2);
        Label lc1 = new Label("CANDIDAT"); lc1.setStyle("-fx-font-size:9px;-fx-text-fill:rgba(255,255,255,0.5);-fx-font-weight:700;");
        Label lc2 = new Label(candidat); lc2.setStyle("-fx-font-size:13px;-fx-text-fill:white;-fx-font-weight:700;");
        vc.getChildren().addAll(lc1, lc2);
        Label pipe = new Label("│"); pipe.setStyle("-fx-text-fill:rgba(255,255,255,0.3);-fx-font-size:18px;");
        VBox vo = new VBox(2);
        Label lo1 = new Label("OFFRE"); lo1.setStyle("-fx-font-size:9px;-fx-text-fill:rgba(255,255,255,0.5);-fx-font-weight:700;");
        Label lo2 = new Label(offre); lo2.setStyle("-fx-font-size:13px;-fx-text-fill:#93C5FD;-fx-font-weight:700;");
        vo.getChildren().addAll(lo1, lo2);
        strip.getChildren().addAll(vc, pipe, vo); header.getChildren().add(strip);

        VBox body = new VBox(0); body.setStyle("-fx-background-color:#F8FAFF;-fx-padding:22 28 16 28;");
        Label recap = new Label("Récapitulatif"); recap.setStyle("-fx-font-size:11px;-fx-font-weight:700;-fx-text-fill:#94A3B8;");
        VBox.setMargin(recap, new Insets(0,0,12,0)); body.getChildren().add(recap);
        body.getChildren().addAll(buildSummaryRow("📅","Date",date), buildSummaryRow("⏱","Horaire",horaire),
                buildSummaryRow("🏷","Type",type), buildSummaryRow("📍","Détail",detail));
        Region dv = new Region(); dv.setPrefHeight(1); dv.setStyle("-fx-background-color:#E2E8F0;");
        VBox.setMargin(dv, new Insets(14,0,0,0)); body.getChildren().add(dv);

        HBox footer = new HBox(12); footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle("-fx-background-color:white;-fx-padding:16 28 22 28;-fx-background-radius:0 0 18px 18px;");
        Button bc = new Button("✕  Annuler"); bc.setPrefWidth(130); bc.setPrefHeight(44);
        bc.setStyle("-fx-background-color:#F1F5F9;-fx-text-fill:#475569;-fx-font-size:13px;-fx-font-weight:700;" +
                "-fx-background-radius:11px;-fx-cursor:hand;-fx-border-color:#E2E8F0;-fx-border-width:1.5;-fx-border-radius:11px;");
        Button bo = new Button("✅  " + cap + " l'entretien"); bo.setPrefHeight(44);
        String ns = "-fx-background-color:linear-gradient(135deg,#1e3a8a 0%,#2563EB 100%);-fx-text-fill:white;" +
                "-fx-font-size:13px;-fx-font-weight:800;-fx-background-radius:11px;-fx-cursor:hand;" +
                "-fx-effect:dropshadow(gaussian,rgba(37,99,235,0.45),12,0,0,4);";
        bo.setStyle(ns);
        bo.setOnMouseEntered(ev -> bo.setStyle("-fx-background-color:linear-gradient(135deg,#1e40af 0%,#1D4ED8 100%);" +
                "-fx-text-fill:white;-fx-font-size:13px;-fx-font-weight:800;-fx-background-radius:11px;-fx-cursor:hand;" +
                "-fx-effect:dropshadow(gaussian,rgba(37,99,235,0.6),16,0,0,5);"));
        bo.setOnMouseExited(ev -> bo.setStyle(ns));
        final boolean[] res = {false};
        bc.setOnAction(ev -> d.close()); bo.setOnAction(ev -> { res[0] = true; d.close(); });
        footer.getChildren().addAll(bc, bo);
        root.getChildren().addAll(header, body, footer);
        Scene sc = new Scene(root); sc.setFill(Color.TRANSPARENT); d.setScene(sc);
        makeDraggable(d, root); d.showAndWait(); return res[0];
    }

    private boolean showSuccessDialog(String mode, String nomCandidatParam) {
        Stage d = new Stage();
        d.initModality(Modality.APPLICATION_MODAL); d.initStyle(StageStyle.UNDECORATED); d.setResizable(false);
        VBox root = new VBox(0); root.setPrefWidth(420); root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color:white;-fx-background-radius:18px;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.28),40,0,0,8);");

        VBox top = new VBox(10); top.setAlignment(Pos.CENTER);
        top.setStyle("-fx-background-color:linear-gradient(135deg,#059669 0%,#10B981 100%);" +
                "-fx-background-radius:18px 18px 0 0;-fx-padding:30 28 24 28;");
        StackPane cc = new StackPane();
        Circle bg1 = new Circle(36); bg1.setFill(Color.web("rgba(255,255,255,0.18)"));
        Circle bg2 = new Circle(28); bg2.setFill(Color.web("rgba(255,255,255,0.14)"));
        Label chk = new Label("✓"); chk.setStyle("-fx-font-size:28px;-fx-font-weight:900;-fx-text-fill:white;");
        cc.getChildren().addAll(bg1, bg2, chk);
        String mm = "réorganiser".equals(mode) ? "réorganisé" : "modifier".equals(mode) ? "modifié" : "créé";
        Label st = new Label("Entretien " + mm + " !"); st.setStyle("-fx-font-size:20px;-fx-font-weight:800;-fx-text-fill:white;");
        String sub = nomCandidatParam.isEmpty() ? "L'entretien a bien été enregistré."
                : "L'entretien avec " + nomCandidatParam + " a bien été enregistré.";
        Label ss = new Label(sub); ss.setStyle("-fx-font-size:12px;-fx-text-fill:rgba(255,255,255,0.75);");
        ss.setMaxWidth(340); ss.setWrapText(true); ss.setAlignment(Pos.CENTER);
        top.getChildren().addAll(cc, st, ss);

        VBox qBox = new VBox(4); qBox.setAlignment(Pos.CENTER);
        qBox.setStyle("-fx-padding:22 32 8 32;-fx-background-color:#F8FAFF;");
        qBox.getChildren().add(new Label("Que souhaitez-vous faire ?") {{
            setStyle("-fx-font-size:14px;-fx-font-weight:700;-fx-text-fill:#1E3A5F;"); }});

        VBox btns = new VBox(10); btns.setAlignment(Pos.CENTER);
        btns.setStyle("-fx-padding:14 32 28 32;-fx-background-color:#F8FAFF;-fx-background-radius:0 0 18px 18px;");
        String an = "-fx-background-color:linear-gradient(135deg,#1e3a8a 0%,#2563EB 100%);-fx-text-fill:white;" +
                "-fx-font-size:14px;-fx-font-weight:800;-fx-background-radius:12px;-fx-cursor:hand;" +
                "-fx-effect:dropshadow(gaussian,rgba(37,99,235,0.40),12,0,0,4);";
        Button ba = new Button("➕  Planifier un autre entretien"); ba.setPrefWidth(340); ba.setPrefHeight(48); ba.setStyle(an);
        ba.setOnMouseEntered(ev -> ba.setStyle("-fx-background-color:linear-gradient(135deg,#1e40af 0%,#1D4ED8 100%);" +
                "-fx-text-fill:white;-fx-font-size:14px;-fx-font-weight:800;-fx-background-radius:12px;-fx-cursor:hand;" +
                "-fx-effect:dropshadow(gaussian,rgba(37,99,235,0.55),16,0,0,6);"));
        ba.setOnMouseExited(ev -> ba.setStyle(an));
        String fn = "-fx-background-color:transparent;-fx-text-fill:#64748B;-fx-font-size:13px;-fx-font-weight:600;" +
                "-fx-background-radius:12px;-fx-cursor:hand;-fx-border-color:#CBD5E1;-fx-border-width:1.5;-fx-border-radius:12px;";
        Button bf = new Button("✕  Fermer le formulaire"); bf.setPrefWidth(340); bf.setPrefHeight(44); bf.setStyle(fn);
        bf.setOnMouseEntered(ev -> bf.setStyle("-fx-background-color:#F1F5F9;-fx-text-fill:#475569;" +
                "-fx-font-size:13px;-fx-font-weight:600;-fx-background-radius:12px;-fx-cursor:hand;" +
                "-fx-border-color:#CBD5E1;-fx-border-width:1.5;-fx-border-radius:12px;"));
        bf.setOnMouseExited(ev -> bf.setStyle(fn));
        final boolean[] add = {false};
        ba.setOnAction(ev -> { add[0] = true; d.close(); }); bf.setOnAction(ev -> d.close());
        btns.getChildren().addAll(ba, bf);
        root.getChildren().addAll(top, qBox, btns);
        Scene sc = new Scene(root); sc.setFill(Color.TRANSPARENT); d.setScene(sc);
        makeDraggable(d, root); d.showAndWait(); return add[0];
    }

    private void showMeetSuccessDialog(String meetLink) {
        Stage d = new Stage();
        d.initModality(Modality.APPLICATION_MODAL); d.initStyle(StageStyle.UNDECORATED);
        VBox root = new VBox(0); root.setPrefWidth(430);
        root.setStyle("-fx-background-color:white;-fx-background-radius:16px;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.25),32,0,0,6);");
        VBox hdr = new VBox(8); hdr.setAlignment(Pos.CENTER);
        hdr.setStyle("-fx-background-color:linear-gradient(135deg,#1a73e8 0%,#0d47a1 100%);" +
                "-fx-background-radius:16px 16px 0 0;-fx-padding:22 28 18 28;");
        hdr.getChildren().addAll(new Label("📹") {{ setStyle("-fx-font-size:34px;"); }},
                new Label("Lien Google Meet généré !") {{ setStyle("-fx-font-size:17px;-fx-font-weight:800;-fx-text-fill:white;"); }});
        VBox body = new VBox(10); body.setStyle("-fx-padding:18 28 12 28;-fx-background-color:#F0F4FF;");
        body.getChildren().add(new Label("Votre lien de réunion :") {{ setStyle("-fx-font-size:12px;-fx-font-weight:700;-fx-text-fill:#475569;"); }});
        HBox lb = new HBox(8); lb.setAlignment(Pos.CENTER_LEFT);
        lb.setStyle("-fx-background-color:white;-fx-border-color:#BFDBFE;-fx-border-radius:10;-fx-border-width:1.5;-fx-background-radius:10;-fx-padding:10 12;");
        TextField lf = new TextField(meetLink); lf.setEditable(false);
        lf.setStyle("-fx-background-color:transparent;-fx-border-color:transparent;-fx-font-size:11px;-fx-text-fill:#1e3a8a;");
        HBox.setHgrow(lf, Priority.ALWAYS);
        lb.getChildren().addAll(new Label("🔗") {{ setStyle("-fx-font-size:14px;"); }}, lf);
        body.getChildren().add(lb);
        HBox footer = new HBox(10); footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle("-fx-padding:12 28 20 28;-fx-background-color:white;-fx-background-radius:0 0 16px 16px;");
        Button bc = new Button("📋 Copier le lien"); bc.setPrefHeight(40);
        bc.setStyle("-fx-background-color:#EFF6FF;-fx-text-fill:#2563EB;-fx-font-weight:700;-fx-font-size:12px;" +
                "-fx-background-radius:10;-fx-cursor:hand;-fx-border-color:#BFDBFE;-fx-border-width:1.5;-fx-border-radius:10;");
        bc.setOnAction(ev -> {
            javafx.scene.input.Clipboard cb = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent cc2 = new javafx.scene.input.ClipboardContent();
            cc2.putString(meetLink); cb.setContent(cc2);
            bc.setText("✅ Copié !"); bc.setStyle("-fx-background-color:#D1FAE5;-fx-text-fill:#059669;" +
                    "-fx-font-weight:700;-fx-font-size:12px;-fx-background-radius:10;-fx-cursor:hand;"); });
        Button bo = new Button("🔗 Rejoindre"); bo.setPrefHeight(40);
        bo.setStyle("-fx-background-color:#1a73e8;-fx-text-fill:white;-fx-font-weight:700;-fx-font-size:13px;" +
                "-fx-background-radius:10;-fx-cursor:hand;-fx-effect:dropshadow(gaussian,rgba(26,115,232,0.4),8,0,0,3);");
        bo.setOnAction(ev -> { try { Desktop.getDesktop().browse(new URI(meetLink)); } catch (Exception ignored) {} });
        Button bco = new Button("Continuer la saisie"); bco.setPrefHeight(40);
        bco.setStyle("-fx-background-color:#F1F5F9;-fx-text-fill:#475569;-fx-font-weight:600;-fx-font-size:12px;-fx-background-radius:10;-fx-cursor:hand;");
        bco.setOnAction(ev -> d.close());
        footer.getChildren().addAll(bc, bo, bco);
        root.getChildren().addAll(hdr, body, footer);
        Scene sc = new Scene(root); sc.setFill(Color.TRANSPARENT); d.setScene(sc);
        makeDraggable(d, root); d.showAndWait();
    }

    private void showStyledAlert(String type, String title, String header, String body) {
        Stage d = new Stage(); d.initModality(Modality.APPLICATION_MODAL); d.initStyle(StageStyle.UNDECORATED);
        String accent, icon, bg;
        switch (type) { case "warning": accent="#F97316"; icon="⚠"; bg="#FFF7ED"; break;
            case "error": accent="#DC2626"; icon="✕"; bg="#FEF2F2"; break;
            default: accent="#2563EB"; icon="ℹ"; bg="#EFF6FF"; break; }
        VBox root = new VBox(0); root.setPrefWidth(380);
        root.setStyle("-fx-background-color:white;-fx-background-radius:14px;-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.22),28,0,0,5);");
        Region tb = new Region(); tb.setPrefHeight(5); tb.setStyle("-fx-background-color:"+accent+";-fx-background-radius:14px 14px 0 0;");
        HBox ct = new HBox(16); ct.setAlignment(Pos.TOP_LEFT); ct.setStyle("-fx-padding:20 24 16 24;-fx-background-color:"+bg+";");
        Label il = new Label(icon); il.setStyle("-fx-font-size:22px;-fx-text-fill:"+accent+";-fx-min-width:36;-fx-alignment:CENTER;");
        VBox tc = new VBox(5);
        Label hl = new Label(header); hl.setStyle("-fx-font-size:14px;-fx-font-weight:800;-fx-text-fill:#1E3A5F;-fx-wrap-text:true;"); hl.setMaxWidth(280); hl.setWrapText(true);
        Label bl = new Label(body); bl.setStyle("-fx-font-size:12px;-fx-text-fill:#64748B;-fx-wrap-text:true;"); bl.setMaxWidth(280); bl.setWrapText(true);
        tc.getChildren().addAll(hl, bl); ct.getChildren().addAll(il, tc);
        HBox ft = new HBox(); ft.setAlignment(Pos.CENTER_RIGHT); ft.setStyle("-fx-padding:10 24 16 24;-fx-background-color:white;-fx-background-radius:0 0 14px 14px;");
        Button ok = new Button("Compris"); ok.setPrefWidth(100); ok.setPrefHeight(38);
        ok.setStyle("-fx-background-color:"+accent+";-fx-text-fill:white;-fx-font-weight:700;-fx-font-size:13px;-fx-background-radius:9px;-fx-cursor:hand;");
        ok.setOnAction(ev -> d.close()); ft.getChildren().add(ok);
        root.getChildren().addAll(tb, ct, ft);
        Scene sc = new Scene(root); sc.setFill(Color.TRANSPARENT); d.setScene(sc);
        makeDraggable(d, root); d.showAndWait();
    }

    private boolean showStyledConfirm(String title, String header, String body, String labelOui, String labelNon) {
        Stage d = new Stage(); d.initModality(Modality.APPLICATION_MODAL); d.initStyle(StageStyle.UNDECORATED);
        VBox root = new VBox(0); root.setPrefWidth(380);
        root.setStyle("-fx-background-color:white;-fx-background-radius:14px;-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.22),28,0,0,5);");
        Region tb = new Region(); tb.setPrefHeight(5); tb.setStyle("-fx-background-color:#2563EB;-fx-background-radius:14px 14px 0 0;");
        HBox ct = new HBox(16); ct.setAlignment(Pos.TOP_LEFT); ct.setStyle("-fx-padding:20 24 16 24;-fx-background-color:#EFF6FF;");
        Label il = new Label("?"); il.setStyle("-fx-font-size:18px;-fx-font-weight:900;-fx-text-fill:#2563EB;" +
                "-fx-background-color:#DBEAFE;-fx-background-radius:50%;-fx-min-width:36;-fx-min-height:36;-fx-alignment:CENTER;");
        VBox tc = new VBox(5);
        Label hl = new Label(header); hl.setStyle("-fx-font-size:14px;-fx-font-weight:800;-fx-text-fill:#1E3A5F;");
        Label bl = new Label(body); bl.setStyle("-fx-font-size:12px;-fx-text-fill:#64748B;-fx-wrap-text:true;"); bl.setMaxWidth(280); bl.setWrapText(true);
        tc.getChildren().addAll(hl, bl); ct.getChildren().addAll(il, tc);
        HBox ft = new HBox(10); ft.setAlignment(Pos.CENTER_RIGHT); ft.setStyle("-fx-padding:10 24 16 24;-fx-background-color:white;-fx-background-radius:0 0 14px 14px;");
        Button no = new Button(labelNon); no.setPrefHeight(38); no.setStyle("-fx-background-color:#F1F5F9;-fx-text-fill:#475569;-fx-font-weight:600;-fx-font-size:12px;-fx-background-radius:9px;-fx-cursor:hand;");
        Button yes = new Button(labelOui); yes.setPrefHeight(38); yes.setStyle("-fx-background-color:#2563EB;-fx-text-fill:white;-fx-font-weight:700;-fx-font-size:13px;-fx-background-radius:9px;-fx-cursor:hand;");
        final boolean[] res = {false}; no.setOnAction(ev -> d.close()); yes.setOnAction(ev -> { res[0] = true; d.close(); });
        ft.getChildren().addAll(no, yes); root.getChildren().addAll(tb, ct, ft);
        Scene sc = new Scene(root); sc.setFill(Color.TRANSPARENT); d.setScene(sc);
        makeDraggable(d, root); d.showAndWait(); return res[0];
    }

    private HBox buildSummaryRow(String emoji, String label, String value) {
        HBox row = new HBox(12); row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding:9 14 9 14;-fx-background-color:white;-fx-background-radius:10;");
        VBox.setMargin(row, new Insets(0,0,6,0));
        StackPane ib = new StackPane();
        Region ibg = new Region(); ibg.setMinSize(36,36); ibg.setMaxSize(36,36);
        ibg.setStyle("-fx-background-color:#EFF6FF;-fx-background-radius:9;");
        Label il = new Label(emoji); il.setStyle("-fx-font-size:15px;");
        ib.getChildren().addAll(ibg, il);
        VBox tb = new VBox(1);
        Label k = new Label(label); k.setStyle("-fx-font-size:10px;-fx-text-fill:#94A3B8;-fx-font-weight:700;");
        Label v = new Label(value); v.setStyle("-fx-font-size:13px;-fx-text-fill:#1E3A5F;-fx-font-weight:600;"); v.setMaxWidth(340); v.setWrapText(true);
        tb.getChildren().addAll(k, v); row.getChildren().addAll(ib, tb); return row;
    }

    private void makeDraggable(Stage stage, VBox root) {
        final double[] delta = new double[2];
        root.setOnMousePressed(ev -> { delta[0] = stage.getX() - ev.getScreenX(); delta[1] = stage.getY() - ev.getScreenY(); });
        root.setOnMouseDragged(ev -> { stage.setX(ev.getScreenX() + delta[0]); stage.setY(ev.getScreenY() + delta[1]); });
    }
}