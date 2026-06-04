package com.teach.javafx.controller.base;

import com.teach.javafx.request.HttpRequestUtil;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.DataResponse;

/**
 * PasswordController 登录交互控制类 对应 base/password-panel.fxml
 *  @FXML  属性 对应fxml文件中的
 *  @FXML 方法 对应于fxml文件中的 on***Click的属性
 */
public class PasswordController {
    @FXML
    private PasswordField oldPasswordField;
    @FXML
    private PasswordField newPasswordField;
    @FXML
    private PasswordField confirmPasswordField;

    /**
     * 点击 确认按钮 执行 onSubmitButtonClick方法，请求后台修改密码
     */
    @FXML
    protected void onSubmitButtonClick() {
        DataRequest request= new DataRequest();
        String oldPassword = oldPasswordField.getText().trim();
        String newPassword = newPasswordField.getText().trim();
        String confirmPassword = confirmPasswordField.getText().trim();

        if(oldPassword.length() == 0 || newPassword.length() == 0 || confirmPassword.length() == 0) {
            MessageDialog.showDialog("密码输入为空，不能修改！");
            return;
        }

        System.out.println("新密码: [" + newPassword + "] 长度: " + newPassword.length());
        System.out.println("确认密码: [" + confirmPassword + "] 长度: " + confirmPassword.length());

        if(!newPassword.equals(confirmPassword)) {
            MessageDialog.showDialog("两次输入的新密码不一致");
            return;
        }

        request.put("oldPassword", oldPassword);
        request.put("newPassword", newPassword);
        request.put("confirmPassword", confirmPassword);
        DataResponse res = HttpRequestUtil.request("/api/base/updatePassword", request);
        if(res.getCode() == 0) {
            MessageDialog.showDialog("修改成功！");
        }else {
            MessageDialog.showDialog(res.getMsg());
        }
    }
}
