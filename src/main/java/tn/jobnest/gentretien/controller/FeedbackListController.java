package tn.jobnest.gentretien.controller;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import tn.jobnest.gentretien.model.Entretien;
import tn.jobnest.gentretien.model.Feedback;
import tn.jobnest.gentretien.service.Entretienservice;
import tn.jobnest.gentretien.service.FeedbackService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FeedbackListController {

    @FXML private VBox feedbacksVBox;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> comboNoteMin;
    @FXML private Label totalFeedbacksCount;
    @FXML private Label moyenneGlobaleLabel;
    @FXML private Label excellentsCount;

    private final FeedbackService feedbackService = new FeedbackService();
    private final Entretienservice entretienService = new Entretienservice();
    private List<Feedback> allFeedbacks;
    private final int currentRecruteurId = 1;

    @FXML
    public void initialize() {
        comboNoteMin.setItems(FXCollections.observableArrayList(
                "Toutes les notes", "≥ 8/10 (Excellent)", "≥ 6/10 (Bien)", "≥ 4/10 (Moyen)", "< 4/10 (Faible)"
        ));
        comboNoteMin.setValue("Toutes les notes");

        comboNoteMin.valueProperty().addListener((obs, old, newVal) -> filterAndDisplay());
        searchField.textProperty().addListener((obs, old, newVal) -> filterAndDisplay());

        rafraichirListe();
    }

    // ── Navigation sidebar ──────────────────────────────────────────

    @FXML
    private void retourEntretiens(ActionEvent event) {
        naviguerVers("/tn/jobnest/gentretien/entretien-view.fxml", "JobNest - Gestion des Entretiens", event);
    }

    @FXML
    private void ouvrirCandidature(ActionEvent event) {
        naviguerVers("/tn/jobnest/gentretien/GestionCandidatures.fxml", "JobNest - Gestion des Candidatures", event);
    }

    private void naviguerVers(String fxmlPath, String titre, ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            if (getClass().getResource("/tn/jobnest/gentretien/styles.css") != null) {
                scene.getStylesheets().add(getClass().getResource("/tn/jobnest/gentretien/styles.css").toExternalForm());
            }
            stage.setScene(scene);
            stage.setTitle(titre);
            stage.show();
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir la page : " + ex.getMessage());
        }
    }

    // ── Chargement données ──────────────────────────────────────────

    private void rafraichirListe() {
        try {
            List<Feedback> tous = feedbackService.afficher();
            allFeedbacks = tous.stream()
                    .filter(f -> {
                        try {
                            Entretien e = getEntretienById(f.getIdEntretien());
                            return e != null && e.getIdRecruteur() == currentRecruteurId;
                        } catch (SQLException ex) {
                            return false;
                        }
                    })
                    .collect(Collectors.toList());

            updateStats();
            filterAndDisplay();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur BD", "Impossible de charger les feedbacks : " + e.getMessage());
        }
    }

    private Entretien getEntretienById(int idEntretien) throws SQLException {
        List<Entretien> entretiens = entretienService.afficher();
        return entretiens.stream()
                .filter(e -> e.getIdEntretien() == idEntretien)
                .findFirst()
                .orElse(null);
    }

    private void updateStats() {
        if (allFeedbacks == null || allFeedbacks.isEmpty()) {
            totalFeedbacksCount.setText("0");
            moyenneGlobaleLabel.setText("0.0");
            excellentsCount.setText("0");
            return;
        }

        totalFeedbacksCount.setText(String.valueOf(allFeedbacks.size()));

        double moyenneGlobale = allFeedbacks.stream()
                .mapToDouble(f -> (f.getCompetenceTechniques() + f.getCompetenceCommunication()
                        + f.getMotivation() + f.getAdequationAuPoste()) / 4.0)
                .average().orElse(0.0);
        moyenneGlobaleLabel.setText(String.format("%.1f/10", moyenneGlobale));

        long excellents = allFeedbacks.stream()
                .filter(f -> (f.getCompetenceTechniques() + f.getCompetenceCommunication()
                        + f.getMotivation() + f.getAdequationAuPoste()) / 4.0 >= 8.0)
                .count();
        excellentsCount.setText(String.valueOf(excellents));
    }

    private void filterAndDisplay() {
        if (allFeedbacks == null || allFeedbacks.isEmpty()) {
            feedbacksVBox.getChildren().clear();
            return;
        }

        String search = searchField.getText().trim().toLowerCase();
        String noteFilter = comboNoteMin.getValue();

        feedbacksVBox.getChildren().clear();

        for (Feedback f : allFeedbacks) {
            try {
                Entretien entretien = getEntretienById(f.getIdEntretien());
                if (entretien == null) continue;

                String titreOffre = entretienService.getOffreTitre(entretien.getIdOffre()).toLowerCase();
                List<String> participants = entretienService.getParticipants(entretien.getIdEntretien());
                String participantsStr = String.join(" ", participants).toLowerCase();

                double moyenne = (f.getCompetenceTechniques() + f.getCompetenceCommunication()
                        + f.getMotivation() + f.getAdequationAuPoste()) / 4.0;

                boolean matchSearch = search.isEmpty()
                        || String.valueOf(entretien.getIdEntretien()).contains(search)
                        || titreOffre.contains(search)
                        || participantsStr.contains(search);

                boolean matchNote = true;
                switch (noteFilter == null ? "" : noteFilter) {
                    case "≥ 8/10 (Excellent)": matchNote = moyenne >= 8.0; break;
                    case "≥ 6/10 (Bien)":      matchNote = moyenne >= 6.0; break;
                    case "≥ 4/10 (Moyen)":     matchNote = moyenne >= 4.0; break;
                    case "< 4/10 (Faible)":    matchNote = moyenne < 4.0;  break;
                }

                if (matchSearch && matchNote) {
                    feedbacksVBox.getChildren().add(
                            createFeedbackCard(f, entretien, participants, titreOffre, moyenne));
                }
            } catch (SQLException ex) {
                System.err.println("Erreur chargement données feedback #" + f.getIdFeedback());
            }
        }
    }

    // ── Carte feedback ──────────────────────────────────────────────

    private Node createFeedbackCard(Feedback f, Entretien entretien, List<String> participants,
                                    String titreOffre, double moyenne) {
        HBox card = new HBox(15);
        card.getStyleClass().add("card");
        card.setPrefHeight(180);

        // Section score
        VBox iconSection = new VBox(10);
        iconSection.setAlignment(javafx.geometry.Pos.CENTER);
        iconSection.setPrefWidth(100);
        iconSection.setStyle(
                "-fx-background-color: " + getCouleurNote(moyenne) + ";" +
                        "-fx-background-radius: 12px; -fx-padding: 15;"
        );

        Label noteLabel = new Label(String.format("%.1f", moyenne));
        noteLabel.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label surDixLabel = new Label("/10");
        surDixLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: white; -fx-font-weight: 500;");

        iconSection.getChildren().addAll(noteLabel, surDixLabel);

        // Section détails
        VBox details = new VBox(10);
        details.setPrefWidth(500);
        details.setPadding(new Insets(10, 0, 10, 0));

        Label entretienLabel = new Label("📅 Entretien #" + entretien.getIdEntretien() + " — " + titreOffre);
        entretienLabel.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

        String candidatNom = participants.isEmpty() ? "Candidat inconnu" : participants.get(0);
        String dateStr = (entretien.getDateEntretien() != null)
                ? entretien.getDateEntretien().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                : "Date inconnue";
        Label candidatLabel = new Label("👤 " + candidatNom + "   •   📆 " + dateStr);
        candidatLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #7F8C8D; -fx-font-weight: 500;");

        // Mini barres de notes
        HBox notesBox = new HBox(20);
        notesBox.getChildren().addAll(
                createNoteItem("💻", "Technique", f.getCompetenceTechniques()),
                createNoteItem("💬", "Comm.", f.getCompetenceCommunication()),
                createNoteItem("🎯", "Motivation", f.getMotivation()),
                createNoteItem("✅", "Adéquation", f.getAdequationAuPoste())
        );

        // Extrait commentaire
        String extrait = f.getCommentaire();
        if (extrait != null && extrait.length() > 100) extrait = extrait.substring(0, 97) + "...";
        Label commentaireLabel = new Label("💬 " + (extrait != null ? extrait : "—"));
        commentaireLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #34495E; -fx-font-style: italic;");
        commentaireLabel.setWrapText(true);

        // Compétences manquantes
        Label competencesLabel = new Label("⚠️ " + f.getCompetenceManquantes());
        competencesLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #E67E22; -fx-font-weight: 500;");
        competencesLabel.setWrapText(true);

        details.getChildren().addAll(entretienLabel, candidatLabel, notesBox, commentaireLabel, competencesLabel);

        // Section actions
        VBox actionsContainer = new VBox(10);
        actionsContainer.setAlignment(javafx.geometry.Pos.CENTER);
        actionsContainer.setPrefWidth(200);
        actionsContainer.setPadding(new Insets(10, 0, 10, 0));

        if (f.isSuggestionFormation()) {
            Label formationBadge = new Label("🎓 Formation recommandée");
            formationBadge.setStyle(
                    "-fx-background-color: #DBEAFE; -fx-text-fill: #1D4ED8;" +
                            "-fx-padding: 5 10; -fx-background-radius: 8px;" +
                            "-fx-font-size: 11px; -fx-font-weight: bold;"
            );
            actionsContainer.getChildren().add(formationBadge);
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        actionsContainer.getChildren().add(spacer);

        // ── Bouton Consulter — ouvre la fenêtre stylisée ──
        Button btnConsulter = new Button("Consulter");
        btnConsulter.getStyleClass().add("button-primary");
        btnConsulter.setPrefWidth(180);
        btnConsulter.setPrefHeight(40);
        final List<String> participantsFinal = participants;
        final String titreFinal = titreOffre;
        btnConsulter.setOnAction(ev -> consulterFeedback(f, entretien, participantsFinal, titreFinal));

        Button btnModifier = new Button("Modifier");
        btnModifier.getStyleClass().add("button-success");
        btnModifier.setPrefWidth(180);
        btnModifier.setPrefHeight(40);
        btnModifier.setOnAction(ev -> modifierFeedback(f, entretien));

        Button btnSupprimer = new Button("Supprimer");
        btnSupprimer.getStyleClass().add("button-danger");
        btnSupprimer.setPrefWidth(180);
        btnSupprimer.setPrefHeight(40);
        btnSupprimer.setOnAction(ev -> supprimerFeedback(f));

        actionsContainer.getChildren().addAll(btnConsulter, btnModifier, btnSupprimer);

        HBox.setHgrow(details, javafx.scene.layout.Priority.ALWAYS);
        card.getChildren().addAll(iconSection, details, actionsContainer);
        return card;
    }

    private VBox createNoteItem(String emoji, String label, int note) {
        VBox item = new VBox(2);
        item.setAlignment(javafx.geometry.Pos.CENTER);
        Label emojiLabel = new Label(emoji);
        emojiLabel.setStyle("-fx-font-size: 15px;");
        Label noteLabel = new Label(note + "/10");
        noteLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");
        Label labelText = new Label(label);
        labelText.setStyle("-fx-font-size: 10px; -fx-text-fill: #7F8C8D;");
        item.getChildren().addAll(emojiLabel, noteLabel, labelText);
        return item;
    }

    private String getCouleurNote(double moyenne) {
        if (moyenne >= 8.0) return "#27AE60";
        if (moyenne >= 6.0) return "#3498DB";
        if (moyenne >= 4.0) return "#F39C12";
        return "#E74C3C";
    }

    // ── Actions ────────────────────────────────────────────────────

    /**
     * Ouvre la fenêtre de détail stylisée — remplace l'ancienne Alert.
     */
    private void consulterFeedback(Feedback f, Entretien entretien,
                                   List<String> participants, String titreOffre) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/tn/jobnest/gentretien/feedback-detail.fxml"));
            Stage stage = new Stage();
            Scene scene = new Scene(loader.load());
            if (getClass().getResource("/tn/jobnest/gentretien/styles.css") != null) {
                scene.getStylesheets().add(
                        getClass().getResource("/tn/jobnest/gentretien/styles.css").toExternalForm());
            }
            stage.setScene(scene);
            stage.setTitle("Détail du Feedback — Entretien #" + entretien.getIdEntretien());
            stage.setResizable(false);

            FeedbackDetailController ctrl = loader.getController();
            ctrl.setData(f, entretien, participants, titreOffre);

            stage.show();
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'ouvrir le détail du feedback : " + ex.getMessage());
        }
    }

    private void modifierFeedback(Feedback f, Entretien entretien) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/tn/jobnest/gentretien/feedback-form.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));

            FeedbackFormController ctrl = loader.getController();
            ctrl.setEntretien(entretien);
            ctrl.setFeedbackExistant(f);

            stage.setTitle("✏️ Modifier le Feedback - Entretien #" + entretien.getIdEntretien());
            stage.showAndWait();
            rafraichirListe();
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'ouvrir le formulaire : " + ex.getMessage());
        }
    }

    private void supprimerFeedback(Feedback f) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer ce feedback ?");
        confirm.setContentText("Cette action est irréversible.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                feedbackService.delete(f.getIdFeedback());
                rafraichirListe();
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Feedback supprimé avec succès.");
            } catch (SQLException ex) {
                showAlert(Alert.AlertType.ERROR, "Erreur",
                        "Impossible de supprimer le feedback : " + ex.getMessage());
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