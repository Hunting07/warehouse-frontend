package com.teach.javafx.controller.base;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.DataResponse;
import com.teach.javafx.request.HttpRequestUtil;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private TableColumn<WarningNode, Integer> warningThresholdCol;
    @FXML
    private TableColumn<WarningNode, String> warningLevelCol;
    @FXML
    private TableColumn<WarningNode, LocalDateTime> updateTimeCol;
    @FXML
    private TableColumn<WarningNode, Void> actionCol;

    @FXML
    private ComboBox<String> warningLevelFilter;
    @FXML
    private TextField searchField;
    @FXML
    private Label severeCountLabel;
    @FXML
    private Label warningCountLabel;
    @FXML
    private Label noticeCountLabel;
    @FXML
    private Label totalCountLabel;

    private final ObservableList<WarningNode> dataList = FXCollections.observableArrayList();
    private final Gson gson = new Gson();
    private Timeline refreshTimeline;

    @FXML
    public void initialize() {
        setupTable();
        setupFilters();
        loadWarnings();
        setupAutoRefresh();
    }

    private void setupTable() {
        idCol.setCellValueFactory(param -> param.getValue().getIdProperty().asObject());
        nameCol.setCellValueFactory(param -> param.getValue().nameProperty());
        codeCol.setCellValueFactory(param -> param.getValue().codeProperty());
        categoryNameCol.setCellValueFactory(param -> param.getValue().categoryNameProperty());
        currentStockCol.setCellValueFactory(param -> param.getValue().currentStockProperty().asObject());
        safetyStockCol.setCellValueFactory(param -> param.getValue().safetyStockProperty().asObject());
        shortageCol.setCellValueFactory(param -> param.getValue().shortageProperty().asObject());
        warningThresholdCol.setCellValueFactory(param -> param.getValue().warningThresholdProperty().asObject());
        warningLevelCol.setCellValueFactory(param -> param.getValue().warningLevelProperty());
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
                    Button settingBtn = new Button("设置");
                    settingBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-cursor: hand;");
                    settingBtn.setOnAction(e -> {
                        WarningNode warning = getTableView().getSelectionModel().getSelectedItem();
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
                    String level = item.getWarningLevel();
                    if ("严重".equals(level)) {
                        setStyle("-fx-background-color: #ffcccc;");
                    } else if ("警告".equals(level)) {
                        setStyle("-fx-background-color: #fff4cc;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        warningTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                WarningNode selected = warningTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    editWarningSetting(selected);
                }
            }
        });

        Label emptyLabel = new Label("暂无预警数据");
        emptyLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #999;");
        warningTable.setPlaceholder(emptyLabel);
    }

    private void setupFilters() {
        warningLevelFilter.getItems().addAll("全部", "严重", "警告", "提示");
        warningLevelFilter.setValue("全部");
        searchField.setPromptText("输入物资名称或编码搜索...");
        
        searchField.setOnAction(event -> searchWarning());
    }

    private void setupAutoRefresh() {
        refreshTimeline = new Timeline(new KeyFrame(Duration.minutes(5), e -> loadWarnings()));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
    }

    @FXML
    private void loadWarnings() {
        showLoading(true);
        try {
            DataRequest request = new DataRequest();
            String responseStr = HttpRequestUtil.post("/api/stock-warning/list", gson.toJson(request.getParams()));

            if (responseStr != null) {
                DataResponse response = gson.fromJson(responseStr, DataResponse.class);
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
        List<Map<String, Object>> warningList = gson.fromJson(gson.toJson(data), listType);

        if (warningList != null) {
            for (Map<String, Object> warning : warningList) {
                WarningNode node = mapToWarningNode(warning);
                dataList.add(node);
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
        if (map.get("warningThreshold") != null) {
            node.setWarningThreshold(((Number) map.get("warningThreshold")).intValue());
        }
        if (map.get("warningLevel") != null) {
            node.setWarningLevel((String) map.get("warningLevel"));
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

        return node;
    }

    private void updateStatistics() {
        long severeCount = dataList.stream().filter(w -> "严重".equals(w.getWarningLevel())).count();
        long warningCount = dataList.stream().filter(w -> "警告".equals(w.getWarningLevel())).count();
        long noticeCount = dataList.stream().filter(w -> "提示".equals(w.getWarningLevel())).count();

        Platform.runLater(() -> {
            severeCountLabel.setText(String.valueOf(severeCount));
            warningCountLabel.setText(String.valueOf(warningCount));
            noticeCountLabel.setText(String.valueOf(noticeCount));
            totalCountLabel.setText(String.valueOf(dataList.size()));
        });
    }

    @FXML
    private void searchWarning() {
        String keyword = searchField.getText();
        String warningLevel = warningLevelFilter.getValue();

        showLoading(true);
        try {
            DataRequest request = new DataRequest();
            if (keyword != null && !keyword.isEmpty()) {
                request.put("keyword", keyword);
            }
            if (warningLevel != null && !"全部".equals(warningLevel)) {
                request.put("warningLevel", warningLevel);
            }

            String responseStr = HttpRequestUtil.post("/api/stock-warning/search", gson.toJson(request.getParams()));

            if (responseStr != null) {
                DataResponse response = gson.fromJson(responseStr, DataResponse.class);
                if (response != null && response.getCode() == 200) {
                    Platform.runLater(() -> {
                        buildDataList(response.getData());
                        updateStatistics();
                        showLoading(false);
                    });
                } else {
                    Platform.runLater(() -> {
                        showError("搜索失败", response != null ? response.getMsg() : "网络错误");
                        showLoading(false);
                    });
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "搜索库存预警异常", e);
            Platform.runLater(() -> {
                showError("搜索异常", e.getMessage());
                showLoading(false);
            });
        }
    }

    @FXML
    private void resetSearch() {
        searchField.clear();
        warningLevelFilter.setValue("全部");
        loadWarnings();
    }

    @FXML
    private void exportWarnings() {
        if (dataList.isEmpty()) {
            showInfo("导出失败", "没有可导出的数据");
            return;
        }

        try {
            StringBuilder csv = new StringBuilder();
            csv.append("\uFEFF");
            csv.append("物资名称,物资编码,所属分类,当前库存,安全库存,短缺数量,预警阈值,预警级别,更新时间\n");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (WarningNode node : dataList) {
                csv.append(escapeCsv(node.getName())).append(",");
                csv.append(escapeCsv(node.getCode())).append(",");
                csv.append(escapeCsv(node.getCategoryName())).append(",");
                csv.append(node.getCurrentStock()).append(",");
                csv.append(node.getSafetyStock()).append(",");
                csv.append(node.getShortage()).append(",");
                csv.append(node.getWarningThreshold()).append(",");
                csv.append(escapeCsv(node.getWarningLevel())).append(",");
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
        if (warning == null) {
            showError("操作失败", "请选择要设置的物资");
            return;
        }

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("库存预警设置 - " + warning.getName());

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setHgap(10);
        grid.setVgap(10);

        TextField safetyStockField = new TextField(String.valueOf(warning.getSafetyStock()));
        safetyStockField.setPromptText("安全库存数量");

        TextField warningThresholdField = new TextField(String.valueOf(warning.getWarningThreshold()));
        warningThresholdField.setPromptText("预警阈值");

        ComboBox<String> warningLevelCombo = new ComboBox<>();
        warningLevelCombo.getItems().addAll("严重", "警告", "提示");
        warningLevelCombo.setValue(warning.getWarningLevel());

        Label infoLabel = new Label("💡 提示：当库存低于预警阈值时会触发预警");
        infoLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");

        grid.add(new Label("物资名称:"), 0, 0);
        grid.add(new Label(warning.getName()), 1, 0);
        grid.add(new Label("物资编码:"), 0, 1);
        grid.add(new Label(warning.getCode()), 1, 1);
        grid.add(new Label("当前库存:"), 0, 2);
        grid.add(new Label(String.valueOf(warning.getCurrentStock())), 1, 2);
        grid.add(new Label("安全库存:"), 0, 3);
        grid.add(safetyStockField, 1, 3);
        grid.add(new Label("预警阈值:"), 0, 4);
        grid.add(warningThresholdField, 1, 4);
        grid.add(new Label("预警级别:"), 0, 5);
        grid.add(warningLevelCombo, 1, 5);
        grid.add(infoLabel, 0, 6, 2, 1);

        Button saveBtn = new Button("保存");
        saveBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-cursor: hand;");
        Button cancelBtn = new Button("取消");
        cancelBtn.setStyle("-fx-cursor: hand;");

        saveBtn.setOnAction(e -> {
            try {
                int safetyStock = Integer.parseInt(safetyStockField.getText());
                int threshold = Integer.parseInt(warningThresholdField.getText());

                if (safetyStock < 0 || threshold < 0) {
                    showError("输入错误", "安全库存和预警阈值不能为负数");
                    return;
                }

                DataRequest request = new DataRequest();
                request.put("materialId", warning.getId());
                request.put("safetyStock", safetyStock);
                request.put("warningThreshold", threshold);
                request.put("warningLevel", warningLevelCombo.getValue());

                String responseStr = HttpRequestUtil.post("/api/stock-warning/setting", gson.toJson(request.getParams()));

                if (responseStr != null) {
                    DataResponse response = gson.fromJson(responseStr, DataResponse.class);
                    if (response != null && response.getCode() == 200) {
                        showInfo("设置成功", "预警设置已更新");
                        dialog.close();
                        loadWarnings();
                    } else {
                        showError("设置失败", response != null ? response.getMsg() : "网络错误");
                    }
                }
            } catch (NumberFormatException ex) {
                showError("输入错误", "请输入有效的数字");
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "设置预警异常", ex);
                showError("设置异常", ex.getMessage());
            }
        });

        cancelBtn.setOnAction(e -> dialog.close());

        HBox buttonBox = new HBox(10, saveBtn, cancelBtn);
        grid.add(buttonBox, 1, 7);

        javafx.scene.Scene scene = new javafx.scene.Scene(grid, 500, 300);
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

    public void cleanup() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }
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
        private final StringProperty warningLevel = new SimpleStringProperty("");
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

        public StringProperty warningLevelProperty() { return warningLevel; }
        public String getWarningLevel() { return warningLevel.get(); }
        public void setWarningLevel(String value) { warningLevel.set(value); }

        public ObjectProperty<LocalDateTime> updateTimeProperty() { return updateTime; }
        public LocalDateTime getUpdateTime() { return updateTime.get(); }
        public void setUpdateTime(LocalDateTime value) { updateTime.set(value); }
    }
}