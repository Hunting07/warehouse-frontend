package com.teach.javafx.controller.base;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.teach.javafx.AppStore;
import com.teach.javafx.models.StockIn;
import com.teach.javafx.request.HttpRequestUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StockInController {

    @FXML
    private TextField searchCodeField;

    @FXML
    private ComboBox<String> typeComboBox;

    @FXML
    private ComboBox<String> statusComboBox;

    @FXML
    private TableView<StockIn> stockInTable;

    @FXML
    private TableColumn<StockIn, Integer> idColumn;

    @FXML
    private TableColumn<StockIn, String> inCodeColumn;

    @FXML
    private TableColumn<StockIn, String> typeColumn;

    @FXML
    private TableColumn<StockIn, BigDecimal> totalAmountColumn;

    @FXML
    private TableColumn<StockIn, Integer> statusColumn;

    @FXML
    private TableColumn<StockIn, LocalDateTime> createTimeColumn;

    @FXML
    private TableColumn<StockIn, LocalDateTime> approveTimeColumn;

    private ObservableList<StockIn> stockInList = FXCollections.observableArrayList();
    private final Gson gson = new Gson();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        inCodeColumn.setCellValueFactory(new PropertyValueFactory<>("inCode"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        totalAmountColumn.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        createTimeColumn.setCellValueFactory(new PropertyValueFactory<>("createTime"));
        approveTimeColumn.setCellValueFactory(new PropertyValueFactory<>("approveTime"));

        statusColumn.setCellFactory(col -> new TableCell<StockIn, Integer>() {
            @Override
            protected void updateItem(Integer status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                } else {
                    switch (status) {
                        case 0: setText("待审批"); break;
                        case 1: setText("已通过"); break;
                        case 2: setText("已入库"); break;
                        case 3: setText("已驳回"); break;
                        default: setText("未知");
                    }
                }
            }
        });

        stockInTable.setItems(stockInList);

        typeComboBox.getItems().addAll("全部", "采购入库", "退货入库", "其他");
        typeComboBox.setValue("全部");

        statusComboBox.getItems().addAll("全部", "待审批", "已通过", "已入库", "已驳回");
        statusComboBox.setValue("全部");

        loadStockInList();
    }

    private void loadStockInList() {
        try {
            String url = HttpRequestUtil.serverUrl + "/stock-in/list";

            String status = statusComboBox.getValue();
            if (status != null && !status.equals("全部")) {
                url += "?status=" + getStatusValue(status);
            }

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .headers("satoken", AppStore.getJwt().getToken())
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, Object> resultMap = gson.fromJson(response.body(), new TypeToken<Map<String, Object>>(){}.getType());
                if (resultMap.get("code").equals(200.0)) {
                    List<Map<String, Object>> dataList = (List<Map<String, Object>>) resultMap.get("data");
                    List<StockIn> list = gson.fromJson(gson.toJson(dataList), new TypeToken<List<StockIn>>(){}.getType());
                    stockInList.setAll(list);
                } else {
                    MessageDialog.showDialog("加载数据失败");
                }
            } else {
                MessageDialog.showDialog("请求失败，状态码：" + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            MessageDialog.showDialog("加载数据异常：" + e.getMessage());
        }
    }

    @FXML
    protected void onSearchButtonClick() {
        loadStockInList();
    }

    @FXML
    protected void onResetButtonClick() {
        searchCodeField.clear();
        typeComboBox.setValue("全部");
        statusComboBox.setValue("全部");
        loadStockInList();
    }

    @FXML
    protected void onAddButtonClick() {
        MessageDialog.showDialog("新增功能开发中");
    }

    @FXML
    protected void onEditButtonClick() {
        MessageDialog.showDialog("入库单创建后不可编辑，只能审批");
    }

    @FXML
    protected void onDeleteButtonClick() {
        StockIn selected = stockInTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            MessageDialog.showDialog("请选择要删除的入库单");
            return;
        }
        if (selected.getStatus() != 0) {
            MessageDialog.showDialog("只能删除待审批状态的入库单");
            return;
        }

        int ret = MessageDialog.choiceDialog("确认删除入库单 " + selected.getInCode() + "？");
        if (ret != MessageDialog.CHOICE_YES) return;

        MessageDialog.showDialog("后端未提供删除接口");
    }

    @FXML
    protected void onApproveButtonClick() {
        StockIn selected = stockInTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            MessageDialog.showDialog("请选择要审批的入库单");
            return;
        }
        if (selected.getStatus() != 0) {
            MessageDialog.showDialog("只能审批待审批状态的入库单");
            return;
        }

        int ret = MessageDialog.choiceDialog("确认批准入库单 " + selected.getInCode() + "？");
        if (ret != MessageDialog.CHOICE_YES) return;

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("stockInId", selected.getId());
            requestBody.put("approved", true);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(HttpRequestUtil.serverUrl + "/stock-in/approve"))
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                    .headers("Content-Type", "application/json", "satoken", AppStore.getJwt().getToken())
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                MessageDialog.showDialog("审批成功");
                loadStockInList();
            } else {
                MessageDialog.showDialog("审批失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            MessageDialog.showDialog("审批异常：" + e.getMessage());
        }
    }

    @FXML
    protected void onCompleteButtonClick() {
        StockIn selected = stockInTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            MessageDialog.showDialog("请选择要入库的入库单");
            return;
        }
        if (selected.getStatus() != 1) {
            MessageDialog.showDialog("只能入库已通过状态的入库单");
            return;
        }

        int ret = MessageDialog.choiceDialog("确认入库 " + selected.getInCode() + "？");
        if (ret != MessageDialog.CHOICE_YES) return;

        MessageDialog.showDialog("后端未提供入库完成接口");
    }

    private Integer getStatusValue(String status) {
        switch (status) {
            case "待审批": return 0;
            case "已通过": return 1;
            case "已入库": return 2;
            case "已驳回": return 3;
            default: return null;
        }
    }
}
