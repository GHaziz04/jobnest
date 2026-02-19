package tn.jobnest.gformation.Controller;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.jobnest.gformation.model.Formation;
import tn.jobnest.gformation.model.Ressource;
import tn.jobnest.gformation.services.ServiceModule;
import tn.jobnest.gformation.services.ServiceRessource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;

public class GestionModulesController {

    @FXML private Label lblNomFormation, lblPourcentage;
    @FXML private VBox containerModules, formContainer, paneFilePicker, paneTextArea;
    @FXML private ScrollPane scrollModules;
    @FXML private TextField txtTitre, txtOrdre, txtPath;
    @FXML private TextArea txtDescription, txtContenuTexte;
    @FXML private ComboBox<String> comboType;
    @FXML private ProgressBar progressFormation;
    @FXML private Button btnAjouter;

    private final ServiceModule service = new ServiceModule();
    private final ServiceRessource ressourceService = new ServiceRessource();
    private Formation formationActive;
    private tn.jobnest.gformation.model.Module moduleSelectionne = null;
    private boolean isEditMode = false;

    @FXML
    public void initialize() {
        if (comboType != null) {
            comboType.setItems(FXCollections.observableArrayList("TEXTE", "PDF", "VIDEO"));
            comboType.setValue("TEXTE");
            comboType.valueProperty().addListener((obs, oldVal, newVal) -> toggleContenuInput(newVal));
        }
        if (scrollModules != null) scrollModules.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        if (progressFormation != null) {
            progressFormation.setStyle("-fx-accent: #38A169;");
        }
        if (lblPourcentage != null) {
            lblPourcentage.setStyle("-fx-text-fill: #38A169;");
        }

        toggleContenuInput("TEXTE");
        handleCloseForm();
    }

    private void toggleContenuInput(String type) {
        boolean isFile = "PDF".equals(type) || "VIDEO".equals(type);
        if (paneFilePicker != null) {
            paneFilePicker.setVisible(isFile);
            paneFilePicker.setManaged(isFile);
        }
        if (paneTextArea != null) {
            paneTextArea.setVisible("TEXTE".equals(type));
            paneTextArea.setManaged("TEXTE".equals(type));
        }
    }

    public void setFormationActive(Formation f) {
        this.formationActive = f;
        if (lblNomFormation != null && f != null) lblNomFormation.setText("Formation : " + f.getTitre());
        chargerModules();
    }

    private void chargerModules() {
        if (containerModules == null || formationActive == null) return;
        containerModules.getChildren().clear();
        List<tn.jobnest.gformation.model.Module> modules = service.getModulesParFormation(formationActive.getId_formation());

        if (modules != null) {
            modules.sort(Comparator.comparingInt(tn.jobnest.gformation.model.Module::getOrdre));
            for (tn.jobnest.gformation.model.Module m : modules) {
                containerModules.getChildren().add(creerItemModule(m));
            }
            updateProgression(modules);
        }
    }

