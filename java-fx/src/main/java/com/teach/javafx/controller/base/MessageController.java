package com.teach.javafx.controller.base;

import com.teach.javafx.MainApplication;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

public class MessageController {
    @FXML
    private TextFlow textFLow;

    private Stage stage;

    @FXML
    public void initialize() {
        textFLow.setLineSpacing(9);
        textFLow.setDisable(false);
        textFLow.setStyle("-fx-padding: 0;");
    }


    @FXML
    public void okButtonClick(){
        MainApplication.setCanClose(true);
        stage.close();
    }
    
    public Stage getStage() {
        return stage;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }
    
    public void showDialog(String msg) {
        textFLow.getChildren().clear();
        
        // 检查是否是库存警告消息
        if (msg.contains("物资") && msg.contains("出库后库存将低于安全库存")) {
            showInventoryWarning(msg);
        } else {
            // 普通消息（如"批准成功"），使用更大字号
            Text text = new Text(msg);
            text.setFill(Color.web("#2c3e50"));
            text.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 22));
            text.setTextOrigin(javafx.geometry.VPos.TOP);
            text.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            textFLow.getChildren().add(text);
        }
        
        this.stage.show();
    }
    
    /**
     * 显示库存警告消息，使用紧凑优化的排版样式
     */
    private void showInventoryWarning(String msg) {
        // 解析消息内容
        // 格式: "物资 黑色中性笔 出库后库存将低于安全库存（当前：10，出库：1，安全库存：20，剩余：9），禁止出库"
        
        String currentStock = "";
        String outQuantity = "";
        String safeStock = "";
        String remaining = "";
        
        // 提取括号内的数据
        int bracketStart = msg.indexOf("（");
        int bracketEnd = msg.indexOf("）");
        if (bracketStart >= 0 && bracketEnd > bracketStart) {
            String bracketContent = msg.substring(bracketStart + 1, bracketEnd);
            
            // 解析各个字段
            String[] parts = bracketContent.split("，");
            for (String part : parts) {
                part = part.trim();
                if (part.startsWith("当前：")) {
                    currentStock = part.substring(3);
                } else if (part.startsWith("出库：")) {
                    outQuantity = part.substring(3);
                } else if (part.startsWith("安全库存：")) {
                    safeStock = part.substring(5);
                } else if (part.startsWith("剩余：")) {
                    remaining = part.substring(3);
                }
            }
        }
        
        // 1. 禁止出库警告（最上面，26px，红色加粗）
        Text warningText = new Text("禁止出库\n");
        warningText.setFill(Color.web("#e74c3c"));
        warningText.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 26));
        warningText.setTextOrigin(javafx.geometry.VPos.TOP);
        textFLow.getChildren().add(warningText);
        
        // 2. 库存信息合并成一行（16px，黑色加粗）
        String detailLine = "出库后库存低于安全库存 (当前：" + currentStock + 
                           "，本次出库：" + outQuantity + 
                           "，安全库存：" + safeStock + 
                           "，出库剩余：" + remaining + ")";
        
        Text detailText = new Text(detailLine);
        detailText.setFill(Color.BLACK);
        detailText.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 16));
        detailText.setTextOrigin(javafx.geometry.VPos.TOP);
        textFLow.getChildren().add(detailText);
    }
}
