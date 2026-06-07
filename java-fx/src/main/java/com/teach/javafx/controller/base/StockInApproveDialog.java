package com.teach.javafx.controller.base;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.teach.javafx.AppStore;
import com.teach.javafx.GsonUtil;
import com.teach.javafx.models.StockIn;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StockInApproveDialog extends Stage {

    @FXML
    private Label orderNoLabel;
    @FXML
    private Label applicantLabel;
    @FXML
    private Label inTypeLabel;
    @FXML
    private Label applyTimeLabel;
    @FXML
    private Label totalAmountLabel;

    @FXML
    private TableView<Map<String, Object>> detailTableView;
    @FXML
    private TableColumn<Map<String, Object>, String> serialNumberColumn;
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
    private StockIn stockIn;
    private Runnable onApproveCallback;

    public void setOnApproveCallback(Runnable callback) {
        this.onApproveCallback = callback;
    }

    // ... existing code ...

    public static StockInApproveDialog createDialog(StockIn stockIn) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    StockInApproveDialog.class.getResource("/com/teach/javafx/base/stockin-approve-dialog.fxml"));

            javafx.scene.Scene scene = new javafx.scene.Scene(loader.load(), 700, 750);
            scene.setFill(javafx.scene.paint.Color.WHITE);
            scene.getStylesheets().add(StockInApproveDialog.class.getResource("/styles/modern-style.css").toExternalForm());

            StockInApproveDialog dialog = loader.getController();
            if (dialog == null) {
                throw new RuntimeException("无法获取控制器实例");
            }

            dialog.setScene(scene);
            dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialog.setTitle("入库单审批");
            dialog.setResizable(false);

            dialog.stockIn = stockIn;
            dialog.initData();
            dialog.loadDetailData();

            return dialog;
        } catch (Exception e) {
            e.printStackTrace();
            MessageDialog.showDialog("打开审批对话框失败：" + e.getMessage());
            return null;
        }
    }

    private void initData() {
        orderNoLabel.setText(stockIn.getInCode());
        applicantLabel.setText(stockIn.getApplyUserName());
        inTypeLabel.setText(getTypeName(stockIn.getType()));
        applyTimeLabel.setText(stockIn.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        totalAmountLabel.setText("¥" + String.format("%.2f", stockIn.getTotalAmount()));

        totalQuantityLabel.setText("0");
        totalAmountSummaryLabel.setText("¥0.00");

        detailTableView.setItems(detailList);

        serialNumberColumn.setCellValueFactory(param -> {
            int index = detailList.indexOf(param.getValue());
            return new SimpleStringProperty(String.valueOf(index + 1));
        });

        materialCodeColumn.setCellValueFactory(param -> {
            String code = (String) param.getValue().get("materialCode");
            if (code == null || code.isEmpty()) {
                code = (String) param.getValue().get("goodsCode");
            }
            if (code == null || code.isEmpty()) {
                code = (String) param.getValue().get("code");
            }
            return new SimpleStringProperty(code != null ? code : "-");
        });

        materialColumn.setCellValueFactory(param -> {
            String name = (String) param.getValue().get("materialName");
            if (name == null || name.isEmpty()) {
                name = (String) param.getValue().get("goodsName");
            }
            if (name == null || name.isEmpty()) {
                name = (String) param.getValue().get("name");
            }
            return new SimpleStringProperty(name != null ? name : "未知");
        });

        unitPriceColumn.setCellValueFactory(param -> {
            Object price = param.getValue().get("price");
            if (price == null) {
                price = param.getValue().get("unitPrice");
            }
            if (price instanceof Number) {
                return new SimpleObjectProperty<>(BigDecimal.valueOf(((Number) price).doubleValue()));
            }
            return new SimpleObjectProperty<>(BigDecimal.ZERO);
        });

        quantityColumn.setCellValueFactory(param -> {
            Object quantity = param.getValue().get("quantity");
            if (quantity == null) {
                quantity = param.getValue().get("num");
            }
            if (quantity instanceof Number) {
                return new SimpleObjectProperty<>(((Number) quantity).intValue());
            }
            return new SimpleObjectProperty<>(0);
        });

        amountColumn.setCellValueFactory(param -> {
            Object price = param.getValue().get("price");
            if (price == null) {
                price = param.getValue().get("unitPrice");
            }
            BigDecimal unitPrice = price instanceof Number ? BigDecimal.valueOf(((Number) price).doubleValue()) : BigDecimal.ZERO;
            Object quantity = param.getValue().get("quantity");
            if (quantity == null) {
                quantity = param.getValue().get("num");
            }
            Integer qty = quantity instanceof Number ? ((Number) quantity).intValue() : 0;
            return new SimpleObjectProperty<>(unitPrice.multiply(BigDecimal.valueOf(qty)));
        });
    }

    private void loadDetailData() {
        new Thread(() -> {
            try {
                String url = HttpRequestUtil.serverUrl + "/stock-in/detail/" + stockIn.getId();
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
                        @SuppressWarnings("unchecked")
                        Map<String, Object> data = (Map<String, Object>) result.get("data");
                        if (data != null) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("items");
                            if (items != null && !items.isEmpty()) {
                                javafx.application.Platform.runLater(() -> {
                                    detailList.clear();
                                    detailList.addAll(items);

                                    int totalQuantity = items.stream()
                                            .mapToInt(item -> {
                                                Object quantity = item.get("quantity");
                                                return quantity instanceof Number ? ((Number) quantity).intValue() : 0;
                                            }).sum();

                                    BigDecimal totalAmount = items.stream()
                                            .map(item -> {
                                                Object price = item.get("price");
                                                BigDecimal unitPrice = price instanceof Number ?
                                                        BigDecimal.valueOf(((Number) price).doubleValue()) : BigDecimal.ZERO;
                                                Object quantity = item.get("quantity");
                                                int qty = quantity instanceof Number ? ((Number) quantity).intValue() : 0;
                                                return unitPrice.multiply(BigDecimal.valueOf(qty));
                                            }).reduce(BigDecimal.ZERO, BigDecimal::add);

                                    totalQuantityLabel.setText(String.valueOf(totalQuantity));
                                    totalAmountSummaryLabel.setText("¥" + String.format("%.2f", totalAmount));
                                });
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() ->
                        MessageDialog.showDialog("加载明细异常：" + e.getMessage()));
            }
        }).start();
    }

    @FXML
    protected void onApproveButtonClick() {
        close();
        approveStockIn(true, null);
    }

    @FXML
    protected void onRejectButtonClick() {
        String rejectReason = rejectReasonArea.getText();
        if (rejectReason == null || rejectReason.trim().isEmpty()) {
            MessageDialog.showDialog("请填写驳回理由");
            return;
        }
        close();
        approveStockIn(false, rejectReason);
    }

    @FXML
    protected void onCancelButtonClick() {
        close();
    }

    private void approveStockIn(boolean approved, String rejectReason) {
        new Thread(() -> {
            try {
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("stockInId", stockIn.getId());
                requestBody.put("approved", approved);
                if (rejectReason != null && !rejectReason.trim().isEmpty()) {
                    requestBody.put("rejectReason", rejectReason);
                }

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(HttpRequestUtil.serverUrl + "/stock-in/approve"))
                        .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                        .headers("Content-Type", "application/json", "satoken", AppStore.getJwt().getToken())
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    Map<String, Object> result = gson.fromJson(response.body(), Map.class);
                    int code = (result.get("code") instanceof Number) ? ((Number) result.get("code")).intValue() : -1;

                    javafx.application.Platform.runLater(() -> {
                        if (code == 200 || code == 0) {
                            MessageDialog.showDialog(approved ? "批准成功，库存已更新" : "驳回成功");

                            if (onApproveCallback != null) {
                                onApproveCallback.run();
                            }
                        } else {
                            MessageDialog.showDialog("审批失败：" + result.get("msg"));
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() ->
                        MessageDialog.showDialog("审批异常：" + e.getMessage()));
            }
        }).start();
    }

    private String getTypeName(Integer type) {
        if (type == null) return "采购入库";
        switch (type) {
            case 1: return "采购入库";
            case 2: return "退货入库";
            case 3: return "其他入库";
            default: return "采购入库";
        }
    }
}