    @FXML
    void handleAjouter() {
        if (formationActive == null) return;
        String titre = txtTitre.getText().trim();
        String type = comboType.getValue();
        String description = txtDescription.getText();
        String valeurSource = "TEXTE".equals(type) ? txtContenuTexte.getText() : txtPath.getText();

        if (titre.isEmpty() || valeurSource.isEmpty()) {
            afficherAlerte("Erreur", "Veuillez remplir le titre et le contenu.");
            return;
        }

        // --- PARTIE CORRIGÉE : VALIDATION DU NOMBRE (ORDRE) ---
        int ordre;
        try {
            String ordreStr = txtOrdre.getText().trim();
            ordre = ordreStr.isEmpty() ? 1 : Integer.parseInt(ordreStr);
        } catch (NumberFormatException e) {
            afficherAlerte("Format Invalide", "Le champ 'Ordre' doit être un nombre entier.");
            return;
        }

        try {
            String valeurFinale = valeurSource;
            if (!"TEXTE".equals(type) && new File(valeurSource).exists()) {
                File source = new File(valeurSource);
                File destDir = new File("uploads");
                if (!destDir.exists()) destDir.mkdirs();
                File destination = new File(destDir, source.getName());
                Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
                valeurFinale = "uploads/" + source.getName();
            }

            if (isEditMode && moduleSelectionne != null) {
                moduleSelectionne.setTitre(titre);
                moduleSelectionne.setOrdre(ordre); // Utilise la variable ordre validée
                moduleSelectionne.setDescription(description);
                service.modifier(moduleSelectionne);
                afficherAlerte("Succès", "Module mis à jour !");
            } else if (moduleSelectionne != null) {
                int prochainOrdre = ressourceService.getRessourcesParModule(moduleSelectionne.getId_module()).size() + 1;
                ressourceService.ajouter(new Ressource(0, moduleSelectionne.getId_module(), titre, type, valeurFinale, prochainOrdre));
                afficherAlerte("Succès", "Ressource ajoutée !");
            } else {
                int idMod = service.ajouter(new tn.jobnest.gformation.model.Module(0, formationActive.getId_formation(), titre, ordre, description, type));
                if (idMod != -1) {
                    ressourceService.ajouter(new Ressource(0, idMod, "Support Initial : " + titre, type, valeurFinale, 1));
                    afficherAlerte("Succès", "Module et ressource créés !");
                }
            }
            chargerModules();
            handleCloseForm();
        } catch (Exception e) {
            e.printStackTrace();
            afficherAlerte("Erreur", "Une erreur est survenue lors de l'enregistrement.");
        }
    }

