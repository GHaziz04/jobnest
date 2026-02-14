package tn.jobnest.gentretien.controller;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import tn.jobnest.gentretien.model.Entretien;
import tn.jobnest.gentretien.service.Entretienservice;

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

    @FXML
    private VBox entretiensVBox;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> comboType;
    @FXML
    private ComboBox<String> comboStatut;
    @FXML
    private Label planifiesCount;
    @FXML
    private Label terminesCount;
    @FXML
    private Label semaineCount;

    private final Entretienservice service = new Entretienservice();
    private List<Entretien> allEntretiens;
    private final int currentRecruteurId = 1; // ID du recruteur connecté

    @FXML
    public void initialize() {
        comboType.setItems(FXCollections.observableArrayList("Tous les types", "présentiel", "visio"));
        comboType.setValue("Tous les types");
        comboStatut.setItems(FXCollections.observableArrayList("Tous les statuts", "proposé", "confirmé", "réalisé", "annulé"));
        comboStatut.setValue("Tous les statuts");

        comboType.valueProperty().addListener((obs, old, newVal) -> filterAndDisplay());
        comboStatut.valueProperty().addListener((obs, old, newVal) -> filterAndDisplay());
        searchField.textProperty().addListener((obs, old, newVal) -> filterAndDisplay());

        rafraichirListe();
    }

    @FXML
    private void ajouterEntretien(ActionEvent event) {
        openForm(false, null);
    }

    private void rafraichirListe() {
        try {
            List<Entretien> tous = service.afficher();
            allEntretiens = tous.stream()
                    .filter(e -> e.getIdRecruteur() == currentRecruteurId)
                    .collect(Collectors.toList());
            updateStats();
            filterAndDisplay();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur BD", "Impossible de charger les entretiens : " + e.getMessage());
        }
    }

    private void updateStats() {
        if (allEntretiens == null) return;

        long planifies = allEntretiens.stream().filter(e -> "proposé".equals(e.getStatut()) || "confirmé".equals(e.getStatut())).count();
        long termines = allEntretiens.stream().filter(e -> "réalisé".equals(e.getStatut())).count();

        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDate endOfWeek = startOfWeek.plusDays(6);

        long cetteSemaine = allEntretiens.stream()
                .filter(e -> e.getDateEntretien() != null)
                .filter(e -> !LocalDate.from(e.getDateEntretien().toLocalDate()).isBefore(startOfWeek)
                        && !LocalDate.from(e.getDateEntretien().toLocalDate()).isAfter(endOfWeek))
                .count();

        planifiesCount.setText(String.valueOf(planifies));
        terminesCount.setText(String.valueOf(termines));
        semaineCount.setText(String.valueOf(cetteSemaine));
    }

    private void filterAndDisplay() {
        if (allEntretiens == null || allEntretiens.isEmpty()) {
            entretiensVBox.getChildren().clear();
            return;
        }

        String search = searchField.getText().trim().toLowerCase();
        String selType = comboType.getValue();
        String selStatut = comboStatut.getValue();

        entretiensVBox.getChildren().clear();

        for (Entretien e : allEntretiens) {
            try {
                String titreOffre = service.getOffreTitre(e.getIdOffre()).toLowerCase();
                List<String> participants = service.getParticipants(e.getIdEntretien());
                String participantsStr = String.join(" ", participants).toLowerCase();

                boolean matchSearch = search.isEmpty()
                        || titreOffre.contains(search)
                        || participantsStr.contains(search);
                boolean matchType = "Tous les types".equals(selType) || selType.equals(e.getTypeEntretien());
                boolean matchStatut = "Tous les statuts".equals(selStatut) || selStatut.equals(e.getStatut());

                if (matchSearch && matchType && matchStatut) {
                    entretiensVBox.getChildren().add(createEntretienCard(e, participants, titreOffre));
                }
            } catch (SQLException ex) {
                System.err.println("Erreur chargement données entretien #" + e.getIdEntretien());
            }
        }
    }

    private Node createEntretienCard(Entretien e, List<String> participants, String titreOffre) {
        HBox card = new HBox(15);
        card.getStyleClass().add("card");
        card.setPrefHeight(130);

        // Avatar avec initiales du premier participant
        String participantName = participants.isEmpty() ? "Candidat" : participants.get(0);
        String initials = participantName.chars()
                .filter(Character::isUpperCase)
                .mapToObj(c -> String.valueOf((char) c))
                .collect(Collectors.joining());
        if (initials.isEmpty() && !participants.isEmpty()) {
            String[] parts = participantName.split(" ");
            initials = parts[0].substring(0, 1);
            if (parts.length > 1) initials += parts[1].substring(0, 1);
        } else if (initials.isEmpty()) {
            initials = "C";
        }

        Circle avatar = new Circle(25);
        avatar.getStyleClass().add("avatar");

        Label avatarLabel = new Label(initials);
        avatarLabel.getStyleClass().add("avatar-text");
        avatarLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

        StackPane avatarStack = new StackPane();
        avatarStack.getChildren().addAll(avatar, avatarLabel);

        VBox avatarBox = new VBox(avatarStack);
        avatarBox.setAlignment(javafx.geometry.Pos.CENTER);
        avatarBox.setPrefWidth(70);

        // Détails de l'entretien
        VBox details = new VBox(8);
        details.setPrefWidth(400);
        details.setPadding(new Insets(5, 0, 5, 0));

        // Nom de l'offre d'emploi
        Label offreLabel = new Label(titreOffre);
        offreLabel.getStyleClass().add("card-title");
        offreLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // Participants
        Label participantsLabel;
        if (participants.size() > 1) {
            String participantsText = "👥 " + String.join(", ", participants);
            participantsLabel = new Label(participantsText);
        } else {
            String participantsText = "👤 " + (participants.isEmpty() ? "Aucun candidat" : participants.get(0));
            participantsLabel = new Label(participantsText);
        }
        participantsLabel.getStyleClass().add("card-participants");
        participantsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #2c3e50; -fx-font-weight: 500;");

        // Date, heure et durée
        String dateStr = (e.getDateEntretien() != null)
                ? e.getDateEntretien().toLocalDate().format(DateTimeFormatter.ofPattern("EEE dd MMM yyyy"))
                : "Date non définie";
        String heureStr = (e.getHeureDebut() != null)
                ? e.getHeureDebut().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
                : "--:--";
        long duree = calculateDuration(e);
        String dureeTxt = (duree > 0) ? "Durée: " + duree + " min" : "Durée non définie";

        Label dateTimeLabel = new Label("📅 " + dateStr + " à " + heureStr + " (" + dureeTxt + ")");
        dateTimeLabel.getStyleClass().add("card-datetime");
        dateTimeLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #7f8c8d;");

        // Lieu ou lien Visio
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
        lieuVisioLabel.getStyleClass().add("card-location");
        lieuVisioLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #3498db; -fx-font-weight: 500;");

        // Note du recruteur
        Label noteLabel = null;
        if (e.getNoteRecruteur() != null && !e.getNoteRecruteur().isEmpty()) {
            noteLabel = new Label("📝 Note: " + e.getNoteRecruteur());
            noteLabel.getStyleClass().add("card-note");
            noteLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #e67e22; -fx-font-style: italic; -fx-font-weight: 500;");
        }

        details.getChildren().addAll(offreLabel, participantsLabel, dateTimeLabel, lieuVisioLabel);
        if (noteLabel != null) {
            details.getChildren().add(noteLabel);
        }

        // Actions - Boutons agrandis
        VBox actionsContainer = new VBox(8);
        actionsContainer.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        actionsContainer.setPrefWidth(420);
        actionsContainer.setPadding(new Insets(5, 0, 5, 0));

        // Première rangée de boutons
        HBox actionsRow1 = new HBox(10);
        actionsRow1.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        Button btnModifier = new Button("Modifier");
        btnModifier.getStyleClass().add("button-primary");
        btnModifier.setPrefWidth(110);
        btnModifier.setPrefHeight(42);
        btnModifier.setOnAction(ev -> openForm(true, e));

        Button btnTermine = new Button("Terminé");
        btnTermine.getStyleClass().add("button-success");
        btnTermine.setPrefWidth(110);
        btnTermine.setPrefHeight(42);
        btnTermine.setOnAction(ev -> marquerTermine(e));

        actionsRow1.getChildren().addAll(btnModifier, btnTermine);

        // Deuxième rangée de boutons
        HBox actionsRow2 = new HBox(10);
        actionsRow2.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        Button btnActionSpecifique;
        if ("présentiel".equals(e.getTypeEntretien())) {
            btnActionSpecifique = new Button("Consulter map");
            btnActionSpecifique.getStyleClass().add("button-map");
            btnActionSpecifique.setPrefWidth(140);
            btnActionSpecifique.setPrefHeight(42);
            btnActionSpecifique.setOnAction(ev -> consulterMap(e));
        } else {
            // ===== VALIDATION : Rejoindre Meet seulement le jour de l'entretien =====
            btnActionSpecifique = new Button("Rejoindre");
            btnActionSpecifique.getStyleClass().add("button-visio");
            btnActionSpecifique.setPrefWidth(120);
            btnActionSpecifique.setPrefHeight(42);
            btnActionSpecifique.setOnAction(ev -> rejoindre(e));

            // Désactiver si pas de lien
            if (e.getLienVisio() == null || e.getLienVisio().isBlank()) {
                btnActionSpecifique.setDisable(true);
                btnActionSpecifique.setText("❌ Lien indisponible");
                btnActionSpecifique.setPrefWidth(150);
            }
            // ===== NOUVEAU : Désactiver si ce n'est pas le jour de l'entretien =====
            else if (e.getDateEntretien() != null) {
                LocalDate dateEntretien = e.getDateEntretien().toLocalDate();
                LocalDate aujourdhui = LocalDate.now();

                if (!dateEntretien.equals(aujourdhui)) {
                    btnActionSpecifique.setDisable(true);
                    if (dateEntretien.isAfter(aujourdhui)) {
                        btnActionSpecifique.setText("Pas encore");
                        btnActionSpecifique.setPrefWidth(130);
                    } else {
                        btnActionSpecifique.setText("Expiré");
                        btnActionSpecifique.setPrefWidth(120);
                    }
                }
            }
        }

        Button btnSupprimer = new Button("Supprimer");
        btnSupprimer.getStyleClass().add("button-danger");
        btnSupprimer.setPrefWidth(110);
        btnSupprimer.setPrefHeight(42);
        btnSupprimer.setOnAction(ev -> supprimerEntretien(e));

        actionsRow2.getChildren().addAll(btnActionSpecifique, btnSupprimer);

        actionsContainer.getChildren().addAll(actionsRow1, actionsRow2);

        HBox.setHgrow(details, javafx.scene.layout.Priority.ALWAYS);
        card.getChildren().addAll(avatarBox, details, actionsContainer);

        return card;
    }

    private long calculateDuration(Entretien e) {
        if (e.getHeureDebut() == null || e.getHeureFin() == null) return 0;
        return TimeUnit.MILLISECONDS.toMinutes(e.getHeureFin().getTime() - e.getHeureDebut().getTime());
    }

    private void openForm(boolean edition, Entretien ent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/jobnest/gentretien/entretien-form.fxml"));
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
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le formulaire : " + ex.getMessage());
        }
    }

    private void marquerTermine(Entretien e) {
        e.setStatut("réalisé");
        try {
            service.update(e);
            rafraichirListe();
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Entretien marqué comme réalisé.");
        } catch (SQLException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de mettre à jour : " + ex.getMessage());
        }
    }

    private void rejoindre(Entretien e) {
        // ===== VALIDATION : Vérifier que c'est bien aujourd'hui =====
        if (e.getDateEntretien() != null) {
            LocalDate dateEntretien = e.getDateEntretien().toLocalDate();
            LocalDate aujourdhui = LocalDate.now();

            if (!dateEntretien.equals(aujourdhui)) {
                if (dateEntretien.isAfter(aujourdhui)) {
                    showAlert(Alert.AlertType.WARNING, "Accès impossible",
                            "Cet entretien est prévu pour le " +
                                    dateEntretien.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ".\n\n" +
                                    "Vous ne pouvez rejoindre la visioconférence que le jour de l'entretien.");
                } else {
                    showAlert(Alert.AlertType.WARNING, "Accès impossible",
                            "Cet entretien était prévu pour le " +
                                    dateEntretien.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ".\n\n" +
                                    "La date est passée, vous ne pouvez plus rejoindre cette visioconférence.");
                }
                return;
            }
        }

        // Si la validation passe, ouvrir le lien
        if ("visio".equals(e.getTypeEntretien()) && e.getLienVisio() != null && !e.getLienVisio().isBlank()) {
            try {
                Desktop.getDesktop().browse(new URI(e.getLienVisio()));
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le lien : " + ex.getMessage());
            }
        }
    }

    private void consulterMap(Entretien e) {
        if ("présentiel".equals(e.getTypeEntretien()) && e.getLieu() != null && !e.getLieu().isBlank()) {
            try {
                Alert choix = new Alert(Alert.AlertType.CONFIRMATION);
                choix.setTitle("Consulter la carte");
                choix.setHeaderText("Ouvrir Google Maps ?");
                choix.setContentText("📍 Lieu : " + e.getLieu());

                ButtonType btnCarte = new ButtonType("🗺️ Voir sur la carte");
                ButtonType btnItineraire = new ButtonType("🧭 Calculer itinéraire");
                ButtonType btnAnnuler = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);

                choix.getButtonTypes().setAll(btnCarte, btnItineraire, btnAnnuler);

                Optional<ButtonType> result = choix.showAndWait();

                if (result.isPresent()) {
                    if (result.get() == btnCarte) {
                        // Ouvrir la localisation sur Google Maps
                        String searchUrl = "https://www.google.com/maps/search/?api=1&query="
                                + java.net.URLEncoder.encode(e.getLieu(), "UTF-8");
                        Desktop.getDesktop().browse(new URI(searchUrl));

                    } else if (result.get() == btnItineraire) {
                        // Demander l'adresse de départ
                        TextInputDialog dialog = new TextInputDialog("Ma position");
                        dialog.setTitle("Point de départ");
                        dialog.setHeaderText("Calculer l'itinéraire");
                        dialog.setContentText("Votre adresse de départ :");

                        Optional<String> depart = dialog.showAndWait();
                        if (depart.isPresent() && !depart.get().trim().isEmpty()) {
                            String itineraireUrl = String.format(
                                    "https://www.google.com/maps/dir/?api=1&origin=%s&destination=%s&travelmode=driving",
                                    java.net.URLEncoder.encode(depart.get(), "UTF-8"),
                                    java.net.URLEncoder.encode(e.getLieu(), "UTF-8")
                            );
                            Desktop.getDesktop().browse(new URI(itineraireUrl));
                        }
                    }
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur",
                        "Impossible d'ouvrir Google Maps : " + ex.getMessage());
            }
        }
    }

    private void supprimerEntretien(Entretien e) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer cet entretien ?");
        confirm.setContentText("Cette action est irréversible.");
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            try {
                service.delete(e.getIdEntretien());
                rafraichirListe();
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Entretien supprimé avec succès.");
            } catch (SQLException ex) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de la suppression : " + ex.getMessage());
            }
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