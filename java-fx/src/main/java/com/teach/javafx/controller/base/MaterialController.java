package com.teach.javafx.controller.base;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.DataResponse;
import com.teach.javafx.request.HttpRequestUtil;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MaterialController {
    private static final Logger logger = Logger.getLogger(MaterialController.class.getName());

    @FXML
    private TableView<MaterialNode> materialTable;

    @FXML
    private TableColumn<MaterialNode, Integer> idCol;
    @FXML
    private TableColumn<MaterialNode, String> nameCol;
    @FXML
    private TableColumn<MaterialNode, String> codeCol;
    @FXML
    private TableColumn<MaterialNode, String> categoryNameCol;
    @FXML
    private TableColumn<MaterialNode, String> unitCol;
    @FXML
    private TableColumn<MaterialNode, Integer> currentStockCol;
    @FXML
    private TableColumn<MaterialNode, Integer> safetyStockCol;
    @FXML
    private TableColumn<MaterialNode, BigDecimal> priceCol;
    @FXML
    private TableColumn<MaterialNode, String> statusCol;
    @FXML
    private TableColumn<MaterialNode, LocalDateTime> createTimeCol;
    @FXML
    private TableColumn<MaterialNode, Void> actionCol;

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> categoryFilter;
    @FXML
    private ComboBox<String> stockStatusFilter;

    private final ObservableList<MaterialNode> dataList = FXCollections.observableArrayList();
    private final Gson gson = new Gson();

    @FXML
    public void initialize() {
        setupTable();
        setupFilters();
        loadMaterials();
    }

    private void setupTable() {
        idCol.setCellValueFactory(param -> param.getValue().getIdProperty().asObject());
        nameCol.setCellValueFactory(param -> param.getValue().nameProperty());
        codeCol.setCellValueFactory(param -> param.getValue().codeProperty());
        categoryNameCol.setCellValueFactory(param -> param.getValue().categoryNameProperty());
        unitCol.setCellValueFactory(param -> param.getValue().unitProperty());
        currentStockCol.setCellValueFactory(param -> param.getValue().currentStockProperty().asObject());
        safetyStockCol.setCellValueFactory(param -> param.getValue().safetyStockProperty().asObject());
        priceCol.setCellValueFactory(param -> param.getValue().priceProperty());
        statusCol.setCellValueFactory(param -> param.getValue().statusProperty());
        createTimeCol.setCellValueFactory(param -> param.getValue().createTimeProperty());

        actionCol.setCellFactory(new Callback<>() {
            @Override
            public TableCell<MaterialNode, Void> call(TableColumn<MaterialNode, Void> param) {
                return new TableCell<>() {
                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            Button editBtn = new Button("编辑");
                            editBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-cursor: hand;");
                            editBtn.setOnAction(e -> {
                                MaterialNode material = getTableView().getSelectionModel().getSelectedItem();
                                editMaterial(material);
                            });

                            Button deleteBtn = new Button("删除");
                            deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-cursor: hand;");
                            deleteBtn.setOnAction(e -> {
                                MaterialNode material = getTableView().getSelectionModel().getSelectedItem();
                                deleteMaterial(material);
                            });

                            javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(5, editBtn, deleteBtn);
                            box.setPadding(new Insets(5));
                            setGraphic(box);
                        }
                    }
                };
            }
        });

        materialTable.setItems(dataList);
    }

    private void setupFilters() {
        stockStatusFilter.getItems().addAll("全部", "正常", "预警");
        stockStatusFilter.setValue("全部");
        searchField.setPromptText("输入物资名称搜索...");

        loadCategoryOptions();
    }

    private void loadCategoryOptions() {
        try {
            DataRequest request = new DataRequest();
            String responseStr = HttpRequestUtil.post("/api/category/options", gson.toJson(request.getParams()));

            if (responseStr != null) {
                DataResponse response = gson.fromJson(responseStr, DataResponse.class);
                if (response != null && response.getCode() == 200) {
                    Platform.runLater(() -> {
                        categoryFilter.getItems().clear();
                        categoryFilter.getItems().add("全部分类");

                        if (response.getData() != null) {
                            Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
                            List<Map<String, Object>> categories = gson.fromJson(gson.toJson(response.getData()), listType);

                            if (categories != null) {
                                for (Map<String, Object> category : categories) {
                                    categoryFilter.getItems().add((String) category.get("name"));
                                }
                            }
                        }
                        categoryFilter.setValue("全部分类");
                    });
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "加载分类选项失败", e);
        }
    }

    @FXML
    private void loadMaterials() {
        try {
            DataRequest request = new DataRequest();
            String responseStr = HttpRequestUtil.post("/api/material/list", gson.toJson(request.getParams()));

            if (responseStr != null) {
                DataResponse response = gson.fromJson(responseStr, DataResponse.class);
                if (response != null && response.getCode() == 200) {
                    Platform.runLater(() -> buildDataList(response.getData()));
                } else {
                    showError("加载失败", response != null ? response.getMsg() : "网络错误");
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "加载物资列表异常", e);
            showError("加载异常", e.getMessage());
        }
    }

    private void buildDataList(Object data) {
        dataList.clear();

        if (data == null) {
            return;
        }

        Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
        List<Map<String, Object>> materialList = gson.fromJson(gson.toJson(data), listType);

        if (materialList != null) {
            for (Map<String, Object> material : materialList) {
                MaterialNode node = mapToMaterialNode(material);
                dataList.add(node);
            }
        }

        materialTable.refresh();
    }

    private MaterialNode mapToMaterialNode(Map<String, Object> map) {
        MaterialNode node = new MaterialNode();

        if (map.get("id") != null) {
            node.setId(((Number) map.get("id")).intValue());
        }
        if (map.get("name") != null) {
            node.setName((String) map.get("name"));
        }
        if (map.get("code") != null) {
            node.setCode((String) map.get("code"));
        }
        if (map.get("categoryName") != null) {
            node.setCategoryName((String) map.get("categoryName"));
        }
        if (map.get("unit") != null) {
            node.setUnit((String) map.get("unit"));
        }
        if (map.get("currentStock") != null) {
            node.setCurrentStock(((Number) map.get("currentStock")).intValue());
        }
        if (map.get("safetyStock") != null) {
            node.setSafetyStock(((Number) map.get("safetyStock")).intValue());
        }
        if (map.get("price") != null) {
            node.setPrice(new BigDecimal(map.get("price").toString()));
        }
        if (map.get("status") != null) {
            node.setStatus((String) map.get("status"));
        }
        if (map.get("createTime") != null) {
            try {
                node.setCreateTime(LocalDateTime.parse(map.get("createTime").toString()));
            } catch (Exception e) {
                logger.log(Level.WARNING, "解析创建时间失败", e);
            }
        }

        return node;
    }

    @FXML
    private void searchMaterial() {
        String keyword = searchField.getText();
        String category = categoryFilter.getValue();
        String stockStatus = stockStatusFilter.getValue();

        try {
            DataRequest request = new DataRequest();
            if (keyword != null && !keyword.isEmpty()) {
                request.put("keyword", keyword);
            }
            if (category != null && !"全部分类".equals(category)) {
                request.put("categoryName", category);
            }
            if (stockStatus != null && !"全部".equals(stockStatus)) {
                request.put("stockStatus", stockStatus);
            }

            String responseStr = HttpRequestUtil.post("/api/material/search", gson.toJson(request.getParams()));

            if (responseStr != null) {
                DataResponse response = gson.fromJson(responseStr, DataResponse.class);
                if (response != null && response.getCode() == 200) {
                    Platform.runLater(() -> buildDataList(response.getData()));
                } else {
                    showError("搜索失败", response != null ? response.getMsg() : "网络错误");
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "搜索物资异常", e);
            showError("搜索异常", e.getMessage());
        }
    }

    @FXML
    private void resetSearch() {
        searchField.clear();
        categoryFilter.setValue("全部分类");
        stockStatusFilter.setValue("全部");
        loadMaterials();
    }

    @FXML
    private void addMaterial() {
        showEditDialog(null);
    }

    private void editMaterial(MaterialNode material) {
        if (material != null) {
            showEditDialog(material);
        }
    }

    private void deleteMaterial(MaterialNode material) {
        if (material == null) {
            showError("删除失败", "请选择要删除的物资");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("确认删除");
        confirm.setHeaderText("确定要删除物资 \"" + material.getName() + "\" 吗？");
        confirm.setContentText("删除后无法恢复！");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    DataRequest request = new DataRequest();
                    request.put("id", material.getId());

                    String responseStr = HttpRequestUtil.post("/api/material/delete", gson.toJson(request.getParams()));

                    if (responseStr != null) {
                        DataResponse resp = gson.fromJson(responseStr, DataResponse.class);
                        if (resp != null && resp.getCode() == 200) {
                            showInfo("删除成功", "物资已删除");
                            loadMaterials();
                        } else {
                            showError("删除失败", resp != null ? resp.getMsg() : "网络错误");
                        }
                    }
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "删除物资异常", e);
                    showError("删除异常", e.getMessage());
                }
            }
        });
    }

    private void showEditDialog(MaterialNode material) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(material == null ? "新增物资" : "编辑物资");

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setHgap(10);
        grid.setVgap(10);

        TextField nameField = new TextField(material != null ? material.getName() : "");
        nameField.setPromptText("请输入物资名称");

        TextField codeField = new TextField(material != null ? material.getCode() : "");
        codeField.setPromptText("请输入物资编码");

        TextField unitField = new TextField(material != null ? material.getUnit() : "");
        unitField.setPromptText("请输入单位(如:个、件、箱)");

        TextField currentStockField = new TextField(material != null ? String.valueOf(material.getCurrentStock()) : "0");
        currentStockField.setPromptText("当前库存数量");

        TextField safetyStockField = new TextField(material != null ? String.valueOf(material.getSafetyStock()) : "0");
        safetyStockField.setPromptText("安全库存数量");

        TextField priceField = new TextField(material != null && material.getPrice() != null ? material.getPrice().toString() : "0");
        priceField.setPromptText("单价");

        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("启用", "停用");
        statusCombo.setValue(material != null ? material.getStatus() : "启用");

        grid.add(new Label("物资名称:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("物资编码:"), 0, 1);
        grid.add(codeField, 1, 1);
        grid.add(new Label("单位:"), 0, 2);
        grid.add(unitField, 1, 2);
        grid.add(new Label("当前库存:"), 0, 3);
        grid.add(currentStockField, 1, 3);
        grid.add(new Label("安全库存:"), 0, 4);
        grid.add(safetyStockField, 1, 4);
        grid.add(new Label("单价:"), 0, 5);
        grid.add(priceField, 1, 5);
        grid.add(new Label("状态:"), 0, 6);
        grid.add(statusCombo, 1, 6);

        Button saveBtn = new Button("保存");
        Button cancelBtn = new Button("取消");

        saveBtn.setOnAction(e -> {
            try {
                DataRequest request = new DataRequest();
                request.put("name", nameField.getText());
                request.put("code", codeField.getText());
                request.put("unit", unitField.getText());
                request.put("currentStock", Integer.parseInt(currentStockField.getText()));
                request.put("safetyStock", Integer.parseInt(safetyStockField.getText()));
                request.put("price", new BigDecimal(priceField.getText()));
                request.put("status", statusCombo.getValue());

                if (material != null) {
                    request.put("id", material.getId());
                }

                String url = material == null ? "/api/material/add" : "/api/material/update";
                String responseStr = HttpRequestUtil.post(url, gson.toJson(request.getParams()));

                if (responseStr != null) {
                    DataResponse response = gson.fromJson(responseStr, DataResponse.class);
                    if (response != null && response.getCode() == 200) {
                        showInfo("操作成功", material == null ? "物资已创建" : "物资已更新");
                        dialog.close();
                        loadMaterials();
                    } else {
                        showError("操作失败", response != null ? response.getMsg() : "网络错误");
                    }
                }
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "操作物资异常", ex);
                showError("操作异常", ex.getMessage());
            }
        });

        cancelBtn.setOnAction(e -> dialog.close());

        javafx.scene.layout.HBox buttonBox = new javafx.scene.layout.HBox(10, saveBtn, cancelBtn);
        grid.add(buttonBox, 1, 7);

        Scene scene = new Scene(grid, 450, 320);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void showInfo(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public static class MaterialNode {
        private final IntegerProperty id = new SimpleIntegerProperty();
        private final StringProperty name = new SimpleStringProperty("");
        private final StringProperty code = new SimpleStringProperty("");
        private final StringProperty categoryName = new SimpleStringProperty("");
        private final StringProperty unit = new SimpleStringProperty("");
        private final IntegerProperty currentStock = new SimpleIntegerProperty();
        private final IntegerProperty safetyStock = new SimpleIntegerProperty();
        private final ObjectProperty<BigDecimal> price = new SimpleObjectProperty<>(BigDecimal.ZERO);
        private final StringProperty status = new SimpleStringProperty("启用");
        private final ObjectProperty<LocalDateTime> createTime = new SimpleObjectProperty<>();

        public IntegerProperty getIdProperty() { return id; }
        public int getId() { return id.get(); }
        public void setId(int value) { id.set(value); }

        public StringProperty nameProperty() { return name; }
        public String getName() { return name.get(); }
        public void setName(String value) { name.set(value); }

        public StringProperty codeProperty() { return code; }
        public String getCode() { return code.get(); }
        public void setCode(String value) { code.set(value); }

        public StringProperty categoryNameProperty() { return categoryName; }
        @SuppressWarnings("unused")
        public String getCategoryName() { return categoryName.get(); }
        public void setCategoryName(String value) { categoryName.set(value); }

        public StringProperty unitProperty() { return unit; }
        public String getUnit() { return unit.get(); }
        public void setUnit(String value) { unit.set(value); }

        public IntegerProperty currentStockProperty() { return currentStock; }
        public int getCurrentStock() { return currentStock.get(); }
        public void setCurrentStock(int value) { currentStock.set(value); }

        public IntegerProperty safetyStockProperty() { return safetyStock; }
        public int getSafetyStock() { return safetyStock.get(); }
        public void setSafetyStock(int value) { safetyStock.set(value); }

        public ObjectProperty<BigDecimal> priceProperty() { return price; }
        public BigDecimal getPrice() { return price.get(); }
        public void setPrice(BigDecimal value) { price.set(value); }

        public StringProperty statusProperty() { return status; }
        public String getStatus() { return status.get(); }
        public void setStatus(String value) { status.set(value); }

        public ObjectProperty<LocalDateTime> createTimeProperty() { return createTime; }
        @SuppressWarnings("unused")
        public LocalDateTime getCreateTime() { return createTime.get(); }
        public void setCreateTime(LocalDateTime value) { createTime.set(value); }
    }
}
