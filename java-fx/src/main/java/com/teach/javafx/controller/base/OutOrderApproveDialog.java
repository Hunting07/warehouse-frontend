package com.teach.javafx.controller.base;

import com.google.gson.Gson;
import com.teach.javafx.AppStore;
import com.teach.javafx.GsonUtil;
import com.teach.javafx.bean.OutOrder;
import com.teach.javafx.request.HttpRequestUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

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
import java.util.stream.Collectors;

public class OutOrderApproveDialog extends Stage {

    @FXML
    private Label orderNoLabel;
    @FXML
    private Label applicantLabel;
    @FXML
    private Label outTypeLabel;
    @FXML
    private Label applyTimeLabel;
    @FXML
    private Label totalAmountLabel;

    @FXML
    private TableView<Map<String, Object>> detailTableView;
    @FXML
    private TableColumn<Map<String, Object>, Integer> serialNumberColumn;
    @FXML
    private TableColumn<Map<String, Object>, String> materialCodeColumn;
    @FXML
    private TableColumn<Map<String, Object>, String> materialColumn;
    @FXML
    private TableColumn<Map<String, Object>, BigDecimal> unitPriceColumn;
    @FXML
    private TableColumn<Map<String, Object>, Integer> quantityColumn;
    @FXML
    private TableColumn<Map<String, Object>, BigDecimal> amountColumn;

    @FXML
    private Label totalQuantityLabel;
    @FXML
    private Label totalAmountSummaryLabel;

    @FXML
    private TextArea rejectReasonArea;

    private final ObservableList<Map<String, Object>> detailList = FXCollections.observableArrayList();
    private final Gson gson = GsonUtil.getGson();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private OutOrder outOrder;

    public OutOrderApproveDialog() {
    }