    private VBox creerItemModule(tn.jobnest.gformation.model.Module m) {
        VBox moduleCard = new VBox(0);
        String[] colors = {"#3182CE", "#38A169", "#805AD5", "#DD6B20", "#E53E3E"};
        String color = colors[m.getOrdre() % colors.length];

        moduleCard.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 15, 0, 0, 5); " +
                "-fx-border-color: " + color + "; -fx-border-width: 0 0 0 5;");
        VBox.setMargin(moduleCard, new Insets(0, 0, 15, 0));

        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(15, 20, 15, 20));

        Label badge = new Label(String.valueOf(m.getOrdre()));
        badge.setStyle("-fx-background-color: " + color + "22; -fx-text-fill: " + color + "; " +
                "-fx-font-weight: bold; -fx-min-width: 32; -fx-min-height: 32; " +
                "-fx-background-radius: 8; -fx-alignment: center;");

        VBox info = new VBox(2);
        Label lblTitre = new Label(m.getTitre());
        lblTitre.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #1A202C;");
        Label lblDesc = new Label(m.getDescription());
        lblDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: #718096;");
        info.getChildren().addAll(lblTitre, lblDesc);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnEdit = new Button("✎");
        btnEdit.setStyle("-fx-background-color: #EDF2F7; -fx-text-fill: #4A5568; -fx-cursor: hand; -fx-background-radius: 6;");
        btnEdit.setOnAction(e -> preparerModification(m));

        Button btnDel = new Button("✕");
        btnDel.setStyle("-fx-background-color: #FFF5F5; -fx-text-fill: #E53E3E; -fx-cursor: hand; -fx-background-radius: 6;");
        btnDel.setOnAction(e -> supprimerModule(m));

        Button btnAddRes = new Button("＋ Ressource");
        btnAddRes.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: bold; -fx-cursor: hand;");
        btnAddRes.setOnAction(e -> {
            this.moduleSelectionne = m;
            this.isEditMode = false;
            btnAjouter.setText("Ajouter la Ressource");
            handleOpenForm();
        });

        actions.getChildren().addAll(btnEdit, btnDel, btnAddRes);
        header.getChildren().addAll(badge, info, spacer, actions);

        FlowPane resourcesFlow = new FlowPane(10, 10);
        resourcesFlow.setPadding(new Insets(0, 20, 20, 67));

        List<Ressource> ressources = ressourceService.getRessourcesParModule(m.getId_module());
        if (ressources != null) {
            for (Ressource r : ressources) {
                Label tag = creerResourceTag(r, m);
                resourcesFlow.getChildren().add(tag);
            }
        }

        moduleCard.getChildren().addAll(header, resourcesFlow);
        return moduleCard;
    }

    private Label creerResourceTag(Ressource r, tn.jobnest.gformation.model.Module m) {
        String icon = "📄";
        String bgColor = "#EDF2F7";
        String textColor = "#4A5568";

        if ("TEXTE".equals(r.getType())) { icon = "📝"; bgColor = "#EBF8FF"; textColor = "#2B6CB0"; }
        else if ("PDF".equals(r.getType())) { icon = "📕"; bgColor = "#FFF5F5"; textColor = "#C53030"; }
        else if ("VIDEO".equals(r.getType())) { icon = "▶️"; bgColor = "#F0FFF4"; textColor = "#2F855A"; }
        else if ("QUIZ".equals(r.getType())) { icon = "✨"; bgColor = "#FAF5FF"; textColor = "#6B46C1"; }

        Label tag = new Label(icon + " " + r.getTitre());
        tag.setStyle("-fx-background-color: " + bgColor + "; -fx-text-fill: " + textColor + "; " +
                "-fx-padding: 6 12; -fx-background-radius: 20; -fx-font-size: 11px; " +
                "-fx-font-weight: bold; -fx-cursor: hand; -fx-border-color: derive(" + bgColor + ", -10%); -fx-border-radius: 20;");

        tag.setOnMouseClicked(e -> ouvrirLiseuse(m, r, tag));
        return tag;
    }

    private void updateProgression(List<tn.jobnest.gformation.model.Module> modules) {
        if (modules == null || modules.isEmpty()) {
            progressFormation.setProgress(0);
            lblPourcentage.setText("0%");
            return;
        }
        double objectifTotal = 5.0;
        int modulesAvecContenu = 0;
        for (tn.jobnest.gformation.model.Module m : modules) {
            List<Ressource> res = ressourceService.getRessourcesParModule(m.getId_module());
            if (res != null && !res.isEmpty()) {
                modulesAvecContenu++;
            }
        }
        double p = (double) modulesAvecContenu / objectifTotal;
        if (p > 1.0) p = 1.0;
        if (progressFormation != null) progressFormation.setProgress(p);
        if (lblPourcentage != null) lblPourcentage.setText((int) (p * 100) + "%");
    }

    private void preparerModification(tn.jobnest.gformation.model.Module m) {
        this.moduleSelectionne = m;
        this.isEditMode = true;
        txtTitre.setText(m.getTitre());
        txtOrdre.setText(String.valueOf(m.getOrdre()));
        txtDescription.setText(m.getDescription());
        btnAjouter.setText("Modifier le Module");
        handleOpenForm();
    }

    private void supprimerModule(tn.jobnest.gformation.model.Module m) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer le module '" + m.getTitre() + "' ?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.YES) {
                service.supprimer(m.getId_module());
                chargerModules();
            }
        });
    }

    private void ouvrirLiseuse(tn.jobnest.gformation.model.Module m, Ressource r, Node sourceNode) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/details_module.fxml"));
            Parent root = loader.load();
            DetailsModuleController controller = loader.getController();
            controller.initData(m, this.formationActive);
            Stage stage = (Stage) sourceNode.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
            afficherAlerte("Erreur", "Impossible de charger la liseuse.");
        }
    }

    @FXML private void handleBrowse() {
        FileChooser fc = new FileChooser();
        File selected = fc.showOpenDialog(txtTitre.getScene().getWindow());
        if (selected != null) txtPath.setText(selected.getAbsolutePath());
    }

    @FXML private void handleOpenForm() { formContainer.setVisible(true); formContainer.setManaged(true); }
    @FXML private void handleCloseForm() { formContainer.setVisible(false); formContainer.setManaged(false); viderChamps(); }

    private void viderChamps() {
        txtTitre.clear(); txtOrdre.clear(); txtDescription.clear(); txtPath.clear(); txtContenuTexte.clear();
        btnAjouter.setText("＋ Créer le module");
        moduleSelectionne = null;
        isEditMode = false;
    }

    private void afficherAlerte(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titre); alert.setHeaderText(null); alert.setContentText(message); alert.showAndWait();
    }

    @FXML
    private void handleRetour() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/hello-view.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) lblNomFormation.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) { e.printStackTrace(); }
    }
}