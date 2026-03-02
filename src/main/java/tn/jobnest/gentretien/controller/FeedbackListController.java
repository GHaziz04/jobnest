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
import javafx.scene.Parent;
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
    private void ouvrirProfil(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/tn/jobnest/gentretien/profil-recruteur.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            java.net.URL css = getClass().getResource("/tn/jobnest/gentretien/styles.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            stage.setScene(scene);
            stage.setTitle("JobNest - Mon Profil");
            stage.show();
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le profil : " + ex.getMessage());
        }
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
        Button btnConsulter = new Button("Consulter");
        btnConsulter.getStyleClass().add("button-primary");
        btnConsulter.setPrefWidth(180);
        btnConsulter.setPrefHeight(40);
        btnConsulter.setOnAction(ev -> consulterFeedback(f, entretien));

        // Bouton Modifier
        Button btnModifier = new Button("Modifier");
        btnModifier.getStyleClass().add("button-success");
        btnModifier.setPrefWidth(180);
        btnModifier.setPrefHeight(40);
        btnModifier.setOnAction(ev -> modifierFeedback(f, entretien));

        // Bouton Supprimer
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
            List<String> participants = entretienService.getParticipants(entretien.getIdEntretien());
            String candidatNom = participants.isEmpty() ? "Candidat inconnu" : participants.get(0);
            String titreOffre = entretienService.getOffreTitre(entretien.getIdOffre());

            double moyenne = (f.getCompetenceTechniques() + f.getCompetenceCommunication() +
                    f.getMotivation() + f.getAdequationAuPoste()) / 4.0;

            // ── Couleur selon la note ──
            String couleur = moyenne >= 8.0 ? "#27AE60"
                    : moyenne >= 6.0 ? "#2563EB"
                    : moyenne >= 4.0 ? "#F97316"
                    : "#EF4444";

            String niveauTexte = moyenne >= 8.0 ? "Excellent"
                    : moyenne >= 6.0 ? "Bien"
                    : moyenne >= 4.0 ? "Moyen"
                    : "Faible";

            // ── Création du Dialog ──
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Détails du Feedback");
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

            // ── ScrollPane principal ──
            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setFitToWidth(true);
            scrollPane.setPrefWidth(720);
            scrollPane.setPrefHeight(580);
            scrollPane.setStyle("-fx-background-color: transparent; -fx-background: #F8FAFC; -fx-border-color: transparent;");

            VBox container = new VBox(0);
            container.setStyle("-fx-background-color: #F8FAFC;");

            // ── HEADER BLEU MARINE ──
            VBox header = new VBox(8);
            header.setStyle("-fx-background-color: #1E3A5F; -fx-padding: 24 30 20 30;");

            HBox headerRow = new HBox(16);
            headerRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            // Badge note ronde
            javafx.scene.layout.StackPane noteBadge = new javafx.scene.layout.StackPane();
            noteBadge.setPrefSize(70, 70);
            noteBadge.setMinSize(70, 70);
            noteBadge.setStyle("-fx-background-color: " + couleur + "; -fx-background-radius: 35;");

            VBox noteContent = new VBox(0);
            noteContent.setAlignment(javafx.geometry.Pos.CENTER);
            Label noteGrande = new Label(String.format("%.1f", moyenne));
            noteGrande.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");
            Label surDix = new Label("/10");
            surDix.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.85);");
            noteContent.getChildren().addAll(noteGrande, surDix);
            noteBadge.getChildren().add(noteContent);

            VBox headerInfo = new VBox(4);
            Label titreLabel = new Label("Feedback — Entretien #" + entretien.getIdEntretien());
            titreLabel.setStyle("-fx-font-size: 19px; -fx-font-weight: bold; -fx-text-fill: white;");
            Label offreLabel = new Label("📌 " + titreOffre + "   •   👤 " + candidatNom);
            offreLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #8BAECF;");

            String dateEntretienStr = entretien.getDateEntretien() != null
                    ? entretien.getDateEntretien().toLocalDate()
                    .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    : "Date inconnue";
            String dateFeedbackStr = f.getDateFeedback() != null
                    ? f.getDateFeedback().toLocalDateTime()
                    .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    : "—";

            Label datesLabel = new Label("📅 Entretien : " + dateEntretienStr
                    + "    🕐 Feedback : " + dateFeedbackStr);
            datesLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #8BAECF;");

            headerInfo.getChildren().addAll(titreLabel, offreLabel, datesLabel);
            headerRow.getChildren().addAll(noteBadge, headerInfo);
            header.getChildren().add(headerRow);

            // ── CORPS ──
            VBox body = new VBox(16);
            body.setStyle("-fx-padding: 24 30 24 30; -fx-background-color: #F8FAFC;");

            // ── Section : Évaluation ──
            Label sectionEval = new Label("ÉVALUATION DÉTAILLÉE");
            sectionEval.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #94A3B8; -fx-padding: 0 0 6 0;");

            // Card notes 4 critères
            HBox notesCard = new HBox(0);
            notesCard.setStyle("-fx-background-color: white; -fx-background-radius: 14; "
                    + "-fx-effect: dropshadow(gaussian, rgba(30,58,95,0.08), 12, 0, 0, 4); "
                    + "-fx-border-color: #EEF2FF; -fx-border-width: 1; -fx-border-radius: 14;");

            String[][] criteres = {
                    {"💻", "Technique",      String.valueOf(f.getCompetenceTechniques())},
                    {"💬", "Communication",  String.valueOf(f.getCompetenceCommunication())},
                    {"🎯", "Motivation",     String.valueOf(f.getMotivation())},
                    {"✅", "Adéquation",     String.valueOf(f.getAdequationAuPoste())}
            };

            for (int i = 0; i < criteres.length; i++) {
                String[] c = criteres[i];
                int noteVal = Integer.parseInt(c[2]);
                String noteCouleur = noteVal >= 8 ? "#27AE60" : noteVal >= 6 ? "#2563EB" : noteVal >= 4 ? "#F97316" : "#EF4444";

                VBox item = new VBox(6);
                item.setAlignment(javafx.geometry.Pos.CENTER);
                item.setPrefWidth(150);
                item.setStyle("-fx-padding: 18 10 18 10;"
                        + (i < 3 ? " -fx-border-color: transparent #EEF2FF transparent transparent; -fx-border-width: 0 1 0 0;" : ""));

                Label emoji = new Label(c[0]);
                emoji.setStyle("-fx-font-size: 22px;");

                Label noteNum = new Label(c[2] + "/10");
                noteNum.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + noteCouleur + ";");

                Label labelCritere = new Label(c[1]);
                labelCritere.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8; -fx-font-weight: 500;");

                // Mini barre de progression
                HBox barBg = new HBox();
                barBg.setPrefWidth(80);
                barBg.setPrefHeight(5);
                barBg.setStyle("-fx-background-color: #F1F5F9; -fx-background-radius: 3;");

                HBox barFill = new HBox();
                barFill.setPrefHeight(5);
                barFill.setPrefWidth(noteVal * 8); // max 80px pour 10
                barFill.setStyle("-fx-background-color: " + noteCouleur + "; -fx-background-radius: 3;");
                barBg.getChildren().add(barFill);

                item.getChildren().addAll(emoji, noteNum, labelCritere, barBg);
                HBox.setHgrow(item, javafx.scene.layout.Priority.ALWAYS);
                notesCard.getChildren().add(item);
            }

            // ── Badge niveau global ──
            HBox badgeRow = new HBox(10);
            badgeRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            Label badgeNiveau = new Label("  " + niveauTexte + "  ");
            badgeNiveau.setStyle("-fx-background-color: " + couleur + "; -fx-text-fill: white; "
                    + "-fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 8; "
                    + "-fx-padding: 4 12 4 12;");
            Label moyenneText = new Label("Moyenne globale : " + String.format("%.1f", moyenne) + " / 10");
            moyenneText.setStyle("-fx-font-size: 13px; -fx-text-fill: #334155; -fx-font-weight: 600;");
            badgeRow.getChildren().addAll(badgeNiveau, moyenneText);

            // ── Section : Commentaire ──
            Label sectionComment = new Label("COMMENTAIRE GÉNÉRAL");
            sectionComment.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #94A3B8; -fx-padding: 8 0 6 0;");

            VBox commentCard = new VBox(10);
            commentCard.setStyle("-fx-background-color: white; -fx-background-radius: 12; "
                    + "-fx-effect: dropshadow(gaussian, rgba(30,58,95,0.08), 12, 0, 0, 4); "
                    + "-fx-border-color: #EEF2FF; -fx-border-width: 1; -fx-border-radius: 12; "
                    + "-fx-padding: 16 18 16 18;");

            Label commentTexte = new Label(f.getCommentaire());
            commentTexte.setWrapText(true);
            commentTexte.setStyle("-fx-font-size: 13px; -fx-text-fill: #334155; -fx-line-spacing: 3;");
            commentCard.getChildren().add(commentTexte);

            // ── Section : Compétences manquantes ──
            Label sectionComp = new Label("COMPÉTENCES MANQUANTES");
            sectionComp.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #94A3B8; -fx-padding: 8 0 6 0;");

            VBox compCard = new VBox(8);
            compCard.setStyle("-fx-background-color: #FFF7ED; -fx-background-radius: 12; "
                    + "-fx-border-color: #FED7AA; -fx-border-width: 1; -fx-border-radius: 12; "
                    + "-fx-padding: 14 18 14 18;");

            HBox compRow = new HBox(10);
            compRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            Label compEmoji = new Label("⚠️");
            compEmoji.setStyle("-fx-font-size: 16px;");
            Label compTexte = new Label(f.getCompetenceManquantes());
            compTexte.setWrapText(true);
            compTexte.setStyle("-fx-font-size: 13px; -fx-text-fill: #9A3412; -fx-font-weight: 500;");
            compRow.getChildren().addAll(compEmoji, compTexte);
            compCard.getChildren().add(compRow);

            // ── Badge formation ──
            if (f.isSuggestionFormation()) {
                Label formationBadge = new Label("  🎓  Formation recommandée pour ce candidat  ");
                formationBadge.setStyle("-fx-background-color: #EFF6FF; -fx-text-fill: #1D4ED8; "
                        + "-fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 10; "
                        + "-fx-border-color: #BFDBFE; -fx-border-width: 1; -fx-border-radius: 10; "
                        + "-fx-padding: 10 16 10 16;");
                body.getChildren().addAll(sectionEval, notesCard, badgeRow, sectionComment, commentCard,
                        sectionComp, compCard, formationBadge);
            } else {
                Label pasFormation = new Label("  ✓  Aucune formation requise  ");
                pasFormation.setStyle("-fx-background-color: #F0FDF4; -fx-text-fill: #166534; "
                        + "-fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 10; "
                        + "-fx-border-color: #BBF7D0; -fx-border-width: 1; -fx-border-radius: 10; "
                        + "-fx-padding: 10 16 10 16;");
                body.getChildren().addAll(sectionEval, notesCard, badgeRow, sectionComment, commentCard,
                        sectionComp, compCard, pasFormation);
            }

            container.getChildren().addAll(header, body);
            scrollPane.setContent(container);

            // ── Style du DialogPane ──
            dialog.getDialogPane().setContent(scrollPane);
            dialog.getDialogPane().setStyle("-fx-background-color: #F8FAFC; -fx-padding: 0; -fx-background-radius: 14;");
            dialog.getDialogPane().setPrefWidth(720);

            // Style bouton Fermer
            javafx.scene.Node closeBtn = dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
            if (closeBtn != null) {
                closeBtn.setStyle("-fx-background-color: #2563EB; -fx-text-fill: white; "
                        + "-fx-font-weight: bold; -fx-background-radius: 8; "
                        + "-fx-padding: 8 22 8 22; -fx-font-size: 13px; -fx-cursor: hand;");
            }

            dialog.showAndWait();

        } catch (SQLException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les détails : " + ex.getMessage());
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