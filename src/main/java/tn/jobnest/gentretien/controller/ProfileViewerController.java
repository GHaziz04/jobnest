package tn.jobnest.gentretien.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import tn.jobnest.gentretien.model.Candidat;
import tn.jobnest.gentretien.service.CandidatService;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ProfileViewerController {

    @FXML private Label lblNomComplet, lblRole, lblEmailCard, lblTelCard, lblLocCard;
    @FXML private TextField txtPrenom, txtNom, txtEmail, txtTel, txtAdresse;
    @FXML private DatePicker dpDateNaissance;
    @FXML private Circle circleAvatar;

    private final CandidatService service = new CandidatService();

    public void setIdCandidat(int id) {
        Candidat c = service.getCandidatById(id);

        if (c != null) {
            // Remplissage de la carte latérale
            lblNomComplet.setText(c.getNomComplet());
            lblRole.setText(c.getTitrePro() != null ? c.getTitrePro() : "Candidat");
            lblEmailCard.setText(c.getEmail());
            lblTelCard.setText(c.getTelephone());
            lblLocCard.setText(c.getVille());

            // Remplissage des champs détaillés
            txtPrenom.setText(c.getPrenom());
            txtNom.setText(c.getNom());
            txtEmail.setText(c.getEmail());
            txtTel.setText(c.getTelephone());
            txtAdresse.setText(c.getVille());

            // Gestion de la date de naissance
            if (c.getDateNaissance() != null && !c.getDateNaissance().isEmpty()) {
                try {
                    // Adapter le format dd/MM/yyyy ou yyyy-MM-dd selon votre BDD
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                    dpDateNaissance.setValue(LocalDate.parse(c.getDateNaissance(), formatter));
                } catch (Exception e) {
                    System.err.println("Erreur format date : " + c.getDateNaissance());
                }
            }

            // Gestion de la photo de profil
            if (c.getImagePath() != null && !c.getImagePath().isEmpty()) {
                File file = new File(c.getImagePath());
                if (file.exists()) {
                    circleAvatar.setFill(new ImagePattern(new Image(file.toURI().toString())));
                }
            }
        } else {
            System.err.println("Aucun candidat trouvé pour l'ID : " + id);
        }
    }
}