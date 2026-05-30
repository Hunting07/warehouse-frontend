package com.teach.javafx.controller.base;

import com.google.gson.Gson;
import com.teach.javafx.AppStore;
import com.teach.javafx.bean.OutOrderDetail;
import com.teach.javafx.request.HttpRequestUtil;
import com.teach.javafx.request.OptionItem;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.IntegerStringConverter;
import javafx.event.ActionEvent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OutboundApplyController {

    @FXML
    private TextField remarkField;
    @FXML
    private TextField applicantNameField;
    @FXML
    private ComboBox<String> outTypeComboBox;
    @FXML
    private TableView<OutOrderDetail> detailTableView;
    @FXML
    private TableColumn<OutOrderDetail, String> materialCol;
    @FXML
    private TableColumn<OutOrderDetail, String> goodsSpecCol;
    @FXML
    private TableColumn<OutOrderDetail, String> unitCol;
    @FXML
    private TableColumn<OutOrderDetail, Integer> outNumCol;

    private final ObservableList<OutOrderDetail> detailList = FXCollections.observableArrayList();
    private final List<OptionItem> materialList = new ArrayList<>();
    private final List<Map<String, Object>> materialMapList = new ArrayList<>();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    @FXML
    public void initialize() {
        materialCol.setCellValueFactory(new PropertyValueFactory<>("goodsName"));
        goodsSpecCol.setCellValueFactory(new PropertyValueFactory<>("goodsSpec"));
        unitCol.setCellValueFactory(new PropertyValueFactory<>("unit"));
        outNumCol.setCellValueFactory(new PropertyValueFactory<>("outNum"));

        detailTableView.setEditable(true);
        outNumCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));

        materialCol.setCellFactory(param -> new TableCell<OutOrderDetail, String>() {
            private final Button selectBtn = new Button("请选择物资");

            {
                selectBtn.setStyle("-fx-background-color: #409eff; -fx-text-fill: white; -fx-font-size: 12px;");
                selectBtn.setOnAction(event -> {
                    OutOrderDetail detail = getTableView().getItems().get(getIndex());
                    showMaterialSelectionDialog(detail);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    if (item != null && !item.equals("请选择物资")) {
                        selectBtn.setText(item);
                    } else {
                        selectBtn.setText("请选择物资");
                    }
                    setGraphic(selectBtn);
                }
            }
        });

        outNumCol.setOnEditCommit(event -> {
            OutOrderDetail detail = event.getRowValue();
            detail.setOutNum(event.getNewValue());
            detailTableView.refresh();
        });

        detailTableView.setItems(detailList);

        initOutTypeComboBox();
        loadUserInfo();
        loadMaterialList();
    }

    private void initOutTypeComboBox() {
        outTypeComboBox.getItems().addAll("领料出库", "销售出库", "报损出库", "其他出库");
        outTypeComboBox.setValue("领料出库");
    }

    private void loadUserInfo() {
        try {
            if (AppStore.getJwt() != null && AppStore.getJwt().getUsername() != null) {
                applicantNameField.setText(AppStore.getJwt().getUsername());
                applicantNameField.setEditable(false);
            }
        } catch (Exception e) {
            System.err.println("加载用户信息失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadMaterialList() {
        new Thread(() -> {
            try {
                System.out.println("=== OutboundApplyController 开始加载物资列表 ===");
                String url = HttpRequestUtil.serverUrl + "/api/material/list";
                System.out.println("请求URL: " + url);

                // 使用 POST 请求，发送空的 JSON 对象
                String requestBody = "{}";
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .headers("Content-Type", "application/json")
                        .headers("satoken", AppStore.getJwt().getToken())
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                System.out.println("响应状态码: " + response.statusCode());
                System.out.println("响应内容: " + response.body());

                if (response.statusCode() == 200) {
                    Map<String, Object> result = gson.fromJson(response.body(), Map.class);

                    if (result.get("code").equals(200.0) || result.get("code").equals(0)) {
                        Object dataObj = result.get("data");

                        final List<Map<String, Object>> data;

                        if (dataObj instanceof List) {
                            // 格式1：直接返回 List
                            System.out.println("物资列表加载成功（格式1-直接List）");
                            data = (List<Map<String, Object>>) dataObj;
                        } else if (dataObj instanceof Map) {
                            // 格式2：分页数据
                            System.out.println("物资列表加载成功（格式2-分页数据）");
                            Map<String, Object> dataMap = (Map<String, Object>) dataObj;
                            data = (List<Map<String, Object>>) dataMap.get("records");
                        } else {
                            System.err.println("物资列表数据格式不正确");
                            data = null;
                        }

                        if (data != null) {
                            final List<Map<String, Object>> finalData = data;
                            Platform.runLater(() -> {
                                materialList.clear();
                                materialMapList.clear();

                                for (Map<String, Object> item : finalData) {
                                    OptionItem option = new OptionItem();
                                    option.setId(((Number) item.get("id")).intValue());
                                    option.setName((String) item.get("name"));
                                    materialList.add(option);
                                    materialMapList.add(item);
                                }

                                System.out.println("物资列表加载完成，共 " + materialList.size() + " 条记录");
                            });
                        } else {
                            System.err.println("物资列表数据为空");
                        }
                    } else {
                        System.err.println("接口返回错误：code=" + result.get("code") + ", msg=" + result.get("msg"));
                    }
                } else {
                    System.err.println("HTTP请求失败，状态码：" + response.statusCode());
                }
            } catch (Exception e) {
                System.err.println("加载物资列表失败：" + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> {
                    MessageDialog.showDialog("加载物资列表失败：" + e.getMessage());
                });
            }
        }).start();
    }

    private void showMaterialSelectionDialog(OutOrderDetail detail) {
        if (materialList.isEmpty()) {
            MessageDialog.showDialog("物资列表为空");
            return;
        }

        Dialog<OptionItem> dialog = new Dialog<>();
        dialog.setTitle("选择物资");
        dialog.setHeaderText("请选择要出库的物资");

        ListView<OptionItem> listView = new ListView<>();
        listView.getItems().addAll(materialList);
        listView.setCellFactory(lv -> new ListCell<OptionItem>() {
            @Override
            protected void updateItem(OptionItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });

        dialog.getDialogPane().setContent(listView);

        ButtonType selectButtonType = new ButtonType("选择", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(selectButtonType, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == selectButtonType) {
                return listView.getSelectionModel().getSelectedItem();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(selectedMaterial -> {
            detail.setGoodsName(selectedMaterial.getName());
            detail.setGoodsId(selectedMaterial.getId());

            for (Map<String, Object> mat : materialMapList) {
                if (((Number) mat.get("id")).intValue() == selectedMaterial.getId()) {
                    detail.setGoodsSpec((String) mat.getOrDefault("spec", "默认规格"));
                    detail.setUnit((String) mat.getOrDefault("unit", "件"));
                    Object priceObj = mat.get("price");
                    if (priceObj instanceof Number) {
                        detail.setUnitPrice(java.math.BigDecimal.valueOf(((Number) priceObj).doubleValue()));
                    }
                    break;
                }
            }
            detailTableView.refresh();
        });
    }

    @FXML
    public void onAdd(ActionEvent event) {
        addGoods();
    }

    @FXML
    public void onDelete(ActionEvent event) {
        deleteGoods();
    }

    @FXML
    public void onSubmit(ActionEvent event) {
        submitApply();
    }

    public void addGoods() {
        OutOrderDetail detail = new OutOrderDetail();
        detail.setGoodsName("请选择物资");
        detail.setGoodsSpec("默认规格");
        detail.setUnit("件");
        detail.setOutNum(1);
        detailList.add(detail);
    }

    public void deleteGoods() {
        OutOrderDetail selectedDetail = detailTableView.getSelectionModel().getSelectedItem();
        if (selectedDetail != null) {
            detailList.remove(selectedDetail);
        }
    }

    public void submitApply() {
        if (detailList.isEmpty()) {
            MessageDialog.showDialog("请添加商品");
            return;
        }

        String outType = outTypeComboBox.getValue();
        if (outType == null || outType.isEmpty()) {
            MessageDialog.showDialog("请选择出库类型");
            return;
        }

        for (OutOrderDetail detail : detailList) {
            if (detail.getGoodsId() == null) {
                MessageDialog.showDialog("请选择物资");
                return;
            }
            if (detail.getOutNum() == null || detail.getOutNum() <= 0) {
                MessageDialog.showDialog("出库数量必须大于0");
                return;
            }
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("outType", getOutTypeValue(outType));
        requestBody.put("remark", remarkField.getText());

        List<Map<String, Object>> items = new ArrayList<>();
        for (OutOrderDetail detail : detailList) {
            Map<String, Object> item = new HashMap<>();
            item.put("materialId", detail.getGoodsId());
            item.put("quantity", detail.getOutNum());
            if (detail.getUnitPrice() != null) {
                item.put("unitPrice", detail.getUnitPrice());
            }
            items.add(item);
        }
        requestBody.put("items", items);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(HttpRequestUtil.serverUrl + "/api/stockOut/submitApply"))
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                    .headers("Content-Type", "application/json", "satoken", AppStore.getJwt().getToken())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, Object> result = gson.fromJson(response.body(), Map.class);
                if (result.get("code").equals(200) || result.get("code").equals(0)) {
                    detailList.clear();
                    remarkField.clear();
                    MessageDialog.showDialog("提交成功！");
                } else {
                    MessageDialog.showDialog("提交失败：" + result.get("msg"));
                }
            } else {
                MessageDialog.showDialog("提交失败！");
            }
        } catch (Exception e) {
            e.printStackTrace();
            MessageDialog.showDialog("提交异常：" + e.getMessage());
        }
    }

    private Integer getOutTypeValue(String outType) {
        switch (outType) {
            case "领料出库": return 1;
            case "销售出库": return 2;
            case "报损出库": return 3;
            case "其他出库": return 4;
            default: return 0;
        }
    }
}
