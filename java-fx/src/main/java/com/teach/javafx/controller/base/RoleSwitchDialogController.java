package com.teach.javafx.controller.base;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class RoleSwitchDialogController {
    @FXML
    private Label titleLabel;
    @FXML
    private Label messageLabel;
    @FXML
    private Button confirmButton;
    @FXML
    private Button cancelButton;

    private boolean confirmed = false;
    private Runnable onConfirmAction;

    @FXML
    public void initialize() {
    }

    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    public void setMessage(String message) {
        messageLabel.setText(message);
    }

    public void setOnConfirm(Runnable action) {
        this.onConfirmAction = action;
    }

    @FXML
    private void handleConfirm() {
        confirmed = true;
        if (onConfirmAction != null) {
            onConfirmAction.run();
        }
        closeDialog();
    }

    @FXML
    private void handleCancel() {
        confirmed = false;
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}
