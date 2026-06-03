package com.teach.javafx.controller.base;

import com.google.gson.Gson;
import com.teach.javafx.AppStore;
import com.teach.javafx.GsonUtil;
import com.teach.javafx.bean.OutOrder;
import com.teach.javafx.request.HttpRequestUtil;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

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
    private TableColumn<Map<String, Object>, String> materialColumn;
    @FXML
    private TableColumn<Map<String, Object>, BigDecimal> unitPriceColumn;
    @FXML
    private TableColumn<Map<String, Object>, Integer> quantityColumn;
    @FXML
    private TableColumn<Map<String, Object>, BigDecimal> amountColumn;

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

        // 初始化表格
        detailTableView.setItems(detailList);

        materialColumn.setCellValueFactory(param -> new SimpleStringProperty(
                (String) param.getValue().getOrDefault("materialName", "未知")));

        unitPriceColumn.setCellValueFactory(param -> {
            Object price = param.getValue().get("unitPrice");
            if (price instanceof Number) {
                return new SimpleObjectProperty<>(BigDecimal.valueOf(((Number) price).doubleValue()));
            }
            return new SimpleObjectProperty<>(BigDecimal.ZERO);
        });

        quantityColumn.setCellValueFactory(param -> {
            Object quantity = param.getValue().get("outQuantity");
            if (quantity instanceof Number) {
                return new SimpleObjectProperty<>(((Number) quantity).intValue());
            }
            return new SimpleObjectProperty<>(0);
        });

        amountColumn.setCellValueFactory(param -> {
            Object price = param.getValue().get("unitPrice");
            BigDecimal unitPrice = price instanceof Number ? BigDecimal.valueOf(((Number) price).doubleValue()) : BigDecimal.ZERO;

            Object quantity = param.getValue().get("outQuantity");
            Integer qty = quantity instanceof Number ? ((Number) quantity).intValue() : 0;

            return new SimpleObjectProperty<>(unitPrice.multiply(BigDecimal.valueOf(qty)));
        });
    }

    private void loadDetailData() {
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

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                System.out.println("响应状态码: " + response.statusCode());
                System.out.println("响应内容: " + response.body());

                if (response.statusCode() == 200) {
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

                                javafx.application.Platform.runLater(() -> {
                                    detailList.clear();
                                    detailList.addAll(items);
                                    System.out.println("✅ 明细加载成功");
                                });
                            } else {
                                System.out.println("⚠️ 没有明细数据");
                            }
                        } else {
                            System.out.println("❌ data 为 null");
                        }
                    } else {
                        System.err.println("❌ 业务错误: " + result.get("msg"));
                    }
                } else {
                    System.err.println("❌ HTTP错误: " + response.statusCode());
                }
            } catch (Exception e) {
                System.err.println("❌ 加载明细数据失败: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    protected void onApproveButtonClick() {
        System.out.println("✅ 用户点击批准按钮");
        close();
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
        close();
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
                            // 刷新列表
                            if (getOwner() != null && getOwner().getScene() != null) {
                                Object controller = getOwner().getScene().getUserData();
                                if (controller instanceof OutOrderListController) {
                                    ((OutOrderListController) controller).loadOutOrderList();
                                }
                            }
                        } else {
                            String errorMsg = result.get("msg") != null ? result.get("msg").toString() : "未知错误";
                            System.err.println("审批失败，错误信息: " + errorMsg);
                            MessageDialog.showDialog("审批失败：" + errorMsg);
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
