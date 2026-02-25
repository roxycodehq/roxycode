package org.roxycode.gui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import io.micronaut.context.annotation.Prototype;

@Prototype
public class MainController {

    @FXML
    private Label messageLabel;

    public void initialize() {
        messageLabel.setText("Hello from Micronaut-managed Controller!");
    }
}