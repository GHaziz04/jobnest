package tn.jobnest.gformation.Controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.jobnest.gformation.model.Formation;
import tn.jobnest.gformation.services.ServiceFormation;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AffichageController {

    @FXML private StackPane mainContent;
    @FXML private VBox viewSessions;
    @FXML private Button btnDashboard;
    @FXML private Button btnSessions;
    @FXML private FlowPane cardsContainer;
    @FXML private TextField txtRecherche;
    @FXML private ComboBox<String> comboTri;
    @FXML private Pagination pagination;
    @FXML private Label lblAucunResultat;

    private final ServiceFormation sf = new ServiceFormation();
    private List<Formation> toutesLesFormations = new ArrayList<>();
    private List<Formation> formationsFiltrees = new ArrayList<>();
    private final int ITEMS_PER_PAGE = 6;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");

    @FXML
    public void initialize() {
        if (comboTri != null) {
            comboTri.setItems(FXCollections.observableArrayList(
                    "Titre (A-Z)", "Titre (Z-A)", "Prix (Croissant)", "Prix (Décroissant)"
            ));
            comboTri.valueProperty().addListener((obs, oldVal, newVal) -> appliquerFiltresEtTri());
        }
        if (txtRecherche != null) {
            txtRecherche.textProperty().addListener((obs, oldVal, newVal) -> appliquerFiltresEtTri());
        }
        if (pagination != null) {
            pagination.currentPageIndexProperty().addListener((obs, oldIndex, newIndex) -> afficherPage(newIndex.intValue()));
        }
        Platform.runLater(this::chargerCards);
    }

    // --- NAVIGATION ---

    @FXML
    private void afficherTableauDeBord() {
        try {
            URL resource = obtenirResource("/tn/jobnest/gformation/view/tableau_de_bord.fxml");
            if (resource != null) {
                Parent dashboard = FXMLLoader.load(resource);
                mainContent.getChildren().setAll(dashboard);
                majStyleBoutons(btnDashboard, btnSessions);
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    private void afficherSessions() {
        if (viewSessions != null) {
            mainContent.getChildren().setAll(viewSessions);
            majStyleBoutons(btnSessions, btnDashboard);
            chargerCards();
        }
    }

    private void majStyleBoutons(Button actif, Button inactif) {
        if(actif != null) actif.getStyleClass().setAll("sidebar-button-active");
        if(inactif != null) inactif.getStyleClass().setAll("sidebar-button");
    }

    // --- GESTION DES CARTES ---

    public void chargerCards() {
        toutesLesFormations = sf.recupererTout();
        appliquerFiltresEtTri();
    }

    private void appliquerFiltresEtTri() {
        String recherche = (txtRecherche != null) ? txtRecherche.getText().toLowerCase().trim() : "";
        String optionTri = (comboTri != null) ? comboTri.getValue() : null;

        formationsFiltrees = toutesLesFormations.stream()
                .filter(f -> f.getTitre().toLowerCase().contains(recherche))
                .collect(Collectors.toList());

        if (lblAucunResultat != null) lblAucunResultat.setVisible(formationsFiltrees.isEmpty());

        if (pagination != null) {
            pagination.setVisible(!formationsFiltrees.isEmpty());
            if (!formationsFiltrees.isEmpty()) {
                if (optionTri != null) {
                    switch (optionTri) {
                        case "Titre (A-Z)" -> formationsFiltrees.sort(Comparator.comparing(f -> f.getTitre().toLowerCase()));
                        case "Titre (Z-A)" -> formationsFiltrees.sort(Comparator.comparing((Formation f) -> f.getTitre().toLowerCase()).reversed());
                        case "Prix (Croissant)" -> formationsFiltrees.sort(Comparator.comparingDouble(Formation::getPrix));
                        case "Prix (Décroissant)" -> formationsFiltrees.sort(Comparator.comparingDouble(Formation::getPrix).reversed());
                    }
                }
                int pageCount = (int) Math.ceil((double) formationsFiltrees.size() / ITEMS_PER_PAGE);
                pagination.setPageCount(pageCount > 0 ? pageCount : 1);
                afficherPage(0);
            }
        }
    }

    private void afficherPage(int pageIndex) {
        if (cardsContainer == null) return;
        cardsContainer.getChildren().clear();
        int start = pageIndex * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, formationsFiltrees.size());

        if (start < formationsFiltrees.size()) {
            List<Formation> pageItems = formationsFiltrees.subList(start, end);
            for (Formation f : pageItems) {
                cardsContainer.getChildren().add(creerCard(f));
            }
        }
    }

    private VBox creerCard(Formation f) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");
        card.setPrefWidth(350);
        card.setPadding(new Insets(0, 0, 15, 0));

        StackPane imagePane = new StackPane();
        ImageView img = new ImageView();
        img.setFitWidth(350);
        img.setFitHeight(170);
        img.setPreserveRatio(false);

        try {
            String url = (f.getUrl_image() == null || f.getUrl_image().isEmpty()) ? "https://via.placeholder.com/350x170" : f.getUrl_image();
            img.setImage(new Image(url, true));
        } catch (Exception e) {}

        Rectangle clip = new Rectangle(350, 170);
        clip.setArcWidth(30);
        clip.setArcHeight(30);
        imagePane.setClip(clip);

        boolean estComplet = f.getNb_places_occupees() >= f.getNb_places();
        Label badgeStatut = new Label(estComplet ? "COMPLET" : (f.getStatut() != null ? f.getStatut().toUpperCase() : "OUVERT"));
        String colorStatut = estComplet ? "#E53E3E" : ("Ouvert".equalsIgnoreCase(f.getStatut()) ? "#48BB78" : "#F56565");
        badgeStatut.setStyle("-fx-background-color: " + colorStatut + "; -fx-text-fill: white; -fx-padding: 5 12; -fx-background-radius: 0 15 0 15; -fx-font-size: 11; -fx-font-weight: bold;");
        StackPane.setAlignment(badgeStatut, Pos.TOP_RIGHT);

        imagePane.getChildren().addAll(img, badgeStatut);

        VBox content = new VBox(12);
        content.setPadding(new Insets(0, 15, 0, 15));

        Label titre = new Label(f.getTitre());
        titre.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: #2D3748;");
        titre.setWrapText(true);
        titre.setMinHeight(50);

        HBox metaLine = new HBox(10);
        metaLine.setAlignment(Pos.CENTER_LEFT);
        Label lblNiveau = new Label("🔸 " + f.getNiveau());
        lblNiveau.setStyle("-fx-text-fill: #ED8936; -fx-font-weight: bold; -fx-font-size: 12px;");
        Label lblFormateur = new Label("👤 " + f.getNomFormateur());
        lblFormateur.setStyle("-fx-text-fill: #718096; -fx-font-size: 12px;");
        metaLine.getChildren().addAll(lblNiveau, new Separator(javafx.geometry.Orientation.VERTICAL), lblFormateur);

        VBox progressBox = new VBox(5);
        int occupees = f.getNb_places_occupees();
        int total = f.getNb_places();
        double ratio = (total > 0) ? (double) occupees / total : 0;

        ProgressBar pb = new ProgressBar(ratio);
        pb.setMaxWidth(Double.MAX_VALUE);
        pb.setPrefHeight(10);
        String pbColor = (ratio >= 0.9) ? "#F56565" : (ratio > 0.6) ? "#ED8936" : "#48BB78";
        pb.setStyle("-fx-accent: " + pbColor + ";");

        Label lblPlaces = new Label("👥 Places : " + occupees + " / " + total);
        lblPlaces.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #4A5568;");
        progressBox.getChildren().addAll(lblPlaces, pb);

        HBox infoLine = new HBox(15);
        infoLine.setAlignment(Pos.CENTER_LEFT);
        Label lblPrix = new Label("💰 " + f.getPrix() + " DT");
        lblPrix.setStyle("-fx-text-fill: #3182CE; -fx-font-weight: 800; -fx-font-size: 15px;");
        Label lblDuree = new Label("⏱ " + f.getDuree_heures() + "h");
        lblDuree.setStyle("-fx-text-fill: #4A5568; -fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        infoLine.getChildren().addAll(lblPrix, spacer, lblDuree);

        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER);
        Button btnDetails = new Button("Détails");
        btnDetails.setStyle("-fx-background-color: #EDF2F7; -fx-text-fill: #4A5568; -fx-background-radius: 8; -fx-font-weight: bold; -fx-cursor: hand; -fx-pref-width: 140;");
        btnDetails.setOnAction(e -> afficherDetailsFormation(f));

        Button btnModules = new Button("Modules");
        btnModules.setStyle("-fx-background-color: #3182CE; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-weight: bold; -fx-cursor: hand; -fx-pref-width: 140;");
        btnModules.setOnAction(e -> ouvrirGestionModules(f));
        actions.getChildren().addAll(btnDetails, btnModules);

        HBox editBar = new HBox(10);
        editBar.setAlignment(Pos.CENTER_RIGHT);
        Button btnEdit = new Button("Modifier");
        btnEdit.setStyle("-fx-background-color: transparent; -fx-text-fill: #A0AEC0; -fx-cursor: hand; -fx-font-size: 11px;");
        btnEdit.setOnAction(e -> ouvrirModifier(f));
        Button btnDelete = new Button("Supprimer");
        btnDelete.setStyle("-fx-background-color: transparent; -fx-text-fill: #FC8181; -fx-cursor: hand; -fx-font-size: 11px;");
        btnDelete.setOnAction(e -> confirmerSuppression(f));
        editBar.getChildren().addAll(btnEdit, btnDelete);

        content.getChildren().addAll(titre, metaLine, progressBox, infoLine, actions, editBar);
        card.getChildren().addAll(imagePane, content);

        return card;
    }

    // --- PARTIE MODIFIÉE : FENÊTRE DÉTAIL MODERNE ---

    // Dans AffichageController.java

    private void afficherDetailsFormation(Formation f) {
        try {
            // CORRECTION DU NOM : details-view.fxml au lieu de Details.fxml
            URL resource = obtenirResource("/tn/jobnest/gformation/view/details-view.fxml");

            if (resource == null) {
                System.err.println("Erreur : Le fichier details-view.fxml est introuvable au chemin spécifié.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();

            DetailsController controller = loader.getController();
            controller.setFormation(f);

            Stage stage = new Stage();
            stage.setTitle("Fiche Formation : " + f.getTitre());

            // Optionnel : On rend la fenêtre transparente sur les bords pour le radius
            Scene scene = new Scene(root);
            scene.setFill(null); // Permet d'avoir des coins arrondis si le FXML le définit
            stage.setScene(scene);

            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Erreur de chargement de la vue : " + e.getMessage());
            alert.show();
        }
    }

    // --- AUTRES MÉTHODES ---

    private void ouvrirGestionModules(Formation f) {
        try {
            URL resource = obtenirResource("/tn/jobnest/gformation/view/gestion_modules.fxml");
            if (resource == null) throw new IOException("Fichier modules introuvable.");
            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();
            GestionModulesController controller = loader.getController();
            controller.setFormationActive(f);
            mainContent.getChildren().setAll(root);
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML void ouvrirAjout(ActionEvent event) { chargerVueModal("/tn/jobnest/gformation/view/ajout-view.fxml", "Ajouter une Session", null); }
    private void ouvrirModifier(Formation f) { chargerVueModal("/tn/jobnest/gformation/view/ajout-view.fxml", "Modifier la Session", f); }

    private void chargerVueModal(String path, String title, Formation f) {
        try {
            URL resource = obtenirResource(path);
            if (resource == null) return;
            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();
            AjoutController controller = loader.getController();
            controller.setOnRefreshCallback(this::chargerCards);
            if (f != null) controller.chargerDonnees(f);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setTitle(title);
            stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    private URL obtenirResource(String path) {
        URL res = getClass().getResource(path);
        if (res == null) {
            String fallback = path.substring(path.lastIndexOf("/") + 1);
            res = getClass().getResource("/view/" + fallback);
        }
        return res;
    }

    private void confirmerSuppression(Formation f) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer " + f.getTitre() + " ?", ButtonType.YES, ButtonType.NO);
        if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            sf.supprimer(f.getId_formation());
            chargerCards();
        }
    }
}