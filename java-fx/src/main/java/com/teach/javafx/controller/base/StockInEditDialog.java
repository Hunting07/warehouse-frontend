package com.teach.javafx.controller.base;

import com.google.gson.Gson;
import com.teach.javafx.AppStore;
import com.teach.javafx.GsonUtil;
import com.teach.javafx.models.StockIn;
import com.teach.javafx.models.StockInItem;
import com.teach.javafx.request.OptionItem;
import com.teach.javafx.request.HttpRequestUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;
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

public class StockInEditDialog extends Stage {

    @FXML
    private ComboBox<String> typeComboBox;

    @FXML
    private TableView<StockInItem> itemTable;

    @FXML
    private TableColumn<StockInItem, String> materialColumn;

    @FXML
    private TableColumn<StockInItem, Integer> quantityColumn;

    @FXML
    private TableColumn<StockInItem, BigDecimal> priceColumn;

    @FXML
    private TableColumn<StockInItem, BigDecimal> amountColumn;

    @FXML
    private TableColumn<StockInItem, Void> actionColumn;

    @FXML
    private Label totalAmountLabel;

    private final ObservableList<StockInItem> itemList = FXCollections.observableArrayList();
    private final Gson gson = GsonUtil.getGson();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private List<OptionItem> materialList = new ArrayList<>();
    private StockIn editingStockIn = null;

    /**
     * 静态工厂方法：创建新增对话框
     */
    public static StockInEditDialog createNewDialog() {
        return createDialog(null);
    }

    /**
     * 静态工厂方法：创建编辑对话框
     */
    public static StockInEditDialog createEditDialog(StockIn stockIn) {
        return createDialog(stockIn);
    }

