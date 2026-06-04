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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class AdminViewController {

    @FXML
    private TableView<AdminInfo> adminTable;
    @FXML
    private TableColumn<AdminInfo, Integer> colRowNum;
    @FXML
    private TableColumn<AdminInfo, String> colAdminNo;
    @FXML
    private TableColumn<AdminInfo, String> colAdminAccount;
    @FXML
    private TableColumn<AdminInfo, String> colAdminName;
    @FXML
    private TableColumn<AdminInfo, String> colAdminPhone;
    @FXML
    private TableColumn<AdminInfo, String> colAdminRole;
    @FXML
    private TableColumn<AdminInfo, String> colAdminStatus;
    @FXML
    private TableColumn<AdminInfo, String> colAdminTime;

    @FXML
    private Label totalAdminsLabel;
    @FXML
    private Label todayAddedLabel;

    private final ObservableList<AdminInfo> adminList = FXCollections.observableArrayList();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    @FXML
    public void initialize() {
        colRowNum.setCellValueFactory(new PropertyValueFactory<>("rowNum"));
        colAdminNo.setCellValueFactory(new PropertyValueFactory<>("employeeNo"));
        colAdminAccount.setCellValueFactory(new PropertyValueFactory<>("account"));
        colAdminName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colAdminPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colAdminRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colAdminStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colAdminTime.setCellValueFactory(new PropertyValueFactory<>("applyTime"));

        addHardcodedAdmin();
        adminTable.setItems(adminList);

        loadUserList();
    }

    private void addHardcodedAdmin() {
        AdminInfo admin = new AdminInfo();
        admin.setRowNum(adminList.size() + 1);
        admin.setEmployeeNo("202610001");
        admin.setAccount("admin");
        admin.setName("系统管理员");
        admin.setPhone("");
        admin.setRole("admin");
        admin.setStatus("active");
        admin.setApplyTime("2024-01-01 00:00:00");

        adminList.add(admin);
    }

    @FXML
    public void loadUserList() {
        adminList.clear();
        addHardcodedAdmin();

        new Thread(() -> {
            try {
                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(HttpRequestUtil.serverUrl + "/user/list?role=admin"))
                        .GET()
                        .headers("satoken", AppStore.getJwt().getTokenValue())
                        .build();

                HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                int totalCount = 0;
                int todayAddedCount = 0;
                String today = LocalDate.now().toString();

                if (response.statusCode() == 200) {
                    Map<String, Object> result = gson.fromJson(response.body(), new TypeToken<Map<String, Object>>(){}.getType());

                    if (result.get("code").equals(200.0)) {
                        Object dataObj = result.get("data");

                        if (dataObj instanceof List) {
                            List<Map<String, Object>> data = (List<Map<String, Object>>) dataObj;

                            int rowNum = adminList.size() + 1;
                            for (int i = 0; i < data.size(); i++) {
                                Map<String, Object> item = data.get(i);
                                String status = (String) item.get("status");

                                if ("approved".equals(status)) {
                                    totalCount++;

                                    AdminInfo info = new AdminInfo();
                                    info.setRowNum(rowNum++);

                                    info.setEmployeeNo((String) item.getOrDefault("employeeNo", ""));

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

                                    String createTime = (String) item.getOrDefault("createTime", "");
                                    info.setApplyTime(createTime);

                                    if (createTime != null && createTime.startsWith(today)) {
                                        todayAddedCount++;
                                    }

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

                int finalTotalCount = totalCount;
                int finalTodayAddedCount = todayAddedCount;

                javafx.application.Platform.runLater(() -> {
                    totalAdminsLabel.setText(String.valueOf(finalTotalCount + 1));
                    todayAddedLabel.setText(String.valueOf(finalTodayAddedCount));
                });

            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() ->
                        MessageDialog.showDialog("获取列表异常：" + e.getMessage())
                );
            }
        }).start();
    }


    public static class AdminInfo {
        private Integer rowNum;
        private String employeeNo;
        private String account;
        private String name;
        private String phone;
        private String role;
        private String status;
        private String applyTime;

        public Integer getRowNum() { return rowNum; }
        public void setRowNum(Integer rowNum) { this.rowNum = rowNum; }

        public String getEmployeeNo() { return employeeNo; }
        public void setEmployeeNo(String employeeNo) { this.employeeNo = employeeNo; }

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
