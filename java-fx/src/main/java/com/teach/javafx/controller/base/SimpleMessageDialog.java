package com.teach.javafx.controller.base;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * 简洁风格的提示对话框
 */
public class SimpleMessageDialog extends Stage {

    public SimpleMessageDialog(String title, String message, MessageType type) {
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.DECORATED);
        setTitle(title);

        VBox root = new VBox(30);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(45, 55, 35, 55));
        root.setStyle("-fx-background-color: white; -fx-background-radius: 8;");

        // 消息文本
        Label messageLabel = new Label(message);
        messageLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-text-alignment: center;");
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(380);

        // 按钮
        Button confirmBtn = new Button("确认");
        confirmBtn.setStyle("-fx-background-color: #4a90d9; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 45; -fx-background-radius: 8; -fx-cursor: hand;");
        confirmBtn.setOnAction(e -> close());

        root.getChildren().addAll(messageLabel, confirmBtn);

        Scene scene = new Scene(root);
        scene.setFill(javafx.scene.paint.Color.WHITE);
        setScene(scene);

        // 按 ESC 键关闭
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                close();
            }
        });
    }

    /**
     * 显示信息提示
     */
    public static void showInfo(String message) {
        SimpleMessageDialog dialog = new SimpleMessageDialog("提示", message, MessageType.INFO);
        dialog.showAndWait();
    }

    /**
     * 显示警告提示
     */
    public static void showWarning(String message) {
        SimpleMessageDialog dialog = new SimpleMessageDialog("警告", message, MessageType.WARNING);
        dialog.showAndWait();
    }

    /**
     * 显示错误提示
     */
    public static void showError(String message) {
        SimpleMessageDialog dialog = new SimpleMessageDialog("错误", message, MessageType.ERROR);
        dialog.showAndWait();
    }

    /**
     * 显示成功提示
     */
    public static void showSuccess(String message) {
        SimpleMessageDialog dialog = new SimpleMessageDialog("成功", message, MessageType.SUCCESS);
        dialog.showAndWait();
    }

    /**
     * 消息类型枚举
     */
    public enum MessageType {
        INFO, WARNING, ERROR, SUCCESS
    }
}
