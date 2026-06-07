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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StockInController extends ToolController {

    @FXML
    private Label titleLabel;
    
    @FXML
    private Label subtitleLabel;
    
    @FXML
    private Button editButton;
    
    @FXML
    private Button approveButton;
    
    @FXML
    private Button deleteButton;

    @FXML
    private TextField searchCodeField;

    @FXML
    private ComboBox<String> typeComboBox;

    @FXML
    private ComboBox<String> statusComboBox;

    @FXML
    private TableView<StockIn> stockInTable;

    @FXML
    private TableColumn<StockIn, Integer> serialNumberColumn;

    @FXML
    private TableColumn<StockIn, String> inCodeColumn;

    @FXML
    private TableColumn<StockIn, String> typeColumn;

    @FXML
    private TableColumn<StockIn, String> materialNameColumn;

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
    
    @FXML
    private TableColumn<StockIn, Void> actionColumn;

    private final ObservableList<StockIn> stockInList = FXCollections.observableArrayList();
    private final Gson gson = GsonUtil.getGson();
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .build();
    private boolean isAdmin = false;

    @FXML
    public void initialize() {
        String role = AppStore.getJwt().getRole();
        isAdmin = "admin".equals(role);

        if (isAdmin) {
            titleLabel.setText("入库审批管理");
            subtitleLabel.setText("管理和审批所有入库单据");
            editButton.setVisible(false);
            editButton.setManaged(false);
        } else {
            titleLabel.setText("入库申请管理");
            subtitleLabel.setText("管理和申请自己的入库单据");
            approveButton.setVisible(false);
            approveButton.setManaged(false);
        }

        // 序号列：倒序显示
        serialNumberColumn.setCellValueFactory(data -> {
            int index = stockInTable.getItems().indexOf(data.getValue());
            int total = stockInTable.getItems().size();
            return new javafx.beans.property.SimpleIntegerProperty(total - index).asObject();
        });

        inCodeColumn.setCellValueFactory(new PropertyValueFactory<>("inCode"));
        typeColumn.setCellValueFactory(cellData -> {
            StockIn stockIn = cellData.getValue();
            String typeName = getTypeName(stockIn.getType());
            return new javafx.beans.property.SimpleStringProperty(typeName);
        });
        materialNameColumn.setCellValueFactory(new PropertyValueFactory<>("materialName"));
        totalAmountColumn.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        statusColumn.setCellValueFactory(cellData -> {
            StockIn stockIn = cellData.getValue();
            String statusName = getStatusName(stockIn.getStatus());
            return new javafx.beans.property.SimpleStringProperty(statusName);
        });
        applyUserColumn.setCellValueFactory(new PropertyValueFactory<>("applyUserName"));
        createTimeColumn.setCellValueFactory(new PropertyValueFactory<>("createTime"));
        approveTimeColumn.setCellValueFactory(new PropertyValueFactory<>("approveTime"));

        // 设置操作列
        setupActionColumn();

        stockInTable.setItems(stockInList);

        typeComboBox.getItems().addAll("全部", "采购入库", "退货入库", "其他入库");
        typeComboBox.setValue("全部");

        statusComboBox.getItems().addAll("全部", "待审批", "已批准", "已驳回", "已完成");
        statusComboBox.setValue("全部");

        loadStockInList();
    }

    /**
     * 设置操作列
     */
    private void setupActionColumn() {
        actionColumn.setCellFactory(col -> new TableCell<StockIn, Void>() {
            private final Button viewBtn = new Button("查看明细");

            {
                viewBtn.setStyle("-fx-background-color: #4096ff; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 4 12; -fx-background-radius: 4; -fx-cursor: hand;");
                viewBtn.setOnAction(event -> {
                    StockIn stockIn = getTableRow().getItem();
                    if (stockIn != null) {
                        onViewButtonClick(stockIn);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                
                if (empty || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }

                HBox hbox = new HBox(5);
                hbox.setAlignment(Pos.CENTER);
                hbox.getChildren().add(viewBtn);
                setGraphic(hbox);
            }
        });
    }

    /**
     * 查看入库单详情
     */
    private void onViewButtonClick(StockIn stockIn) {
        StockInViewDialog.showDialog(stockIn);
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
            case 1: return "已入库";
            case 2: return "已驳回";
            case 3: return "已入库";
            default: return "未知";
        }
    }

    private void loadStockInList() {
        new Thread(() -> {
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

                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(urlBuilder.toString()))
                        .GET()
                        .headers("satoken", AppStore.getJwt().getToken())
                        .build();

                HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    Map<String, Object> resultMap = gson.fromJson(response.body(), new TypeToken<Map<String, Object>>(){}.getType());
                    if (resultMap.get("code").equals(200.0)) {
                        List<Map<String, Object>> dataList = (List<Map<String, Object>>) resultMap.get("data");
                        List<StockIn> list = gson.fromJson(gson.toJson(dataList), new TypeToken<List<StockIn>>(){}.getType());

                        String loginIdStr = AppStore.getJwt().getLoginId();
                        Integer currentUserId = null;
                        if (loginIdStr != null && !loginIdStr.isEmpty()) {
                            try {
                                if (loginIdStr.contains(".")) {
                                    currentUserId = (int) Double.parseDouble(loginIdStr);
                                } else {
                                    currentUserId = Integer.valueOf(loginIdStr);
                                }
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                        }

                        System.out.println("=== 调试信息 ===");
                        System.out.println("当前用户角色: " + AppStore.getJwt().getRole());
                        System.out.println("当前用户名: " + AppStore.getJwt().getUsername());
                        System.out.println("当前用户loginId: " + loginIdStr);
                        System.out.println("转换后的用户ID: " + currentUserId);
                        System.out.println("后端返回数据条数: " + list.size());
                        for (StockIn stockIn : list) {
                            System.out.println("  单据ID=" + stockIn.getId() + ", 申请人ID=" + stockIn.getApplyUserId() + ", 申请人姓名=" + stockIn.getApplyUserName());
                        }

                        if (!isAdmin && currentUserId != null) {
                            List<StockIn> filteredList = new ArrayList<>();
                            for (StockIn stockIn : list) {
                                if (currentUserId.equals(stockIn.getApplyUserId())) {
                                    filteredList.add(stockIn);
                                }
                            }
                            list = filteredList;
                        }

                        System.out.println("过滤后数据条数: " + list.size());
                        System.out.println("===============\n");


                        final List<StockIn> finalList = new ArrayList<>(list);
                        javafx.application.Platform.runLater(() -> {
                            stockInList.setAll(finalList);
                        });
                    } else {
                        javafx.application.Platform.runLater(() -> {
                            MessageDialog.showDialog("加载数据失败：" + resultMap.get("msg"));
                        });
                    }
                } else {
                    javafx.application.Platform.runLater(() -> {
                        String errorMsg = "请求失败，状态码：" + response.statusCode();
                        MessageDialog.showDialog(errorMsg);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    MessageDialog.showDialog("加载数据异常：" + e.getMessage());
                });
            }
        }).start();
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
        StockIn selected = stockInTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            SimpleMessageDialog.showWarning("请选择要编辑的入库单");
            return;
        }

        if (selected.getStatus() != 0 && selected.getStatus() != 2) {
            SimpleMessageDialog.showWarning("只能编辑待审批或已驳回状态的入库单");
            return;
        }

        String loginIdStr = AppStore.getJwt().getLoginId();
        if (loginIdStr == null || loginIdStr.isEmpty()) {
            SimpleMessageDialog.showError("会话已过期，请重新登录");
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
            SimpleMessageDialog.showError("会话数据异常，请重新登录");
            return;
        }

        if (!isAdmin && !selected.getApplyUserId().equals(currentUserId)) {
            SimpleMessageDialog.showWarning("只能编辑自己创建的入库单");
            return;
        }

        if (selected.getStatus() == 2) {
            String rejectReason = selected.getRejectReason();
            showRejectReasonDialog(rejectReason);
        }
        
        try {
            StockInEditDialog dialog = StockInEditDialog.createEditDialog(selected);
            
            if (dialog != null) {
                dialog.showAndWait();
                loadStockInList();
            } else {
                SimpleMessageDialog.showError("打开编辑窗口失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            SimpleMessageDialog.showError("打开编辑窗口失败：" + e.getMessage());
        }
    }

    @FXML
    protected void onDeleteButtonClick() {
        StockIn selected = stockInTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            SimpleMessageDialog.showWarning("请选择要删除的入库单");
            return;
        }

        if (selected.getStatus() != 0) {
            SimpleMessageDialog.showWarning("只能删除待审批状态的入库单");
            return;
        }

        String loginIdStr = AppStore.getJwt().getLoginId();
        if (loginIdStr == null || loginIdStr.isEmpty()) {
            SimpleMessageDialog.showError("会话已过期，请重新登录");
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
            SimpleMessageDialog.showError("会话数据异常，请重新登录");
            return;
        }

        if (!isAdmin && !selected.getApplyUserId().equals(currentUserId)) {
            SimpleMessageDialog.showWarning("只能删除自己创建的入库单");
            return;
        }

        showConfirmDialog("确认删除", "确认删除入库单 " + selected.getInCode() + "？", () -> {
            try {
                String url = HttpRequestUtil.serverUrl + "/stock-in/delete/" + selected.getId();
                
                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .DELETE()
                        .headers("satoken", AppStore.getJwt().getToken())
                        .build();

                HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    Map<String, Object> resultMap = gson.fromJson(response.body(), new TypeToken<Map<String, Object>>(){}.getType());
                    if (resultMap.get("code").equals(200.0)) {
                        MessageDialog.showDialog("删除成功");
                        loadStockInList();
                    } else {
                        SimpleMessageDialog.showError("删除失败：" + resultMap.get("msg"));
                    }
                } else {
                    SimpleMessageDialog.showError("请求失败");
                }

            } catch (Exception e) {
                e.printStackTrace();
                SimpleMessageDialog.showError("删除异常：" + e.getMessage());
            }
        });
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

        showStockInApproveDialog(selected);
    }

    private void showStockInApproveDialog(StockIn stockIn) {
        StockInApproveDialog dialog = StockInApproveDialog.createDialog(stockIn);
        if (dialog != null) {
            dialog.setOnApproveCallback(() -> {
                javafx.application.Platform.runLater(() -> {
                    loadStockInList();
                });
            });
            dialog.showAndWait();
        }
    }


    private void approveStockIn(StockIn stockIn, boolean approved, String rejectReason) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("stockInId", stockIn.getId());
            requestBody.put("approved", approved);
            if (rejectReason != null) {
                requestBody.put("rejectReason", rejectReason);
            }

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(HttpRequestUtil.serverUrl + "/stock-in/approve"))
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                    .headers("Content-Type", "application/json", "satoken", AppStore.getJwt().getToken())
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, Object> resultMap = gson.fromJson(response.body(), new TypeToken<Map<String, Object>>(){}.getType());
                
                if (resultMap.get("code").equals(200.0)) {
                    MessageDialog.showDialog(approved ? "审批通过" : "已驳回该入库单");
                    loadStockInList();
                } else {
                    MessageDialog.showDialog("审批失败：" + resultMap.get("msg"));
                }
            } else {
                String errorMsg = "";
                if (response.body() != null && !response.body().isEmpty()) {
                    try {
                        Map<String, Object> errorResult = gson.fromJson(response.body(), new TypeToken<Map<String, Object>>(){}.getType());
                        if (errorResult.get("msg") != null) {
                            errorMsg = String.valueOf(errorResult.get("msg"));
                        }
                    } catch (Exception e) {
                        errorMsg += "\n响应内容：" + response.body();
                    }
                }
                MessageDialog.showDialog(errorMsg);
            }
        } catch (Exception e) {
            e.printStackTrace();
            MessageDialog.showDialog("审批异常：" + e.getMessage());
        }
    }

    private void approveAndCompleteStockIn(StockIn stockIn) {
        try {
            Map<String, Object> approveBody = new HashMap<>();
            approveBody.put("stockInId", stockIn.getId());
            approveBody.put("approved", true);

            String approveUrl = HttpRequestUtil.serverUrl + "/stock-in/approve";
            
            HttpRequest approveRequest = HttpRequest.newBuilder()
                    .uri(URI.create(approveUrl))
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(approveBody)))
                    .headers("Content-Type", "application/json", "satoken", AppStore.getJwt().getToken())
                    .build();

            HttpResponse<String> approveResponse = httpClient.send(approveRequest, HttpResponse.BodyHandlers.ofString());

            if (approveResponse.statusCode() == 200) {
                Map<String, Object> approveResult = gson.fromJson(approveResponse.body(), new TypeToken<Map<String, Object>>(){}.getType());
                
                if (approveResult.get("code").equals(200.0)) {
                    MessageDialog.showDialog("批准成功，库存已更新");
                    loadStockInList();
                } else {
                    MessageDialog.showDialog("操作失败：" + approveResult.get("msg"));
                }
            } else {
                String errorMsg = "";
                if (approveResponse.body() != null && !approveResponse.body().isEmpty()) {
                    try {
                        Map<String, Object> errorResult = gson.fromJson(approveResponse.body(), new TypeToken<Map<String, Object>>(){}.getType());
                        if (errorResult.get("msg") != null) {
                            errorMsg = String.valueOf(errorResult.get("msg"));
                        }
                    } catch (Exception e) {
                        errorMsg += "\n响应内容：" + approveResponse.body();
                    }
                }
                MessageDialog.showDialog(errorMsg);
            }
        } catch (Exception e) {
            e.printStackTrace();
            MessageDialog.showDialog("操作异常：" + e.getMessage());
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

    /**
     * 创建美化的 Alert 对话框
     */
    private javafx.scene.control.Alert createStyledAlert(javafx.scene.control.Alert.AlertType type, String title) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        
        // 美化对话框样式
        alert.getDialogPane().setStyle("-fx-background-color: linear-gradient(to bottom, #f5f7fa, #ffffff);");
        alert.getDialogPane().getScene().getStylesheets().add(
            getClass().getResource("/styles/modern-style.css").toExternalForm()
        );
        
        // 美化按钮
        Button okButton = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.setStyle("-fx-background-color: #4a90d9; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 8 30; -fx-background-radius: 6; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(74, 144, 217, 0.3), 8, 0, 0, 2);");
        }
        
        Button cancelButton = (Button) alert.getDialogPane().lookupButton(ButtonType.CANCEL);
        if (cancelButton != null) {
            cancelButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #7f8c8d; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 8 25; -fx-background-radius: 6; -fx-border-color: #e0e6ed; -fx-border-width: 1.5; -fx-border-radius: 6; -fx-cursor: hand;");
        }
        
        return alert;
    }

    /**
     * 显示美化的 Alert 对话框（简单消息）
     */
    private void showStyledAlert(javafx.scene.control.Alert.AlertType type, String title, String message) {
        javafx.scene.control.Alert alert = createStyledAlert(type, title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 显示驳回理由对话框
     */
    private void showRejectReasonDialog(String rejectReason) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.DECORATED);
        dialog.setTitle("驳回理由");

        VBox root = new VBox(25);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(35, 45, 30, 45));
        root.setStyle("-fx-background-color: white; -fx-background-radius: 8;");

        Label titleLabel = new Label("该入库单已被驳回");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label reasonLabel = new Label();
        if (rejectReason != null && !rejectReason.isEmpty()) {
            reasonLabel.setText("驳回理由：\n\n" + rejectReason);
        } else {
            reasonLabel.setText("该入库单已被驳回（无驳回理由）");
        }
        reasonLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #546e7a; -fx-text-alignment: center;");
        reasonLabel.setWrapText(true);
        reasonLabel.setMaxWidth(320);

        Label hintLabel = new Label("点击确定后进入编辑模式");
        hintLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #95a5a6;");

        Button confirmBtn = new Button("确定");
        confirmBtn.setStyle("-fx-background-color: #4a90d9; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 45; -fx-background-radius: 8; -fx-cursor: hand;");
        confirmBtn.setOnAction(e -> dialog.close());

        root.getChildren().addAll(titleLabel, reasonLabel, hintLabel, confirmBtn);

        Scene scene = new Scene(root);
        scene.setFill(Color.WHITE);
        dialog.setScene(scene);

        dialog.showAndWait();
    }

    /**
     * 显示确认对话框
     */
    private void showConfirmDialog(String title, String message, Runnable onConfirm) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.DECORATED);
        dialog.setTitle(title);

        VBox root = new VBox(30);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(45, 55, 35, 55));
        root.setStyle("-fx-background-color: white; -fx-background-radius: 8;");

        Label messageLabel = new Label(message);
        messageLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-text-alignment: center;");
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(380);

        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);

        Button cancelBtn = new Button("取消");
        cancelBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #7f8c8d; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 35; -fx-background-radius: 8; -fx-border-color: #e0e6ed; -fx-border-width: 2; -fx-border-radius: 8; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> dialog.close());

        Button confirmBtn = new Button("确认");
        confirmBtn.setStyle("-fx-background-color: #4a90d9; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 45; -fx-background-radius: 8; -fx-cursor: hand;");
        confirmBtn.setOnAction(e -> {
            dialog.close();
            onConfirm.run();
        });

        buttonBox.getChildren().addAll(cancelBtn, confirmBtn);
        root.getChildren().addAll(messageLabel, buttonBox);

        Scene scene = new Scene(root);
        scene.setFill(Color.WHITE);
        dialog.setScene(scene);

        scene.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                dialog.close();
            }
        });

        dialog.showAndWait();
    }

}
