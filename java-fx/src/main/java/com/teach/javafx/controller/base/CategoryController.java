package com.teach.javafx.controller.base;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.DataResponse;
import com.teach.javafx.request.HttpRequestUtil;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

public class CategoryController {
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
    private TreeTableColumn<CategoryNode, Integer> sortCol;
    @FXML
    private TreeTableColumn<CategoryNode, String> statusCol;
    @FXML
    private TreeTableColumn<CategoryNode, LocalDateTime> createTimeCol;
    @FXML
    private TreeTableColumn<CategoryNode, Void> actionCol;

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> statusFilter;

    private final Gson gson = new Gson();

    @FXML
    public void initialize() {
        setupTreeTable();
        setupFilters();
        loadCategoryTree();
    }

    private void setupTreeTable() {
        idCol.setCellValueFactory(param -> param.getValue().getValue().idProperty().asObject());
        nameCol.setCellValueFactory(param -> param.getValue().getValue().nameProperty());
        codeCol.setCellValueFactory(param -> param.getValue().getValue().codeProperty());
        sortCol.setCellValueFactory(param -> param.getValue().getValue().sortProperty().asObject());
        statusCol.setCellValueFactory(param -> param.getValue().getValue().statusProperty());
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
                            Button editBtn = new Button("编辑");
                            editBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-cursor: hand;");
                            editBtn.setOnAction(e -> {
                                CategoryNode node = getTreeTableView().getSelectionModel().getSelectedItem().getValue();
                                editCategory(node);
                            });

                            Button deleteBtn = new Button("删除");
                            deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-cursor: hand;");
                            deleteBtn.setOnAction(e -> {
                                CategoryNode node = getTreeTableView().getSelectionModel().getSelectedItem().getValue();
                                deleteCategory(node);
                            });

                            javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(5, editBtn, deleteBtn);
                            box.setPadding(new Insets(5));
                            setGraphic(box);
                        }
                    }
                };
            }
        });

        categoryTreeTable.setShowRoot(false);
    }

    private void setupFilters() {
        statusFilter.getItems().addAll("全部", "启用", "禁用");
        statusFilter.setValue("全部");

        searchField.setPromptText("输入分类名称搜索...");
    }

    @FXML
    private void loadCategoryTree() {
        try {
            DataRequest request = new DataRequest();
            String responseStr = HttpRequestUtil.post("/api/category/tree", gson.toJson(request.getParams()));

            if (responseStr != null) {
                DataResponse response = gson.fromJson(responseStr, DataResponse.class);
                if (response != null && response.getCode() == 200) {
                    Platform.runLater(() -> buildTreeFromData(response.getData()));
                } else {
                    showError("加载失败", response != null ? response.getMsg() : "网络错误");
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "加载分类树异常", e);
            showError("加载异常", e.getMessage());
        }
    }

    private void buildTreeFromData(Object data) {
        TreeItem<CategoryNode> root = new TreeItem<>(new CategoryNode());

        if (data == null) {
            categoryTreeTable.setRoot(root);
            return;
        }

        Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
        List<Map<String, Object>> categoryList = gson.fromJson(gson.toJson(data), listType);

        if (categoryList != null) {
            buildTreeRecursive(categoryList, root);
        }

        categoryTreeTable.setRoot(root);
    }

    private void buildTreeRecursive(List<Map<String, Object>> categories, TreeItem<CategoryNode> parent) {
        if (categories == null) {
            return;
        }

        for (Map<String, Object> category : categories) {
            CategoryNode node = mapToCategoryNode(category);
            TreeItem<CategoryNode> treeItem = new TreeItem<>(node);

            if (category.containsKey("children") && category.get("children") != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> children = (List<Map<String, Object>>) category.get("children");
                buildTreeRecursive(children, treeItem);
            }

            parent.getChildren().add(treeItem);
        }
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
        }
        if (map.get("sort") != null) {
            node.setSort(((Number) map.get("sort")).intValue());
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
    private void addCategory() {
        showEditDialog(null);
    }

    private void editCategory(CategoryNode node) {
        if (node != null) {
            showEditDialog(node);
        }
    }

    private void deleteCategory(CategoryNode node) {
        if (node == null) {
            showError("删除失败", "无效的分类节点");
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

                    String responseStr = HttpRequestUtil.post("/api/category/delete", gson.toJson(request.getParams()));

                    if (responseStr != null) {
                        DataResponse resp = gson.fromJson(responseStr, DataResponse.class);
                        if (resp != null && resp.getCode() == 200) {
                            showInfo("删除成功", "分类已删除");
                            loadCategoryTree();
                        } else {
                            showError("删除失败", resp != null ? resp.getMsg() : "网络错误");
                        }
                    }
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "删除分类异常", e);
                    showError("删除异常", e.getMessage());
                }
            }
        });
    }

    @FXML
    private void searchCategory() {
        String keyword = searchField.getText();
        String status = statusFilter.getValue();

        try {
            DataRequest request = new DataRequest();
            if (keyword != null && !keyword.isEmpty()) {
                request.put("keyword", keyword);
            }
            if (!"全部".equals(status)) {
                request.put("status", status);
            }

            String responseStr = HttpRequestUtil.post("/api/category/search", gson.toJson(request.getParams()));

            if (responseStr != null) {
                DataResponse response = gson.fromJson(responseStr, DataResponse.class);
                if (response != null && response.getCode() == 200) {
                    Platform.runLater(() -> buildTreeFromData(response.getData()));
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "搜索分类异常", e);
            showError("搜索异常", e.getMessage());
        }
    }

    @FXML
    private void resetSearch() {
        searchField.clear();
        statusFilter.setValue("全部");
        loadCategoryTree();
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

        TextField sortField = new TextField(node != null ? String.valueOf(node.getSort()) : "0");
        sortField.setPromptText("排序号");

        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("启用", "禁用");
        statusCombo.setValue(node != null ? node.getStatus() : "启用");

        grid.add(new Label("分类名称:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("分类编码:"), 0, 1);
        grid.add(codeField, 1, 1);
        grid.add(new Label("排序:"), 0, 2);
        grid.add(sortField, 1, 2);
        grid.add(new Label("状态:"), 0, 3);
        grid.add(statusCombo, 1, 3);

        Button saveBtn = new Button("保存");
        Button cancelBtn = new Button("取消");

        saveBtn.setOnAction(e -> {
            try {
                DataRequest request = new DataRequest();
                request.put("name", nameField.getText());
                request.put("code", codeField.getText());
                request.put("sort", Integer.parseInt(sortField.getText()));
                request.put("status", statusCombo.getValue());

                if (node != null) {
                    request.put("id", node.getId());
                }

                String url = node == null ? "/api/category/add" : "/api/category/update";
                String responseStr = HttpRequestUtil.post(url, gson.toJson(request.getParams()));

                if (responseStr != null) {
                    DataResponse response = gson.fromJson(responseStr, DataResponse.class);
                    if (response != null && response.getCode() == 200) {
                        showInfo("操作成功", node == null ? "分类已创建" : "分类已更新");
                        dialog.close();
                        loadCategoryTree();
                    } else {
                        showError("操作失败", response != null ? response.getMsg() : "网络错误");
                    }
                }
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "操作分类异常", ex);
                showError("操作异常", ex.getMessage());
            }
        });

        cancelBtn.setOnAction(e -> dialog.close());

        javafx.scene.layout.HBox buttonBox = new javafx.scene.layout.HBox(10, saveBtn, cancelBtn);
        grid.add(buttonBox, 1, 4);

        Scene scene = new Scene(grid, 400, 250);
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

    public static class CategoryNode {
        private final IntegerProperty id;
        private final StringProperty name;
        private final StringProperty code;
        private final IntegerProperty sort;
        private final StringProperty status;
        private final ObjectProperty<LocalDateTime> createTime;

        public CategoryNode() {
            this.id = new SimpleIntegerProperty(0);
            this.name = new SimpleStringProperty("");
            this.code = new SimpleStringProperty("");
            this.sort = new SimpleIntegerProperty(0);
            this.status = new SimpleStringProperty("");
            this.createTime = new SimpleObjectProperty<>(null);
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

        public int getSort() {
            return sort.get();
        }

        public void setSort(int value) {
            sort.set(value);
        }

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
    }
}
