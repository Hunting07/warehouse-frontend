package com.teach.javafx.controller.base;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.teach.javafx.AppStore;
import com.teach.javafx.bean.OutOrder;
import com.teach.javafx.request.HttpRequestUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OutOrderListController {

    @FXML
    private TextField searchOrderNoField;

    @FXML
    private ComboBox<String> statusComboBox;

    @FXML
    private TextField applicantNameField;

    @FXML
    private TableView<OutOrder> outOrderTable;

    @FXML
    private TableColumn<OutOrder, Integer> idColumn;

    @FXML
    private TableColumn<OutOrder, String> orderNoColumn;

    @FXML
    private TableColumn<OutOrder, String> applicantNameColumn;

    @FXML
    private TableColumn<OutOrder, LocalDateTime> applyTimeColumn;

    @FXML
    private TableColumn<OutOrder, Integer> totalNumColumn;

    @FXML
    private TableColumn<OutOrder, BigDecimal> totalAmountColumn;

    @FXML
    private TableColumn<OutOrder, Integer> statusColumn;

    @FXML
    private TableColumn<OutOrder, String> auditUserNameColumn;

    @FXML
    private TableColumn<OutOrder, LocalDateTime> auditTimeColumn;

    @FXML
    private TableColumn<OutOrder, String> remarkColumn;

    private final ObservableList<OutOrder> outOrderList = FXCollections.observableArrayList();
    private final Gson gson = new Gson();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        orderNoColumn.setCellValueFactory(new PropertyValueFactory<>("orderNo"));
        applicantNameColumn.setCellValueFactory(new PropertyValueFactory<>("applicantName"));
        applyTimeColumn.setCellValueFactory(new PropertyValueFactory<>("applyTime"));
        totalNumColumn.setCellValueFactory(new PropertyValueFactory<>("totalNum"));
        totalAmountColumn.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        auditUserNameColumn.setCellValueFactory(new PropertyValueFactory<>("auditUserName"));
        auditTimeColumn.setCellValueFactory(new PropertyValueFactory<>("auditTime"));
        remarkColumn.setCellValueFactory(new PropertyValueFactory<>("remark"));

        statusColumn.setCellFactory(col -> new TableCell<OutOrder, Integer>() {
            @Override
            protected void updateItem(Integer status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                } else {
                    switch (status) {
                        case 0: setText("待审批"); break;
                        case 1: setText("已通过"); break;
                        case 2: setText("已完成"); break;
                        case 3: setText("已驳回"); break;
                        default: setText("未知");
                    }
                }
            }
        });

        outOrderTable.setItems(outOrderList);

        statusComboBox.getItems().addAll("全部", "待审批", "已通过", "已完成", "已驳回");
        statusComboBox.setValue("全部");

        loadOutOrderList();
    }

    private void loadOutOrderList() {
        try {
            StringBuilder urlBuilder = new StringBuilder(HttpRequestUtil.serverUrl + "/api/outOrder/list");
            boolean hasParam = false;

            String status = statusComboBox.getValue();
            if (status != null && !status.equals("全部")) {
                urlBuilder.append(hasParam ? "&" : "?").append("status=").append(getStatusValue(status));
                hasParam = true;
            }

            String applicantName = applicantNameField.getText();
            if (applicantName != null && !applicantName.trim().isEmpty()) {
                urlBuilder.append(hasParam ? "&" : "?").append("applicantName=").append(applicantName.trim());
                hasParam = true;
            }

            String orderNo = searchOrderNoField.getText();
            if (orderNo != null && !orderNo.trim().isEmpty()) {
                urlBuilder.append(hasParam ? "&" : "?").append("orderNo=").append(orderNo.trim());
                hasParam = true;
            }

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(urlBuilder.toString()))
                    .GET()
                    .headers("satoken", AppStore.getJwt().getToken())
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, Object> resultMap = gson.fromJson(response.body(), new TypeToken<Map<String, Object>>(){}.getType());
                if (resultMap.get("code").equals(200.0) || resultMap.get("code").equals(0)) {
                    List<Map<String, Object>> dataList = (List<Map<String, Object>>) resultMap.get("data");
                    if (dataList != null) {
                        List<OutOrder> list = gson.fromJson(gson.toJson(dataList), new TypeToken<List<OutOrder>>(){}.getType());
                        outOrderList.setAll(list);
                    }
                } else {
                    MessageDialog.showDialog("加载数据失败：" + resultMap.get("msg"));
                }
            } else {
                MessageDialog.showDialog("请求失败，状态码：" + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            MessageDialog.showDialog("加载数据异常：" + e.getMessage());
        }
    }

    @FXML
    protected void onSearchButtonClick() {
        loadOutOrderList();
    }

    @FXML
    protected void onResetButtonClick() {
        searchOrderNoField.clear();
        applicantNameField.clear();
        statusComboBox.setValue("全部");
        loadOutOrderList();
    }

    @FXML
    protected void onApproveButtonClick() {
        OutOrder selected = outOrderTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            MessageDialog.showDialog("请选择要审批的出库单");
            return;
        }
        if (selected.getStatus() != 0) {
            MessageDialog.showDialog("只能审批待审批状态的出库单");
            return;
        }

        int ret = MessageDialog.choiceDialog("确认批准出库单 " + selected.getOrderNo() + "？");
        if (ret != MessageDialog.CHOICE_YES) return;

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("outOrderId", selected.getId());
            requestBody.put("approved", true);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(HttpRequestUtil.serverUrl + "/api/outOrder/approve"))
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                    .headers("Content-Type", "application/json", "satoken", AppStore.getJwt().getToken())
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                MessageDialog.showDialog("审批成功");
                loadOutOrderList();
            } else {
                MessageDialog.showDialog("审批失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            MessageDialog.showDialog("审批异常：" + e.getMessage());
        }
    }

    @FXML
    protected void onRejectButtonClick() {
        OutOrder selected = outOrderTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            MessageDialog.showDialog("请选择要驳回的出库单");
            return;
        }
        if (selected.getStatus() != 0) {
            MessageDialog.showDialog("只能驳回待审批状态的出库单");
            return;
        }

        int ret = MessageDialog.choiceDialog("确认驳回出库单 " + selected.getOrderNo() + "？");
        if (ret != MessageDialog.CHOICE_YES) return;

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("outOrderId", selected.getId());
            requestBody.put("approved", false);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(HttpRequestUtil.serverUrl + "/api/outOrder/approve"))
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                    .headers("Content-Type", "application/json", "satoken", AppStore.getJwt().getToken())
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                MessageDialog.showDialog("驳回成功");
                loadOutOrderList();
            } else {
                MessageDialog.showDialog("驳回失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            MessageDialog.showDialog("驳回异常：" + e.getMessage());
        }
    }

    @FXML
    protected void onCompleteButtonClick() {
        OutOrder selected = outOrderTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            MessageDialog.showDialog("请选择要完成出库的出库单");
            return;
        }
        if (selected.getStatus() != 1) {
            MessageDialog.showDialog("只能完成已通过状态的出库单");
            return;
        }

        int ret = MessageDialog.choiceDialog("确认完成出库 " + selected.getOrderNo() + "？");
        if (ret != MessageDialog.CHOICE_YES) return;

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("outOrderId", selected.getId());

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(HttpRequestUtil.serverUrl + "/api/outOrder/complete"))
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                    .headers("Content-Type", "application/json", "satoken", AppStore.getJwt().getToken())
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                MessageDialog.showDialog("出库完成成功");
                loadOutOrderList();
            } else {
                MessageDialog.showDialog("出库完成失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            MessageDialog.showDialog("出库完成异常：" + e.getMessage());
        }
    }

    @FXML
    protected void onDeleteButtonClick() {
        OutOrder selected = outOrderTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            MessageDialog.showDialog("请选择要删除的出库单");
            return;
        }
        if (selected.getStatus() != 0 && selected.getStatus() != 3) {
            MessageDialog.showDialog("只能删除待审批或已驳回状态的出库单");
            return;
        }

        int ret = MessageDialog.choiceDialog("确认删除出库单 " + selected.getOrderNo() + "？");
        if (ret != MessageDialog.CHOICE_YES) return;

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("outOrderId", selected.getId());

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(HttpRequestUtil.serverUrl + "/api/outOrder/delete"))
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                    .headers("Content-Type", "application/json", "satoken", AppStore.getJwt().getToken())
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                MessageDialog.showDialog("删除成功");
                loadOutOrderList();
            } else {
                MessageDialog.showDialog("删除失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            MessageDialog.showDialog("删除异常：" + e.getMessage());
        }
    }

    private Integer getStatusValue(String status) {
        switch (status) {
            case "待审批": return 0;
            case "已通过": return 1;
            case "已完成": return 2;
            case "已驳回": return 3;
            default: return null;
        }
    }
}
