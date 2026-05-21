package com.teach.javafx.controller.base;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户申请界面控制器
 * 对应 fxml/UserApply.fxml 界面
 */
public class UserApplyController {

    // 绑定FXML里的组件ID，和上面的fxml文件一一对应
    @FXML
    private TextField accountField; // 申请账号输入框
    @FXML
    private TextField nameField; // 申请人姓名输入框
    @FXML
    private TextField phoneField; // 联系电话输入框
    @FXML
    private ComboBox<String> roleCombo; // 申请角色下拉框
    @FXML
    private CheckBox perm1, perm2, perm3, perm4, perm5, perm6; // 权限复选框
    @FXML
    private TextArea descArea; // 申请说明输入框

    /**
     * 提交申请按钮点击事件
     */
    @FXML
    public void handleSubmit() {
        // 1. 表单非空校验
        if (accountField.getText().isBlank() || nameField.getText().isBlank()
                || phoneField.getText().isBlank() || roleCombo.getValue() == null) {
            showAlert(AlertType.ERROR, "提交失败", "请填写完整的必填信息！");
            return;
        }

        // 2. 收集表单数据
        String account = accountField.getText().trim();
        String name = nameField.getText().trim();
        String phone = phoneField.getText().trim();
        String role = roleCombo.getValue();
        String desc = descArea.getText().trim();

        // 收集勾选的权限
        List<String> permissionList = new ArrayList<>();
        if (perm1.isSelected()) permissionList.add("商品信息管理");
        if (perm2.isSelected()) permissionList.add("入库单创建/编辑");
        if (perm3.isSelected()) permissionList.add("出库单创建/编辑");
        if (perm4.isSelected()) permissionList.add("库存数据查看");
        if (perm5.isSelected()) permissionList.add("库存预警设置");
        if (perm6.isSelected()) permissionList.add("供应商信息管理");
        String permissions = String.join(", ", permissionList);

        // 3. 生成申请单号（课设模拟，实际项目从后端获取）
        String applyNo = "AP" + System.currentTimeMillis();

        // 4. 提交成功提示
        String successMsg = String.format(
                "申请提交成功！\n申请单号：%s\n申请人：%s\n申请角色：%s\n申请权限：%s",
                applyNo, name, role, permissions.isBlank() ? "无" : permissions
        );
        showAlert(AlertType.INFORMATION, "提交成功", successMsg);

        // 5. 提交后重置表单
        handleReset();
    }

    /**
     * 重置表单按钮点击事件
     */
    @FXML
    public void handleReset() {
        accountField.clear();
        nameField.clear();
        phoneField.clear();
        roleCombo.setValue(null);
        perm1.setSelected(false);
        perm2.setSelected(false);
        perm3.setSelected(false);
        perm4.setSelected(false);
        perm5.setSelected(false);
        perm6.setSelected(false);
        descArea.clear();
    }

    /**
     * 通用弹窗提示方法
     */
    private void showAlert(AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}