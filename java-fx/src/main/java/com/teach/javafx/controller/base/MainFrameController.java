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
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.DataResponse;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MainFrameController {
    class ChangePanelHandler implements EventHandler<ActionEvent> {
        @Override
        public void handle(ActionEvent actionEvent) {
            changeContent(actionEvent);
        }
    }
    private final Map<String,Tab> tabMap = new HashMap<String,Tab>();
    private final Map<String,Scene> sceneMap = new HashMap<String,Scene>();
    private final Map<String,ToolController> controlMap =new HashMap<String,ToolController>();
    @FXML
    private MenuBar menuBar;
    @FXML
    private TreeView<MyTreeNode> menuTree;
    @FXML
    protected TabPane contentTabPane;
    @FXML
    private Label systemPrompt;

    private ChangePanelHandler handler= null;

    void addMenuItems(Menu parent, List<Map> mList) {
        String name, title;
        List sList;
        Map ms;
        Menu menu;
        MenuItem item;
        for ( Map m :mList) {
            sList = (List<Map>)m.get("sList");
            name = (String)m.get("name");
            title = (String)m.get("title");
            if(sList == null || sList.size()== 0) {
                item = new MenuItem();
                item.setId(name);
                item.setText(title);
                item.setOnAction(this::changeContent);
                parent.getItems().add(item);
            }else {
                menu = new Menu();
                menu.setText(title);
                addMenuItems(menu,sList);
                parent.getItems().add(menu);
            }
        }
    }

    public void addMenuItem(Menu menu, String name, String title){
        MenuItem item;
        item = new MenuItem();
        item.setText(title);
        item.setId(name);
        item.setOnAction(this::changeContent);
        menu.getItems().add(item);
    }

    public void initMenuBar(List<Map> mList){
        Menu menu;
        Map m;
        int i;
        List<Map> sList;
        for(i = 0; i < mList.size();i++) {
            m = mList.get(i);
            sList = (List<Map>)m.get("sList");
            menu = new Menu();
            menu.setText((String)m.get("title"));
            if(sList != null && sList.size()> 0) {
                addMenuItems(menu,sList);
            }
            menuBar.getMenus().add(menu);
        }
    }

    void addMenuItems( TreeItem<MyTreeNode> parent, List<Map> mList) {
        List sList;
        TreeItem<MyTreeNode> menu;
        for ( Map m :mList) {
            sList = (List<Map>)m.get("sList");
            menu = new TreeItem<>(new MyTreeNode(null,(String)m.get("name") ,(String)m.get("title"),0));
            parent.getChildren().add(menu);
            if(sList !=  null && sList.size()> 0) {
                addMenuItems(menu, sList);
            }
        }
    }

    public void initMenuTree(List<Map> mList) {
        String role = AppStore.getJwt().getRole();
        MyTreeNode node = new MyTreeNode(null, null, "菜单", 0);
        TreeItem<MyTreeNode> root = new TreeItem<>(node);
        TreeItem<MyTreeNode> menu;
        int i, j;
        Map m;
        List<Map> sList;

        // 先从后端返回的菜单中过滤出左侧菜单
        for (i = 0; i < mList.size(); i++) {
            m = mList.get(i);
            sList = (List<Map>) m.get("sList");
            Object isLeftObj = m.get("isLeft");
            int isLeft = isLeftObj instanceof Number ? ((Number) isLeftObj).intValue() : 0;

            // 只添加 isLeft=1 的菜单到左侧树
            if (isLeft == 1) {
                menu = new TreeItem<>(new MyTreeNode(null, (String) m.get("name"), (String) m.get("title"), isLeft));
                if (sList != null && sList.size() > 0) {
                    addMenuItems(menu, sList);
                }
                root.getChildren().add(menu);
            }
        }

        menuTree.setRoot(root);
        menuTree.setShowRoot(false);
        menuTree.addEventFilter(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            public void handle(MouseEvent event) {
                System.out.println("========== 树菜单点击调试 ==========");

                Node node = event.getPickResult().getIntersectedNode();
                TreeItem<MyTreeNode> treeItem = menuTree.getSelectionModel().getSelectedItem();
                System.out.println("treeItem: " + (treeItem == null ? "null" : treeItem.getValue()));

                if (treeItem == null) {
                    System.out.println("treeItem 为空，返回");
                    return;
                }

                MyTreeNode menu = treeItem.getValue();
                System.out.println("menu: " + (menu == null ? "null" : menu.getLabel()));

                if (menu == null) {
                    System.out.println("menu 为空，返回");
                    return;
                }

                String name = menu.getValue();
                System.out.println("name: " + name);
                System.out.println("label: " + menu.getLabel());
                System.out.println("========================================");

                if (name == null || name.length() == 0) {
                    System.out.println("name 为空，返回");
                    return;
                }
                if ("logout".equals(name)) {
                    logout();
                } else if (name.endsWith("Command")) {
                    try {
                        Method m = this.getClass().getMethod(name);
                        m.invoke(this);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    changeContent(name, menu.getLabel());
                }
            }
        });
    }

    @FXML
    public void initialize() {
        handler = new ChangePanelHandler();

        Menu inventoryMenu = new Menu("库存管理");

        MenuItem stockWarningItem = new MenuItem("库存预警");
        stockWarningItem.setId("view/StockWarningView");
        stockWarningItem.setText("库存预警");
        stockWarningItem.setOnAction(this::changeContent);
        inventoryMenu.getItems().add(stockWarningItem);

        menuBar.getMenus().add(inventoryMenu);

        Menu userMenu = new Menu("用户中心");

        MenuItem profileItem = new MenuItem("个人中心");
        profileItem.setId("base/profile-panel");
        profileItem.setText("个人中心");
        profileItem.setOnAction(this::changeContent);
        userMenu.getItems().add(profileItem);

        String role = AppStore.getJwt().getRole();
        if ("admin".equals(role)) {
            MenuItem auditItem = new MenuItem("用户审批");
            auditItem.setId("base/user-audit");
            auditItem.setText("用户审批");
            auditItem.setOnAction(this::changeContent);
            userMenu.getItems().add(auditItem);
        }

        MenuItem logoutItem = new MenuItem("退出登录");
        logoutItem.setId("logout");
        logoutItem.setText("退出登录");
        logoutItem.setOnAction(this::changeContent);
        userMenu.getItems().add(logoutItem);

        menuBar.getMenus().add(userMenu);

        DataRequest req = new DataRequest();
        DataResponse res = HttpRequestUtil.request("/api/base/getMenuList", req);

        if (res == null || res.getCode() != 200 || res.getData() == null) {
            System.out.println("菜单加载失败，请检查后端接口");
            if (res != null) {
                System.out.println("返回码：" + res.getCode() + "，消息：" + res.getMsg());
            }
            return;
        }

        List<Map> mList = (List<Map>) res.getData();
        System.out.println("成功加载 " + mList.size() + " 个菜单项");

        initMenuBar(mList);
        initMenuTree(mList);
        contentTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        contentTabPane.setStyle("-fx-background-image: url('shanda1.jpg'); -fx-background-repeat: no-repeat; -fx-background-size: cover;");
    }

    protected void onLogoutMenuClick(ActionEvent event){
        logout();
    }

    protected void logout(){
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
        if(obj instanceof MenuItem item) {
            name = item.getId();
            title = item.getText();
        }
        if(name == null)
            return;
        changeContent(name, title);
    }

    public void changeContent(String name, String title) {
        System.out.println("========== changeContent 调试 ==========");
        System.out.println("name: " + name);
        System.out.println("title: " + title);
        System.out.println("========================================");

        if(name == null || name.length() == 0)
            return;

        String fxmlPath = name;

        if ("material".equals(name)) {
            fxmlPath = "view/MaterialView";
        } else if ("category".equals(name)) {
            fxmlPath = "view/CategoryView";
        } else if ("stock-warning".equals(name) || "warning".equals(name)) {
            fxmlPath = "view/StockWarningView";
        } else if (name.contains("stockin")) {
            fxmlPath = "base/stockin-panel";
        } else if ("stockout-approve".equals(name) || name.contains("outbound-approve")) {
            fxmlPath = "base/outbound-panel";
        } else if ("stockout-apply".equals(name) || name.contains("outbound-apply")) {
            fxmlPath = "base/outbound-apply";
        } else if (name.contains("stockout") || name.contains("outbound")) {
            fxmlPath = "base/outbound-panel";
        } else if (name.contains("outorder")) {
            fxmlPath = "base/outorder-list-panel";
        } else if (name.contains("user-audit") || name.contains("user-approve")) {
            fxmlPath = "base/user-audit";
        } else if (name.contains("profile")) {
            fxmlPath = "base/profile-panel";
        } else if (name.contains("password")) {
            fxmlPath = "base/password-panel";
        } else if (name.contains("dictionary")) {
            fxmlPath = "base/dictionary-panel";
        }

        Tab tab = tabMap.get(fxmlPath);
        Scene scene;
        Object c;
        if(tab == null) {
            scene = sceneMap.get(fxmlPath);
            if(scene == null) {
                String resourcePath = fxmlPath + ".fxml";
                System.out.println("尝试加载 FXML: " + resourcePath);
                
                java.net.URL resource = MainApplication.class.getResource(resourcePath);
                if (resource == null) {
                    System.err.println(" FXML 文件不存在: " + resourcePath);
                    System.err.println("请检查文件是否在 resources 目录下");
                    MessageDialog.showDialog("界面文件不存在: " + resourcePath);
                    return;
                }
                
                System.out.println("✅ FXML 文件找到: " + resource.getPath());
                
                FXMLLoader fxmlLoader = new FXMLLoader(resource);
                try {
                    scene = new Scene(fxmlLoader.load(), 1024, 768);
                    sceneMap.put(fxmlPath, scene);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("加载 FXML 失败: " + e.getMessage());
                    MessageDialog.showDialog("加载界面失败: " + e.getMessage());
                    return;
                }
                c = fxmlLoader.getController();
                if(c instanceof ToolController) {
                    controlMap.put(fxmlPath,(ToolController)c);
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


    public void tabSelectedChanged(Event e) {
        Tab tab = (Tab)e.getSource();
        String name = tab.getId();
        ToolController c = controlMap.get(name);
        if(c != null)
            c.doRefresh();
    }

    public void tabOnClosed(Event e) {
        Tab tab = (Tab)e.getSource();
        String name = tab.getId();

        Object controller = controlMap.get(name);
        if (controller instanceof StockWarningController) {
            ((StockWarningController) controller).cleanup();
        }

        contentTabPane.getTabs().remove(tab);
        tabMap.remove(name);
        controlMap.remove(name);
    }

    public ToolController getCurrentToolController(){
        Iterator<String> iterator = controlMap.keySet().iterator();
        String name;
        Tab tab;
        while(iterator.hasNext()) {
            name = iterator.next();
            tab = tabMap.get(name);
            if(tab.isSelected()) {
                return controlMap.get(name);
            }
        }
        return null;
    }

    protected void doNewCommand(){
        ToolController c = getCurrentToolController();
        if(c == null)
            return;
        c.doNew();
    }

    protected void doSaveCommand(){
        ToolController c = getCurrentToolController();
        if(c == null)
            return;
        c.doSave();
    }

    protected void doDeleteCommand(){
        ToolController c = getCurrentToolController();
        if(c == null)
            return;
        c.doDelete();
    }

    protected void doPrintCommand(){
        ToolController c = getCurrentToolController();
        if(c == null)
            return;
        c.doPrint();
    }

    protected void doExportCommand(){
        ToolController c = getCurrentToolController();
        if(c == null)
            return;
        c.doExport();
    }

    protected void doImportCommand(){
        ToolController c = getCurrentToolController();
        if(c == null)
            return;
        c.doImport();
    }

    protected void doTestCommand(){
        ToolController c = getCurrentToolController();
        if(c == null) {
            c= new ToolController(){};
        }
        c.doTest();
    }

    public ToolController getToolController(String name){
        return controlMap.get(name);
    }
}
