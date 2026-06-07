package com.teach.javafx.controller.base;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.teach.javafx.AppStore;
import com.teach.javafx.bean.OutOrder;
import com.teach.javafx.request.HttpRequestUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OutOrderListController extends ToolController {

    @FXML
    private Label titleLabel;

    @FXML
    private Label subtitleLabel;

    @FXML
    private Button editButton;

    @FXML
    private Button approveButton;

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
    private TableColumn<OutOrder, Integer> indexColumn;
    @FXML
    private TableColumn<OutOrder, String> orderNoColumn;
    @FXML
    private TableColumn<OutOrder, String> outTypeColumn;
    @FXML
    private TableColumn<OutOrder, String> applicantNameColumn;
    @FXML
    private TableColumn<OutOrder, String> materialNamesColumn;
    @FXML
    private TableColumn<OutOrder, BigDecimal> totalAmountColumn;
    @FXML
    private TableColumn<OutOrder, LocalDateTime> applyTimeColumn;
    @FXML
    private TableColumn<OutOrder, LocalDateTime> auditTimeColumn;
    @FXML
    private TableColumn<OutOrder, String> statusColumn;
    @FXML
    private TableColumn<OutOrder, String> auditUserNameColumn;
    @FXML
    private TableColumn<OutOrder, Void> actionColumn;

    private final ObservableList<OutOrder> outOrderList = FXCollections.observableArrayList();
    private final Gson gson = new Gson();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private boolean isAdmin = false;

    @FXML
    public void initialize() {
        String role = AppStore.getJwt().getRole();
        isAdmin = "admin".equals(role) || "ADMIN".equals(role);

        if (isAdmin) {
            titleLabel.setText("出库审批管理");
            subtitleLabel.setText("管理和审批所有出库单据");
            editButton.setVisible(false);
            editButton.setManaged(false);
        } else {
            titleLabel.setText("出库申请管理");
            subtitleLabel.setText("查看和管理我的出库申请单");
            approveButton.setVisible(false);
            approveButton.setManaged(false);
        }

        indexColumn.setCellValueFactory(data -> {
            int index = outOrderTable.getItems().indexOf(data.getValue());
            int total = outOrderTable.getItems().size();
            return new javafx.beans.property.SimpleIntegerProperty(total - index).asObject();
        });
        
        outTypeColumn.setCellValueFactory(new PropertyValueFactory<>("outTypeName"));
        orderNoColumn.setCellValueFactory(new PropertyValueFactory<>("orderNo"));
        applicantNameColumn.setCellValueFactory(new PropertyValueFactory<>("applicantName"));
        
        if (materialNamesColumn != null) {
            materialNamesColumn.setCellValueFactory(new PropertyValueFactory<>("materialNames"));
        }
        
        if (totalAmountColumn != null) {
            totalAmountColumn.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
            totalAmountColumn.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(BigDecimal item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null || item.compareTo(BigDecimal.ZERO) == 0) {
                        setText("--");
                        setStyle("-fx-text-fill: #86909c;");
                    } else {
                        setText(String.format("¥%.2f", item));
                        setStyle("-fx-text-fill: #1d3f66;");
                    }
                }
            });
        }
        
        if (applyTimeColumn != null) {
            applyTimeColumn.setCellValueFactory(new PropertyValueFactory<>("applyTime"));
            applyTimeColumn.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(LocalDateTime item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText("");
                    } else {
                        setText(item.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    }
                }
            });
        }
        
        if (auditTimeColumn != null) {
            auditTimeColumn.setCellValueFactory(new PropertyValueFactory<>("auditTime"));
            auditTimeColumn.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(LocalDateTime item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText("");
                    } else {
                        setText(item.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    }
                }
            });
        }
        
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("statusName"));
        auditUserNameColumn.setCellValueFactory(new PropertyValueFactory<>("auditUserName"));

        if (actionColumn != null) {
            actionColumn.setCellFactory(param -> new TableCell<>() {
                private final Button viewDetailBtn = new Button("查看明细");

                {
                    viewDetailBtn.setStyle("-fx-background-color: #4A90E2; -fx-text-fill: white; -fx-font-size: 13px; -fx-cursor: hand; -fx-background-radius: 5; -fx-padding: 8 18 8 18; -fx-font-weight: bold;");
                    viewDetailBtn.setOnAction(e -> {
                        OutOrder order = getTableView().getItems().get(getIndex());
                        showDetailDialog(order);
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        setGraphic(viewDetailBtn);
                        setAlignment(javafx.geometry.Pos.CENTER);
                    }
                }
            });
        } else {
            System.err.println("⚠️ 警告: actionColumn 为 null，操作列将不会显示");
        }

        outOrderTable.setItems(outOrderList);
        outOrderTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        outOrderTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

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

    public void loadOutOrderList() {
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
                } else if (!isAdmin) {
                } else {
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


                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(urlBuilder.toString()))
                        .GET()
                        .headers("satoken", AppStore.getJwt().getToken())
                        .build();

                HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());


                if (response.statusCode() == 200) {
                    Map<String, Object> resultMap = gson.fromJson(response.body(), new TypeToken<Map<String, Object>>(){}.getType());
                    Object codeObj = resultMap.get("code");
                    int code = codeObj instanceof Number ? ((Number) codeObj).intValue() : -1;

                    if (code == 200 || code == 0) {
                        Map<String, Object> dataMap = (Map<String, Object>) resultMap.get("data");
                        List<Map<String, Object>> dataList = (List<Map<String, Object>>) dataMap.get("records");
                        if (dataList != null) {
                            
                            if (!dataList.isEmpty()) {
                            }

                            List<OutOrder> list = new ArrayList<>();
                            for (Map<String, Object> map : dataList) {
                                OutOrder order = new OutOrder();
                                
                                if (map.get("id") != null) {
                                    order.setId(((Number) map.get("id")).intValue());
                                }
                                
                                order.setOrderNo((String) map.get("orderNo"));
                                
                                if (map.get("outType") != null) {
                                    order.setOutType(((Number) map.get("outType")).intValue());
                                    
                                    Integer outType = ((Number) map.get("outType")).intValue();
                                    String outTypeName = getOutTypeName(outType);
                                    order.setOutTypeName(outTypeName);
                                } else {
                                    order.setOutType(1); // 默认为领料出库
                                    order.setOutTypeName("领料出库");
                                }
                                
                                if (map.get("applicantId") != null) {
                                    order.setApplicantId(((Number) map.get("applicantId")).intValue());
                                }
                                
                                String applicantName = (String) map.get("applicantName");
                                if (applicantName == null || applicantName.trim().isEmpty()) {
                                    Integer applicantId = order.getApplicantId();
                                    if (applicantId != null) {
                                        applicantName = "用户" + applicantId;
                                    } else {
                                        applicantName = "未知";
                                    }
                                }
                                order.setApplicantName(applicantName);

                                String applyTimeStr = (String) map.get("applyTime");
                                if (applyTimeStr != null) {
                                    order.setApplyTime(java.time.LocalDateTime.parse(applyTimeStr));
                                }

                                if (map.get("status") != null) {
                                    order.setStatus(((Number) map.get("status")).intValue());
                                    
                                    Integer status = ((Number) map.get("status")).intValue();
                                    String statusName = getStatusName(status);
                                    order.setStatusName(statusName);
                                } else {
                                    order.setStatus(0); // 默认为待审批
                                    order.setStatusName("待审批");
                                }
                                
                                order.setAuditUserId(map.get("auditUserId") != null ? ((Number) map.get("auditUserId")).intValue() : null);
                                
                                String auditUserName = (String) map.get("auditUserName");
                                if (auditUserName == null || auditUserName.isEmpty()) {
                                    Integer auditUserId = map.get("auditUserId") != null ? ((Number) map.get("auditUserId")).intValue() : null;
                                    if (auditUserId != null) {
                                        auditUserName = "管理员";
                                    }
                                }
                                order.setAuditUserName(auditUserName);

                                String auditTimeStr = (String) map.get("auditTime");
                                if (auditTimeStr != null) {
                                    order.setAuditTime(java.time.LocalDateTime.parse(auditTimeStr));
                                }

                                order.setRemark((String) map.get("remark"));
                                order.setRejectReason((String) map.get("rejectReason"));
                                
                                String materialNames = (String) map.get("materialNames");
                                if (materialNames == null || materialNames.isEmpty()) {
                                    materialNames = "暂无物品";
                                }
                                order.setMaterialNames(materialNames);
                                
                                
                                if (map.get("totalAmount") != null) {
                                    Object amountObj = map.get("totalAmount");
                                    if (amountObj instanceof Number) {
                                        order.setTotalAmount(new java.math.BigDecimal(amountObj.toString()));
                                    } else {
                                        order.setTotalAmount(java.math.BigDecimal.ZERO);
                                    }
                                } else {
                                    order.setTotalAmount(java.math.BigDecimal.ZERO);
                                }
                                
                                if (map.get("totalNum") != null) {
                                    Object numObj = map.get("totalNum");
                                    if (numObj instanceof Number) {
                                        order.setTotalNum(((Number) numObj).intValue());
                                    } else {
                                        order.setTotalNum(0);
                                    }
                                } else {
                                    order.setTotalNum(0);
                                }

                                list.add(order);
                            }

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

                            if (!isAdmin && currentUserId != null) {
                                List<OutOrder> filteredList = new ArrayList<>();
                                for (OutOrder order : list) {
                                    if (currentUserId.equals(order.getApplicantId())) {
                                        filteredList.add(order);
                                    }
                                }
                                list = filteredList;
                            }

                            final List<OutOrder> finalList = new ArrayList<>(list);
                            javafx.application.Platform.runLater(() -> {
                                outOrderList.setAll(finalList);
                            });
                        }
                } else {
                    final String errorMsg = resultMap.get("msg") != null ? resultMap.get("msg").toString() : "未知错误";
                    javafx.application.Platform.runLater(() -> {
                        MessageDialog.showDialog("加载数据失败：" + errorMsg);
                    });
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
                    final String finalErrorMsg = errorMsg;
                    javafx.application.Platform.runLater(() -> {
                        MessageDialog.showDialog(finalErrorMsg);
                    });
                }
            } catch (Exception e) {
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

                new Thread(() -> {
                    try {
                        Thread.sleep(500);
                        javafx.application.Platform.runLater(() -> {
                            loadOutOrderList();
                        });
                    } catch (InterruptedException e) {
                    }
                }).start();
            }
        } catch (Exception e) {
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
            showRejectReasonDialog(selected.getRejectReason());
            return;
        }

        try {
            OutOrderEditDialog dialog = OutOrderEditDialog.createEditDialog(selected);
            if (dialog != null) {
                dialog.showAndWait();

                new Thread(() -> {
                    try {
                        Thread.sleep(500);
                        javafx.application.Platform.runLater(() -> {
                            loadOutOrderList();
                        });
                    } catch (InterruptedException e) {
                    }
                }).start();
            }
        } catch (Exception e) {
            MessageDialog.showDialog("打开编辑窗口失败：" + e.getMessage());
        }
    }

    private void showRejectReasonDialog(String rejectReason) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.DECORATED);
        dialog.setTitle("驳回理由");

        VBox root = new VBox(30);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(45, 55, 40, 55));
        root.setStyle("-fx-background-color: white; -fx-background-radius: 8;");

        Label titleLabel = new Label("该出库单已被驳回");
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label reasonLabel = new Label();
        if (rejectReason != null && !rejectReason.isEmpty()) {
            reasonLabel.setText("驳回理由：\n\n" + rejectReason);
        } else {
            reasonLabel.setText("该出库单已被驳回（无驳回理由）");
        }
        reasonLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: #546e7a; -fx-text-alignment: center;");
        reasonLabel.setWrapText(true);
        reasonLabel.setMaxWidth(400);

        Label hintLabel = new Label("只有待审批的出库单可以重新编辑");
        hintLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #e74c3c; -fx-font-weight: bold;");

        Button confirmBtn = new Button("确定");
        confirmBtn.setStyle("-fx-background-color: #4a90d9; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 12 55; -fx-background-radius: 8; -fx-cursor: hand;");
        confirmBtn.setOnAction(e -> dialog.close());

        root.getChildren().addAll(titleLabel, reasonLabel, hintLabel, confirmBtn);

        Scene scene = new Scene(root, 500, 380);
        scene.setFill(Color.WHITE);
        dialog.setScene(scene);

        dialog.showAndWait();
    }

    @FXML
    protected void onDeleteButtonClick() {
        OutOrder selected = outOrderTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            MessageDialog.showDialog("请选择要删除的出库单");
            return;
        }

        String status = String.valueOf(selected.getStatus());
        if (!"0".equals(status)) {
            if ("2".equals(status)) {
                MessageDialog.showDialog("已驳回的出库单无法删除\n\n请修改后重新提交审批");
            } else {
                MessageDialog.showDialog("只能删除待审批状态的出库单\n\n已完成审批流程的单据不可删除");
            }
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

        int ret = MessageDialog.choiceDialog("确认删除出库单" + selected.getOrderNo() + "？");
        if (ret != MessageDialog.CHOICE_YES) return;

        try {
            String[] possibleUrls = {
                HttpRequestUtil.serverUrl + "/api/stockOut/delete/" + selected.getId(),
                HttpRequestUtil.serverUrl + "/stockOut/delete/" + selected.getId(),
                HttpRequestUtil.serverUrl + "/api/outOrder/delete/" + selected.getId(),
                HttpRequestUtil.serverUrl + "/outOrder/delete/" + selected.getId()
            };
            
            String successUrl = null;
            HttpResponse<String> response = null;
            
            for (String testUrl : possibleUrls) {
                try {
                    
                    HttpRequest httpRequest = HttpRequest.newBuilder()
                            .uri(URI.create(testUrl))
                            .DELETE()
                            .headers("satoken", AppStore.getJwt().getToken())
                            .build();

                    response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                    
                    
                    if (response.statusCode() == 200) {
                        Map<String, Object> result = gson.fromJson(response.body(), Map.class);
                        int code = (result.get("code") instanceof Number) ? ((Number) result.get("code")).intValue() : -1;
                        
                        if (code == 200 || code == 0) {
                            successUrl = testUrl;
                            break;
                        } else {
                        }
                    } else {
                    }
                } catch (Exception e) {
                }
            }
            
            if (successUrl != null) {
                MessageDialog.showDialog("删除成功");
                loadOutOrderList();
            } else {
                System.err.println("\n所有删除URL都失败了！");
                MessageDialog.showDialog("删除失败，请检查后端接口");
            }
        } catch (Exception e) {
            System.err.println("删除异常: " + e.getMessage());
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

        showApproveDialog(selected);
    }

    private void showApproveDialog(OutOrder outOrder) {
        OutOrderApproveDialog dialog = OutOrderApproveDialog.createDialog(outOrder);
        if (dialog != null) {
            dialog.showAndWait();
            
            new Thread(() -> {
                try {
                    Thread.sleep(300);
                    javafx.application.Platform.runLater(() -> {
                        loadOutOrderList();
                    });
                } catch (InterruptedException e) {
                }
            }).start();
        }
    }

    @FXML
    protected void onRejectButtonClick() {
        MessageDialog.showDialog("请使用审批功能进行驳回操作");
    }

    @FXML
    protected void onViewDetailButtonClick() {
        OutOrder selected = outOrderTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            MessageDialog.showDialog("请选择要查看的出库单");
            return;
        }
        showDetailDialog(selected);
    }

    private void showDetailDialog(OutOrder order) {
        Stage dialog = new Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("出库单明细");
        dialog.setMinWidth(800);
        dialog.setMinHeight(550);

        VBox mainContainer = new VBox(12);
        mainContainer.setPadding(new javafx.geometry.Insets(15));
        mainContainer.setStyle("-fx-background-color: white;");

        VBox titleBar = new VBox(3);
        Label titleLabel = new Label("出库单明细");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1d3f66;");
        Label subtitleLabel = new Label("查看出库单详细信息");
        subtitleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #86909c;");
        titleBar.getChildren().addAll(titleLabel, subtitleLabel);

        GridPane headerGrid = new GridPane();
        headerGrid.setHgap(20);
        headerGrid.setVgap(10);
        headerGrid.setPadding(new javafx.geometry.Insets(12));
        headerGrid.setStyle("-fx-background-color: #F8F9FA; -fx-background-radius: 8; -fx-border-color: #E9ECEF; -fx-border-width: 1; -fx-border-radius: 8;");

        Label orderNoLabel = new Label("出库单号：");
        orderNoLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        Label orderNoValue = new Label(order.getOrderNo() != null ? order.getOrderNo() : "无");
        orderNoValue.setStyle("-fx-font-size: 14px; -fx-text-fill: #4A90E2; -fx-font-weight: bold;");

        Label outTypeLabel = new Label("出库类型：");
        outTypeLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        Label outTypeValue = new Label(getOutTypeName(order.getOutType()));
        outTypeValue.setStyle("-fx-font-size: 14px; -fx-text-fill: #5CB85C; -fx-font-weight: bold;");

        Label applicantLabel = new Label("申请人：");
        applicantLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        Label applicantValue = new Label(order.getApplicantName() != null ? order.getApplicantName() : "无");
        applicantValue.setStyle("-fx-font-size: 14px;");

        Label applyTimeLabel = new Label("申请时间：");
        applyTimeLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        Label applyTimeValue = new Label(order.getApplyTime() != null ? order.getApplyTime().format(formatter) : "无");
        applyTimeValue.setStyle("-fx-font-size: 14px;");

        Label statusLabel = new Label("审批状态：");
        statusLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        Label statusValue = new Label(getStatusName(order.getStatus()));
        statusValue.setStyle("-fx-font-size: 14px; -fx-text-fill: #FFA940; -fx-font-weight: bold;");

        Label auditTimeLabel = new Label("审批时间：");
        auditTimeLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        Label auditTimeValue = new Label(order.getAuditTime() != null ? order.getAuditTime().format(formatter) : "无");
        auditTimeValue.setStyle("-fx-font-size: 14px;");

        headerGrid.add(orderNoLabel, 0, 0);
        headerGrid.add(orderNoValue, 1, 0);
        headerGrid.add(outTypeLabel, 2, 0);
        headerGrid.add(outTypeValue, 3, 0);
        headerGrid.add(applicantLabel, 4, 0);
        headerGrid.add(applicantValue, 5, 0);
        headerGrid.add(applyTimeLabel, 0, 1);
        headerGrid.add(applyTimeValue, 1, 1);
        headerGrid.add(statusLabel, 2, 1);
        headerGrid.add(statusValue, 3, 1);
        headerGrid.add(auditTimeLabel, 4, 1);
        headerGrid.add(auditTimeValue, 5, 1);

        Label totalNumLabel = new Label("总数量：");
        totalNumLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        Label totalNumValue = new Label("0");
        totalNumValue.setStyle("-fx-font-size: 14px; -fx-text-fill: #4A90E2; -fx-font-weight: bold;");
        Label totalAmountLabel = new Label("合计总金额：");
        totalAmountLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        Label totalAmountValue = new Label("\u00a50.00");
        totalAmountValue.setStyle("-fx-font-size: 14px; -fx-text-fill: #F5222D; -fx-font-weight: bold;");

        HBox summaryBar = new HBox(30);
        summaryBar.setPadding(new javafx.geometry.Insets(10, 15, 10, 15));
        summaryBar.setStyle("-fx-background-color: #F8F9FA; -fx-background-radius: 6;");
        summaryBar.getChildren().addAll(totalNumLabel, totalNumValue, totalAmountLabel, totalAmountValue);

        TableView<Map<String, Object>> detailTable = new TableView<>();
        detailTable.setStyle("-fx-background-color: white; -fx-border-color: #E0E0E0; -fx-border-width: 1; -fx-border-radius: 8;");
        detailTable.getStyleClass().add("detail-table");
        
        TableColumn<Map<String, Object>, String> seqCol = new TableColumn<>("序号");
        seqCol.setMinWidth(60);
        seqCol.setPrefWidth(60);
        seqCol.getStyleClass().add("table-header");
        seqCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setText(null); }
                else {
                    int idx = getIndex();
                    if (idx >= 0 && idx < getTableView().getItems().size()) { setText(String.valueOf(idx + 1)); }
                    else { setText(null); }
                }
            }
        });

        TableColumn<Map<String, Object>, String> goodsNameCol = new TableColumn<>("物品名称");
        goodsNameCol.setMinWidth(150);
        goodsNameCol.setPrefWidth(180);
        goodsNameCol.getStyleClass().add("table-header");
        goodsNameCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
            param.getValue().getOrDefault("goodsName", "").toString()
        ));
        goodsNameCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isEmpty()) { 
                    setText(null); 
                }
                else { 
                    setText(item); 
                }
            }
        });

        TableColumn<Map<String, Object>, BigDecimal> unitPriceCol = new TableColumn<>("单价");
        unitPriceCol.setMinWidth(100);
        unitPriceCol.setPrefWidth(110);
        unitPriceCol.getStyleClass().add("table-header");
        unitPriceCol.setCellValueFactory(param -> {
            Object value = param.getValue().get("unitPrice");
            if (value instanceof BigDecimal) {
                return new javafx.beans.property.SimpleObjectProperty<>((BigDecimal) value);
            } else if (value instanceof Number) {
                return new javafx.beans.property.SimpleObjectProperty<>(new BigDecimal(value.toString()));
            }
            return new javafx.beans.property.SimpleObjectProperty<>(null);
        });
        unitPriceCol.setCellFactory(col -> new TableCell<>() {
            private final TextField tf = new TextField();
            {
                tf.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
                tf.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 0 5 0 5;");
                tf.setFont(javafx.scene.text.Font.font("Microsoft YaHei", 13));
                tf.setEditable(false);
                tf.setFocusTraversable(false);
                tf.setCursor(javafx.scene.Cursor.DEFAULT);
            }
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { 
                    setGraphic(null); 
                    setText(null);
                }
                else {
                    tf.setText(String.format("%.2f", item));
                    setGraphic(tf);
                    setText(null);
                }
            }
        });

        TableColumn<Map<String, Object>, Integer> outNumCol = new TableColumn<>("出库数量");
        outNumCol.setMinWidth(90);
        outNumCol.setPrefWidth(100);
        outNumCol.getStyleClass().add("table-header");
        outNumCol.setCellValueFactory(param -> {
            Object value = param.getValue().get("outNum");
            if (value instanceof Integer) {
                return new javafx.beans.property.SimpleObjectProperty<>((Integer) value);
            } else if (value instanceof Number) {
                return new javafx.beans.property.SimpleObjectProperty<>(((Number) value).intValue());
            }
            return new javafx.beans.property.SimpleObjectProperty<>(null);
        });
        outNumCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { 
                    setText(null); 
                }
                else { 
                    setText(String.valueOf(item)); 
                }
            }
        });

        TableColumn<Map<String, Object>, BigDecimal> totalPriceCol = new TableColumn<>("单品金额");
        totalPriceCol.setMinWidth(120);
        totalPriceCol.setPrefWidth(130);
        totalPriceCol.getStyleClass().add("table-header");
        totalPriceCol.setCellValueFactory(param -> {
            Object value = param.getValue().get("amount");
            if (value instanceof BigDecimal) {
                return new javafx.beans.property.SimpleObjectProperty<>((BigDecimal) value);
            } else if (value instanceof Number) {
                return new javafx.beans.property.SimpleObjectProperty<>(new BigDecimal(value.toString()));
            }
            return new javafx.beans.property.SimpleObjectProperty<>(null);
        });
        totalPriceCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { 
                    setText(null); 
                }
                else { 
                    setText(String.format("¥%.2f", item)); 
                }
            }
        });

        detailTable.getColumns().addAll(seqCol, goodsNameCol, unitPriceCol, outNumCol, totalPriceCol);
        detailTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        detailTable.setPlaceholder(new Label("暂无数据"));
        
        detailTable.setFixedCellSize(40);
        detailTable.setTableMenuButtonVisible(false);
        detailTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        VBox.setVgrow(headerGrid, javafx.scene.layout.Priority.NEVER);
        VBox.setVgrow(summaryBar, javafx.scene.layout.Priority.NEVER);

        VBox tableContainer = new VBox(8);
        VBox.setVgrow(tableContainer, javafx.scene.layout.Priority.ALWAYS);
        
        ScrollPane scrollPane = new ScrollPane(detailTable);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setMinHeight(40);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        
        tableContainer.getChildren().add(scrollPane);

        HBox buttonBar = new HBox();
        buttonBar.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        buttonBar.setPadding(new javafx.geometry.Insets(5, 0, 0, 0));
        VBox.setVgrow(buttonBar, javafx.scene.layout.Priority.NEVER);
        Button closeBtn = new Button("关闭");
        closeBtn.setStyle("-fx-background-color: #E0E0E0; -fx-text-fill: #333333; -fx-font-size: 13px; -fx-cursor: hand; -fx-background-radius: 4; -fx-padding: 6 20 6 20;");
        closeBtn.setOnAction(e -> dialog.close());
        buttonBar.getChildren().add(closeBtn);

        mainContainer.getChildren().addAll(titleBar, headerGrid, tableContainer, summaryBar, buttonBar);
        
        Scene scene = new Scene(mainContainer, 820, 580);
        scene.getStylesheets().add(getClass().getResource("/styles/modern-style.css").toExternalForm());
        scene.setFill(Color.WHITE);
        dialog.setScene(scene);

        new Thread(() -> {
            try {
                String url = HttpRequestUtil.serverUrl + "/api/stockOut/detail/" + order.getId();
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .headers("satoken", AppStore.getJwt().getToken())
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                
                if (response.statusCode() == 200) {
                    Map<String, Object> result = gson.fromJson(response.body(), Map.class);
                    int code = (result.get("code") instanceof Number) ? ((Number) result.get("code")).intValue() : -1;
                    if (code == 200 || code == 0) {
                        Map<String, Object> dataMap = (Map<String, Object>) result.get("data");
                        List<Map<String, Object>> items = (List<Map<String, Object>>) dataMap.get("items");
                        
                        if (items != null && !items.isEmpty()) {
                        }
                        
                        Platform.runLater(() -> {
                            if (items != null && !items.isEmpty()) {
                                ObservableList<Map<String, Object>> filtered = FXCollections.observableArrayList();
                                int totalNum = 0;
                                BigDecimal totalAmt = BigDecimal.ZERO;
                                int seq = 1;
                                
                                for (Map<String, Object> item : items) {
                                    String name = (String) item.get("goodsName");
                                    if (name == null || name.isEmpty()) {
                                        name = (String) item.get("materialName");
                                    }
                                    if (name == null || name.isEmpty()) {
                                        name = (String) item.get("name");
                                    }
                                    
                                    Object qtyObj = item.get("outNum");
                                    if (qtyObj == null) {
                                        qtyObj = item.get("outQuantity");
                                    }
                                    if (qtyObj == null) {
                                        qtyObj = item.get("quantity");
                                    }
                                    int qty = qtyObj instanceof Number ? ((Number) qtyObj).intValue() : 0;
                                    
                                    BigDecimal unitPrice = BigDecimal.ZERO;
                                    Object priceObj = item.get("unitPrice");
                                    if (priceObj instanceof Number) {
                                        unitPrice = new BigDecimal(priceObj.toString());
                                    }
                                    
                                    BigDecimal amount = BigDecimal.ZERO;
                                    Object amountObj = item.get("amount");
                                    if (amountObj == null) {
                                        amountObj = item.get("totalAmount");
                                    }
                                    if (amountObj == null) {
                                        amountObj = item.get("totalPrice");
                                    }
                                    if (amountObj instanceof Number) {
                                        amount = new BigDecimal(amountObj.toString());
                                    }
                                    
                                    if (amount.compareTo(BigDecimal.ZERO) == 0 && unitPrice.compareTo(BigDecimal.ZERO) > 0 && qty > 0) {
                                        amount = unitPrice.multiply(BigDecimal.valueOf(qty));
                                    }
                                    
                                    Map<String, Object> detail = new HashMap<>(item);
                                    detail.put("seq", seq++);
                                    if (name != null) {
                                        detail.put("goodsName", name);
                                    }
                                    detail.put("outNum", qty);
                                    detail.put("unitPrice", unitPrice);
                                    detail.put("amount", amount);
                                    
                                    filtered.add(detail);
                                    totalNum += qty;
                                    totalAmt = totalAmt.add(amount);
                                    
                                }
                                
                                detailTable.setItems(filtered);
                                totalNumValue.setText(String.valueOf(totalNum));
                                totalAmountValue.setText(String.format("¥%.2f", totalAmt));
                                
                                int rowCount = filtered.size();
                                double tableHeight = rowCount * 40 + 40;
                                scrollPane.setPrefHeight(tableHeight);
                                
                            } else {
                            }
                        });
                    }
                }
            } catch (Exception e) {
                Platform.runLater(() -> MessageDialog.showDialog("加载明细异常：" + e.getMessage()));
            }
        }).start();

        dialog.showAndWait();
    }

    @Override
    public void doRefresh() {
        loadOutOrderList();
    }
}
