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
import javafx.scene.layout.Priority;

/**
 * 入库单详情查看弹窗
 */
public class StockInViewDialog extends Stage {

    private final Gson gson = GsonUtil.getGson();
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .build();
    
    private TableView<Map<String, Object>> itemTable;
    private Label totalQuantityLabel;
    private Label totalAmountLabel;

    public StockInViewDialog(StockIn stockIn) {
        initModality(Modality.APPLICATION_MODAL);
        setTitle("入库单明细");
        setResizable(true);

        VBox mainContent = new VBox(16);
        mainContent.setStyle("-fx-background-color: #f5f7fa;");
        mainContent.setPadding(new Insets(24, 24, 0, 24));

        // 标题区域
        VBox titleBox = new VBox(8);
        Label titleLabel = new Label("入库单明细");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #1d3f66;");
        
        Separator separator = new Separator();
        separator.setStyle("-fx-background-color: #e5e6eb;");
        
        titleBox.getChildren().addAll(titleLabel, separator);

        // 基本信息卡片
        VBox infoCard = createInfoCard(stockIn);

        // 物资明细卡片
        VBox tableCard = createTableCard();

        mainContent.getChildren().addAll(titleBox, infoCard, tableCard);
        VBox.setMargin(tableCard, new Insets(0, 0, 24, 0));

        Scene scene = new Scene(mainContent, 750, 650);
        scene.getStylesheets().add(getClass().getResource("/styles/modern-style.css").toExternalForm());
        setScene(scene);

        // 加载物资明细数据
        loadStockInItems(stockIn);
    }

    /**
     * 创建基本信息卡片
     */
    private VBox createInfoCard(StockIn stockIn) {
        VBox infoCard = new VBox(0);
        infoCard.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 0, 2); -fx-padding: 20 24 20 24;");

        // 第一行：入库单号、申请人、审批人
        HBox row1 = new HBox(0);
        row1.setSpacing(24);
        row1.getChildren().addAll(
            createInfoField("入库单号", stockIn.getInCode(), 180),
            createInfoField("申请人", stockIn.getApplyUserName(), 180),
            createInfoField("审批人", getApproveUserName(stockIn), 180)
        );

        // 第二行：入库类型、状态
        HBox row2 = new HBox(0);
        row2.setSpacing(24);
        row2.getChildren().addAll(
            createInfoField("入库类型", getTypeName(stockIn.getType()), 180),
            createInfoField("状态", getStatusName(stockIn.getStatus()), 180)
        );

