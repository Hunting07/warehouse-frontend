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
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Optional;

public class CategoryController extends ToolController {
    private static final Logger logger = Logger.getLogger(CategoryController.class.getName());

    @FXML
    private TreeTableView<CategoryNode> categoryTreeTable;

    @FXML
    private TreeTableColumn<CategoryNode, Integer> idCol;
    @FXML
    private TreeTableColumn<CategoryNode, String> nameCol;
    @FXML
    private TreeTableColumn<CategoryNode, String> codeCol;
    @FXML
    private TreeTableColumn<CategoryNode, String> statusCol;
    @FXML
    private TreeTableColumn<CategoryNode, LocalDateTime> createTimeCol;
    @FXML
    private TreeTableColumn<CategoryNode, Integer> materialCountCol;
    @FXML
    private TreeTableColumn<CategoryNode, Void> actionCol;

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> statusFilter;
    @FXML
    private Button addCategoryBtn;

    @FXML
    private Label titleLabel;

    @FXML
    private Label subtitleLabel;

    private final Gson gson = new Gson();

    private boolean isAdmin = false;

    @FXML
    public void initialize() {
        checkUserRole();
        setupTreeTable();
        setupFilters();
        applyRolePermissions();
        loadCategoryTree();

        // 设置页面说明文字
        if (subtitleLabel != null) {
            subtitleLabel.setText("管理和维护物资分类信息");
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
            if (addCategoryBtn != null) {
                addCategoryBtn.setVisible(false);
                addCategoryBtn.setManaged(false);
            }
        }
    }
    private void setupTreeTable() {
        idCol.setCellFactory(col -> new TreeTableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    int rowIndex = getIndex() + 1;
                    setText(String.valueOf(rowIndex));
                }
            }
        });
        nameCol.setCellValueFactory(param -> param.getValue().getValue().nameProperty());
        codeCol.setCellValueFactory(param -> param.getValue().getValue().codeProperty());
        statusCol.setCellValueFactory(param -> param.getValue().getValue().statusProperty());
        materialCountCol.setCellValueFactory(param -> param.getValue().getValue().materialCountProperty().asObject());
        createTimeCol.setCellValueFactory(param -> param.getValue().getValue().createTimeProperty());

        nameCol.setCellFactory(column -> new TreeTableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(item == null ? "" : item);
                setStyle("-fx-font-size: 14px;");
            }
        });

        codeCol.setCellFactory(column -> new TreeTableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(item == null ? "" : item);
                setStyle("-fx-font-size: 14px;");
            }
        });

        statusCol.setCellFactory(column -> new TreeTableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(item == null ? "" : item);
                setStyle("-fx-font-size: 14px;");
            }
        });

        materialCountCol.setCellFactory(column -> new TreeTableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setText(item == null ? "" : item.toString());
                setStyle("-fx-font-size: 14px;");
            }
        });

        createTimeCol.setCellFactory(column -> new TreeTableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(item == null ? "" : item.toString());
                setStyle("-fx-font-size: 14px;");
            }
        });

        actionCol.setCellFactory(new Callback<>() {
            @Override
            public TreeTableCell<CategoryNode, Void> call(TreeTableColumn<CategoryNode, Void> param) {
                return new TreeTableCell<>() {
                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            Button viewMaterialsBtn = new Button("查看物资");
                            viewMaterialsBtn.setStyle("-fx-background-color: #4A90E2; -fx-text-fill: white; -fx-cursor: hand;");
                            viewMaterialsBtn.setOnAction(e -> {
                                TreeItem<CategoryNode> treeItem = getTreeTableView().getTreeItem(getIndex());
                                if (treeItem != null) {
                                    CategoryNode node = treeItem.getValue();
                                    viewMaterialsInCategory(node);
                                }
                            });

                            if (isAdmin) {
                                Button editBtn = new Button("编辑");
                                editBtn.setStyle("-fx-background-color: #5CB85C; -fx-text-fill: white; -fx-cursor: hand;");
                                editBtn.setOnAction(e -> {
                                    TreeItem<CategoryNode> treeItem = getTreeTableView().getTreeItem(getIndex());
                                    if (treeItem != null) {
                                        CategoryNode node = treeItem.getValue();
                                        showEditDialog(node);
                                    }
                                });

                                Button deleteBtn = new Button("删除");
                                deleteBtn.setStyle("-fx-background-color: #E57373; -fx-text-fill: white; -fx-cursor: hand;");
                                deleteBtn.setOnAction(e -> {
                                    TreeItem<CategoryNode> treeItem = getTreeTableView().getTreeItem(getIndex());
                                    if (treeItem != null) {
                                        CategoryNode node = treeItem.getValue();
                                        deleteCategory(node);
                                    }
                                });

                                javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(5, viewMaterialsBtn, editBtn, deleteBtn);
                                box.setAlignment(javafx.geometry.Pos.CENTER);
                                box.setPadding(new Insets(5));
                                setGraphic(box);
                            } else {
                                javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(viewMaterialsBtn);
                                box.setAlignment(javafx.geometry.Pos.CENTER);
                                box.setPadding(new Insets(5));
                                setGraphic(box);
                            }
                        }
                    }
                };
            }
        });

        TreeItem<CategoryNode> root = new TreeItem<>(new CategoryNode());
        root.setExpanded(true);
        categoryTreeTable.setRoot(root);
        categoryTreeTable.setShowRoot(false);

        categoryTreeTable.setRowFactory(tv -> new TreeTableRow<>() {
            @Override
            protected void updateItem(CategoryNode item, boolean empty) {
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
        searchField.setPromptText("输入分类名称搜索...");
        if (statusFilter != null) {
            statusFilter.getItems().addAll("全部", "启用", "禁用");
            statusFilter.setValue("全部");
        }
    }

    @FXML
    private void loadCategoryTree() {
        try {
            DataRequest request = new DataRequest();
            DataResponse response = HttpRequestUtil.request("/api/category/tree", request);

            if (response != null && response.getCode() == 200) {
                Platform.runLater(() -> buildTreeFromData(response.getData()));
            } else {
                showError("加载失败", response != null ? response.getMsg() : "网络错误");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "加载分类树异常", e);
            showError("加载异常", e.getMessage());
        }
    }

    private void buildTreeFromData(Object data) {
        Platform.runLater(() -> {
            categoryTreeTable.getRoot().getChildren().clear();

            if (data == null) {
                return;
            }

            Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
            List<Map<String, Object>> categoryList = gson.fromJson(gson.toJson(data), listType);

            if (categoryList != null) {
                for (Map<String, Object> category : categoryList) {
                    CategoryNode node = mapToCategoryNode(category);
                    TreeItem<CategoryNode> treeItem = new TreeItem<>(node);
                    categoryTreeTable.getRoot().getChildren().add(treeItem);
                }
            }

            categoryTreeTable.refresh();
        });
    }

    private CategoryNode mapToCategoryNode(Map<String, Object> map) {
        CategoryNode node = new CategoryNode();

        if (map.get("id") != null) {
            node.setId(((Number) map.get("id")).intValue());
        }
        if (map.get("name") != null) {
            node.setName((String) map.get("name"));
        }
        if (map.get("code") != null) {
            node.setCode((String) map.get("code"));
        } else {}
        if (map.get("sort") != null) {
            node.setSort(((Number) map.get("sort")).intValue());
        }
        if (map.get("status") != null) {
            Object statusObj = map.get("status");
            String statusText;
            if (statusObj instanceof Number) {
                int statusValue = ((Number) statusObj).intValue();
                statusText = (statusValue == 1) ? "启用" : "禁用";
            } else {
                String statusStr = statusObj.toString();
                statusText = ("1".equals(statusStr) || "启用".equals(statusStr)) ? "启用" : "禁用";
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
        if (map.get("materialCount") != null) {
            Object countObj = map.get("materialCount");
            if (countObj instanceof Number) {
                node.setMaterialCount(((Number) countObj).intValue());
            }
        }

        return node;
    }

    @FXML
    private void searchCategory() {
        String keyword = searchField.getText();
        String status = statusFilter != null ? statusFilter.getValue() : "全部";

        try {
            DataRequest request = new DataRequest();

            DataResponse response = HttpRequestUtil.request("/api/category/tree", request);

            if (response != null && response.getCode() == 200) {
                Platform.runLater(() -> {
                    Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
                    List<Map<String, Object>> allCategories = gson.fromJson(
                        gson.toJson(response.getData()),
                        listType
                    );

                    if (allCategories == null) {
                        buildTreeFromData(null);
                        return;
                    }

                    List<Map<String, Object>> filteredCategories = new java.util.ArrayList<>();

                    for (Map<String, Object> category : allCategories) {
                        String categoryName = (String) category.get("name");
                        String categoryCode = (String) category.get("code");

                        Object statusObj = category.get("status");
                        int statusCode = 0;
                        if (statusObj instanceof Number) {
                            statusCode = ((Number) statusObj).intValue();
                        } else if (statusObj != null) {
                            String statusStr = statusObj.toString();
                            statusCode = ("1".equals(statusStr) || "启用".equals(statusStr)) ? 1 : 0;
                        }
                        String statusText = (statusCode == 1) ? "启用" : "禁用";

                        boolean matchesKeyword = true;
                        if (keyword != null && !keyword.trim().isEmpty()) {
                            String kw = keyword.trim().toLowerCase();
                            boolean nameMatch = categoryName != null && categoryName.toLowerCase().contains(kw);
                            boolean codeMatch = categoryCode != null && categoryCode.toLowerCase().contains(kw);
                            matchesKeyword = nameMatch || codeMatch;
                        }

                        boolean matchesStatusFilter = true;
                        if (status != null && !"全部".equals(status)) {
                            if ("启用".equals(status)) {
                                matchesStatusFilter = (statusCode == 1);
                            } else if ("禁用".equals(status)) {
                                matchesStatusFilter = (statusCode == 0);
                            }
                        }

                        if (matchesKeyword && matchesStatusFilter) {
                            filteredCategories.add(category);
                        } else {}
                    }

                    buildTreeFromData(filteredCategories);
                });
            } else {
                String errorMsg = response != null ? response.getMsg() : "网络错误";
                showError("搜索失败", "后端返回错误: " + errorMsg);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "搜索分类异常", e);
            showError("搜索异常", e.getMessage());
        }
    }

    @FXML
    private void resetSearch() {
        searchField.clear();
        if (statusFilter != null) {
            statusFilter.setValue("全部");
        }
        loadCategoryTree();
    }

    @FXML
    private void refreshData() {
        searchField.clear();
        if (statusFilter != null) {
            statusFilter.setValue("全部");
        }
        loadCategoryTree();
    }

    @FXML
    private void addCategory() {
        if (!isAdmin) {
            showError("权限不足", "只有管理员可以新增分类");
            return;
        }
        showEditDialog(null);
    }

    private void deleteCategory(CategoryNode node) {
        if (!isAdmin) {
            showError("权限不足", "只有管理员可以删除分类");
            return;
        }
        if (node == null) {
            showError("删除失败", "请选择要删除的分类");
            return;
        }

        try {
            DataRequest checkRequest = new DataRequest();

            DataResponse checkResponse = HttpRequestUtil.request("/api/material/list", checkRequest);

            if (checkResponse != null && checkResponse.getCode() == 200 && checkResponse.getData() != null) {
                Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
                List<Map<String, Object>> materialList = gson.fromJson(gson.toJson(checkResponse.getData()), listType);

                int materialCount = 0;
                if (materialList != null) {
                    for (Map<String, Object> material : materialList) {
                        String categoryName = (String) material.get("categoryName");
                        if (node.getName().equals(categoryName)) {
                            materialCount++;
                        }
                    }
                }

                if (materialCount > 0) {
                    showError("删除失败", "该分类下有 " + materialCount + " 个物资，无法删除！请先删除或转移这些物资。");
                    return;
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "检查分类物资异常", e);
            showError("检查失败", "无法检查分类下的物资: " + e.getMessage());
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

        Label titleLabel = new Label("确定要删除分类 \"" + node.getName() + "\" 吗？");
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
                request.put("id", node.getId());

                DataResponse resp = HttpRequestUtil.request("/api/category/delete", request);

                if (resp != null && resp.getCode() == 200) {
                    showInfo("删除成功", "分类已删除");
                    loadCategoryTree();
                } else {
                    String errorMsg = resp != null ? resp.getMsg() : "网络错误";
                    showError("删除失败", "后端返回错误: " + errorMsg);
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "删除分类异常", e);
                showError("删除异常", e.getMessage());
            }
        }
    }

    private void viewMaterialsInCategory(CategoryNode category) {
        if (category == null || category.getId() == 0) {
            showError("查看失败", "请选择有效的分类");
            return;
        }

        try {
            DataRequest request = new DataRequest();

            DataResponse response = HttpRequestUtil.request("/api/material/list", request);

            if (response != null && response.getCode() == 200) {
                List<Map<String, Object>> allMaterials = gson.fromJson(
                    gson.toJson(response.getData()),
                    new TypeToken<List<Map<String, Object>>>(){}.getType()
                );

                List<Map<String, Object>> filteredMaterials = new java.util.ArrayList<>();
                if (allMaterials != null) {
                    for (Map<String, Object> material : allMaterials) {
                        String categoryName = (String) material.get("categoryName");
                        if (category.getName().equals(categoryName)) {
                            filteredMaterials.add(material);
                        }
                    }
                }

                showMaterialsDialog(category.getName(), filteredMaterials);
            } else {
                String errorMsg = response != null ? response.getMsg() : "网络错误";
                showError("加载失败", "后端返回错误: " + errorMsg);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "加载物资异常", e);
            showError("加载异常", e.getMessage());
        }
    }

    private void showMaterialsDialog(String categoryName, Object data) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("物资列表");

        VBox mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(30));
        mainContainer.setStyle("-fx-background-color: white;");

        VBox titleBar = new VBox(5);
        Label titleLabel = new Label("分类 [" + categoryName + "] 下的物资");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #4A90E2;");
        Label subtitleLabel = new Label("查看该分类下的所有物资信息");
        subtitleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #999999;");
        titleBar.getChildren().addAll(titleLabel, subtitleLabel);

        TableView<MaterialNode> materialTable = new TableView<>();
        materialTable.setStyle("-fx-border-color: #E0E0E0; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-color: white;");
        materialTable.getStylesheets().add(getClass().getResource("/styles/modern-style.css").toExternalForm());

        materialTable.lookupAll(".column-header-background").forEach(node -> {
            node.setStyle("-fx-background-color: #4A90E2;");
        });

        materialTable.lookupAll(".column-header .label").forEach(node -> {
            node.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");
        });

        TableColumn<MaterialNode, Integer> idCol = new TableColumn<>("编号");
        idCol.setCellValueFactory(param -> {
            int index = materialTable.getItems().indexOf(param.getValue()) + 1;
            return new SimpleIntegerProperty(index).asObject();
        });
        idCol.setPrefWidth(60);
        idCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<MaterialNode, String> nameCol = new TableColumn<>("物资名称");
        nameCol.setCellValueFactory(param -> param.getValue().nameProperty());
        nameCol.setPrefWidth(150);

        TableColumn<MaterialNode, String> codeCol = new TableColumn<>("物资编码");
        codeCol.setCellValueFactory(param -> param.getValue().codeProperty());
        codeCol.setPrefWidth(120);

        TableColumn<MaterialNode, String> unitCol = new TableColumn<>("单位");
        unitCol.setCellValueFactory(param -> param.getValue().unitProperty());
        unitCol.setPrefWidth(80);
        unitCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<MaterialNode, Integer> currentStockCol = new TableColumn<>("当前库存");
        currentStockCol.setCellValueFactory(param -> param.getValue().currentStockProperty().asObject());
        currentStockCol.setPrefWidth(100);
        currentStockCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<MaterialNode, Integer> safetyStockCol = new TableColumn<>("安全库存");
        safetyStockCol.setCellValueFactory(param -> param.getValue().safetyStockProperty().asObject());
        safetyStockCol.setPrefWidth(100);
        safetyStockCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<MaterialNode, String> warningCol = new TableColumn<>("库存状态");
        warningCol.setCellValueFactory(param -> {
            MaterialNode material = param.getValue();
            int currentStock = material.getCurrentStock();
            int safetyStock = material.getSafetyStock();

            if (currentStock < safetyStock) {
                return new SimpleStringProperty("预警");
            } else {
                return new SimpleStringProperty("正常");
            }
        });
        warningCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("预警".equals(item)) {
                        setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                    }
                }
            }
        });
        warningCol.setPrefWidth(100);
        warningCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<MaterialNode, String> statusCol = new TableColumn<>("状态");
        statusCol.setCellValueFactory(param -> param.getValue().statusProperty());
        statusCol.setPrefWidth(80);
        statusCol.setStyle("-fx-alignment: CENTER;");

        @SuppressWarnings("unchecked")
        TableColumn<MaterialNode, ?>[] columns = new TableColumn[]{idCol, nameCol, codeCol, unitCol, currentStockCol, safetyStockCol, warningCol, statusCol};
        materialTable.getColumns().addAll(columns);

        materialTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        materialTable.setRowFactory(tv -> {
            TableRow<MaterialNode> row = new TableRow<>();
            row.setStyle("-fx-background-color: white;");
            row.hoverProperty().addListener((obs, wasHovered, isNowHovered) -> {
                if (isNowHovered && !row.isEmpty()) {
                    row.setStyle("-fx-background-color: #F5F9FF;");
                } else {
                    row.setStyle("-fx-background-color: white;");
                }
            });
            return row;
        });

        List<Map<String, Object>> materialList = gson.fromJson(
            gson.toJson(data),
            new TypeToken<List<Map<String, Object>>>(){}.getType()
        );

        if (materialList != null) {
            for (Map<String, Object> material : materialList) {
                try {
                    MaterialNode node = mapToMaterialNode(material);
                    if (node != null) {
                        materialTable.getItems().add(node);
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING, "转换物资数据失败", e);
                }
            }
        }

        Button closeBtn = new Button("关闭");
        closeBtn.setPrefWidth(100);
        closeBtn.setPrefHeight(40);
        closeBtn.setStyle("-fx-background-color: #E0E0E0; -fx-text-fill: #666666; -fx-font-size: 14px; -fx-cursor: hand; -fx-background-radius: 5;");
        closeBtn.setOnAction(e -> dialog.close());

        HBox buttonBox = new HBox(closeBtn);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        VBox contentBox = new VBox(20, titleBar, materialTable, buttonBox);
        contentBox.setPadding(new Insets(30));
        contentBox.setStyle("-fx-background-color: white;");

        Scene scene = new Scene(contentBox, 950, 550);
        scene.setFill(javafx.scene.paint.Color.WHITE);
        dialog.setScene(scene);
        dialog.setMinWidth(950);
        dialog.setMinHeight(550);
        dialog.showAndWait();
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
                    java.math.BigDecimal price;
                    if (priceObj instanceof java.math.BigDecimal) {
                        price = (java.math.BigDecimal) priceObj;
                    } else if (priceObj instanceof Double) {
                        price = java.math.BigDecimal.valueOf((Double) priceObj);
                    } else if (priceObj instanceof Number) {
                        price = java.math.BigDecimal.valueOf(((Number) priceObj).doubleValue());
                    } else {
                        price = new java.math.BigDecimal(priceObj.toString());
                    }
                    node.setPrice(price);
                } catch (Exception e) {
                    node.setPrice(java.math.BigDecimal.ZERO);
                }
            }
            if (map.get("status") != null) {
            Object statusObj = map.get("status");
            String statusText;
            if (statusObj instanceof Number number) {
                statusText = (number.intValue() == 1) ? "启用" : "禁用";
            } else {
                String statusStr = statusObj.toString();
                statusText = ("1".equals(statusStr) || "启用".equals(statusStr)) ? "启用" : "禁用";
            }
            node.setStatus(statusText);
        }

        } catch (Exception e) {
            logger.log(Level.WARNING, "转换物资数据失败", e);
            return null;
        }

        return node;
    }

    private static class MaterialNode {
        private final IntegerProperty id = new SimpleIntegerProperty();
        private final StringProperty name = new SimpleStringProperty("");
        private final StringProperty code = new SimpleStringProperty("");
        private final IntegerProperty categoryId = new SimpleIntegerProperty();
        private final StringProperty categoryName = new SimpleStringProperty("");
        private final StringProperty unit = new SimpleStringProperty("");
        private final IntegerProperty currentStock = new SimpleIntegerProperty();
        private final IntegerProperty safetyStock = new SimpleIntegerProperty();
        private final ObjectProperty<java.math.BigDecimal> price = new SimpleObjectProperty<>(java.math.BigDecimal.ZERO);
        private final StringProperty status = new SimpleStringProperty("启用");

        @SuppressWarnings("unused")
        public IntegerProperty getIdProperty() { return id; }
        public void setId(int value) { id.set(value); }

        public StringProperty nameProperty() { return name; }
        public void setName(String value) { name.set(value); }

        public StringProperty codeProperty() { return code; }
        public void setCode(String value) { code.set(value); }

        @SuppressWarnings("unused")
        public IntegerProperty categoryIdProperty() { return categoryId; }
        public void setCategoryId(int value) { categoryId.set(value); }

        @SuppressWarnings("unused")
        public StringProperty categoryNameProperty() { return categoryName; }
        public void setCategoryName(String value) { categoryName.set(value); }

        public StringProperty unitProperty() { return unit; }
        public void setUnit(String value) { unit.set(value); }

        public IntegerProperty currentStockProperty() { return currentStock; }
        public int getCurrentStock() { return currentStock.get(); }
        public void setCurrentStock(int value) { currentStock.set(value); }

        public IntegerProperty safetyStockProperty() { return safetyStock; }
        public int getSafetyStock() { return safetyStock.get(); }
        public void setSafetyStock(int value) { safetyStock.set(value); }

        @SuppressWarnings("unused")
        public ObjectProperty<java.math.BigDecimal> priceProperty() { return price; }
        public void setPrice(java.math.BigDecimal value) { price.set(value); }

        public StringProperty statusProperty() { return status; }
        public void setStatus(String value) { status.set(value); }


    }

    private void showEditDialog(CategoryNode node) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(node == null ? "新增分类" : "编辑分类");

        VBox mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(30));
        mainContainer.setStyle("-fx-background-color: white;");

        VBox titleBar = new VBox(5);
        Label titleLabel = new Label(node == null ? "新增分类" : "编辑分类");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #4A90E2;");
        Label subtitleLabel = new Label("请填写分类信息");
        subtitleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #999999;");
        titleBar.getChildren().addAll(titleLabel, subtitleLabel);

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(18);

        Label nameLabel = new Label("分类名称:");
        nameLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333;");
        TextField nameField = new TextField(node != null ? node.getName() : "");
        nameField.setPromptText("请输入分类名称");
        nameField.setStyle("-fx-font-size: 14px; -fx-padding: 8 12 8 12; -fx-background-color: #F5F7FA; -fx-border-color: #E0E0E0; -fx-border-width: 1; -fx-border-radius: 5;");
        nameField.setPrefHeight(40);

        Label statusLabel = new Label("状态:");
        statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333;");
        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("启用", "禁用");
        statusCombo.setValue(node != null ? node.getStatus() : "启用");
        statusCombo.setStyle("-fx-font-size: 14px; -fx-padding: 5 10 5 10; -fx-background-color: #F5F7FA; -fx-border-color: #E0E0E0; -fx-border-width: 1; -fx-border-radius: 5;");
        statusCombo.setPrefHeight(40);

        grid.add(nameLabel, 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(statusLabel, 0, 1);
        grid.add(statusCombo, 1, 1);

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
                    showError(node == null ? "新增失败" : "编辑失败", "分类名称不能为空，请输入分类名称！");
                    nameField.requestFocus();
                    return;
                }

                if (node == null) {
                    boolean nameExists = false;

                    ObservableList<TreeItem<CategoryNode>> rootChildren = categoryTreeTable.getRoot().getChildren();
                    for (TreeItem<CategoryNode> treeItem : rootChildren) {
                        CategoryNode existingNode = treeItem.getValue();
                        if (existingNode != null && name.equals(existingNode.getName())) {
                            nameExists = true;
                            break;
                        }
                    }

                    if (nameExists) {
                        showError("新增失败", "分类名称已存在，请使用其他名称！");
                        nameField.requestFocus();
                        return;
                    }
                } else {
                    boolean nameExists = false;

                    ObservableList<TreeItem<CategoryNode>> rootChildren = categoryTreeTable.getRoot().getChildren();
                    for (TreeItem<CategoryNode> treeItem : rootChildren) {
                        CategoryNode existingNode = treeItem.getValue();
                        if (existingNode != null && existingNode.getId() != node.getId() && name.equals(existingNode.getName())) {
                            nameExists = true;
                            break;
                        }
                    }

                    if (nameExists) {
                        showError("编辑失败", "分类名称已存在，请使用其他名称！");
                        nameField.requestFocus();
                        return;
                    }
                }

                DataRequest request = new DataRequest();
                request.put("name", name);

                if (node == null) {
                    String code = generateCategoryCode();
                    request.put("code", code);
                }

                String statusValue = statusCombo.getValue();
                int statusCode = "启用".equals(statusValue) ? 1 : 0;
                request.put("status", statusCode);

                if (node != null) {
                    request.put("id", node.getId());
                }

                String url = node == null ? "/api/category/add" : "/api/category/update";

                DataResponse response = HttpRequestUtil.request(url, request);

                if (response != null && response.getCode() == 200) {
                    showInfo("操作成功", node == null ? "分类已创建" : "分类已更新");
                    dialog.close();
                    loadCategoryTree();
                } else {
                    String errorMsg = response != null ? response.getMsg() : "网络错误";
                    showError("操作失败", "后端返回错误: " + errorMsg);
                }
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "操作分类异常", ex);
                showError("操作异常", ex.getMessage());
            }
        });


        cancelBtn.setOnAction(e -> dialog.close());

        buttonBox.getChildren().addAll(cancelBtn, saveBtn);

        mainContainer.getChildren().addAll(titleBar, grid, buttonBox);

        Scene scene = new Scene(mainContainer, 500, 280);
        scene.setFill(javafx.scene.paint.Color.WHITE);
        dialog.setScene(scene);
        dialog.setMinWidth(500);
        dialog.setMinHeight(280);
        dialog.showAndWait();
    }
    private String generateCategoryCode() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        int year = now.getYear();
        int month = now.getMonthValue();
        int day = now.getDayOfMonth();
        int hour = now.getHour();
        int minute = now.getMinute();

        return String.format("WZ%04d%02d%02d%02d%02d", year, month, day, hour, minute);
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
        loadCategoryTree();
    }

    @Override
    public void doNew() {
        addCategory();
    }

    @Override
    @SuppressWarnings({"unused", "RedundantSuppression"})
    public void doSave() {
        // 分类保存通过编辑对话框完成，此方法暂不使用
    }

    @Override
    @SuppressWarnings({"unused", "RedundantSuppression"})
    public void doDelete() {
        // 分类删除通过操作列按钮完成，此方法暂不使用
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
        loadCategoryTree();
    }

    public static class CategoryNode {
        private final IntegerProperty id;
        private final StringProperty name;
        private final StringProperty code;
        private final IntegerProperty sort;
        private final StringProperty status;
        private final ObjectProperty<LocalDateTime> createTime;
        private final IntegerProperty materialCount;


        public CategoryNode() {
            this.id = new SimpleIntegerProperty(0);
            this.name = new SimpleStringProperty("");
            this.code = new SimpleStringProperty("");
            this.sort = new SimpleIntegerProperty(0);
            this.status = new SimpleStringProperty("");
            this.createTime = new SimpleObjectProperty<>(null);
            this.materialCount = new SimpleIntegerProperty(0);
        }

        public int getId() {
            return id.get();
        }

        public void setId(int value) {
            id.set(value);
        }

        public IntegerProperty idProperty() {
            return id;
        }

        public String getName() {
            return name.get();
        }

        public void setName(String value) {
            name.set(value);
        }

        public StringProperty nameProperty() {
            return name;
        }

        public String getCode() {
            return code.get();
        }

        public void setCode(String value) {
            code.set(value);
        }

        public StringProperty codeProperty() {
            return code;
        }

        @SuppressWarnings("unused")
        public int getSort() {
            return sort.get();
        }

        public void setSort(int value) {
            sort.set(value);
        }

        @SuppressWarnings("unused")
        public IntegerProperty sortProperty() {
            return sort;
        }

        public String getStatus() {
            return status.get();
        }

        public void setStatus(String value) {
            status.set(value);
        }

        public StringProperty statusProperty() {
            return status;
        }

        @SuppressWarnings("unused")
        public LocalDateTime getCreateTime() {
            return createTime.get();
        }

        public void setCreateTime(LocalDateTime value) {
            createTime.set(value);
        }

        public ObjectProperty<LocalDateTime> createTimeProperty() {
            return createTime;
        }

        @SuppressWarnings("unused")
        public int getMaterialCount() { return materialCount.get(); }

        public void setMaterialCount(int value) { materialCount.set(value); }

        @SuppressWarnings("unused")
        public IntegerProperty materialCountProperty() { return materialCount; }

    }
}
