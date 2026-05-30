package com.teach.javafx.controller.base;

import com.teach.javafx.AppStore;
import com.teach.javafx.MainApplication;
import com.teach.javafx.request.HttpRequestUtil;
import com.teach.javafx.request.MyTreeNode;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.DataResponse;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainFrameController {
    class ChangePanelHandler implements EventHandler<ActionEvent> {
        @Override
        public void handle(ActionEvent actionEvent) {
            changeContent(actionEvent);
        }
    }
    private final Map<String,Tab> tabMap = new HashMap<>();
    private final Map<String,Scene> sceneMap = new HashMap<>();
    private final Map<String,ToolController> controlMap =new HashMap<>();
    @FXML
    private MenuBar menuBar;
    @FXML
    private TreeView<MyTreeNode> menuTree;
    @FXML
    protected TabPane contentTabPane;
    @FXML
    @SuppressWarnings("unused")
    private Label systemPrompt;

    void addMenuItems(Menu parent, List<Map<String, Object>> mList) {
        String name, title;
        Menu menu;
        MenuItem item;
        for (Map<String, Object> m : mList) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> sList = (List<Map<String, Object>>) m.get("sList");
            name = (String) m.get("name");
            title = (String) m.get("title");
            if (sList == null || sList.isEmpty()) {
                item = new MenuItem();
                item.setId(name);
                item.setText(title);
                item.setOnAction(this::changeContent);
                parent.getItems().add(item);
            } else {
                menu = new Menu();
                menu.setText(title);
                addMenuItems(menu, sList);
                parent.getItems().add(menu);
            }
        }
    }


    public void initMenuBar(List<Map<String, Object>> mList) {
        Menu menu;
        List<Map<String, Object>> sList;
        for (Map<String, Object> m : mList) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> list = (List<Map<String, Object>>) m.get("sList");
            sList = list;
            menu = new Menu();
            menu.setText((String) m.get("title"));
            if (sList != null && !sList.isEmpty()) {
                addMenuItems(menu, sList);
            }
            menuBar.getMenus().add(menu);
        }
    }

    void addMenuItems(TreeItem<MyTreeNode> parent, List<Map<String, Object>> mList) {
        List<Map<String, Object>> sList;
        TreeItem<MyTreeNode> menu;
        for (Map<String, Object> m : mList) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> list = (List<Map<String, Object>>) m.get("sList");
            sList = list;
            String name = (String) m.get("name");
            String title = (String) m.get("title");

            if ("system".equals(name) || "系统管理".equals(title)) {
                continue;
            }

            menu = new TreeItem<>(new MyTreeNode(null, name, title, 0));
            parent.getChildren().add(menu);
            if (sList != null && !sList.isEmpty()) {
                addMenuItems(menu, sList);
            }
        }
    }

    public void initMenuTree(List<Map<String, Object>> mList) {
        MyTreeNode node = new MyTreeNode(null, null, "菜单", 0);
        TreeItem<MyTreeNode> root = new TreeItem<>(node);
        TreeItem<MyTreeNode> menu;

        for (Map<String, Object> m : mList) {
            String name = (String) m.get("name");
            String title = (String) m.get("title");

            if ("system".equals(name) || "系统管理".equals(title)) {
                continue;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> sList = (List<Map<String, Object>>) m.get("sList");
            Object isLeftObj = m.get("isLeft");
            int isLeft = isLeftObj instanceof Number ? ((Number) isLeftObj).intValue() : 0;

            if (isLeft == 1) {
                menu = new TreeItem<>(new MyTreeNode(null, name, title, isLeft));
                if (sList != null && !sList.isEmpty()) {
                    addMenuItems(menu, sList);
                }
                root.getChildren().add(menu);
            }
        }

        menuTree.setRoot(root);
        menuTree.setShowRoot(false);
        menuTree.addEventFilter(MouseEvent.MOUSE_CLICKED, new EventHandler<>() {
            public void handle(MouseEvent event) {
                TreeItem<MyTreeNode> treeItem = menuTree.getSelectionModel().getSelectedItem();

                if (treeItem == null) {
                    return;
                }

                MyTreeNode menu = treeItem.getValue();

                if (menu == null) {
                    return;
                }

                String menuName = menu.getValue();

                if (menuName == null || menuName.isEmpty()) {
                    return;
                }
                if ("logout".equals(menuName)) {
                    logout();
                } else if (menuName.endsWith("Command")) {
                    try {
                        Method m = this.getClass().getMethod(menuName);
                        m.invoke(this);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    changeContent(menuName, menu.getLabel());
                }
            }
        });
    }

    @FXML
    public void initialize() {
        @SuppressWarnings("unused")
        ChangePanelHandler handler = new ChangePanelHandler();

        DataRequest req = new DataRequest();
        DataResponse res = HttpRequestUtil.request("/api/base/getMenuList", req);

        if (res == null || res.getCode() != 200 || res.getData() == null) {
            return;
        }

        List<Map<String, Object>> mList = (List<Map<String, Object>>) res.getData();

        initMenuTree(mList);

        String role = AppStore.getJwt().getRole();
        addUserCenterToTree(role);

        contentTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        contentTabPane.setStyle("-fx-background-image: url('shanda1.jpg'); -fx-background-repeat: no-repeat; -fx-background-size: cover;");
    }

    private void addUserCenterToTree(String role) {
        TreeItem<MyTreeNode> root = menuTree.getRoot();
        if (root == null) {
            return;
        }

        for (TreeItem<MyTreeNode> item : new java.util.ArrayList<>(root.getChildren())) {
            MyTreeNode node = item.getValue();
            if (node != null && node.getValue() != null && node.getValue().contains("profile")) {
                root.getChildren().remove(item);
            }
        }

        TreeItem<MyTreeNode> profileItem = new TreeItem<>(
                new MyTreeNode(null, "base/profile-panel", "个人中心", 0)
        );
        root.getChildren().add(profileItem);
    }

    protected void logout() {
        AppStore.setJwt(null);
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("base/login-view.fxml"));
        try {
            Scene scene = new Scene(fxmlLoader.load(), 320, 240);
            MainApplication.loginStage("Login", scene);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void changeContent(ActionEvent ae) {
        Object obj = ae.getSource();
        String name = null, title = null;
        if (obj instanceof MenuItem item) {
            name = item.getId();
            title = item.getText();
        }
        if (name == null)
            return;
        changeContent(name, title);
    }

    public void changeContent(String name, String title) {
        if (name == null || name.isEmpty())
            return;

        String fxmlPath = name;

        if ("material".equals(name)) {
            fxmlPath = "/view/MaterialView";
        } else if ("category".equals(name)) {
            fxmlPath = "/view/CategoryView";
        } else if ("stock-warning".equals(name) || "warning".equals(name)) {
            fxmlPath = "/view/StockWarningView";
        } else if (name.contains("stockin")) {
            fxmlPath = "/com/teach/javafx/base/stockin-panel";
        } else if (name.contains("stockout") || name.contains("outbound")) {
            fxmlPath = "/com/teach/javafx/base/outbound-panel";
        } else if (name.contains("outorder")) {
            fxmlPath = "/com/teach/javafx/base/outorder-list-panel";
        } else if (name.contains("user-audit") || name.contains("user-approve")) {
            fxmlPath = "/com/teach/javafx/base/user-audit";
        } else if (name.contains("profile")) {
            fxmlPath = "/com/teach/javafx/base/profile-panel";
        } else if (name.contains("password")) {
            fxmlPath = "/com/teach/javafx/base/password-panel";
        } else if (name.contains("dictionary")) {
            fxmlPath = "/com/teach/javafx/base/dictionary-panel";
        } else if (name.contains("apply")) {
            fxmlPath = "/com/teach/javafx/base/outbound-apply";
        }

        Tab tab = tabMap.get(fxmlPath);
        Scene scene;
        Object c;
        if (tab == null) {
            scene = sceneMap.get(fxmlPath);
            if (scene == null) {
                URL fxmlUrl = MainApplication.class.getResource(fxmlPath + ".fxml");

                if (fxmlUrl == null) {
                    showError("找不到页面文件", fxmlPath + ".fxml");
                    return;
                }

                FXMLLoader fxmlLoader = new FXMLLoader(fxmlUrl);
                try {
                    scene = new Scene(fxmlLoader.load(), 1024, 768);
                    sceneMap.put(fxmlPath, scene);
                } catch (IOException e) {
                    System.err.println("加载 FXML 失败: " + e.getMessage());
                    e.printStackTrace();
                    showError("加载失败", e.getMessage());
                    return;
                }
                c = fxmlLoader.getController();
                if (c instanceof ToolController) {
                    controlMap.put(fxmlPath, (ToolController) c);
                }
            }

            tab = new Tab(title);
            tab.setId(fxmlPath);
            tab.setOnSelectionChanged(this::tabSelectedChanged);
            tab.setOnClosed(this::tabOnClosed);
            tab.setContent(scene.getRoot());
            contentTabPane.getTabs().add(tab);
            tabMap.put(fxmlPath, tab);
        }
        contentTabPane.getSelectionModel().select(tab);
    }

    private void showError(String title, String message) {
        javafx.application.Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public void tabSelectedChanged(Event e) {
        Tab tab = (Tab) e.getSource();
        String name = tab.getId();
        ToolController c = controlMap.get(name);
        if (c != null)
            c.doRefresh();
    }

    public void tabOnClosed(Event e) {
        Tab tab = (Tab) e.getSource();
        String name = tab.getId();

        Object controller = controlMap.get(name);
        if (controller instanceof StockWarningController) {
            ((StockWarningController) controller).cleanup();
        }

        contentTabPane.getTabs().remove(tab);
        tabMap.remove(name);
        controlMap.remove(name);
    }

    public ToolController getCurrentToolController() {
        for (String name : controlMap.keySet()) {
            Tab tab = tabMap.get(name);
            if (tab != null && tab.isSelected()) {
                return controlMap.get(name);
            }
        }
        return null;
    }

    @SuppressWarnings("unused")
    protected void doNewCommand() {
        ToolController c = getCurrentToolController();
        if (c != null) {
            c.doNew();
        }
    }

    @SuppressWarnings("unused")
    protected void doSaveCommand() {
        ToolController c = getCurrentToolController();
        if (c != null) {
            c.doSave();
        }
    }

    @SuppressWarnings("unused")
    protected void doDeleteCommand() {
        ToolController c = getCurrentToolController();
        if (c != null) {
            c.doDelete();
        }
    }

    @SuppressWarnings("unused")
    protected void doPrintCommand() {
        ToolController c = getCurrentToolController();
        if (c != null) {
            c.doPrint();
        }
    }

    @SuppressWarnings("unused")
    protected void doExportCommand() {
        ToolController c = getCurrentToolController();
        if (c != null) {
            c.doExport();
        }
    }

    @SuppressWarnings("unused")
    protected void doImportCommand() {
        ToolController c = getCurrentToolController();
        if (c != null) {
            c.doImport();
        }
    }

    @SuppressWarnings("unused")
    protected void doTestCommand() {
        ToolController c = getCurrentToolController();
        if (c != null) {
            c.doTest();
        }
    }

    @SuppressWarnings("unused")
    public ToolController getToolController(String name) {
        return controlMap.get(name);
    }

}