    public static OutOrderApproveDialog createDialog(OutOrder order) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    OutOrderApproveDialog.class.getResource("/com/teach/javafx/base/outorder-approve-dialog.fxml"));

            javafx.scene.Scene scene = new javafx.scene.Scene(loader.load(), 700, 750);
            scene.getStylesheets().add(OutOrderApproveDialog.class.getResource("/styles/modern-style.css").toExternalForm());

            OutOrderApproveDialog dialog = loader.getController();
            if (dialog == null) {
                throw new RuntimeException("无法获取控制器实例");
            }

            dialog.setScene(scene);
            dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialog.setTitle("出库单审批");
            dialog.setResizable(false);

            dialog.outOrder = order;
            dialog.initData();
            dialog.loadDetailData();

            return dialog;
        } catch (Exception e) {
            System.err.println("加载审批对话框失败: " + e.getMessage());
            e.printStackTrace();
            MessageDialog.showDialog("打开审批对话框失败：" + e.getMessage());
            return null;
        }
    }

    private void initData() {
        orderNoLabel.setText(outOrder.getOrderNo());
        applicantLabel.setText(outOrder.getApplicantName());
        outTypeLabel.setText(getOutTypeName(outOrder.getOutType()));
        applyTimeLabel.setText(outOrder.getApplyTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        totalAmountLabel.setText("¥" + String.format("%.2f", outOrder.getTotalAmount()));

        // 初始化合计标签
        totalQuantityLabel.setText("0");
        totalAmountSummaryLabel.setText("¥0.00");

        // 初始化表格
        detailTableView.setItems(detailList);

        // 序号列
        serialNumberColumn.setCellValueFactory(param -> {
            int index = detailList.indexOf(param.getValue()) + 1;
            return new SimpleObjectProperty<>(index);
        });

        materialCodeColumn.setCellValueFactory(param -> {
            String code = (String) param.getValue().get("goodsCode");
            if (code == null || code.isEmpty()) {
                code = (String) param.getValue().get("materialCode");
            }
            if (code == null || code.isEmpty()) {
                code = "-";
            }
            return new SimpleStringProperty(code);
        });

        materialColumn.setCellValueFactory(param -> {
            String materialName = (String) param.getValue().get("goodsName");
            if (materialName == null || materialName.isEmpty()) {
                materialName = (String) param.getValue().get("materialName");
            }
            if (materialName == null || materialName.isEmpty()) {
                materialName = "未知";
            }
            return new SimpleStringProperty(materialName);
        });

        unitPriceColumn.setCellValueFactory(param -> {
            Object price = param.getValue().get("unitPrice");
            if (price instanceof Number) {
                return new SimpleObjectProperty<>(BigDecimal.valueOf(((Number) price).doubleValue()));
            }
            return new SimpleObjectProperty<>(BigDecimal.ZERO);
        });

        quantityColumn.setCellValueFactory(param -> {
            Object quantity = param.getValue().get("outNum");
            if (quantity == null) {
                quantity = param.getValue().get("quantity");
            }
            if (quantity == null) {
                quantity = param.getValue().get("outQuantity");
            }
            if (quantity == null) {
                quantity = param.getValue().get("num");
            }
            if (quantity instanceof Number) {
                return new SimpleObjectProperty<>(((Number) quantity).intValue());
            }
            return new SimpleObjectProperty<>(0);
        });

        amountColumn.setCellValueFactory(param -> {
            // 直接使用规范化后的 totalPrice 字段
            Object totalPrice = param.getValue().get("totalPrice");
            if (totalPrice instanceof Number) {
                return new SimpleObjectProperty<>(BigDecimal.valueOf(((Number) totalPrice).doubleValue()));
            }
            return new SimpleObjectProperty<>(BigDecimal.ZERO);
        });
    }

    private void loadDetailData() {
        new Thread(() -> {
            try {
                System.out.println("\n=== [审批对话框] 开始加载明细 ===");
                System.out.println("出库单ID: " + outOrder.getId());

                String[] possibleUrls = {
                    HttpRequestUtil.serverUrl + "/api/stockOut/getDetail/" + outOrder.getId(),
                    HttpRequestUtil.serverUrl + "/api/stockOut/detail/" + outOrder.getId(),
                    HttpRequestUtil.serverUrl + "/stockOut/detail/" + outOrder.getId(),
                    HttpRequestUtil.serverUrl + "/api/outOrder/detail/" + outOrder.getId()
                };
                
                String url = null;
                HttpResponse<String> response = null;
                
                for (String testUrl : possibleUrls) {
                    try {
                        System.out.println("尝试URL: " + testUrl);
                        
                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(testUrl))
                                .GET()
                                .headers("satoken", AppStore.getJwt().getToken())
                                .build();

                        response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                        
                        System.out.println("HTTP状态码: " + response.statusCode());
                        
                        if (response.statusCode() == 200) {
                            url = testUrl;
                            System.out.println("✓ 找到正确的URL: " + url);
                            break;
                        } else {
                            System.out.println("✗ 此URL不可用，响应: " + response.body());
                        }
                    } catch (Exception e) {
                        System.out.println("✗ 请求失败: " + e.getMessage());
                    }
                }
                
                if (url == null || response == null || response.statusCode() != 200) {
                    System.err.println("\n所有URL都失败了！");
                    javafx.application.Platform.runLater(() -> {
                        MessageDialog.showDialog("无法加载出库单明细，请检查后端接口");
                    });
                    return;
                }

                System.out.println("响应内容: " + response.body());

                Map<String, Object> result = gson.fromJson(response.body(), Map.class);
                int code = (result.get("code") instanceof Number) ? ((Number) result.get("code")).intValue() : -1;

                if (code == 200 || code == 0) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) result.get("data");

                    if (data != null) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("items");

                        if (items != null && !items.isEmpty()) {
                            System.out.println("明细数量: " + items.size());
                            System.out.println("明细字段: " + items.get(0).keySet());
                            
                            System.out.println("第一条明细数据: " + items.get(0));
                            
                            Map<String, Object> firstItem = items.get(0);
                            System.out.println("  - goodsCode: " + firstItem.get("goodsCode"));
                            System.out.println("  - goodsName: " + firstItem.get("goodsName"));
                            System.out.println("  - materialCode: " + firstItem.get("materialCode"));
                            System.out.println("  - materialName: " + firstItem.get("materialName"));
                            System.out.println("  - unitPrice: " + firstItem.get("unitPrice"));
                            System.out.println("  - outNum: " + firstItem.get("outNum"));
                            System.out.println("  - quantity: " + firstItem.get("quantity"));
                            System.out.println("  - totalPrice: " + firstItem.get("totalPrice"));
                            System.out.println("  - amount: " + firstItem.get("amount"));

                            javafx.application.Platform.runLater(() -> {
                                detailList.clear();
                                
                                // 规范化字段名，统一处理
                                List<Map<String, Object>> normalizedItems = new ArrayList<>();
                                for (Map<String, Object> item : items) {
                                    Map<String, Object> normalized = new HashMap<>(item);
                                    
                                    // 统一物资名称字段
                                    String materialName = (String) item.get("materialName");
                                    if (materialName == null || materialName.isEmpty()) {
                                        materialName = (String) item.get("goodsName");
                                    }
                                    normalized.put("materialName", materialName != null ? materialName : "未知");
                                    normalized.put("goodsName", materialName);
                                    
                                    // 统一物资编号字段
                                    String materialCode = (String) item.get("materialCode");
                                    if (materialCode == null || materialCode.isEmpty()) {
                                        materialCode = (String) item.get("goodsCode");
                                    }
                                    normalized.put("materialCode", materialCode != null ? materialCode : "-");
                                    normalized.put("goodsCode", materialCode);
                                    
                                    // 统一数量字段
                                    Object quantity = item.get("outNum");
                                    if (quantity == null) {
                                        quantity = item.get("quantity");
                                    }
                                    if (quantity == null) {
                                        quantity = item.get("outQuantity");
                                    }
                                    if (quantity == null) {
                                        quantity = item.get("num");
                                    }
                                    if (quantity == null) {
                                        quantity = item.get("materialOutNum");
                                    }
                                    int qty = quantity instanceof Number ? ((Number) quantity).intValue() : 0;
                                    normalized.put("outNum", qty);
                                    normalized.put("quantity", qty);
                                    
                                    // 统一总金额字段
                                    Object totalPrice = item.get("totalPrice");
                                    if (totalPrice == null) {
                                        totalPrice = item.get("amount");
                                    }
                                    if (totalPrice == null && item.get("unitPrice") != null && qty > 0) {
                                        Object unitPriceObj = item.get("unitPrice");
                                        BigDecimal unitPrice = unitPriceObj instanceof Number ? 
                                            BigDecimal.valueOf(((Number) unitPriceObj).doubleValue()) : BigDecimal.ZERO;
                                        totalPrice = unitPrice.multiply(BigDecimal.valueOf(qty));
                                    }
                                    normalized.put("totalPrice", totalPrice instanceof Number ? 
                                        BigDecimal.valueOf(((Number) totalPrice).doubleValue()) : BigDecimal.ZERO);
                                    normalized.put("amount", totalPrice);
                                    
                                    // 统一单价字段
                                    Object unitPrice = item.get("unitPrice");
                                    normalized.put("unitPrice", unitPrice instanceof Number ? 
                                        BigDecimal.valueOf(((Number) unitPrice).doubleValue()) : BigDecimal.ZERO);
                                    
                                    normalizedItems.add(normalized);
                                }
                                
                                List<Map<String, Object>> filteredItems = normalizedItems.stream()
                                    .filter(item -> {
                                        String name = (String) item.get("materialName");
                                        int qty = (int) item.get("outNum");
                                        return name != null && !name.equals("未知") && qty > 0;
                                    })
                                    .collect(Collectors.toList());
                                
                                System.out.println("过滤后有效物资数量: " + filteredItems.size());
                                
                                detailList.addAll(filteredItems);
                                
                                int totalQuantity = filteredItems.stream()
                                    .mapToInt(item -> (int) item.get("outNum"))
                                    .sum();
                                
                                BigDecimal totalAmount = filteredItems.stream()
                                    .map(item -> (BigDecimal) item.get("totalPrice"))
                                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                                
                                totalQuantityLabel.setText(String.valueOf(totalQuantity));
                                totalAmountSummaryLabel.setText("¥" + String.format("%.2f", totalAmount));
                                
                                System.out.println("✅ 明细加载成功 - 总数量: " + totalQuantity + ", 总金额: " + totalAmount);
                            });
                        } else {
                            System.out.println("️ 没有明细数据");
                            javafx.application.Platform.runLater(() -> {
                                MessageDialog.showDialog("该出库单没有明细数据");
                            });
                        }
                    } else {
                        System.out.println("❌ data 为 null");
                    }
                } else {
                    System.err.println("❌ 业务错误: " + result.get("msg"));
                    javafx.application.Platform.runLater(() -> {
                        MessageDialog.showDialog("加载明细失败：" + result.get("msg"));
                    });
                }
            } catch (Exception e) {
                System.err.println("❌ 加载明细数据失败: " + e.getMessage());
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    MessageDialog.showDialog("加载明细异常：" + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    protected void onApproveButtonClick() {
        System.out.println("✅ 用户点击批准按钮");
        approveOutOrder(true, null);
    }

    @FXML
    protected void onRejectButtonClick() {
        String rejectReason = rejectReasonArea.getText();
        if (rejectReason == null || rejectReason.trim().isEmpty()) {
            MessageDialog.showDialog("请填写驳回理由");
            return;
        }
        System.out.println("❌ 用户点击驳回按钮，理由: " + rejectReason);
        approveOutOrder(false, rejectReason);
    }

    @FXML
    protected void onCancelButtonClick() {
        System.out.println("⚠️ 用户点击取消按钮");
        close();
    }

    private void approveOutOrder(boolean approved, String rejectReason) {
        new Thread(() -> {
            try {
                Map<String, Object> requestBody = new java.util.HashMap<>();
                requestBody.put("orderId", outOrder.getId());
                requestBody.put("status", approved ? 1 : 2);
                if (rejectReason != null && !rejectReason.trim().isEmpty()) {
                    requestBody.put("rejectReason", rejectReason);
                }

                System.out.println("\n=== [审批对话框] 审批请求 ===");
                System.out.println("请求URL: " + HttpRequestUtil.serverUrl + "/api/stockOut/approve");
                System.out.println("请求方法: PUT");
                System.out.println("Token: " + AppStore.getJwt().getToken());
                System.out.println("请求体: " + gson.toJson(requestBody));
                System.out.println("审批结果: " + (approved ? "批准" : "驳回"));
                System.out.println("出库单ID: " + outOrder.getId());
                System.out.println("出库单号: " + outOrder.getOrderNo());

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(HttpRequestUtil.serverUrl + "/api/stockOut/approve"))
                        .PUT(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                        .headers("Content-Type", "application/json")
                        .headers("satoken", AppStore.getJwt().getToken())
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                System.out.println("响应状态码: " + response.statusCode());
                System.out.println("响应内容: " + response.body());

                if (response.statusCode() == 200) {
                    Map<String, Object> result = gson.fromJson(response.body(), Map.class);
                    int code = (result.get("code") instanceof Number) ? ((Number) result.get("code")).intValue() : -1;

                    javafx.application.Platform.runLater(() -> {
                        if (code == 200 || code == 0) {
                            MessageDialog.showDialog(approved ? "批准成功" : "驳回成功");
                            close();
                        } else {
                            String errorMsg = result.get("msg") != null ? result.get("msg").toString() : "未知错误";
                            System.err.println("审批失败，错误信息: " + errorMsg);
                            MessageDialog.showDialog(errorMsg);
                        }
                    });
                } else {
                    String errorBody = response.body();
                    System.err.println("HTTP请求失败，状态码: " + response.statusCode());
                    System.err.println("响应内容: " + errorBody);
                    
                    String errorMsg = "请求失败";
                    if (errorBody != null && !errorBody.isEmpty()) {
                        try {
                            Map<String, Object> errorResult = gson.fromJson(errorBody, Map.class);
                            if (errorResult.get("msg") != null) {
                                errorMsg = errorResult.get("msg").toString();
                            }
                        } catch (Exception e) {
                            errorMsg = errorBody;
                        }
                    }
                    
                    final String finalErrorMsg = errorMsg;
                    javafx.application.Platform.runLater(() -> {
                        MessageDialog.showDialog(finalErrorMsg);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("审批异常: " + e.getMessage());
                javafx.application.Platform.runLater(() -> {
                    MessageDialog.showDialog("审批异常：" + e.getMessage());
                });
            }
        }).start();
    }

    private String getOutTypeName(Integer type) {
        if (type == null) return "领料出库";
        switch (type) {
            case 1: return "领料出库";
            case 2: return "销售出库";
            case 3: return "报损出库";
            case 4: return "其他出库";
            default: return "领料出库";
        }
    }
}
