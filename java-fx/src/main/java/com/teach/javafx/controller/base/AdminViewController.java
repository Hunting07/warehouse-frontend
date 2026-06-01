package com.teach.javafx.controller.base;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.teach.javafx.AppStore;
import com.teach.javafx.request.HttpRequestUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

public class AdminViewController {

    @FXML
    private TableView<AdminInfo> adminTable;
    @FXML
    private TableColumn<AdminInfo, Integer> colAdminId;
    @FXML
    private TableColumn<AdminInfo, String> colAdminAccount, colAdminName, colAdminPhone, colAdminRole, colAdminStatus, colAdminTime;

    private final ObservableList<AdminInfo> adminList = FXCollections.observableArrayList();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    @FXML
    public void initialize() {
        colAdminId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colAdminAccount.setCellValueFactory(new PropertyValueFactory<>("account"));
        colAdminName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colAdminPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colAdminRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colAdminStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colAdminTime.setCellValueFactory(new PropertyValueFactory<>("applyTime"));

        adminTable.setItems(adminList);
        loadUserList();
    }

    @FXML
    public void loadUserList() {
        adminList.clear();

        new Thread(() -> {
            try {
                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(HttpRequestUtil.serverUrl + "/user/list?role=admin"))
                        .GET()
                        .headers("satoken", AppStore.getJwt().getTokenValue())
                        .build();

                HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    Map<String, Object> result = gson.fromJson(response.body(), new TypeToken<Map<String, Object>>(){}.getType());

                    if (result.get("code").equals(200.0)) {
                        List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");

                        if (data != null) {
                            for (Map<String, Object> item : data) {
                                String status = (String) item.get("status");

                                // 只显示已批准的管理员
                                if ("approved".equals(status) || "active".equals(status)) {
                                    AdminInfo info = new AdminInfo();
                                    info.setId(((Number) item.get("id")).intValue());

                                    String account = (String) item.get("username");
                                    if (account == null) account = (String) item.get("account");
                                    info.setAccount(account);

                                    String name = (String) item.get("realName");
                                    if (name == null || name.isEmpty()) {
                                        name = account;
                                    }
                                    info.setName(name);

                                    info.setPhone((String) item.getOrDefault("phone", ""));
                                    info.setRole((String) item.get("role"));
                                    info.setStatus((String) item.get("status"));
                                    info.setApplyTime((String) item.getOrDefault("createTime", ""));

                                    javafx.application.Platform.runLater(() -> adminList.add(info));
                                }
                            }
                        }
                    } else {
                        javafx.application.Platform.runLater(() ->
                                MessageDialog.showDialog("获取列表失败：" + result.get("msg"))
                        );
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() ->
                        MessageDialog.showDialog("获取列表异常：" + e.getMessage())
                );
            }
        }).start();
    }


    public static class AdminInfo {
        private Integer id;
        private String account;
        private String name;
        private String phone;
        private String role;
        private String status;
        private String applyTime;

        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }
        public String getAccount() { return account; }
        public void setAccount(String account) { this.account = account; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getApplyTime() { return applyTime; }
        public void setApplyTime(String applyTime) { this.applyTime = applyTime; }
    }
}
