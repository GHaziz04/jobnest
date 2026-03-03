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

import java.sql.Date;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class OffreFormController {

    // ================= FXML =================
    @FXML private TextField  titreField;
    @FXML private TextField  entrepriseField;
    @FXML private ComboBox<String> contratBox;
    @FXML private TextField  salaireField;
    @FXML private TextArea   descriptionField;
    @FXML private ListView<Competence> competencesList;
    @FXML private ListView<Experience> experiencesList;

    // ================= SERVICES =================
    private final CompetenceService   competenceService  = new CompetenceService();
    private final ExperienceService   experienceService  = new ExperienceService();
    private final HuggingFaceAIService aiService         = new HuggingFaceAIService();
    private final AutoSkillService    autoSkillService   = new AutoSkillService();

    // ================= STATE =================
    private OffreEmploi offre;

    // ================= INITIALIZE =================
    @FXML
    public void initialize() {

        contratBox.setItems(FXCollections.observableArrayList(
                "CDI", "CDD", "Stage", "Freelance"
        ));

        // Validation salaire : chiffres uniquement
        salaireField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue.matches("\\d*(\\.\\d*)?")) {
                salaireField.setText(oldValue);
            }
        });

        loadCompetences();
        loadExperiences();
    }

    // ================= AI SUGGEST SKILLS =================
    @FXML
    private void suggestSkillsAI() {

        String description = descriptionField.getText();
        if (description == null || description.trim().isEmpty()) return;

        // Auto-création compétences depuis description
        autoSkillService.processDescription(description);

        // Recharger la liste depuis DB
        loadCompetences();
        competencesList.refresh();

        // Extraction AI
        List<String> aiSkills = aiService.extractSkills(description);
        if (aiSkills == null) aiSkills = new ArrayList<>();

        System.out.println("AI Skills Detected: " + aiSkills);

        // Nettoyage skills
        Set<String> cleanedSkills = new HashSet<>();
        for (String skill : aiSkills) {
            if (skill == null) continue;
            String s = skill.toLowerCase().trim().replaceAll("[^a-zA-Z0-9+ ]", "");
            if (s.isEmpty()) continue;
            cleanedSkills.add(s);
            for (String p : s.split("\\s+")) {
                if (p.length() > 2) cleanedSkills.add(p);
            }
        }

        System.out.println("CLEANED SKILLS = " + cleanedSkills);

        // Reset sélection
        for (Competence c : competencesList.getItems()) c.setSelected(false);

        // Matching compétences
        for (Competence c : competencesList.getItems()) {
            if (c.getNom() == null) continue;
            String dbName = c.getNom().toLowerCase().trim();
            for (String skill : cleanedSkills) {
                if (dbName.equals(skill) || dbName.contains(skill) || skill.contains(dbName)) {
                    c.setSelected(true);
                    System.out.println("MATCHED: " + dbName + " <--> " + skill);
                    break;
                }
            }
        }
        competencesList.refresh();

        // Suggestion expérience
        boolean yearsFound = suggestExperienceFromDescription(description);

        if (!yearsFound) {
            Experience bestExperience = null;
            double bestScore = 0;

            for (Experience exp : experiencesList.getItems()) {
                double score = calculateMatchingScore(new ArrayList<>(cleanedSkills), exp);
                System.out.println("Score for " + exp.getNom() + " = " + score);
                if (score > bestScore) {
                    bestScore = score;
                    bestExperience = exp;
                }
            }

            if (bestExperience != null && bestScore >= 50) {
                for (Experience exp : experiencesList.getItems()) exp.setSelected(false);
                bestExperience.setSelected(true);
                System.out.println("Best Experience (AI): " + bestExperience.getNom() + " | Score = " + bestScore);
            }
        }

        experiencesList.refresh();
    }

    // ================= MATCHING SCORE =================
    private double calculateMatchingScore(List<String> offerSkills, Experience experience) {

        if (offerSkills == null || offerSkills.isEmpty() || experience == null) return 0;

        String expText = (experience.getNom() + " " + experience.getDescription())
                .toLowerCase().replaceAll("[^a-z0-9+#. ]", " ");

        Set<String> expandedSkills = new HashSet<>();
        for (String skill : offerSkills) {
            if (skill == null) continue;
            String cleaned = skill.trim().toLowerCase();
            if (cleaned.length() < 2) continue;
            expandedSkills.add(cleaned);
            for (String part : cleaned.split("\\s+")) {
                if (part.length() > 2) expandedSkills.add(part);
            }
        }

        int matchCount = 0;
        for (String skill : expandedSkills) {
            if (expText.contains(skill)) matchCount++;
        }

        if (expandedSkills.isEmpty()) return 0;
        return Math.round((matchCount * 100.0 / expandedSkills.size()) * 10.0) / 10.0;
    }

    // ================= SUGGEST EXPERIENCE FROM YEARS =================
    private boolean suggestExperienceFromDescription(String description) {

        Pattern pattern = Pattern.compile("(\\d+)(?:\\s*[-+]\\s*(\\d+))?\\s*(an|ans|ann\u00e9e|ann\u00e9es)");
        Matcher matcher = pattern.matcher(description.toLowerCase());

        if (!matcher.find()) return false;

        int years;
        if (matcher.group(2) != null) {
            years = (Integer.parseInt(matcher.group(1)) + Integer.parseInt(matcher.group(2))) / 2;
        } else {
            years = Integer.parseInt(matcher.group(1));
        }

        System.out.println("YEARS DETECTED = " + years);

        for (Experience exp : experiencesList.getItems()) exp.setSelected(false);

        Experience bestMatch = null;
        int smallestDifference = Integer.MAX_VALUE;

        for (Experience exp : experiencesList.getItems()) {
            String text = (exp.getNom() + " " + exp.getDescription()).toLowerCase();
            Pattern expPattern = Pattern.compile("(\\d+)\\s*(an|ans)");
            Matcher expMatcher = expPattern.matcher(text);
            if (expMatcher.find()) {
                int diff = Math.abs(Integer.parseInt(expMatcher.group(1)) - years);
                if (diff < smallestDifference) {
                    smallestDifference = diff;
                    bestMatch = exp;
                }
            }
        }

        if (bestMatch == null) {
            for (Experience exp : experiencesList.getItems()) {
                String niveau = exp.getNiveauRequis() != null ? exp.getNiveauRequis().toLowerCase() : "";
                if      (years <= 1 && niveau.contains("debut"))  { bestMatch = exp; break; }
                else if (years <= 3 && niveau.contains("inter"))  { bestMatch = exp; break; }
                else if (years >= 4 && niveau.contains("expert")) { bestMatch = exp; break; }
            }
        }

        if (bestMatch != null) {
            bestMatch.setSelected(true);
            System.out.println("Best Experience (YEARS): " + bestMatch.getNom());
            return true;
        }

        return false;
    }

    // ================= LOAD COMPETENCES =================
    private void loadCompetences() {
        try {
            ObservableList<Competence> list =
                    FXCollections.observableArrayList(competenceService.getAll());

            competencesList.setItems(list);
            competencesList.setCellFactory(
                    CheckBoxListCell.forListView(
                            Competence::selectedProperty,
                            new StringConverter<Competence>() {
                                @Override public String toString(Competence c) {
                                    return c.getNom() + " (" + c.getCategorie() + ")";
                                }
                                @Override public Competence fromString(String s) { return null; }
                            }
                    )
            );
        } catch (Exception e) {
            showError("Erreur chargement compétences : " + e.getMessage());
        }
    }

    // ================= LOAD EXPERIENCES =================
    private void loadExperiences() {
        try {
            ObservableList<Experience> list =
                    FXCollections.observableArrayList(experienceService.getAll());

            experiencesList.setItems(list);
            experiencesList.setCellFactory(
                    CheckBoxListCell.forListView(
                            Experience::selectedProperty,
                            new StringConverter<Experience>() {
                                @Override public String toString(Experience exp) {
                                    return exp.getNom() + " (" + exp.getNiveauRequis() + ")";
                                }
                                @Override public Experience fromString(String s) { return null; }
                            }
                    )
            );
        } catch (Exception e) {
            showError("Erreur chargement expériences : " + e.getMessage());
        }
    }

    // ================= ADD COMPETENCE POPUP =================
    @FXML
    private void handleAddCompetence() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/tn/jobnest/gentretien/competence_form.fxml"));
            Parent root = loader.load();
            CompetenceFormController controller = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("Nouvelle Compétence");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            Competence newCompetence = controller.getCompetence();
            if (newCompetence != null) {
                competencesList.getItems().add(newCompetence);
                newCompetence.setSelected(true);
                competencesList.refresh();
            }
        } catch (Exception e) {
            showError("Erreur ouverture popup compétence : " + e.getMessage());
        }
    }

    // ================= ADD EXPERIENCE POPUP =================
    @FXML
    private void handleAddExperience() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/tn/jobnest/gentretien/experience_form.fxml"));
            Parent root = loader.load();
            ExperienceFormController controller = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("Nouvelle Expérience");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            Experience newExp = controller.getExperience();
            if (newExp != null) {
                experienceService.addExperience(newExp);
                loadExperiences();
                for (Experience exp : experiencesList.getItems()) {
                    if (exp.getNom().equals(newExp.getNom())) exp.setSelected(true);
                }
                experiencesList.refresh();
            }
        } catch (Exception e) {
            showError("Erreur ouverture popup expérience : " + e.getMessage());
        }
    }

    // ================= SET OFFRE (EDIT MODE) =================
    public void setOffre(OffreEmploi o) {
        this.offre = o;
        if (o == null) return;

        titreField.setText(o.getTitre());
        entrepriseField.setText(o.getEntreprise());
        contratBox.setValue(o.getTypeContrat());
        descriptionField.setText(o.getDescription());
        salaireField.setText(String.valueOf(o.getSalaireMin()));

        if (o.getCompetences() != null) {
            for (Competence c : competencesList.getItems()) {
                for (Competence sel : o.getCompetences()) {
                    if (c.getIdCompetence() == sel.getIdCompetence()) c.setSelected(true);
                }
            }
        }

        if (o.getExperiences() != null) {
            for (Experience exp : experiencesList.getItems()) {
                for (Experience sel : o.getExperiences()) {
                    if (exp.getIdExperience() == sel.getIdExperience()) exp.setSelected(true);
                }
            }
        }
    }

    // ================= GETTERS =================
    public OffreEmploi getOffre()                { return offre; }

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

    // ================= SAVE =================
    @FXML
    private void ajouter() {

        if (!validerFormulaire()) return;

        try {
            if (offre == null) {
                offre = new OffreEmploi();
                offre.setIdRecruteur(1);
                offre.setStatut("publiee");
                offre.setDatePublication(Date.valueOf(LocalDate.now()));
                offre.setDateExpiration(Date.valueOf(LocalDate.now().plusDays(30)));
            }

            double salaire = Double.parseDouble(salaireField.getText());

            offre.setTitre(titreField.getText().trim());
            offre.setEntreprise(entrepriseField.getText().trim());
            offre.setTypeContrat(contratBox.getValue());
            offre.setDescription(descriptionField.getText().trim());
            offre.setSalaireMin(salaire);
            offre.setSalaireMax(salaire);
            offre.setCompetences(getSelectedCompetences());
            offre.setExperiences(getSelectedExperiences());

            // Calcul du matching score
            List<String> aiSkills = aiService.extractSkills(descriptionField.getText());
            if (aiSkills == null) aiSkills = new ArrayList<>();

            double bestScore = 0;
            for (Experience exp : getSelectedExperiences()) {
                double score = calculateMatchingScore(aiSkills, exp);
                if (score > bestScore) bestScore = score;
            }

            System.out.println("FINAL SCORE = " + bestScore);
            offre.setMatchingScore(bestScore);

            closePopup();

        } catch (Exception e) {
            showError("Erreur sauvegarde : " + e.getMessage());
        }
    }

    // ================= VALIDATION =================
    private boolean validerFormulaire() {
        StringBuilder errors = new StringBuilder();

        if (titreField.getText().isBlank())       errors.append("• Titre obligatoire\n");
        if (entrepriseField.getText().isBlank())  errors.append("• Entreprise obligatoire\n");
        if (contratBox.getValue() == null)        errors.append("• Contrat obligatoire\n");
        if (descriptionField.getText().isBlank()) errors.append("• Description obligatoire\n");
        if (salaireField.getText().isBlank())     errors.append("• Salaire obligatoire\n");
        if (getSelectedCompetences().isEmpty())   errors.append("• Sélectionner au moins une compétence\n");

        if (errors.length() > 0) {
            showError(errors.toString());
            return false;
        }
        return true;
    }

    // ================= UTILS =================
    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }

    @FXML
    private void closePopup() {
        ((Stage) titreField.getScene().getWindow()).close();
    }
}