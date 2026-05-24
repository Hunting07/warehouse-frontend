package com.teach.javafx.controller.base;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.teach.javafx.MainApplication;
import com.teach.javafx.request.HttpRequestUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

public class RegisterController {
    @FXML
    private RadioButton staffRadio;
    @FXML
    private RadioButton adminRadio;
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;

    private ToggleGroup roleGroup;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    @FXML
    public void initialize() {
        roleGroup = new ToggleGroup();
        staffRadio.setToggleGroup(roleGroup);
        adminRadio.setToggleGroup(roleGroup);
    }

    @FXML
    protected void handleRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (username.isEmpty()) {
            MessageDialog.showDialog("请输入用户名");
            return;
        }
        if (password.isEmpty()) {
            MessageDialog.showDialog("请输入密码");
            return;
        }
        if (!password.equals(confirmPassword)) {
            MessageDialog.showDialog("两次输入的密码不一致");
            return;
        }
        if (password.length() < 6) {
            MessageDialog.showDialog("密码长度不能少于6位");
            return;
        }

        boolean isAdmin = adminRadio.isSelected();
        String url = isAdmin ? "/auth/register/admin" : "/auth/register/staff";

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("username", username);
        requestBody.put("password", password);

        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(HttpRequestUtil.serverUrl + url))
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                    .headers("Content-Type", "application/json")
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, Object> result = gson.fromJson(response.body(), new TypeToken<Map<String, Object>>(){}.getType());

                if (result.get("code").equals(200.0)) {
                    String msg = isAdmin ? "注册成功！请等待管理员审批" : "注册成功！请登录";
                    MessageDialog.showDialog(msg);
                    handleBack();
                } else {
                    MessageDialog.showDialog("注册失败：" + result.get("msg"));
                }
            } else {
                MessageDialog.showDialog("请求失败，状态码：" + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            MessageDialog.showDialog("注册异常：" + e.getMessage());
        }
    }

    @FXML
    protected void handleBack() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("base/login-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), -1, -1);
            MainApplication.resetStage("仓储管理系统 - 登录", scene);
        } catch (IOException e) {
            e.printStackTrace();
            MessageDialog.showDialog("返回登录页面失败：" + e.getMessage());
        }
    }
}
