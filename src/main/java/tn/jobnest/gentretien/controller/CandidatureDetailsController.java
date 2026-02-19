package tn.jobnest.gentretien.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.jobnest.gentretien.model.CandidatureDTO;
import tn.jobnest.gentretien.model.Document;
import tn.jobnest.gentretien.service.CandidatureService;
// Assurez-vous que l'import du contrôleur de profil est correct selon votre structure
import tn.jobnest.gentretien.controller.ProfileViewerController;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;

public class CandidatureDetailsController {

    @FXML private Label lblTitreFenetre;
    @FXML private TableView<Document> tableDocuments;
    @FXML private TableColumn<Document, String> colType;
    @FXML private TableColumn<Document, String> colNom;
    @FXML private TableColumn<Document, String> colDate;
    @FXML private TableColumn<Document, Void> colAction;

    private final CandidatureService service = new CandidatureService();

    // --- CORRECTION : Déclaration de la variable pour stocker la candidature sélectionnée ---
    private CandidatureDTO candidatureCourante;

    @FXML
    public void initialize() {
        // 1. Configuration de la colonne TYPE
        colType.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getTypeDocument()));

        // 2. Configuration de la colonne NOM DU FICHIER
        colNom.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getNomDocument()));

        // 3. Configuration de la colonne DATE (Avec formatage)
        colDate.setCellValueFactory(data -> {
            Timestamp ts = data.getValue().getDateUpload();
            if (ts != null) {
                String dateFormatee = ts.toString();
                if (dateFormatee.length() > 19) {
                    dateFormatee = dateFormatee.substring(0, 19);
                }
                return new SimpleStringProperty(dateFormatee);
            } else {
                return new SimpleStringProperty("-");
            }
        });

        // 4. Configuration de la colonne ACTION (Bouton Ouvrir)
        ajouterBoutonOuvrir();
    }

    /**
     * Méthode appelée par le contrôleur principal pour charger les infos
     */
    public void chargerDonnees(CandidatureDTO dto) {
        // --- CORRECTION : On stocke l'objet reçu pour pouvoir l'utiliser plus tard ---
        this.candidatureCourante = dto;

        lblTitreFenetre.setText("Documents de : " + dto.getNomComplet());

        // Récupération depuis la base de données
        List<Document> docs = service.getDocumentsByCandidature(dto.getIdCandidature());

        ObservableList<Document> observableDocs = FXCollections.observableArrayList(docs);
        tableDocuments.setItems(observableDocs);

        if(docs.isEmpty()){
            tableDocuments.setPlaceholder(new Label("Aucun document fourni pour cette candidature."));
        }
    }

    private void ajouterBoutonOuvrir() {
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("👁 Ouvrir");

            {
                btn.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white; -fx-font-size: 11px; -fx-cursor: hand; -fx-font-weight: bold;");
                btn.setOnAction(event -> {
                    Document doc = getTableView().getItems().get(getIndex());
                    if (doc != null) {
                        ouvrirFichier(doc.getCheminFichier());
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btn);
                    setAlignment(Pos.CENTER);
                }
            }
        });
    }

    private void ouvrirFichier(String chemin) {
        if (chemin == null || chemin.isEmpty()) {
            afficherAlerte("Erreur", "Le chemin du fichier est vide.");
            return;
        }

        try {
            File file = new File(chemin);
            if (file.exists()) {
                Desktop.getDesktop().open(file);
            } else {
                afficherAlerte("Fichier introuvable", "Le fichier n'existe pas : " + chemin);
            }
        } catch (IOException e) {
            e.printStackTrace();
            afficherAlerte("Erreur système", "Impossible d'ouvrir le fichier.");
        }
    }

    // --- CORRECTION : Méthode pour voir le profil avec l'ID du candidat ---
    @FXML
    private void voirProfilAction() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/jobnest/gentretien/ProfileView.fxml"));
            Parent root = loader.load();

            // C'est cette étape qui "récupère" les données
            ProfileViewerController controller = loader.getController();
            controller.setIdCandidat(candidatureCourante.getIdCandidat());

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void afficherAlerte(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("JobNest");
        alert.setHeaderText(titre);
        alert.setContentText(message);
        alert.show();
    }

    @FXML
    private void fermer() {
        Stage stage = (Stage) tableDocuments.getScene().getWindow();
        stage.close();
    }

}