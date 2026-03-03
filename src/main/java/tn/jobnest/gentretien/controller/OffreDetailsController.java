package tn.jobnest.gentretien.controller;

import tn.jobnest.gentretien.model.OffreEmploi;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class OffreDetailsController {

    @FXML private Label titreLabel;
    @FXML private Label entrepriseLabel;
    @FXML private Label contratLabel;
    @FXML private Label experienceLabel;
    @FXML private Label salaireLabel;
    @FXML private Label postesLabel;
    @FXML private Label publicationLabel;
    @FXML private Label expirationLabel;
    @FXML private Label descriptionLabel;

    public void setOffre(OffreEmploi o) {

        if (o == null) return;

        titreLabel.setText(val(o.getTitre()));
        entrepriseLabel.setText(val(o.getEntreprise()));
        contratLabel.setText(val(o.getTypeContrat()));
        experienceLabel.setText(val(o.getNiveauExperience()));
        salaireLabel.setText(o.getSalaireMin() + " - " + o.getSalaireMax() + " DT");
        postesLabel.setText(String.valueOf(o.getNbPostes()));
        publicationLabel.setText(o.getDatePublication() != null ? o.getDatePublication().toString() : "—");
        expirationLabel.setText(o.getDateExpiration()  != null ? o.getDateExpiration().toString()  : "—");
        descriptionLabel.setText(val(o.getDescription()));
    }

    private String val(String s) { return s != null ? s : ""; }

    @FXML
    private void closePopup() {
        ((Stage) titreLabel.getScene().getWindow()).close();
    }
}