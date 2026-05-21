package com.teach.javafx.controller.base;

import com.google.gson.Gson;
import com.teach.javafx.AppStore;
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
    private final Gson gson = new Gson();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private List<OptionItem> materialList = new ArrayList<>();

    public StockInEditDialog() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/teach/javafx/base/stockin-edit-dialog.fxml"));
            loader.setController(this);

            Scene scene = new Scene(loader.load());
            this.setScene(scene);
            this.initModality(Modality.APPLICATION_MODAL);
            this.setTitle("新增入库单");
            this.setResizable(false);

            initControls();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initControls() {
        typeComboBox.getItems().addAll("采购入库", "退货入库", "其他");
        typeComboBox.setValue("采购入库");

        itemTable.setItems(itemList);
        itemTable.setEditable(true);

        materialColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("materialName"));
        materialColumn.setCellFactory(ComboBoxTableCell.forTableColumn(
                FXCollections.observableArrayList()
        ));

        quantityColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("quantity"));
        quantityColumn.setCellFactory(TextFieldTableCell.forTableColumn(
                new IntegerStringConverter()
        ));
        quantityColumn.setOnEditCommit(event -> {
            StockInItem item = event.getRowValue();
            item.setQuantity(event.getNewValue());
            updateAmount(item);
            calculateTotal();
        });

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
            item.setPrice(event.getNewValue());
            updateAmount(item);
            calculateTotal();
        });

        amountColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("amount"));

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

        loadMaterialList();
    }

    private void loadMaterialList() {
        try {
            String url = HttpRequestUtil.serverUrl + "/material/list";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .headers("satoken", AppStore.getJwt().getToken())
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
                        option.setName((String) item.get("name")); // 现在 OptionItem 有 setName 了
                        materialList.add(option);
                    }
                    updateMaterialComboBox();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("加载物资列表失败：" + e.getMessage());
        }
    }

    private void updateMaterialComboBox() {
        List<String> names = new ArrayList<>();
        for (OptionItem item : materialList) {
            names.add(item.getName()); // 现在用 getName()
        }
        materialColumn.setCellFactory(ComboBoxTableCell.forTableColumn(
                FXCollections.observableArrayList(names)
        ));

        materialColumn.setOnEditCommit(event -> {
            StockInItem item = event.getRowValue();
            String newName = event.getNewValue();
            item.setMaterialName(newName);

            for (OptionItem opt : materialList) {
                if (opt.getName().equals(newName)) { // 现在用 getName()
                    item.setMaterialId(opt.getId());
                    break;
                }
            }
        });
    }

    @FXML
    protected void onAddItemButtonClick() {
        StockInItem newItem = new StockInItem();
        newItem.setQuantity(0);
        newItem.setPrice(BigDecimal.ZERO);
        newItem.setAmount(BigDecimal.ZERO);
        itemList.add(newItem);
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
            MessageDialog.showDialog("请至少添加一条明细");
            return;
        }

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("type", getTypeValue(typeComboBox.getValue()));

            List<Map<String, Object>> items = new ArrayList<>();
            for (StockInItem item : itemList) {
                if (item.getMaterialId() == null) {
                    MessageDialog.showDialog("物资不能为空");
                    return;
                }
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("materialId", item.getMaterialId());
                itemMap.put("quantity", item.getQuantity());
                itemMap.put("price", item.getPrice());
                items.add(itemMap);
            }
            requestBody.put("items", items);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(HttpRequestUtil.serverUrl + "/stock-in/create"))
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                    .headers("Content-Type", "application/json", "satoken", AppStore.getJwt().getToken())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                MessageDialog.showDialog("入库单提交成功");
                this.close();
            } else {
                MessageDialog.showDialog("提交失败：" + response.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
            MessageDialog.showDialog("提交异常：" + e.getMessage());
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
            case "其他": return 3;
            default: return 1;
        }
    }
}
