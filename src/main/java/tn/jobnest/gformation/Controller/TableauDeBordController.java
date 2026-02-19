package tn.jobnest.gformation.Controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import tn.jobnest.gformation.model.Formation;
import tn.jobnest.gformation.services.ServiceFormation;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TableauDeBordController {

    @FXML private Label lblFormationsRecentes, lblSessionsAVenir;
    @FXML private Label lblMoisAnnee; // Assure-toi d'ajouter ce fx:id dans ton FXML
    @FXML private LineChart<String, Number> lineChartEvolution;
    @FXML private GridPane calendarGrid;

    private final ServiceFormation service = new ServiceFormation();
    private final int currentUserId = 1;

    // --- NOUVEAU : Pivot pour la navigation réelle ---
    private LocalDate datePivot = LocalDate.now();

    @FXML
    public void initialize() {
        refreshAll();
    }

    @FXML
    void rafraichirDonnees(ActionEvent event) {
        datePivot = LocalDate.now(); // Reset à aujourd'hui si on rafraîchit
        refreshAll();
    }

    // --- NOUVEAU : Méthodes de navigation ---
    @FXML
    void moisSuivant(ActionEvent event) {
        datePivot = datePivot.plusMonths(1);
        genererCalendrier();
    }

    @FXML
    void moisPrecedent(ActionEvent event) {
        datePivot = datePivot.minusMonths(1);
        genererCalendrier();
    }

    private void refreshAll() {
        if (lineChartEvolution != null) chargerCourbe();
        if (calendarGrid != null) genererCalendrier();
    }

    private void chargerCourbe() {
        lineChartEvolution.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Formations créées");

        Map<String, Integer> stats = service.getStatsCreationsTroisMois();
        if (stats.isEmpty()) {
            series.getData().add(new XYChart.Data<>("Aucune donnée", 0));
        } else {
            for (Map.Entry<String, Integer> entry : stats.entrySet()) {
                series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
            }
        }
        lineChartEvolution.getData().add(series);

        if (lblFormationsRecentes != null) {
            lblFormationsRecentes.setText(String.valueOf(service.getTotalFormations()));
        }
    }

    private void genererCalendrier() {
        calendarGrid.getChildren().clear();

        // 1. Mise à jour du Titre (Ex: Février 2026)
        if (lblMoisAnnee != null) {
            String moisNom = datePivot.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH);
            lblMoisAnnee.setText(moisNom.toUpperCase() + " " + datePivot.getYear());
        }

        // 2. Ajout des en-têtes (Lun, Mar...)
        String[] jours = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};
        for (int i = 0; i < jours.length; i++) {
            Label lbl = new Label(jours[i]);
            lbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #718096;");
            calendarGrid.add(lbl, i, 0);
        }

        // 3. Calcul de la logique temporelle
        int joursDansMois = datePivot.lengthOfMonth();
        LocalDate premierDuMois = datePivot.withDayOfMonth(1);
        int decalage = premierDuMois.getDayOfWeek().getValue() - 1;

        List<Formation> mesFormations = service.getHistoriqueFormateur(currentUserId);

        // Mettre à jour le badge des sessions pour le mois affiché
        long countMois = mesFormations.stream().filter(f -> {
            if (f.getDate_debut() == null) return false;
            LocalDate d = f.getDate_debut().toLocalDate();
            return d.getMonth() == datePivot.getMonth() && d.getYear() == datePivot.getYear();
        }).count();

        if (lblSessionsAVenir != null) {
            lblSessionsAVenir.setText(String.valueOf(countMois));
        }

        // 4. Remplissage de la grille
        int ligne = 1;
        for (int j = 1; j <= joursDansMois; j++) {
            int col = (j + decalage - 1) % 7;

            VBox caseJ = new VBox();
            caseJ.setPrefSize(45, 45);
            caseJ.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-alignment: center; -fx-border-color: #EDF2F7;");

            Label lblNum = new Label(String.valueOf(j));
            caseJ.getChildren().add(lblNum);

            final int jourFinal = j;
            boolean aFormation = mesFormations.stream().anyMatch(f -> {
                if (f.getDate_debut() == null) return false;
                LocalDate d = f.getDate_debut().toLocalDate();
                return d.getDayOfMonth() == jourFinal &&
                        d.getMonth() == datePivot.getMonth() &&
                        d.getYear() == datePivot.getYear();
            });

            if (aFormation) {
                caseJ.setStyle("-fx-background-color: #3182CE; -fx-background-radius: 8; -fx-cursor: hand;");
                lblNum.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
                Tooltip.install(caseJ, new Tooltip("Session prévue"));
            }

            calendarGrid.add(caseJ, col, ligne);
            if (col == 6) ligne++;
        }
    }
}