package com.teach.javafx.controller.base;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.teach.javafx.AppStore;
import com.teach.javafx.models.StockIn;
import com.teach.javafx.request.HttpRequestUtil;
import com.teach.javafx.GsonUtil;
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

public class StockInController extends ToolController {

    @FXML
    private TextField searchCodeField;

    @FXML
    private ComboBox<String> typeComboBox;

    @FXML
    private ComboBox<String> statusComboBox;

    @FXML
    private TableView<StockIn> stockInTable;

    @FXML
    private TableColumn<StockIn, Integer> idColumn;

    @FXML
    private TableColumn<StockIn, String> inCodeColumn;

    @FXML
    private TableColumn<StockIn, String> typeColumn;

    @FXML
    private TableColumn<StockIn, BigDecimal> totalAmountColumn;

    @FXML
    private TableColumn<StockIn, String> statusColumn;

    @FXML
    private TableColumn<StockIn, String> applyUserColumn;

    @FXML
    private TableColumn<StockIn, LocalDateTime> createTimeColumn;

    @FXML
    private TableColumn<StockIn, LocalDateTime> approveTimeColumn;

    private final ObservableList<StockIn> stockInList = FXCollections.observableArrayList();
    private final Gson gson = GsonUtil.getGson();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private boolean isAdmin = false;

    @FXML
    public void initialize() {
        String role = AppStore.getJwt().getRole();
        isAdmin = "admin".equals(role);

        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        inCodeColumn.setCellValueFactory(new PropertyValueFactory<>("inCode"));
        typeColumn.setCellValueFactory(cellData -> {
            StockIn stockIn = cellData.getValue();
            String typeName = getTypeName(stockIn.getType());
            return new javafx.beans.property.SimpleStringProperty(typeName);
        });
        totalAmountColumn.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        statusColumn.setCellValueFactory(cellData -> {
            StockIn stockIn = cellData.getValue();
            String statusName = getStatusName(stockIn.getStatus());
            return new javafx.beans.property.SimpleStringProperty(statusName);
        });
        applyUserColumn.setCellValueFactory(new PropertyValueFactory<>("applyUserName"));
        createTimeColumn.setCellValueFactory(new PropertyValueFactory<>("createTime"));
        approveTimeColumn.setCellValueFactory(new PropertyValueFactory<>("approveTime"));

        stockInTable.setItems(stockInList);

        typeComboBox.getItems().addAll("全部", "采购入库", "退货入库", "其他入库");
        typeComboBox.setValue("全部");

        statusComboBox.getItems().addAll("全部", "待审批", "已批准", "已驳回", "已完成");
        statusComboBox.setValue("全部");

        loadStockInList();
    }

    private String getTypeName(Integer type) {
        if (type == null) return "";
        switch (type) {
            case 1: return "采购入库";
            case 2: return "退货入库";
            case 3: return "其他入库";
            default: return "未知";
        }
    }

    private String getStatusName(Integer status) {
        if (status == null) return "";
        switch (status) {
            case 0: return "待审批";
            case 1: return "已批准";
            case 2: return "已驳回";
            case 3: return "已完成";
            default: return "未知";
        }
    }

    private void loadStockInList() {
        try {
            StringBuilder urlBuilder = new StringBuilder(HttpRequestUtil.serverUrl + "/stock-in/list");
            boolean hasParam = false;

            String status = statusComboBox.getValue();
            if (status != null && !status.equals("全部")) {
                urlBuilder.append(hasParam ? "&" : "?").append("status=").append(getStatusValue(status));
                hasParam = true;
            }

            String type = typeComboBox.getValue();
            if (type != null && !type.equals("全部")) {
                urlBuilder.append(hasParam ? "&" : "?").append("type=").append(getTypeValue(type));
                hasParam = true;
            }

            String searchCode = searchCodeField.getText();
            if (searchCode != null && !searchCode.trim().isEmpty()) {
                urlBuilder.append(hasParam ? "&" : "?").append("inCode=").append(searchCode.trim());
                hasParam = true;
            }

            System.out.println("=== [前端] 加载入库列表 ===");
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
                if (resultMap.get("code").equals(200.0)) {
                    List<Map<String, Object>> dataList = (List<Map<String, Object>>) resultMap.get("data");
                    List<StockIn> list = gson.fromJson(gson.toJson(dataList), new TypeToken<List<StockIn>>(){}.getType());
                    stockInList.setAll(list);
                    System.out.println("成功加载 " + list.size() + " 条数据");
                } else {
                    MessageDialog.showDialog("加载数据失败：" + resultMap.get("msg"));
                    System.out.println("业务错误: " + resultMap.get("msg"));
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
                MessageDialog.showDialog(errorMsg);
                System.out.println("HTTP错误: " + errorMsg);
            }
        } catch (Exception e) {
            e.printStackTrace();
            MessageDialog.showDialog("加载数据异常：" + e.getMessage());
            System.out.println("网络异常: " + e.getMessage());
        }
    }

