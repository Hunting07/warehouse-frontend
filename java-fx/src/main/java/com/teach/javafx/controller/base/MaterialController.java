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
import java.util.Optional;

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

    @FXML
    private Label titleLabel;

    @FXML
    private Label subtitleLabel;

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

        // 设置页面说明文字
        if (subtitleLabel != null) {
            subtitleLabel.setText("管理仓库物资信息");
        }
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

    private String generateMaterialCode() {
        LocalDateTime now = LocalDateTime.now();
        int year = now.getYear();
        int month = now.getMonthValue();
        int day = now.getDayOfMonth();
        int hour = now.getHour();
        int minute = now.getMinute();

        return String.format("AW%04d%02d%02d%02d%02d", year, month, day, hour, minute);
    }

    private void setupTable() {
        if (isAdmin) {
            actionCol.setPrefWidth(200);
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
                        setStyle("-fx-text-fill: red; -fx-font-weight: bold; -fx-font-size: 14px;");
                    } else {
                        setStyle("-fx-text-fill: green; -fx-font-size: 14px;");

                    }
                }
            }
        });

        priceCol.setCellValueFactory(param -> param.getValue().priceProperty());
        statusCol.setCellValueFactory(param -> param.getValue().statusProperty());
        createTimeCol.setCellValueFactory(param -> param.getValue().createTimeProperty());

        nameCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(item == null ? "" : item);
                setStyle("-fx-font-size: 14px;");
            }
        });

        codeCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(item == null ? "" : item);
                setStyle("-fx-font-size: 14px;");
            }
        });

        categoryNameCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(item == null ? "" : item);
                setStyle("-fx-font-size: 14px;");
            }
        });

        unitCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(item == null ? "" : item);
                setStyle("-fx-font-size: 14px;");
            }
        });

        currentStockCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setText(item == null ? "" : item.toString());
                setStyle("-fx-font-size: 14px;");
            }
        });

        safetyStockCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setText(item == null ? "" : item.toString());
                setStyle("-fx-font-size: 14px;");
            }
        });

        priceCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(item == null ? "" : item.toString());
                setStyle("-fx-font-size: 14px;");
            }
        });

        statusCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(item == null ? "" : item);
                setStyle("-fx-font-size: 14px;");
            }
        });

        createTimeCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(item == null ? "" : item.toString());
                setStyle("-fx-font-size: 14px;");
            }
        });

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
                            editBtn.setStyle("-fx-background-color: #5CB85C; -fx-text-fill: white; -fx-cursor: hand;");
                            editBtn.setOnAction(e -> {
                                MaterialNode material = getTableView().getItems().get(getIndex());
                                editMaterial(material);
                            });

                            Button deleteBtn = new Button("删除");
                            deleteBtn.setStyle("-fx-background-color: #E57373; -fx-text-fill: white; -fx-cursor: hand;");
                            deleteBtn.setOnAction(e -> {
                                MaterialNode material = getTableView().getItems().get(getIndex());
                                deleteMaterial(material);
                            });

                            javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(5, editBtn, deleteBtn);
                            box.setAlignment(javafx.geometry.Pos.CENTER);
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

        System.out.println("=== 物资管理搜索 ===");
        System.out.println("keyword: [" + keyword + "]");
        System.out.println("category: [" + category + "]");
        System.out.println("stockStatus: [" + stockStatus + "]");
        System.out.println("materialStatus: [" + materialStatus + "]");

        try {
            DataRequest request = new DataRequest();

            System.out.println("发送的请求参数: " + request);

            DataResponse response = HttpRequestUtil.request("/api/material/list", request);

            System.out.println("搜索响应码: " + (response != null ? response.getCode() : "null"));

            if (response != null && response.getCode() == 200) {
                Platform.runLater(() -> {
                    List<Map<String, Object>> allMaterials = gson.fromJson(
                        gson.toJson(response.getData()),
                        new TypeToken<List<Map<String, Object>>>(){}.getType()
                    );

                    System.out.println("后端返回的物资数量: " + (allMaterials != null ? allMaterials.size() : 0));

                    if (allMaterials == null) {
                        buildDataList(null);
                        return;
                    }

                    List<Map<String, Object>> filteredMaterials = new java.util.ArrayList<>();

                    for (Map<String, Object> material : allMaterials) {
                        try {
                            String materialName = (String) material.get("name");
                            String materialCode = (String) material.get("code");
                            String categoryName2 = (String) material.get("categoryName");
                            int currentStock = ((Number) material.get("currentStock")).intValue();
                            int safetyStock = ((Number) material.get("safetyStock")).intValue();

                            Object statusObj = material.get("status");
                            int materialStatusCode = 0;
                            if (statusObj instanceof Number) {
                                materialStatusCode = ((Number) statusObj).intValue();
                            } else if (statusObj != null) {
                                String statusStr = statusObj.toString();
                                materialStatusCode = ("1".equals(statusStr) || "启用".equals(statusStr)) ? 1 : 0;
                            }

                            System.out.println("物资: " + materialName +
                                " | 分类: " + categoryName2 +
                                " | 库存: " + currentStock + "/" + safetyStock +
                                " | 状态码: " + materialStatusCode);

                            boolean matchesKeyword = true;
                            if (keyword != null && !keyword.trim().isEmpty()) {
                                String kw = keyword.trim().toLowerCase();
                                boolean nameMatch = materialName != null && materialName.toLowerCase().contains(kw);
                                boolean codeMatch = materialCode != null && materialCode.toLowerCase().contains(kw);
                                matchesKeyword = nameMatch || codeMatch;
                                System.out.println("  -> 关键字匹配: " + matchesKeyword + " (nameMatch=" + nameMatch + ", codeMatch=" + codeMatch + ")");
                            }

                            boolean matchesCategoryFilter = true;
                            if (category != null && !"全部分类".equals(category)) {
                                matchesCategoryFilter = category.equals(categoryName2);
                                System.out.println("  -> 分类匹配: " + matchesCategoryFilter + " (期望=" + category + ", 实际=" + categoryName2 + ")");
                            }

                            boolean matchesStockFilter = true;
                            if (stockStatus != null && !"全部".equals(stockStatus)) {
                                boolean isWarning = currentStock < safetyStock;

                                if ("预警".equals(stockStatus)) {
                                    matchesStockFilter = isWarning;
                                    System.out.println("  -> 库存预警过滤: " + isWarning);
                                } else if ("正常".equals(stockStatus)) {
                                    matchesStockFilter = !isWarning;
                                    System.out.println("  -> 库存正常过滤: " + !isWarning);
                                }
                            }

                            boolean matchesStatusFilter = true;
                            if (materialStatus != null && !"全部".equals(materialStatus)) {
                                boolean categoryEnabled = true;

                                if (categoryName2 != null && !categoryName2.isEmpty()) {
                                    if (categoryListCache != null) {
                                        for (Map<String, Object> cat : categoryListCache) {
                                            if (categoryName2.equals(cat.get("name"))) {
                                                Object catStatusObj = cat.get("status");
                                                if (catStatusObj instanceof Number) {
                                                    categoryEnabled = ((Number) catStatusObj).intValue() == 1;
                                                } else if (catStatusObj != null) {
                                                    String catStatusStr = catStatusObj.toString();
                                                    categoryEnabled = "1".equals(catStatusStr) || "启用".equals(catStatusStr);
                                                }
                                                break;
                                            }
                                        }
                                    }
                                }

                                if ("启用".equals(materialStatus)) {
                                    if (categoryName2 == null || categoryName2.isEmpty()) {
                                        matchesStatusFilter = (materialStatusCode == 1);
                                    } else {
                                        matchesStatusFilter = categoryEnabled && (materialStatusCode == 1);
                                    }
                                    System.out.println("  -> 启用状态过滤: 分类启用=" + categoryEnabled + ", 物资启用=" + (materialStatusCode == 1) + ", 结果=" + matchesStatusFilter);
                                } else if ("停用".equals(materialStatus)) {
                                    if (categoryName2 == null || categoryName2.isEmpty()) {
                                        matchesStatusFilter = (materialStatusCode == 0);
                                    } else {
                                        matchesStatusFilter = !categoryEnabled || (materialStatusCode == 0);
                                    }
                                    System.out.println("  -> 停用状态过滤: 分类启用=" + categoryEnabled + ", 物资停用=" + (materialStatusCode == 0) + ", 结果=" + matchesStatusFilter);
                                }
                            }

                            boolean passAll = matchesKeyword && matchesCategoryFilter && matchesStockFilter && matchesStatusFilter;
                            System.out.println("  -> 关键字: " + matchesKeyword +
                                " | 分类: " + matchesCategoryFilter +
                                " | 库存: " + matchesStockFilter +
                                " | 状态: " + matchesStatusFilter +
                                " | 总结果: " + passAll);

                            if (passAll) {
                                filteredMaterials.add(material);
                                System.out.println("  -> ✓ 通过筛选");
                            } else {
                                System.out.println("  -> ✗ 未通过筛选");
                            }
                        } catch (Exception e) {
                            logger.log(Level.WARNING, "过滤物资数据失败", e);
                        }
                    }

                    System.out.println("最终过滤后的物资数量: " + filteredMaterials.size());
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

        Stage confirmDialog = new Stage();
        confirmDialog.initModality(Modality.APPLICATION_MODAL);
        confirmDialog.setTitle("确认删除");

        VBox mainBox = new VBox(30);
        mainBox.setPadding(new Insets(40, 40, 30, 40));
        mainBox.setAlignment(javafx.geometry.Pos.CENTER);
        mainBox.setStyle("-fx-background-color: white;");

        VBox contentBox = new VBox(15);
        contentBox.setAlignment(javafx.geometry.Pos.CENTER);

        Label titleLabel = new Label("确定要删除物资 \"" + material.getName() + "\" 吗？");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");

        Label messageLabel = new Label("删除后无法恢复！");
        messageLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #4a5568; -fx-wrap-text: true; -fx-alignment: center;");
        messageLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        messageLabel.setMaxWidth(400);

        contentBox.getChildren().addAll(titleLabel, messageLabel);

        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));

        Button okBtn = new Button("确定");
        okBtn.setPrefWidth(120);
        okBtn.setPrefHeight(45);
        okBtn.setStyle("-fx-background-color: #E57373; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 10;");

        Button cancelBtn = new Button("取消");
        cancelBtn.setPrefWidth(120);
        cancelBtn.setPrefHeight(45);
        cancelBtn.setStyle("-fx-background-color: #E0E0E0; -fx-text-fill: #666666; -fx-font-size: 16px; -fx-cursor: hand; -fx-background-radius: 10;");

        final boolean[] confirmed = {false};

        okBtn.setOnAction(e -> {
            confirmed[0] = true;
            confirmDialog.close();
        });

        cancelBtn.setOnAction(e -> confirmDialog.close());

        buttonBox.getChildren().addAll(okBtn, cancelBtn);

        mainBox.getChildren().addAll(contentBox, buttonBox);

        Scene scene = new Scene(mainBox, 500, 280);
        scene.setFill(javafx.scene.paint.Color.WHITE);
        confirmDialog.setScene(scene);

        confirmDialog.showAndWait();

        if (confirmed[0]) {
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

        final String initialCode = material != null ? material.getCode() : generateMaterialCode();
        Label codeValueLabel = new Label(initialCode);
        codeValueLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #000000; -fx-padding: 8 0 8 0;");

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
        grid.add(codeValueLabel, 1, 1);
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
                    showError("新增失败", "物资名称不能为空，请输入物资名称！");
                    nameField.requestFocus();
                    return;
                }

                if (material == null) {
                    DataRequest checkRequest = new DataRequest();
                    DataResponse checkResponse = HttpRequestUtil.request("/api/material/list", checkRequest);

                    if (checkResponse != null && checkResponse.getCode() == 200 && checkResponse.getData() != null) {
                        Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
                        List<Map<String, Object>> materialList = gson.fromJson(gson.toJson(checkResponse.getData()), listType);

                        if (materialList != null) {
                            for (Map<String, Object> m : materialList) {
                                String existingName = (String) m.get("name");
                                if (name.equals(existingName)) {
                                    showError("新增失败", "物资名称已存在，请使用其他名称！");
                                    nameField.requestFocus();
                                    return;
                                }
                            }
                        }
                    }
                }

                DataRequest request = new DataRequest();
                request.put("name", name);
                request.put("code", initialCode);

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
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle(title);

            VBox mainBox = new VBox(30);
            mainBox.setPadding(new Insets(40, 40, 30, 40));
            mainBox.setAlignment(javafx.geometry.Pos.CENTER);
            mainBox.setStyle("-fx-background-color: white;");

            VBox contentBox = new VBox(15);
            contentBox.setAlignment(javafx.geometry.Pos.CENTER);

            Label titleLabel = new Label(title);
            titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");

            Label messageLabel = new Label(message);
            messageLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #4a5568; -fx-wrap-text: true; -fx-alignment: center;");
            messageLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            messageLabel.setMaxWidth(400);

            contentBox.getChildren().addAll(titleLabel, messageLabel);

            HBox buttonBox = new HBox(20);
            buttonBox.setAlignment(javafx.geometry.Pos.CENTER);
            buttonBox.setPadding(new Insets(10, 0, 0, 0));

            Button okBtn = new Button("确定");
            okBtn.setPrefWidth(120);
            okBtn.setPrefHeight(45);
            okBtn.setStyle("-fx-background-color: #4A90E2; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 10;");
            okBtn.setOnAction(e -> dialog.close());

            buttonBox.getChildren().add(okBtn);

            mainBox.getChildren().addAll(contentBox, buttonBox);

            Scene scene = new Scene(mainBox, 500, 250);
            scene.setFill(javafx.scene.paint.Color.WHITE);
            dialog.setScene(scene);
            dialog.showAndWait();
        });
    }

    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle(title);

            VBox mainBox = new VBox(30);
            mainBox.setPadding(new Insets(40, 40, 30, 40));
            mainBox.setAlignment(javafx.geometry.Pos.CENTER);
            mainBox.setStyle("-fx-background-color: white;");

            VBox contentBox = new VBox(15);
            contentBox.setAlignment(javafx.geometry.Pos.CENTER);

            Label titleLabel = new Label(title);
            titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");

            Label messageLabel = new Label(message);
            messageLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #4a5568; -fx-wrap-text: true; -fx-alignment: center;");
            messageLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            messageLabel.setMaxWidth(400);

            contentBox.getChildren().addAll(titleLabel, messageLabel);

            HBox buttonBox = new HBox(20);
            buttonBox.setAlignment(javafx.geometry.Pos.CENTER);
            buttonBox.setPadding(new Insets(10, 0, 0, 0));

            Button okBtn = new Button("确定");
            okBtn.setPrefWidth(120);
            okBtn.setPrefHeight(45);
            okBtn.setStyle("-fx-background-color: #E57373; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 10;");
            okBtn.setOnAction(e -> dialog.close());

            buttonBox.getChildren().add(okBtn);

            mainBox.getChildren().addAll(contentBox, buttonBox);

            Scene scene = new Scene(mainBox, 500, 250);
            scene.setFill(javafx.scene.paint.Color.WHITE);
            dialog.setScene(scene);
            dialog.showAndWait();
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
