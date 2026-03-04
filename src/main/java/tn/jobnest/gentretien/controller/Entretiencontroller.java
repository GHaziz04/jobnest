package tn.jobnest.gentretien.controller;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.jobnest.gentretien.model.Entretien;
import tn.jobnest.gentretien.model.Notification;
import tn.jobnest.gentretien.service.Entretienservice;
import tn.jobnest.gentretien.service.NotificationService;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Entretiencontroller {

    // ── FXML — vue principale ─────────────────────────────────────────
    @FXML private VBox             entretiensVBox;
    @FXML private TextField        searchField;
    @FXML private ComboBox<String> comboType;
    @FXML private ComboBox<String> comboStatut;
    @FXML private Label            planifiesCount;
    @FXML private Label            terminesCount;
    @FXML private Label            semaineCount;

    // ── FXML — cloche et badge ────────────────────────────────────────
    @FXML private Label  notifBadgeLabel;
    @FXML private Button btnNotifications;

    // ── fx:include — convention JavaFX : fx:id="notifInclude"
    //    → nœud   injecté sous  @FXML Node notifInclude
    //    → ctrl   injecté sous  @FXML NotificationPanelController notifIncludeController
    @FXML private Node                        notifInclude;
    @FXML private NotificationPanelController notifIncludeController;

    // ── Services — NE PAS stocker la connexion ici, chaque service
    //    doit appeler MyDatabase.getInstance().getConn() à chaque requête
    private final Entretienservice    service      = new Entretienservice();
    private final NotificationService notifService = NotificationService.getInstance();

    private List<Entretien> allEntretiens;
    private List<Entretien> entretiensFiltres;
    private final int currentRecruteurId = 1;
    private boolean   panelVisible       = false;

    // ════════════════════════════════════════════════════════════════
    //  INITIALISATION
    //  ORDRE CRITIQUE :
    //   1. UI (combos, listeners)
    //   2. Cacher panneau
    //   3. Enregistrer callback notif  ← AVANT le chargement des données
    //   4. Charger données (déclenche planification rappels)
    //   5. Mettre à jour badge
    // ════════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {

        // 1. Combos et listeners de filtre
        comboType.setItems(FXCollections.observableArrayList("Tous les types", "présentiel", "visio"));
        comboType.setValue("Tous les types");
        comboStatut.setItems(FXCollections.observableArrayList("Tous les statuts", "proposé"));
        comboStatut.setValue("Tous les statuts");
        comboType  .valueProperty().addListener((obs, o, n) -> filterAndDisplay());
        comboStatut.valueProperty().addListener((obs, o, n) -> filterAndDisplay());
        searchField.textProperty() .addListener((obs, o, n) -> filterAndDisplay());

        // 2. Panneau notifications caché au départ
        masquerPanneau();

        // 3. Callback — doit être enregistré AVANT rafraichirListe()
        notifService.setOnNewNotification(notif -> {
            // Déjà sur le JavaFX thread (Platform.runLater dans NotificationService)
            afficherToast(notif);
            updateBadge();
            if (panelVisible && notifIncludeController != null) {
                notifIncludeController.charger();
            }
        });

        // 4. Chargement initial (sur thread séparé pour ne pas bloquer l'UI)
        chargerDonneesAsync();

        // 5. Badge initial (notifs déjà en BDD des sessions précédentes)
        updateBadge();
    }

    // ════════════════════════════════════════════════════════════════
    //  CHARGEMENT ASYNCHRONE — évite le blocage du thread JavaFX
    //  et les conflits de connexion entre threads
    // ════════════════════════════════════════════════════════════════
    private void chargerDonneesAsync() {
        Thread loader = new Thread(() -> {
            try {
                // --- opérations BDD sur le thread de fond ---
                service.annulerEntretiensExpires();
                notifService.detecterEtNotifierAnnulations(currentRecruteurId, service);

                List<Entretien> tous = service.afficher();

                List<Entretien> tousRecruteur = tous.stream()
                        .filter(e -> e.getIdRecruteur() == currentRecruteurId)
                        .collect(Collectors.toList());

                List<Entretien> filtresPropose = tousRecruteur.stream()
                        .filter(e -> "proposé".equals(e.getStatut()))
                        .collect(Collectors.toList());

                notifService.planifierRappels(filtresPropose);

                // --- retour sur le JavaFX thread pour mettre à jour l'UI ---
                Platform.runLater(() -> {
                    allEntretiens     = tousRecruteur;
                    entretiensFiltres = filtresPropose;
                    updateStats();
                    filterAndDisplay();
                    updateBadge();
                });

            } catch (SQLException ex) {
                Platform.runLater(() ->
                        showAlert(Alert.AlertType.ERROR, "Erreur BD",
                                "Impossible de charger les entretiens : " + ex.getMessage()));
            }
        });
        loader.setDaemon(true);
        loader.setName("jobnest-data-loader");
        loader.start();
    }

    // Version synchrone utilisée après une action utilisateur (bouton Actualiser, suppression…)
    private void rafraichirListe() {
        chargerDonneesAsync();
    }

    // ════════════════════════════════════════════════════════════════
    //  ACTUALISER (bouton)
    // ════════════════════════════════════════════════════════════════
    @FXML
    private void actualiserListe(ActionEvent event) {
        rafraichirListe();
        showAlert(Alert.AlertType.INFORMATION, "Actualisation",
                "La liste des entretiens a été actualisée.");
    }

    // ════════════════════════════════════════════════════════════════
    //  CLOCHE — TOGGLE PANNEAU NOTIFICATIONS
    // ════════════════════════════════════════════════════════════════
    @FXML
    private void toggleNotificationPanel(ActionEvent event) {
        if (notifInclude == null) {
            System.err.println("[Notif] notifInclude est null — vérifiez fx:id dans le FXML");
            return;
        }
        if (!panelVisible) ouvrirPanneau();
        else               fermerPanneau();
    }

    private void ouvrirPanneau() {
        notifInclude.setVisible(true);
        notifInclude.setManaged(true);
        panelVisible = true;

        notifInclude.setOpacity(0);
        notifInclude.setTranslateX(30);
        FadeTransition      fade  = new FadeTransition(Duration.millis(220), notifInclude);
        fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(220), notifInclude);
        slide.setToX(0);
        new ParallelTransition(fade, slide).play();

        if (notifIncludeController != null) notifIncludeController.charger();
        updateBadge();
    }

    private void fermerPanneau() {
        FadeTransition      fade  = new FadeTransition(Duration.millis(180), notifInclude);
        fade.setToValue(0);
        TranslateTransition slide = new TranslateTransition(Duration.millis(180), notifInclude);
        slide.setToX(30);
        ParallelTransition anim = new ParallelTransition(fade, slide);
        anim.setOnFinished(e -> masquerPanneau());
        anim.play();
        panelVisible = false;
    }

    private void masquerPanneau() {
        if (notifInclude != null) {
            notifInclude.setVisible(false);
            notifInclude.setManaged(false);
        }
    }

    // ── Badge ─────────────────────────────────────────────────────────
    private void updateBadge() {
        if (notifBadgeLabel == null) return;
        // Lecture BDD sur thread de fond, mise à jour badge sur JavaFX thread
        Thread t = new Thread(() -> {
            try {
                int count = notifService.countNonLues(currentRecruteurId);
                Platform.runLater(() -> {
                    if (count > 0) {
                        notifBadgeLabel.setText(count > 9 ? "9+" : String.valueOf(count));
                        notifBadgeLabel.setVisible(true);
                        notifBadgeLabel.setManaged(true);
                        ScaleTransition pulse = new ScaleTransition(Duration.millis(200), notifBadgeLabel);
                        pulse.setFromX(1); pulse.setFromY(1);
                        pulse.setToX(1.35); pulse.setToY(1.35);
                        pulse.setAutoReverse(true);
                        pulse.setCycleCount(2);
                        pulse.play();
                    } else {
                        notifBadgeLabel.setVisible(false);
                        notifBadgeLabel.setManaged(false);
                    }
                });
            } catch (SQLException e) {
                System.err.println("[Badge] Erreur countNonLues : " + e.getMessage());
            }
        });
        t.setDaemon(true);
        t.start();
    }

    // ── Toast ─────────────────────────────────────────────────────────
    private void afficherToast(Notification notif) {
        try {
            if (entretiensVBox.getScene() != null) {
                Stage stage = (Stage) entretiensVBox.getScene().getWindow();
                NotificationToast.show(stage, notif);
            }
        } catch (Exception e) {
            System.err.println("[Toast] Impossible d'afficher : " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  STATISTIQUES
    // ════════════════════════════════════════════════════════════════
    private void updateStats() {
        if (allEntretiens == null) return;
        long planifies = allEntretiens.stream()
                .filter(e -> "proposé".equals(e.getStatut())).count();
        long termines  = allEntretiens.stream()
                .filter(e -> "réalisé".equals(e.getStatut())).count();

        LocalDate today       = LocalDate.now();
        LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDate endOfWeek   = startOfWeek.plusDays(6);
        long cetteSemaine = allEntretiens.stream()
                .filter(e -> e.getDateEntretien() != null)
                .filter(e -> {
                    LocalDate d = e.getDateEntretien().toLocalDate();
                    return !d.isBefore(startOfWeek) && !d.isAfter(endOfWeek);
                }).count();

        planifiesCount.setText(String.valueOf(planifies));
        terminesCount .setText(String.valueOf(termines));
        semaineCount  .setText(String.valueOf(cetteSemaine));
    }

    // ════════════════════════════════════════════════════════════════
    //  FILTRE + AFFICHAGE
    // ════════════════════════════════════════════════════════════════
    private void filterAndDisplay() {
        entretiensVBox.getChildren().clear();
        if (entretiensFiltres == null || entretiensFiltres.isEmpty()) {
            entretiensVBox.getChildren().add(emptyLabel("Aucun entretien en cours (proposé)."));
            return;
        }
        String  search    = searchField.getText().trim().toLowerCase();
        String  selType   = comboType.getValue();
        String  selStatut = comboStatut.getValue();
        boolean auMoinsUn = false;

        for (Entretien e : entretiensFiltres) {
            try {
                String       titreOffre = service.getOffreTitre(e.getIdOffre()).toLowerCase();
                List<String> parts      = service.getParticipants(e.getIdEntretien());
                String       partStr    = String.join(" ", parts).toLowerCase();

                boolean matchSearch = search.isEmpty()
                        || titreOffre.contains(search) || partStr.contains(search);
                boolean matchType   = "Tous les types".equals(selType)    || selType.equals(e.getTypeEntretien());
                boolean matchStatut = "Tous les statuts".equals(selStatut) || selStatut.equals(e.getStatut());

                if (matchSearch && matchType && matchStatut) {
                    entretiensVBox.getChildren().add(
                            createEntretienCard(e, parts, service.getOffreTitre(e.getIdOffre())));
                    auMoinsUn = true;
                }
            } catch (SQLException ex) {
                System.err.println("[filterAndDisplay] Erreur entretien #" + e.getIdEntretien() + " : " + ex.getMessage());
            }
        }
        if (!auMoinsUn)
            entretiensVBox.getChildren().add(emptyLabel("Aucun résultat pour ces filtres."));
    }

    private Label emptyLabel(String msg) {
        Label l = new Label(msg);
        l.setStyle("-fx-font-size:14px; -fx-text-fill:#94A3B8; -fx-padding:30;");
        return l;
    }

    // ════════════════════════════════════════════════════════════════
    //  CARTE ENTRETIEN
    // ════════════════════════════════════════════════════════════════
    private Node createEntretienCard(Entretien e, List<String> participants, String titreOffre) {
        HBox card = new HBox(15);
        card.getStyleClass().add("card");
        card.setPrefHeight(130);

        // Avatar
        String    initials   = participants.isEmpty() ? "C" : buildInitials(participants.get(0));
        Circle    avatar     = new Circle(25);
        avatar.getStyleClass().add("avatar");
        Label     avatarLbl  = new Label(initials);
        avatarLbl.setStyle("-fx-font-size:18px; -fx-font-weight:bold; -fx-text-fill:white;");
        StackPane avatarPane = new StackPane(avatar, avatarLbl);
        VBox      avatarBox  = new VBox(avatarPane);
        avatarBox.setAlignment(javafx.geometry.Pos.CENTER);
        avatarBox.setPrefWidth(70);

        // Détails
        VBox details = new VBox(8);
        details.setPrefWidth(400);
        details.setPadding(new Insets(5, 0, 5, 0));

        Label offreLabel = new Label(titreOffre);
        offreLabel.setStyle("-fx-font-size:18px; -fx-font-weight:bold;");

        Label participantsLabel = participants.size() > 1
                ? new Label("👥 " + String.join(", ", participants))
                : new Label("👤 " + (participants.isEmpty() ? "Aucun candidat" : participants.get(0)));
        participantsLabel.setStyle("-fx-font-size:14px; -fx-text-fill:#2c3e50; -fx-font-weight:500;");

        String dateStr  = e.getDateEntretien() != null
                ? e.getDateEntretien().toLocalDate().format(DateTimeFormatter.ofPattern("EEE dd MMM yyyy"))
                : "Date non définie";
        String heureStr = e.getHeureDebut() != null
                ? e.getHeureDebut().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")) : "--:--";
        long duree = calculateDuration(e);
        Label dateTimeLabel = new Label("📅 " + dateStr + " à " + heureStr
                + (duree > 0 ? "  (Durée : " + duree + " min)" : "  (Durée non définie)"));
        dateTimeLabel.setStyle("-fx-font-size:13px; -fx-text-fill:#7f8c8d;");

        Label lieuVisioLabel;
        if ("présentiel".equals(e.getTypeEntretien())) {
            String lieu = (e.getLieu() != null && !e.getLieu().isEmpty()) ? e.getLieu() : "Lieu non défini";
            lieuVisioLabel = new Label("📍 " + lieu);
        } else {
            String lien = (e.getLienVisio() != null && !e.getLienVisio().isEmpty())
                    ? e.getLienVisio() : "Lien non défini";
            if (lien.length() > 30) lien = lien.substring(0, 27) + "...";
            lieuVisioLabel = new Label("🔗 " + lien);
        }
        lieuVisioLabel.setStyle("-fx-font-size:13px; -fx-text-fill:#1e3a8a; -fx-font-weight:500;");

        details.getChildren().addAll(offreLabel, participantsLabel, dateTimeLabel,
                lieuVisioLabel, buildStatutBadge(e.getStatut()));

        if (e.getNoteRecruteur() != null && !e.getNoteRecruteur().isEmpty()) {
            Label noteLabel = new Label("📝 Note : " + e.getNoteRecruteur());
            noteLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#1e3a8a; -fx-font-style:italic;");
            details.getChildren().add(noteLabel);
        }

        // Actions
        boolean estAujourdhui = e.getDateEntretien() != null
                && e.getDateEntretien().toLocalDate().equals(LocalDate.now());

        VBox actionsContainer = new VBox(8);
        actionsContainer.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        actionsContainer.setPrefWidth(320);
        actionsContainer.setPadding(new Insets(5, 0, 5, 0));

        // Ligne 1 : Modifier
        HBox row1 = new HBox(10);
        row1.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        Button btnModifier = new Button("Modifier");
        btnModifier.getStyleClass().add("button-primary");
        btnModifier.setPrefWidth(120);
        btnModifier.setPrefHeight(42);
        if (estAujourdhui) {
            btnModifier.setDisable(true);
            btnModifier.setStyle("-fx-background-color:#E2E8F0; -fx-text-fill:#94A3B8;"
                    + "-fx-font-weight:bold; -fx-background-radius:10px;");
            Tooltip.install(btnModifier,
                    new Tooltip("⚠️ Modification impossible\nL'entretien est prévu aujourd'hui."));
        } else {
            btnModifier.setOnAction(ev -> openForm(true, e));
        }
        row1.getChildren().add(btnModifier);

        // Ligne 2 : Action spécifique + Supprimer
        HBox row2 = new HBox(10);
        row2.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        Button btnAction;
        if ("présentiel".equals(e.getTypeEntretien())) {
            btnAction = new Button("Consulter map");
            btnAction.getStyleClass().add("button-map");
            btnAction.setPrefWidth(155);
            btnAction.setPrefHeight(42);
            if (estAujourdhui) {
                btnAction.setOnAction(ev -> consulterMap(e));
            } else {
                btnAction.setDisable(true);
                btnAction.setStyle("-fx-background-color:#E2E8F0; -fx-text-fill:#94A3B8;"
                        + "-fx-font-weight:bold; -fx-background-radius:10px;");
            }
        } else {
            btnAction = new Button("📹 Rejoindre");
            btnAction.getStyleClass().add("button-visio");
            btnAction.setPrefWidth(130);
            btnAction.setPrefHeight(42);
            btnAction.setOnAction(ev -> rejoindre(e));
            if (e.getLienVisio() == null || e.getLienVisio().isBlank()) {
                btnAction.setDisable(true);
                btnAction.setText("❌ Lien indisponible");
                btnAction.setPrefWidth(160);
            } else if (e.getDateEntretien() != null) {
                LocalDate dateE = e.getDateEntretien().toLocalDate();
                LocalDate auj   = LocalDate.now();
                if (!dateE.equals(auj)) {
                    btnAction.setDisable(true);
                    btnAction.setText(dateE.isAfter(auj) ? "⏳ Pas encore" : "⛔ Expiré");
                    btnAction.setPrefWidth(140);
                }
            }
        }

        Button btnSupprimer = new Button("Supprimer");
        btnSupprimer.setPrefWidth(120);
        btnSupprimer.setPrefHeight(42);
        if ("proposé".equals(e.getStatut()) && !estAujourdhui) {
            btnSupprimer.getStyleClass().add("button-danger");
            btnSupprimer.setOnAction(ev -> supprimerEntretien(e));
        } else if ("proposé".equals(e.getStatut()) && estAujourdhui) {
            btnSupprimer.setDisable(true);
            btnSupprimer.setStyle("-fx-background-color:#E2E8F0; -fx-text-fill:#94A3B8;"
                    + "-fx-font-weight:bold; -fx-background-radius:10px;");
        } else {
            btnSupprimer.setVisible(false);
            btnSupprimer.setManaged(false);
        }

        row2.getChildren().addAll(btnAction, btnSupprimer);
        actionsContainer.getChildren().addAll(row1, row2);

        HBox.setHgrow(details, Priority.ALWAYS);
        card.getChildren().addAll(avatarBox, details, actionsContainer);
        return card;
    }

    // ════════════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════════════
    private String buildInitials(String name) {
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 0) return "?";
        StringBuilder sb = new StringBuilder();
        sb.append(Character.toUpperCase(parts[0].charAt(0)));
        if (parts.length > 1) sb.append(Character.toUpperCase(parts[parts.length - 1].charAt(0)));
        return sb.toString();
    }

    private Label buildStatutBadge(String statut) {
        String emoji, color, bgColor;
        switch (statut == null ? "" : statut) {
            case "proposé":  emoji = "🕐"; color = "#D97706"; bgColor = "#FEF3C7"; break;
            case "confirmé": emoji = "✅"; color = "#1D4ED8"; bgColor = "#DBEAFE"; break;
            default:         emoji = "❓"; color = "#6B7280"; bgColor = "#F3F4F6";
        }
        Label badge = new Label(emoji + " " + (statut != null ? statut : "inconnu"));
        badge.setStyle("-fx-background-color:" + bgColor + "; -fx-text-fill:" + color + ";"
                + "-fx-font-size:11px; -fx-font-weight:700; -fx-padding:3 10 3 10;"
                + "-fx-background-radius:20px;");
        return badge;
    }

    private long calculateDuration(Entretien e) {
        if (e.getHeureDebut() == null || e.getHeureFin() == null) return 0;
        return TimeUnit.MILLISECONDS.toMinutes(
                e.getHeureFin().getTime() - e.getHeureDebut().getTime());
    }

    // ════════════════════════════════════════════════════════════════
    //  ACTIONS ENTRETIEN
    // ════════════════════════════════════════════════════════════════
    private void openForm(boolean edition, Entretien ent) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/tn/jobnest/gentretien/entretien-form.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            EntretienFormController ctrl = loader.getController();
            if (edition && ent != null) {
                ctrl.setEntretien(ent);
                stage.setTitle("Modifier l'entretien");
            } else {
                stage.setTitle("Ajouter un entretien");
            }
            stage.showAndWait();
            rafraichirListe();
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'ouvrir le formulaire : " + ex.getMessage());
        }
    }

    private void rejoindre(Entretien e) {
        if (e.getDateEntretien() != null) {
            LocalDate dateE = e.getDateEntretien().toLocalDate();
            LocalDate auj   = LocalDate.now();
            if (!dateE.equals(auj)) {
                showAlert(Alert.AlertType.WARNING, "Accès impossible",
                        dateE.isAfter(auj)
                                ? "Cet entretien est prévu pour le "
                                + dateE.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                + ".\nVous ne pouvez rejoindre que le jour J."
                                : "La date de cet entretien est passée.");
                return;
            }
        }
        if ("visio".equals(e.getTypeEntretien())
                && e.getLienVisio() != null && !e.getLienVisio().isBlank()) {
            try { Desktop.getDesktop().browse(new URI(e.getLienVisio())); }
            catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Erreur",
                        "Impossible d'ouvrir le lien : " + ex.getMessage());
            }
        }
    }

    private void consulterMap(Entretien e) {
        if (!"présentiel".equals(e.getTypeEntretien())) return;
        if (e.getLieu() == null || e.getLieu().isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Adresse manquante",
                    "Aucune adresse n'est définie pour cet entretien présentiel.");
            return;
        }
        try {
            String     titrePoste = service.getOffreTitre(e.getIdOffre());
            FXMLLoader loader     = new FXMLLoader(
                    getClass().getResource("/tn/jobnest/gentretien/map-view.fxml"));
            Parent     root       = loader.load();
            MapController mapCtrl = loader.getController();
            mapCtrl.setAdresse(e.getLieu(), "📅 " + titrePoste);
            Stage mapStage = new Stage();
            mapStage.setTitle("JobNest – Carte : " + e.getLieu());
            mapStage.setScene(new Scene(root));
            mapStage.setMinWidth(700); mapStage.setMinHeight(500);
            mapStage.setWidth(960);    mapStage.setHeight(680);
            mapStage.centerOnScreen();
            mapStage.show();
        } catch (IOException | SQLException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'ouvrir la carte : " + ex.getMessage());
        }
    }

    private void supprimerEntretien(Entretien e) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer cet entretien ?");
        confirm.setContentText("Cette action est irréversible.");
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            Thread t = new Thread(() -> {
                try {
                    service.delete(e.getIdEntretien());
                    Platform.runLater(() -> {
                        rafraichirListe();
                        showAlert(Alert.AlertType.INFORMATION, "Succès",
                                "Entretien supprimé avec succès.");
                    });
                } catch (SQLException ex) {
                    Platform.runLater(() ->
                            showAlert(Alert.AlertType.ERROR, "Erreur",
                                    "Échec de la suppression : " + ex.getMessage()));
                }
            });
            t.setDaemon(true);
            t.start();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // ════════════════════════════════════════════════════════════════
    //  NAVIGATION SIDEBAR
    // ════════════════════════════════════════════════════════════════
    @FXML private void ouvrirOffresEmploi(ActionEvent e) { navigateTo(e, "/tn/jobnest/gentretien/offre-emploi_view.fxml",   "JobNest - Offres d'Emploi"); }
    @FXML private void ouvrirFeedbacks   (ActionEvent e) { navigateTo(e, "/tn/jobnest/gentretien/feedback-interface.fxml",  "JobNest - Gestion des Feedbacks"); }
    @FXML private void ouvrirHistorique  (ActionEvent e) { navigateTo(e, "/tn/jobnest/gentretien/historique-entretien.fxml","JobNest - Historique des Entretiens"); }
    @FXML private void ouvrirCandidature (ActionEvent e) { navigateTo(e, "/tn/jobnest/gentretien/GestionCandidatures.fxml", "JobNest - Gestion des Candidatures"); }
    @FXML private void ouvrirProfil      (ActionEvent e) { navigateTo(e, "/tn/jobnest/gentretien/profil-recruteur.fxml",    "JobNest - Mon Profil"); }
    @FXML private void ouvrirMatching    (ActionEvent e) { navigateTo(e, "/tn/jobnest/gentretien/matching-view.fxml",       "JobNest - Matching"); }

    private void navigateTo(ActionEvent event, String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            java.net.URL css = getClass().getResource("/tn/jobnest/gentretien/styles.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            stage.setScene(scene);
            stage.setTitle(title);
            stage.show();
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'ouvrir la page : " + ex.getMessage());
        }
    }
}