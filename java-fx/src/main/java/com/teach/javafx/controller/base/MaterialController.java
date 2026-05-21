package com.teach.javafx.controller.base;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;
import java.time.LocalDateTime;

public class MaterialController {

    @FXML
    private TableView<Material> materialTable;

    @FXML
    private TableColumn<Material, Integer> idCol;
    @FXML
    private TableColumn<Material, String> nameCol;
    @FXML
    private TableColumn<Material, String> categoryNameCol;
    @FXML
    private TableColumn<Material, Integer> currentStockCol;
    @FXML
    private TableColumn<Material, Integer> safetyStockCol;
    @FXML
    private TableColumn<Material, LocalDateTime> createTimeCol;
    @FXML
    private TableColumn<Material, Void> actionCol;

    private ObservableList<Material> dataList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // 绑定列
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        categoryNameCol.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        currentStockCol.setCellValueFactory(new PropertyValueFactory<>("currentStock"));
        safetyStockCol.setCellValueFactory(new PropertyValueFactory<>("safetyStock"));
        createTimeCol.setCellValueFactory(new PropertyValueFactory<>("createTime"));

        // 操作列（和 CategoryController 完全一样）
        actionCol.setCellFactory(new Callback<>() {
            @Override
            public TableCell<Material, Void> call(TableColumn<Material, Void> param) {
                return new TableCell<>() {
                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        setGraphic(null);
                    }
                };
            }
        });

        // 假数据（和 CategoryController 格式完全一致）
        dataList.add(new Material(1, "笔记本电脑", "电子设备", 10, 5, LocalDateTime.now()));
        dataList.add(new Material(2, "打印纸", "办公用品", 50, 20, LocalDateTime.now()));

        materialTable.setItems(dataList);
    }

    @FXML
    private void addMaterial() {
        new Alert(Alert.AlertType.INFORMATION, "新增功能暂未实现").show();
    }

    @FXML
    private void editMaterial() {
        new Alert(Alert.AlertType.INFORMATION, "编辑功能暂未实现").show();
    }

    @FXML
    private void deleteMaterial() {
        new Alert(Alert.AlertType.INFORMATION, "删除功能暂未实现").show();
    }

    // 内部类（和 Category 格式完全一致）
    public static class Material {
        private Integer id;
        private String name;
        private String categoryName;
        private Integer currentStock;
        private Integer safetyStock;
        private LocalDateTime createTime;

        public Material() {}

        public Material(Integer id, String name, String categoryName, Integer currentStock, Integer safetyStock, LocalDateTime createTime) {
            this.id = id;
            this.name = name;
            this.categoryName = categoryName;
            this.currentStock = currentStock;
            this.safetyStock = safetyStock;
            this.createTime = createTime;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public void setCategoryName(String categoryName) {
            this.categoryName = categoryName;
        }

        public Integer getCurrentStock() {
            return currentStock;
        }

        public void setCurrentStock(Integer currentStock) {
            this.currentStock = currentStock;
        }

        public Integer getSafetyStock() {
            return safetyStock;
        }

        public void setSafetyStock(Integer safetyStock) {
            this.safetyStock = safetyStock;
        }

        public LocalDateTime getCreateTime() {
            return createTime;
        }

        public void setCreateTime(LocalDateTime createTime) {
            this.createTime = createTime;
        }
    }
}
