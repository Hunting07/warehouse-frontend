package com.teach.javafx.controller.base;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.teach.javafx.AppStore;
import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.DataResponse;
import com.teach.javafx.request.HttpRequestUtil;
import com.teach.javafx.request.JwtResponse;
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

public class MaterialController extends ToolController {
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
    private TableColumn<MaterialNode, String> warningCol;
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
    @FXML
    private ComboBox<String> materialStatusFilter;
    @FXML
    private Button addMaterialBtn;

    private final ObservableList<MaterialNode> dataList = FXCollections.observableArrayList();
    private final Gson gson = new Gson();
    private boolean isAdmin = false;
    private List<Map<String, Object>> categoryListCache;

    @FXML
    public void initialize() {
        checkUserRole();
        applyRolePermissions();
        setupTable();
        setupFilters();
        loadMaterials();
    }


    private void checkUserRole() {
        JwtResponse jwt = AppStore.getJwt();
        if (jwt != null && jwt.getRole() != null) {
            isAdmin = "admin".equals(jwt.getRole()) || "管理员".equals(jwt.getRole());
        }
        System.out.println("物资管理-当前用户角色: " + (jwt != null ? jwt.getRole() : "null") + ", 是否管理员: " + isAdmin);
    }

    private void applyRolePermissions() {
        if (!isAdmin) {
            if (addMaterialBtn != null) {
                addMaterialBtn.setVisible(false);
                addMaterialBtn.setManaged(false);
            }
            System.out.println("物资管理-当前为普通员工，仅查看权限");
        } else {
            System.out.println("物资管理-当前为管理员，拥有完整操作权限");
        }
    }

