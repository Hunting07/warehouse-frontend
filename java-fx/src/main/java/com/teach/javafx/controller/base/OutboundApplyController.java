package com.teach.javafx.controller.base;

import com.teach.javafx.AppStore;
import com.teach.javafx.bean.OutOrderDetail;
import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.DataResponse;
import com.teach.javafx.request.HttpRequestUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.BigDecimalStringConverter;
import javafx.util.converter.IntegerStringConverter;
import javafx.event.ActionEvent;

import java.math.BigDecimal;

public class OutboundApplyController {

    @FXML
    private TextField remarkField;
    @FXML
    private TextField applicantIdField;
    @FXML
    private TextField applicantNameField;
    @FXML
    private TableView<OutOrderDetail> detailTableView;
    @FXML
    private TableColumn<OutOrderDetail, String> goodsNameCol;
    @FXML
    private TableColumn<OutOrderDetail, String> goodsSpecCol;
    @FXML
    private TableColumn<OutOrderDetail, String> unitCol;
    @FXML
    private TableColumn<OutOrderDetail, Integer> outNumCol;
    @FXML
    private TableColumn<OutOrderDetail, BigDecimal> unitPriceCol;
    @FXML
    private TableColumn<OutOrderDetail, BigDecimal> totalPriceCol;

    private final ObservableList<OutOrderDetail> detailList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        goodsNameCol.setCellValueFactory(new PropertyValueFactory<>("goodsName"));
        goodsSpecCol.setCellValueFactory(new PropertyValueFactory<>("goodsSpec"));
        unitCol.setCellValueFactory(new PropertyValueFactory<>("unit"));
        outNumCol.setCellValueFactory(new PropertyValueFactory<>("outNum"));
        unitPriceCol.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        totalPriceCol.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));

        detailTableView.setEditable(true);
        goodsNameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        goodsSpecCol.setCellFactory(TextFieldTableCell.forTableColumn());
        unitCol.setCellFactory(TextFieldTableCell.forTableColumn());
        outNumCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        unitPriceCol.setCellFactory(TextFieldTableCell.forTableColumn(new BigDecimalStringConverter()));

        outNumCol.setOnEditCommit(event -> {
            OutOrderDetail detail = event.getRowValue();
            detail.setOutNum(event.getNewValue());
            if(detail.getUnitPrice() != null) {
                detail.setTotalPrice(detail.getUnitPrice().multiply(new BigDecimal(detail.getOutNum())));
            }
            detailTableView.refresh();
        });

        unitPriceCol.setOnEditCommit(event -> {
            OutOrderDetail detail = event.getRowValue();
            detail.setUnitPrice(event.getNewValue());
            detail.setTotalPrice(detail.getUnitPrice().multiply(new BigDecimal(detail.getOutNum())));
            detailTableView.refresh();
        });

        detailTableView.setItems(detailList);
        
        loadUserInfo();
    }
    
    private void loadUserInfo() {
        try {
            if (AppStore.getJwt() != null && AppStore.getJwt().getUsername() != null) {
                applicantNameField.setText(AppStore.getJwt().getUsername());
            }
            if (AppStore.getJwt() != null && AppStore.getJwt().getId() != null) {
                applicantIdField.setText(String.valueOf(AppStore.getJwt().getId()));
            }
        } catch (Exception e) {
            System.err.println("加载用户信息失败: " + e.getMessage());
        }
    }

    // ===================== 按钮方法 =====================
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

    // ===================== 业务逻辑 =====================
    public void addGoods() {
        OutOrderDetail detail = new OutOrderDetail();
        detail.setGoodsName("请输入商品名称");
        detail.setGoodsSpec("默认规格");
        detail.setUnit("件");
        detail.setOutNum(1);
        detail.setUnitPrice(BigDecimal.ZERO);
        detail.setTotalPrice(BigDecimal.ZERO);
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
        
        String applicantId = applicantIdField.getText();
        String applicantName = applicantNameField.getText();
        
        if (applicantId == null || applicantId.trim().isEmpty()) {
            MessageDialog.showDialog("申请人ID不能为空");
            return;
        }

        DataRequest req = new DataRequest();
        req.put("applicantId", Integer.parseInt(applicantId));
        req.put("applicantName", applicantName);
        req.put("remark", remarkField.getText());
        req.put("detailList", detailList);

        DataResponse res = HttpRequestUtil.request("/api/outOrder/create", req);

        if (res != null && (res.getCode() == 0 || res.getCode() == 200)) {
            detailList.clear();
            remarkField.clear();
            MessageDialog.showDialog("提交成功！");
        } else {
            MessageDialog.showDialog("提交失败！");
        }
    }
}