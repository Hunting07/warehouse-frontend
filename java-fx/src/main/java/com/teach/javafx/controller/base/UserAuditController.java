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
    private TableView<UserAuditInfo> adminPendingTable;
    @FXML
    private TableColumn<UserAuditInfo, Integer> colPendingId;
    @FXML
    private TableColumn<UserAuditInfo, String> colPendingAccount, colPendingName, colPendingPhone, colPendingRole, colPendingStatus, colPendingTime;
    @FXML
    private TableColumn<UserAuditInfo, Void> colPendingOperate;

    @FXML
    private TableView<UserAuditInfo> adminApprovedTable;
    @FXML
    private TableColumn<UserAuditInfo, Integer> colApprovedId;
    @FXML
    private TableColumn<UserAuditInfo, String> colApprovedAccount, colApprovedName, colApprovedPhone, colApprovedRole, colApprovedStatus, colApprovedTime;

    @FXML
    private TableView<UserAuditInfo> staffTable;
    @FXML
    private TableColumn<UserAuditInfo, Integer> colStaffId;
    @FXML
    private TableColumn<UserAuditInfo, String> colStaffAccount, colStaffName, colStaffPhone, colStaffRole, colStaffStatus, colStaffTime;

    private final ObservableList<UserAuditInfo> adminPendingList = FXCollections.observableArrayList();
    private final ObservableList<UserAuditInfo> adminApprovedList = FXCollections.observableArrayList();
    private final ObservableList<UserAuditInfo> staffList = FXCollections.observableArrayList();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    @FXML
    public void initialize() {
        // 管理员待审批表格
        colPendingId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colPendingAccount.setCellValueFactory(new PropertyValueFactory<>("account"));
        colPendingName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPendingPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colPendingRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colPendingStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colPendingTime.setCellValueFactory(new PropertyValueFactory<>("applyTime"));

        colPendingOperate.setCellFactory(param -> new TableCell<UserAuditInfo, Void>() {
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
                    setGraphic(box);
                }
            }
        });

        adminPendingTable.setItems(adminPendingList);

        // 管理员已审批表格
        colApprovedId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colApprovedAccount.setCellValueFactory(new PropertyValueFactory<>("account"));
        colApprovedName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colApprovedPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colApprovedRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colApprovedStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colApprovedTime.setCellValueFactory(new PropertyValueFactory<>("applyTime"));

        adminApprovedTable.setItems(adminApprovedList);

        // 员工表格
        colStaffId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colStaffAccount.setCellValueFactory(new PropertyValueFactory<>("account"));
        colStaffName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colStaffPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colStaffRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colStaffStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStaffTime.setCellValueFactory(new PropertyValueFactory<>("applyTime"));

        staffTable.setItems(staffList);
        loadUserList();
    }

    @FXML
    public void loadUserList() {
        adminPendingList.clear();
        adminApprovedList.clear();
        staffList.clear();

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

                    if (data != null) {
                        for (Map<String, Object> item : data) {
                            UserAuditInfo info = new UserAuditInfo();
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

                            // 分类显示
                            if ("admin".equals(info.getRole())) {
                                if ("pending".equals(info.getStatus())) {
                                    adminPendingList.add(info);
                                } else {
                                    adminApprovedList.add(info);
                                }
                            } else {
                                staffList.add(info);
                            }
                        }
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
        loadUserList();
    }

    private void handleApprove(UserAuditInfo info, boolean approved) {
        String statusText = approved ? "通过" : "驳回";
        int ret = MessageDialog.choiceDialog("确认要" + statusText + "管理员【" + info.getName() + "】的申请吗？");

        if (ret != MessageDialog.CHOICE_YES) return;

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("id", info.getId());
        requestBody.put("status", approved ? "approved" : "rejected");

        try {
            System.out.println("审批请求参数：" + gson.toJson(requestBody));
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(HttpRequestUtil.serverUrl + "/user/approve"))
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                    .headers("Content-Type", "application/json", "satoken", AppStore.getJwt().getTokenValue())
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            System.out.println("审批接口返回状态码：" + response.statusCode());
            System.out.println("审批接口返回内容：" + response.body());

            if (response.statusCode() == 200) {
                Map<String, Object> result = gson.fromJson(response.body(), new TypeToken<Map<String, Object>>(){}.getType());
                if (result.get("code").equals(200.0)) {
                    MessageDialog.showDialog("审批" + statusText + "成功！");
                    loadUserList();
                } else {
                    MessageDialog.showDialog("审批失败：" + result.get("msg"));
                }
            } else {
                try {
                    Map<String, Object> result = gson.fromJson(response.body(), new TypeToken<Map<String, Object>>(){}.getType());
                    String msg = (String) result.get("msg");
                    if (msg != null && !msg.isEmpty()) {
                        MessageDialog.showDialog("审批失败：" + msg);
                    } else {
                        MessageDialog.showDialog("审批失败，状态码：" + response.statusCode());
                    }
                } catch (Exception e) {
                    MessageDialog.showDialog("审批失败，状态码：" + response.statusCode());
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