    @FXML
    protected void onSearchButtonClick() {
        loadStockInList();
    }

    @FXML
    protected void onResetButtonClick() {
        searchCodeField.clear();
        typeComboBox.setValue("全部");
        statusComboBox.setValue("全部");
        loadStockInList();
    }

    @FXML
    protected void onAddButtonClick() {
        try {
            StockInEditDialog dialog = StockInEditDialog.createNewDialog();
            if (dialog != null) {
                dialog.showAndWait();
                loadStockInList();
            }
        } catch (Exception e) {
            e.printStackTrace();
            MessageDialog.showDialog("打开新增窗口失败：" + e.getMessage());
        }
    }

    @FXML
    protected void onEditButtonClick() {
        System.out.println("\n\n========== 编辑按钮被点击 ==========");
        
        StockIn selected = stockInTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
            alert.setTitle("警告");
            alert.setHeaderText(null);
            alert.setContentText("请选择要编辑的入库单");
            alert.showAndWait();
            return;
        }

        System.out.println("选中的入库单: ID=" + selected.getId() + ", 状态=" + selected.getStatus());

        // 允许编辑待审批和已驳回的入库单
        if (selected.getStatus() != 0 && selected.getStatus() != 2) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
            alert.setTitle("警告");
            alert.setHeaderText(null);
            alert.setContentText("只能编辑待审批或已驳回状态的入库单");
            alert.showAndWait();
            return;
        }

        // 检查登录ID是否为空
        String loginIdStr = AppStore.getJwt().getLoginId();
        if (loginIdStr == null || loginIdStr.isEmpty()) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText(null);
            alert.setContentText("会话已过期，请重新登录");
            alert.showAndWait();
            return;
        }

        // 正确处理数字格式（可能是 "4" 或 "4.0"）
        Integer currentUserId;
        try {
            if (loginIdStr.contains(".")) {
                currentUserId = (int) Double.parseDouble(loginIdStr);
            } else {
                currentUserId = Integer.valueOf(loginIdStr);
            }
        } catch (NumberFormatException e) {
            System.err.println("用户ID格式错误: " + loginIdStr);
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText(null);
            alert.setContentText("会话数据异常，请重新登录");
            alert.showAndWait();
            return;
        }

        System.out.println("当前用户ID: " + currentUserId + ", 申请人ID: " + selected.getApplyUserId());

        if (!isAdmin && !selected.getApplyUserId().equals(currentUserId)) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
            alert.setTitle("警告");
            alert.setHeaderText(null);
            alert.setContentText("只能编辑自己创建的入库单");
            alert.showAndWait();
            return;
        }

        // 如果是已驳回状态，先显示驳回理由
        if (selected.getStatus() == 2) {
            String rejectReason = selected.getRejectReason();
            System.out.println("入库单已驳回，驳回理由: " + rejectReason);
            
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
            alert.setTitle("驳回理由");
            alert.setHeaderText("该入库单已被驳回");
            
            String message = rejectReason != null && !rejectReason.isEmpty() 
                ? "驳回理由：\n\n" + rejectReason + "\n\n点击确定后进入编辑模式"
                : "该入库单已被驳回（无驳回理由）\n\n点击确定后进入编辑模式";
            
            alert.setContentText(message);
            alert.showAndWait();
            
            System.out.println("驳回理由弹窗已关闭，准备打开编辑对话框...");
        }

        System.out.println("开始创建编辑对话框...");
        
        try {
            StockInEditDialog dialog = StockInEditDialog.createEditDialog(selected);
            System.out.println("对话框创建结果: " + dialog);
            
            if (dialog != null) {
                System.out.println("显示对话框...");
                dialog.showAndWait();
                System.out.println("对话框关闭，刷新列表...");
                loadStockInList();
            } else {
                System.out.println("对话框创建失败");
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                alert.setTitle("错误");
                alert.setHeaderText(null);
                alert.setContentText("打开编辑窗口失败");
                alert.showAndWait();
            }
        } catch (Exception e) {
            System.err.println("打开编辑窗口异常: " + e.getMessage());
            e.printStackTrace();
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText(null);
            alert.setContentText("打开编辑窗口失败：" + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    protected void onDeleteButtonClick() {
        System.out.println("========== 删除按钮被点击 ==========");
        
        StockIn selected = stockInTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
            alert.setTitle("警告");
            alert.setHeaderText(null);
            alert.setContentText("请选择要删除的入库单");
            alert.showAndWait();
            return;
        }

        System.out.println("选中的入库单: ID=" + selected.getId() + ", 状态=" + selected.getStatus());

        if (selected.getStatus() != 0) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
            alert.setTitle("警告");
            alert.setHeaderText(null);
            alert.setContentText("只能删除待审批状态的入库单");
            alert.showAndWait();
            return;
        }

        // 检查登录ID
        String loginIdStr = AppStore.getJwt().getLoginId();
        if (loginIdStr == null || loginIdStr.isEmpty()) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText(null);
            alert.setContentText("会话已过期，请重新登录");
            alert.showAndWait();
            return;
        }

        // 正确处理数字格式
        Integer currentUserId;
        try {
            if (loginIdStr.contains(".")) {
                currentUserId = (int) Double.parseDouble(loginIdStr);
            } else {
                currentUserId = Integer.valueOf(loginIdStr);
            }
        } catch (NumberFormatException e) {
            System.err.println("用户ID格式错误: " + loginIdStr);
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText(null);
            alert.setContentText("会话数据异常，请重新登录");
            alert.showAndWait();
            return;
        }

        System.out.println("当前用户ID: " + currentUserId + ", 申请人ID: " + selected.getApplyUserId());

        if (!isAdmin && !selected.getApplyUserId().equals(currentUserId)) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
            alert.setTitle("警告");
            alert.setHeaderText(null);
            alert.setContentText("只能删除自己创建的入库单");
            alert.showAndWait();
            return;
        }

        // 确认删除
        javafx.scene.control.Alert confirmAlert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("确认删除");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("确认删除入库单 " + selected.getInCode() + "？");
        
        java.util.Optional<javafx.scene.control.ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get().getButtonData() != javafx.scene.control.ButtonBar.ButtonData.OK_DONE) {
            System.out.println("用户取消删除");
            return;
        }

        System.out.println("开始删除入库单...");

        try {
            // 后端接口是 DELETE /stock-in/delete/{id}
            String url = HttpRequestUtil.serverUrl + "/stock-in/delete/" + selected.getId();
            
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .DELETE()
                    .headers("satoken", AppStore.getJwt().getToken())
                    .build();

            System.out.println("删除请求: DELETE " + url);

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            System.out.println("响应状态码: " + response.statusCode());
            System.out.println("响应内容: " + response.body());

            if (response.statusCode() == 200) {
                Map<String, Object> resultMap = gson.fromJson(response.body(), new TypeToken<Map<String, Object>>(){}.getType());
                if (resultMap.get("code").equals(200.0)) {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                    alert.setTitle("成功");
                    alert.setHeaderText(null);
                    alert.setContentText("删除成功");
                    alert.showAndWait();
                    
                    loadStockInList();
                } else {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                    alert.setTitle("错误");
                    alert.setHeaderText(null);
                    alert.setContentText("删除失败：" + resultMap.get("msg"));
                    alert.showAndWait();
                }
            } else {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                alert.setTitle("错误");
                alert.setHeaderText(null);
                alert.setContentText("请求失败，状态码：" + response.statusCode());
                alert.showAndWait();
            }
        } catch (Exception e) {
            System.err.println("删除异常: " + e.getMessage());
            e.printStackTrace();
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText(null);
            alert.setContentText("删除异常：" + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    protected void onApproveButtonClick() {
        if (!isAdmin) {
            MessageDialog.showDialog("只有管理员可以审批入库单");
            return;
        }

        StockIn selected = stockInTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            MessageDialog.showDialog("请选择要审批的入库单");
            return;
        }

        if (selected.getStatus() != 0) {
            MessageDialog.showDialog("只能审批待审批状态的入库单");
            return;
        }

        showApproveDialog(selected);
    }

    private void showApproveDialog(StockIn stockIn) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("入库单审批");
        dialog.setHeaderText("审批入库单：" + stockIn.getInCode());

        ButtonType approveButtonType = new ButtonType("批准", ButtonBar.ButtonData.OK_DONE);
        ButtonType rejectButtonType = new ButtonType("驳回", ButtonBar.ButtonData.OTHER);
        ButtonType cancelButtonType = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);

        dialog.getDialogPane().getButtonTypes().addAll(approveButtonType, rejectButtonType, cancelButtonType);

        TextArea rejectReasonArea = new TextArea();
        rejectReasonArea.setPromptText("请输入驳回理由（仅驳回时需要）");
        rejectReasonArea.setPrefRowCount(4);
        rejectReasonArea.setPrefWidth(400);

        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(10);
        content.getChildren().addAll(
            new Label("申请人：" + stockIn.getApplyUserName()),
            new Label("入库类型：" + getTypeName(stockIn.getType())),
            new Label("总金额：" + stockIn.getTotalAmount()),
            new Label("申请时间：" + stockIn.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))),
            new javafx.scene.control.Separator(),
            new Label("驳回理由："),
            rejectReasonArea
        );

        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == approveButtonType) {
                return true;
            } else if (dialogButton == rejectButtonType) {
                return false;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(approved -> {
            if (approved) {
                approveStockIn(stockIn, true, null);
            } else {
                String rejectReason = rejectReasonArea.getText();
                if (rejectReason == null || rejectReason.trim().isEmpty()) {
                    MessageDialog.showDialog("请填写驳回理由");
                    return;
                }
                approveStockIn(stockIn, false, rejectReason);
            }
        });
    }

    private void approveStockIn(StockIn stockIn, boolean approved, String rejectReason) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("stockInId", stockIn.getId());
            requestBody.put("approved", approved);
            if (rejectReason != null) {
                requestBody.put("rejectReason", rejectReason);
            }

            System.out.println("\n=== [前端] 审批请求 ===");
            System.out.println("请求URL: " + HttpRequestUtil.serverUrl + "/stock-in/approve");
            System.out.println("请求方法: POST");
            System.out.println("Token: " + AppStore.getJwt().getToken());
            System.out.println("Token长度: " + (AppStore.getJwt().getToken() != null ? AppStore.getJwt().getToken().length() : 0));
            System.out.println("请求体: " + gson.toJson(requestBody));
            System.out.println("入库单号: " + stockIn.getInCode());
            System.out.println("审批结果: " + (approved ? "批准" : "驳回"));

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(HttpRequestUtil.serverUrl + "/stock-in/approve"))
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                    .headers("Content-Type", "application/json", "satoken", AppStore.getJwt().getToken())
                    .build();

            System.out.println("发送请求...");
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            System.out.println("接收响应...");
            System.out.println("响应状态码: " + response.statusCode());
            System.out.println("响应头: " + response.headers().map());
            System.out.println("响应内容: " + response.body());

            if (response.statusCode() == 200) {
                Map<String, Object> resultMap = gson.fromJson(response.body(), new TypeToken<Map<String, Object>>(){}.getType());
                System.out.println("解析后的结果: code=" + resultMap.get("code") + ", msg=" + resultMap.get("msg"));
                
                if (resultMap.get("code").equals(200.0)) {
                    MessageDialog.showDialog(approved ? "审批通过，库存已自动更新" : "已驳回该入库单");
                    System.out.println("✅ 审批成功");
                    loadStockInList();
                } else {
                    MessageDialog.showDialog("审批失败：" + resultMap.get("msg"));
                    System.out.println("❌ 业务错误: " + resultMap.get("msg"));
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
                MessageDialog.showDialog(errorMsg);
                System.out.println("❌ HTTP错误: " + errorMsg);
            }
        } catch (Exception e) {
            e.printStackTrace();
            MessageDialog.showDialog("审批异常：" + e.getMessage());
            System.out.println("❌ 网络异常: " + e.getMessage());
            System.out.println("异常类型: " + e.getClass().getName());
        }
    }

    @FXML
    protected void onCompleteButtonClick() {
        if (!isAdmin) {
            MessageDialog.showDialog("只有管理员可以确认入库");
            return;
        }

        StockIn selected = stockInTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            MessageDialog.showDialog("请选择要入库的入库单");
            return;
        }

        if (selected.getStatus() != 1) {
            MessageDialog.showDialog("只能确认已批准状态的入库单");
            return;
        }

        int ret = MessageDialog.choiceDialog("确认完成入库 " + selected.getInCode() + "？");
        if (ret != MessageDialog.CHOICE_YES) return;

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("stockInId", selected.getId());

            System.out.println("\n=== [前端] 确认入库请求 ===");
            System.out.println("请求URL: " + HttpRequestUtil.serverUrl + "/stock-in/complete");
            System.out.println("请求方法: POST");
            System.out.println("Token: " + AppStore.getJwt().getToken());
            System.out.println("请求体: " + gson.toJson(requestBody));
            System.out.println("入库单号: " + selected.getInCode());
            System.out.println("入库单ID: " + selected.getId());

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(HttpRequestUtil.serverUrl + "/stock-in/complete"))
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                    .headers("Content-Type", "application/json", "satoken", AppStore.getJwt().getToken())
                    .build();

            System.out.println("发送请求...");
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            System.out.println("接收响应...");
            System.out.println("响应状态码: " + response.statusCode());
            System.out.println("响应头: " + response.headers().map());
            System.out.println("响应内容: " + response.body());

            if (response.statusCode() == 200) {
                Map<String, Object> resultMap = gson.fromJson(response.body(), new TypeToken<Map<String, Object>>(){}.getType());
                System.out.println("解析后的结果: code=" + resultMap.get("code") + ", msg=" + resultMap.get("msg"));
                
                if (resultMap.get("code").equals(200.0)) {
                    MessageDialog.showDialog("入库完成");
                    System.out.println("✅ 入库成功");
                    loadStockInList();
                } else {
                    MessageDialog.showDialog("入库失败：" + resultMap.get("msg"));
                    System.out.println("❌ 业务错误: " + resultMap.get("msg"));
                }
            } else {
                String errorMsg = "请求失败，状态码：" + response.statusCode();
                if (response.body() != null && !response.body().isEmpty()) {
                    try {
                        Map<String, Object> errorResult = gson.fromJson(response.body(), new TypeToken<Map<String, Object>>(){}.getType());
                        if (errorResult.get("msg") != null) {
                            errorMsg += "\n错误信息：" + errorResult.get("msg");
                        }
                        if (errorResult.get("data") != null) {
                            errorMsg += "\n详细信息：" + errorResult.get("data");
                        }
                    } catch (Exception e) {
                        errorMsg += "\n响应内容：" + response.body();
                    }
                }
                MessageDialog.showDialog(errorMsg);
                System.out.println("❌ HTTP错误: " + errorMsg);
            }
        } catch (Exception e) {
            e.printStackTrace();
            MessageDialog.showDialog("入库异常：" + e.getMessage());
            System.out.println("❌ 网络异常: " + e.getMessage());
            System.out.println("异常类型: " + e.getClass().getName());
        }
    }

    @Override
    public void doRefresh() {
        loadStockInList();
    }

    private Integer getStatusValue(String status) {
        switch (status) {
            case "待审批": return 0;
            case "已批准": return 1;
            case "已驳回": return 2;
            case "已完成": return 3;
            default: return null;
        }
    }

    private Integer getTypeValue(String type) {
        switch (type) {
            case "采购入库": return 1;
            case "退货入库": return 2;
            case "其他入库": return 3;
            default: return null;
        }
    }
}