    /**
     * 内部方法：创建对话框
     */
    private static StockInEditDialog createDialog(StockIn stockIn) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    StockInEditDialog.class.getResource("/com/teach/javafx/base/stockin-edit-dialog.fxml"));

            Scene scene = new Scene(loader.load());
            
            scene.getStylesheets().add(StockInEditDialog.class.getResource("/styles/modern-style.css").toExternalForm());
            
            StockInEditDialog dialog = loader.getController();
            
            if (dialog == null) {
                throw new RuntimeException("无法获取控制器实例");
            }
            
            System.out.println("=== FXML 加载成功 ===");
            System.out.println("控制器实例: " + dialog);
            System.out.println("typeComboBox: " + dialog.typeComboBox);
            
            dialog.setScene(scene);
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle(stockIn == null ? "新增入库单" : "编辑入库单");
            dialog.setResizable(false);
            
            dialog.editingStockIn = stockIn;
            
            dialog.initControls();
            
            if (stockIn != null) {
                dialog.loadStockInDetail(stockIn);
            }
            
            System.out.println("=== 对话框初始化完成 ===");
            
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
        if (typeComboBox == null) {
            System.err.println("错误：typeComboBox 为 null，FXML 绑定失败");
            return;
        }
        
        System.out.println("开始初始化控件...");
        
        // 初始化入库类型
        typeComboBox.getItems().addAll("采购入库", "退货入库", "其他入库");
        typeComboBox.setValue("采购入库");
        
        // 监听入库类型变化
        typeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean isReturn = "退货入库".equals(newVal);
            updateTableCellsEditable(isReturn);
        });

        // 初始化表格
        itemTable.setItems(itemList);
        itemTable.setEditable(true);

        // 物资名称列 - 支持直接输入和按钮选择
        materialColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("materialName"));
        materialColumn.setCellFactory(col -> {
            return new TableCell<StockInItem, String>() {
                private final TextField textField = new TextField();
                private final Button selectBtn = new Button("选择");
                private final HBox container = new HBox(5, textField, selectBtn);
                
                {
                    textField.setEditable(true);
                    textField.setPromptText("输入物资名称");
                    
                    // 监听文本变化
                    textField.textProperty().addListener((obs, oldVal, newVal) -> {
                        StockInItem item = getTableRow() != null ? getTableRow().getItem() : null;
                        if (item != null) {
                            item.setMaterialName(newVal);
                            // 手动输入时清空 materialId
                            item.setMaterialId(null);
                        }
                    });
                    
                    // 选择按钮点击事件
                    selectBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 11px; -fx-cursor: hand;");
                    selectBtn.setOnAction(e -> {
                        StockInItem item = getTableRow() != null ? getTableRow().getItem() : null;
                        if (item == null) {
                            MessageDialog.showDialog("请先添加物资行");
                            return;
                        }
                        showMaterialSelectionDialog(item);
                    });
                    
                    HBox.setHgrow(textField, Priority.ALWAYS);
                }
                
                @Override
                protected void updateItem(String materialName, boolean empty) {
                    super.updateItem(materialName, empty);
                    
                    if (empty || getTableRow().getItem() == null) {
                        setGraphic(null);
                        setText(null);
                    } else {
                        // 始终显示控件，即使 materialName 为空
                        if (materialName != null) {
                            textField.setText(materialName);
                        } else {
                            textField.clear();
                        }
                        
                        // 根据入库类型控制是否可编辑
                        boolean isReturn = "退货入库".equals(typeComboBox.getValue());
                        textField.setEditable(!isReturn);
                        textField.setDisable(isReturn);
                        
                        setGraphic(container);
                        setText(null);
                    }
                }
            };
        });

        // 数量列 - 可编辑（使用 TextField）
        quantityColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("quantity"));
        quantityColumn.setCellFactory(col -> {
            return new TableCell<StockInItem, Integer>() {
                private final TextField textField = new TextField();
                
                {
                    textField.setEditable(true);
                    textField.setPromptText("数量");
                    
                    // 失去焦点时保存
                    textField.focusedProperty().addListener((obs, oldVal, newVal) -> {
                        if (!newVal) { // 失去焦点
                            StockInItem item = getTableRow() != null ? getTableRow().getItem() : null;
                            if (item != null) {
                                try {
                                    String text = textField.getText().trim();
                                    if (!text.isEmpty()) {
                                        Integer newQuantity = Integer.parseInt(text);
                                        if (newQuantity > 0) {
                                            item.setQuantity(newQuantity);
                                            updateAmount(item);
                                            calculateTotal();
                                        } else {
                                            MessageDialog.showDialog("数量必须大于0");
                                            itemTable.refresh();
                                        }
                                    }
                                } catch (NumberFormatException e) {
                                    MessageDialog.showDialog("请输入有效的数字");
                                    itemTable.refresh();
                                }
                            }
                        }
                    });
                    
                    // 按回车键也保存
                    textField.setOnAction(e -> {
                        StockInItem item = getTableRow() != null ? getTableRow().getItem() : null;
                        if (item != null) {
                            try {
                                String text = textField.getText().trim();
                                if (!text.isEmpty()) {
                                    Integer newQuantity = Integer.parseInt(text);
                                    if (newQuantity > 0) {
                                        item.setQuantity(newQuantity);
                                        updateAmount(item);
                                        calculateTotal();
                                    } else {
                                        MessageDialog.showDialog("数量必须大于0");
                                        itemTable.refresh();
                                    }
                                }
                            } catch (NumberFormatException e2) {
                                MessageDialog.showDialog("请输入有效的数字");
                                itemTable.refresh();
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

        // 单价列 - 可编辑（使用 TextField）
        priceColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("price"));
        priceColumn.setCellFactory(col -> {
            return new TableCell<StockInItem, BigDecimal>() {
                private final TextField textField = new TextField();
                
                {
                    textField.setEditable(true);
                    textField.setPromptText("单价");
                    
                    // 失去焦点时保存
                    textField.focusedProperty().addListener((obs, oldVal, newVal) -> {
                        if (!newVal) { // 失去焦点
                            StockInItem item = getTableRow() != null ? getTableRow().getItem() : null;
                            if (item != null) {
                                try {
                                    String text = textField.getText().trim();
                                    if (!text.isEmpty()) {
                                        BigDecimal newPrice = new BigDecimal(text);
                                        if (newPrice.compareTo(BigDecimal.ZERO) >= 0) {
                                            item.setPrice(newPrice);
                                            updateAmount(item);
                                            calculateTotal();
                                        } else {
                                            MessageDialog.showDialog("单价不能为负数");
                                            itemTable.refresh();
                                        }
                                    }
                                } catch (NumberFormatException e) {
                                    MessageDialog.showDialog("请输入有效的数字");
                                    itemTable.refresh();
                                }
                            }
                        }
                    });
                    
                    // 按回车键也保存
                    textField.setOnAction(e -> {
                        StockInItem item = getTableRow() != null ? getTableRow().getItem() : null;
                        if (item != null) {
                            try {
                                String text = textField.getText().trim();
                                if (!text.isEmpty()) {
                                    BigDecimal newPrice = new BigDecimal(text);
                                    if (newPrice.compareTo(BigDecimal.ZERO) >= 0) {
                                        item.setPrice(newPrice);
                                        updateAmount(item);
                                        calculateTotal();
                                    } else {
                                        MessageDialog.showDialog("单价不能为负数");
                                        itemTable.refresh();
                                    }
                                }
                            } catch (NumberFormatException e2) {
                                MessageDialog.showDialog("请输入有效的数字");
                                itemTable.refresh();
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
                        if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
                            textField.setText(price.toPlainString());
                        } else {
                            textField.clear();
                        }
                        
                        // 根据入库类型控制是否可编辑
                        boolean isReturn = "退货入库".equals(typeComboBox.getValue());
                        textField.setEditable(!isReturn);
                        textField.setDisable(isReturn);
                        
                        setGraphic(textField);
                        setText(null);
                    }
                }
            };
        });

        // 金额列（只读）
        amountColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("amount"));

        // 操作列 - 只保留删除按钮
        actionColumn.setCellFactory(col -> new TableCell<StockInItem, Void>() {
            private final Button deleteBtn = new Button("删除");
            {
                deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-size: 11px; -fx-cursor: hand;");
                deleteBtn.setOnAction(e -> {
                    StockInItem item = getTableView().getItems().get(getIndex());
                    itemList.remove(item);
                    calculateTotal();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteBtn);
            }
        });

        System.out.println("控件初始化完成，开始加载物资列表...");
        
        // 先添加一个空行，让用户可以开始编辑
        if (itemList.isEmpty()) {
            onAddItemButtonClick();
        }
        
        // 加载物资列表
        loadMaterialList();
    }
    
    /**
     * 更新表格单元格的可编辑状态
     */
    private void updateTableCellsEditable(boolean isReturn) {
        // 刷新表格以应用新的编辑状态
        itemTable.refresh();
        
    }

    /**
     * 显示物资选择对话框
     */
    private void showMaterialSelectionDialog(StockInItem item) {
        if (materialList.isEmpty()) {
            MessageDialog.showDialog("物资列表为空，无法选择。您可以直接输入物资名称。");
            return;
        }

        // 创建选择对话框
        Dialog<OptionItem> dialog = new Dialog<>();
        dialog.setTitle("选择物资");
        dialog.setHeaderText("请选择要入库的物资");

        // 创建物资列表
        ListView<OptionItem> listView = new ListView<>();
        listView.getItems().addAll(materialList);
        listView.setCellFactory(lv -> new ListCell<OptionItem>() {
            @Override
            protected void updateItem(OptionItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });

        dialog.getDialogPane().setContent(listView);

        // 添加按钮
        ButtonType selectButtonType = new ButtonType("选择", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(selectButtonType, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == selectButtonType) {
                return listView.getSelectionModel().getSelectedItem();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(selectedMaterial -> {
            // 用户选择了物资
            item.setMaterialId(selectedMaterial.getId());
            item.setMaterialName(selectedMaterial.getName());
            
            // 如果是退货入库，直接使用已加载的单价
            boolean isReturn = "退货入库".equals(typeComboBox.getValue());
            if (isReturn && selectedMaterial.getPrice() != null) {
                item.setPrice(selectedMaterial.getPrice());
                updateAmount(item);
                calculateTotal();
                System.out.println("已自动填充单价: " + selectedMaterial.getPrice());
            }
            
            System.out.println("已选择物资: " + selectedMaterial.getName() + " (ID: " + selectedMaterial.getId() + ")");
            
            // 刷新表格
            itemTable.refresh();
        });
    }

    private void loadStockInDetail(StockIn stockIn) {
        typeComboBox.setValue(getTypeName(stockIn.getType()));

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
                if (result.get("code").equals(200.0)) {
                    Map<String, Object> data = (Map<String, Object>) result.get("data");
                    List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("items");

                    itemList.clear();
                    for (Map<String, Object> itemMap : items) {
                        StockInItem item = gson.fromJson(gson.toJson(itemMap), StockInItem.class);
                        itemList.add(item);
                    }
                    calculateTotal();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("加载入库单详情失败：" + e.getMessage());
        }
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

    private void loadMaterialList() {
        try {
            String url = HttpRequestUtil.serverUrl + "/api/material/list";

            // 使用 POST 方法，发送空的 JSON 对象
            Map<String, Object> emptyBody = new HashMap<>();
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(emptyBody)))
                    .headers("Content-Type", "application/json", "satoken", AppStore.getJwt().getToken())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, Object> result = gson.fromJson(response.body(), Map.class);
                
                if (result.get("code").equals(200.0)) {
                    List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");
                    materialList.clear();
                    for (Map<String, Object> item : data) {
                        OptionItem option = new OptionItem();
                        option.setId(((Number) item.get("id")).intValue());
                        option.setName((String) item.get("name"));
                        
                        // 加载单价字段（兼容 price 和 unitPrice）
                        Object priceObj = item.get("price");
                        if (priceObj == null) {
                            priceObj = item.get("unitPrice");
                        }
                        if (priceObj != null) {
                            option.setPrice(new BigDecimal(priceObj.toString()));
                        }
                        
                        materialList.add(option);
                    }
                    updateMaterialComboBox();
                    
                    // 刷新表格，让 ComboBox 显示最新的物资列表
                    itemTable.refresh();
                } else {
                    System.err.println("物资列表业务错误: " + result.get("msg"));
                }
            } else {
                System.err.println("物资列表HTTP错误: " + response.statusCode());
                System.err.println("错误响应内容: " + response.body());
            }
        } catch (Exception e) {
            System.err.println("加载物资列表异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateMaterialComboBox() {
        System.out.println("物资列表已加载，共 " + materialList.size() + " 个物资");
        for (OptionItem item : materialList) {
            System.out.println("  - " + item.getName() + " (ID: " + item.getId() + ")");
        }
    }

    @FXML
    protected void onAddItemButtonClick() {
        StockInItem newItem = new StockInItem();
        newItem.setMaterialName("");  // 设置为空字符串而不是 null
        newItem.setQuantity(1);  // 默认数量为1
        newItem.setPrice(BigDecimal.ZERO);
        newItem.setAmount(BigDecimal.ZERO);
        itemList.add(newItem);
        
        System.out.println("添加了新物资行，当前共 " + itemList.size() + " 行");
    }

    private void updateAmount(StockInItem item) {
        if (item.getQuantity() != null && item.getPrice() != null) {
            item.setAmount(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
    }

    private void calculateTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (StockInItem item : itemList) {
            if (item.getAmount() != null) {
                total = total.add(item.getAmount());
            }
        }
        totalAmountLabel.setText("总金额：¥" + total);
    }

    @FXML
    protected void onSubmitButtonClick() {
        if (itemList.isEmpty()) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
            alert.setTitle("警告");
            alert.setHeaderText(null);
            alert.setContentText("请至少添加一条明细");
            alert.showAndWait();
            return;
        }

        // 验证所有物资
        for (StockInItem item : itemList) {
            if (item.getMaterialName() == null || item.getMaterialName().trim().isEmpty()) {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
                alert.setTitle("警告");
                alert.setHeaderText(null);
                alert.setContentText("请填写物资名称");
                alert.showAndWait();
                return;
            }
            
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
                alert.setTitle("警告");
                alert.setHeaderText(null);
                alert.setContentText("物资数量必须大于0");
                alert.showAndWait();
                return;
            }
        }

        try {
            int successCount = 0;
            int failCount = 0;
            StringBuilder errorMsg = new StringBuilder();
            
            // 遍历每个物资，创建独立的入库单
            for (StockInItem item : itemList) {
                try {
                    // 计算金额
                    BigDecimal price = item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO;
                    BigDecimal amount = price.multiply(BigDecimal.valueOf(item.getQuantity()));
                    
                    // 构建单个物资的请求体
                    Map<String, Object> requestBody = new HashMap<>();
                    requestBody.put("type", getTypeValue(typeComboBox.getValue()));
                    
                    List<Map<String, Object>> items = new ArrayList<>();
                    Map<String, Object> itemMap = new HashMap<>();
                    
                    // 如果选择了物资（有materialId），则提交materialId
                    if (item.getMaterialId() != null) {
                        itemMap.put("materialId", item.getMaterialId());
                    }
                    
                    // 始终提交物资名称
                    itemMap.put("materialName", item.getMaterialName());
                    itemMap.put("quantity", item.getQuantity());
                    itemMap.put("price", price);
                    itemMap.put("amount", amount);
                    
                    // 如果有其他字段，也一并提交
                    if (item.getMaterialCode() != null && !item.getMaterialCode().isEmpty()) {
                        itemMap.put("materialCode", item.getMaterialCode());
                    }
                    if (item.getMaterialSpec() != null && !item.getMaterialSpec().isEmpty()) {
                        itemMap.put("materialSpec", item.getMaterialSpec());
                    }
                    if (item.getId() != null) {
                        itemMap.put("id", item.getId());
                    }
                    
                    items.add(itemMap);
                    requestBody.put("items", items);
                    
                    String url = HttpRequestUtil.serverUrl + "/stock-in/create";
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                            .headers("Content-Type", "application/json", "satoken", AppStore.getJwt().getToken())
                            .build();
                    
                    System.out.println("提交入库单 " + (successCount + 1) + ": " + item.getMaterialName());
                    
                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    
                    if (response.statusCode() == 200) {
                        Map<String, Object> result = gson.fromJson(response.body(), Map.class);
                        if (result.get("code").equals(200.0)) {
                            successCount++;
                            System.out.println("入库单 " + (successCount) + " 提交成功");
                        } else {
                            failCount++;
                            errorMsg.append("物资「").append(item.getMaterialName()).append("」提交失败: ")
                                    .append(result.get("msg")).append("\n");
                            System.err.println("入库单提交失败: " + result.get("msg"));
                        }
                    } else {
                        failCount++;
                        errorMsg.append("物资「").append(item.getMaterialName()).append("」HTTP错误: ")
                                .append(response.statusCode()).append("\n");
                        System.err.println("HTTP错误: " + response.statusCode());
                    }
                    
                    // 稍微延迟，避免请求过快
                    Thread.sleep(100);
                    
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    failCount++;
                    errorMsg.append("物资「").append(item.getMaterialName()).append("」被中断\n");
                } catch (Exception e) {
                    e.printStackTrace();
                    failCount++;
                    errorMsg.append("物资「").append(item.getMaterialName()).append("」异常: ")
                            .append(e.getMessage()).append("\n");
                }
            }
            
            // 显示提交结果
            StringBuilder resultMsg = new StringBuilder();
            resultMsg.append("提交完成！\n");
            resultMsg.append("成功: ").append(successCount).append(" 个\n");
            resultMsg.append("失败: ").append(failCount).append(" 个");
            
            if (errorMsg.length() > 0) {
                resultMsg.append("\n\n失败详情:\n").append(errorMsg.toString());
            }
            
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                failCount == 0 ? javafx.scene.control.Alert.AlertType.INFORMATION : javafx.scene.control.Alert.AlertType.WARNING);
            alert.setTitle("提交结果");
            alert.setHeaderText(null);
            alert.setContentText(resultMsg.toString());
            alert.showAndWait();
            
            // 如果全部成功，关闭对话框
            if (failCount == 0) {
                this.close();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText(null);
            alert.setContentText("提交异常：" + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    protected void onCancelButtonClick() {
        this.close();
    }

    private Integer getTypeValue(String type) {
        switch (type) {
            case "采购入库": return 1;
            case "退货入库": return 2;
            case "其他入库": return 3;
            default: return 1;
        }
    }
}
