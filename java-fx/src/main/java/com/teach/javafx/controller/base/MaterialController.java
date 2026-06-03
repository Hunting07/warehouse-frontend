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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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
    }

    private void applyRolePermissions() {
        if (!isAdmin) {
            if (addMaterialBtn != null) {
                addMaterialBtn.setVisible(false);
                addMaterialBtn.setManaged(false);
            }
        }
    }
    private void setupTable() {
        if (isAdmin) {
            actionCol.setPrefWidth(150);
        } else {
            actionCol.setVisible(false);
        }

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
                                MaterialNode material = getTableView().getItems().get(getIndex());
                                editMaterial(material);
                            });

                            Button deleteBtn = new Button("删除");
                            deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-cursor: hand;");
                            deleteBtn.setOnAction(e -> {
                                MaterialNode material = getTableView().getItems().get(getIndex());
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
        materialTable.setRowFactory(tv -> new TableRow<MaterialNode>() {
            @Override
            protected void updateItem(MaterialNode item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setStyle("");
                } else {
                    int index = getIndex();
                    if (index % 2 == 0) {
                        // 偶数行（第0, 2, 4...行）：白色
                        setStyle("-fx-background-color: white;");
                    } else {
                        // 奇数行（第1, 3, 5...行）：浅蓝色
                        setStyle("-fx-background-color: #F0F7FF;");
                    }
                }
            }
        });
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

                    switch (priceObj) {
                        case BigDecimal bd -> price = bd;
                        case Double d -> price = BigDecimal.valueOf(d);
                        case Float f -> price = BigDecimal.valueOf(f);
                        case Integer i -> price = BigDecimal.valueOf(i);
                        case Long l -> price = BigDecimal.valueOf(l);
                        case Number n -> price = BigDecimal.valueOf(n.doubleValue());
                        default -> price = new BigDecimal(priceObj.toString());
                    }

                    node.setPrice(price);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "解析价格字段失败: " + map.get("price"), e);
                    node.setPrice(BigDecimal.ZERO);
                }
            }
            
            if (map.get("status") != null) {
                Object statusObj = map.get("status");
                String statusText;
                if (statusObj instanceof Number) {
                    int statusValue = ((Number) statusObj).intValue();
                    statusText = (statusValue == 1) ? "启用" : "停用";
                } else {
                    String statusStr = statusObj.toString();
                    statusText = ("1".equals(statusStr) || "启用".equals(statusStr)) ? "启用" : "停用";
                }
                node.setStatus(statusText);
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
            }

            DataResponse response = HttpRequestUtil.request("/api/material/search", request);

            if (response != null && response.getCode() == 200) {
                Platform.runLater(() -> {
                    List<Map<String, Object>> allMaterials = gson.fromJson(
                        gson.toJson(response.getData()),
                        new TypeToken<List<Map<String, Object>>>(){}.getType()
                    );

                    if (allMaterials == null) {
                        buildDataList(null);
                        return;
                    }

                    List<Map<String, Object>> filteredMaterials = new java.util.ArrayList<>();

                    for (Map<String, Object> material : allMaterials) {
                        try {
                            boolean matchesStockFilter = true;
                            if (stockStatus != null && !"全部".equals(stockStatus)) {
                                int currentStock = ((Number) material.get("currentStock")).intValue();
                                int safetyStock = ((Number) material.get("safetyStock")).intValue();
                                boolean isWarning = currentStock < safetyStock;

                                if ("预警".equals(stockStatus)) {
                                    matchesStockFilter = isWarning;
                                } else if ("正常".equals(stockStatus)) {
                                    matchesStockFilter = !isWarning;
                                }
                            }

                            boolean matchesStatusFilter = true;
                            if (materialStatus != null && !"全部".equals(materialStatus)) {
                                int materialStatusCode = 0;
                                if (material.get("status") != null) {
                                    Object statusObj = material.get("status");
                                    if (statusObj instanceof Number) {
                                        materialStatusCode = ((Number) statusObj).intValue();
                                    } else {
                                        String statusStr = statusObj.toString();
                                        materialStatusCode = ("1".equals(statusStr) || "启用".equals(statusStr)) ? 1 : 0;
                                    }
                                }

                                String categoryName = (String) material.get("categoryName");

                                if (categoryName == null || categoryName.isEmpty()) {
                                    // 无分类物资：根据自身状态决定
                                    if ("启用".equals(materialStatus)) {
                                        matchesStatusFilter = (materialStatusCode == 1);
                                    } else {
                                        matchesStatusFilter = (materialStatusCode == 0);
                                    }
                                } else {
                                    // 有分类物资：查找分类状态
                                    int categoryStatusCode = -1;
                                    boolean foundCategory = false;

                                    if (categoryListCache != null) {
                                        for (Map<String, Object> cat : categoryListCache) {
                                            if (categoryName.equals(cat.get("name"))) {
                                                Object catStatusObj = cat.get("status");
                                                if (catStatusObj instanceof Number) {
                                                    categoryStatusCode = ((Number) catStatusObj).intValue();
                                                } else {
                                                    String catStatusStr = catStatusObj.toString();
                                                    categoryStatusCode = ("1".equals(catStatusStr) || "启用".equals(catStatusStr)) ? 1 : 0;
                                                }
                                                foundCategory = true;
                                                break;
                                            }
                                        }
                                    }

                                    // 如果缓存中没找到，默认为停用状态
                                    if (!foundCategory) {
                                        categoryStatusCode = 0;
                                    }

                                    // 有分类物资：只有分类和自身状态都为启用时才显示在"启用"结果
                                    // 其他情况一律显示在"停用"结果
                                    if ("启用".equals(materialStatus)) {
                                        matchesStatusFilter = (materialStatusCode == 1 && categoryStatusCode == 1);
                                    } else {
                                        // 停用筛选：只要不是"分类和自身都为启用"的情况
                                        matchesStatusFilter = !(materialStatusCode == 1 && categoryStatusCode == 1);
                                    }

                                }
                            }


                            if (matchesStockFilter && matchesStatusFilter) {
                                filteredMaterials.add(material);
                            }
                        } catch (Exception e) {
                            logger.log(Level.WARNING, "过滤物资数据失败", e);
                        }
                    }

                    buildDataList(filteredMaterials);
                });
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
    private void refreshData() {
        searchField.clear();
        categoryFilter.setValue("全部分类");
        stockStatusFilter.setValue("全部");
        if (materialStatusFilter != null) {
            materialStatusFilter.setValue("全部");
        }
        categoryFilter.getItems().clear();
        loadCategoryOptions();
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

        confirm.getDialogPane().setStyle("-fx-background-color: white;");
        confirm.getDialogPane().getStylesheets().add(getClass().getResource("/styles/modern-style.css").toExternalForm());

        ButtonType okBtn = new ButtonType("确定", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(okBtn, cancelBtn);

        Button okButton = (Button) confirm.getDialogPane().lookupButton(okBtn);
        okButton.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5; -fx-padding: 8 20 8 20;");

        Button cancelButton = (Button) confirm.getDialogPane().lookupButton(cancelBtn);
        cancelButton.setStyle("-fx-background-color: #E0E0E0; -fx-text-fill: #666666; -fx-font-size: 14px; -fx-cursor: hand; -fx-background-radius: 5; -fx-padding: 8 20 8 20;");

        System.out.println("=== 删除按钮被点击 ===");
        System.out.println("isAdmin: " + isAdmin);
        System.out.println("material: " + (material != null ? material.getName() : "null"));

        confirm.showAndWait().ifPresent(response -> {
            if (response == okBtn) {
                try {
                    DataRequest request = new DataRequest();
                    request.put("id", material.getId());

                    DataResponse resp = HttpRequestUtil.request("/api/material/delete", request);

                    if (resp != null && resp.getCode() == 200) {
                        showInfo("删除成功", "物资已删除");
                        loadMaterials();
                    } else {
                        String errorMsg = resp != null ? resp.getMsg() : "网络错误";
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

        VBox mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(30));
        mainContainer.setStyle("-fx-background-color: white;");

        VBox titleBar = new VBox(5);
        Label titleLabel = new Label(material == null ? "新增物资" : "编辑物资");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #4A90E2;");
        Label subtitleLabel = new Label("请填写物资信息");
        subtitleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #999999;");
        titleBar.getChildren().addAll(titleLabel, subtitleLabel);

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(18);

        Label nameLabel = new Label("物资名称:");
        nameLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333;");
        TextField nameField = new TextField(material != null ? material.getName() : "");
        nameField.setPromptText("请输入物资名称");
        nameField.setStyle("-fx-font-size: 14px; -fx-padding: 8 12 8 12; -fx-background-color: #F5F7FA; -fx-border-color: #E0E0E0; -fx-border-width: 1; -fx-border-radius: 5;");
        nameField.setPrefHeight(40);

        Label codeLabel = new Label("物资编码:");
        codeLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333;");
        TextField codeField = new TextField(material != null ? material.getCode() : "");
        codeField.setPromptText("请输入物资编码");
        codeField.setStyle("-fx-font-size: 14px; -fx-padding: 8 12 8 12; -fx-background-color: #F5F7FA; -fx-border-color: #E0E0E0; -fx-border-width: 1; -fx-border-radius: 5;");
        codeField.setPrefHeight(40);

        Label categoryLabel = new Label("所属分类:");
        categoryLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333;");
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
        categoryCombo.setStyle("-fx-font-size: 14px; -fx-padding: 5 10 5 10; -fx-background-color: #F5F7FA; -fx-border-color: #E0E0E0; -fx-border-width: 1; -fx-border-radius: 5;");
        categoryCombo.setPrefHeight(40);

        Label unitLabel = new Label("单位:");
        unitLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333;");
        TextField unitField = new TextField(material != null ? material.getUnit() : "");
        unitField.setPromptText("请输入单位(如:个、件、箱)");
        unitField.setStyle("-fx-font-size: 14px; -fx-padding: 8 12 8 12; -fx-background-color: #F5F7FA; -fx-border-color: #E0E0E0; -fx-border-width: 1; -fx-border-radius: 5;");
        unitField.setPrefHeight(40);

        Label currentStockLabel = new Label("当前库存:");
        currentStockLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333;");
        TextField currentStockField = new TextField(material != null ? String.valueOf(material.getCurrentStock()) : "0");
        currentStockField.setPromptText("当前库存数量");
        currentStockField.setStyle("-fx-font-size: 14px; -fx-padding: 8 12 8 12; -fx-background-color: #F5F7FA; -fx-border-color: #E0E0E0; -fx-border-width: 1; -fx-border-radius: 5;");
        currentStockField.setPrefHeight(40);

        Label safetyStockLabel = new Label("安全库存:");
        safetyStockLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333;");
        TextField safetyStockField = new TextField(material != null ? String.valueOf(material.getSafetyStock()) : "0");
        safetyStockField.setPromptText("安全库存数量");
        safetyStockField.setStyle("-fx-font-size: 14px; -fx-padding: 8 12 8 12; -fx-background-color: #F5F7FA; -fx-border-color: #E0E0E0; -fx-border-width: 1; -fx-border-radius: 5;");
        safetyStockField.setPrefHeight(40);

        Label priceLabel = new Label("单价:");
        priceLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333;");
        TextField priceField = new TextField(material != null && material.getPrice() != null ? material.getPrice().toString() : "0");
        priceField.setPromptText("单价");
        priceField.setStyle("-fx-font-size: 14px; -fx-padding: 8 12 8 12; -fx-background-color: #F5F7FA; -fx-border-color: #E0E0E0; -fx-border-width: 1; -fx-border-radius: 5;");
        priceField.setPrefHeight(40);

        Label statusLabel = new Label("状态:");
        statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333;");
        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("启用", "停用");
        statusCombo.setValue(material != null ? material.getStatus() : "启用");
        statusCombo.setStyle("-fx-font-size: 14px; -fx-padding: 5 10 5 10; -fx-background-color: #F5F7FA; -fx-border-color: #E0E0E0; -fx-border-width: 1; -fx-border-radius: 5;");
        statusCombo.setPrefHeight(40);

        grid.add(nameLabel, 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(codeLabel, 0, 1);
        grid.add(codeField, 1, 1);
        grid.add(categoryLabel, 0, 2);
        grid.add(categoryCombo, 1, 2);
        grid.add(unitLabel, 0, 3);
        grid.add(unitField, 1, 3);
        grid.add(currentStockLabel, 0, 4);
        grid.add(currentStockField, 1, 4);
        grid.add(safetyStockLabel, 0, 5);
        grid.add(safetyStockField, 1, 5);
        grid.add(priceLabel, 0, 6);
        grid.add(priceField, 1, 6);
        grid.add(statusLabel, 0, 7);
        grid.add(statusCombo, 1, 7);

        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        Button saveBtn = new Button("保存");
        saveBtn.setPrefWidth(100);
        saveBtn.setPrefHeight(40);
        saveBtn.setStyle("-fx-background-color: #4A90E2; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5;");

        Button cancelBtn = new Button("取消");
        cancelBtn.setPrefWidth(100);
        cancelBtn.setPrefHeight(40);
        cancelBtn.setStyle("-fx-background-color: #E0E0E0; -fx-text-fill: #666666; -fx-font-size: 14px; -fx-cursor: hand; -fx-background-radius: 5;");

        saveBtn.setOnAction(e -> {
            try {
                String name = nameField.getText().trim();

                if (name.isEmpty()) {
                    showError("验证失败", "物资名称不能为空，请输入物资名称！");
                    nameField.requestFocus();
                    return;
                }

                DataRequest request = new DataRequest();
                request.put("name", name);
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

                String statusValue = statusCombo.getValue();
                int statusCode = "启用".equals(statusValue) ? 1 : 0;
                request.put("status", statusCode);

                if (material != null) {
                    request.put("id", material.getId());
                }

                String url = material == null ? "/api/material/add" : "/api/material/update";

                DataResponse response = HttpRequestUtil.request(url, request);

                if (response != null && response.getCode() == 200) {
                    showInfo("操作成功", material == null ? "物资已创建" : "物资已更新");
                    dialog.close();
                    loadMaterials();
                } else {
                    String errorMsg = response != null ? response.getMsg() : "网络错误";
                    showError("操作失败", "后端返回错误: " + errorMsg);
                }
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "操作物资异常", ex);
                showError("操作异常", ex.getMessage());
            }
        });

        cancelBtn.setOnAction(e -> dialog.close());

        buttonBox.getChildren().addAll(cancelBtn, saveBtn);

        VBox contentBox = new VBox(20, titleBar, grid, buttonBox);
        contentBox.setPadding(new Insets(30));
        contentBox.setStyle("-fx-background-color: white;");

        Scene scene = new Scene(contentBox, 600, 650);
        scene.setFill(javafx.scene.paint.Color.WHITE);
        dialog.setScene(scene);
        dialog.setMinWidth(600);
        dialog.setMinHeight(650);
        dialog.showAndWait();
    }

    private void showInfo(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);

            alert.getDialogPane().setStyle("-fx-background-color: white;");
            alert.getDialogPane().getStylesheets().add(getClass().getResource("/styles/modern-style.css").toExternalForm());

            ButtonType okBtn = new ButtonType("确定", ButtonBar.ButtonData.OK_DONE);
            alert.getButtonTypes().setAll(okBtn);

            Button button = (Button) alert.getDialogPane().lookupButton(okBtn);
            button.setStyle("-fx-background-color: #4A90E2; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5; -fx-padding: 8 20 8 20;");
            button.setDefaultButton(true);

            alert.showAndWait();
        });
    }

    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.getDialogPane().setStyle("-fx-background-color: white;");
            alert.getDialogPane().getStylesheets().add(getClass().getResource("/styles/modern-style.css").toExternalForm());

            ButtonType okBtn = new ButtonType("确定", ButtonBar.ButtonData.OK_DONE);
            alert.getButtonTypes().setAll(okBtn);

            Button button = (Button) alert.getDialogPane().lookupButton(okBtn);
            button.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5; -fx-padding: 8 20 8 20;");
            button.setDefaultButton(true);

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

        @SuppressWarnings("unused")
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
