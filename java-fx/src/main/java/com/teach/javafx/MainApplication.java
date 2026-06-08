package com.teach.javafx;

import com.teach.javafx.request.HttpRequestUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * 应用的主程序 MainApplication 按照编程规范，需继承Application 重写start 方法 主方法调用Application的launch() 启动程序
 */
public class MainApplication extends Application {
    /**
     * 加载登录对话框，设置登录Scene到Stage,显示该场景
     */
    private static Stage mainStage;

    private static boolean canClose=true;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("base/login-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1200, 650);
        scene.getStylesheets().add(getClass().getResource("/styles/modern-style.css").toExternalForm());

        stage.setTitle("登录");
        stage.setScene(scene);
        stage.setResizable(true);
        stage.show();
        stage.setOnCloseRequest(event -> {
            if(canClose) {
                HttpRequestUtil.close();
            }else {
                event.consume();
            }
        });
        mainStage = stage;
    }



    /**
     * 给舞台设置新的Scene
     * @param name 标题
     * @param scene 新的场景对象
     */

    public static void resetStage(String name, Scene scene) {
        mainStage.setTitle(name);
        mainStage.setScene(scene);
        // 先取消最大化，设置固定尺寸
        mainStage.setMaximized(false);
        mainStage.setWidth(1200);
        mainStage.setHeight(800);
        // 等待场景加载完成后最大化，避免闪烁和黑块
        javafx.application.Platform.runLater(() -> {
            mainStage.setMaximized(true);
        });
        mainStage.show();
    }


    public static void main(String[] args) {
        launch();
    }

    public static Stage getMainStage() {
        return mainStage;
    }

    public static void setCanClose(boolean canClose) {
        MainApplication.canClose = canClose;
    }
}