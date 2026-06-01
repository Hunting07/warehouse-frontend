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
import java.util.List;
import java.util.Map;

public class AdminApproveController {

    @FXML
    private TableView<AdminInfo> pendingAdminTable;
    @FXML
    private TableColumn<AdminInfo, Integer> pendingNoColumn;
    @FXML
    private TableColumn<AdminInfo, String> pendingEmployeeNoColumn;
    @FXML
    private TableColumn<AdminInfo, String> pendingUsernameColumn;
    @FXML
    private TableColumn<AdminInfo, String> pendingRealNameColumn;
    @FXML
    private TableColumn<AdminInfo, String> pendingPhoneColumn;
    @FXML
    private TableColumn<AdminInfo, String> pendingRoleColumn;
    @FXML
    private TableColumn<AdminInfo, String> pendingStatusColumn;
    @FXML
    private TableColumn<AdminInfo, String> pendingTimeColumn;
    @FXML
    private TableColumn<AdminInfo, Void> pendingActionColumn;

    @FXML
    private TableView<AdminInfo> approvedAdminTable;
    @FXML
    private TableColumn<AdminInfo, Integer> approvedNoColumn;
    @FXML
    private TableColumn<AdminInfo, String> approvedEmployeeNoColumn;
    @FXML
    private TableColumn<AdminInfo, String> approvedUsernameColumn;
    @FXML
    private TableColumn<AdminInfo, String> approvedRealNameColumn;
    @FXML
    private TableColumn<AdminInfo, String> approvedPhoneColumn;
    @FXML
    private TableColumn<AdminInfo, String> approvedRoleColumn;
    @FXML
    private TableColumn<AdminInfo, String> approvedStatusColumn;
    @FXML
    private TableColumn<AdminInfo, String> approvedTimeColumn;

    private ObservableList<AdminInfo> pendingList = FXCollections.observableArrayList();
    private ObservableList<AdminInfo> approvedList = FXCollections.observableArrayList();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    @FXML
    public void initialize() {
        setupPendingTable();
        setupApprovedTable();
        loadUserList();
    }

    private void setupPendingTable() {
        pendingNoColumn.setCellValueFactory(new PropertyValueFactory<>("rowNum"));
        pendingEmployeeNoColumn.setCellValueFactory(new PropertyValueFactory<>("employeeNo"));
        pendingUsernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        pendingRealNameColumn.setCellValueFactory(new PropertyValueFactory<>("realName"));
        pendingPhoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        pendingRoleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        pendingStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        pendingTimeColumn.setCellValueFactory(new PropertyValueFactory<>("createTime"));

        pendingActionColumn.setCellFactory(col -> {
            TableCell<AdminInfo, Void> cell = new TableCell<AdminInfo, Void>() {
                private final Button approveBtn = new Button("通过");
                private final Button rejectBtn = new Button("驳回");

                {
                    approveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 5 15; -fx-background-radius: 4;");
                    rejectBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 5 15; -fx-background-radius: 4;");

                    approveBtn.setOnAction(e -> {
                        AdminInfo info = getTableView().getItems().get(getIndex());
                        handleApprove(info, true);
                    });
                    rejectBtn.setOnAction(e -> {
                        AdminInfo info = getTableView().getItems().get(getIndex());
                        handleApprove(info, false);
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        HBox box = new HBox(5, approveBtn, rejectBtn);
                        box.setStyle("-fx-alignment: center;");
                        setGraphic(box);
                    }
                }
            };
            return cell;
        });

        pendingAdminTable.setItems(pendingList);
    }

    private void setupApprovedTable() {
        approvedNoColumn.setCellValueFactory(new PropertyValueFactory<>("rowNum"));
        approvedEmployeeNoColumn.setCellValueFactory(new PropertyValueFactory<>("employeeNo"));
        approvedUsernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        approvedRealNameColumn.setCellValueFactory(new PropertyValueFactory<>("realName"));
        approvedPhoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        approvedRoleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        approvedStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        approvedTimeColumn.setCellValueFactory(new PropertyValueFactory<>("createTime"));

        approvedAdminTable.setItems(approvedList);
    }

    @FXML
    public void loadUserList() {
        pendingList.clear();
        approvedList.clear();

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
                            int pendingIndex = 1;
                            int approvedIndex = 1;

                            for (Map<String, Object> item : data) {
                                AdminInfo info = new AdminInfo();
                                info.setUsername((String) item.get("username"));

                                String realName = (String) item.get("realName");
                                if (realName == null || realName.isEmpty()) {
                                    realName = info.getUsername();
                                }
                                info.setRealName(realName);

                                info.setEmployeeNo((String) item.getOrDefault("employeeNo", ""));
                                info.setPhone((String) item.getOrDefault("phone", ""));
                                info.setRole((String) item.get("role"));
                                info.setStatus((String) item.get("status"));
                                info.setCreateTime((String) item.getOrDefault("createTime", ""));

                                if ("pending".equals(info.getStatus())) {
                                    info.setRowNum(pendingIndex++);
                                    pendingList.add(info);
                                } else {
                                    info.setRowNum(approvedIndex++);
                                    approvedList.add(info);
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

    private void handleApprove(AdminInfo info, boolean approved) {
        String statusText = approved ? "通过" : "驳回";
        int ret = MessageDialog.choiceDialog("确认要" + statusText + "管理员【" + info.getRealName() + "】的申请吗？");

        if (ret != MessageDialog.CHOICE_YES) return;

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("id", info.getId());
        requestBody.put("status", approved ? "approved" : "rejected");

        new Thread(() -> {
            try {
                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(HttpRequestUtil.serverUrl + "/user/approve"))
                        .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                        .headers("Content-Type", "application/json", "satoken", AppStore.getJwt().getTokenValue())
                        .build();

                HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    Map<String, Object> result = gson.fromJson(response.body(), new TypeToken<Map<String, Object>>(){}.getType());

                    javafx.application.Platform.runLater(() -> {
                        if (result.get("code").equals(200.0)) {
                            MessageDialog.showDialog(statusText + "成功");
                            loadUserList();
                        } else {
                            MessageDialog.showDialog(statusText + "失败：" + result.get("msg"));
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() ->
                        MessageDialog.showDialog(statusText + "异常：" + e.getMessage())
                );
            }
        }).start();
    }

    public static class AdminInfo {
        private Integer rowNum;
        private int id;
        private String employeeNo;
        private String username;
        private String realName;
        private String phone;
        private String role;
        private String status;
        private String createTime;

        public Integer getRowNum() { return rowNum; }
        public void setRowNum(Integer rowNum) { this.rowNum = rowNum; }

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getEmployeeNo() { return employeeNo; }
        public void setEmployeeNo(String employeeNo) { this.employeeNo = employeeNo; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getRealName() { return realName; }
        public void setRealName(String realName) { this.realName = realName; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getCreateTime() { return createTime; }
        public void setCreateTime(String createTime) { this.createTime = createTime; }
    }
}
