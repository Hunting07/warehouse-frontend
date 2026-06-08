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
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.File;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StockWarningController {
    private static final Logger logger = Logger.getLogger(StockWarningController.class.getName());

    @FXML
    private TableView<WarningNode> warningTable;

    @FXML
    private TableColumn<WarningNode, Integer> idCol;
    @FXML
    private TableColumn<WarningNode, String> nameCol;
    @FXML
    private TableColumn<WarningNode, String> codeCol;
    @FXML
    private TableColumn<WarningNode, String> categoryNameCol;
    @FXML
    private TableColumn<WarningNode, Integer> currentStockCol;
    @FXML
    private TableColumn<WarningNode, Integer> safetyStockCol;
    @FXML
    private TableColumn<WarningNode, Integer> shortageCol;
    @FXML
    private TableColumn<WarningNode, LocalDateTime> updateTimeCol;
    @FXML
    private TableColumn<WarningNode, Void> actionCol;

    @FXML
    private TextField searchField;
    @FXML
    private Label warningCountLabel;
    @FXML
    private Label totalCountLabel;
    @FXML
    private Button exportBtn;

    private final ObservableList<WarningNode> dataList = FXCollections.observableArrayList();
    private final Gson gson = new Gson();
    private boolean isAdmin = false;

    @FXML
    public void initialize() {
        checkUserRole();
        setupTable();
        setupFilters();
        setupWarningTableStyle();
        loadWarnings();
        applyRolePermissions();
    }


    private void checkUserRole() {
        JwtResponse jwt = AppStore.getJwt();
        if (jwt != null && jwt.getRole() != null) {
            isAdmin = "admin".equals(jwt.getRole()) || "管理员".equals(jwt.getRole());
        }
    }

    private void applyRolePermissions() {
        if (!isAdmin) {
            if (exportBtn != null) {
                exportBtn.setVisible(false);
                exportBtn.setManaged(false);
            }
        }
    }

    private void setupTable() {
        idCol.setCellFactory(col -> new TableCell<WarningNode, Integer>() {
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
        nameCol.setCellValueFactory(param -> param.getValue().nameProperty());
        codeCol.setCellValueFactory(param -> param.getValue().codeProperty());
        categoryNameCol.setCellValueFactory(param -> param.getValue().categoryNameProperty());
        currentStockCol.setCellValueFactory(param -> param.getValue().currentStockProperty().asObject());
        safetyStockCol.setCellValueFactory(param -> param.getValue().safetyStockProperty().asObject());

        shortageCol.setCellValueFactory(param -> param.getValue().shortageProperty().asObject());
        shortageCol.setCellFactory(col -> new TableCell<WarningNode, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.valueOf(item));
                    setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;");
                }
            }
        });

        updateTimeCol.setCellValueFactory(param -> param.getValue().updateTimeProperty());

        for (TableColumn<WarningNode, ?> column : warningTable.getColumns()) {
            column.setSortable(true);
        }

        actionCol.setCellFactory(param -> new TableCell<>() {
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

                    Button settingBtn = new Button("设置");
                    settingBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #f59e0b, #d97706); -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 16; -fx-background-radius: 6; -fx-effect: dropshadow(gaussian, rgba(245, 158, 11, 0.3), 4, 0, 0, 2);");
                    settingBtn.setOnAction(e -> {
                        WarningNode warning = getTableRow().getItem();
                        editWarningSetting(warning);
                    });
                    setGraphic(settingBtn);
                }
            }
        });

        warningTable.setItems(dataList);

        warningTable.setRowFactory(tv -> new TableRow<WarningNode>() {
            @Override
            protected void updateItem(WarningNode item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else {
                    if (isSelected()) {
                        setStyle("-fx-background-color: #E3F2FD; -fx-text-fill: #333333;");
                    } else if (getIndex() % 2 == 0) {
                        setStyle("-fx-background-color: #fff5f5;");
                    } else {
                        setStyle("-fx-background-color: #ffffff;");
                    }
                }
            }
        });

        warningTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && isAdmin) {
                WarningNode selected = warningTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    editWarningSetting(selected);
                }
            }
        });

        Label emptyLabel = new Label("✅ 库存充足，暂无预警");
        emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #10b981; -fx-font-weight: 500;");
        warningTable.setPlaceholder(emptyLabel);
    }

    private void setupWarningTableStyle() {
        Platform.runLater(() -> {
            try {
                warningTable.lookup(".column-header-background").setStyle(
                        "-fx-background-color: linear-gradient(to bottom, #ff8a80, #ff5252); -fx-background-radius: 8;"
                );

                warningTable.lookupAll(".column-header .label").forEach(label -> {
                    label.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");
                });
            } catch (Exception e) {
                logger.warning("设置表格样式失败: " + e.getMessage());
            }
        });
    }

    private void setupFilters() {
        searchField.setPromptText("输入物资名称或编码搜索...");

        searchField.setOnAction(event -> searchWarning());
    }

    @FXML
    private void loadWarnings() {
        showLoading(true);
        try {
            DataRequest request = new DataRequest();
            DataResponse response = HttpRequestUtil.request("/api/material/list", request);

            if (response != null && response.getCode() == 200) {
                Platform.runLater(() -> {
                    buildDataList(response.getData());
                    updateStatistics();
                    showLoading(false);
                });
            } else {
                Platform.runLater(() -> {
                    showError("加载失败", response != null ? response.getMsg() : "网络错误");
                    showLoading(false);
                });
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "加载库存预警列表异常", e);
            Platform.runLater(() -> {
                showError("加载异常", e.getMessage());
                showLoading(false);
            });
        }
    }

    private void buildDataList(Object data) {
        dataList.clear();

        if (data == null) {
            return;
        }

        Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
        List<Map<String, Object>> allMaterials = gson.fromJson(gson.toJson(data), listType);

        if (allMaterials != null) {
            for (Map<String, Object> material : allMaterials) {
                try {
                    int currentStock = material.get("currentStock") != null ?
                            ((Number) material.get("currentStock")).intValue() : 0;
                    int safetyStock = material.get("safetyStock") != null ?
                            ((Number) material.get("safetyStock")).intValue() : 0;

                    if (currentStock < safetyStock) {
                        WarningNode node = mapToWarningNode(material);
                        dataList.add(node);
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING, "转换物资数据失败", e);
                }
            }
        }

        warningTable.refresh();
    }

    private WarningNode mapToWarningNode(Map<String, Object> map) {
        WarningNode node = new WarningNode();

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
        if (map.get("currentStock") != null) {
            node.setCurrentStock(((Number) map.get("currentStock")).intValue());
        }
        if (map.get("safetyStock") != null) {
            node.setSafetyStock(((Number) map.get("safetyStock")).intValue());
        }
        if (map.get("updateTime") != null) {
            try {
                node.setUpdateTime(LocalDateTime.parse(map.get("updateTime").toString()));
            } catch (Exception e) {
                logger.log(Level.WARNING, "解析更新时间失败", e);
            }
        }

        int shortage = node.getSafetyStock() - node.getCurrentStock();
        node.setShortage(shortage > 0 ? shortage : 0);
        node.setWarningThreshold(node.getSafetyStock());

        return node;
    }

    private void updateStatistics() {
        long warningCount = dataList.size();

        Platform.runLater(() -> {
            warningCountLabel.setText(String.valueOf(warningCount));
            totalCountLabel.setText(String.valueOf(dataList.size()));
        });
    }

    @FXML
    private void searchWarning() {
        String keyword = searchField.getText();

        if (keyword == null || keyword.trim().isEmpty()) {
            warningTable.setItems(dataList);
            updateStatistics();
            return;
        }

        ObservableList<WarningNode> filteredList = FXCollections.observableArrayList();
        for (WarningNode node : dataList) {
            if (node.getName().contains(keyword) || node.getCode().contains(keyword)) {
                filteredList.add(node);
            }
        }
        warningTable.setItems(filteredList);
        updateStatistics();
    }