        // 第三行：申请时间、审批时间
        HBox row3 = new HBox(0);
        row3.setSpacing(24);
        row3.getChildren().addAll(
            createInfoField("申请时间", stockIn.getCreateTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), 180),
            createInfoField("审批时间", stockIn.getApproveTime() != null ? 
                stockIn.getApproveTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "-", 180)
        );

        infoCard.getChildren().addAll(row1, row2, row3);
        VBox.setMargin(row1, new Insets(0, 0, 12, 0));
        VBox.setMargin(row2, new Insets(0, 0, 12, 0));

        return infoCard;
    }

    /**
     * 创建信息字段（标签+值）
     */
    private VBox createInfoField(String label, String value) {
        return createInfoField(label, value, -1);
    }
    
    /**
     * 创建信息字段（标签+值），可指定宽度
     */
    private VBox createInfoField(String label, String value, double width) {
        VBox field = new VBox(4);
        if (width > 0) {
            field.setPrefWidth(width);
            field.setMaxWidth(width);
            field.setMinWidth(width);
        } else {
            HBox.setHgrow(field, Priority.ALWAYS);
        }
        
        Label labelNode = new Label(label);
        labelNode.setStyle("-fx-font-size: 12px; -fx-text-fill: #86909c;");
        
        Label valueNode = new Label(value);
        valueNode.setStyle("-fx-font-size: 14px; -fx-text-fill: #1d3f66; -fx-font-weight: bold;");
        valueNode.setWrapText(true);
        valueNode.setAlignment(Pos.TOP_LEFT);
        
        field.getChildren().addAll(labelNode, valueNode);
        return field;
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
     * 创建物资明细卡片
     */
    private VBox createTableCard() {
        VBox tableCard = new VBox(12);
        tableCard.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 0, 2); -fx-padding: 20 24 20 24;");

        Label tableTitle = new Label("物资明细");
        tableTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1d3f66;");

        itemTable = new TableView<>();
        itemTable.setStyle("-fx-background-color: white; -fx-border-color: #f0f0f0; -fx-border-width: 1; -fx-border-radius: 6; -fx-padding: 2;");
        itemTable.setMinHeight(180);
        itemTable.setPrefHeight(200);
        itemTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // 物资名称列 - 左对齐，垂直居中
        TableColumn<Map<String, Object>, String> materialColumn = new TableColumn<>("物资名称");
        materialColumn.setPrefWidth(160);
        materialColumn.setCellValueFactory(data -> {
            Map<String, Object> item = data.getValue();
            String value = item != null ? (String) item.get("materialName") : null;
            return new javafx.beans.property.SimpleStringProperty(value);
        });
        materialColumn.setCellFactory(col -> new TableCell<Map<String, Object>, String>() {
            {
                setStyle("-fx-alignment: CENTER_LEFT;");
            }
            
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                }
            }
        });

        // 单价列 - 居中对齐
        TableColumn<Map<String, Object>, BigDecimal> priceColumn = new TableColumn<>("单价");
        priceColumn.setPrefWidth(100);
        priceColumn.setCellValueFactory(data -> {
            Map<String, Object> item = data.getValue();
            Object value = item != null ? item.get("price") : null;
            BigDecimal decimalValue = value != null ? new BigDecimal(value.toString()) : null;
            return new javafx.beans.property.SimpleObjectProperty<>(decimalValue);
        });
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

        // 数量列 - 居中对齐
        TableColumn<Map<String, Object>, Integer> quantityColumn = new TableColumn<>("数量");
        quantityColumn.setPrefWidth(100);
        quantityColumn.setCellValueFactory(data -> {
            Map<String, Object> item = data.getValue();
            Object value = item != null ? item.get("quantity") : null;
            Integer intValue = value != null ? ((Number) value).intValue() : null;
            return new javafx.beans.property.SimpleObjectProperty<>(intValue);
        });
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

        // 总金额列 - 居中对齐
        TableColumn<Map<String, Object>, BigDecimal> totalAmountColumn = new TableColumn<>("总金额");
        totalAmountColumn.setPrefWidth(120);
        totalAmountColumn.setCellValueFactory(data -> {
            Map<String, Object> item = data.getValue();
            Object value = item != null ? item.get("totalAmount") : null;
            if (value == null) {
                value = item != null ? item.get("amount") : null;
            }
            BigDecimal decimalValue = value != null ? new BigDecimal(value.toString()) : null;
            return new javafx.beans.property.SimpleObjectProperty<>(decimalValue);
        });
        totalAmountColumn.setCellFactory(col -> new TableCell<Map<String, Object>, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setAlignment(Pos.CENTER);
                } else {
                    setText(String.format("¥%.2f", item));
                    setAlignment(Pos.CENTER);
                }
            }
        });

        itemTable.getColumns().addAll(materialColumn, priceColumn, quantityColumn, totalAmountColumn);

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

        // 合计行
        HBox summaryBox = new HBox(24);
        summaryBox.setAlignment(Pos.CENTER_LEFT);
        summaryBox.setStyle("-fx-padding: 12 0 0 0; -fx-border-color: #e5e6eb; -fx-border-width: 1 0 0 0;");
        
        Label totalQtyLabel = new Label("总数量:");
        totalQtyLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1d3f66;");
        
        totalQuantityLabel = new Label("0");
        totalQuantityLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #389e0d; -fx-font-weight: bold;");
        HBox.setHgrow(totalQuantityLabel, Priority.ALWAYS);
        
        Label totalAmtLabel = new Label("合计总金额:");
        totalAmtLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1d3f66;");
        
        totalAmountLabel = new Label("¥0.00");
        totalAmountLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #f53f3f; -fx-font-weight: bold;");
        HBox.setHgrow(totalAmountLabel, Priority.ALWAYS);
        
        summaryBox.getChildren().addAll(totalQtyLabel, totalQuantityLabel, totalAmtLabel, totalAmountLabel);

        tableCard.getChildren().addAll(tableTitle, itemTable, summaryBox);
        VBox.setVgrow(itemTable, Priority.ALWAYS);

        return tableCard;
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
                                
                                // 计算合计
                                int totalQuantity = items.stream()
                                    .mapToInt(item -> {
                                        Object quantity = item.get("quantity");
                                        return quantity instanceof Number ? ((Number) quantity).intValue() : 0;
                                    })
                                    .sum();
                                
                                BigDecimal totalAmount = items.stream()
                                    .map(item -> {
                                        Object value = item.get("totalAmount");
                                        if (value == null) {
                                            value = item.get("amount");
                                        }
                                        return value != null ? new BigDecimal(value.toString()) : BigDecimal.ZERO;
                                    })
                                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                                
                                totalQuantityLabel.setText(String.valueOf(totalQuantity));
                                totalAmountLabel.setText("¥" + String.format("%.2f", totalAmount));
                                
                                System.out.println("=== [查看详情] 合计 - 总数量: " + totalQuantity + ", 总金额: " + totalAmount);
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
