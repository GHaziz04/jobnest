package tn.jobnest.gformation.Controller;

import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import tn.jobnest.gformation.model.Ressource;
import tn.jobnest.gformation.model.Module;
import tn.jobnest.gformation.model.Formation;
import tn.jobnest.gformation.model.QuizQuestion;
import tn.jobnest.gformation.services.ServiceRessource;
import tn.jobnest.gformation.services.ServiceQuizz;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class DetailsModuleController {

    @FXML private VBox sidebarResources;
    @FXML private StackPane viewerContainer;
    @FXML private Label lblTitreModule, lblNomRessource, lblIndex;
    @FXML private Button btnPrecedent, btnSuivant;

    private final ServiceRessource serviceRessource = new ServiceRessource();
    private final ServiceQuizz serviceQuizz = new ServiceQuizz();

    private List<Ressource> listeRessources = new ArrayList<>();
    private int currentIndex = 0;
    private int idModuleActuel;
    private Formation formationActive;
    private Module moduleActuel;
    private MediaPlayer mediaPlayer;

    public void initData(Module module, Formation formation) {
        this.formationActive = formation;
        this.moduleActuel = module;
        if (module != null) {
            this.idModuleActuel = module.getId_module();
            if (lblTitreModule != null) lblTitreModule.setText(module.getTitre().toUpperCase());
            chargerDonnees();
        }
    }

    private void chargerDonnees() {
        List<Ressource> res = serviceRessource.getRessourcesParModule(idModuleActuel);
        this.listeRessources = (res != null) ? res : new ArrayList<>();

        if (!listeRessources.isEmpty()) {
            if (currentIndex >= listeRessources.size()) currentIndex = 0;
            afficherRessourceActuelle();
        } else {
            if (lblNomRessource != null) lblNomRessource.setText("Aucune ressource");
            viewerContainer.getChildren().setAll(new Label("Ce module ne contient pas encore de ressources."));
            sidebarResources.getChildren().clear();
        }
    }

    private void afficherRessourceActuelle() {
        if (listeRessources.isEmpty()) return;
        stopVideo();

        Ressource res = listeRessources.get(currentIndex);
        if (lblNomRessource != null) lblNomRessource.setText(res.getTitre());
        if (lblIndex != null) lblIndex.setText("RESSOURCE " + (currentIndex + 1) + " SUR " + listeRessources.size());

        viewerContainer.getChildren().clear();
        VBox mainContent = new VBox(25);
        mainContent.setPadding(new Insets(30));
        mainContent.setStyle("-fx-background-color: #f8fafc;");

        // --- BARRE D'OUTILS ADMIN ---
        HBox adminToolbar = new HBox(15);
        adminToolbar.setAlignment(Pos.CENTER_RIGHT);
        adminToolbar.setPadding(new Insets(0, 0, 15, 0));
        adminToolbar.setStyle("-fx-border-color: #e2e8f0; -fx-border-width: 0 0 1 0;");

        Button btnEdit = new Button("✎ Modifier");
        btnEdit.setStyle("-fx-background-color: white; -fx-text-fill: #2b6cb0; -fx-font-weight: bold; -fx-border-color: #cbd5e0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand;");
        btnEdit.setOnAction(e -> handleModifierRessource(res));

        Button btnDelete = new Button("🗑 Supprimer");
        btnDelete.setStyle("-fx-background-color: #fff5f5; -fx-text-fill: #c53030; -fx-font-weight: bold; -fx-border-color: #fed7d7; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand;");
        btnDelete.setOnAction(e -> handleSupprimerRessource(res));

        adminToolbar.getChildren().addAll(btnEdit, btnDelete);
        mainContent.getChildren().add(adminToolbar);

        VBox displayArea = new VBox(25);
        displayArea.setAlignment(Pos.TOP_CENTER);
        VBox.setVgrow(displayArea, Priority.ALWAYS);

        // --- ROUTAGE DU RENDU ---
        if ("QUIZ".equalsIgnoreCase(res.getType())) {
            renderQuizView(displayArea, res);
        } else {
            String contenuStr = res.getValeur_contenu();
            if (contenuStr != null) {
                String lower = contenuStr.toLowerCase();
                if (lower.endsWith(".mp4") || lower.endsWith(".m4v")) renderVideoView(displayArea, contenuStr);
                else if (lower.endsWith(".pdf")) renderPdfView(displayArea, contenuStr);
                else renderTextView(displayArea, res);
            }
        }

        mainContent.getChildren().add(displayArea);

        ScrollPane scroll = new ScrollPane(mainContent);
        scroll.setFitToWidth(true);
        scroll.setPannable(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: #f8fafc;");
        viewerContainer.getChildren().setAll(scroll);

        if (btnPrecedent != null) btnPrecedent.setDisable(currentIndex == 0);
        if (btnSuivant != null) btnSuivant.setText(currentIndex == listeRessources.size() - 1 ? "Terminer" : "Suivant");
        remplirPlanSidebar();
    }

    private void renderQuizView(VBox container, Ressource res) {
        List<QuizQuestion> questions = serviceQuizz.getQuizComplet(res.getId_ressource());
        Label t = new Label("🎯 Évaluation : " + res.getTitre());
        t.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #1a202c;");
        container.getChildren().add(t);

        if (questions == null || questions.isEmpty()) {
            container.getChildren().add(new Label("Aucune question trouvée pour ce quiz."));
            return;
        }

        for (int i = 0; i < questions.size(); i++) {
            QuizQuestion q = questions.get(i);
            VBox qCard = new VBox(15);
            qCard.setMaxWidth(850);
            qCard.setStyle("-fx-padding: 25; -fx-background-color: white; -fx-background-radius: 15; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.06), 10, 0, 0, 5);");

            Label lblQ = new Label((i + 1) + ". " + q.getQuestion());
            lblQ.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
            lblQ.setWrapText(true);
            qCard.getChildren().add(lblQ);

            ToggleGroup group = new ToggleGroup();
            for (String opt : q.getOptions()) {
                RadioButton rb = new RadioButton(opt);
                rb.setToggleGroup(group);
                rb.setStyle("-fx-padding: 5 0;");
                qCard.getChildren().add(rb);
            }
            container.getChildren().add(qCard);
        }
    }

    // --- LA MÉTHODE MODIFIÉE POUR ÉVITER L'ÉCRAN BLANC ---
    private void renderPdfView(VBox container, String pdfPath) {
        VBox card = new VBox(20);
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: white; -fx-padding: 50; -fx-background-radius: 20; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 15, 0, 0, 5);");
        card.setMaxWidth(600);

        Label icon = new Label("PDF");
        icon.setStyle("-fx-font-size: 60px; -fx-text-fill: #e53e3e; -fx-font-weight: bold;");

        Label info = new Label("Cliquer ci-dessous pour ouvrir le document.");
        info.setStyle("-fx-font-size: 18px; -fx-text-fill: #4a5568;");

        Button btnOpen = new Button("📄 Consulter le Cours (PDF)");
        btnOpen.setStyle("-fx-background-color: #3182ce; -fx-text-fill: white; -fx-font-size: 16px; " +
                "-fx-font-weight: bold; -fx-padding: 15 30; -fx-background-radius: 10; -fx-cursor: hand;");

        btnOpen.setOnAction(e -> {
            try {
                File file = new File(pdfPath);
                if (file.exists()) {
                    Desktop.getDesktop().open(file);
                } else {
                    System.out.println("Fichier non trouvé sur le disque : " + pdfPath);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        card.getChildren().addAll(icon, info, btnOpen);
        container.getChildren().add(card);
    }

    private void renderTextView(VBox container, Ressource res) {
        VBox card = new VBox(25);
        card.setMaxWidth(850);
        card.setStyle("-fx-padding: 40; -fx-background-color: white; -fx-background-radius: 20; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 20, 0, 0, 8);");

        Label titre = new Label(res.getTitre());
        titre.setStyle("-fx-font-size: 30px; -fx-font-weight: bold;");

        Label contenu = new Label(res.getValeur_contenu());
        contenu.setWrapText(true);
        contenu.setStyle("-fx-font-size: 17px; -fx-line-spacing: 6;");

        card.getChildren().addAll(titre, contenu);
        container.getChildren().add(card);
    }

    private void renderVideoView(VBox container, String videoPath) {
        try {
            File file = new File(videoPath);
            if (file.exists()) {
                mediaPlayer = new MediaPlayer(new Media(file.toURI().toString()));
                MediaView mediaView = new MediaView(mediaPlayer);
                mediaView.setFitWidth(750);
                mediaView.setPreserveRatio(true);

                Button btnPlay = new Button("▶ Lecture / Pause");
                btnPlay.setStyle("-fx-background-color: #3182ce; -fx-text-fill: white; -fx-background-radius: 30; -fx-padding: 10 25; -fx-font-weight: bold;");
                btnPlay.setOnAction(e -> {
                    if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) mediaPlayer.pause();
                    else mediaPlayer.play();
                });

                VBox videoBox = new VBox(20, mediaView, btnPlay);
                videoBox.setAlignment(Pos.CENTER);
                videoBox.setStyle("-fx-padding: 25; -fx-background-color: #1a202c; -fx-background-radius: 20;");
                container.getChildren().add(videoBox);
                mediaPlayer.play();
            }
        } catch (Exception e) {
            container.getChildren().add(new Label("Erreur vidéo."));
        }
    }

    private void remplirPlanSidebar() {
        sidebarResources.getChildren().clear();
        for (int i = 0; i < listeRessources.size(); i++) {
            Ressource r = listeRessources.get(i);
            int idx = i;
            boolean isActive = (i == currentIndex);

            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(14, 18, 14, 18));
            row.setCursor(javafx.scene.Cursor.HAND);

            if (isActive) {
                row.setStyle("-fx-background-color: #ebf8ff; -fx-border-color: #3182ce; -fx-border-width: 0 0 0 5;");
            }

            Label lblTitre = new Label((i + 1) + ". " + r.getTitre());
            lblTitre.setStyle(isActive ? "-fx-text-fill: #2b6cb0; -fx-font-weight: bold;" : "-fx-text-fill: #4a5568;");

            row.getChildren().add(lblTitre);
            row.setOnMouseClicked(e -> { currentIndex = idx; afficherRessourceActuelle(); });
            sidebarResources.getChildren().add(row);
        }
    }

    private void handleModifierRessource(Ressource res) {
        if ("QUIZ".equalsIgnoreCase(res.getType())) {
            ouvrirEditeurQuiz(res);
        } else {
            viewerContainer.getChildren().clear();
            VBox editBox = new VBox(20);
            editBox.setPadding(new Insets(30));
            editBox.setStyle("-fx-background-color: white; -fx-background-radius: 20;");

            TextField titleField = new TextField(res.getTitre());
            TextArea contentArea = new TextArea(res.getValeur_contenu());
            contentArea.setPrefHeight(400);

            Button btnSave = new Button("🚀 Sauvegarder");
            btnSave.setStyle("-fx-background-color: #38a169; -fx-text-fill: white; -fx-font-weight: bold;");
            btnSave.setOnAction(e -> {
                res.setTitre(titleField.getText());
                res.setValeur_contenu(contentArea.getText());
                serviceRessource.modifier(res);
                afficherRessourceActuelle();
            });

            editBox.getChildren().addAll(new Label("Titre :"), titleField, new Label("Contenu :"), contentArea, btnSave);
            viewerContainer.getChildren().setAll(editBox);
        }
    }

    private void handleSupprimerRessource(Ressource res) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer ?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                stopVideo();
                serviceRessource.supprimer(res.getId_ressource());
                chargerDonnees();
            }
        });
    }

    private void stopVideo() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
    }

    private void ouvrirEditeurQuiz(Ressource res) {
        try {
            stopVideo();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/quiz_editeur.fxml"));
            Parent root = loader.load();
            QuizController ctrl = loader.getController();
            ctrl.setRessourcePourQuiz(res, this.moduleActuel, this.formationActive);
            lblTitreModule.getScene().setRoot(root);
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML private void handlePrecedent() { if (currentIndex > 0) { currentIndex--; afficherRessourceActuelle(); } }

    @FXML private void handleSuivant(ActionEvent event) {
        if (currentIndex < listeRessources.size() - 1) { currentIndex++; afficherRessourceActuelle(); }
        else { handleRetour(event); }
    }

    @FXML private void handleRetour(ActionEvent event) { stopVideo(); allerVersGestionModules(event); }

    private void allerVersGestionModules(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/gestion_modules.fxml"));
            Parent root = loader.load();
            if (loader.getController() instanceof GestionModulesController ctrl) ctrl.setFormationActive(this.formationActive);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML private void handleGenererQuizIA(ActionEvent event) {
        if (listeRessources.isEmpty()) return;
        ouvrirEditeurQuiz(listeRessources.get(currentIndex));
    }
}