package com.teach.javafx.controller.base;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.teach.javafx.AppStore;
import com.teach.javafx.bean.OutOrder;
import com.teach.javafx.bean.OutOrderDetail;
import com.teach.javafx.request.HttpRequestUtil;
import com.teach.javafx.request.OptionItem;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class OutOrderEditDialog {

    private Stage dialog;
    private OutOrder outOrder;
    private boolean isNew;
    private final Gson gson = new Gson();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private ComboBox<String> outTypeComboBox;
    private TextArea remarkArea;
    private TableView<OutOrderDetail> detailTable;
    private ObservableList<OutOrderDetail> detailList;
    private List<OptionItem> materialList;
    private List<Map<String, Object>> materialMapList = new ArrayList<>();
    private Label totalAmountLabel;

    public static OutOrderEditDialog createNewDialog() {
        OutOrderEditDialog dialog = new OutOrderEditDialog();
        dialog.outOrder = new OutOrder();
        dialog.isNew = true;
        dialog.outOrder.setOutType(1);
        dialog.outOrder.setTotalNum(0);
        dialog.outOrder.setTotalAmount(BigDecimal.ZERO);
        return dialog;
    }

    public static OutOrderEditDialog createEditDialog(OutOrder order) {
        OutOrderEditDialog dialog = new OutOrderEditDialog();
        dialog.outOrder = order;
        dialog.isNew = false;
        return dialog;
    }

    public void showAndWait() {
        dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(isNew ? "新增出库单" : "编辑出库单");
        dialog.setMinWidth(900);
        dialog.setMinHeight(600);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(15));

        VBox topBox = new VBox(10);
        topBox.setPadding(new Insets(10));

        HBox typeBox = new HBox(10);
        typeBox.getChildren().addAll(new Label("出库类型："), outTypeComboBox = new ComboBox<>());
        outTypeComboBox.getItems().addAll("领料出库", "销售出库", "报损出库", "其他出库");
        outTypeComboBox.setValue(getOutTypeName(outOrder.getOutType()));

        HBox remarkBox = new HBox(10);
        remarkBox.getChildren().addAll(new Label("备注："));
        remarkArea = new TextArea();
        remarkArea.setPrefRowCount(2);
        remarkArea.setPrefColumnCount(50);
        remarkArea.setText(outOrder.getRemark());
        remarkBox.getChildren().add(remarkArea);

        topBox.getChildren().addAll(typeBox, remarkBox);
        root.setTop(topBox);

        VBox centerBox = new VBox(10);
        centerBox.setPadding(new Insets(10));

        HBox buttonBox = new HBox(10);
        Button addBtn = new Button("添加商品");
        addBtn.setOnAction(e -> onAddItem());
        Button deleteBtn = new Button("删除选中");
        deleteBtn.setOnAction(e -> onDeleteItem());
        buttonBox.getChildren().addAll(addBtn, deleteBtn);

        detailTable = new TableView<>();
        detailTable.setEditable(true);

        TableColumn<OutOrderDetail, String> materialCol = new TableColumn<>("物资名称");
        materialCol.setPrefWidth(200);
        materialCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getGoodsName()));
        materialCol.setCellFactory(param -> new TableCell<OutOrderDetail, String>() {
            private final Button selectBtn = new Button("加载中...");
            {
                selectBtn.setStyle("-fx-background-color: #999; -fx-text-fill: white;");
                selectBtn.setDisable(true);
                selectBtn.setOnAction(event -> {
                    OutOrderDetail detail = getTableView().getItems().get(getIndex());
                    System.out.println("=== 点击选择物资按钮 ===");
                    System.out.println("materialList 是否为null: " + (materialList == null));
                    System.out.println("materialList 大小: " + (materialList != null ? materialList.size() : "N/A"));
                    showMaterialSelectionDialog(detail);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    if (item != null && !item.equals("请选择物资") && !item.equals("加载中...")) {
                        selectBtn.setText(item);
                        selectBtn.setStyle("-fx-background-color: #409eff; -fx-text-fill: white;");
                        selectBtn.setDisable(false);
                    } else if (item != null && item.equals("请选择物资")) {
                        selectBtn.setText("请选择物资");
                        selectBtn.setStyle("-fx-background-color: #409eff; -fx-text-fill: white;");
                        selectBtn.setDisable(false);
                    } else {
                        selectBtn.setText("加载中...");
                        selectBtn.setStyle("-fx-background-color: #999; -fx-text-fill: white;");
                        selectBtn.setDisable(true);
                    }
                    setGraphic(selectBtn);
                }
            }
        });

        TableColumn<OutOrderDetail, String> specCol = new TableColumn<>("规格型号");
        specCol.setPrefWidth(120);
        specCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getGoodsSpec()));

        TableColumn<OutOrderDetail, String> unitCol = new TableColumn<>("单位");
        unitCol.setPrefWidth(80);
        unitCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getUnit()));

        TableColumn<OutOrderDetail, Integer> quantityCol = new TableColumn<>("出库数量");
        quantityCol.setPrefWidth(100);
        quantityCol.setCellValueFactory(new PropertyValueFactory<>("outNum"));
        quantityCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        quantityCol.setOnEditCommit(event -> {
            OutOrderDetail detail = event.getRowValue();
            detail.setOutNum(event.getNewValue());
            updateAmount(detail);
            calculateTotalAmount();
            detailTable.refresh();
        });

        TableColumn<OutOrderDetail, BigDecimal> priceCol = new TableColumn<>("单价");
        priceCol.setPrefWidth(100);
        priceCol.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        priceCol.setCellFactory(TextFieldTableCell.forTableColumn(new javafx.util.converter.BigDecimalStringConverter()));
        priceCol.setOnEditCommit(event -> {
            OutOrderDetail detail = event.getRowValue();
            BigDecimal newPrice = event.getNewValue();
            if (newPrice != null && newPrice.compareTo(BigDecimal.ZERO) >= 0) {
                detail.setUnitPrice(newPrice);
                updateAmount(detail);
                calculateTotalAmount();
            } else {
                MessageDialog.showDialog("单价不能为负数");
                detailTable.refresh();
            }
        });

        TableColumn<OutOrderDetail, BigDecimal> amountCol = new TableColumn<>("金额");
        amountCol.setPrefWidth(120);
        amountCol.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));

        TableColumn<OutOrderDetail, Void> actionCol = new TableColumn<>("操作");
        actionCol.setPrefWidth(80);
        actionCol.setCellFactory(col -> new TableCell<OutOrderDetail, Void>() {
            private final Button deleteBtn = new Button("删除");
            {
                deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-size: 11px;");
                deleteBtn.setOnAction(e -> {
                    OutOrderDetail item = getTableView().getItems().get(getIndex());
                    detailList.remove(item);
                    calculateTotalAmount();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteBtn);
            }
        });

        detailTable.getColumns().addAll(materialCol, specCol, unitCol, quantityCol, priceCol, amountCol, actionCol);

        detailList = FXCollections.observableArrayList();
        detailTable.setItems(detailList);

        centerBox.getChildren().addAll(buttonBox, detailTable);
        root.setCenter(centerBox);

        HBox bottomBox = new HBox(10);
        bottomBox.setPadding(new Insets(10));
        bottomBox.setStyle("-fx-alignment: center-left;");

        totalAmountLabel = new Label("总金额：0.00");
        totalAmountLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2196F3;");

        Button submitBtn = new Button("提交");
        submitBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        submitBtn.setOnAction(e -> onSubmitButtonClick());

        Button cancelBtn = new Button("取消");
        cancelBtn.setOnAction(e -> dialog.close());

        HBox rightButtons = new HBox(10);
        rightButtons.setStyle("-fx-alignment: center-right; -fx-hgrow: always;");
        rightButtons.getChildren().addAll(submitBtn, cancelBtn);

        bottomBox.getChildren().addAll(totalAmountLabel, rightButtons);
        root.setBottom(bottomBox);

        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        dialog.setScene(scene);

        System.out.println("========================================");
        System.out.println("=== 开始初始化出库编辑对话框 ===");
        System.out.println("========================================");
        System.out.println("isNew: " + isNew);
        System.out.println("outOrder: " + outOrder);
        System.out.println("materialList 初始状态: " + materialList);

        CountDownLatch latch = new CountDownLatch(1);
        System.out.println("=== 调用 loadMaterialListWithLatch ===");
        loadMaterialListWithLatch(latch);

        try {
            System.out.println("=== 等待物资列表加载完成（最多15秒）===");
            boolean completed = latch.await(15, TimeUnit.SECONDS);
            if (!completed) {
                System.err.println("⚠️ 物资列表加载超时");
                Platform.runLater(() -> {
                    MessageDialog.showDialog("加载物资列表超时，请检查网络连接");
                });
            } else {
                System.out.println("✅ 等待完成");
                System.out.println("materialList 最终状态: " + materialList);
                System.out.println("materialList 大小: " + (materialList != null ? materialList.size() : "null"));
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.err.println("❌ 等待被中断");
            Platform.runLater(() -> {
                MessageDialog.showDialog("加载物资列表被中断");
            });
        }

        if (!isNew) {
            System.out.println("=== 加载出库单明细 ===");
            loadDetailList();
        }

        System.out.println("=== 显示对话框 ===");
        System.out.println("========================================");
        dialog.showAndWait();
    }

    private void loadMaterialListWithLatch(CountDownLatch latch) {
        new Thread(() -> {
            try {
                System.out.println("=== 开始加载物资列表 ===");
                String url = HttpRequestUtil.serverUrl + "/api/material/list";
                System.out.println("请求URL: " + url);
                System.out.println("Token: " + AppStore.getJwt().getToken());

                // 使用 POST 请求，发送空的 JSON 对象
                String requestBody = "{}";
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .headers("Content-Type", "application/json")
                        .headers("satoken", AppStore.getJwt().getToken())
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                System.out.println("响应状态码: " + response.statusCode());
                System.out.println("响应内容: " + response.body());

                if (response.statusCode() == 200) {
                    Map<String, Object> result = gson.fromJson(response.body(), Map.class);

                    int c = (result.get("code") instanceof Number) ? ((Number) result.get("code")).intValue() : -1;
                    if (c == 200 || c == 0) {
                        Object dataObj = result.get("data");

                        if (dataObj instanceof List) {
                            // 格式1：直接返回 List
                            System.out.println("物资列表加载成功（格式1-直接List）");
                            List<Map<String, Object>> data = (List<Map<String, Object>>) dataObj;
                            materialList = new ArrayList<>();
                            materialMapList = new ArrayList<>();
                            for (Map<String, Object> item : data) {
                                OptionItem option = new OptionItem();
                                option.setId(((Number) item.get("id")).intValue());
                                option.setName((String) item.get("name"));
                                materialList.add(option);
                                materialMapList.add(item);
                            }
                            System.out.println("物资列表加载完成，共 " + materialList.size() + " 条记录");
                        } else if (dataObj instanceof Map) {
                            // 格式2：分页数据
                            System.out.println("物资列表加载成功（格式2-分页数据）");
                            Map<String, Object> dataMap = (Map<String, Object>) dataObj;
                            List<Map<String, Object>> data = (List<Map<String, Object>>) dataMap.get("records");
                            materialList = new ArrayList<>();
                            materialMapList = new ArrayList<>();
                            if (data != null) {
                                for (Map<String, Object> item : data) {
                                    OptionItem option = new OptionItem();
                                    option.setId(((Number) item.get("id")).intValue());
                                    option.setName((String) item.get("name"));
                                    materialList.add(option);
                                    materialMapList.add(item);
                                }
                            }
                            System.out.println("物资列表加载完成，共 " + materialList.size() + " 条记录");
                        } else {
                            System.err.println("物资列表数据格式不正确，dataObj类型: " + (dataObj != null ? dataObj.getClass() : "null"));
                            materialList = new ArrayList<>();
                            materialMapList = new ArrayList<>();
                        }
                    } else {
                        System.err.println("接口返回错误：code=" + result.get("code") + ", msg=" + result.get("msg"));
                        materialList = new ArrayList<>();
                        materialMapList = new ArrayList<>();
                    }
                } else {
                    System.err.println("HTTP请求失败，状态码：" + response.statusCode());
                    materialList = new ArrayList<>();
                    materialMapList = new ArrayList<>();
                }
            } catch (Exception e) {
                System.err.println("加载物资列表异常：" + e.getMessage());
                e.printStackTrace();
                materialList = new ArrayList<>();
                materialMapList = new ArrayList<>();
            } finally {
                System.out.println("=== 释放锁 ===");
                latch.countDown();
            }
        }).start();
    }

    private void loadDetailList() {
        new Thread(() -> {
            try {
                String url = HttpRequestUtil.serverUrl + "/api/stockOut/getDetails/" + outOrder.getId();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .headers("satoken", AppStore.getJwt().getToken())
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    Map<String, Object> result = gson.fromJson(response.body(), Map.class);
                    int c = (result.get("code") instanceof Number) ? ((Number) result.get("code")).intValue() : -1;
                    if (c == 200 || c == 0) {
                        List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");
                        Platform.runLater(() -> {
                            if (data != null) {
                                List<OutOrderDetail> list = gson.fromJson(gson.toJson(data), new TypeToken<List<OutOrderDetail>>(){}.getType());
                                detailList.addAll(list);
                            }
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void showMaterialSelectionDialog(OutOrderDetail detail) {
        if (materialList == null || materialList.isEmpty()) {
            Alert loadingAlert = new Alert(Alert.AlertType.INFORMATION);
            loadingAlert.setTitle("加载中");
            loadingAlert.setHeaderText(null);
            loadingAlert.setContentText("物资列表正在加载中，请稍候再试...");
            loadingAlert.showAndWait();
            return;
        }

        Dialog<OptionItem> selectionDialog = new Dialog<>();
        selectionDialog.setTitle("选择物资");
        selectionDialog.setHeaderText("请选择要出库的物资");

        ListView<OptionItem> listView = new ListView<>();
        listView.getItems().addAll(materialList);
        listView.setCellFactory(lv -> new ListCell<OptionItem>() {
            @Override
            protected void updateItem(OptionItem item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });

        selectionDialog.getDialogPane().setContent(listView);

        ButtonType selectButtonType = new ButtonType("选择", ButtonBar.ButtonData.OK_DONE);
        selectionDialog.getDialogPane().getButtonTypes().addAll(selectButtonType, ButtonType.CANCEL);

        selectionDialog.setResultConverter(dialogButton -> {
            if (dialogButton == selectButtonType) {
                return listView.getSelectionModel().getSelectedItem();
            }
            return null;
        });

        selectionDialog.showAndWait().ifPresent(selectedMaterial -> {
            detail.setGoodsName(selectedMaterial.getName());
            detail.setGoodsId(selectedMaterial.getId());

            if (materialMapList != null) {
                for (Map<String, Object> mat : materialMapList) {
                    if (((Number) mat.get("id")).intValue() == selectedMaterial.getId()) {
                        detail.setGoodsSpec((String) mat.getOrDefault("spec", "默认规格"));
                        detail.setUnit((String) mat.getOrDefault("unit", "件"));
                        Object priceObj = mat.get("price");
                        if (priceObj != null) {
                            try {
                                detail.setUnitPrice(new BigDecimal(priceObj.toString()));
                            } catch (NumberFormatException ex) {
                                detail.setUnitPrice(BigDecimal.ZERO);
                            }
                        } else {
                            detail.setUnitPrice(BigDecimal.ZERO);
                        }
                        updateAmount(detail);
                        calculateTotalAmount();
                        break;
                    }
                }
            }
            detailTable.refresh();
        });
    }

    private void onAddItem() {
        OutOrderDetail detail = new OutOrderDetail();
        detail.setGoodsName("请选择物资");
        detail.setGoodsSpec("默认规格");
        detail.setUnit("件");
        detail.setOutNum(1);
        detail.setUnitPrice(BigDecimal.ZERO);
        detail.setTotalPrice(BigDecimal.ZERO);
        detailList.add(detail);
    }

    private void onDeleteItem() {
        OutOrderDetail selectedItem = detailTable.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            detailList.remove(selectedItem);
            calculateTotalAmount();
        } else {
            MessageDialog.showDialog("请先选择要删除的商品");
        }
    }

    private void updateAmount(OutOrderDetail detail) {
        if (detail.getOutNum() != null && detail.getUnitPrice() != null) {
            detail.setTotalPrice(detail.getUnitPrice().multiply(BigDecimal.valueOf(detail.getOutNum())));
        }
        detailTable.refresh();
    }

    private void calculateTotalAmount() {
        BigDecimal total = BigDecimal.ZERO;
        int totalQty = 0;
        for (OutOrderDetail detail : detailList) {
            if (detail.getTotalPrice() != null) {
                total = total.add(detail.getTotalPrice());
            }
            if (detail.getOutNum() != null) {
                totalQty += detail.getOutNum();
            }
        }
        outOrder.setTotalAmount(total);
        outOrder.setTotalNum(totalQty);
        totalAmountLabel.setText("总金额：" + total);
    }

    @FXML
    protected void onSubmitButtonClick() {
        if (detailList.isEmpty()) {
            MessageDialog.showDialog("请添加商品");
            return;
        }

        for (OutOrderDetail detail : detailList) {
            if (detail.getGoodsId() == null) {
                MessageDialog.showDialog("请选择物资");
                return;
            }
            if (detail.getOutNum() == null || detail.getOutNum() <= 0) {
                MessageDialog.showDialog("出库数量必须大于0");
                return;
            }
        }

        String outType = outTypeComboBox.getValue();
        if (outType == null || outType.isEmpty()) {
            MessageDialog.showDialog("请选择出库类型");
            return;
        }

        try {
            Map<String, Object> requestBody = new HashMap<>();
            if (!isNew) {
                requestBody.put("id", outOrder.getId());
            }
            requestBody.put("outType", getOutTypeValue(outType));

            // 确保备注不为空
            String remark = remarkArea.getText();
            if (remark == null || remark.trim().isEmpty()) {
                remark = "无";
            }
            requestBody.put("remark", remark);

            // 添加申请人ID（从登录信息中获取）
            Integer applicantId = AppStore.getJwt().getId();
            if (applicantId != null) {
                requestBody.put("applyUserId", applicantId);
            }

            // 添加申请人名称（从登录信息中获取）
            String applicantName = AppStore.getJwt().getUsername();
            if (applicantName != null && !applicantName.isEmpty()) {
                requestBody.put("applicantName", applicantName);
            }
            requestBody.put("totalNum", outOrder.getTotalNum());
            requestBody.put("totalAmount", outOrder.getTotalAmount());

            List<Map<String, Object>> items = new ArrayList<>();
            for (OutOrderDetail detail : detailList) {
                if (detail.getGoodsId() == null) {
                    MessageDialog.showDialog("请选择物资");
                    return;
                }
                if (detail.getOutNum() == null || detail.getOutNum() <= 0) {
                    MessageDialog.showDialog("出库数量必须大于0");
                    return;
                }
                Map<String, Object> item = new HashMap<>();
                item.put("materialId", detail.getGoodsId());
                item.put("quantity", detail.getOutNum());
                if (detail.getUnitPrice() != null) {
                    item.put("unitPrice", detail.getUnitPrice());
                }
                items.add(item);
            }
            requestBody.put("items", items);

            String url;
            HttpRequest request;

            if (isNew) {
                // 新增：POST /api/stockOut/submitApply
                url = "/api/stockOut/submitApply";
                request = HttpRequest.newBuilder()
                        .uri(URI.create(HttpRequestUtil.serverUrl + url))
                        .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                        .headers("Content-Type", "application/json")
                        .headers("satoken", AppStore.getJwt().getToken())
                        .build();
            } else {
                // 编辑：PUT /api/stockOut/update/{id}
                url = "/api/stockOut/update/" + outOrder.getId();
                request = HttpRequest.newBuilder()
                        .uri(URI.create(HttpRequestUtil.serverUrl + url))
                        .method("PUT", HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                        .headers("Content-Type", "application/json")
                        .headers("satoken", AppStore.getJwt().getToken())
                        .build();
            }

            System.out.println("========================================");
            System.out.println("=== 开始提交出库单 ===");
            System.out.println("========================================");
            System.out.println("请求方法: " + request.method());
            System.out.println("URL: " + HttpRequestUtil.serverUrl + url);
            System.out.println("Token: " + AppStore.getJwt().getToken());
            System.out.println("请求体: " + gson.toJson(requestBody));

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("响应状态码: " + response.statusCode());
            System.out.println("响应内容: " + response.body());
            System.out.println("========================================");

            if (response.statusCode() == 200) {
                Map<String, Object> result = gson.fromJson(response.body(), Map.class);
                int c = (result.get("code") instanceof Number) ? ((Number) result.get("code")).intValue() : -1;
                if (c == 200 || c == 0) {
                    MessageDialog.showDialog(isNew ? "提交成功" : "更新成功");
                    dialog.close();
                } else {
                    MessageDialog.showDialog("失败：" + result.get("msg"));
                }
            } else {
                System.err.println("HTTP请求失败：");
                System.err.println("状态码: " + response.statusCode());
                System.err.println("响应内容: " + response.body());
                MessageDialog.showDialog("请求失败，状态码：" + response.statusCode() + "\n请查看控制台获取详细信息");
            }
        } catch (Exception e) {
            System.err.println("提交出库单异常！");
            e.printStackTrace();
            MessageDialog.showDialog("异常：" + e.getMessage());
        }
    }

    private boolean checkStockAvailability() throws Exception {
        for (OutOrderDetail detail : detailList) {
            if (detail.getGoodsId() == null) continue;

            // 查询物资当前库存
            String url = HttpRequestUtil.serverUrl + "/api/material/getById/" + detail.getGoodsId();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .headers("satoken", AppStore.getJwt().getToken())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, Object> result = gson.fromJson(response.body(), Map.class);
                int c = (result.get("code") instanceof Number) ? ((Number) result.get("code")).intValue() : -1;
                if (c == 200 || c == 0) {
                    Map<String, Object> data = (Map<String, Object>) result.get("data");
                    Integer currentStock = (Integer) data.getOrDefault("currentStock", 0);
                    String materialName = (String) data.get("materialName");

                    if (currentStock < detail.getOutNum()) {
                        MessageDialog.showDialog("库存不足：\n物资：" + materialName +
                                "\n当前库存：" + currentStock +
                                "\n需要出库：" + detail.getOutNum());
                        return false;
                    }
                }
            }
        }
        return true;
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

    private Integer getOutTypeValue(String outType) {
        switch (outType) {
            case "领料出库": return 1;
            case "销售出库": return 2;
            case "报损出库": return 3;
            case "其他出库": return 4;
            default: return 1;
        }
    }
}
