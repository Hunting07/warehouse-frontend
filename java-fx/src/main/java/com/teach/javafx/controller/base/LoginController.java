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
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.GridPane;
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
    private TextInputControl passwordInputControl;
    @FXML
    private PasswordField passwordField;
    @FXML
    private VBox vbox;
    @FXML
    private Button passwordToggleBtn;

    private boolean passwordVisible = false;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    @FXML
    public void initialize() {
        vbox.setStyle("-fx-background-image: url('shanda1.jpg'); -fx-background-repeat: no-repeat; -fx-background-size: cover;");
        passwordInputControl = passwordField;
    }

    @FXML
    protected void onAdminLoginButtonClick() {
        String username = usernameField.getText().trim();
        String password = passwordInputControl.getText();

        if (username.isEmpty() || password.isEmpty()) {
            MessageDialog.showDialog("请输入用户名和密码");
            return;
        }

        login(username, password, true);
    }

    @FXML
    protected void onEmployeeLoginButtonClick() {
        String username = usernameField.getText().trim();
        String password = passwordInputControl.getText();

        if (username.isEmpty() || password.isEmpty()) {
            MessageDialog.showDialog("请输入用户名和密码");
            return;
        }

        login(username, password, false);
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

    @FXML
    protected void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        GridPane parent = (GridPane) passwordInputControl.getParent();
        Integer rowIndex = GridPane.getRowIndex(passwordInputControl);
        Integer columnIndex = GridPane.getColumnIndex(passwordInputControl);
        int row = (rowIndex == null) ? 1 : rowIndex;
        int col = (columnIndex == null) ? 1 : columnIndex;

        String pwdText = passwordInputControl.getText();
        String pwdStyle = passwordInputControl.getStyle();
        String pwdId = passwordInputControl.getId();
        String pwdPrompt = passwordInputControl.getPromptText();

        parent.getChildren().remove(passwordInputControl);

        if (passwordVisible) {
            TextField textField = new TextField();
            textField.setText(pwdText);
            textField.setId(pwdId);
            textField.setStyle(pwdStyle);
            textField.setPromptText(pwdPrompt);
            GridPane.setRowIndex(textField, row);
            GridPane.setColumnIndex(textField, col);
            parent.getChildren().add(textField);
            passwordInputControl = textField;
            passwordToggleBtn.setText("🔓");
        } else {
            PasswordField newPwdField = new PasswordField();
            newPwdField.setText(pwdText);
            newPwdField.setId(pwdId);
            newPwdField.setStyle(pwdStyle);
            newPwdField.setPromptText(pwdPrompt);
            GridPane.setRowIndex(newPwdField, row);
            GridPane.setColumnIndex(newPwdField, col);
            parent.getChildren().add(newPwdField);
            passwordInputControl = newPwdField;
            passwordToggleBtn.setText("👁");
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
                    String token = (String) data.get("token");
                    Map<String, Object> userInfo = (Map<String, Object>) data.get("userInfo");

                    if (token == null || userInfo == null) {
                        MessageDialog.showDialog("登录失败：未获取到完整数据");
                        return;
                    }

                    String userName = (String) userInfo.get("username");
                    String role = (String) userInfo.get("role");

                    Object userIdObj = userInfo.get("id");
                    if (userIdObj == null) {
                        userIdObj = userInfo.get("userId");
                    }

                    String loginId = null;
                    if (userIdObj != null) {
                        if (userIdObj instanceof Number) {
                            loginId = String.valueOf(((Number) userIdObj).intValue());
                        } else {
                            loginId = String.valueOf(userIdObj);
                        }
                    }

                    if (isAdmin && !"admin".equals(role)) {
                        MessageDialog.showDialog("请使用员工登录入口");
                        return;
                    }
                    if (!isAdmin && !"staff".equals(role)) {
                        MessageDialog.showDialog("请使用管理员登录入口");
                        return;
                    }

                    JwtResponse jwt = new JwtResponse();
                    jwt.setToken(token);
                    jwt.setTokenValue(token);
                    jwt.setUsername(userName);
                    jwt.setRole(role);
                    jwt.setLoginId(loginId);
                    AppStore.setJwt(jwt);

                    MessageDialog.showDialog("登录成功");

                    FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("base/main-frame.fxml"));
                    Scene scene = new Scene(fxmlLoader.load(), -1, -1);
                    AppStore.setMainFrameController(fxmlLoader.getController());
                    MainApplication.resetStage("仓储管理系统", scene);
                } else {
                    String msg = (String) result.get("msg");
                    if (msg != null) {
                        if (msg.contains("待审批") || msg.contains("pending") || msg.contains("未通过")) {
                            MessageDialog.showDialog("请等待管理员审批通过哦~");
                        } else if (msg.contains("驳回") || msg.contains("rejected") || msg.contains("拒绝")) {
                            MessageDialog.showDialog("很抱歉！您的管理员申请已被驳回！");
                        } else {
                            MessageDialog.showDialog("登录失败：" + msg);
                        }
                    } else {
                        MessageDialog.showDialog("登录失败");
                    }
                }
            } else {
                try {
                    Map<String, Object> result = gson.fromJson(response.body(), new TypeToken<Map<String, Object>>(){}.getType());
                    String msg = (String) result.get("msg");
                    if (msg != null) {
                        if (msg.contains("待审批") || msg.contains("pending") || msg.contains("未通过")) {
                            MessageDialog.showDialog("请等待管理员审批通过哦~");
                        } else if (msg.contains("驳回") || msg.contains("rejected") || msg.contains("拒绝")) {
                            MessageDialog.showDialog("很抱歉！您的管理员申请已被驳回！");
                        } else {
                            MessageDialog.showDialog("登录失败：" + msg);
                        }
                    } else {
                        MessageDialog.showDialog("请求失败，状态码：" + response.statusCode());
                    }
                } catch (Exception e) {
                    MessageDialog.showDialog("请求失败，状态码：" + response.statusCode());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            MessageDialog.showDialog("登录异常：" + e.getMessage());
        }
    }

}
