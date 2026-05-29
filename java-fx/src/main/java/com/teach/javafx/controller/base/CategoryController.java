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

    private final Gson gson = new Gson();
    private boolean isAdmin = false;

    @FXML
    public void initialize() {
        checkUserRole();
        setupTreeTable();
        setupFilters();
        applyRolePermissions();
        loadCategoryTree();
    }

    private void checkUserRole() {
        JwtResponse jwt = AppStore.getJwt();
        if (jwt != null && jwt.getRole() != null) {
            isAdmin = "admin".equals(jwt.getRole()) || "管理员".equals(jwt.getRole());
        }
        System.out.println("物资分类-当前用户角色: " + (jwt != null ? jwt.getRole() : "null") + ", 是否管理员: " + isAdmin);
    }

    private void applyRolePermissions() {
        if (!isAdmin) {
            if (addCategoryBtn != null) {
                addCategoryBtn.setVisible(false);
                addCategoryBtn.setManaged(false);
            }
            System.out.println("物资分类-当前为普通员工，仅查看权限");
        } else {
            System.out.println("物资分类-当前为管理员，拥有完整操作权限");
        }
    }

    private void setupTreeTable() {
        idCol.setCellValueFactory(param -> param.getValue().getValue().idProperty().asObject());
        nameCol.setCellValueFactory(param -> param.getValue().getValue().nameProperty());
        codeCol.setCellValueFactory(param -> param.getValue().getValue().codeProperty());
        statusCol.setCellValueFactory(param -> param.getValue().getValue().statusProperty());
        materialCountCol.setCellValueFactory(param -> param.getValue().getValue().materialCountProperty().asObject());
        createTimeCol.setCellValueFactory(param -> param.getValue().getValue().createTimeProperty());

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
                            // 查看物资按钮：所有用户都可见
                            Button viewMaterialsBtn = new Button("查看物资");
                            viewMaterialsBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-cursor: hand;");
                            viewMaterialsBtn.setOnAction(e -> {
                                TreeItem<CategoryNode> treeItem = getTreeTableView().getTreeItem(getIndex());
                                if (treeItem != null) {
                                    CategoryNode node = treeItem.getValue();
                                    viewMaterialsInCategory(node);
                                }
                            });

                            if (isAdmin) {
                                // 管理员：显示编辑和删除按钮
                                Button editBtn = new Button("编辑");
                                editBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-cursor: hand;");
                                editBtn.setOnAction(e -> {
                                    TreeItem<CategoryNode> treeItem = getTreeTableView().getTreeItem(getIndex());
                                    if (treeItem != null) {
                                        CategoryNode node = treeItem.getValue();
                                        showEditDialog(node);
                                    }
                                });

                                Button deleteBtn = new Button("删除");
                                deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-cursor: hand;");
                                deleteBtn.setOnAction(e -> {
                                    TreeItem<CategoryNode> treeItem = getTreeTableView().getTreeItem(getIndex());
                                    if (treeItem != null) {
                                        CategoryNode node = treeItem.getValue();
                                        deleteCategory(node);
                                    }
                                });

                                javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(5, viewMaterialsBtn, editBtn, deleteBtn);
                                box.setPadding(new Insets(5));
                                setGraphic(box);
                            } else {
                                // 普通员工：只显示查看物资按钮
                                javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(5, viewMaterialsBtn);
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
    }

    private CategoryNode mapToCategoryNode(Map<String, Object> map) {
        CategoryNode node = new CategoryNode();

        System.out.println("====== 分类数据映射 ======");
        System.out.println("原始数据: " + gson.toJson(map));

        if (map.get("id") != null) {
            node.setId(((Number) map.get("id")).intValue());
        }
        if (map.get("name") != null) {
            node.setName((String) map.get("name"));
        }
        if (map.get("code") != null) {
            String code = (String) map.get("code");
            System.out.println("分类编码: " + code);
            node.setCode(code);
        } else {
            System.out.println("分类编码: null (后端未返回)");
        }
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
            System.out.println("状态: " + statusText + " (原始值: " + statusObj + ")");
        }
        if (map.get("createTime") != null) {
            try {
                node.setCreateTime(LocalDateTime.parse(map.get("createTime").toString()));
            } catch (Exception e) {
                logger.log(Level.WARNING, "解析创建时间失败", e);
            }
        }

        loadMaterialCountForCategory(node);

        return node;
    }


    private void loadMaterialCountForCategory(CategoryNode categoryNode) {
        Platform.runLater(() -> {
            try {
                DataRequest request = new DataRequest();
                request.put("categoryName", categoryNode.getName());

                DataResponse response = HttpRequestUtil.request("/api/material/search", request);

                if (response != null && response.getCode() == 200 && response.getData() != null) {
                    Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
                    List<Map<String, Object>> materialList = gson.fromJson(gson.toJson(response.getData()), listType);

                    int count = (materialList != null) ? materialList.size() : 0;
                    categoryNode.setMaterialCount(count);
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "加载分类物资数量失败: " + categoryNode.getName(), e);
            }
        });
    }

    @FXML
    private void searchCategory() {
        String keyword = searchField.getText();
        String status = statusFilter != null ? statusFilter.getValue() : "全部";

        try {
            DataRequest request = new DataRequest();
            if (keyword != null && !keyword.isEmpty()) {
                request.put("keyword", keyword);
            }
            if (status != null && !"全部".equals(status)) {
                int statusCode = "启用".equals(status) ? 1 : 0;
                request.put("status", statusCode);
                System.out.println("状态转换: " + status + " -> " + statusCode);
            }

            System.out.println("====== 分类搜索请求 ======");
            System.out.println("请求URL: /api/category/search");
            System.out.println("请求参数: " + gson.toJson(request.getParams()));

            DataResponse response = HttpRequestUtil.request("/api/category/search", request);

            System.out.println("响应结果: " + (response != null ? gson.toJson(response) : "null"));

            if (response != null && response.getCode() == 200) {
                Platform.runLater(() -> buildTreeFromData(response.getData()));
            } else {
                String errorMsg = response != null ? response.getMsg() : "网络错误";
                System.out.println("搜索失败: " + errorMsg);
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

        // 检查分类下是否有物资
        try {
            DataRequest checkRequest = new DataRequest();
            checkRequest.put("categoryName", node.getName());

            System.out.println("====== 检查分类下物资 ======");
            System.out.println("请求URL: /api/material/search");
            System.out.println("请求参数: " + gson.toJson(checkRequest.getParams()));

            DataResponse checkResponse = HttpRequestUtil.request("/api/material/search", checkRequest);

            System.out.println("响应结果: " + (checkResponse != null ? gson.toJson(checkResponse) : "null"));

            if (checkResponse != null && checkResponse.getCode() == 200 && checkResponse.getData() != null) {
                Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
                List<Map<String, Object>> materialList = gson.fromJson(gson.toJson(checkResponse.getData()), listType);

                int materialCount = (materialList != null) ? materialList.size() : 0;

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

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("确认删除");
        confirm.setHeaderText("确定要删除分类 \"" + node.getName() + "\" 吗？");
        confirm.setContentText("删除后无法恢复！");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    DataRequest request = new DataRequest();
                    request.put("id", node.getId());

                    System.out.println("====== 分类删除请求 ======");
                    System.out.println("请求URL: /api/category/delete");
                    System.out.println("请求参数: " + gson.toJson(request.getParams()));

                    DataResponse resp = HttpRequestUtil.request("/api/category/delete", request);

                    System.out.println("响应结果: " + (resp != null ? gson.toJson(resp) : "null"));

                    if (resp != null && resp.getCode() == 200) {
                        showInfo("删除成功", "分类已删除");
                        loadCategoryTree();
                    } else {
                        String errorMsg = resp != null ? resp.getMsg() : "网络错误";
                        System.out.println("删除失败: " + errorMsg);
                        showError("删除失败", "后端返回错误: " + errorMsg);
                    }
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "删除分类异常", e);
                    showError("删除异常", e.getMessage());
                }
            }
        });
    }

    private void viewMaterialsInCategory(CategoryNode category) {
        if (category == null || category.getId() == 0) {
            showError("查看失败", "请选择有效的分类");
            return;
        }

        try {
            DataRequest request = new DataRequest();
            request.put("categoryName", category.getName());

            System.out.println("====== 查看分类下物资请求 ======");
            System.out.println("请求URL: /api/material/search");
            System.out.println("请求参数: " + gson.toJson(request.getParams()));

            DataResponse response = HttpRequestUtil.request("/api/material/search", request);

            System.out.println("响应结果: " + (response != null ? gson.toJson(response) : "null"));

            if (response != null && response.getCode() == 200) {
                showMaterialsDialog(category.getName(), response.getData());
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
        dialog.setTitle("分类 [" + categoryName + "] 下的物资列表");

        TableView<MaterialNode> materialTable = new TableView<>();

        TableColumn<MaterialNode, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(param -> param.getValue().getIdProperty().asObject());
        idCol.setPrefWidth(60);

        TableColumn<MaterialNode, String> nameCol = new TableColumn<>("物资名称");
        nameCol.setCellValueFactory(param -> param.getValue().nameProperty());
        nameCol.setPrefWidth(150);

        TableColumn<MaterialNode, String> codeCol = new TableColumn<>("物资编码");
        codeCol.setCellValueFactory(param -> param.getValue().codeProperty());
        codeCol.setPrefWidth(120);

        TableColumn<MaterialNode, String> unitCol = new TableColumn<>("单位");
        unitCol.setCellValueFactory(param -> param.getValue().unitProperty());
        unitCol.setPrefWidth(80);

        TableColumn<MaterialNode, Integer> currentStockCol = new TableColumn<>("当前库存");
        currentStockCol.setCellValueFactory(param -> param.getValue().currentStockProperty().asObject());
        currentStockCol.setPrefWidth(100);

        TableColumn<MaterialNode, Integer> safetyStockCol = new TableColumn<>("安全库存");
        safetyStockCol.setCellValueFactory(param -> param.getValue().safetyStockProperty().asObject());
        safetyStockCol.setPrefWidth(100);

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
                        setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: green;");
                    }
                }
            }
        });
        warningCol.setPrefWidth(100);

        TableColumn<MaterialNode, String> statusCol = new TableColumn<>("状态");
        statusCol.setCellValueFactory(param -> param.getValue().statusProperty());
        statusCol.setPrefWidth(80);

        materialTable.getColumns().addAll(idCol, nameCol, codeCol, unitCol, currentStockCol, safetyStockCol, warningCol, statusCol);

        @SuppressWarnings("unchecked")
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
        closeBtn.setOnAction(e -> dialog.close());
        closeBtn.setStyle("-fx-background-color: #757575; -fx-text-fill: white; -fx-cursor: hand;");

        VBox vbox = new VBox(10, materialTable, closeBtn);
        vbox.setPadding(new Insets(10));
        vbox.setAlignment(javafx.geometry.Pos.CENTER);

        Scene scene = new Scene(vbox, 900, 500);
        dialog.setScene(scene);
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
                node.setStatus(map.get("status").toString());
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

        public IntegerProperty getIdProperty() { return id; }
        public void setId(int value) { id.set(value); }

        public StringProperty nameProperty() { return name; }
        public void setName(String value) { name.set(value); }

        public StringProperty codeProperty() { return code; }
        public void setCode(String value) { code.set(value); }

        public IntegerProperty categoryIdProperty() { return categoryId; }
        public void setCategoryId(int value) { categoryId.set(value); }

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

        public ObjectProperty<java.math.BigDecimal> priceProperty() { return price; }
        public void setPrice(java.math.BigDecimal value) { price.set(value); }

        public StringProperty statusProperty() { return status; }
        public void setStatus(String value) { status.set(value); }

    }


    private void showEditDialog(CategoryNode node) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(node == null ? "新增分类" : "编辑分类");

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setHgap(10);
        grid.setVgap(10);

        TextField nameField = new TextField(node != null ? node.getName() : "");
        nameField.setPromptText("请输入分类名称");

        TextField codeField = new TextField(node != null ? node.getCode() : "");
        codeField.setPromptText("请输入分类编码");

        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("启用", "禁用");
        statusCombo.setValue(node != null ? node.getStatus() : "启用");

        grid.add(new Label("分类名称:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("分类编码:"), 0, 1);
        grid.add(codeField, 1, 1);
        grid.add(new Label("状态:"), 0, 2);
        grid.add(statusCombo, 1, 2);

        Button saveBtn = new Button("保存");
        Button cancelBtn = new Button("取消");

        saveBtn.setOnAction(e -> {
            try {
                DataRequest request = new DataRequest();
                request.put("name", nameField.getText());
                request.put("code", codeField.getText());

                String statusValue = statusCombo.getValue();
                int statusCode = "启用".equals(statusValue) ? 1 : 0;
                request.put("status", statusCode);

                if (node != null) {
                    request.put("id", node.getId());
                }

                String url = node == null ? "/api/category/add" : "/api/category/update";
                System.out.println("====== 分类保存请求 ======");
                System.out.println("请求URL: " + url);
                System.out.println("请求参数: " + gson.toJson(request.getParams()));
                System.out.println("状态转换: " + statusValue + " -> " + statusCode);

                DataResponse response = HttpRequestUtil.request(url, request);

                System.out.println("响应结果: " + (response != null ? gson.toJson(response) : "null"));

                if (response != null && response.getCode() == 200) {
                    showInfo("操作成功", node == null ? "分类已创建" : "分类已更新");
                    dialog.close();
                    System.out.println("重新加载分类树...");
                    loadCategoryTree();
                } else {
                    String errorMsg = response != null ? response.getMsg() : "网络错误";
                    System.out.println("保存失败: " + errorMsg);
                    showError("操作失败", "后端返回错误: " + errorMsg);
                }
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "操作分类异常", ex);
                showError("操作异常", ex.getMessage());
            }
        });

        cancelBtn.setOnAction(e -> dialog.close());

        javafx.scene.layout.HBox buttonBox = new javafx.scene.layout.HBox(10, saveBtn, cancelBtn);
        grid.add(buttonBox, 1, 3);

        Scene scene = new Scene(grid, 400, 200);
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
