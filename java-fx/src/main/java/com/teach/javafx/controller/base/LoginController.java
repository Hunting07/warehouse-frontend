package com.teach.javafx.controller.base;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.teach.javafx.AppStore;
import com.teach.javafx.MainApplication;
import com.teach.javafx.request.HttpRequestUtil;
import com.teach.javafx.request.JwtResponse;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

public class LoginController {
    @FXML
    private TextField usernameField;
    @FXML
    private TextField passwordField;
    @FXML
    private VBox vbox;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    @FXML
    public void initialize() {
        vbox.setStyle("-fx-background-image: url('shanda1.jpg'); -fx-background-repeat: no-repeat; -fx-background-size: cover;");
    }

    @FXML
    protected void onAdminLoginButtonClick() {
        login("admin", "123456", true);
    }

    @FXML
    protected void onEmployeeLoginButtonClick() {
        login("staff01", "123123", false);
    }

    @FXML
    protected void onRegisterButtonClick() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("base/register-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), -1, -1);
            MainApplication.resetStage("用户注册", scene);
        } catch (IOException e) {
            e.printStackTrace();
            MessageDialog.showDialog("打开注册页面失败：" + e.getMessage());
        }
    }

    private void login(String username, String password, boolean isAdmin) {
        String url = isAdmin ? "/auth/login/admin" : "/auth/login/staff";

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
                    Map<String, Object> data = (Map<String, Object>) result.get("data");
                    String token = (String) data.get("tokenValue");

                    JwtResponse jwt = new JwtResponse();
                    jwt.setTokenValue(token);
                    jwt.setToken(token);
                    AppStore.setJwt(jwt);

                    MessageDialog.showDialog("登录成功");

                    FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("base/main-frame.fxml"));
                    Scene scene = new Scene(fxmlLoader.load(), -1, -1);
                    AppStore.setMainFrameController(fxmlLoader.getController());
                    MainApplication.resetStage("仓储管理系统", scene);
                } else {
                    MessageDialog.showDialog("登录失败：" + result.get("msg"));
                }
            } else {
                MessageDialog.showDialog("请求失败，状态码：" + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            MessageDialog.showDialog("登录异常：" + e.getMessage());
        }
    }
}
