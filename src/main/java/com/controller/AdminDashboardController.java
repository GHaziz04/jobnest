package com.controller;

import com.utils.DBConnection;
import com.utils.EmailSender;
import com.utils.SessionManager;
import com.utils.StageUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class AdminDashboardController {

    // ===== SIDEBAR =====
    @FXML private Label  adminAvatarLabel;
    @FXML private Label  adminNameLabel;
    @FXML private Button navDashboard;
    @FXML private Button navUsers;
    @FXML private Button navOffres;
    @FXML private Button navFormations;

    // ===== TOP BAR =====
    @FXML private Label pageTitleLabel;
    @FXML private Label welcomeLabel;
    @FXML private Label dateLabel;
    @FXML private Label systemStatusLabel;

    // ===== KPI ROW 1 =====
    @FXML private Label       totalUsersLabel;
    @FXML private Label       candidatsLabel;
    @FXML private Label       recruteursLabel;
    @FXML private Label       formateursLabel;
    @FXML private ProgressBar progressUsers;
    @FXML private ProgressBar progressCandidats;
    @FXML private ProgressBar progressRecruteurs;
    @FXML private ProgressBar progressFormateurs;
    @FXML private Label       trendUsersLabel;

    // ===== KPI ROW 2 =====
    @FXML private Label comptesBloqueLabel;
    @FXML private Label inscriptionsAujourdLabel;
    @FXML private Label comptesActifsLabel;
    @FXML private Label demandesAttenteLabel;

    // ===== TOOLBAR =====
    @FXML private TextField        searchField;
    @FXML private ComboBox<String> roleFilterCombo;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private ComboBox<String> sortCombo;

    // ===== TABLE =====
    @FXML private TableView<User>            usersTable;
    @FXML private TableColumn<User, Boolean> selectColumn;
    @FXML private TableColumn<User, Integer> idColumn;
    @FXML private TableColumn<User, String>  prenomColumn;
    @FXML private TableColumn<User, String>  nomColumn;
    @FXML private TableColumn<User, String>  emailColumn;
    @FXML private TableColumn<User, String>  roleColumn;
    @FXML private TableColumn<User, String>  telephoneColumn;
    @FXML private TableColumn<User, String>  statusColumn;
    @FXML private TableColumn<User, String>  dateCreationColumn;
    @FXML private TableColumn<User, Void>    actionsColumn;

    // ===== PAGINATION =====
    @FXML private Label  paginationInfoLabel;
    @FXML private Label  tableSubtitle;
    @FXML private Button page1Btn;
    @FXML private Button page2Btn;
    @FXML private Button page3Btn;

    // ===== QUICK STATS =====
    @FXML private Label       tauxActivationLabel;
    @FXML private ProgressBar tauxActivationBar;

    // ===== DATA =====
    private final ObservableList<User> usersList = FXCollections.observableArrayList();
    private FilteredList<User> filteredData;

    private static final int PAGE_SIZE = 25;
    private int currentPage = 1;
    private int totalPages  = 1;

    // =================================================================
    @FXML
    public void initialize() {
        if (!SessionManager.isLoggedIn()) { redirectToLogin(); return; }
        if (!isAdmin())                    { redirectToHome();  return; }
        setupAdminInfo();
        setupComboBoxes();
        setupTable();
        loadStatistics();
        loadUsers();
        setupSearchAndFilter();
        updateDateLabel();
    }

    // ─────────────────────────────────────────────────────────────────
    //  SETUP
    // ─────────────────────────────────────────────────────────────────
    private void setupAdminInfo() {
        String prenom = SessionManager.getInstance().getPrenom();
        String nom    = SessionManager.getInstance().getNom();
        welcomeLabel.setText("Bonjour, " + prenom + " 👋");
        adminNameLabel.setText(prenom + " " + (nom != null ? nom : ""));
        adminAvatarLabel.setText(prenom != null && !prenom.isEmpty()
                ? String.valueOf(prenom.charAt(0)).toUpperCase() : "A");
    }

    private void setupComboBoxes() {
        roleFilterCombo.setItems(FXCollections.observableArrayList(
                "Tous les rôles","Candidat","Recruteur","Formateur","Admin"));
        roleFilterCombo.setValue("Tous les rôles");
        statusFilterCombo.setItems(FXCollections.observableArrayList(
                "Tous les statuts","Actif","Bloqué","Non vérifié"));
        statusFilterCombo.setValue("Tous les statuts");
        sortCombo.setItems(FXCollections.observableArrayList(
                "Trier par...","Date (récent)","Date (ancien)","Nom A→Z","Nom Z→A","Rôle"));
        sortCombo.setValue("Trier par...");
    }

    private void updateDateLabel() {
        dateLabel.setText(LocalDate.now()
                .format(DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy", java.util.Locale.FRENCH)));
    }

    private void setupTable() {
        usersTable.setEditable(true);

        selectColumn.setCellValueFactory(cd -> cd.getValue().selectedProperty());
        selectColumn.setCellFactory(CheckBoxTableCell.forTableColumn(selectColumn));
        selectColumn.setEditable(true);

        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        prenomColumn.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        nomColumn.setCellValueFactory(new PropertyValueFactory<>("nom"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        telephoneColumn.setCellValueFactory(new PropertyValueFactory<>("telephone"));
        dateCreationColumn.setCellValueFactory(new PropertyValueFactory<>("dateInscription"));

        // Badge rôle coloré
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        roleColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String role, boolean empty) {
                super.updateItem(role, empty);
                if (empty || role == null) { setGraphic(null); return; }
                Label b = new Label(role);
                b.getStyleClass().add(switch (role.toLowerCase()) {
                    case "recruteur" -> "badge-recruteur";
                    case "formateur" -> "badge-formateur";
                    case "admin"     -> "badge-admin";
                    default          -> "badge-candidat";
                });
                setGraphic(b); setText(null);
            }
        });

        // Bouton statut cliquable
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("statut"));
        statusColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); return; }
                Button btn = new Button(s);
                btn.getStyleClass().add("Actif".equalsIgnoreCase(s) ? "toggle-active" : "toggle-inactive");
                btn.setTooltip(new Tooltip("Cliquer pour " + ("Actif".equalsIgnoreCase(s) ? "bloquer" : "réactiver")));
                btn.setOnAction(e -> toggleUserStatus(getTableView().getItems().get(getIndex())));
                setGraphic(btn); setText(null);
            }
        });

        // Actions
        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button vBtn = new Button("👁");
            private final Button eBtn = new Button("✏️");
            private final Button dBtn = new Button("🗑️");
            private final HBox   box  = new HBox(5, vBtn, eBtn, dBtn);
            {
                vBtn.getStyleClass().add("view-btn");
                eBtn.getStyleClass().add("edit-btn");
                dBtn.getStyleClass().add("delete-btn");
                box.setStyle("-fx-alignment:center;");
                vBtn.setOnAction(e -> viewUser  (getTableView().getItems().get(getIndex())));
                eBtn.setOnAction(e -> editUser  (getTableView().getItems().get(getIndex())));
                dBtn.setOnAction(e -> deleteUser(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty); setGraphic(empty ? null : box);
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────
    //  CHARGEMENT DONNÉES
    // ─────────────────────────────────────────────────────────────────
    private void loadStatistics() {
        try (Connection c = DBConnection.getConnection(); Statement s = c.createStatement()) {
            int total = q(s,"SELECT COUNT(*) FROM users");
            int cand  = q(s,"SELECT COUNT(*) FROM users WHERE role='Candidat'");
            int recr  = q(s,"SELECT COUNT(*) FROM users WHERE role='Recruteur'");
            int form  = q(s,"SELECT COUNT(*) FROM users WHERE role='Formateur'");
            int blq   = q(s,"SELECT COUNT(*) FROM users WHERE statut='Bloqué'");
            int act   = q(s,"SELECT COUNT(*) FROM users WHERE statut='Actif' OR statut IS NULL");
            int today = q(s,"SELECT COUNT(*) FROM users WHERE DATE(date_inscription)=CURDATE()");

            totalUsersLabel.setText(String.valueOf(total));
            candidatsLabel.setText(String.valueOf(cand));
            recruteursLabel.setText(String.valueOf(recr));
            formateursLabel.setText(String.valueOf(form));
            comptesBloqueLabel.setText(String.valueOf(blq));
            comptesActifsLabel.setText(String.valueOf(act));
            inscriptionsAujourdLabel.setText(String.valueOf(today));
            demandesAttenteLabel.setText(String.valueOf(blq));

            if (total > 0) {
                progressCandidats.setProgress((double)cand/total);
                progressRecruteurs.setProgress((double)recr/total);
                progressFormateurs.setProgress((double)form/total);
                progressUsers.setProgress(Math.min((double)total/500, 1.0));
                double taux = (double)act/total;
                tauxActivationLabel.setText(String.format("%.1f%%", taux*100));
                tauxActivationBar.setProgress(taux);
            }
            if (tableSubtitle != null) tableSubtitle.setText(total + " comptes enregistrés");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadUsers() {
        usersList.clear();
        try (Connection c = DBConnection.getConnection();
             ResultSet rs  = c.createStatement().executeQuery(
                     "SELECT * FROM users ORDER BY date_inscription DESC")) {
            while (rs.next()) {
                User u = new User(
                        rs.getInt("id_user"), rs.getString("prenom"), rs.getString("nom"),
                        rs.getString("email"), rs.getString("role"), rs.getString("telephone"),
                        rs.getTimestamp("date_inscription") != null
                                ? rs.getTimestamp("date_inscription").toLocalDateTime() : null);
                u.setAdresse(rs.getString("adresse"));
                try { u.setStatut(rs.getString("statut")); }   catch (SQLException ig) {}
                try { u.setLastLogin(rs.getString("last_login")); } catch (SQLException ig) {}
                usersList.add(u);
            }
            updatePaginationInfo();
        } catch (Exception e) { showAlert("Erreur", e.getMessage()); }
    }

    private void setupSearchAndFilter() {
        filteredData = new FilteredList<>(usersList, p -> true);
        searchField.textProperty().addListener((o,ov,nv) -> applyFilters());
        roleFilterCombo.valueProperty().addListener((o,ov,nv)   -> applyFilters());
        statusFilterCombo.valueProperty().addListener((o,ov,nv) -> applyFilters());
        sortCombo.valueProperty().addListener((o,ov,nv)         -> applySort());
        SortedList<User> sorted = new SortedList<>(filteredData);
        sorted.comparatorProperty().bind(usersTable.comparatorProperty());
        usersTable.setItems(sorted);
    }

    private void applyFilters() {
        filteredData.setPredicate(u -> {
            String src  = searchField.getText().toLowerCase().trim();
            String role = roleFilterCombo.getValue();
            String stat = statusFilterCombo.getValue();
            boolean ok  = src.isEmpty()
                    || u.getPrenom().toLowerCase().contains(src)
                    || u.getNom().toLowerCase().contains(src)
                    || u.getEmail().toLowerCase().contains(src)
                    || u.getTelephone().contains(src);
            if (role != null && !role.startsWith("Tous"))
                ok = ok && u.getRole().equalsIgnoreCase(role);
            if (stat != null && !stat.startsWith("Tous"))
                ok = ok && u.getStatut().equalsIgnoreCase(stat);
            return ok;
        });
        updatePaginationInfo();
    }

    private void applySort() {
        String s = sortCombo.getValue();
        if (s == null || s.startsWith("Trier")) return;
        ObservableList<User> copy = FXCollections.observableArrayList(usersList);
        switch (s) {
            case "Nom A→Z"       -> copy.sort((a,b) -> a.getNom().compareToIgnoreCase(b.getNom()));
            case "Nom Z→A"       -> copy.sort((a,b) -> b.getNom().compareToIgnoreCase(a.getNom()));
            case "Rôle"          -> copy.sort((a,b) -> a.getRole().compareToIgnoreCase(b.getRole()));
            case "Date (récent)" -> copy.sort((a,b) -> b.getDateInscription().compareTo(a.getDateInscription()));
            case "Date (ancien)" -> copy.sort((a,b) -> a.getDateInscription().compareTo(b.getDateInscription()));
        }
        usersList.setAll(copy);
    }

    // ─────────────────────────────────────────────────────────────────
    //  TOGGLE STATUT — Bloque / Réactive + email auto via EmailSender
    // ─────────────────────────────────────────────────────────────────
    private void toggleUserStatus(User user) {
        boolean wasActive = "Actif".equalsIgnoreCase(user.getStatut());
        String  newStatus = wasActive ? "Bloqué" : "Actif";

        if (!confirm("Confirmer", "Voulez-vous " + (wasActive ? "bloquer" : "réactiver")
                + " le compte de " + user.getPrenom() + " " + user.getNom() + " ?")) return;

        // 1 — Mise à jour BDD
        try (Connection c = DBConnection.getConnection()) {
            PreparedStatement ps = c.prepareStatement("UPDATE users SET statut=? WHERE id_user=?");
            ps.setString(1, newStatus); ps.setInt(2, user.getId());
            if (ps.executeUpdate() > 0) { user.setStatut(newStatus); usersTable.refresh(); loadStatistics(); }
        } catch (Exception e) { showAlert("Erreur BDD", e.getMessage()); return; }

        // 2 — Email asynchrone via EmailSender.sendOTP réutilisé avec sujet/corps personnalisés
        //     On envoie un email HTML styled en utilisant EmailSender
        final String email  = user.getEmail();
        final String prenom = user.getPrenom();
        final boolean blocked = wasActive;

        Task<Void> t = new Task<>() {
            @Override protected Void call() throws Exception {
                if (blocked) {
                    // Compte bloqué
                    EmailSender.sendBlockedNotification(email, prenom);
                } else {
                    // Compte réactivé
                    EmailSender.sendReactivatedNotification(email, prenom);
                }
                return null;
            }
        };
        t.setOnSucceeded(e -> Platform.runLater(() ->
                showSuccess("Compte " + newStatus.toLowerCase() + " ✅\nEmail envoyé à " + email)));
        t.setOnFailed(e -> Platform.runLater(() ->
                showSuccess("Compte " + newStatus.toLowerCase() + " ✅\n⚠️ Email non envoyé — vérifiez la config SMTP")));
        new Thread(t, "email-toggle").start();
    }

    // ─────────────────────────────────────────────────────────────────
    //  EMAIL EN MASSE — via EmailSender.sendBulkAnnouncement
    // ─────────────────────────────────────────────────────────────────
    @FXML private void sendBulkEmail() {
        // Formulaire de saisie
        TextField subj = new TextField(); subj.setPromptText("Objet de l'email...");
        TextArea  body = new TextArea();
        body.setPromptText("Contenu du message...");
        body.setPrefRowCount(6); body.setWrapText(true);

        VBox content = new VBox(8, new Label("Objet :"), subj, new Label("Message :"), body);
        content.setPrefWidth(460);
        content.setStyle("-fx-padding:10px;");

        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("📨 Email en masse");
        dlg.setHeaderText("Envoyer un email à tous les utilisateurs actifs");
        dlg.getDialogPane().setContent(content);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.getDialogPane().setPrefWidth(500);

        if (dlg.showAndWait().filter(b -> b == ButtonType.OK).isEmpty()) return;

        String subject = subj.getText().trim();
        String message = body.getText().trim();

        if (subject.isEmpty() || message.isEmpty()) {
            showAlert("Champs vides", "Veuillez remplir l'objet et le message."); return;
        }

        // Construire la liste des destinataires actifs
        List<EmailSender.Recipient> recipients = usersList.stream()
                .filter(u -> "Actif".equalsIgnoreCase(u.getStatut()))
                .map(u -> new EmailSender.Recipient(u.getEmail(), u.getPrenom()))
                .collect(Collectors.toList());

        if (recipients.isEmpty()) { showAlert("Info", "Aucun utilisateur actif trouvé."); return; }
        if (!confirm("Confirmer l'envoi", "Envoyer à " + recipients.size() + " utilisateurs actifs ?")) return;

        // Barre de progression
        ProgressDialog pd = new ProgressDialog(recipients.size(), searchField);

        final String finalSubject = subject;
        final String finalMessage = message;

        Task<Integer> task = new Task<>() {
            @Override protected Integer call() {
                return EmailSender.sendBulkAnnouncement(
                        recipients, finalSubject, finalMessage,
                        (current, total, email, success) ->
                                Platform.runLater(() -> pd.update(current))
                );
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            pd.close();
            showSuccess("✅ Envoyé à " + task.getValue() + "/" + recipients.size() + " utilisateurs");
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            pd.close();
            showAlert("Erreur SMTP", task.getException().getMessage());
        }));

        pd.show();
        new Thread(task, "bulk-email").start();
    }

    // ─────────────────────────────────────────────────────────────────
    //  RÉINITIALISATION MDP — via EmailSender.sendPasswordReset
    // ─────────────────────────────────────────────────────────────────
    @FXML private void resetAllPasswords() {
        User sel = usersTable.getSelectionModel().getSelectedItem();

        List<String> options = new ArrayList<>();
        if (sel != null) options.add("Utilisateur sélectionné (" + sel.getPrenom() + " " + sel.getNom() + ")");
        options.add("Tous les utilisateurs (sauf Admin)");

        ChoiceDialog<String> cd = new ChoiceDialog<>(options.get(0), options);
        cd.setTitle("Réinitialisation MDP"); cd.setHeaderText(null);
        cd.setContentText("Réinitialiser pour :");
        Optional<String> r = cd.showAndWait();
        if (r.isEmpty()) return;

        if (r.get().startsWith("Utilisateur") && sel != null) {
            resetSinglePassword(sel);
        } else {
            resetAllPasswordsBulk();
        }
    }

    private void resetSinglePassword(User user) {
        if (!confirm("Réinitialiser le MDP de " + user.getPrenom() + " ?",
                "Un email avec un mot de passe temporaire sera envoyé.")) return;

        String tmpPwd = generateTempPassword();

        // 1 — Mise à jour BDD
        try (Connection c = DBConnection.getConnection()) {
            PreparedStatement ps = c.prepareStatement("UPDATE users SET mot_de_passe=? WHERE id_user=?");
            ps.setString(1, tmpPwd); ps.setInt(2, user.getId());
            ps.executeUpdate();
        } catch (Exception e) { showAlert("Erreur BDD", e.getMessage()); return; }

        // 2 — Email via EmailSender.sendPasswordReset
        Task<Void> t = new Task<>() {
            @Override protected Void call() throws Exception {
                EmailSender.sendPasswordReset(user.getEmail(), user.getPrenom(), tmpPwd);
                return null;
            }
        };
        t.setOnSucceeded(e -> Platform.runLater(() ->
                showSuccess("✅ MDP réinitialisé\nEmail envoyé à " + user.getEmail())));
        t.setOnFailed(e -> Platform.runLater(() ->
                showSuccess("✅ MDP réinitialisé : " + tmpPwd + "\n⚠️ Email non envoyé — vérifiez SMTP")));
        new Thread(t, "reset-pwd").start();
    }

    private void resetAllPasswordsBulk() {
        if (!confirm("Réinitialiser TOUS les mots de passe ?",
                "Un email sera envoyé à chaque utilisateur (sauf les Admins).")) return;

        Task<Integer> task = new Task<>() {
            @Override protected Integer call() throws Exception {
                int count = 0;
                try (Connection c = DBConnection.getConnection()) {
                    PreparedStatement upd = c.prepareStatement(
                            "UPDATE users SET mot_de_passe=? WHERE id_user=?");
                    ResultSet rs = c.createStatement().executeQuery(
                            "SELECT id_user, prenom, email FROM users WHERE role != 'Admin'");

                    while (rs.next()) {
                        String tmp    = generateTempPassword();
                        String prenom = rs.getString("prenom");
                        String email  = rs.getString("email");
                        int    id     = rs.getInt("id_user");

                        upd.setString(1, tmp); upd.setInt(2, id); upd.addBatch();

                        try {
                            EmailSender.sendPasswordReset(email, prenom, tmp);
                        } catch (Exception ignored) {
                            System.err.println("Email non envoyé à : " + email);
                        }
                        count++;
                    }
                    upd.executeBatch();
                }
                return count;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() ->
                showSuccess("✅ " + task.getValue() + " mots de passe réinitialisés\nEmails envoyés.")));
        task.setOnFailed(e -> Platform.runLater(() ->
                showAlert("Erreur", task.getException().getMessage())));
        new Thread(task, "reset-all-pwd").start();
    }

    // ─────────────────────────────────────────────────────────────────
    //  VERROUILLER COMPTES INACTIFS
    // ─────────────────────────────────────────────────────────────────
    @FXML private void lockInactiveAccounts() {
        ChoiceDialog<String> cd = new ChoiceDialog<>("90 jours",
                "30 jours","60 jours","90 jours","180 jours","1 an");
        cd.setTitle("Inactivité"); cd.setHeaderText("Bloquer les comptes inactifs depuis :");
        Optional<String> res = cd.showAndWait();
        if (res.isEmpty()) return;

        int jours = switch (res.get()) {
            case "30 jours"  ->  30; case "60 jours"  ->  60;
            case "180 jours" -> 180; case "1 an"      -> 365; default -> 90;
        };
        if (!confirm("Verrouiller", "Bloquer les comptes sans activité depuis " + jours + " jours ?")) return;

        try (Connection conn = DBConnection.getConnection()) {
            boolean hasLL = colExists(conn, "users", "last_login");
            String  sql   = hasLL
                    ? "UPDATE users SET statut='Bloqué' WHERE statut='Actif' AND role!='Admin' AND (last_login IS NULL OR last_login < DATE_SUB(NOW(), INTERVAL ? DAY))"
                    : "UPDATE users SET statut='Bloqué' WHERE statut='Actif' AND role!='Admin' AND date_inscription < DATE_SUB(NOW(), INTERVAL ? DAY)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, jours);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                // Envoyer emails en arrière-plan
                new Thread(() -> {
                    try (Connection c2 = DBConnection.getConnection();
                         ResultSet rs  = c2.createStatement().executeQuery(
                                 "SELECT prenom, email FROM users WHERE statut='Bloqué' AND role!='Admin'")) {
                        while (rs.next()) {
                            try {
                                EmailSender.sendBlockedNotification(
                                        rs.getString("email"), rs.getString("prenom"));
                            } catch (Exception ig) {}
                        }
                    } catch (Exception ig) {}
                }, "lock-emails").start();

                refresh();
                showSuccess(rows + " compte(s) verrouillé(s) ✅\nNotifications envoyées.");
            } else {
                showAlert("Info", "Aucun compte inactif trouvé pour cette période.");
            }
        } catch (Exception e) { showAlert("Erreur", e.getMessage()); }
    }

    // ─────────────────────────────────────────────────────────────────
    //  EXPORT CSV — format Excel parfait
    // ─────────────────────────────────────────────────────────────────
    @FXML private void exportCSV() {
        ChoiceDialog<String> cd = new ChoiceDialog<>("Tous les utilisateurs",
                "Tous les utilisateurs","Vue filtrée actuelle",
                "Actifs seulement","Bloqués seulement","Candidats","Recruteurs","Formateurs");
        cd.setTitle("Export CSV Excel"); cd.setHeaderText("Données à exporter :");
        Optional<String> choice = cd.showAndWait();
        if (choice.isEmpty()) return;

        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Excel (*.csv)","*.csv"));
        fc.setInitialFileName("jobnest_users_" + LocalDate.now() + ".csv");
        File file = fc.showSaveDialog(searchField.getScene().getWindow());
        if (file == null) return;

        List<User> data = switch (choice.get()) {
            case "Vue filtrée actuelle" -> filteredData!=null ? new ArrayList<>(filteredData) : new ArrayList<>(usersList);
            case "Actifs seulement"     -> usersList.stream().filter(u->"Actif".equalsIgnoreCase(u.getStatut())).collect(Collectors.toList());
            case "Bloqués seulement"    -> usersList.stream().filter(u->"Bloqué".equalsIgnoreCase(u.getStatut())).collect(Collectors.toList());
            case "Candidats"            -> usersList.stream().filter(u->"Candidat".equalsIgnoreCase(u.getRole())).collect(Collectors.toList());
            case "Recruteurs"           -> usersList.stream().filter(u->"Recruteur".equalsIgnoreCase(u.getRole())).collect(Collectors.toList());
            case "Formateurs"           -> usersList.stream().filter(u->"Formateur".equalsIgnoreCase(u.getRole())).collect(Collectors.toList());
            default                     -> new ArrayList<>(usersList);
        };

        if (data.isEmpty()) { showAlert("Vide","Aucune donnée à exporter."); return; }

        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {

            bw.write('\uFEFF'); // BOM UTF-8 pour Excel

            bw.write(csvRow("ID","Prénom","Nom","Email","Rôle",
                    "Téléphone","Adresse","Statut","Date Inscription","Dernière Connexion"));
            bw.newLine();

            for (User u : data) {
                bw.write(csvRow(
                        String.valueOf(u.getId()),
                        u.getPrenom(), u.getNom(), u.getEmail(), u.getRole(),
                        u.getTelephone(),
                        u.getAdresse()   != null ? u.getAdresse()   : "",
                        u.getStatut(),
                        u.getDateInscription(),
                        u.getLastLogin() != null ? u.getLastLogin() : ""));
                bw.newLine();
            }
            showSuccess("✅ Export réussi — " + data.size() + " lignes\n→ " + file.getName()
                    + "\n\nDouble-cliquez sur le fichier pour l'ouvrir dans Excel.");
        } catch (IOException e) { showAlert("Erreur export", e.getMessage()); }
    }

    /** Ligne CSV : séparateur ; + guillemets doubles */
    private String csvRow(String... vals) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < vals.length; i++) {
            if (i > 0) sb.append(';');
            String v = vals[i] != null ? vals[i].replace("\"","\"\"") : "";
            sb.append('"').append(v).append('"');
        }
        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────────
    //  IMPORT CSV
    // ─────────────────────────────────────────────────────────────────
    @FXML private void importUsers() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Importer CSV");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV","*.csv"));
        File file = fc.showOpenDialog(searchField.getScene().getWindow());
        if (file == null) return;

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line = br.readLine(); // ignorer en-tête
            if (line != null && line.startsWith("\uFEFF")) line = line.substring(1);

            int ok = 0, err = 0;
            try (Connection conn = DBConnection.getConnection()) {
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT IGNORE INTO users (prenom,nom,email,mot_de_passe,role,telephone,adresse,statut,date_inscription) VALUES (?,?,?,?,?,?,?,'Actif',NOW())");
                while ((line = br.readLine()) != null) {
                    if (line.isBlank()) continue;
                    String[] c = line.split("[;,](?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                    if (c.length < 4) { err++; continue; }
                    try {
                        ps.setString(1, cl(c[0])); ps.setString(2, cl(c[1]));
                        ps.setString(3, cl(c[2])); ps.setString(4, "changeme123");
                        ps.setString(5, cl(c[3]));
                        ps.setString(6, c.length>4 ? cl(c[4]) : "");
                        ps.setString(7, c.length>5 ? cl(c[5]) : "");
                        ps.addBatch(); ok++;
                    } catch (Exception ex) { err++; }
                }
                ps.executeBatch();
            }
            refresh();
            showSuccess("Import terminé ✅\n" + ok + " importés, " + err + " erreurs.\nMDP temporaire : changeme123");
        } catch (Exception e) { showAlert("Erreur import", e.getMessage()); }
    }

    // ─────────────────────────────────────────────────────────────────
    //  RAPPORT HTML
    // ─────────────────────────────────────────────────────────────────
    @FXML private void generateReport() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("HTML","*.html"));
        fc.setInitialFileName("rapport_jobnest_" + LocalDate.now() + ".html");
        File file = fc.showSaveDialog(searchField.getScene().getWindow());
        if (file == null) return;
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            pw.println("<!DOCTYPE html><html lang='fr'><head><meta charset='UTF-8'><title>Rapport JobNest</title>");
            pw.println("<style>body{font-family:Arial;background:#0d1117;color:#c9d1d9;padding:30px}h1{color:#58a6ff}");
            pw.println("table{width:100%;border-collapse:collapse;background:#161b22}th{background:#21262d;padding:10px;text-align:left;color:#8b949e}");
            pw.println("td{padding:10px;border-bottom:1px solid #21262d}.actif{background:rgba(63,185,80,.15);color:#3fb950;padding:2px 10px;border-radius:20px;font-size:11px}");
            pw.println(".bloque{background:rgba(248,81,73,.15);color:#f85149;padding:2px 10px;border-radius:20px;font-size:11px}</style></head><body>");
            pw.println("<h1>📊 Rapport JobNest — " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + "</h1>");
            pw.println("<table><tr><th>Indicateur</th><th>Valeur</th></tr>");
            pw.printf("<tr><td>Total</td><td>%s</td></tr><tr><td>Candidats</td><td>%s</td></tr>",totalUsersLabel.getText(),candidatsLabel.getText());
            pw.printf("<tr><td>Recruteurs</td><td>%s</td></tr><tr><td>Formateurs</td><td>%s</td></tr>",recruteursLabel.getText(),formateursLabel.getText());
            pw.printf("<tr><td>Actifs</td><td>%s</td></tr><tr><td>Bloqués</td><td>%s</td></tr></table>",comptesActifsLabel.getText(),comptesBloqueLabel.getText());
            pw.println("<h2 style='margin-top:30px'>Liste</h2><table><tr><th>ID</th><th>Prénom</th><th>Nom</th><th>Email</th><th>Rôle</th><th>Statut</th><th>Date</th></tr>");
            for (User u : usersList) {
                String cls = "Actif".equals(u.getStatut()) ? "actif" : "bloque";
                pw.printf("<tr><td>%d</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td><span class='%s'>%s</span></td><td>%s</td></tr>%n",
                        u.getId(),e(u.getPrenom()),e(u.getNom()),e(u.getEmail()),e(u.getRole()),cls,e(u.getStatut()),e(u.getDateInscription()));
            }
            pw.println("</table></body></html>");
            showSuccess("Rapport généré ✅ → " + file.getName());
        } catch (IOException ex) { showAlert("Erreur", ex.getMessage()); }
    }

    @FXML private void backupDatabase() {
        showAlert("Sauvegarde","Commande :\nmysqldump -u root -p jobnest > backup_" + LocalDate.now() + ".sql");
    }

    // ─────────────────────────────────────────────────────────────────
    //  PURGE COMPTES NON VÉRIFIÉS
    // ─────────────────────────────────────────────────────────────────
    @FXML private void purgeUnverified() {
        try (Connection conn = DBConnection.getConnection()) {
            if (!colExists(conn,"users","email_verified")) {
                showAlert("Info","Colonne email_verified absente.\nExécutez migration.sql."); return;
            }
            ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT COUNT(*) FROM users WHERE email_verified=0 AND date_inscription<DATE_SUB(NOW(),INTERVAL 30 DAY)");
            rs.next(); int count = rs.getInt(1);
            if (count == 0) { showAlert("Info","Aucun compte non vérifié depuis 30+ jours."); return; }
            if (!confirm("Purger "+count+" comptes ?","Action irréversible.")) return;
            int del = conn.createStatement().executeUpdate(
                    "DELETE FROM users WHERE email_verified=0 AND date_inscription<DATE_SUB(NOW(),INTERVAL 30 DAY)");
            refresh(); showSuccess(del + " comptes supprimés ✅");
        } catch (Exception e) { showAlert("Erreur", e.getMessage()); }
    }

    // ─────────────────────────────────────────────────────────────────
    //  CRUD
    // ─────────────────────────────────────────────────────────────────
    @FXML private void addUser() {
        try {
            FXMLLoader l = new FXMLLoader(getClass().getResource("/fxml/UserFormDialog.fxml"));
            Stage dlg = buildDlg("Nouvel Utilisateur", l.load());
            UserFormDialogController ctrl = l.getController(); ctrl.setDialogStage(dlg);
            dlg.showAndWait();
            if (ctrl.isValid()) {
                User u = ctrl.getUser();
                try (Connection c = DBConnection.getConnection()) {
                    PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO users (prenom,nom,email,mot_de_passe,role,telephone,adresse,statut,date_inscription) VALUES (?,?,?,?,?,?,?,'Actif',NOW())");
                    ps.setString(1,u.getPrenom()); ps.setString(2,u.getNom()); ps.setString(3,u.getEmail());
                    ps.setString(4,u.getMotDePasse()!=null?u.getMotDePasse():"changeme");
                    ps.setString(5,u.getRole()); ps.setString(6,u.getTelephone()); ps.setString(7,u.getAdresse());
                    if (ps.executeUpdate()>0) { showSuccess("Ajouté ✅"); refresh(); }
                }
            }
        } catch (Exception e) { showAlert("Erreur",e.getMessage()); }
    }

    private void viewUser(User u) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Fiche #"+u.getId()); a.setHeaderText(u.getPrenom()+" "+u.getNom());
        a.setContentText("📧 "+u.getEmail()+"\n👤 "+u.getRole()+"\n📱 "+u.getTelephone()+
                "\n📍 "+(u.getAdresse()!=null?u.getAdresse():"—")+"\n🔒 "+u.getStatut()+"\n📅 "+u.getDateInscription());
        a.showAndWait();
    }

    private void editUser(User user) {
        try {
            FXMLLoader l = new FXMLLoader(getClass().getResource("/fxml/UserFormDialog.fxml"));
            Stage dlg = buildDlg("Modifier", l.load());
            UserFormDialogController ctrl = l.getController(); ctrl.setDialogStage(dlg); ctrl.setUser(user);
            dlg.showAndWait();
            if (ctrl.isValid()) {
                User u = ctrl.getUser();
                try (Connection c = DBConnection.getConnection()) {
                    PreparedStatement ps = c.prepareStatement(
                            "UPDATE users SET prenom=?,nom=?,email=?,role=?,telephone=?,adresse=?,statut=? WHERE id_user=?");
                    ps.setString(1,u.getPrenom()); ps.setString(2,u.getNom()); ps.setString(3,u.getEmail());
                    ps.setString(4,u.getRole()); ps.setString(5,u.getTelephone()); ps.setString(6,u.getAdresse());
                    ps.setString(7,u.getStatut()); ps.setInt(8,u.getId());
                    if (ps.executeUpdate()>0) { showSuccess("Modifié ✅"); refresh(); }
                }
            }
        } catch (Exception e) { showAlert("Erreur",e.getMessage()); }
    }

    private void deleteUser(User u) {
        if (!confirm("Supprimer "+u.getPrenom()+" "+u.getNom()+" ?","Action irréversible.")) return;
        try (Connection c = DBConnection.getConnection()) {
            PreparedStatement ps = c.prepareStatement("DELETE FROM users WHERE id_user=?");
            ps.setInt(1,u.getId());
            if (ps.executeUpdate()>0) { showSuccess("Supprimé ✅"); refresh(); }
        } catch (Exception e) { showAlert("Erreur",e.getMessage()); }
    }

    @FXML private void deleteSelected() {
        List<User> sel = usersList.stream().filter(User::isSelected).collect(Collectors.toList());
        if (sel.isEmpty()) { showAlert("Info","Cochez des lignes d'abord."); return; }
        if (!confirm("Supprimer "+sel.size()+" utilisateurs ?","Action irréversible.")) return;
        try (Connection c = DBConnection.getConnection()) {
            PreparedStatement ps = c.prepareStatement("DELETE FROM users WHERE id_user=?");
            for (User u : sel) { ps.setInt(1,u.getId()); ps.addBatch(); }
            ps.executeBatch(); showSuccess(sel.size()+" supprimés ✅"); refresh();
        } catch (Exception e) { showAlert("Erreur",e.getMessage()); }
    }

    // ─────────────────────────────────────────────────────────────────
    //  NAVIGATION
    // ─────────────────────────────────────────────────────────────────
    @FXML private void showDashboard()     { pageTitleLabel.setText("Tableau de bord");  nav(navDashboard); }
    @FXML private void showUsers()         { pageTitleLabel.setText("Utilisateurs");      nav(navUsers); }
    @FXML private void showOffres()        { pageTitleLabel.setText("Offres d'emploi");   nav(navOffres); }
    @FXML private void showFormations()    { pageTitleLabel.setText("Formations");         nav(navFormations); }
    @FXML private void showRapports()      { generateReport(); }
    @FXML private void showNotifications() { showAlert("Notifications","Aucune nouvelle notification."); }
    @FXML private void showParametres()    { showAlert("Paramètres","Disponible prochainement."); }
    @FXML private void showFullLog()       { showAlert("Journal","Journalisation complète à venir."); }

    private void nav(Button active) {
        for (Button b : new Button[]{navDashboard,navUsers,navOffres,navFormations}) {
            if (b==null) continue;
            b.getStyleClass().remove("nav-item-active");
            if (!b.getStyleClass().contains("nav-item")) b.getStyleClass().add("nav-item");
        }
        if (active!=null) { active.getStyleClass().remove("nav-item"); active.getStyleClass().add("nav-item-active"); }
    }

    @FXML private void prevPage()  { if (currentPage>1)         { currentPage--; updatePaginationInfo(); } }
    @FXML private void nextPage()  { if (currentPage<totalPages) { currentPage++; updatePaginationInfo(); } }
    @FXML private void goToPage1() { currentPage=1; updatePaginationInfo(); }
    @FXML private void goToPage2() { if (totalPages>=2) { currentPage=2; updatePaginationInfo(); } }
    @FXML private void goToPage3() { if (totalPages>=3) { currentPage=3; updatePaginationInfo(); } }

    private void updatePaginationInfo() {
        int total = filteredData!=null ? filteredData.size() : usersList.size();
        totalPages = Math.max(1,(int)Math.ceil((double)total/PAGE_SIZE));
        int from = total==0?0:((currentPage-1)*PAGE_SIZE+1);
        int to   = Math.min(currentPage*PAGE_SIZE,total);
        if (paginationInfoLabel!=null)
            paginationInfoLabel.setText("Affichage de "+from+"–"+to+" sur "+total+" utilisateurs");
    }

    @FXML private void refreshData() { refresh(); showSuccess("Actualisé 🔄"); }

    @FXML private void logout() {
        if (confirm("Se déconnecter ?","Quitter le panneau admin ?")) {
            SessionManager.getInstance().clearSession(); redirectToLogin();
        }
    }
    @FXML private void goBack() { redirectToHome(); }

    // ─────────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────────
    private void refresh() { loadUsers(); loadStatistics(); }
    private int  q(Statement s, String sql) throws SQLException { ResultSet r=s.executeQuery(sql); return r.next()?r.getInt(1):0; }
    private boolean colExists(Connection c, String t, String col) {
        try { return c.getMetaData().getColumns(null,null,t,col).next(); } catch (SQLException e) { return false; }
    }
    private String generateTempPassword() { return UUID.randomUUID().toString().replace("-","").substring(0,10); }
    private Stage buildDlg(String title, Parent root) {
        Stage s=new Stage(); s.setTitle(title); s.initModality(Modality.APPLICATION_MODAL);
        s.initOwner(searchField.getScene().getWindow()); s.setScene(new Scene(root)); s.setResizable(false); return s;
    }
    private boolean confirm(String h, String c) {
        Alert a=new Alert(Alert.AlertType.CONFIRMATION); a.setTitle("Confirmation"); a.setHeaderText(h); a.setContentText(c);
        return a.showAndWait().filter(b->b==ButtonType.OK).isPresent();
    }
    private void showSuccess(String m) { Alert a=new Alert(Alert.AlertType.INFORMATION); a.setTitle("Succès"); a.setHeaderText(null); a.setContentText(m); a.showAndWait(); }
    private void showAlert(String t,String m) { Alert a=new Alert(Alert.AlertType.INFORMATION); a.setTitle(t); a.setHeaderText(null); a.setContentText(m); a.showAndWait(); }
    private String e(String s)  { return s!=null?s.replace("<","&lt;").replace(">","&gt;"):""; }
    private String cl(String s) { return s!=null?s.trim().replace("\"",""):""; }
    private boolean isAdmin()   { return SessionManager.getInstance().getRole()!=null && SessionManager.getInstance().getRole().equalsIgnoreCase("Admin"); }
    private void redirectToHome() {
        try { Parent r=FXMLLoader.load(getClass().getResource("/fxml/Home.fxml")); Stage s=(Stage)pageTitleLabel.getScene().getWindow(); s.setScene(new Scene(r)); StageUtils.forceMaximized(s); s.show(); } catch (Exception e) { e.printStackTrace(); }
    }
    private void redirectToLogin() {
        try { Parent r=FXMLLoader.load(getClass().getResource("/fxml/Login.fxml")); Stage s=(Stage)pageTitleLabel.getScene().getWindow(); s.setScene(new Scene(r)); s.setTitle("JobNest - Connexion"); s.show(); } catch (Exception e) { e.printStackTrace(); }
    }

    // ─────────────────────────────────────────────────────────────────
    //  PROGRESS DIALOG
    // ─────────────────────────────────────────────────────────────────
    private static class ProgressDialog {
        private final Stage stage; private final Label lbl; private final ProgressBar bar; private final int total;
        ProgressDialog(int total, javafx.scene.Node owner) {
            this.total=total; stage=new Stage(); stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Envoi en cours..."); stage.setResizable(false);
            if (owner!=null&&owner.getScene()!=null) stage.initOwner(owner.getScene().getWindow());
            lbl=new Label("Envoi 0/"+total+"..."); lbl.setStyle("-fx-font-size:14px;-fx-text-fill:#c9d1d9;");
            bar=new ProgressBar(0); bar.setPrefWidth(300);
            VBox box=new VBox(15,lbl,bar); box.setStyle("-fx-padding:25px;-fx-background-color:#161b22;-fx-alignment:center;");
            stage.setScene(new Scene(box));
        }
        void show()        { stage.show(); }
        void close()       { stage.close(); }
        void update(int c) { lbl.setText("Envoi "+c+"/"+total+"..."); bar.setProgress((double)c/total); }
    }

    // =================================================================
    //  USER MODEL
    // =================================================================
    public static class User {
        private final SimpleBooleanProperty selected=new SimpleBooleanProperty(false);
        private final SimpleIntegerProperty id;
        private final SimpleStringProperty prenom,nom,email,role,telephone,dateInscription,statut;
        private String motDePasse,adresse,lastLogin;

        public User(int id,String prenom,String nom,String email,String role,String telephone,LocalDateTime dt) {
            this.id=new SimpleIntegerProperty(id);
            this.prenom=new SimpleStringProperty(s(prenom)); this.nom=new SimpleStringProperty(s(nom));
            this.email=new SimpleStringProperty(s(email)); this.role=new SimpleStringProperty(s(role));
            this.telephone=new SimpleStringProperty(s(telephone));
            this.dateInscription=new SimpleStringProperty(dt!=null?dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")):"");
            this.statut=new SimpleStringProperty("Actif");
        }
        private static String s(String v){return v!=null?v:"";}

        public SimpleBooleanProperty selectedProperty(){return selected;}
        public boolean isSelected(){return selected.get();}
        public int    getId()              {return id.get();}
        public String getPrenom()          {return prenom.get();}
        public String getNom()             {return nom.get();}
        public String getEmail()           {return email.get();}
        public String getRole()            {return role.get();}
        public String getTelephone()       {return telephone.get();}
        public String getDateInscription() {return dateInscription.get();}
        public String getStatut()          {return statut.get()!=null?statut.get():"Actif";}
        public String getMotDePasse()      {return motDePasse;}
        public String getAdresse()         {return adresse;}
        public String getLastLogin()       {return lastLogin;}
        public void setPrenom(String v)    {prenom.set(v);}
        public void setNom(String v)       {nom.set(v);}
        public void setEmail(String v)     {email.set(v);}
        public void setRole(String v)      {role.set(v);}
        public void setTelephone(String v) {telephone.set(v);}
        public void setStatut(String v)    {statut.set(v!=null?v:"Actif");}
        public void setMotDePasse(String v){motDePasse=v;}
        public void setAdresse(String v)   {adresse=v;}
        public void setLastLogin(String v) {lastLogin=v;}
    }
}