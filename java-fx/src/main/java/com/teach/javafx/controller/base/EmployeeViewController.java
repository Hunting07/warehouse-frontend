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

public class EmployeeViewController {

    @FXML
    private TableView<EmployeeInfo> employeeTable;
    @FXML
    private TableColumn<EmployeeInfo, Integer> colRowNum;
    @FXML
    private TableColumn<EmployeeInfo, String> colEmployeeNo;
    @FXML
    private TableColumn<EmployeeInfo, String> colEmployeeAccount;
    @FXML
    private TableColumn<EmployeeInfo, String> colEmployeeName;
    @FXML
    private TableColumn<EmployeeInfo, String> colEmployeePhone;
    @FXML
    private TableColumn<EmployeeInfo, String> colEmployeeRole;
    @FXML
    private TableColumn<EmployeeInfo, String> colEmployeeStatus;
    @FXML
    private TableColumn<EmployeeInfo, String> colEmployeeTime;

    @FXML
    private Label totalEmployeesLabel;
    @FXML
    private Label activeEmployeesLabel;

    private final ObservableList<EmployeeInfo> employeeList = FXCollections.observableArrayList();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    @FXML
    public void initialize() {
        colRowNum.setCellValueFactory(new PropertyValueFactory<>("rowNum"));
        colEmployeeNo.setCellValueFactory(new PropertyValueFactory<>("employeeNo"));
        colEmployeeAccount.setCellValueFactory(new PropertyValueFactory<>("account"));
        colEmployeeName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colEmployeePhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colEmployeeRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colEmployeeStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colEmployeeTime.setCellValueFactory(new PropertyValueFactory<>("applyTime"));

        employeeTable.setItems(employeeList);
        loadEmployeeList();
    }

    @FXML
    public void loadEmployeeList() {
        employeeList.clear();

        new Thread(() -> {
            try {
                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(HttpRequestUtil.serverUrl + "/user/list?role=staff"))
                        .GET()
                        .headers("satoken", AppStore.getJwt().getTokenValue())
                        .build();

                HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                int totalCount = 0;
                int activeCount = 0;

                if (response.statusCode() == 200) {
                    Map<String, Object> result = gson.fromJson(response.body(), new TypeToken<Map<String, Object>>(){}.getType());

                    if (result.get("code").equals(200.0)) {
                        Object dataObj = result.get("data");

                        if (dataObj instanceof List) {
                            List<Map<String, Object>> data = (List<Map<String, Object>>) dataObj;

                            totalCount = data.size();

                            for (int i = 0; i < data.size(); i++) {
                                Map<String, Object> item = data.get(i);
                                EmployeeInfo info = new EmployeeInfo();
                                info.setRowNum(i + 1);

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
                                String status = (String) item.get("status");
                                info.setStatus(status);
                                info.setApplyTime((String) item.getOrDefault("createTime", ""));

                                if ("active".equals(status) || "approved".equals(status)) {
                                    activeCount++;
                                }

                                javafx.application.Platform.runLater(() -> employeeList.add(info));
                            }
                        }
                    } else {
                        javafx.application.Platform.runLater(() ->
                                MessageDialog.showDialog("获取列表失败：" + result.get("msg"))
                        );
                    }
                }

                int finalTotalCount = totalCount;
                int finalActiveCount = activeCount;

                javafx.application.Platform.runLater(() -> {
                    if (totalEmployeesLabel != null) {
                        totalEmployeesLabel.setText(String.valueOf(finalTotalCount));
                    }
                    if (activeEmployeesLabel != null) {
                        activeEmployeesLabel.setText(String.valueOf(finalActiveCount));
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() ->
                        MessageDialog.showDialog("获取列表异常：" + e.getMessage())
                );
            }
        }).start();
    }

    public static class EmployeeInfo {
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
