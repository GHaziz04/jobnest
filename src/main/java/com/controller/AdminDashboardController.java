package com.controller;

import com.utils.DBConnection;
import com.utils.SessionManager;
import com.utils.StageUtils;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AdminDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label totalUsersLabel;
    @FXML private Label candidatsLabel;
    @FXML private Label recruteursLabel;
    @FXML private Label formateursLabel;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilterCombo;

    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, Integer> idColumn;
    @FXML private TableColumn<User, String> prenomColumn;
    @FXML private TableColumn<User, String> nomColumn;
    @FXML private TableColumn<User, String> emailColumn;
    @FXML private TableColumn<User, String> roleColumn;
    @FXML private TableColumn<User, String> telephoneColumn;
    @FXML private TableColumn<User, String> dateCreationColumn;
    @FXML private TableColumn<User, Void> actionsColumn;

    private ObservableList<User> usersList = FXCollections.observableArrayList();
    private FilteredList<User> filteredData;

    @FXML
    public void initialize() {
        // Vérifier si admin
        if (!SessionManager.isLoggedIn()) {
            redirectToLogin();
            return;
        }

        if (!isAdmin()) {
            redirectToHome();
            return;
        }

        welcomeLabel.setText("Dashboard Admin - " + SessionManager.getInstance().getPrenom() + " 👋");

        // Initialiser les filtres
        roleFilterCombo.setItems(FXCollections.observableArrayList(
                "Tous", "Candidat", "Recruteur", "Formateur", "Admin"
        ));
        roleFilterCombo.setValue("Tous");

        // Configurer la table
        setupTable();

        // Charger les données
        loadStatistics();
        loadUsers();

        // Configurer la recherche et les filtres
        setupSearchAndFilter();
    }

    private void setupTable() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        prenomColumn.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        nomColumn.setCellValueFactory(new PropertyValueFactory<>("nom"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        telephoneColumn.setCellValueFactory(new PropertyValueFactory<>("telephone"));
        dateCreationColumn.setCellValueFactory(new PropertyValueFactory<>("dateInscription"));

        actionsColumn.setCellFactory(new Callback<TableColumn<User, Void>, TableCell<User, Void>>() {
            @Override
            public TableCell<User, Void> call(TableColumn<User, Void> param) {
                return new TableCell<User, Void>() {
                    private final Button editBtn = new Button("✏️");
                    private final Button deleteBtn = new Button("🗑️");
                    private final HBox hBox = new HBox(8, editBtn, deleteBtn);

                    {
                        editBtn.getStyleClass().add("edit-btn");
                        deleteBtn.getStyleClass().add("delete-btn");
                        hBox.setStyle("-fx-alignment: center;");

                        editBtn.setOnAction(event -> {
                            User user = getTableView().getItems().get(getIndex());
                            editUser(user);
                        });

                        deleteBtn.setOnAction(event -> {
                            User user = getTableView().getItems().get(getIndex());
                            deleteUser(user);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        setGraphic(empty ? null : hBox);
                    }
                };
            }
        });

        usersTable.setItems(usersList);
    }

    private void loadStatistics() {
        try (Connection conn = DBConnection.getConnection()) {
            Statement stmt = conn.createStatement();

            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users");
            if (rs.next()) totalUsersLabel.setText(String.valueOf(rs.getInt(1)));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM users WHERE role = 'Candidat'");
            if (rs.next()) candidatsLabel.setText(String.valueOf(rs.getInt(1)));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM users WHERE role = 'Recruteur'");
            if (rs.next()) recruteursLabel.setText(String.valueOf(rs.getInt(1)));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM users WHERE role = 'Formateur'");
            if (rs.next()) formateursLabel.setText(String.valueOf(rs.getInt(1)));

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les statistiques: " + e.getMessage());
        }
    }

    private void loadUsers() {
        usersList.clear();
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT * FROM users ORDER BY date_inscription DESC";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                User user = new User(
                        rs.getInt("id_user"),
                        rs.getString("prenom"),
                        rs.getString("nom"),
                        rs.getString("email"),
                        rs.getString("role"),
                        rs.getString("telephone"),
                        rs.getTimestamp("date_inscription") != null
                                ? rs.getTimestamp("date_inscription").toLocalDateTime()
                                : null
                );
                user.setAdresse(rs.getString("adresse"));
                usersList.add(user);
            }

            System.out.println("Chargé " + usersList.size() + " utilisateurs");

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les utilisateurs: " + e.getMessage());
        }
    }

    private void setupSearchAndFilter() {
        filteredData = new FilteredList<>(usersList, p -> true);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        roleFilterCombo.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());

        SortedList<User> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(usersTable.comparatorProperty());
        usersTable.setItems(sortedData);
    }

    private void applyFilters() {
        filteredData.setPredicate(user -> {
            String searchText = searchField.getText().toLowerCase();
            String roleFilter = roleFilterCombo.getValue();

            boolean matchesSearch = searchText.isEmpty() ||
                    user.getPrenom().toLowerCase().contains(searchText) ||
                    user.getNom().toLowerCase().contains(searchText) ||
                    user.getEmail().toLowerCase().contains(searchText) ||
                    user.getTelephone().contains(searchText);

            boolean matchesRole = roleFilter.equals("Tous") ||
                    user.getRole().equalsIgnoreCase(roleFilter);

            return matchesSearch && matchesRole;
        });
    }

    @FXML
    private void addUser() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UserFormDialog.fxml"));
            Parent root = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Ajouter un utilisateur");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(welcomeLabel.getScene().getWindow());
            dialogStage.setScene(new Scene(root));
            dialogStage.setResizable(false);

            UserFormDialogController controller = loader.getController();
            controller.setDialogStage(dialogStage);

            dialogStage.showAndWait();

            if (controller.isValid()) {
                saveNewUser(controller.getUser());
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir le formulaire: " + e.getMessage());
        }
    }

    private void saveNewUser(User user) {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "INSERT INTO users (prenom, nom, email, mot_de_passe, role, telephone, adresse, date_inscription) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, user.getPrenom());
            ps.setString(2, user.getNom());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getMotDePasse());
            ps.setString(5, user.getRole());
            ps.setString(6, user.getTelephone());
            ps.setString(7, user.getAdresse());

            if (ps.executeUpdate() > 0) {
                showAlert("Succès", "Utilisateur ajouté avec succès ✅");
                loadUsers();
                loadStatistics();
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors de l'ajout: " + e.getMessage());
        }
    }

    private void editUser(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UserFormDialog.fxml"));
            Parent root = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Modifier l'utilisateur");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(welcomeLabel.getScene().getWindow());
            dialogStage.setScene(new Scene(root));
            dialogStage.setResizable(false);

            UserFormDialogController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setUser(user);

            dialogStage.showAndWait();

            if (controller.isValid()) {
                updateUser(controller.getUser());
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir le formulaire: " + e.getMessage());
        }
    }

    private void updateUser(User user) {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "UPDATE users SET prenom = ?, nom = ?, email = ?, role = ?, " +
                    "telephone = ?, adresse = ? WHERE id_user = ?";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, user.getPrenom());
            ps.setString(2, user.getNom());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getRole());
            ps.setString(5, user.getTelephone());
            ps.setString(6, user.getAdresse());
            ps.setInt(7, user.getId());

            if (ps.executeUpdate() > 0) {
                showAlert("Succès", "Utilisateur modifié avec succès ✅");
                loadUsers();
                loadStatistics();
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors de la modification: " + e.getMessage());
        }
    }

    private void deleteUser(User user) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer l'utilisateur ?");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer " + user.getPrenom() + " " + user.getNom() + " ?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try (Connection conn = DBConnection.getConnection()) {
                    String sql = "DELETE FROM users WHERE id_user = ?";
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setInt(1, user.getId());

                    if (ps.executeUpdate() > 0) {
                        showAlert("Succès", "Utilisateur supprimé ✅");
                        loadUsers();
                        loadStatistics();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert("Erreur", "Erreur lors de la suppression: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void refreshData() {
        loadUsers();
        loadStatistics();
        searchField.clear();
        roleFilterCombo.setValue("Tous");
        showAlert("Info", "Données actualisées 🔄");
    }

    @FXML
    private void goBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/Home.fxml"));
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Dashboard - JobNest");
            StageUtils.forceMaximized(stage);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void redirectToHome() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/Home.fxml"));
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Dashboard - JobNest");
            StageUtils.forceMaximized(stage);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void redirectToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/Login.fxml"));
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("JobNest - Connexion");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isAdmin() {
        return SessionManager.getInstance().getRole() != null &&
                SessionManager.getInstance().getRole().equalsIgnoreCase("Admin");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ===== USER CLASS =====
    public static class User {
        private final SimpleIntegerProperty id;
        private final SimpleStringProperty prenom;
        private final SimpleStringProperty nom;
        private final SimpleStringProperty email;
        private final SimpleStringProperty role;
        private final SimpleStringProperty telephone;
        private final SimpleStringProperty dateInscription;
        private String motDePasse;
        private String adresse;

        public User(int id, String prenom, String nom, String email, String role, String telephone, LocalDateTime dateInscription) {
            this.id = new SimpleIntegerProperty(id);
            this.prenom = new SimpleStringProperty(prenom != null ? prenom : "");
            this.nom = new SimpleStringProperty(nom != null ? nom : "");
            this.email = new SimpleStringProperty(email != null ? email : "");
            this.role = new SimpleStringProperty(role != null ? role : "");
            this.telephone = new SimpleStringProperty(telephone != null ? telephone : "");
            this.dateInscription = new SimpleStringProperty(
                    dateInscription != null ? dateInscription.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : ""
            );
        }

        // Getters
        public int getId() { return id.get(); }
        public String getPrenom() { return prenom.get(); }
        public String getNom() { return nom.get(); }
        public String getEmail() { return email.get(); }
        public String getRole() { return role.get(); }
        public String getTelephone() { return telephone.get(); }
        public String getDateInscription() { return dateInscription.get(); }
        public String getMotDePasse() { return motDePasse; }
        public String getAdresse() { return adresse; }

        // Setters
        public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }
        public void setAdresse(String adresse) { this.adresse = adresse; }
        public void setPrenom(String prenom) { this.prenom.set(prenom); }
        public void setNom(String nom) { this.nom.set(nom); }
        public void setEmail(String email) { this.email.set(email); }
        public void setRole(String role) { this.role.set(role); }
        public void setTelephone(String telephone) { this.telephone.set(telephone); }
    }
}