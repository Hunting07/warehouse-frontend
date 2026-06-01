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
    private TableColumn<OutOrder, LocalDateTime> applyTimeColumn;
    @FXML
    private TableColumn<OutOrder, Integer> totalNumColumn;
    @FXML
    private TableColumn<OutOrder, BigDecimal> totalAmountColumn;
    @FXML
    private TableColumn<OutOrder, String> statusColumn;
    @FXML
    private TableColumn<OutOrder, String> auditUserNameColumn;
    @FXML
    private TableColumn<OutOrder, LocalDateTime> auditTimeColumn;
    @FXML
    private TableColumn<OutOrder, String> remarkColumn;

    private final ObservableList<OutOrder> outOrderList = FXCollections.observableArrayList();
    private final Gson gson = new Gson();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private boolean isAdmin = false;

    @FXML
    public void initialize() {
        // 判断是否为管理员
        String role = AppStore.getJwt().getRole();
        isAdmin = "admin".equals(role) || "ADMIN".equals(role);
        
        System.out.println("\n=== [出库列表] 初始化 ===");
        System.out.println("当前用户角色: " + role);
        System.out.println("是否管理员: " + isAdmin);
        System.out.println("用户ID: " + AppStore.getJwt().getId());

        // 序号列：显示行号
        indexColumn.setCellValueFactory(data -> {
            int index = outOrderTable.getItems().indexOf(data.getValue());
            int total = outOrderTable.getItems().size();
            return new javafx.beans.property.SimpleIntegerProperty(total - index).asObject();
        });
        outTypeColumn.setCellValueFactory(new PropertyValueFactory<>("outTypeName"));
        orderNoColumn.setCellValueFactory(new PropertyValueFactory<>("orderNo"));
        applicantNameColumn.setCellValueFactory(new PropertyValueFactory<>("applicantName"));
        materialNamesColumn.setCellValueFactory(new PropertyValueFactory<>("materialNames"));
        applyTimeColumn.setCellValueFactory(new PropertyValueFactory<>("applyTime"));
        totalNumColumn.setCellValueFactory(new PropertyValueFactory<>("totalNum"));
        totalAmountColumn.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("statusName"));
        auditUserNameColumn.setCellValueFactory(new PropertyValueFactory<>("auditUserName"));
        auditTimeColumn.setCellValueFactory(new PropertyValueFactory<>("auditTime"));
        remarkColumn.setCellValueFactory(new PropertyValueFactory<>("remark"));

        outOrderTable.setItems(outOrderList);

        outOrderTable.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.SINGLE);

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

                // 添加调试日志
                System.out.println("\n=== [出库列表] 加载数据 ===");
                System.out.println("当前用户角色: " + AppStore.getJwt().getRole());
                System.out.println("是否管理员: " + isAdmin);
                System.out.println("用户ID: " + AppStore.getJwt().getId());
                
                // 调试：打印 JwtResponse 的所有信息
                System.out.println("完整 JwtResponse 对象: " + AppStore.getJwt());

                if (!isAdmin && AppStore.getJwt() != null && AppStore.getJwt().getId() != null) {
                    urlBuilder.append(hasParam ? "&" : "?").append("userId=").append(AppStore.getJwt().getId());
                    hasParam = true;
                    System.out.println("添加 userId 参数（非管理员，只看自己的数据）");
                } else if (!isAdmin) {
                    // 非管理员但 ID 为 null，尝试从 Token 获取
                    System.out.println("警告：非管理员但用户ID为null，不添加userId参数");
                } else {
                    System.out.println("不添加 userId 参数（管理员，看所有数据）");
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
                            
                            // 打印第一条数据的 totalAmount，用于调试
                            if (!dataList.isEmpty()) {
                                System.out.println("第一条数据的 totalAmount: " + dataList.get(0).get("totalAmount"));
                                System.out.println("第一条数据的 totalNum: " + dataList.get(0).get("totalNum"));
                            }

                            List<OutOrder> list = new ArrayList<>();
                            for (Map<String, Object> map : dataList) {
                                OutOrder order = new OutOrder();
                                order.setId(((Number) map.get("id")).intValue());
                                order.setOrderNo((String) map.get("orderNo"));
                                order.setOutType(((Number) map.get("outType")).intValue());
                                
                                // 设置出库类型名称
                                Integer outType = ((Number) map.get("outType")).intValue();
                                String outTypeName = getOutTypeName(outType);
                                order.setOutTypeName(outTypeName);
                                
                                order.setApplicantId(((Number) map.get("applicantId")).intValue());
                                
                                // 处理申请人名称，如果为空则显示申请人ID
                                String applicantName = (String) map.get("applicantName");
                                if (applicantName == null || applicantName.trim().isEmpty()) {
                                    Integer applicantId = ((Number) map.get("applicantId")).intValue();
                                    applicantName = "用户" + applicantId;
                                }
                                order.setApplicantName(applicantName);

                                String applyTimeStr = (String) map.get("applyTime");
                                if (applyTimeStr != null) {
                                    order.setApplyTime(java.time.LocalDateTime.parse(applyTimeStr));
                                }

                                order.setStatus(((Number) map.get("status")).intValue());
                                
                                // 设置状态名称
                                Integer status = ((Number) map.get("status")).intValue();
                                String statusName = getStatusName(status);
                                order.setStatusName(statusName);
                                
                                order.setAuditUserId(map.get("auditUserId") != null ? ((Number) map.get("auditUserId")).intValue() : null);
                                
                                // 处理审批人名称
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
                                
                                // 设置物品名称（后端已返回）
                                String materialNames = (String) map.get("materialNames");
                                if (materialNames == null || materialNames.isEmpty()) {
                                    materialNames = "暂无物品";
                                }
                                order.setMaterialNames(materialNames);
                                
                                // 调试日志
                                System.out.println("出库单 " + order.getId() + " 物品名称: " + materialNames);
                                
                                // 优先使用后端返回的 totalAmount
                                if (map.get("totalAmount") != null) {
                                    Object amountObj = map.get("totalAmount");
                                    if (amountObj instanceof Number) {
                                        order.setTotalAmount(new java.math.BigDecimal(amountObj.toString()));
                                    } else {
                                        order.setTotalAmount(java.math.BigDecimal.ZERO);
                                    }
                                } else {
                                    // 后端未返回，暂时设置为 0
                                    order.setTotalAmount(java.math.BigDecimal.ZERO);
                                }
                                
                                // 使用后端返回的 totalNum
                                if (map.get("totalNum") != null) {
                                    Object numObj = map.get("totalNum");
                                    if (numObj instanceof Number) {
                                        order.setTotalNum(((Number) numObj).intValue());
                                    } else {
                                        order.setTotalNum(0);
                                    }
                                } else {
                                    // 后端未返回，设置为 0
                                    order.setTotalNum(0);
                                }

                                list.add(order);
                            }

                            // 后端已返回 totalNum，不需要异步加载明细
                            
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

        int ret = MessageDialog.choiceDialog("确认删除出库单" + selected.getOrderNo() + "？");
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
                int code = (result.get("code") instanceof Number) ? ((Number) result.get("code")).intValue() : -1;
                if (code == 200 || code == 0) {
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

        showApproveDialog(selected);
    }

    private void showApproveDialog(OutOrder outOrder) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("出库单审批");
        dialog.setHeaderText("审批出库单：" + outOrder.getOrderNo());

        ButtonType approveButtonType = new ButtonType("批准", ButtonBar.ButtonData.OK_DONE);
        ButtonType rejectButtonType = new ButtonType("驳回", ButtonBar.ButtonData.OTHER);
        ButtonType cancelButtonType = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);

        dialog.getDialogPane().getButtonTypes().addAll(approveButtonType, rejectButtonType, cancelButtonType);

        TextArea rejectReasonArea = new TextArea();
        rejectReasonArea.setPromptText("请输入驳回理由（仅驳回时需要）");
        rejectReasonArea.setPrefRowCount(4);
        rejectReasonArea.setPrefWidth(400);

        javafx.scene.control.Button rejectBtn = (javafx.scene.control.Button) dialog.getDialogPane().lookupButton(rejectButtonType);
        rejectBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (rejectReasonArea.getText() == null || rejectReasonArea.getText().trim().isEmpty()) {
                MessageDialog.showDialog("请填写驳回理由");
                event.consume();
            }
        });

        // 创建明细信息文本区域（使用 TextArea 可以自动换行）
        TextArea detailArea = new TextArea();
        detailArea.setEditable(false);
        detailArea.setWrapText(true);
        detailArea.setPrefRowCount(6);
        detailArea.setPrefWidth(450);
        detailArea.setText("加载中...");

        // 异步加载明细数据
        new Thread(() -> {
            try {
                System.out.println("\n=== [审批对话框] 开始加载明细 ===");
                System.out.println("出库单ID: " + outOrder.getId());
                
                String url = HttpRequestUtil.serverUrl + "/api/stockOut/getDetail/" + outOrder.getId();
                System.out.println("请求URL: " + url);
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .headers("satoken", AppStore.getJwt().getToken())
                        .build();
                
                System.out.println("发送请求...");
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                System.out.println("响应状态码: " + response.statusCode());
                System.out.println("响应内容: " + response.body());
                
                if (response.statusCode() == 200) {
                    Map<String, Object> result = gson.fromJson(response.body(), new TypeToken<Map<String, Object>>(){}.getType());
                    int code = (result.get("code") instanceof Number) ? ((Number) result.get("code")).intValue() : -1;
                    
                    System.out.println("解析结果: code=" + code);
                    
                    if (code == 200 || code == 0) {
                        Map<String, Object> data = (Map<String, Object>) result.get("data");
                        
                        if (data != null) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("items");
                            
                            System.out.println("明细数量: " + (items != null ? items.size() : 0));
                            
                            if (items != null && !items.isEmpty()) {
                                System.out.println("明细字段: " + items.get(0).keySet());
                                
                                StringBuilder detailText = new StringBuilder();
                                detailText.append("出库明细：\n\n");
                                
                                int index = 1;
                                for (Map<String, Object> item : items) {
                                    String materialName = (String) item.get("materialName");
                                    Object unitPrice = item.get("unitPrice");
                                    Object outQuantity = item.get("outQuantity");
                                    
                                    detailText.append(index++).append(". ");
                                    detailText.append("物品：").append(materialName != null ? materialName : "未知");
                                    detailText.append("  |  单价：").append(unitPrice != null ? unitPrice : "0");
                                    detailText.append("  |  数量：").append(outQuantity != null ? outQuantity : "0");
                                    detailText.append("\n");
                                }
                                
                                final String finalDetailText = detailText.toString();
                                javafx.application.Platform.runLater(() -> {
                                    detailArea.setText(finalDetailText);
                                    System.out.println("✅ 明细加载成功");
                                });
                            } else {
                                javafx.application.Platform.runLater(() -> {
                                    detailArea.setText("暂无明细数据");
                                    System.out.println("⚠️ 没有明细数据");
                                });
                            }
                        } else {
                            System.out.println("❌ data 为 null");
                            javafx.application.Platform.runLater(() -> {
                                detailArea.setText("加载失败：数据为空");
                            });
                        }
                    } else {
                        System.out.println("❌ 业务错误: " + result.get("msg"));
                        javafx.application.Platform.runLater(() -> {
                            detailArea.setText("加载失败：" + result.get("msg"));
                        });
                    }
                } else {
                    System.out.println("❌ HTTP错误: " + response.statusCode());
                    javafx.application.Platform.runLater(() -> {
                        detailArea.setText("加载失败：HTTP " + response.statusCode());
                    });
                }
            } catch (Exception e) {
                System.err.println("❌ 加载明细数据失败: " + e.getMessage());
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    detailArea.setText("加载失败：" + e.getMessage());
                });
            }
        }).start();

        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(10);
        content.setPadding(new javafx.geometry.Insets(10));
        content.getChildren().addAll(
                new Label("申请人：" + outOrder.getApplicantName()),
                new Label("出库类型：" + getOutTypeName(outOrder.getOutType())),
                new Label("总金额：" + outOrder.getTotalAmount()),
                new Label("申请时间：" + outOrder.getApplyTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))),
                new javafx.scene.control.Separator(),
                new Label("出库明细："),
                detailArea,
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
                approveOutOrder(outOrder, true, null);
            } else {
                approveOutOrder(outOrder, false, rejectReasonArea.getText());
            }
        });
    }

    private void approveOutOrder(OutOrder outOrder, boolean approved, String rejectReason) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("orderId", outOrder.getId());
            requestBody.put("status", approved ? 1 : 2);
            if (rejectReason != null && !rejectReason.trim().isEmpty()) {
                requestBody.put("rejectReason", rejectReason);
            }

            System.out.println("\n=== [前端] 出库单审批请求 ===");
            System.out.println("请求URL: " + HttpRequestUtil.serverUrl + "/api/stockOut/approve");
            System.out.println("请求方法: PUT");
            System.out.println("Token: " + AppStore.getJwt().getToken());
            System.out.println("Token长度: " + (AppStore.getJwt().getToken() != null ? AppStore.getJwt().getToken().length() : 0));
            System.out.println("请求体: " + gson.toJson(requestBody));
            System.out.println("出库单号: " + outOrder.getOrderNo());
            System.out.println("审批结果: " + (approved ? "批准(status=1)" : "驳回(status=2)"));

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(HttpRequestUtil.serverUrl + "/api/stockOut/approve"))
                    .PUT(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                    .headers("Content-Type", "application/json", "satoken", AppStore.getJwt().getToken())
                    .build();

            System.out.println("发送请求...");
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            System.out.println("接收响应...");
            System.out.println("响应状态码: " + response.statusCode());
            System.out.println("响应头: " + response.headers().map());
            System.out.println("响应内容: " + response.body());

            if (response.statusCode() == 200) {
                Map<String, Object> result = gson.fromJson(response.body(), Map.class);
                int code = (result.get("code") instanceof Number) ? ((Number) result.get("code")).intValue() : -1;
                System.out.println("解析后的结果: code=" + code + ", msg=" + result.get("msg"));

                if (code == 200 || code == 0) {
                    MessageDialog.showDialog(approved ? "审批通过" : "已驳回该出库单");
                    System.out.println("✅ 审批成功");
                    loadOutOrderList();
                } else {
                    MessageDialog.showDialog("审批失败：" + result.get("msg"));
                    System.out.println("❌ 业务错误: " + result.get("msg"));
                }
            } else {
                String errorMsg = "";
                if (response.body() != null && !response.body().isEmpty()) {
                    try {
                        Map<String, Object> errorResult = gson.fromJson(response.body(), Map.class);
                        if (errorResult.get("msg") != null) {
                            errorMsg = String.valueOf(errorResult.get("msg"));
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
                int code = (result.get("code") instanceof Number) ? ((Number) result.get("code")).intValue() : -1;
                if (code == 200 || code == 0) {
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
