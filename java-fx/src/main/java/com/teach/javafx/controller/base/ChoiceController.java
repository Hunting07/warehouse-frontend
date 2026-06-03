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
    private Label messageLabel;
    @FXML
    private Label warningLabel;
    @FXML
    private Button yesButton;
    @FXML
    private Button noButton;

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
        messageLabel.setText(msg);
        
        // 根据消息内容判断是否显示警告提示
        if (msg.contains("删除")) {
            warningLabel.setVisible(true);
            warningLabel.setManaged(true);
        } else {
            warningLabel.setVisible(false);
            warningLabel.setManaged(false);
        }
        
        this.stage.showAndWait();
        return choice;
    }
}
