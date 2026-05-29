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
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
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

            // 加载 FXML，会自动创建控制器实例
            Scene scene = new Scene(loader.load());
            
            // 获取 FXML 自动创建的控制器实例
            StockInEditDialog dialog = loader.getController();
            
            if (dialog == null) {
                throw new RuntimeException("无法获取控制器实例");
            }
            
            System.out.println("=== FXML 加载成功 ===");
            System.out.println("控制器实例: " + dialog);
            System.out.println("typeComboBox: " + dialog.typeComboBox);
            
            // 设置对话框属性
            dialog.setScene(scene);
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle(stockIn == null ? "新增入库单" : "编辑入库单");
            dialog.setResizable(false);
            
            // 设置数据
            dialog.editingStockIn = stockIn;
            
            // 初始化控件
            dialog.initControls();
            
            // 如果是编辑模式，加载详情
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

        // 初始化表格
        itemTable.setItems(itemList);
        itemTable.setEditable(true);

        // 物资列 - 使用按钮选择
        materialColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("materialName"));
        materialColumn.setCellFactory(col -> new TableCell<StockInItem, String>() {
            private final Button selectBtn = new Button("选择物资");
            {
                selectBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 11px;");
                selectBtn.setOnAction(e -> {
                    StockInItem item = getTableView().getItems().get(getIndex());
                    showMaterialSelectionDialog(item);
                });
            }

            @Override
            protected void updateItem(String materialName, boolean empty) {
                super.updateItem(materialName, empty);
                if (empty || materialName == null) {
                    setText("未选择");
                    setGraphic(selectBtn);
                } else {
                    setText(materialName);
                    setGraphic(selectBtn);
                }
            }
        });

        // 数量列
        quantityColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("quantity"));
        quantityColumn.setCellFactory(TextFieldTableCell.forTableColumn(
                new IntegerStringConverter()
        ));
        quantityColumn.setOnEditCommit(event -> {
            StockInItem item = event.getRowValue();
            Integer newQuantity = event.getNewValue();
            if (newQuantity != null && newQuantity > 0) {
                item.setQuantity(newQuantity);
                updateAmount(item);
                calculateTotal();
            } else {
                MessageDialog.showDialog("数量必须大于0");
                itemTable.refresh();
            }
        });

        // 单价列
        priceColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("price"));
        priceColumn.setCellFactory(TextFieldTableCell.forTableColumn(new StringConverter<BigDecimal>() {
            @Override
            public String toString(BigDecimal object) {
                return object == null ? "" : object.toPlainString();
            }

            @Override
            public BigDecimal fromString(String string) {
                if (string == null || string.isEmpty()) return BigDecimal.ZERO;
                try {
                    return new BigDecimal(string);
                } catch (NumberFormatException e) {
                    return BigDecimal.ZERO;
                }
            }
        }));

        priceColumn.setOnEditCommit(event -> {
            StockInItem item = event.getRowValue();
            BigDecimal newPrice = event.getNewValue();
            if (newPrice != null && newPrice.compareTo(BigDecimal.ZERO) >= 0) {
                item.setPrice(newPrice);
                updateAmount(item);
                calculateTotal();
            } else {
                MessageDialog.showDialog("单价不能为负数");
                itemTable.refresh();
            }
        });

        // 金额列（只读）
        amountColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("amount"));

        // 操作列 - 删除按钮
        actionColumn.setCellFactory(col -> new TableCell<StockInItem, Void>() {
            private final Button deleteBtn = new Button("删除");
            {
                deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-size: 11px;");
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
     * 显示物资选择对话框
     */
    private void showMaterialSelectionDialog(StockInItem item) {
        if (materialList.isEmpty()) {
            MessageDialog.showDialog("物资列表为空，请先添加物资");
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
            System.out.println("开始加载物资列表...");
            String url = HttpRequestUtil.serverUrl + "/material/list";
            System.out.println("请求URL: " + url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .headers("satoken", AppStore.getJwt().getToken())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("物资列表响应状态码: " + response.statusCode());
            if (response.body() != null) {
                System.out.println("物资列表响应内容长度: " + response.body().length());
            }

            if (response.statusCode() == 200) {
                Map<String, Object> result = gson.fromJson(response.body(), Map.class);
                if (result.get("code").equals(200.0)) {
                    List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");
                    materialList.clear();
                    for (Map<String, Object> item : data) {
                        OptionItem option = new OptionItem();
                        option.setId(((Number) item.get("id")).intValue());
                        option.setName((String) item.get("name"));
                        materialList.add(option);
                    }
                    updateMaterialComboBox();
                    System.out.println("成功加载 " + materialList.size() + " 个物资");
                } else {
                    System.err.println("物资列表业务错误: " + result.get("msg"));
                }
            } else {
                System.err.println("物资列表HTTP错误: " + response.statusCode());
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
        itemTable.refresh();
    }

    private void calculateTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (StockInItem item : itemList) {
            if (item.getAmount() != null) {
                total = total.add(item.getAmount());
            }
        }
        totalAmountLabel.setText("总金额：" + total);
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

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("type", getTypeValue(typeComboBox.getValue()));

            List<Map<String, Object>> items = new ArrayList<>();
            for (StockInItem item : itemList) {
                if (item.getMaterialId() == null) {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
                    alert.setTitle("警告");
                    alert.setHeaderText(null);
                    alert.setContentText("物资不能为空");
                    alert.showAndWait();
                    return;
                }
                if (item.getQuantity() <= 0) {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
                    alert.setTitle("警告");
                    alert.setHeaderText(null);
                    alert.setContentText("物资数量必须大于0");
                    alert.showAndWait();
                    return;
                }
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("materialId", item.getMaterialId());
                itemMap.put("quantity", item.getQuantity());
                itemMap.put("price", item.getPrice());
                if (item.getId() != null) {
                    itemMap.put("id", item.getId());
                }
                items.add(itemMap);
            }
            requestBody.put("items", items);

            String url;
            HttpRequest request;
            
            if (editingStockIn == null) {
                // 新增：POST /stock-in/create
                url = HttpRequestUtil.serverUrl + "/stock-in/create";
                request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                        .headers("Content-Type", "application/json", "satoken", AppStore.getJwt().getToken())
                        .build();
            } else {
                // 编辑：PUT /stock-in/update/{id}
                url = HttpRequestUtil.serverUrl + "/stock-in/update/" + editingStockIn.getId();
                request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .method("PUT", HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                        .headers("Content-Type", "application/json", "satoken", AppStore.getJwt().getToken())
                        .build();
            }

            System.out.println("提交请求: " + request.method() + " " + url);
            System.out.println("请求体: " + gson.toJson(requestBody));

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("响应状态码: " + response.statusCode());
            System.out.println("响应内容: " + response.body());

            if (response.statusCode() == 200) {
                Map<String, Object> result = gson.fromJson(response.body(), Map.class);
                if (result.get("code").equals(200.0)) {
                    String message = editingStockIn == null ? "入库单提交成功" : "入库单更新成功";
                    
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                    alert.setTitle("成功");
                    alert.setHeaderText(null);
                    alert.setContentText(message);
                    alert.showAndWait();
                    
                    this.close();
                } else {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                    alert.setTitle("错误");
                    alert.setHeaderText(null);
                    alert.setContentText("提交失败：" + result.get("msg"));
                    alert.showAndWait();
                }
            } else {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                alert.setTitle("错误");
                alert.setHeaderText(null);
                alert.setContentText("提交失败：" + response.body());
                alert.showAndWait();
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
