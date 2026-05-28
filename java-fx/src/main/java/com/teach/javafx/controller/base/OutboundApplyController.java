package com.teach.javafx.controller.base;

import com.google.gson.Gson;
import com.teach.javafx.AppStore;
import com.teach.javafx.bean.OutOrderDetail;
import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.DataResponse;
import com.teach.javafx.request.HttpRequestUtil;
import com.teach.javafx.request.OptionItem;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
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
    private TextField applicantIdField;
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
        System.out.println("========== OutboundApplyController 初始化开始 ==========");
        
        try {
            materialCol.setCellValueFactory(new PropertyValueFactory<>("goodsName"));
            goodsSpecCol.setCellValueFactory(new PropertyValueFactory<>("goodsSpec"));
            unitCol.setCellValueFactory(new PropertyValueFactory<>("unit"));
            outNumCol.setCellValueFactory(new PropertyValueFactory<>("outNum"));

            detailTableView.setEditable(true);
            outNumCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
            
            materialCol.setCellFactory(ComboBoxTableCell.forTableColumn(
                    FXCollections.observableArrayList()
            ));

            outNumCol.setOnEditCommit(event -> {
                OutOrderDetail detail = event.getRowValue();
                detail.setOutNum(event.getNewValue());
                detailTableView.refresh();
            });

            materialCol.setOnEditCommit(event -> {
                OutOrderDetail detail = event.getRowValue();
                String newName = event.getNewValue();
                detail.setGoodsName(newName);

                for (OptionItem opt : materialList) {
                    if (opt.getName().equals(newName)) {
                        detail.setGoodsId(opt.getId());
                        for (Map<String, Object> mat : materialMapList) {
                            if (((Number) mat.get("id")).intValue() == opt.getId()) {
                                detail.setGoodsSpec((String) mat.getOrDefault("spec", "默认规格"));
                                detail.setUnit((String) mat.getOrDefault("unit", "件"));
                                break;
                            }
                        }
                        break;
                    }
                }
                detailTableView.refresh();
            });

            detailTableView.setItems(detailList);
            
            System.out.println("UI 组件初始化完成");
            
            initOutTypeComboBox();
            System.out.println("出库类型下拉框初始化完成");
            
            loadUserInfo();
            System.out.println("用户信息加载完成");
            
            loadMaterialList();
            System.out.println("物资列表加载任务已启动");
            
            System.out.println("========== OutboundApplyController 初始化完成 ==========");
        } catch (Exception e) {
            System.err.println("初始化失败：" + e.getMessage());
            e.printStackTrace();
        }
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
            if (AppStore.getJwt() != null && AppStore.getJwt().getId() != null) {
                applicantIdField.setText(String.valueOf(AppStore.getJwt().getId()));
                applicantIdField.setEditable(false);
            }
        } catch (Exception e) {
            System.err.println("加载用户信息失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadMaterialList() {
        new Thread(() -> {
            try {
                String url = HttpRequestUtil.serverUrl + "/api/material/getMaterialList";
                System.out.println("请求物资列表: " + url);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .headers("satoken", AppStore.getJwt().getToken())
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                System.out.println("物资列表响应状态: " + response.statusCode());

                if (response.statusCode() == 200) {
                    Map<String, Object> result = gson.fromJson(response.body(), Map.class);
                    System.out.println("物资列表响应数据: " + result);
                    
                    if (result.get("code").equals(200) || result.get("code").equals(0)) {
                        Map<String, Object> dataMap = (Map<String, Object>) result.get("data");
                        List<Map<String, Object>> data = (List<Map<String, Object>>) dataMap.get("records");
                        
                        Platform.runLater(() -> {
                            materialList.clear();
                            materialMapList.clear();
                            
                            if (data != null) {
                                materialMapList.addAll(data);
                                for (Map<String, Object> item : data) {
                                    OptionItem option = new OptionItem();
                                    option.setId(((Number) item.get("id")).intValue());
                                    option.setName((String) item.get("materialName"));
                                    materialList.add(option);
                                }
                            }
                            
                            updateMaterialComboBox();
                            System.out.println("物资列表加载完成，共 " + materialList.size() + " 条");
                        });
                    }
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

    private void updateMaterialComboBox() {
        List<String> names = new ArrayList<>();
        for (OptionItem item : materialList) {
            names.add(item.getName());
        }
        
        materialCol.setCellFactory(ComboBoxTableCell.forTableColumn(
                FXCollections.observableArrayList(names)
        ));
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
        
        String applicantId = applicantIdField.getText();
        String applicantName = applicantNameField.getText();
        
        if (applicantId == null || applicantId.trim().isEmpty()) {
            MessageDialog.showDialog("申请人ID不能为空");
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
                    MessageDialog.showDialog("提交失败！" + result.get("msg"));
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