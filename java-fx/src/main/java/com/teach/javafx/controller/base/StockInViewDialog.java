package com.teach.javafx.controller.base;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.teach.javafx.AppStore;
import com.teach.javafx.GsonUtil;
import com.teach.javafx.models.StockIn;
import com.teach.javafx.request.HttpRequestUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

/**
 * 入库单详情查看弹窗
 */
public class StockInViewDialog extends Stage {

    private final Gson gson = GsonUtil.getGson();
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .build();
    
    private TableView<Map<String, Object>> itemTable;

    public StockInViewDialog(StockIn stockIn) {
        initModality(Modality.APPLICATION_MODAL);
        setTitle("入库单详情");
        setResizable(true);

        VBox mainContent = new VBox(20);
        mainContent.setStyle("-fx-background-color: linear-gradient(to bottom, #f5f7fa, #ffffff); -fx-padding: 25;");

        // 标题
        Label titleLabel = new Label("入库单详情");
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Separator separator = new Separator();
        separator.setStyle("-fx-background-color: #e0e6ed;");

        HBox titleBox = new HBox(10, titleLabel);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        // 基本信息区域
        VBox infoBox = createInfoBox(stockIn);

        // 物资明细表格
        VBox tableBox = createTableBox();

        mainContent.getChildren().addAll(titleBox, separator, infoBox, tableBox);

        Scene scene = new Scene(mainContent, 750, 550);
        scene.getStylesheets().add(getClass().getResource("/styles/modern-style.css").toExternalForm());
        setScene(scene);

        // 加载物资明细数据
        loadStockInItems(stockIn);
    }

