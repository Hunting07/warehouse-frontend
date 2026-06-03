package com.teach.javafx.controller.base;

import com.teach.javafx.MainApplication;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * ChoiceController 确认对话框交互控制类 对应 base/choice-dialog.fxml
 * @FXML 属性对应fxml文件中的
 * @FXML 方法对应于fxml文件中的 on***Click的属性
 */
public class ChoiceController {
    @FXML
    private BorderPane rootPane;
    @FXML
    private Label iconLabel;
    @FXML
    private Label messageLabel;
    @FXML
    private Button yesButton;
    @FXML
    private Button noButton;
    @FXML
    private Button cancelButton;

    private Stage stage;
    private int choice = 0;

    /**
     * 页面加载对象创建完成初始化方法，页面中控件属性的设置，初始数据显示等初始操作都在这里完成，其他代码都在事件处理方法里
     */
    @FXML
    public void initialize() {
        // 设置对话框圆角
        if (rootPane != null) {
            rootPane.setStyle(
                    "-fx-background-color: white; " +
                            "-fx-background-radius: 12;"
            );
        }
        if (stage != null && stage.getScene() != null) {
            stage.getScene().getRoot().setStyle("-fx-background-color: transparent;");
        }
    }

    @FXML
    public void cancelButtonClick(){
        choice = MessageDialog.CHOICE_CANCEL;
        if (stage != null) {
            stage.close();
        }
    }

    @FXML
    public void yesButtonClick(){
        choice = MessageDialog.CHOICE_YES;
        if (stage != null) {
            stage.close();
        }
    }

    @FXML
    public void noButtonClick(){
        choice = MessageDialog.CHOICE_NO;
        if (stage != null) {
            stage.close();
        }
    }


    public Stage getStage() {
        return stage;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public int choiceDialog(String msg) {
        // 根据消息内容判断图标
        if (msg.contains("驳回") || msg.contains("拒绝") || msg.contains("删除")) {
            iconLabel.setText("❌");
        } else if (msg.contains("通过") || msg.contains("批准") || msg.contains("同意")) {
            iconLabel.setText("✅");
        } else if (msg.contains("警告") || msg.contains("注意") || msg.contains("确认")) {
            iconLabel.setText("⚠️");
        } else {
            iconLabel.setText("❓");
        }

        messageLabel.setText(msg);
        this.stage.showAndWait();
        return choice;
    }
}