// ... existing code ...

    @FXML
    private void resetSearch() {
        searchField.clear();
        warningTable.setItems(dataList);
        updateStatistics();
    }

    @FXML
    private void exportWarnings() {
        if (!isAdmin) {
            showError("权限不足", "只有管理员可以导出数据");
            return;
        }

        if (dataList.isEmpty()) {
            showInfo("导出失败", "没有可导出的数据");
            return;
        }

        try {
            StringBuilder csv = new StringBuilder();
            csv.append("\uFEFF");
            csv.append("序号,物资名称,物资编码,所属分类,当前库存,安全库存,短缺数量,更新时间\n");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (int i = 0; i < dataList.size(); i++) {
                WarningNode node = dataList.get(i);
                csv.append(i + 1).append(",");
                csv.append(escapeCsv(node.getName())).append(",");
                csv.append(escapeCsv(node.getCode())).append(",");
                csv.append(escapeCsv(node.getCategoryName())).append(",");
                csv.append(node.getCurrentStock()).append(",");
                csv.append(node.getSafetyStock()).append(",");
                csv.append(node.getShortage()).append(",");
                csv.append(node.getUpdateTime() != null ? node.getUpdateTime().format(formatter) : "").append("\n");
            }

            Stage stage = new Stage();
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("导出库存预警");
            fileChooser.setInitialFileName("库存预警_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV文件", "*.csv"));

            File file = fileChooser.showSaveDialog(stage);
            if (file != null) {
                java.nio.file.Files.writeString(file.toPath(), csv.toString(), java.nio.charset.StandardCharsets.UTF_8);
                showInfo("导出成功", "文件已保存到: " + file.getAbsolutePath());
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "导出失败", e);
            showError("导出失败", e.getMessage());
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }


    private void editWarningSetting(WarningNode warning) {
        if (!isAdmin) {
            showError("权限不足", "只有管理员可以修改安全库存设置");
            return;
        }

        if (warning == null) {
            showError("操作失败", "请选择要设置的物资");
            return;
        }

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("库存预警设置 - " + warning.getName());

        VBox mainContainer = new VBox(16);
        mainContainer.setPadding(new Insets(24));
        mainContainer.setStyle("-fx-background-color: linear-gradient(to bottom, #ffffff, #f8fafc);");

        HBox titleBox = new HBox(12);
        titleBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label iconLabel = new Label("⚙");
        iconLabel.setStyle("-fx-font-size: 24px;");
        Label titleLabel = new Label("安全库存设置");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        titleBox.getChildren().addAll(iconLabel, titleLabel);

        VBox infoCard = new VBox(12);
        infoCard.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 12; -fx-border-color: #e2e8f0; -fx-border-width: 1; -fx-border-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2);");

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(14);

        Label nameLabel = new Label("物资名称:");
        nameLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b; -fx-font-weight: 500;");
        Label nameValue = new Label(warning.getName());
        nameValue.setStyle("-fx-font-size: 14px; -fx-text-fill: #1e293b; -fx-font-weight: bold;");

        Label codeLabel = new Label("物资编码:");
        codeLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b; -fx-font-weight: 500;");
        Label codeValue = new Label(warning.getCode());
        codeValue.setStyle("-fx-font-size: 14px; -fx-text-fill: #1e293b; -fx-font-weight: bold;");

        Label stockLabel = new Label("当前库存:");
        stockLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b; -fx-font-weight: 500;");
        Label stockValue = new Label(String.valueOf(warning.getCurrentStock()));
        stockValue.setStyle("-fx-font-size: 14px; -fx-text-fill: #dc2626; -fx-font-weight: bold;");

        Label safetyLabel = new Label("安全库存:");
        safetyLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b; -fx-font-weight: 500;");
        TextField safetyStockField = new TextField(String.valueOf(warning.getSafetyStock()));
        safetyStockField.setPromptText("请输入安全库存数量");
        safetyStockField.setStyle("-fx-font-size: 14px; -fx-padding: 10 12; -fx-background-radius: 8; -fx-border-color: #cbd5e1; -fx-border-width: 1.5; -fx-border-radius: 8;");

        grid.add(nameLabel, 0, 0);
        grid.add(nameValue, 1, 0);
        grid.add(codeLabel, 0, 1);
        grid.add(codeValue, 1, 1);
        grid.add(stockLabel, 0, 2);
        grid.add(stockValue, 1, 2);
        grid.add(safetyLabel, 0, 3);
        grid.add(safetyStockField, 1, 3);

        infoCard.getChildren().add(grid);

        VBox tipBox = new VBox(8);
        tipBox.setStyle("-fx-background-color: #fef3c7; -fx-padding: 14 18; -fx-background-radius: 10; -fx-border-color: #fde68a; -fx-border-width: 1; -fx-border-radius: 10;");
        Label tipIcon = new Label("💡");
        tipIcon.setStyle("-fx-font-size: 16px;");
        Label tipText = new Label("当库存低于安全库存时会触发预警，请及时补货");
        tipText.setStyle("-fx-text-fill: #92400e; -fx-font-size: 13px; -fx-font-weight: 500;");
        tipBox.getChildren().addAll(tipIcon, tipText);

        HBox buttonBox = new HBox(12);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(8, 0, 0, 0));

        Button saveBtn = new Button("💾 保存设置");
        saveBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #10b981, #059669); -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 12 32; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(16, 185, 129, 0.3), 8, 0, 0, 2);");

        Button cancelBtn = new Button("取消");
        cancelBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #f8fafc, #e2e8f0); -fx-text-fill: #475569; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 12 32; -fx-background-radius: 10; -fx-border-color: #cbd5e1; -fx-border-width: 1.5; -fx-border-radius: 10;");

        buttonBox.getChildren().addAll(cancelBtn, saveBtn);

        mainContainer.getChildren().addAll(titleBox, infoCard, tipBox, buttonBox);

        // ... existing code ...
        saveBtn.setOnAction(e -> {
            try {
                int safetyStock = Integer.parseInt(safetyStockField.getText());

                if (safetyStock < 0) {
                    showError("输入错误", "安全库存不能为负数");
                    return;
                }

                DataRequest request = new DataRequest();
                request.put("id", warning.getId());
                request.put("safetyStock", safetyStock);

                logger.info("=== 开始发送更新请求 ===");
                logger.info("物资ID: " + warning.getId());
                logger.info("安全库存: " + safetyStock);
                logger.info("请求参数: " + gson.toJson(request));

                try {
                    DataResponse response = HttpRequestUtil.request("/api/material/update", request);

                    logger.info("响应结果: " + (response != null ? "code=" + response.getCode() + ", msg=" + response.getMsg() : "null"));

                    if (response != null && response.getCode() == 200) {
                        showInfo("设置成功", "安全库存已更新为 " + safetyStock);
                        dialog.close();
                        loadWarnings();
                    } else if (response != null) {
                        showError("设置失败", "服务器返回错误: " + response.getMsg());
                    } else {
                        showError("设置失败", "HTTP请求返回null，请查看控制台日志");
                    }
                } catch (Exception httpEx) {
                    logger.log(Level.SEVERE, "HTTP请求异常", httpEx);
                    showError("网络错误", "请求异常: " + httpEx.getMessage());
                }
            } catch (NumberFormatException ex) {
                showError("输入错误", "请输入有效的数字");
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "设置预警异常", ex);
                showError("设置异常", ex.getMessage());
            }
        });


        cancelBtn.setOnAction(e -> dialog.close());

        javafx.scene.Scene scene = new javafx.scene.Scene(mainContainer, 560, 450);
        scene.getStylesheets().add(getClass().getResource("/styles/modern-style.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void showLoading(boolean loading) {
        Platform.runLater(() -> {
            if (loading) {
                warningTable.setOpacity(0.5);
            } else {
                warningTable.setOpacity(1.0);
            }
        });
    }

    // ... existing code ...
    private void showInfo(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);

            alert.getDialogPane().setStyle("-fx-background-color: linear-gradient(to bottom, #ffffff, #f8fafc); -fx-padding: 20 24 16 24;");
            alert.getDialogPane().getStylesheets().add(getClass().getResource("/styles/modern-style.css").toExternalForm());

            Label messageLabel = (Label) alert.getDialogPane().lookup(".content.label");
            if (messageLabel != null) {
                messageLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: #1e293b; -fx-font-weight: 500;");
            }

            ButtonType okBtn = new ButtonType("✓ 确定", ButtonBar.ButtonData.OK_DONE);
            alert.getButtonTypes().setAll(okBtn);

            Button button = (Button) alert.getDialogPane().lookupButton(okBtn);
            if (button != null) {
                button.setStyle("-fx-background-color: linear-gradient(to bottom, #10b981, #059669); -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 10; -fx-padding: 12 32; -fx-effect: dropshadow(gaussian, rgba(16, 185, 129, 0.4), 8, 0, 0, 2);");
                button.setDefaultButton(true);
            }

            alert.showAndWait();
        });
    }


    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);

            alert.getDialogPane().setStyle("-fx-background-color: linear-gradient(to bottom, #ffffff, #fef2f2); -fx-padding: 20 24 16 24;");
            alert.getDialogPane().getStylesheets().add(getClass().getResource("/styles/modern-style.css").toExternalForm());

            Label messageLabel = (Label) alert.getDialogPane().lookup(".content.label");
            if (messageLabel != null) {
                messageLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: #dc2626; -fx-font-weight: 500;");
            }

            ButtonType okBtn = new ButtonType("✗ 确定", ButtonBar.ButtonData.OK_DONE);
            alert.getButtonTypes().setAll(okBtn);

            Button button = (Button) alert.getDialogPane().lookupButton(okBtn);
            if (button != null) {
                button.setStyle("-fx-background-color: linear-gradient(to bottom, #ef4444, #dc2626); -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 10; -fx-padding: 12 32; -fx-effect: dropshadow(gaussian, rgba(239, 68, 68, 0.4), 8, 0, 0, 2);");
                button.setDefaultButton(true);
            }

            alert.showAndWait();
        });
    }


    public void cleanup() {
    }

    public static class WarningNode {
        private final IntegerProperty id = new SimpleIntegerProperty();
        private final StringProperty name = new SimpleStringProperty("");
        private final StringProperty code = new SimpleStringProperty("");
        private final StringProperty categoryName = new SimpleStringProperty("");
        private final IntegerProperty currentStock = new SimpleIntegerProperty();
        private final IntegerProperty safetyStock = new SimpleIntegerProperty();
        private final IntegerProperty shortage = new SimpleIntegerProperty();
        private final IntegerProperty warningThreshold = new SimpleIntegerProperty();
        private final ObjectProperty<LocalDateTime> updateTime = new SimpleObjectProperty<>();

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
        public String getCategoryName() { return categoryName.get(); }
        public void setCategoryName(String value) { categoryName.set(value); }

        public IntegerProperty currentStockProperty() { return currentStock; }
        public int getCurrentStock() { return currentStock.get(); }
        public void setCurrentStock(int value) { currentStock.set(value); }

        public IntegerProperty safetyStockProperty() { return safetyStock; }
        public int getSafetyStock() { return safetyStock.get(); }
        public void setSafetyStock(int value) { safetyStock.set(value); }

        public IntegerProperty shortageProperty() { return shortage; }
        public int getShortage() { return shortage.get(); }
        public void setShortage(int value) { shortage.set(value); }

        public IntegerProperty warningThresholdProperty() { return warningThreshold; }
        public int getWarningThreshold() { return warningThreshold.get(); }
        public void setWarningThreshold(int value) { warningThreshold.set(value); }

        public ObjectProperty<LocalDateTime> updateTimeProperty() { return updateTime; }
        public LocalDateTime getUpdateTime() { return updateTime.get(); }
        public void setUpdateTime(LocalDateTime value) { updateTime.set(value); }
    }
}
