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
import java.time.format.DateTimeFormatter;
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
    private TableColumn<OutOrder, String> applyTimeColumn;

    @FXML
    private TableColumn<OutOrder, Integer> totalNumColumn;

    @FXML
    private TableColumn<OutOrder, BigDecimal> totalAmountColumn;

    @FXML
    private TableColumn<OutOrder, String> statusColumn;

    @FXML
    private TableColumn<OutOrder, String> auditUserNameColumn;

    @FXML
    private TableColumn<OutOrder, String> auditTimeColumn;

    @FXML
    private TableColumn<OutOrder, String> remarkColumn;

    @FXML
    private TableColumn<OutOrder, String> outTypeColumn;

    @FXML
    private TableColumn<OutOrder, String> rejectReasonColumn;

    private final ObservableList<OutOrder> outOrderList = FXCollections.observableArrayList();
    private final Gson gson = new Gson();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private boolean isAdmin = false;

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        orderNoColumn.setCellValueFactory(new PropertyValueFactory<>("orderNo"));
        applicantNameColumn.setCellValueFactory(new PropertyValueFactory<>("applicantName"));
        applyTimeColumn.setCellValueFactory(cellData -> {
            LocalDateTime time = cellData.getValue().getApplyTime();
            return new javafx.beans.property.SimpleStringProperty(time != null ? time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "");
        });
        totalNumColumn.setCellValueFactory(new PropertyValueFactory<>("totalNum"));
        totalAmountColumn.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        auditUserNameColumn.setCellValueFactory(new PropertyValueFactory<>("auditUserName"));
        auditTimeColumn.setCellValueFactory(cellData -> {
            LocalDateTime time = cellData.getValue().getAuditTime();
            return new javafx.beans.property.SimpleStringProperty(time != null ? time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "");
        });
        remarkColumn.setCellValueFactory(new PropertyValueFactory<>("remark"));
        outTypeColumn.setCellValueFactory(cellData -> {
            Integer type = cellData.getValue().getOutType();
            return new javafx.beans.property.SimpleStringProperty(getOutTypeName(type));
        });
        rejectReasonColumn.setCellValueFactory(new PropertyValueFactory<>("rejectReason"));

        statusColumn.setCellFactory(col -> new TableCell<OutOrder, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    switch (status) {
                        case "0":
                            setText("待审批");
                            setStyle("-fx-background-color: #fff4cc;");
                            break;
                        case "1":
                            setText("已出库");
                            setStyle("-fx-background-color: #ccffcc;");
                            break;
                        case "2":
                            setText("已驳回");
                            setStyle("-fx-background-color: #ffcccc;");
                            break;
                        default:
                            setText(status);
                            setStyle("");
                    }
                }
            }
        });

        outOrderTable.setItems(outOrderList);

        statusComboBox.getItems().addAll("全部", "待审批", "已出库", "已驳回");
        statusComboBox.setValue("全部");

        checkUserRole();
        loadOutOrderList();
    }

    private void checkUserRole() {
        if (AppStore.getJwt() != null && AppStore.getJwt().getRole() != null) {
            String role = AppStore.getJwt().getRole();
            isAdmin = role.contains("admin") || role.contains("管理员") || role.equals("ADMIN");
        }
    }

    private void loadOutOrderList() {
        try {
            StringBuilder urlBuilder = new StringBuilder(HttpRequestUtil.serverUrl + "/api/stockOut/getAllStockOutList");
            boolean hasParam = false;

            if (!isAdmin && AppStore.getJwt() != null && AppStore.getJwt().getId() != null) {
                urlBuilder.append(hasParam ? "&" : "?").append("userId=").append(AppStore.getJwt().getId());
                hasParam = true;
            }

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
                if (resultMap.get("code").equals(200) || resultMap.get("code").equals(0)) {
                    Map<String, Object> dataMap = (Map<String, Object>) resultMap.get("data");
                    List<Map<String, Object>> dataList = (List<Map<String, Object>>) dataMap.get("records");
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
        if (!"0".equals(String.valueOf(selected.getStatus()))) {
            MessageDialog.showDialog("只能审批待审批状态的出库单");
            return;
        }

        TextInputDialog remarkDialog = new TextInputDialog();
        remarkDialog.setTitle("审批出库单");
        remarkDialog.setHeaderText("出库单号：" + selected.getOrderNo());
        remarkDialog.setContentText("请输入审批意见（可选）：");

        remarkDialog.showAndWait().ifPresent(remark -> {
            try {
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("orderId", selected.getId());
                requestBody.put("approved", true);
                if (remark != null && !remark.trim().isEmpty()) {
                    requestBody.put("remark", remark);
                }

                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(HttpRequestUtil.serverUrl + "/api/stockOut/approve"))
                        .PUT(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                        .headers("Content-Type", "application/json", "satoken", AppStore.getJwt().getToken())
                        .build();

                HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    Map<String, Object> result = gson.fromJson(response.body(), Map.class);
                    if (result.get("code").equals(200) || result.get("code").equals(0)) {
                        MessageDialog.showDialog("审批成功");
                        loadOutOrderList();
                    } else {
                        MessageDialog.showDialog("审批失败：" + result.get("msg"));
                    }
                } else {
                    MessageDialog.showDialog("审批失败");
                }
            } catch (Exception e) {
                e.printStackTrace();
                MessageDialog.showDialog("审批异常：" + e.getMessage());
            }
        });
    }

    @FXML
    protected void onRejectButtonClick() {
        OutOrder selected = outOrderTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            MessageDialog.showDialog("请选择要驳回的出库单");
            return;
        }
        if (!"0".equals(String.valueOf(selected.getStatus()))) {
            MessageDialog.showDialog("只能驳回待审批状态的出库单");
            return;
        }

        TextInputDialog rejectReasonDialog = new TextInputDialog();
        rejectReasonDialog.setTitle("驳回出库单");
        rejectReasonDialog.setHeaderText("出库单号：" + selected.getOrderNo());
        rejectReasonDialog.setContentText("请输入驳回理由（必填）：");

        rejectReasonDialog.showAndWait().ifPresent(reason -> {
            if (reason == null || reason.trim().isEmpty()) {
                MessageDialog.showDialog("驳回理由不能为空");
                return;
            }

            try {
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("orderId", selected.getId());
                requestBody.put("rejectReason", reason);

                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(HttpRequestUtil.serverUrl + "/api/stockOut/reject"))
                        .PUT(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                        .headers("Content-Type", "application/json", "satoken", AppStore.getJwt().getToken())
                        .build();

                HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    Map<String, Object> result = gson.fromJson(response.body(), Map.class);
                    if (result.get("code").equals(200) || result.get("code").equals(0)) {
                        MessageDialog.showDialog("驳回成功");
                        loadOutOrderList();
                    } else {
                        MessageDialog.showDialog("驳回失败：" + result.get("msg"));
                    }
                } else {
                    MessageDialog.showDialog("驳回失败");
                }
            } catch (Exception e) {
                e.printStackTrace();
                MessageDialog.showDialog("驳回异常：" + e.getMessage());
            }
        });
    }

    @FXML
    protected void onDeleteButtonClick() {
        OutOrder selected = outOrderTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            MessageDialog.showDialog("请选择要删除的出库单");
            return;
        }
        
        String status = String.valueOf(selected.getStatus());
        if (!"0".equals(status) && !"2".equals(status)) {
            MessageDialog.showDialog("只能删除待审批或已驳回状态的出库单");
            return;
        }

        int ret = MessageDialog.choiceDialog("确认删除出库单 " + selected.getOrderNo() + "？");
        if (ret != MessageDialog.CHOICE_YES) return;

        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(HttpRequestUtil.serverUrl + "/api/stockOut/delete/" + selected.getId()))
                    .DELETE()
                    .headers("satoken", AppStore.getJwt().getToken())
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, Object> result = gson.fromJson(response.body(), Map.class);
                if (result.get("code").equals(200) || result.get("code").equals(0)) {
                    MessageDialog.showDialog("删除成功");
                    loadOutOrderList();
                } else {
                    MessageDialog.showDialog("删除失败：" + result.get("msg"));
                }
            } else {
                MessageDialog.showDialog("删除失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            MessageDialog.showDialog("删除异常：" + e.getMessage());
        }
    }

    @FXML
    protected void onEditButtonClick() {
        OutOrder selected = outOrderTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            MessageDialog.showDialog("请选择要编辑的出库单");
            return;
        }
        
        String status = String.valueOf(selected.getStatus());
        if (!"0".equals(status)) {
            MessageDialog.showDialog("只能编辑待审批状态的出库单");
            return;
        }

        MessageDialog.showDialog("编辑功能开发中，请等待后端接口支持");
    }

    @FXML
    protected void onViewDetailButtonClick() {
        OutOrder selected = outOrderTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            MessageDialog.showDialog("请选择要查看的出库单");
            return;
        }

        StringBuilder detail = new StringBuilder();
        detail.append("==================== 出库单详情 ====================\n\n");
        detail.append("出库单号：").append(selected.getOrderNo()).append("\n");
        detail.append("申请人：").append(selected.getApplicantName()).append("\n");
        detail.append("申请时间：").append(selected.getApplyTime() != null ? selected.getApplyTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "").append("\n");
        detail.append("出库类型：").append(getOutTypeName(selected.getOutType())).append("\n");
        detail.append("状态：").append(getStatusName(selected.getStatus())).append("\n");
        detail.append("总数量：").append(selected.getTotalNum()).append("\n");
        detail.append("总金额：").append(selected.getTotalAmount()).append("\n");
        
        if (selected.getAuditUserName() != null) {
            detail.append("审批人：").append(selected.getAuditUserName()).append("\n");
        }
        if (selected.getAuditTime() != null) {
            detail.append("审批时间：").append(selected.getAuditTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("\n");
        }
        if (selected.getRejectReason() != null && !selected.getRejectReason().isEmpty()) {
            detail.append("驳回理由：").append(selected.getRejectReason()).append("\n");
        }
        if (selected.getRemark() != null && !selected.getRemark().isEmpty()) {
            detail.append("备注：").append(selected.getRemark()).append("\n");
        }
        
        detail.append("\n==================== 出库明细 ====================\n");
        detail.append("加载明细数据中...（需要后端接口支持）\n");

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("出库单详情");
        alert.setHeaderText(null);
        alert.setContentText(detail.toString());
        alert.getDialogPane().setPrefWidth(600);
        alert.getDialogPane().setPrefHeight(500);
        alert.showAndWait();
    }

    private String getStatusName(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "待审批";
            case 1: return "已出库";
            case 2: return "已驳回";
            default: return "未知";
        }
    }

    private String getOutTypeName(Integer type) {
        if (type == null) return "未知";
        switch (type) {
            case 1: return "领料出库";
            case 2: return "销售出库";
            case 3: return "报损出库";
            case 4: return "其他出库";
            default: return "未知";
        }
    }

    private Integer getStatusValue(String status) {
        switch (status) {
            case "待审批": return 0;
            case "已出库": return 1;
            case "已驳回": return 2;
            default: return null;
        }
    }
}