    private void setupTable() {
        idCol.setCellValueFactory(param -> {
            int rowIndex = materialTable.getItems().indexOf(param.getValue()) + 1;
            return new SimpleIntegerProperty(rowIndex).asObject();
        });
        nameCol.setCellValueFactory(param -> param.getValue().nameProperty());
        codeCol.setCellValueFactory(param -> param.getValue().codeProperty());
        categoryNameCol.setCellValueFactory(param -> param.getValue().categoryNameProperty());
        unitCol.setCellValueFactory(param -> param.getValue().unitProperty());
        currentStockCol.setCellValueFactory(param -> param.getValue().currentStockProperty().asObject());
        safetyStockCol.setCellValueFactory(param -> param.getValue().safetyStockProperty().asObject());

        // 库存预警列：根据当前库存和安全库存的关系显示"正常"或"预警"
        warningCol.setCellValueFactory(cellData -> {
            MaterialNode material = cellData.getValue();
            int currentStock = material.getCurrentStock();
            int safetyStock = material.getSafetyStock();

            if (currentStock < safetyStock) {
                return new SimpleStringProperty("预警");
            } else {
                return new SimpleStringProperty("正常");
            }
        });

        // 设置预警列的单元格工厂，实现标红效果
        warningCol.setCellFactory(column -> new TableCell<>() {

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);

                    // 根据库存状态设置样式
                    if ("预警".equals(item)) {
                        setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: green;");
                    }
                }
            }
        });

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
                            if (!isAdmin) {
                                setGraphic(null);
                                return;
                            }

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

        materialStatusFilter.getItems().addAll("全部", "启用", "停用");
        materialStatusFilter.setValue("全部");

        searchField.setPromptText("输入物资名称搜索...");

        loadCategoryOptions();
    }

    private void loadCategoryOptions() {
        try {
            categoryFilter.getItems().add("全部分类");
            categoryFilter.setValue("全部分类");

            DataRequest request = new DataRequest();
            DataResponse response = HttpRequestUtil.request("/api/category/tree", request);

            if (response != null && response.getCode() == 200) {
                Platform.runLater(() -> {
                    if (response.getData() != null) {
                        Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
                        categoryListCache = gson.fromJson(gson.toJson(response.getData()), listType);

                        if (categoryListCache != null) {
                            for (Map<String, Object> category : categoryListCache) {
                                String name = (String) category.get("name");
                                if (name != null && !categoryFilter.getItems().contains(name)) {
                                    categoryFilter.getItems().add(name);
                                }
                            }
                        }
                    }
                });
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "加载分类选项失败", e);
            if (categoryFilter.getItems().isEmpty()) {
                categoryFilter.getItems().add("全部分类");
                categoryFilter.setValue("全部分类");
            }
        }
    }

    @FXML
    private void loadMaterials() {
        try {
            DataRequest request = new DataRequest();
            DataResponse response = HttpRequestUtil.request("/api/material/list", request);

            if (response != null && response.getCode() == 200) {
                Platform.runLater(() -> buildDataList(response.getData()));
            } else {
                String errorMsg = response != null ? response.getMsg() : "网络错误";
                System.out.println("物资列表加载失败: " + errorMsg);
                showError("加载失败", "后端接口返回错误: " + errorMsg);
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
                try {
                    MaterialNode node = mapToMaterialNode(material);
                    if (node != null) {
                        dataList.add(node);
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING, "转换物资数据失败: " + material, e);
                }
            }
        }

        materialTable.refresh();
    }

    private MaterialNode mapToMaterialNode(Map<String, Object> map) {
        MaterialNode node = new MaterialNode();

        try {
            if (map.get("id") != null) {
                node.setId(((Number) map.get("id")).intValue());
            }
            if (map.get("name") != null) {
                node.setName((String) map.get("name"));
            }
            if (map.get("code") != null) {
                node.setCode((String) map.get("code"));
            }
            if (map.get("categoryId") != null) {
                node.setCategoryId(((Number) map.get("categoryId")).intValue());
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
                try {
                    Object priceObj = map.get("price");
                    BigDecimal price;

                    if (priceObj instanceof BigDecimal) {
                        price = (BigDecimal) priceObj;
                    } else if (priceObj instanceof Double) {
                        price = BigDecimal.valueOf((Double) priceObj);
                    } else if (priceObj instanceof Float) {
                        price = BigDecimal.valueOf((Float) priceObj);
                    } else if (priceObj instanceof Integer) {
                        price = BigDecimal.valueOf((Integer) priceObj);
                    } else if (priceObj instanceof Long) {
                        price = BigDecimal.valueOf((Long) priceObj);
                    } else if (priceObj instanceof Number) {
                        price = BigDecimal.valueOf(((Number) priceObj).doubleValue());
                    } else {
                        price = new BigDecimal(priceObj.toString());
                    }

                    node.setPrice(price);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "解析价格字段失败: " + map.get("price"), e);
                    node.setPrice(BigDecimal.ZERO);
                }
            }

            if (map.get("status") != null) {
                Object statusObj = map.get("status");
                if (statusObj instanceof String) {
                    node.setStatus((String) statusObj);
                } else if (statusObj instanceof Number) {
                    node.setStatus(String.valueOf(((Number) statusObj).intValue()));
                } else {
                    node.setStatus(statusObj.toString());
                }
            }

            if (map.get("createTime") != null) {
                try {
                    node.setCreateTime(LocalDateTime.parse(map.get("createTime").toString()));
                } catch (Exception e) {
                    logger.log(Level.WARNING, "解析创建时间失败", e);
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "转换物资数据失败", e);
            return null;
        }

        return node;
    }

    @FXML
    private void searchMaterial() {
        String keyword = searchField.getText();
        String category = categoryFilter.getValue();
        String stockStatus = stockStatusFilter.getValue();
        String materialStatus = materialStatusFilter != null ? materialStatusFilter.getValue() : "全部";

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
                System.out.println("库存状态筛选: " + stockStatus);
            }
            if (materialStatus != null && !"全部".equals(materialStatus)) {
                int statusCode = "启用".equals(materialStatus) ? 1 : 0;
                request.put("status", statusCode);
                System.out.println("物资状态转换: " + materialStatus + " -> " + statusCode);
            }

            System.out.println("====== 物资搜索请求 ======");
            System.out.println("请求URL: /api/material/search");
            System.out.println("请求参数: " + gson.toJson(request.getParams()));

            DataResponse response = HttpRequestUtil.request("/api/material/search", request);

            System.out.println("响应结果: " + (response != null ? gson.toJson(response) : "null"));

            if (response != null && response.getCode() == 200) {
                // 如果后端不支持 stockStatus 参数，在前端进行过滤
                if (stockStatus != null && !"全部".equals(stockStatus)) {
                    Platform.runLater(() -> {
                        List<Map<String, Object>> allMaterials = gson.fromJson(
                            gson.toJson(response.getData()),
                            new TypeToken<List<Map<String, Object>>>(){}.getType()
                        );

                        List<Map<String, Object>> filteredMaterials = new java.util.ArrayList<>();
                        for (Map<String, Object> material : allMaterials) {
                            try {
                                int currentStock = ((Number) material.get("currentStock")).intValue();
                                int safetyStock = ((Number) material.get("safetyStock")).intValue();

                                boolean isWarning = currentStock < safetyStock;
                                boolean matchesFilter = false;

                                if ("预警".equals(stockStatus)) {
                                    matchesFilter = isWarning;
                                } else if ("正常".equals(stockStatus)) {
                                    matchesFilter = !isWarning;
                                }

                                if (matchesFilter) {
                                    filteredMaterials.add(material);
                                }
                            } catch (Exception e) {
                                logger.log(Level.WARNING, "过滤物资数据失败", e);
                            }
                        }

                        System.out.println("前端过滤后物资数量: " + filteredMaterials.size() + " / " + allMaterials.size());
                        buildDataList(filteredMaterials);
                    });
                } else {
                    Platform.runLater(() -> buildDataList(response.getData()));
                }
            } else {
                String errorMsg = response != null ? response.getMsg() : "网络错误";
                System.out.println("搜索失败: " + errorMsg);
                showError("搜索失败", "后端返回错误: " + errorMsg);
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
        if (materialStatusFilter != null) {
            materialStatusFilter.setValue("全部");
        }
        loadMaterials();
    }

    @FXML
    private void addMaterial() {
        if (!isAdmin) {
            showError("权限不足", "只有管理员可以新增物资");
            return;
        }
        showEditDialog(null);
    }

    private void editMaterial(MaterialNode material) {
        if (!isAdmin) {
            showError("权限不足", "只有管理员可以编辑物资");
            return;
        }
        if (material != null) {
            showEditDialog(material);
        }
    }

        private void deleteMaterial(MaterialNode material) {
        if (!isAdmin) {
            showError("权限不足", "只有管理员可以删除物资");
            return;
        }
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

                    System.out.println("====== 物资删除请求 ======");
                    System.out.println("请求URL: /api/material/delete");
                    System.out.println("请求参数: " + gson.toJson(request.getParams()));

                    DataResponse resp = HttpRequestUtil.request("/api/material/delete", request);

                    System.out.println("响应结果: " + (resp != null ? gson.toJson(resp) : "null"));

                    if (resp != null && resp.getCode() == 200) {
                        showInfo("删除成功", "物资已删除");
                        loadMaterials();
                    } else {
                        String errorMsg = resp != null ? resp.getMsg() : "网络错误";
                        System.out.println("删除失败: " + errorMsg);
                        showError("删除失败", "后端返回错误: " + errorMsg);
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

        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.getItems().add("请选择分类");

        try {
            DataRequest catRequest = new DataRequest();
            DataResponse catResponse = HttpRequestUtil.request("/api/category/tree", catRequest);

            if (catResponse != null && catResponse.getCode() == 200 && catResponse.getData() != null) {
                Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
                List<Map<String, Object>> freshCategoryList = gson.fromJson(gson.toJson(catResponse.getData()), listType);

                if (freshCategoryList != null) {
                    for (Map<String, Object> category : freshCategoryList) {
                        String name = (String) category.get("name");
                        if (name != null && !categoryCombo.getItems().contains(name)) {
                            categoryCombo.getItems().add(name);
                        }
                    }
                }

                categoryListCache = freshCategoryList;
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "刷新分类选项失败", e);
            if (categoryListCache != null) {
                for (Map<String, Object> category : categoryListCache) {
                    String name = (String) category.get("name");
                    if (name != null && !categoryCombo.getItems().contains(name)) {
                        categoryCombo.getItems().add(name);
                    }
                }
            }
        }

        if (material != null && material.getCategoryName() != null && !material.getCategoryName().isEmpty()) {
            categoryCombo.setValue(material.getCategoryName());
        } else {
            categoryCombo.setValue("请选择分类");
        }

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
        grid.add(new Label("所属分类:"), 0, 2);
        grid.add(categoryCombo, 1, 2);
        grid.add(new Label("单位:"), 0, 3);
        grid.add(unitField, 1, 3);
        grid.add(new Label("当前库存:"), 0, 4);
        grid.add(currentStockField, 1, 4);
        grid.add(new Label("安全库存:"), 0, 5);
        grid.add(safetyStockField, 1, 5);
        grid.add(new Label("单价:"), 0, 6);
        grid.add(priceField, 1, 6);
        grid.add(new Label("状态:"), 0, 7);
        grid.add(statusCombo, 1, 7);

        Button saveBtn = new Button("保存");
        Button cancelBtn = new Button("取消");

        saveBtn.setOnAction(e -> {
            try {
                DataRequest request = new DataRequest();
                request.put("name", nameField.getText());
                request.put("code", codeField.getText());

                String selectedCategory = categoryCombo.getValue();
                if (selectedCategory != null && !"请选择分类".equals(selectedCategory)) {
                    request.put("categoryName", selectedCategory);
                    if (categoryListCache != null) {
                        for (Map<String, Object> category : categoryListCache) {
                            if (selectedCategory.equals(category.get("name"))) {
                                request.put("categoryId", ((Number) category.get("id")).intValue());
                                break;
                            }
                        }
                    }
                }

                request.put("unit", unitField.getText());
                request.put("currentStock", Integer.parseInt(currentStockField.getText()));
                request.put("safetyStock", Integer.parseInt(safetyStockField.getText()));

                String priceText = priceField.getText();
                if (priceText != null && !priceText.isEmpty()) {
                    request.put("price", new BigDecimal(priceText));
                } else {
                    request.put("price", BigDecimal.ZERO);
                }
                request.put("status", statusCombo.getValue());

                if (material != null) {
                    request.put("id", material.getId());
                }

                String url = material == null ? "/api/material/add" : "/api/material/update";
                System.out.println("====== 物资保存请求 ======");
                System.out.println("请求URL: " + url);
                System.out.println("请求参数: " + gson.toJson(request.getParams()));

                DataResponse response = HttpRequestUtil.request(url, request);

                System.out.println("响应结果: " + (response != null ? gson.toJson(response) : "null"));

                if (response != null && response.getCode() == 200) {
                    showInfo("操作成功", material == null ? "物资已创建" : "物资已更新");
                    dialog.close();
                    loadMaterials();
                } else {
                    String errorMsg = response != null ? response.getMsg() : "网络错误";
                    System.out.println("保存失败: " + errorMsg);
                    showError("操作失败", "后端返回错误: " + errorMsg);
                }
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "操作物资异常", ex);
                showError("操作异常", ex.getMessage());
            }
        });

        cancelBtn.setOnAction(e -> dialog.close());

        javafx.scene.layout.HBox buttonBox = new javafx.scene.layout.HBox(10, saveBtn, cancelBtn);
        grid.add(buttonBox, 1, 8);

        Scene scene = new Scene(grid, 450, 350);
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

    @Override
    public void doRefresh() {
        loadMaterials();
    }

    @Override
    public void doNew() {
        addMaterial();
    }

    @Override
    @SuppressWarnings({"unused", "RedundantSuppression"})
    public void doSave() {
        // 物资保存通过编辑对话框完成，此方法暂不使用
    }

    @Override
    @SuppressWarnings({"unused", "RedundantSuppression"})
    public void doDelete() {
        // 物资删除通过操作列按钮完成，此方法暂不使用
    }

    @Override
    @SuppressWarnings({"unused", "RedundantSuppression"})
    public void doPrint() {
        // 打印功能待实现
    }

    @Override
    @SuppressWarnings({"unused", "RedundantSuppression"})
    public void doExport() {
        // 导出功能待实现
    }

    @Override
    @SuppressWarnings({"unused", "RedundantSuppression"})
    public void doImport() {
        // 导入功能待实现
    }

    @Override
    public void doTest() {
        loadMaterials();
    }

    public static class MaterialNode {
        private final IntegerProperty id = new SimpleIntegerProperty();
        private final StringProperty name = new SimpleStringProperty("");
        private final StringProperty code = new SimpleStringProperty("");
        private final IntegerProperty categoryId = new SimpleIntegerProperty();
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

        @SuppressWarnings("unused")
        public IntegerProperty categoryIdProperty() { return categoryId; }
        @SuppressWarnings("unused")
        public int getCategoryId() { return categoryId.get(); }
        public void setCategoryId(int value) { categoryId.set(value); }

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
