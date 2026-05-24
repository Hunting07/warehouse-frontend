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
import javafx.scene.layout.HBox;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

public class UserAuditController {

    @FXML
    private TextField searchField;
    @FXML
    private TableView<UserAuditInfo> auditTable;
    @FXML
    private TableColumn<UserAuditInfo, Integer> colId;
    @FXML
    private TableColumn<UserAuditInfo, String> colAccount, colName, colPhone, colRole, colStatus, colApplyTime;
    @FXML
    private TableColumn<UserAuditInfo, Void> colOperate;

    private final ObservableList<UserAuditInfo> auditList = FXCollections.observableArrayList();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colAccount.setCellValueFactory(new PropertyValueFactory<>("account"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colApplyTime.setCellValueFactory(new PropertyValueFactory<>("applyTime"));

        colOperate.setCellFactory(param -> new TableCell<UserAuditInfo, Void>() {
            private final Button passBtn = new Button("通过");
            private final Button rejectBtn = new Button("驳回");
            private final HBox box = new HBox(10, passBtn, rejectBtn);

            {
                passBtn.setStyle("-fx-background-color:#28a745;-fx-text-fill:white;-fx-pref-width:70;");
                rejectBtn.setStyle("-fx-background-color:#dc3545;-fx-text-fill:white;-fx-pref-width:70;");

                passBtn.setOnAction(event -> {
                    UserAuditInfo info = getTableView().getItems().get(getIndex());
                    handleApprove(info, true);
                });

                rejectBtn.setOnAction(event -> {
                    UserAuditInfo info = getTableView().getItems().get(getIndex());
                    handleApprove(info, false);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    UserAuditInfo info = getTableView().getItems().get(getIndex());
                    if ("待审批".equals(info.getStatus())) {
                        setGraphic(box);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });

        auditTable.setItems(auditList);
        loadAuditList();
    }

    @FXML
    public void loadAuditList() {
        auditList.clear();

        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(HttpRequestUtil.serverUrl + "/user/list"))
                    .GET()
                    .headers("satoken", AppStore.getJwt().getTokenValue())
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, Object> result = gson.fromJson(response.body(), new TypeToken<Map<String, Object>>(){}.getType());
                if (result.get("code").equals(200.0)) {
                    java.util.List<Map<String, Object>> data = (java.util.List<Map<String, Object>>) result.get("data");
                    for (Map<String, Object> item : data) {
                        UserAuditInfo info = new UserAuditInfo();
                        info.setId(((Number) item.get("id")).intValue());
                        info.setAccount((String) item.get("account"));
                        info.setName((String) item.get("name"));
                        info.setPhone((String) item.getOrDefault("phone", ""));
                        info.setRole((String) item.get("role"));
                        info.setStatus((String) item.get("status"));
                        info.setApplyTime((String) item.getOrDefault("applyTime", ""));
                        auditList.add(info);
                    }
                } else {
                    MessageDialog.showDialog("获取列表失败：" + result.get("msg"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            MessageDialog.showDialog("获取列表异常：" + e.getMessage());
        }
    }

    @FXML
    public void handleSearch() {
        String keyword = searchField.getText().trim();
        if (keyword.isBlank()) {
            loadAuditList();
            return;
        }

        ObservableList<UserAuditInfo> filterList = FXCollections.observableArrayList();
        for (UserAuditInfo info : auditList) {
            if (info.getAccount().contains(keyword) || info.getName().contains(keyword)) {
                filterList.add(info);
            }
        }
        auditTable.setItems(filterList);
    }

    private void handleApprove(UserAuditInfo info, boolean approved) {
        String statusText = approved ? "通过" : "驳回";
        int ret = MessageDialog.choiceDialog("确认要" + statusText + "用户【" + info.getName() + "】的申请吗？");

        if (ret != MessageDialog.CHOICE_YES) return;

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("id", info.getId());
        requestBody.put("role", info.getRole());

        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(HttpRequestUtil.serverUrl + "/user/approve"))
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                    .headers("Content-Type", "application/json", "satoken", AppStore.getJwt().getTokenValue())
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, Object> result = gson.fromJson(response.body(), new TypeToken<Map<String, Object>>(){}.getType());
                if (result.get("code").equals(200.0)) {
                    MessageDialog.showDialog("审批" + statusText + "成功！");
                    loadAuditList();
                } else {
                    MessageDialog.showDialog("审批失败：" + result.get("msg"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            MessageDialog.showDialog("审批异常：" + e.getMessage());
        }
    }

    public static class UserAuditInfo {
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
