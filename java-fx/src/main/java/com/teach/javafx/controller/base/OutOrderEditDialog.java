package com.teach.javafx.controller.base;

import com.google.gson.Gson;
import com.teach.javafx.AppStore;
import com.teach.javafx.GsonUtil;
import com.teach.javafx.bean.OutOrder;
import com.teach.javafx.bean.OutOrderDetail;
import com.teach.javafx.request.HttpRequestUtil;
import com.teach.javafx.request.OptionItem;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.converter.IntegerStringConverter;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OutOrderEditDialog extends Stage {

    @FXML
    private Label titleLabel;

    @FXML
    private ComboBox<String> outTypeComboBox;

    @FXML
    private TableView<OutOrderDetail> detailTable;

    @FXML
    private TableColumn<OutOrderDetail, String> materialColumn;

    @FXML
    private TableColumn<OutOrderDetail, Integer> quantityColumn;

    @FXML
    private TableColumn<OutOrderDetail, BigDecimal> priceColumn;

    @FXML
    private TableColumn<OutOrderDetail, BigDecimal> amountColumn;

    @FXML
    private TableColumn<OutOrderDetail, Void> actionColumn;

    @FXML
    private Label totalAmountLabel;

    private final ObservableList<OutOrderDetail> detailList = FXCollections.observableArrayList();
    private final Gson gson = GsonUtil.getGson();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private List<OptionItem> materialList = new ArrayList<>();
    private List<Map<String, Object>> materialMapList = new ArrayList<>();
    private OutOrder editingOutOrder = null;
    private boolean isNew = true;

    /**
     * 静态工厂方法：创建新增对话框
     */
    public static OutOrderEditDialog createNewDialog() {
        return createDialog(null, true);
    }

    /**
     * 静态工厂方法：创建编辑对话框
     */
    public static OutOrderEditDialog createEditDialog(OutOrder order) {
        return createDialog(order, false);
    }

    /**
     * 内部方法：创建对话框
     */
    private static OutOrderEditDialog createDialog(OutOrder order, boolean isNew) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    OutOrderEditDialog.class.getResource("/com/teach/javafx/base/outorder-edit-dialog.fxml"));

            Scene scene = new Scene(loader.load());
            
            scene.getStylesheets().add(OutOrderEditDialog.class.getResource("/styles/modern-style.css").toExternalForm());
            
            OutOrderEditDialog dialog = loader.getController();
            
            if (dialog == null) {
                throw new RuntimeException("无法获取控制器实例");
            }
            
            dialog.setScene(scene);
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle(isNew ? "新增出库单" : "编辑出库单");
            dialog.setResizable(false);
            
            dialog.editingOutOrder = order;
            dialog.isNew = isNew;
            
            // 先同步加载物资列表，确保数据已经加载完成
            System.out.println("=== 开始同步加载物资列表 ===");
            dialog.loadMaterialListSync();
            System.out.println("=== 物资列表加载完成 ===");
            
            dialog.initControls();
            
            if (order != null) {
                dialog.loadOutOrderDetail(order);
            }
            
            return dialog;
            
        } catch (Exception e) {
            System.err.println("=== FXML 加载失败 ===");
            System.err.println("错误类型: " + e.getClass().getName());
            System.err.println("错误消息: " + e.getMessage());
            e.printStackTrace();
            
            StringBuilder errorMsg = new StringBuilder("打开对话框失败：\n");
            errorMsg.append("错误类型: ").append(e.getClass().getSimpleName()).append("\n");
            errorMsg.append("错误消息: ").append(e.getMessage()).append("\n");
            
            if (e.getCause() != null) {
                errorMsg.append("原因: ").append(e.getCause().getMessage()).append("\n");
            }
            
            MessageDialog.showDialog(errorMsg.toString());
            return null;
        }
    }

    private void initControls() {
        if (outTypeComboBox == null) {
            System.err.println("错误：outTypeComboBox 为 null，FXML 绑定失败");
            return;
        }
        
        // 初始化出库类型
        outTypeComboBox.getItems().addAll("领料出库", "销售出库", "报损出库", "其他出库");
        outTypeComboBox.setValue(isNew ? "领料出库" : getOutTypeName(editingOutOrder.getOutType()));
        
        // 添加出库类型切换监听器
        outTypeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                updateFieldEditableState(newVal);
            }
        });
        
        // 初始化表格
        detailTable.setItems(detailList);
        detailTable.setEditable(true);

        // 物资名称列
        materialColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getGoodsName()));
        materialColumn.setCellFactory(col -> {
            return new TableCell<OutOrderDetail, String>() {
                private final TextField textField = new TextField();
                private final Button selectBtn = new Button("选择");
                private final HBox container = new HBox(5, textField, selectBtn);
                
                {
                    textField.setEditable(false);
                    textField.setPromptText("请选择物资");
                    
                    // 修改按钮样式，增加内边距和最小宽度
                    selectBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 6 12 6 12; -fx-min-width: 50; -fx-cursor: hand; -fx-background-radius: 6;");
                    selectBtn.setOnAction(e -> {
                        OutOrderDetail detail = getTableRow() != null ? getTableRow().getItem() : null;
                        if (detail == null) {
                            MessageDialog.showDialog("请先添加物资行");
                            return;
                        }
                        showMaterialSelectionDialog(detail);
                    });
                    
                    HBox.setHgrow(textField, Priority.ALWAYS);
                }
                
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    
                    if (empty || getTableRow().getItem() == null) {
                        setGraphic(null);
                        setText(null);
                    } else {
                        if (item != null && !item.isEmpty()) {
                            textField.setText(item);
                        } else {
                            textField.clear();
                        }
                        setGraphic(container);
                        setText(null);
                    }
                }
            };
        });

        // 数量列
        quantityColumn.setCellValueFactory(param -> new javafx.beans.property.SimpleObjectProperty<>(param.getValue().getOutNum()));
        quantityColumn.setCellFactory(col -> {
            return new TableCell<OutOrderDetail, Integer>() {
                private final TextField textField = new TextField();
                
                {
                    textField.setEditable(true);
                    textField.setPromptText("数量");
                    
                    textField.focusedProperty().addListener((obs, oldVal, newVal) -> {
                        if (!newVal) {
                            OutOrderDetail detail = getTableRow() != null ? getTableRow().getItem() : null;
                            if (detail != null) {
                                try {
                                    String text = textField.getText().trim();
                                    if (!text.isEmpty()) {
                                        Integer newQuantity = Integer.parseInt(text);
                                        if (newQuantity > 0) {
                                            detail.setOutNum(newQuantity);
                                            calculateTotal();
                                            detailTable.refresh();
                                        } else {
                                            MessageDialog.showDialog("数量必须大于0");
                                            detailTable.refresh();
                                        }
                                    }
                                } catch (NumberFormatException e) {
                                    MessageDialog.showDialog("请输入有效的数字");
                                    detailTable.refresh();
                                }
                            }
                        }
                    });
                    
                    textField.setOnAction(e -> {
                        OutOrderDetail detail = getTableRow() != null ? getTableRow().getItem() : null;
                        if (detail != null) {
                            try {
                                String text = textField.getText().trim();
                                if (!text.isEmpty()) {
                                    Integer newQuantity = Integer.parseInt(text);
                                    if (newQuantity > 0) {
                                        detail.setOutNum(newQuantity);
                                        calculateTotal();
                                        detailTable.refresh();
                                    } else {
                                        MessageDialog.showDialog("数量必须大于0");
                                        detailTable.refresh();
                                    }
                                }
                            } catch (NumberFormatException e2) {
                                MessageDialog.showDialog("请输入有效的数字");
                                detailTable.refresh();
                            }
                        }
                    });
                }
                
                @Override
                protected void updateItem(Integer quantity, boolean empty) {
                    super.updateItem(quantity, empty);
                    
                    if (empty || getTableRow().getItem() == null) {
                        setGraphic(null);
                        setText(null);
                    } else {
                        if (quantity != null && quantity > 0) {
                            textField.setText(String.valueOf(quantity));
                        } else {
                            textField.clear();
                        }
                        setGraphic(textField);
                        setText(null);
                    }
                }
            };
        });

        // 单价列
        priceColumn.setCellValueFactory(param -> new javafx.beans.property.SimpleObjectProperty<>(param.getValue().getUnitPrice()));
        priceColumn.setCellFactory(col -> {
            return new TableCell<OutOrderDetail, BigDecimal>() {
                private final TextField textField = new TextField();
                
                {
                    textField.setEditable(true);
                    textField.setPromptText("单价");
                    // 统一输入框样式
                    textField.setStyle("-fx-background-color: white; -fx-text-fill: #2c3e50; -fx-prompt-text-fill: #bdc3c7; -fx-font-size: 13px; -fx-padding: 8 12 8 12; -fx-background-radius: 6; -fx-border-color: #e0e6ed; -fx-border-width: 1.5; -fx-border-radius: 6; -fx-effect: innershadow(gaussian, rgba(0, 0, 0, 0.05), 4, 0, 0, 1);");
                    
                    textField.focusedProperty().addListener((obs, oldVal, newVal) -> {
                        if (!newVal) {
                            OutOrderDetail detail = getTableRow() != null ? getTableRow().getItem() : null;
                            if (detail != null) {
                                try {
                                    String text = textField.getText().trim();
                                    if (!text.isEmpty()) {
                                        BigDecimal newPrice = new BigDecimal(text);
                                        if (newPrice.compareTo(BigDecimal.ZERO) >= 0) {
                                            detail.setUnitPrice(newPrice);
                                            calculateTotal();
                                            detailTable.refresh();
                                        } else {
                                            MessageDialog.showDialog("单价不能为负数");
                                            detailTable.refresh();
                                        }
                                    }
                                } catch (NumberFormatException e) {
                                    MessageDialog.showDialog("请输入有效的数字");
                                    detailTable.refresh();
                                }
                            }
                        }
                    });
                    
                    textField.setOnAction(e -> {
                        OutOrderDetail detail = getTableRow() != null ? getTableRow().getItem() : null;
                        if (detail != null) {
                            try {
                                String text = textField.getText().trim();
                                if (!text.isEmpty()) {
                                    BigDecimal newPrice = new BigDecimal(text);
                                    if (newPrice.compareTo(BigDecimal.ZERO) >= 0) {
                                        detail.setUnitPrice(newPrice);
                                        calculateTotal();
                                        detailTable.refresh();
                                    } else {
                                        MessageDialog.showDialog("单价不能为负数");
                                        detailTable.refresh();
                                    }
                                }
                            } catch (NumberFormatException e2) {
                                MessageDialog.showDialog("请输入有效的数字");
                                detailTable.refresh();
                            }
                        }
                    });
                }
                
                @Override
                protected void updateItem(BigDecimal price, boolean empty) {
                    super.updateItem(price, empty);
                    
                    if (empty || getTableRow().getItem() == null) {
                        setGraphic(null);
                        setText(null);
                    } else {
                        if (price != null && price.compareTo(BigDecimal.ZERO) >= 0) {
                            textField.setText(price.toPlainString());
                        } else {
                            textField.clear();
                        }
                        setGraphic(textField);
                        setText(null);
                    }
                }
            };
        });

        // 金额列（只读）
        amountColumn.setCellValueFactory(param -> {
            OutOrderDetail d = param.getValue();
            BigDecimal amt = BigDecimal.ZERO;
            if (d.getUnitPrice() != null && d.getOutNum() != null) {
                amt = d.getUnitPrice().multiply(BigDecimal.valueOf(d.getOutNum()));
            }
            return new javafx.beans.property.SimpleObjectProperty<>(amt);
        });
        
        amountColumn.setCellFactory(col -> new TableCell<OutOrderDetail, BigDecimal>() {
            private final TextField textField = new TextField();
            
            {
                textField.setEditable(false);
                textField.setFocusTraversable(false);
                // 样式与单价输入框完全一致（包括边框颜色#e0e6ed和粗细1.5）
                textField.setStyle("-fx-background-color: white; -fx-text-fill: #2c3e50; -fx-font-size: 13px; -fx-padding: 8 12 8 12; -fx-background-radius: 6; -fx-border-color: #e0e6ed; -fx-border-width: 1.5; -fx-border-radius: 6; -fx-effect: innershadow(gaussian, rgba(0, 0, 0, 0.05), 4, 0, 0, 1);");
            }
            
            @Override
            protected void updateItem(BigDecimal amount, boolean empty) {
                super.updateItem(amount, empty);
                
                if (empty || getTableRow().getItem() == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
                        textField.setText(String.format("%.2f", amount));
                    } else {
                        // 初始值显示为0
                        textField.setText("0");
                    }
                    setGraphic(textField);
                    setText(null);
                }
            }
        });

        // 操作列
        actionColumn.setCellFactory(col -> new TableCell<OutOrderDetail, Void>() {
            private final Button deleteBtn = new Button("删除");
            {
                deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-size: 11px; -fx-cursor: hand;");
                deleteBtn.setOnAction(e -> {
                    OutOrderDetail detail = getTableView().getItems().get(getIndex());
                    detailList.remove(detail);
                    calculateTotal();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteBtn);
            }
        });

        // 添加一个空行
        if (detailList.isEmpty()) {
            onAddItemButtonClick();
        }
        
        // 初始化字段编辑状态
        updateFieldEditableState(outTypeComboBox.getValue());
        
        calculateTotal();
    }

    /**
     * 根据出库类型更新字段的编辑状态
     * @param outType 出库类型
     */
    private void updateFieldEditableState(String outType) {
        boolean isSales = "销售出库".equals(outType);
        
        // 遍历所有行，更新单价和金额输入框的状态
        for (OutOrderDetail detail : detailList) {
            if (!isSales) {
                // 领料、报损、其他出库：单价和金额全部只读为0
                detail.setUnitPrice(BigDecimal.ZERO);
            }
        }
        
        // 刷新表格以应用更改
        detailTable.refresh();
        
        // 重新计算总金额
        calculateTotal();
    }

    private void loadOutOrderDetail(OutOrder outOrder) {
        outTypeComboBox.setValue(getOutTypeName(outOrder.getOutType()));

        try {
            String url = HttpRequestUtil.serverUrl + "/api/stockOut/detail/" + outOrder.getId();

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
                    Map<String, Object> data = (Map<String, Object>) result.get("data");
                    List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("items");

                    detailList.clear();
                    for (Map<String, Object> itemMap : items) {
                        OutOrderDetail detail = gson.fromJson(gson.toJson(itemMap), OutOrderDetail.class);
                        detailList.add(detail);
                    }
                    calculateTotal();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("加载出库单详情失败：" + e.getMessage());
        }
    }

    /**
     * 显示物资选择对话框
     */
    private void showMaterialSelectionDialog(OutOrderDetail detail) {
        System.out.println("=== 打开物资选择对话框 ===");
        System.out.println("materialList大小: " + materialList.size());
        if (!materialList.isEmpty()) {
            System.out.println("第一个物资: " + materialList.get(0).getName());
        }
        
        if (materialList.isEmpty()) {
            MessageDialog.showDialog("物资列表为空，无法选择。");
            return;
        }

        MaterialSelectionDialog dialog = MaterialSelectionDialog.createDialog(materialList);
        if (dialog == null) {
            return;
        }

        dialog.showAndWait();
        
        OptionItem selectedMaterial = dialog.getSelectedMaterial();
        if (selectedMaterial != null) {
            System.out.println("选中物资: " + selectedMaterial.getName());
            detail.setGoodsName(selectedMaterial.getName());
            detail.setGoodsId(selectedMaterial.getId());

            for (Map<String, Object> mat : materialMapList) {
                if (((Number) mat.get("id")).intValue() == selectedMaterial.getId()) {
                    detail.setGoodsSpec((String) mat.getOrDefault("spec", "默认规格"));
                    detail.setUnit((String) mat.getOrDefault("unit", "件"));
                    Object priceObj = mat.get("price");
                    if (priceObj instanceof Number) {
                        detail.setUnitPrice(BigDecimal.valueOf(((Number) priceObj).doubleValue()));
                    }
                    break;
                }
            }
            detailTable.refresh();
            calculateTotal();
        }
    }

    /**
     * 同步加载物资列表（阻塞式，确保数据加载完成后再继续）
     */
    private void loadMaterialListSync() {
        try {
            System.out.println("开始同步加载物资列表...");
            String url = HttpRequestUtil.serverUrl + "/api/material/list";
            System.out.println("请求URL: " + url);
            
            // 使用 POST 方法，发送空的 JSON 对象（与入库单保持一致）
            Map<String, Object> emptyBody = new HashMap<>();
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(emptyBody)))
                    .headers("Content-Type", "application/json", "satoken", AppStore.getJwt().getToken())
                    .build();
            
            System.out.println("发送HTTP请求...");
            // 使用 send() 方法同步发送请求，会阻塞直到收到响应
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            System.out.println("HTTP状态码: " + response.statusCode());
            
            if (response.statusCode() == 200) {
                Map<String, Object> result = gson.fromJson(response.body(), Map.class);
                int code = (result.get("code") instanceof Number) ? ((Number) result.get("code")).intValue() : -1;
                System.out.println("返回code: " + code);
                
                if (code == 200 || code == 0) {
                    List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");
                    System.out.println("data列表大小: " + (data != null ? data.size() : "null"));
                    
                    materialList.clear();
                    if (data != null) {
                        for (int i = 0; i < data.size() && i < 3; i++) {
                            Map<String, Object> item = data.get(i);
                            System.out.println("物资项 " + i + ": name=" + item.get("name") + ", price=" + item.get("price"));
                        }
                        
                        for (Map<String, Object> item : data) {
                            OptionItem option = new OptionItem();
                            option.setId(((Number) item.get("id")).intValue());
                            
                            // 修复：后端返回的字段是 name，不是 materialName
                            String materialName = (String) item.get("name");
                            option.setName(materialName);
                            
                            // 修复：后端返回的字段是 price，不是 unitPrice
                            if (item.get("price") instanceof Number) {
                                option.setPrice(BigDecimal.valueOf(((Number) item.get("price")).doubleValue()));
                            }
                            materialList.add(option);
                        }
                    }
                    System.out.println("物资列表加载成功，共 " + materialList.size() + " 条数据");
                    if (!materialList.isEmpty()) {
                        System.out.println("第一个物资: id=" + materialList.get(0).getId() + 
                                         ", name=" + materialList.get(0).getName() + 
                                         ", price=" + materialList.get(0).getPrice());
                    }
                } else {
                    System.err.println("加载物资列表失败: code=" + code);
                    System.err.println("错误信息: " + result.get("msg"));
                }
            } else {
                System.err.println("HTTP请求失败: " + response.statusCode());
                System.err.println("响应内容: " + response.body());
            }
        } catch (Exception e) {
            System.err.println("同步加载物资列表异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadMaterialList() {
        try {
            String url = HttpRequestUtil.serverUrl + "/api/material/list";

            // 使用 POST 方法，发送空的 JSON 对象（与入库单保持一致）
            Map<String, Object> emptyBody = new HashMap<>();
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(emptyBody)))
                    .headers("Content-Type", "application/json", "satoken", AppStore.getJwt().getToken())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, Object> result = gson.fromJson(response.body(), Map.class);
                int code = (result.get("code") instanceof Number) ? ((Number) result.get("code")).intValue() : -1;
                if (code == 200 || code == 0) {
                    List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");
                    materialList.clear();
                    for (Map<String, Object> item : data) {
                        OptionItem option = new OptionItem();
                        option.setId(((Number) item.get("id")).intValue());
                        
                        // 修复：后端返回的字段是 name，不是 materialName
                        String materialName = (String) item.get("name");
                        option.setName(materialName);
                        
                        // 修复：后端返回的字段是 price，不是 unitPrice
                        if (item.get("price") instanceof Number) {
                            option.setPrice(BigDecimal.valueOf(((Number) item.get("price")).doubleValue()));
                        }
                        materialList.add(option);
                    }
                    System.out.println("物资列表加载成功，共 " + materialList.size() + " 条数据");
                } else {
                    System.err.println("加载物资列表失败: code=" + code);
                }
            } else {
                System.err.println("HTTP请求失败: " + response.statusCode());
                System.err.println("响应内容: " + response.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    protected void onAddItemButtonClick() {
        OutOrderDetail newItem = new OutOrderDetail();
        newItem.setGoodsName("");
        newItem.setOutNum(1);
        newItem.setUnitPrice(BigDecimal.ZERO);
        detailList.add(newItem);
        calculateTotal();
    }

    private void calculateTotal() {
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OutOrderDetail detail : detailList) {
            if (detail.getOutNum() != null && detail.getUnitPrice() != null) {
                totalAmount = totalAmount.add(detail.getUnitPrice().multiply(BigDecimal.valueOf(detail.getOutNum())));
            }
        }
        totalAmountLabel.setText("总金额：¥" + String.format("%.2f", totalAmount));
    }

    @FXML
    protected void onSubmitButtonClick() {
        if (detailList.isEmpty()) {
            MessageDialog.showDialog("请添加商品");
            return;
        }

        String outType = outTypeComboBox.getValue();
        if (outType == null || outType.isEmpty()) {
            MessageDialog.showDialog("请选择出库类型");
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

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("outType", getOutTypeValue(outType));
            
            String remark = editingOutOrder != null ? editingOutOrder.getRemark() : "";
            if (remark == null || remark.trim().isEmpty()) {
                remark = "无";
            }
            requestBody.put("remark", remark);

            BigDecimal totalAmount = BigDecimal.ZERO;
            int totalNum = 0;
            for (OutOrderDetail detail : detailList) {
                if (detail.getOutNum() != null) {
                    totalNum += detail.getOutNum();
                    if (detail.getUnitPrice() != null) {
                        totalAmount = totalAmount.add(detail.getUnitPrice().multiply(BigDecimal.valueOf(detail.getOutNum())));
                    }
                }
            }
            requestBody.put("totalNum", totalNum);
            requestBody.put("totalAmount", totalAmount);

            Integer userId = AppStore.getJwt().getId();
            String userName = AppStore.getJwt().getUsername();
            if (userId != null) requestBody.put("applicantId", userId);
            if (userName != null) requestBody.put("applicantName", userName);

            List<Map<String, Object>> items = new ArrayList<>();
            for (OutOrderDetail detail : detailList) {
                Map<String, Object> item = new HashMap<>();
                item.put("materialId", detail.getGoodsId());
                item.put("quantity", detail.getOutNum());
                if (detail.getUnitPrice() != null) item.put("unitPrice", detail.getUnitPrice());
                if (detail.getId() != null) item.put("id", detail.getId());
                items.add(item);
            }
            requestBody.put("items", items);

            String url = isNew ? "/api/stockOut/submitApply" : "/api/stockOut/update";
            
            // 打印详细的请求信息
            System.out.println("\n=== [出库单" + (isNew ? "新增" : "编辑") + "] 提交数据 ===");
            System.out.println("请求URL: " + HttpRequestUtil.serverUrl + url);
            System.out.println("Token: " + AppStore.getJwt().getToken());
            System.out.println("请求体: " + gson.toJson(requestBody));

            HttpRequest request;
            if (isNew) {
                // 新增使用 POST
                request = HttpRequest.newBuilder()
                        .uri(URI.create(HttpRequestUtil.serverUrl + url))
                        .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                        .headers("Content-Type", "application/json")
                        .headers("satoken", AppStore.getJwt().getToken())
                        .build();
            } else {
                // 更新使用 PUT
                request = HttpRequest.newBuilder()
                        .uri(URI.create(HttpRequestUtil.serverUrl + url))
                        .PUT(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                        .headers("Content-Type", "application/json")
                        .headers("satoken", AppStore.getJwt().getToken())
                        .build();
            }

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("响应状态码: " + response.statusCode());
            System.out.println("响应内容: " + response.body());

            if (response.statusCode() == 200) {
                Map<String, Object> result = gson.fromJson(response.body(), Map.class);
                int code = (result.get("code") instanceof Number) ? ((Number) result.get("code")).intValue() : -1;
                if (code == 200 || code == 0) {
                    MessageDialog.showDialog(isNew ? "提交成功" : "更新成功");
                    this.close();
                } else {
                    String errorMsg = result.get("msg") != null ? result.get("msg").toString() : "未知错误";
                    System.err.println("提交失败，错误信息: " + errorMsg);
                    MessageDialog.showDialog("失败：" + errorMsg);
                }
            } else {
                System.err.println("HTTP请求失败，状态码: " + response.statusCode());
                System.err.println("响应内容: " + response.body());
                MessageDialog.showDialog("请求失败！状态码: " + response.statusCode() + "\n响应: " + response.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("提交异常: " + e.getMessage());
            MessageDialog.showDialog("异常：" + e.getMessage());
        }
    }

    @FXML
    protected void onCancelButtonClick() {
        this.close();
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