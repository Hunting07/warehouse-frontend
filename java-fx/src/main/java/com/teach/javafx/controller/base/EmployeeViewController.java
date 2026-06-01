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
    private TableColumn<EmployeeInfo, Integer> idColumn;
    @FXML
    private TableColumn<EmployeeInfo, String> usernameColumn;
    @FXML
    private TableColumn<EmployeeInfo, String> realNameColumn;
    @FXML
    private TableColumn<EmployeeInfo, String> phoneColumn;
    @FXML
    private TableColumn<EmployeeInfo, String> roleColumn;
    @FXML
    private TableColumn<EmployeeInfo, String> statusColumn;
    @FXML
    private TableColumn<EmployeeInfo, String> createTimeColumn;

    private ObservableList<EmployeeInfo> employeeList = FXCollections.observableArrayList();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        realNameColumn.setCellValueFactory(new PropertyValueFactory<>("realName"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        createTimeColumn.setCellValueFactory(new PropertyValueFactory<>("createTime"));

        employeeTable.setItems(employeeList);
        loadEmployeeList();
    }

    @FXML
    public void loadEmployeeList() {
        employeeList.clear();

        new Thread(() -> {
            try {
                System.out.println("=== [前端] 加载员工列表 ===");
                System.out.println("请求URL: " + HttpRequestUtil.serverUrl + "/user/list?role=employee");
                System.out.println("Token: " + AppStore.getJwt().getTokenValue());

                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(HttpRequestUtil.serverUrl + "/user/list?role=employee"))
                        .GET()
                        .headers("satoken", AppStore.getJwt().getTokenValue())
                        .build();

                HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                System.out.println("响应状态码: " + response.statusCode());
                System.out.println("响应内容: " + response.body());

                if (response.statusCode() == 200) {
                    Map<String, Object> result = gson.fromJson(response.body(), new TypeToken<Map<String, Object>>(){}.getType());

                    if (result.get("code").equals(200.0)) {
                        List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");

                        if (data != null) {
                            System.out.println("员工数量: " + data.size());
                            for (Map<String, Object> item : data) {
                                EmployeeInfo info = new EmployeeInfo();
                                info.setId(((Number) item.get("id")).intValue());
                                info.setUsername((String) item.get("username"));

                                String realName = (String) item.get("realName");
                                if (realName == null || realName.isEmpty()) {
                                    realName = info.getUsername();
                                }
                                info.setRealName(realName);

                                info.setPhone((String) item.getOrDefault("phone", ""));
                                info.setRole((String) item.get("role"));
                                info.setStatus((String) item.get("status"));
                                info.setCreateTime((String) item.getOrDefault("createTime", ""));

                                javafx.application.Platform.runLater(() -> employeeList.add(info));
                            }
                            System.out.println("员工列表加载完成");
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


    public static class EmployeeInfo {
        private int id;
        private String username;
        private String realName;
        private String phone;
        private String role;
        private String status;
        private String createTime;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

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
