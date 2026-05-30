package com.teach.javafx.controller.base;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.teach.javafx.AppStore;
import com.teach.javafx.MainApplication;
import com.teach.javafx.request.HttpRequestUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

public class ProfileController {
    @FXML
    private Label usernameLabel;
    @FXML
    private Label roleLabel;
    @FXML
    private TextField nameField;
    @FXML
    private TextField phoneField;
    @FXML
    private PasswordField oldPasswordField;
    @FXML
    private PasswordField newPasswordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private Button oldPasswordToggleBtn;
    @FXML
    private Button newPasswordToggleBtn;
    @FXML
    private Button confirmPasswordToggleBtn;

    private boolean oldPasswordVisible = false;
    private boolean newPasswordVisible = false;
    private boolean confirmPasswordVisible = false;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    @FXML
    public void initialize() {
        loadProfile();
        clearPasswordFields();
        clearPhoneField();
    }

    private void clearPasswordFields() {
        if (oldPasswordField != null) oldPasswordField.clear();
        if (newPasswordField != null) newPasswordField.clear();
        if (confirmPasswordField != null) confirmPasswordField.clear();
    }

    private void clearPhoneField() {
        if (phoneField != null) phoneField.clear();
    }

    private void loadProfile() {
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(HttpRequestUtil.serverUrl + "/user/profile"))
                    .GET()
                    .headers("satoken", AppStore.getJwt().getTokenValue())
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, Object> result = gson.fromJson(response.body(), new TypeToken<Map<String, Object>>(){}.getType());
                if (result.get("code").equals(200.0)) {
                    Map<String, Object> data = (Map<String, Object>) result.get("data");

                    String username = (String) data.get("username");
                    String realName = (String) data.get("realName");

                    String name = (realName == null || realName.isEmpty()) ? username : realName;

                    usernameLabel.setText(username != null ? username : "未知用户");
                    roleLabel.setText((String) data.get("role"));
                    nameField.setText(name != null ? name : "");
                } else {
                    MessageDialog.showDialog("获取个人信息失败：" + result.get("msg"));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            MessageDialog.showDialog("获取个人信息异常：" + e.getMessage());
        }
    }


    @FXML
    protected void handleUpdateProfile() {
        String name = nameField.getText().trim();
        String phone = phoneField.getText().trim();

        if (name.isEmpty()) {
            MessageDialog.showDialog("请输入姓名");
            return;
        }

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("realName", name);
        requestBody.put("phone", phone);

        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(HttpRequestUtil.serverUrl + "/user/updateProfile"))
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                    .headers("Content-Type", "application/json", "satoken", AppStore.getJwt().getTokenValue())
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, Object> result = gson.fromJson(response.body(), new TypeToken<Map<String, Object>>(){}.getType());
                if (result.get("code").equals(200.0)) {
                    MessageDialog.showDialog("更新成功");
                    clearPasswordFields();
                    clearPhoneField();
                    loadProfile();
                } else {
                    MessageDialog.showDialog("更新失败：" + result.get("msg"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            MessageDialog.showDialog("更新异常：" + e.getMessage());
        }
    }

    @FXML
    protected void handleChangePassword() {
        String oldPassword = oldPasswordField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (oldPassword.isEmpty()) {
            MessageDialog.showDialog("请输入旧密码");
            return;
        }
        if (newPassword.isEmpty()) {
            MessageDialog.showDialog("请输入新密码");
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            MessageDialog.showDialog("两次输入的新密码不一致");
            return;
        }
        if (newPassword.length() < 6) {
            MessageDialog.showDialog("新密码长度不能少于6位");
            return;
        }

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("oldPassword", oldPassword);
        requestBody.put("newPassword", newPassword);

        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(HttpRequestUtil.serverUrl + "/user/changePassword"))
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                    .headers("Content-Type", "application/json", "satoken", AppStore.getJwt().getTokenValue())
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, Object> result = gson.fromJson(response.body(), new TypeToken<Map<String, Object>>(){}.getType());
                if (result.get("code").equals(200.0)) {
                    MessageDialog.showDialog("密码修改成功，请重新登录");
                    handleLogout();
                } else {
                    MessageDialog.showDialog("修改失败：" + result.get("msg"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            MessageDialog.showDialog("修改异常：" + e.getMessage());
        }
    }

    @FXML
    protected void handleLogout() {
        AppStore.setJwt(null);
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("base/login-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), -1, -1);
            MainApplication.resetStage("仓储管理系统 - 登录", scene);
        } catch (IOException e) {
            e.printStackTrace();
            MessageDialog.showDialog("退出登录失败：" + e.getMessage());
        }
    }

    @FXML
    protected void toggleOldPasswordVisibility() {
        oldPasswordVisible = !oldPasswordVisible;
        togglePasswordField(oldPasswordField, oldPasswordToggleBtn, oldPasswordVisible);
    }

    @FXML
    protected void toggleNewPasswordVisibility() {
        newPasswordVisible = !newPasswordVisible;
        togglePasswordField(newPasswordField, newPasswordToggleBtn, newPasswordVisible);
    }

    @FXML
    protected void toggleConfirmPasswordVisibility() {
        confirmPasswordVisible = !confirmPasswordVisible;
        togglePasswordField(confirmPasswordField, confirmPasswordToggleBtn, confirmPasswordVisible);
    }

    private void togglePasswordField(PasswordField passwordField, Button toggleBtn, boolean visible) {
        GridPane parent = (GridPane) passwordField.getParent();
        Integer rowIndex = GridPane.getRowIndex(passwordField);
        Integer columnIndex = GridPane.getColumnIndex(passwordField);
        int row = (rowIndex == null) ? 0 : rowIndex;
        int col = (columnIndex == null) ? 0 : columnIndex;

        String pwdText = passwordField.getText();
        String pwdStyle = passwordField.getStyle();
        String pwdId = passwordField.getId();
        String pwdPrompt = passwordField.getPromptText();

        parent.getChildren().remove(passwordField);

        if (visible) {
            TextField textField = new TextField();
            textField.setText(pwdText);
            textField.setId(pwdId);
            textField.setStyle(pwdStyle);
            textField.setPromptText(pwdPrompt);
            GridPane.setRowIndex(textField, row);
            GridPane.setColumnIndex(textField, col);
            parent.getChildren().add(textField);
            toggleBtn.setText("");
        } else {
            PasswordField newPwdField = new PasswordField();
            newPwdField.setText(pwdText);
            newPwdField.setId(pwdId);
            newPwdField.setStyle(pwdStyle);
            newPwdField.setPromptText(pwdPrompt);
            GridPane.setRowIndex(newPwdField, row);
            GridPane.setColumnIndex(newPwdField, col);
            parent.getChildren().add(newPwdField);
            toggleBtn.setText("👁");
        }
    }
}