    /**
     * 创建基本信息区域
     */
    private VBox createInfoBox(StockIn stockIn) {
        VBox infoBox = new VBox(12);
        infoBox.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-padding: 15; -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.05), 5, 0, 0, 1);");

        Label inCodeLabel = createInfoLabel("入库单号：", stockIn.getInCode());
        Label applyUserLabel = createInfoLabel("申请人：", stockIn.getApplyUserName());
        Label approveUserLabel = createInfoLabel("审批人：", getApproveUserName(stockIn));
        Label typeLabel = createInfoLabel("入库类型：", getTypeName(stockIn.getType()));
        Label statusLabel = createInfoLabel("状态：", getStatusName(stockIn.getStatus()));
        Label createTimeLabel = createInfoLabel("申请时间：", stockIn.getCreateTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        Label approveTimeLabel = createInfoLabel("审批时间：", stockIn.getApproveTime() != null ? stockIn.getApproveTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "-");

        infoBox.getChildren().addAll(inCodeLabel, applyUserLabel, approveUserLabel, typeLabel, statusLabel, createTimeLabel, approveTimeLabel);

        return infoBox;
    }

    /**
     * 获取审批人名称
     */
    private String getApproveUserName(StockIn stockIn) {
        // 待审批状态（status=0）时，审批人为无
        if (stockIn.getStatus() == 0) {
            return "无";
        }
        // 已入库（status=1或3）或已驳回（status=2）状态，显示审批人姓名
        return stockIn.getApproveUserName() != null && !stockIn.getApproveUserName().isEmpty() 
            ? stockIn.getApproveUserName() 
            : "无";
    }

    /**
     * 创建信息标签
     */
    private Label createInfoLabel(String prefix, String value) {
        Label label = new Label(prefix + value);
        label.setStyle("-fx-font-size: 13px; -fx-text-fill: #546e7a;");
        return label;
    }

    /**
     * 创建物资明细表格区域
     */
    private VBox createTableBox() {
        VBox tableBox = new VBox(10);
        tableBox.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-padding: 15; -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.05), 5, 0, 0, 1);");

        Label tableTitle = new Label("物资明细");
        tableTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        itemTable = new TableView<>();
        itemTable.setStyle("-fx-background-color: white; -fx-background-radius: 4;");
        itemTable.setMinHeight(180);
        itemTable.setPrefHeight(250);
        itemTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);  // 固定列宽策略

        TableColumn<Map<String, Object>, String> materialColumn = new TableColumn<>("物资名称");
        materialColumn.setCellValueFactory(data -> {
            Map<String, Object> item = data.getValue();
            String value = item != null ? (String) item.get("materialName") : null;
            return new javafx.beans.property.SimpleStringProperty(value);
        });
        materialColumn.setPrefWidth(200);

        TableColumn<Map<String, Object>, BigDecimal> priceColumn = new TableColumn<>("单价");
        priceColumn.setCellValueFactory(data -> {
            Map<String, Object> item = data.getValue();
            Object value = item != null ? item.get("price") : null;
            BigDecimal decimalValue = value != null ? new BigDecimal(value.toString()) : null;
            return new javafx.beans.property.SimpleObjectProperty<>(decimalValue);
        });
        priceColumn.setPrefWidth(120);
        priceColumn.setCellFactory(col -> new TableCell<Map<String, Object>, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setAlignment(Pos.CENTER);
                } else {
                    setText(String.format("%.2f", item));
                    setAlignment(Pos.CENTER);
                }
            }
        });

        TableColumn<Map<String, Object>, Integer> quantityColumn = new TableColumn<>("数量");
        quantityColumn.setCellValueFactory(data -> {
            Map<String, Object> item = data.getValue();
            Object value = item != null ? item.get("quantity") : null;
            Integer intValue = value != null ? ((Number) value).intValue() : null;
            return new javafx.beans.property.SimpleObjectProperty<>(intValue);
        });
        quantityColumn.setPrefWidth(100);
        quantityColumn.setCellFactory(col -> new TableCell<Map<String, Object>, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setAlignment(Pos.CENTER);
                } else {
                    setText(item.toString());
                    setAlignment(Pos.CENTER);
                }
            }
        });

        TableColumn<Map<String, Object>, BigDecimal> totalAmountColumn = new TableColumn<>("总金额");
        totalAmountColumn.setCellValueFactory(data -> {
            Map<String, Object> item = data.getValue();
            Object value = item != null ? item.get("totalAmount") : null;
            // 兼容 amount 字段
            if (value == null) {
                value = item != null ? item.get("amount") : null;
            }
            BigDecimal decimalValue = value != null ? new BigDecimal(value.toString()) : null;
            return new javafx.beans.property.SimpleObjectProperty<>(decimalValue);
        });
        totalAmountColumn.setPrefWidth(120);
        totalAmountColumn.setCellFactory(col -> new TableCell<Map<String, Object>, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setAlignment(Pos.CENTER);
                } else {
                    setText(String.format("%.2f", item));
                    setAlignment(Pos.CENTER);
                }
            }
        });

        itemTable.getColumns().addAll(materialColumn, priceColumn, quantityColumn, totalAmountColumn);

        // 设置行样式
        itemTable.setRowFactory(tv -> new TableRow<Map<String, Object>>() {
            @Override
            protected void updateItem(Map<String, Object> item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setStyle("");
                } else {
                    if (getIndex() % 2 == 0) {
                        setStyle("-fx-background-color: #fafbfc;");
                    } else {
                        setStyle("-fx-background-color: white;");
                    }
                }
            }
        });

        tableBox.getChildren().addAll(tableTitle, itemTable);
        VBox.setVgrow(itemTable, Priority.ALWAYS);

        return tableBox;
    }

    /**
     * 加载物资明细数据
     */
    private void loadStockInItems(StockIn stockIn) {
        new Thread(() -> {
            try {
                String url = HttpRequestUtil.serverUrl + "/stock-in/detail/" + stockIn.getId();
                System.out.println("=== [查看详情] 请求URL: " + url);

                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .headers("satoken", AppStore.getJwt().getToken())
                        .build();

                HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                System.out.println("=== [查看详情] 响应状态码: " + response.statusCode());
                System.out.println("=== [查看详情] 响应内容: " + response.body());

                if (response.statusCode() == 200) {
                    Map<String, Object> resultMap = gson.fromJson(response.body(), new TypeToken<Map<String, Object>>(){}.getType());

                    if (resultMap.get("code").equals(200.0)) {
                        Map<String, Object> data = (Map<String, Object>) resultMap.get("data");
                        System.out.println("=== [查看详情] data对象: " + data);
                        
                        List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("items");
                        System.out.println("=== [查看详情] items列表: " + items);
                        System.out.println("=== [查看详情] items数量: " + (items != null ? items.size() : "null"));

                        javafx.application.Platform.runLater(() -> {
                            if (items != null && !items.isEmpty()) {
                                // 打印每个item的字段
                                for (int i = 0; i < items.size(); i++) {
                                    Map<String, Object> item = items.get(i);
                                    System.out.println("=== [查看详情] item[" + i + "] 字段: " + item.keySet());
                                    System.out.println("=== [查看详情] item[" + i + "] 内容: " + item);
                                }
                                
                                itemTable.getItems().setAll(items);
                                System.out.println("=== [查看详情] 已填充 " + items.size() + " 条数据到表格");
                            } else {
                                System.out.println("=== [查看详情] items为空或null");
                                MessageDialog.showDialog("该入库单没有物资明细");
                            }
                        });
                    } else {
                        System.out.println("=== [查看详情] 业务错误: " + resultMap.get("msg"));
                        javafx.application.Platform.runLater(() -> {
                            MessageDialog.showDialog("加载物资明细失败：" + resultMap.get("msg"));
                        });
                    }
                } else {
                    System.out.println("=== [查看详情] HTTP错误: " + response.statusCode());
                    javafx.application.Platform.runLater(() -> {
                        MessageDialog.showDialog("请求失败，状态码：" + response.statusCode());
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

    /**
     * 显示入库单详情弹窗
     */
    public static void showDialog(StockIn stockIn) {
        StockInViewDialog dialog = new StockInViewDialog(stockIn);
        dialog.showAndWait();
    }

    private String getTypeName(Integer type) {
        if (type == null) return "未知";
        switch (type) {
            case 1: return "采购入库";
            case 2: return "退货入库";
            case 3: return "其他入库";
            default: return "未知";
        }
    }

    private String getStatusName(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "待审批";
            case 1: return "已入库";
            case 2: return "已驳回";
            case 3: return "已完成";
            default: return "未知";
        }
    }
}
