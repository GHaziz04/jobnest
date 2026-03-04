package tn.jobnest.gentretien.controller;

import tn.jobnest.gentretien.model.Competence;
import tn.jobnest.gentretien.model.Experience;
import tn.jobnest.gentretien.model.OffreEmploi;
import tn.jobnest.gentretien.service.CompetenceService;
import tn.jobnest.gentretien.service.ExperienceService;
import tn.jobnest.gentretien.service.HuggingFaceAIService;
import tn.jobnest.gentretien.service.AutoSkillService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.net.URL;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class OffreFormController {

    // ===== FXML =====
    @FXML private Label             headerLabel;
    @FXML private TextField         titreField;
    @FXML private TextField         entrepriseField;
    @FXML private ComboBox<String>  contratBox;
    @FXML private TextField         salaireMinField;
    @FXML private TextField         salaireMaxField;
    @FXML private TextField         nbPostesField;
    @FXML private DatePicker        dateExpirationPicker;
    @FXML private TextArea          descriptionField;
    @FXML private ListView<Competence> competencesList;
    @FXML private ListView<Experience> experiencesList;

    // ===== SERVICES =====
    private final CompetenceService    competenceService = new CompetenceService();
    private final ExperienceService    experienceService = new ExperienceService();
    private final HuggingFaceAIService aiService         = new HuggingFaceAIService();
    private final AutoSkillService     autoSkillService  = new AutoSkillService();

    private OffreEmploi offre;

    // ============================
    //  INITIALIZE
    // ============================
    @FXML
    public void initialize() {
        contratBox.setItems(FXCollections.observableArrayList(
                "CDI", "CDD", "Stage", "Alternance", "Freelance"));

        salaireMinField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d*)?")) salaireMinField.setText(oldVal);
        });
        salaireMaxField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d*)?")) salaireMaxField.setText(oldVal);
        });

        nbPostesField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) nbPostesField.setText(oldVal);
        });

        dateExpirationPicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now().plusDays(1)));
            }
        });

        chargerCompetences();
        chargerExperiences();
    }

    // ============================
    //  IA — SUGGESTION DE COMPÉTENCES
    // ============================
    @FXML
    private void suggestSkillsAI() {
        String description = descriptionField.getText();
        if (description == null || description.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING,
                    "Description requise",
                    "Veuillez saisir une description avant de suggérer des compétences.");
            return;
        }
        autoSkillService.processDescription(description);
        chargerCompetences();
        competencesList.refresh();

        List<String> aiSkills = aiService.extractSkills(description);
        if (aiSkills == null) aiSkills = new ArrayList<>();

        Set<String> cleanedSkills = new HashSet<>();
        for (String skill : aiSkills) {
            if (skill == null) continue;
            String s = skill.toLowerCase().trim().replaceAll("[^a-zA-Z0-9+ ]", "");
            if (s.isEmpty()) continue;
            cleanedSkills.add(s);
            for (String p : s.split("\\s+")) if (p.length() > 2) cleanedSkills.add(p);
        }

        for (Competence c : competencesList.getItems()) c.setSelected(false);
        for (Competence c : competencesList.getItems()) {
            if (c.getNom() == null) continue;
            String dbName = c.getNom().toLowerCase().trim();
            for (String skill : cleanedSkills) {
                if (dbName.equals(skill) || dbName.contains(skill) || skill.contains(dbName)) {
                    c.setSelected(true); break;
                }
            }
        }
        competencesList.refresh();

        boolean yearsFound = suggestExperienceFromDescription(description);
        if (!yearsFound) {
            Experience best = null; double bestScore = 0;
            for (Experience exp : experiencesList.getItems()) {
                double score = calculerScore(new ArrayList<>(cleanedSkills), exp);
                if (score > bestScore) { bestScore = score; best = exp; }
            }
            if (best != null && bestScore >= 50) {
                for (Experience e : experiencesList.getItems()) e.setSelected(false);
                best.setSelected(true);
            }
        }
        experiencesList.refresh();
    }

    private double calculerScore(List<String> skills, Experience exp) {
        if (skills == null || skills.isEmpty() || exp == null) return 0;
        String txt = (exp.getNom() + " " + exp.getDescription())
                .toLowerCase().replaceAll("[^a-z0-9+#. ]", " ");
        Set<String> expanded = new HashSet<>();
        for (String s : skills) {
            if (s == null) continue;
            String c = s.trim().toLowerCase();
            if (c.length() < 2) continue;
            expanded.add(c);
            for (String p : c.split("\\s+")) if (p.length() > 2) expanded.add(p);
        }
        int matches = 0;
        for (String s : expanded) if (txt.contains(s)) matches++;
        return expanded.isEmpty() ? 0 : Math.round((matches * 100.0 / expanded.size()) * 10.0) / 10.0;
    }

    private boolean suggestExperienceFromDescription(String description) {
        Pattern pat = Pattern.compile("(\\d+)(?:\\s*[-+]\\s*(\\d+))?\\s*(an|ans|année|années)");
        Matcher mat = pat.matcher(description.toLowerCase());
        if (!mat.find()) return false;
        int years = mat.group(2) != null
                ? (Integer.parseInt(mat.group(1)) + Integer.parseInt(mat.group(2))) / 2
                : Integer.parseInt(mat.group(1));
        for (Experience e : experiencesList.getItems()) e.setSelected(false);
        Experience best = null; int minDiff = Integer.MAX_VALUE;
        for (Experience exp : experiencesList.getItems()) {
            String txt = (exp.getNom() + " " + exp.getDescription()).toLowerCase();
            Matcher m2 = Pattern.compile("(\\d+)\\s*(an|ans)").matcher(txt);
            if (m2.find()) {
                int diff = Math.abs(Integer.parseInt(m2.group(1)) - years);
                if (diff < minDiff) { minDiff = diff; best = exp; }
            }
        }
        if (best == null) {
            for (Experience exp : experiencesList.getItems()) {
                String niveau = exp.getNiveauRequis() != null ? exp.getNiveauRequis().toLowerCase() : "";
                if      (years <= 1 && niveau.contains("debut"))  { best = exp; break; }
                else if (years <= 3 && niveau.contains("inter"))  { best = exp; break; }
                else if (years >= 4 && niveau.contains("expert")) { best = exp; break; }
            }
        }
        if (best != null) { best.setSelected(true); return true; }
        return false;
    }

    // ============================
    //  CHARGEMENT LISTES
    // ============================
    private void chargerCompetences() {
        try {
            ObservableList<Competence> list =
                    FXCollections.observableArrayList(competenceService.getAll());
            competencesList.setItems(list);
            competencesList.setCellFactory(CheckBoxListCell.forListView(
                    Competence::selectedProperty,
                    new StringConverter<>() {
                        @Override public String toString(Competence c) {
                            return c.getNom() + " (" + c.getCategorie() + ")";
                        }
                        @Override public Competence fromString(String s) { return null; }
                    }));
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Erreur chargement compétences : " + e.getMessage());
        }
    }

    private void chargerExperiences() {
        try {
            ObservableList<Experience> list =
                    FXCollections.observableArrayList(experienceService.getAll());
            experiencesList.setItems(list);
            experiencesList.setCellFactory(CheckBoxListCell.forListView(
                    Experience::selectedProperty,
                    new StringConverter<>() {
                        @Override public String toString(Experience e) {
                            return e.getNom() + " (" + e.getNiveauRequis() + ")";
                        }
                        @Override public Experience fromString(String s) { return null; }
                    }));
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Erreur chargement expériences : " + e.getMessage());
        }
    }

    // ============================
    //  POPUP COMPÉTENCE
    // ============================
    @FXML
    private void handleAddCompetence() {
        try {
            URL fxmlUrl = getClass().getResource("/tn/jobnest/gentretien/competence_form.fxml");
            if (fxmlUrl == null) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "competence_form.fxml introuvable.");
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            CompetenceFormController ctrl = loader.getController();
            Stage stage = new Stage();
            stage.setTitle("Nouvelle Compétence");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            Competence nc = ctrl.getCompetence();
            if (nc != null) {
                competencesList.getItems().add(nc);
                nc.setSelected(true);
                competencesList.refresh();
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Erreur popup compétence : " + e.getMessage());
        }
    }

    // ============================
    //  POPUP EXPÉRIENCE
    // ============================
    @FXML
    private void handleAddExperience() {
        try {
            URL fxmlUrl = getClass().getResource("/tn/jobnest/gentretien/experience_form.fxml");
            if (fxmlUrl == null) {
                showAlert(Alert.AlertType.ERROR, "Erreur",
                        "Fichier experience_form.fxml introuvable.\n" +
                                "Vérifiez que le fichier est dans src/main/resources/tn/jobnest/gentretien/");
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            ExperienceFormController ctrl = loader.getController();
            Stage stage = new Stage();
            stage.setTitle("Nouvelle Expérience");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setMinWidth(450);
            stage.setMinHeight(420);
            stage.showAndWait();
            Experience newExp = ctrl.getExperience();
            if (newExp != null) {
                experienceService.addExperience(newExp);
                chargerExperiences();
                for (Experience exp : experiencesList.getItems()) {
                    if (exp.getNom() != null && exp.getNom().equals(newExp.getNom()))
                        exp.setSelected(true);
                }
                experiencesList.refresh();
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Erreur ouverture popup expérience : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ============================
    //  MODE ÉDITION
    // ============================
    public void setOffre(OffreEmploi o) {
        this.offre = o;
        if (o == null) return;

        if (headerLabel != null) headerLabel.setText(" Modifier l'Offre");

        titreField.setText(o.getTitre());
        entrepriseField.setText(o.getEntreprise());
        contratBox.setValue(o.getTypeContrat());
        descriptionField.setText(o.getDescription());

        salaireMinField.setText(o.getSalaireMin() > 0 ? String.valueOf((int) o.getSalaireMin()) : "");
        salaireMaxField.setText(o.getSalaireMax() > 0 ? String.valueOf((int) o.getSalaireMax()) : "");

        if (o.getNbPostes() > 0) nbPostesField.setText(String.valueOf(o.getNbPostes()));

        if (o.getDateExpiration() != null)
            dateExpirationPicker.setValue(o.getDateExpiration().toLocalDate());

        if (o.getCompetences() != null) {
            for (Competence c : competencesList.getItems())
                for (Competence sel : o.getCompetences())
                    if (c.getIdCompetence() == sel.getIdCompetence()) c.setSelected(true);
            competencesList.refresh();
        }
        if (o.getExperiences() != null) {
            for (Experience exp : experiencesList.getItems())
                for (Experience sel : o.getExperiences())
                    if (exp.getIdExperience() == sel.getIdExperience()) exp.setSelected(true);
            experiencesList.refresh();
        }
    }

    // ============================
    //  MODE REPUBLICATION
    // ============================
    public void setModeRepublication(boolean actif) {
        if (!actif) return;

        if (headerLabel != null)
            headerLabel.setText("🔄  Republier l'Offre");

        if (dateExpirationPicker != null) {
            dateExpirationPicker.setStyle(
                    "-fx-font-size:13px;" +
                            "-fx-border-color:#F97316;" +
                            "-fx-border-radius:9;" +
                            "-fx-background-radius:9;" +
                            "-fx-border-width:2;");
            dateExpirationPicker.setPromptText("⚠  Obligatoire : choisissez une nouvelle date limite");
        }

        javafx.application.Platform.runLater(() -> {
            if (titreField == null || titreField.getScene() == null) return;
            titreField.getScene().lookup("#btnPublier");
        });
    }

    // ============================
    //  GETTERS
    // ============================
    public OffreEmploi getOffre() { return offre; }

    public List<Competence> getSelectedCompetences() {
        return competencesList.getItems().stream()
                .filter(Competence::isSelected)
                .collect(Collectors.toList());
    }

    public List<Experience> getSelectedExperiences() {
        return experiencesList.getItems().stream()
                .filter(Experience::isSelected)
                .collect(Collectors.toList());
    }

    // ============================
    //  SAUVEGARDER
    // ============================
    @FXML
    private void ajouter() {
        if (!validerFormulaire()) return;
        try {
            if (offre == null) {
                offre = new OffreEmploi();
                offre.setIdRecruteur(1);
                offre.setStatut("publiee");
                offre.setDatePublication(Date.valueOf(LocalDate.now()));
            }

            double salaireMin = salaireMinField.getText().trim().isEmpty()
                    ? 0 : Double.parseDouble(salaireMinField.getText().trim());
            double salaireMax = salaireMaxField.getText().trim().isEmpty()
                    ? 0 : Double.parseDouble(salaireMaxField.getText().trim());
            int nbPostes = nbPostesField.getText().trim().isEmpty()
                    ? 0 : Integer.parseInt(nbPostesField.getText().trim());

            offre.setTitre(titreField.getText().trim());
            offre.setEntreprise(entrepriseField.getText().trim());
            offre.setTypeContrat(contratBox.getValue());
            offre.setDescription(descriptionField.getText().trim());
            offre.setSalaireMin(salaireMin);
            offre.setSalaireMax(salaireMax);
            offre.setNbPostes(nbPostes);

            if (dateExpirationPicker.getValue() != null)
                offre.setDateExpiration(Date.valueOf(dateExpirationPicker.getValue()));
            else
                offre.setDateExpiration(Date.valueOf(LocalDate.now().plusDays(30)));

            offre.setCompetences(getSelectedCompetences());
            offre.setExperiences(getSelectedExperiences());
            // ✅ setMatchingScore() supprimé — attribut retiré du modèle

            closePopup();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur sauvegarde", e.getMessage());
            e.printStackTrace();
        }
    }

    // ============================
    //  VALIDATION
    // ============================
    private boolean validerFormulaire() {
        StringBuilder errors = new StringBuilder();
        if (titreField.getText() == null || titreField.getText().isBlank())
            errors.append("• Titre obligatoire\n");
        if (entrepriseField.getText() == null || entrepriseField.getText().isBlank())
            errors.append("• Entreprise obligatoire\n");
        if (contratBox.getValue() == null)
            errors.append("• Type de contrat obligatoire\n");
        if (descriptionField.getText() == null || descriptionField.getText().isBlank())
            errors.append("• Description obligatoire\n");

        if (!salaireMinField.getText().trim().isEmpty() && !salaireMaxField.getText().trim().isEmpty()) {
            double min = Double.parseDouble(salaireMinField.getText().trim());
            double max = Double.parseDouble(salaireMaxField.getText().trim());
            if (max < min) errors.append("• Le salaire max doit être ≥ au salaire min\n");
        }

        if (dateExpirationPicker.getValue() == null)
            errors.append("• La date d'expiration est obligatoire\n");
        else if (!dateExpirationPicker.getValue().isAfter(LocalDate.now()))
            errors.append("• La date d'expiration doit être dans le futur\n");

        if (getSelectedCompetences().isEmpty())
            errors.append("• Sélectionnez au moins une compétence\n");

        if (errors.length() > 0) {
            showAlert(Alert.AlertType.ERROR, "Formulaire incomplet", errors.toString());
            return false;
        }
        return true;
    }

    // ============================
    //  UTILS
    // ============================
    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    @FXML
    private void closePopup() {
        if (titreField != null && titreField.getScene() != null)
            ((Stage) titreField.getScene().getWindow()).close();
    }
}