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

    @FXML
    private VBox feedbacksVBox;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> comboNoteMin;
    @FXML
    private Label totalFeedbacksCount;
    @FXML
    private Label moyenneGlobaleLabel;
    @FXML
    private Label excellentsCount;

    private final FeedbackService feedbackService = new FeedbackService();
    private final Entretienservice entretienService = new Entretienservice();
    private List<Feedback> allFeedbacks;
    private final int currentRecruteurId = 1; // ID du recruteur connecté

    @FXML
    public void initialize() {
        // Initialiser le filtre par note
        comboNoteMin.setItems(FXCollections.observableArrayList(
                "Toutes les notes", "≥ 8/10 (Excellent)", "≥ 6/10 (Bien)", "≥ 4/10 (Moyen)", "< 4/10 (Faible)"
        ));
        comboNoteMin.setValue("Toutes les notes");

        // Listeners pour les filtres
        comboNoteMin.valueProperty().addListener((obs, old, newVal) -> filterAndDisplay());
        searchField.textProperty().addListener((obs, old, newVal) -> filterAndDisplay());

        rafraichirListe();
    }

    @FXML
    private void retourEntretiens(javafx.event.ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/jobnest/gentretien/entretien-view.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/tn/jobnest/gentretien/styles.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("JobNest - Gestion des Entretiens");
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir l'interface des entretiens : " + ex.getMessage());
        }
    }

    private void rafraichirListe() {
        try {
            // Récupérer tous les feedbacks
            List<Feedback> tous = feedbackService.afficher();

            // Filtrer par recruteur (via les entretiens)
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
            showAlert(Alert.AlertType.ERROR, "Erreur BD",
                    "Impossible de charger les feedbacks : " + e.getMessage());
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

        // Total de feedbacks
        totalFeedbacksCount.setText(String.valueOf(allFeedbacks.size()));

        // Calculer la moyenne globale de tous les critères
        double moyenneGlobale = allFeedbacks.stream()
                .mapToDouble(f -> (f.getCompetenceTechniques() +
                        f.getCompetenceCommunication() +
                        f.getMotivation() +
                        f.getAdequationAuPoste()) / 4.0)
                .average()
                .orElse(0.0);

        moyenneGlobaleLabel.setText(String.format("%.1f/10", moyenneGlobale));

        // Compter les excellents (moyenne >= 8)
        long excellents = allFeedbacks.stream()
                .filter(f -> {
                    double moy = (f.getCompetenceTechniques() +
                            f.getCompetenceCommunication() +
                            f.getMotivation() +
                            f.getAdequationAuPoste()) / 4.0;
                    return moy >= 8.0;
                })
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

                // Calculer la moyenne
                double moyenne = (f.getCompetenceTechniques() +
                        f.getCompetenceCommunication() +
                        f.getMotivation() +
                        f.getAdequationAuPoste()) / 4.0;

                // Filtre de recherche par entretien (ID, titre offre, candidat)
                String idEntretienStr = String.valueOf(entretien.getIdEntretien());
                boolean matchSearch = search.isEmpty()
                        || idEntretienStr.contains(search)
                        || titreOffre.contains(search)
                        || participantsStr.contains(search);

                // Filtre par note
                boolean matchNote = true;
                if ("≥ 8/10 (Excellent)".equals(noteFilter)) {
                    matchNote = moyenne >= 8.0;
                } else if ("≥ 6/10 (Bien)".equals(noteFilter)) {
                    matchNote = moyenne >= 6.0;
                } else if ("≥ 4/10 (Moyen)".equals(noteFilter)) {
                    matchNote = moyenne >= 4.0;
                } else if ("< 4/10 (Faible)".equals(noteFilter)) {
                    matchNote = moyenne < 4.0;
                }

                if (matchSearch && matchNote) {
                    feedbacksVBox.getChildren().add(createFeedbackCard(f, entretien, participants, titreOffre, moyenne));
                }

            } catch (SQLException ex) {
                System.err.println("Erreur chargement données feedback #" + f.getIdFeedback());
            }
        }
    }

    private Node createFeedbackCard(Feedback f, Entretien entretien, List<String> participants,
                                    String titreOffre, double moyenne) {
        HBox card = new HBox(15);
        card.getStyleClass().add("card");
        card.setPrefHeight(180);

        // Section gauche - Icône et note globale
        VBox iconSection = new VBox(10);
        iconSection.setAlignment(javafx.geometry.Pos.CENTER);
        iconSection.setPrefWidth(100);
        iconSection.setStyle("-fx-background-color: " + getCouleurNote(moyenne) + "; " +
                "-fx-background-radius: 12px; -fx-padding: 15;");

        Label noteLabel = new Label(String.format("%.1f", moyenne));
        noteLabel.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label surDixLabel = new Label("/10");
        surDixLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: white; -fx-font-weight: 500;");

        iconSection.getChildren().addAll(noteLabel, surDixLabel);

        // Section centrale - Détails
        VBox details = new VBox(10);
        details.setPrefWidth(500);
        details.setPadding(new Insets(10, 0, 10, 0));

        // Titre avec ID entretien
        Label entretienLabel = new Label("📅 Entretien #" + entretien.getIdEntretien() + " - " + titreOffre);
        entretienLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

        // Candidat et date
        String candidatNom = participants.isEmpty() ? "Candidat inconnu" : participants.get(0);
        String dateStr = (entretien.getDateEntretien() != null)
                ? entretien.getDateEntretien().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                : "Date inconnue";

        Label candidatLabel = new Label("👤 " + candidatNom + " • 📆 " + dateStr);
        candidatLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7F8C8D; -fx-font-weight: 500;");

        // Notes détaillées
        HBox notesBox = new HBox(20);
        notesBox.getChildren().addAll(
                createNoteItem("💻", "Technique", f.getCompetenceTechniques()),
                createNoteItem("💬", "Communication", f.getCompetenceCommunication()),
                createNoteItem("🎯", "Motivation", f.getMotivation()),
                createNoteItem("✅", "Adéquation", f.getAdequationAuPoste())
        );

        // Commentaire (extrait)
        String commentaireExtrait = f.getCommentaire();
        if (commentaireExtrait.length() > 100) {
            commentaireExtrait = commentaireExtrait.substring(0, 97) + "...";
        }
        Label commentaireLabel = new Label("💬 " + commentaireExtrait);
        commentaireLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #34495E; -fx-font-style: italic;");
        commentaireLabel.setWrapText(true);

        // Compétences manquantes
        Label competencesLabel = new Label("⚠️ Compétences manquantes : " + f.getCompetenceManquantes());
        competencesLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #E67E22; -fx-font-weight: 500;");
        competencesLabel.setWrapText(true);

        details.getChildren().addAll(entretienLabel, candidatLabel, notesBox, commentaireLabel, competencesLabel);

        // Section droite - Actions
        VBox actionsContainer = new VBox(12);
        actionsContainer.setAlignment(javafx.geometry.Pos.CENTER);
        actionsContainer.setPrefWidth(200);
        actionsContainer.setPadding(new Insets(10, 0, 10, 0));

        // Badge formation
        if (f.isSuggestionFormation()) {
            Label formationBadge = new Label("🎓 Formation recommandée");
            formationBadge.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; " +
                    "-fx-padding: 5 10; -fx-background-radius: 8px; " +
                    "-fx-font-size: 11px; -fx-font-weight: bold;");
            actionsContainer.getChildren().add(formationBadge);
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        actionsContainer.getChildren().add(spacer);

        // Bouton Consulter (Détails complets)
        Button btnConsulter = new Button("👁️ Consulter");
        btnConsulter.getStyleClass().add("button-primary");
        btnConsulter.setPrefWidth(180);
        btnConsulter.setPrefHeight(40);
        btnConsulter.setOnAction(ev -> consulterFeedback(f, entretien));

        // Bouton Modifier
        Button btnModifier = new Button("✏️ Modifier");
        btnModifier.getStyleClass().add("button-success");
        btnModifier.setPrefWidth(180);
        btnModifier.setPrefHeight(40);
        btnModifier.setOnAction(ev -> modifierFeedback(f, entretien));

        // Bouton Supprimer
        Button btnSupprimer = new Button("🗑️ Supprimer");
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
        emojiLabel.setStyle("-fx-font-size: 16px;");

        Label noteLabel = new Label(note + "/10");
        noteLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

        Label labelText = new Label(label);
        labelText.setStyle("-fx-font-size: 10px; -fx-text-fill: #7F8C8D;");

        item.getChildren().addAll(emojiLabel, noteLabel, labelText);
        return item;
    }

    private String getCouleurNote(double moyenne) {
        if (moyenne >= 8.0) return "#27AE60"; // Vert - Excellent
        if (moyenne >= 6.0) return "#3498DB"; // Bleu - Bien
        if (moyenne >= 4.0) return "#F39C12"; // Orange - Moyen
        return "#E74C3C"; // Rouge - Faible
    }

    private void consulterFeedback(Feedback f, Entretien entretien) {
        try {
            // Récupérer les participants
            List<String> participants = entretienService.getParticipants(entretien.getIdEntretien());
            String candidatNom = participants.isEmpty() ? "Candidat inconnu" : participants.get(0);
            String titreOffre = entretienService.getOffreTitre(entretien.getIdOffre());

            // Calculer la moyenne
            double moyenne = (f.getCompetenceTechniques() + f.getCompetenceCommunication() +
                    f.getMotivation() + f.getAdequationAuPoste()) / 4.0;

            // Créer une boîte de dialogue de consultation
            Alert dialog = new Alert(Alert.AlertType.INFORMATION);
            dialog.setTitle("📊 Détails du Feedback");
            dialog.setHeaderText("Feedback - Entretien #" + entretien.getIdEntretien());

            String content = String.format(
                    "📋 INFORMATIONS GÉNÉRALES\n" +
                            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                            "Offre : %s\n" +
                            "Candidat : %s\n" +
                            "Date entretien : %s\n" +
                            "Date feedback : %s\n\n" +
                            "📊 ÉVALUATION (Moyenne : %.1f/10)\n" +
                            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                            "💻 Compétences Techniques : %d/10\n" +
                            "💬 Compétences Communication : %d/10\n" +
                            "🎯 Motivation : %d/10\n" +
                            "✅ Adéquation au Poste : %d/10\n\n" +
                            "💬 COMMENTAIRE GÉNÉRAL\n" +
                            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                            "%s\n\n" +
                            "⚠️ COMPÉTENCES MANQUANTES\n" +
                            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                            "%s\n\n" +
                            "🎓 FORMATION RECOMMANDÉE\n" +
                            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                            "%s",
                    titreOffre,
                    candidatNom,
                    entretien.getDateEntretien().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    f.getDateFeedback().toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                    moyenne,
                    f.getCompetenceTechniques(),
                    f.getCompetenceCommunication(),
                    f.getMotivation(),
                    f.getAdequationAuPoste(),
                    f.getCommentaire(),
                    f.getCompetenceManquantes(),
                    f.isSuggestionFormation() ? "✅ Oui, une formation est recommandée" : "❌ Non, pas de formation nécessaire"
            );

            dialog.setContentText(content);

            // Agrandir la fenêtre
            dialog.getDialogPane().setPrefWidth(700);
            dialog.getDialogPane().setPrefHeight(600);

            dialog.showAndWait();

        } catch (SQLException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de charger les détails : " + ex.getMessage());
        }
    }

    private void modifierFeedback(Feedback f, Entretien entretien) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/jobnest/gentretien/feedback-form.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));

            FeedbackFormController ctrl = loader.getController();
            ctrl.setEntretien(entretien);
            ctrl.setFeedbackExistant(f); // Nouvelle méthode à ajouter

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
        confirm.setTitle("⚠️ Confirmation");
        confirm.setHeaderText("Supprimer ce feedback ?");
        confirm.setContentText("Cette action est irréversible. Toutes les données du feedback seront perdues.");

        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                feedbackService.delete(f.getIdFeedback());
                rafraichirListe();
                showAlert(Alert.AlertType.INFORMATION, "✅ Succès",
                        "Le feedback a été supprimé avec succès.");
            } catch (SQLException ex) {
                showAlert(Alert.AlertType.ERROR, "❌ Erreur",
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