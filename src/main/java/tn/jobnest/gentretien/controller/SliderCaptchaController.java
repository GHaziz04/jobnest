package tn.jobnest.gentretien.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Slider;
import javafx.stage.Stage;

public class SliderCaptchaController {

    @FXML private Slider slider;

    private boolean validated = false;

    public boolean isValidated() { return validated; }

    @FXML
    private void handleVerify() {
        if (slider.getValue() >= 99) {
            validated = true;
            ((Stage) slider.getScene().getWindow()).close();
        } else {
            new Alert(Alert.AlertType.ERROR, "Glissez complètement la barre ❌").showAndWait();
        }
    }
}