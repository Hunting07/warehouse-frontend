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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OutOrderListController extends ToolController {

    @FXML
    private TextField searchOrderNoField;

    @FXML
    private TextField applicantNameField;

    @FXML
    private ComboBox<String> statusComboBox;

    @FXML
    private ComboBox<String> typeComboBox;

    @FXML
    private TableView<OutOrder> outOrderTable;

    @FXML
    private TableColumn<OutOrder, Integer> idColumn;

    @FXML
    private TableColumn<OutOrder, String> orderNoColumn;

    @FXML
    private TableColumn<OutOrder, String> outTypeColumn;

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

    private final ObservableList<OutOrder> outOrderList = FXCollections.observableArrayList();
    private final Gson gson = new Gson();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private boolean isAdmin = false;

    @FXML
    public void initialize() {
        String role = AppStore.getJwt().getRole();
        isAdmin = "admin".equals(role) || role.contains("管理员");

        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        orderNoColumn.setCellValueFactory(new PropertyValueFactory<>("orderNo"));
        
        outTypeColumn.setCellValueFactory(cellData -> {
            Integer type = cellData.getValue().getOutType();
            return new javafx.beans.property.SimpleStringProperty(getOutTypeName(type));
        });
        
        applicantNameColumn.setCellValueFactory(new PropertyValueFactory<>("applicantName"));
        
        applyTimeColumn.setCellValueFactory(cellData -> {
            LocalDateTime time = cellData.getValue().getApplyTime();
            return new javafx.beans.property.SimpleStringProperty(time != null ? time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "");
        });
        
        totalNumColumn.setCellValueFactory(new PropertyValueFactory<>("totalNum"));
        totalAmountColumn.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        
        statusColumn.setCellValueFactory(cellData -> {
            Integer status = cellData.getValue().getStatus();
            return new javafx.beans.property.SimpleStringProperty(getStatusName(status));
        });
        
        statusColumn.setCellFactory(col -> new TableCell<OutOrder, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    switch (status) {
                        case "待审批":
                            setStyle("-fx-background-color: #fff4cc;");
                            break;
                        case "已出库":
                            setStyle("-fx-background-color: #ccffcc;");
                            break;
                        case "已驳回":
                            setStyle("-fx-background-color: #ffcccc;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });
        
        auditUserNameColumn.setCellValueFactory(new PropertyValueFactory<>("auditUserName"));
        
        auditTimeColumn.setCellValueFactory(cellData -> {
            LocalDateTime time = cellData.getValue().getAuditTime();
            return new javafx.beans.property.SimpleStringProperty(time != null ? time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "");
        });
        
        remarkColumn.setCellValueFactory(new PropertyValueFactory<>("remark"));

        outOrderTable.setItems(outOrderList);

        typeComboBox.getItems().addAll("全部", "领料出库", "销售出库", "报损出库", "其他出库");
        typeComboBox.setValue("全部");

        statusComboBox.getItems().addAll("全部", "待审批", "已出库", "已驳回");
        statusComboBox.setValue("全部");

        loadOutOrderList();
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

    private Integer getTypeValue(String type) {
        switch (type) {
            case "领料出库": return 1;
            case "销售出库": return 2;
            case "报损出库": return 3;
            case "其他出库": return 4;
            default: return null;
        }
    }

    private void loadOutOrderList() {
        new Thread(() -> {
            try {
                final String currentStatus = statusComboBox.getValue();
                final String currentType = typeComboBox.getValue();
                final String currentApplicantName = applicantNameField.getText();
                final String currentOrderNo = searchOrderNoField.getText();
                
                StringBuilder urlBuilder = new StringBuilder(HttpRequestUtil.serverUrl + "/api/stockOut/getAllStockOutList");
                boolean hasParam = false;

                if (!isAdmin && AppStore.getJwt() != null && AppStore.getJwt().getId() != null) {
                    urlBuilder.append(hasParam ? "&" : "?").append("userId=").append(AppStore.getJwt().getId());
                    hasParam = true;
                }

                if (currentStatus != null && !currentStatus.equals("全部")) {
                    urlBuilder.append(hasParam ? "&" : "?").append("status=").append(getStatusValue(currentStatus));
                    hasParam = true;
                }

                if (currentType != null && !currentType.equals("全部")) {
                    urlBuilder.append(hasParam ? "&" : "?").append("outType=").append(getTypeValue(currentType));
                    hasParam = true;
                }

                if (currentApplicantName != null && !currentApplicantName.trim().isEmpty()) {
                    urlBuilder.append(hasParam ? "&" : "?").append("applicantName=").append(currentApplicantName.trim());
                    hasParam = true;
                }

                if (currentOrderNo != null && !currentOrderNo.trim().isEmpty()) {
                    urlBuilder.append(hasParam ? "&" : "?").append("orderNo=").append(currentOrderNo.trim());
                    hasParam = true;
                }

                System.out.println("=== [前端] 加载出库列表 ===");
                System.out.println("请求URL: " + urlBuilder.toString());
                System.out.println("Token: " + AppStore.getJwt().getToken());
                System.out.println("用户角色: " + AppStore.getJwt().getRole());

                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(urlBuilder.toString()))
                        .GET()
                        .headers("satoken", AppStore.getJwt().getToken())
                        .build();

                HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                System.out.println("响应状态码: " + response.statusCode());
                System.out.println("响应内容: " + response.body());

                if (response.statusCode() == 200) {
                    Map<String, Object> resultMap = gson.fromJson(response.body(), new TypeToken<Map<String, Object>>(){}.getType());
                    Object codeObj = resultMap.get("code");
                    int code = codeObj instanceof Number ? ((Number) codeObj).intValue() : -1;
                    
                    if (code == 200 || code == 0) {
                        Map<String, Object> dataMap = (Map<String, Object>) resultMap.get("data");
                        List<Map<String, Object>> dataList = (List<Map<String, Object>>) dataMap.get("records");
                        if (dataList != null) {
                            System.out.println("成功加载 " + dataList.size() + " 条数据");
                            
                            List<OutOrder> list = new ArrayList<>();
                            for (Map<String, Object> map : dataList) {
                                OutOrder order = new OutOrder();
                                order.setId(((Number) map.get("id")).intValue());
                                order.setOrderNo((String) map.get("orderNo"));
                                order.setOutType(((Number) map.get("outType")).intValue());
                                order.setApplicantId(((Number) map.get("applicantId")).intValue());
                                order.setApplicantName((String) map.get("applicantName"));
                                
                                String applyTimeStr = (String) map.get("applyTime");
                                if (applyTimeStr != null) {
                                    order.setApplyTime(java.time.LocalDateTime.parse(applyTimeStr));
                                }
                                
                                order.setStatus(((Number) map.get("status")).intValue());
                                order.setAuditUserId(map.get("auditUserId") != null ? ((Number) map.get("auditUserId")).intValue() : null);
                                order.setAuditUserName((String) map.get("auditUserName"));
                                
                                String auditTimeStr = (String) map.get("auditTime");
                                if (auditTimeStr != null) {
                                    order.setAuditTime(java.time.LocalDateTime.parse(auditTimeStr));
                                }
                                
                                order.setRemark((String) map.get("remark"));
                                order.setRejectReason((String) map.get("rejectReason"));
                                
                                list.add(order);
                            }
                            
                            javafx.application.Platform.runLater(() -> {
                                outOrderList.setAll(list);
                            });
                        } else {
                            System.out.println("records 为 null");
                            javafx.application.Platform.runLater(() -> {
                                outOrderList.clear();
                            });
                        }
                    } else {
                        final String errorMsg = resultMap.get("msg") != null ? resultMap.get("msg").toString() : "未知错误";
                        System.out.println("业务错误: " + errorMsg);
                        javafx.application.Platform.runLater(() -> {
                            MessageDialog.showDialog("加载数据失败：" + errorMsg);
                        });
                    }

                } else {
                    String errorMsg = "请求失败，状态码：" + response.statusCode();
                    if (response.body() != null && !response.body().isEmpty()) {
                        try {
                            Map<String, Object> errorResult = gson.fromJson(response.body(), new TypeToken<Map<String, Object>>(){}.getType());
                            if (errorResult.get("msg") != null) {
                                errorMsg += "\n错误信息：" + errorResult.get("msg");
                            }
                        } catch (Exception e) {
                            errorMsg += "\n响应内容：" + response.body();
                        }
                    }
                    final String finalErrorMsg = errorMsg;
                    System.out.println("HTTP错误: " + finalErrorMsg);
                    javafx.application.Platform.runLater(() -> {
                        MessageDialog.showDialog(finalErrorMsg);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("网络异常: " + e.getMessage());
                javafx.application.Platform.runLater(() -> {
                    MessageDialog.showDialog("加载数据异常：" + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    protected void onSearchButtonClick() {
        loadOutOrderList();
    }

    @FXML
    protected void onResetButtonClick() {
        searchOrderNoField.clear();
        applicantNameField.clear();
        typeComboBox.setValue("全部");
        statusComboBox.setValue("全部");
        loadOutOrderList();
    }

    @FXML
    protected void onAddButtonClick() {
        try {
            OutOrderEditDialog dialog = OutOrderEditDialog.createNewDialog();
            if (dialog != null) {
                dialog.showAndWait();
                
                // 延迟500毫秒后再刷新，确保数据库事务已提交
                new Thread(() -> {
                    try {
                        Thread.sleep(500);
                        javafx.application.Platform.runLater(() -> {
                            System.out.println("=== 刷新出库列表 ===");
                            loadOutOrderList();
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
            MessageDialog.showDialog("打开新增窗口失败：" + e.getMessage());
        }
    }

    @FXML
    protected void onEditButtonClick() {
        OutOrder selected = outOrderTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            MessageDialog.showDialog("请选择要编辑的出库单");
            return;
        }

        if (selected.getStatus() != 0 && selected.getStatus() != 2) {
            MessageDialog.showDialog("只能编辑待审批或已驳回状态的出库单");
            return;
        }

        String loginIdStr = AppStore.getJwt().getLoginId();
        if (loginIdStr == null || loginIdStr.isEmpty()) {
            MessageDialog.showDialog("会话已过期，请重新登录");
            return;
        }

        Integer currentUserId;
        try {
            if (loginIdStr.contains(".")) {
                currentUserId = (int) Double.parseDouble(loginIdStr);
            } else {
                currentUserId = Integer.valueOf(loginIdStr);
            }
        } catch (NumberFormatException e) {
            MessageDialog.showDialog("会话数据异常，请重新登录");
            return;
        }

        if (!isAdmin && !selected.getApplicantId().equals(currentUserId)) {
            MessageDialog.showDialog("只能编辑自己创建的出库单");
            return;
        }

        if (selected.getStatus() == 2) {
            String rejectReason = selected.getRejectReason();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("驳回理由");
            alert.setHeaderText("该出库单已被驳回");
            String message = rejectReason != null && !rejectReason.isEmpty() 
                ? "驳回理由：\n\n" + rejectReason + "\n\n点击确定后进入编辑模式"
                : "该出库单已被驳回\n\n点击确定后进入编辑模式";
            alert.setContentText(message);
            alert.showAndWait();
        }

        try {
            OutOrderEditDialog dialog = OutOrderEditDialog.createEditDialog(selected);
            if (dialog != null) {
                dialog.showAndWait();
                loadOutOrderList();
            }
        } catch (Exception e) {
            e.printStackTrace();
            MessageDialog.showDialog("打开编辑窗口失败：" + e.getMessage());
        }
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

        String loginIdStr = AppStore.getJwt().getLoginId();
        if (loginIdStr == null || loginIdStr.isEmpty()) {
            MessageDialog.showDialog("会话已过期，请重新登录");
            return;
        }

        Integer currentUserId;
        try {
            if (loginIdStr.contains(".")) {
                currentUserId = (int) Double.parseDouble(loginIdStr);
            } else {
                currentUserId = Integer.valueOf(loginIdStr);
            }
        } catch (NumberFormatException e) {
            MessageDialog.showDialog("会话数据异常，请重新登录");
            return;
        }

        if (!isAdmin && !selected.getApplicantId().equals(currentUserId)) {
            MessageDialog.showDialog("只能删除自己创建的出库单");
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
    protected void onApproveButtonClick() {
        if (!isAdmin) {
            MessageDialog.showDialog("只有管理员可以审批");
            return;
        }

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
                        MessageDialog.showDialog("审批通过");
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
        if (!isAdmin) {
            MessageDialog.showDialog("只有管理员可以驳回");
            return;
        }

        OutOrder selected = outOrderTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            MessageDialog.showDialog("请选择要驳回的出库单");
            return;
        }
        if (!"0".equals(String.valueOf(selected.getStatus()))) {
            MessageDialog.showDialog("只能驳回待审批状态的出库单");
            return;
        }

        TextInputDialog remarkDialog = new TextInputDialog();
        remarkDialog.setTitle("驳回出库单");
        remarkDialog.setHeaderText("出库单号：" + selected.getOrderNo());
        remarkDialog.setContentText("请输入驳回理由（必填）：");

        remarkDialog.showAndWait().ifPresent(remark -> {
            if (remark == null || remark.trim().isEmpty()) {
                MessageDialog.showDialog("驳回理由不能为空");
                return;
            }

            try {
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("orderId", selected.getId());
                requestBody.put("approved", false);
                requestBody.put("rejectReason", remark);

                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(HttpRequestUtil.serverUrl + "/api/stockOut/approve"))
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
    protected void onCompleteButtonClick() {
        if (!isAdmin) {
            MessageDialog.showDialog("只有管理员可以确认出库");
            return;
        }

        OutOrder selected = outOrderTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            MessageDialog.showDialog("请选择要确认出库的出库单");
            return;
        }
        if (!"1".equals(String.valueOf(selected.getStatus()))) {
            MessageDialog.showDialog("只能确认已审批通过的出库单");
            return;
        }

        int ret = MessageDialog.choiceDialog("确认出库单 " + selected.getOrderNo() + " 已完成出库？");
        if (ret != MessageDialog.CHOICE_YES) return;

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("orderId", selected.getId());

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(HttpRequestUtil.serverUrl + "/api/stockOut/complete"))
                    .PUT(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                    .headers("Content-Type", "application/json", "satoken", AppStore.getJwt().getToken())
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, Object> result = gson.fromJson(response.body(), Map.class);
                if (result.get("code").equals(200) || result.get("code").equals(0)) {
                    MessageDialog.showDialog("确认出库成功");
                    loadOutOrderList();
                } else {
                    MessageDialog.showDialog("确认失败：" + result.get("msg"));
                }
            } else {
                MessageDialog.showDialog("确认失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            MessageDialog.showDialog("确认异常：" + e.getMessage());
        }
    }

    @FXML
    protected void onViewDetailButtonClick() {
        OutOrder selected = outOrderTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            MessageDialog.showDialog("请选择要查看的出库单");
            return;
        }

        try {
            String url = HttpRequestUtil.serverUrl + "/api/stockOut/getDetails/" + selected.getId();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .headers("satoken", AppStore.getJwt().getToken())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, Object> result = gson.fromJson(response.body(), Map.class);
                if (result.get("code").equals(200) || result.get("code").equals(0)) {
                    List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");
                    
                    StringBuilder detail = new StringBuilder();
                    detail.append("出库单号：").append(selected.getOrderNo()).append("\n");
                    detail.append("出库类型：").append(getOutTypeName(selected.getOutType())).append("\n");
                    detail.append("申请人：").append(selected.getApplicantName()).append("\n");
                    detail.append("申请时间：").append(selected.getApplyTime()).append("\n");
                    detail.append("状态：").append(getStatusName(selected.getStatus())).append("\n");
                    detail.append("\n=== 出库明细 ===\n\n");
                    
                    if (data != null && !data.isEmpty()) {
                        int index = 1;
                        for (Map<String, Object> item : data) {
                            detail.append(index++).append(". ");
                            detail.append(item.get("goodsName")).append(" ");
                            detail.append(item.get("goodsSpec")).append(" ");
                            detail.append(item.get("unit")).append(" ");
                            detail.append("×").append(item.get("outNum")).append("\n");
                        }
                    } else {
                        detail.append("暂无明细");
                    }
                    
                    detail.append("\n备注：").append(selected.getRemark() != null ? selected.getRemark() : "无");
                    
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("出库单明细");
                    alert.setHeaderText("出库单详情");
                    alert.setContentText(detail.toString());
                    alert.getDialogPane().setPrefSize(500, 400);
                    alert.showAndWait();
                } else {
                    MessageDialog.showDialog("加载失败：" + result.get("msg"));
                }
            } else {
                MessageDialog.showDialog("请求失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            MessageDialog.showDialog("加载异常：" + e.getMessage());
        }
    }

    @Override
    public void doRefresh() {
        loadOutOrderList();
    }
}
