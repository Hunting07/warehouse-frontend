package com.teach.javafx.controller.base;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 用户审批界面控制器
 * 对应 fxml/UserApprove.fxml 界面
 */
public class UserApproveController {

    // 绑定FXML里的组件
    @FXML
    private TextField searchField; // 搜索框
    @FXML
    private TableView<ApplyInfo> applyTable; // 审批表格
    @FXML
    private TableColumn<ApplyInfo, String> colApplyNo, colName, colRole, colTime, colStatus;
    @FXML
    private TableColumn<ApplyInfo, Void> colOperate;

    // 表格数据集合
    private final ObservableList<ApplyInfo> applyList = FXCollections.observableArrayList();
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * 界面初始化方法，打开界面时自动加载数据
     */
    @FXML
    public void initialize() {
        // 绑定表格列和数据模型
        colApplyNo.setCellValueFactory(new PropertyValueFactory<>("applyNo"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("applyTime"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // 给操作列添加「通过/驳回」按钮
        colOperate.setCellFactory(param -> new TableCell<>() {
            private final Button passBtn = new Button("通过");
            private final Button rejectBtn = new Button("驳回");
            private final HBox box = new HBox(10, passBtn, rejectBtn);

            {
                // 设置按钮样式
                passBtn.setStyle("-fx-background-color:#28a745;-fx-text-fill:white;-fx-pref-width:70;");
                rejectBtn.setStyle("-fx-background-color:#dc3545;-fx-text-fill:white;-fx-pref-width:70;");

                // 通过按钮点击事件
                passBtn.setOnAction(event -> {
                    ApplyInfo info = getTableView().getItems().get(getIndex());
                    handleApprove(info, 1); // 1代表通过
                });

                // 驳回按钮点击事件
                rejectBtn.setOnAction(event -> {
                    ApplyInfo info = getTableView().getItems().get(getIndex());
                    handleApprove(info, 2); // 2代表驳回
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    ApplyInfo info = getTableView().getItems().get(getIndex());
                    // 只有待审批的申请，才显示操作按钮
                    if ("待审批".equals(info.getStatus())) {
                        setGraphic(box);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });

        // 给表格绑定数据
        applyTable.setItems(applyList);
        // 初始化加载模拟数据
        loadApplyList();
    }

    /**
     * 加载申请列表（课设模拟数据，实际项目从后端接口获取）
     */
    @FXML
    public void loadApplyList() {
        applyList.clear();
        // 模拟3条申请数据
        applyList.add(new ApplyInfo(
                "AP16842356", "张三", "普通操作员",
                LocalDateTime.now().minusHours(2).format(timeFormatter), "待审批"
        ));
        applyList.add(new ApplyInfo(
                "AP16842198", "李四", "数据查看员",
                LocalDateTime.now().minusDays(1).format(timeFormatter), "待审批"
        ));
        applyList.add(new ApplyInfo(
                "AP16841872", "王五", "供应商账号",
                LocalDateTime.now().minusDays(2).format(timeFormatter), "已通过"
        ));
        applyList.add(new ApplyInfo(
                "AP16841534", "赵六", "仓储管理员",
                LocalDateTime.now().minusDays(3).format(timeFormatter), "已驳回"
        ));
    }

    /**
     * 搜索查询按钮点击事件
     */
    @FXML
    public void handleSearch() {
        String keyword = searchField.getText().trim();
        if (keyword.isBlank()) {
            loadApplyList();
            return;
        }
        // 模拟搜索过滤
        ObservableList<ApplyInfo> filterList = FXCollections.observableArrayList();
        for (ApplyInfo info : applyList) {
            if (info.getApplyNo().contains(keyword) || info.getName().contains(keyword)) {
                filterList.add(info);
            }
        }
        applyTable.setItems(filterList);
    }

    /**
     * 审批操作（通过/驳回）
     */
    private void handleApprove(ApplyInfo info, int status) {
        String statusText = status == 1 ? "通过" : "驳回";
        // 确认弹窗
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("审批确认");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText(String.format(
                "确认要%s申请单号【%s】的申请吗？", statusText, info.getApplyNo()
        ));
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // 更新状态
                info.setStatus(status == 1 ? "已通过" : "已驳回");
                // 刷新表格
                applyTable.refresh();
                // 成功提示
                showAlert(Alert.AlertType.INFORMATION, "操作成功",
                        String.format("申请【%s】已成功%s！", info.getApplyNo(), statusText));
            }
        });
    }

    /**
     * 通用弹窗提示方法
     */
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * 申请单数据模型（内部类，封装表格数据）
     */
    public static class ApplyInfo {
        private String applyNo;
        private String name;
        private String role;
        private String applyTime;
        private String status;

        public ApplyInfo(String applyNo, String name, String role, String applyTime, String status) {
            this.applyNo = applyNo;
            this.name = name;
            this.role = role;
            this.applyTime = applyTime;
            this.status = status;
        }

        // Getter和Setter方法，表格绑定必须要有
        public String getApplyNo() { return applyNo; }
        public void setApplyNo(String applyNo) { this.applyNo = applyNo; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getApplyTime() { return applyTime; }
        public void setApplyTime(String applyTime) { this.applyTime = applyTime; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
