package com.teach.javafx.controller.base;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;
import java.time.LocalDateTime;

public class CategoryController {
    @FXML
    private TableView<Category> categoryTable;
    @FXML
    private TableColumn<Category, Integer> idCol;
    @FXML
    private TableColumn<Category, String> nameCol;
    @FXML
    private TableColumn<Category, LocalDateTime> createTimeCol;
    @FXML
    private TableColumn<Category, Void> actionCol;

    private ObservableList<Category> dataList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // 绑定列
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        createTimeCol.setCellValueFactory(new PropertyValueFactory<>("createTime"));

        // 操作列（先留空，不写按钮逻辑）
        actionCol.setCellFactory(new Callback<>() {
            @Override
            public TableCell<Category, Void> call(TableColumn<Category, Void> param) {
                return new TableCell<>();
            }
        });

        // 先加几条假数据，保证表格能显示
        dataList.add(new Category(1, "办公用品", LocalDateTime.now()));
        dataList.add(new Category(2, "电子设备", LocalDateTime.now()));
        categoryTable.setItems(dataList);
    }

    @FXML
    private void addCategory() {
        new Alert(Alert.AlertType.INFORMATION, "新增功能暂未实现").show();
    }

    // 内部类（必须完整）
    public static class Category {
        private Integer id;
        private String name;
        private LocalDateTime createTime;

        public Category() {}
        public Category(Integer id, String name, LocalDateTime createTime) {
            this.id = id;
            this.name = name;
            this.createTime = createTime;
        }

        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public LocalDateTime getCreateTime() { return createTime; }
        public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    }
}
