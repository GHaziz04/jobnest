package tn.jobnest.gformation.Controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.jobnest.gformation.model.*;
import tn.jobnest.gformation.services.*;
import tn.jobnest.gformation.Controller.DetailsModuleController;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class QuizController {

    @FXML private TextField txtTitreQuiz;
    @FXML private VBox questionsContainer;
    @FXML private Button btnGenererIA;

    private final ServiceGemini serviceGemini = new ServiceGemini();
    private final ServiceQuizz serviceQuizz = new ServiceQuizz();
    private final ServiceRessource serviceRessource = new ServiceRessource();

    private tn.jobnest.gformation.model.Module moduleSource;
    private Formation formationSource;
    private int idRessourceParent;

    public void setRessourcePourQuiz(Ressource ressource, tn.jobnest.gformation.model.Module module, Formation formation) {
        this.moduleSource = module;
        this.formationSource = formation;
        // Ici, on garde l'ID de la ressource cours juste pour charger des questions si on veut
        this.idRessourceParent = ressource.getId_ressource();
        this.txtTitreQuiz.setText("Quiz : " + ressource.getTitre());

        chargerSiExistant();
    }

    private void chargerSiExistant() {
        List<QuizQuestion> existantes = serviceQuizz.getQuizComplet(idRessourceParent);
        if (existantes != null && !existantes.isEmpty()) {
            questionsContainer.getChildren().clear();
            for (QuizQuestion q : existantes) {
                ajouterLigneQuestionParam(q.getQuestion(), q.getOptions(), q.getReponseCorrecte());
            }
        }
    }

    @FXML
    private void handleGenererIA() {
        btnGenererIA.setDisable(true);
        btnGenererIA.setText("Génération en cours...");

        new Thread(() -> {
            try {
                List<QuizQuestion> questionsIA = serviceGemini.genererQuiz(moduleSource.getDescription());

                Platform.runLater(() -> {
                    if (questionsIA != null) {
                        for (QuizQuestion q : questionsIA) {
                            ajouterLigneQuestionParam(q.getQuestion(), q.getOptions(), q.getReponseCorrecte());
                        }
                    }
                    btnGenererIA.setDisable(false);
                    btnGenererIA.setText("Ajouter d'autres questions via IA");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    btnGenererIA.setDisable(false);
                    btnGenererIA.setText("Erreur IA - Réessayer");
                });
            }
        }).start();
    }

    @FXML
    private void handleAjouterQuestionManuelle() {
        ajouterLigneQuestionParam("", List.of("", "", ""), "");
    }

    private void ajouterLigneQuestionParam(String question, List<String> options, String correcte) {
        VBox block = new VBox(10);
        block.setStyle("-fx-background-color: #ffffff; -fx-padding: 15; -fx-border-color: #cccccc; -fx-border-radius: 5;");

        TextField tfQ = new TextField(question); tfQ.setPromptText("Texte de la question");
        TextField tfO1 = new TextField(options.get(0)); tfO1.setPromptText("Option 1");
        TextField tfO2 = new TextField(options.get(1)); tfO2.setPromptText("Option 2");
        TextField tfO3 = new TextField(options.get(2)); tfO3.setPromptText("Option 3");

        ComboBox<Integer> cbCorrect = new ComboBox<>();
        cbCorrect.getItems().addAll(1, 2, 3);

        if (!correcte.isEmpty()) {
            for (int i = 0; i < options.size(); i++) {
                if (options.get(i).equalsIgnoreCase(correcte)) cbCorrect.setValue(i + 1);
            }
        }

        Button btnSuppr = new Button("Supprimer");
        btnSuppr.setStyle("-fx-background-color: #ff4d4d; -fx-text-fill: white;");
        btnSuppr.setOnAction(e -> questionsContainer.getChildren().remove(block));

        block.getChildren().addAll(new Label("Question :"), tfQ, new Label("Options :"), tfO1, tfO2, tfO3, new Label("N° Bonne Réponse :"), cbCorrect, btnSuppr);
        questionsContainer.getChildren().add(block);
    }

    @FXML
    private void handleSauvegarderTout() {
        try {
            // --- PARTIE CORRIGÉE : Création d'une NOUVELLE ressource au lieu de modifier l'ancienne ---
            Ressource nouvelleResQuiz = new Ressource();
            nouvelleResQuiz.setTitre(txtTitreQuiz.getText());
            nouvelleResQuiz.setValeur_contenu("DATABASE");
            nouvelleResQuiz.setType("QUIZ");
            nouvelleResQuiz.setId_module(this.moduleSource.getId_module());
            nouvelleResQuiz.setOrdre(10); // Un ordre élevé pour qu'il soit après le cours

            // On ajoute la ressource et on récupère son ID généré
            int newIdRessource = serviceRessource.ajouter(nouvelleResQuiz);

            // 1. Créer le Quiz parent lié à la nouvelle ressource
            int idQuizz = serviceQuizz.creerQuizz(newIdRessource, txtTitreQuiz.getText());

            // 2. Sauvegarder les questions
            for (Node node : questionsContainer.getChildren()) {
                if (node instanceof VBox block) {
                    String q = ((TextField) block.getChildren().get(1)).getText();
                    String o1 = ((TextField) block.getChildren().get(3)).getText();
                    String o2 = ((TextField) block.getChildren().get(4)).getText();
                    String o3 = ((TextField) block.getChildren().get(5)).getText();

                    @SuppressWarnings("unchecked")
                    Integer correct = ((ComboBox<Integer>) block.getChildren().get(7)).getValue();

                    if (correct != null && !q.isEmpty()) {
                        serviceQuizz.ajouterQuestion(idQuizz, q, o1, o2, o3, correct);
                    }
                }
            }

            new Alert(Alert.AlertType.INFORMATION, "Nouveau Quiz créé avec succès !").showAndWait();
            handleQuitter();

        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Erreur lors de la sauvegarde : " + e.getMessage()).showAndWait();
            e.printStackTrace();
        }
    }

    @FXML
    private void handleQuitter() {
        try {
            String fxmlPath = "/view/details_module.fxml";
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));

            if (loader.getLocation() == null) {
                fxmlPath = "/tn/jobnest/gformation/view/details_module.fxml";
                loader.setLocation(getClass().getResource(fxmlPath));
            }

            Parent root = loader.load();
            DetailsModuleController controller = loader.getController();
            controller.initData(this.moduleSource, this.formationSource);

            Stage stage = (Stage) txtTitreQuiz.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}