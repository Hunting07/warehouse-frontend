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
import javafx.scene.layout.VBox;
import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.DataResponse;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
    @FXML
    private Label welcomeLabel;
    @FXML
    private Label timeLabel;
    @FXML
    private Label totalMaterialsLabel;
    @FXML
    private Label todayInLabel;
    @FXML
    private Label todayOutLabel;
    @FXML
    private Label warningCountLabel;
    @FXML
    private VBox dashboardView;
    @FXML
    private Button refreshButton;


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
            if ("user-audit".equals(name) || "用户注册审批".equals(title)) {
                continue;
            }

            String icon = getMenuIcon(name, title);
            String displayText = icon + "  " + title;
            menu = new TreeItem<>(new MyTreeNode(null, name, displayText, 0));
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
            }if ("user-audit".equals(name) || "用户注册审批".equals(title)) {
                continue;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> sList = (List<Map<String, Object>>) m.get("sList");
            Object isLeftObj = m.get("isLeft");
            int isLeft = isLeftObj instanceof Number ? ((Number) isLeftObj).intValue() : 0;

            if (isLeft == 1) {
                String icon = getMenuIcon(name, title);
                String displayText = icon + "  " + title;
                menu = new TreeItem<>(new MyTreeNode(null, name, displayText, isLeft));
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

    private String getMenuIcon(String name, String title) {
        if (title == null) return "";

        if (title.contains("物资分类") || title.contains("分类")) return "📦";
        if (title.contains("物资管理") || title.contains("物资")) return "📋";
        if (title.contains("入库")) return "📥";
        if (title.contains("出库")) return "📤";
        if (title.contains("库存预警") || title.contains("预警")) return "⚠️";
        if (title.contains("个人中心") || title.contains("个人")) return "👤";
        if (title.contains("用户管理") || title.contains("用户")) return "👥";
        if (title.contains("员工管理") || title.contains("员工")) return "💼";
        if (title.contains("管理员管理") || title.contains("管理员列表")) return "👑";
        if (title.contains("审批")) return "✅";
        if (title.contains("金额统计") || title.contains("统计")) return "💰";
        if (title.contains("密码")) return "🔒";
        if (title.contains("字典")) return "📖";

        return "📌";
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

        addCustomMenus(role);

        setupWelcomeMessage(role);

        startTimeUpdate();

        loadDashboardData();

        contentTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
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
                new MyTreeNode(null, "base/profile-panel", "👤  个人中心", 0)
        );
        root.getChildren().add(profileItem);
    }

    private void addCustomMenus(String role) {
        TreeItem<MyTreeNode> root = menuTree.getRoot();
        if (root == null) {
            return;
        }

        if ("admin".equals(role)) {
            TreeItem<MyTreeNode> userManageItem = new TreeItem<>(
                    new MyTreeNode(null, "", "👥  用户管理", 1)
            );

            TreeItem<MyTreeNode> employeeViewItem = new TreeItem<>(
                    new MyTreeNode(null, "employee-view", "💼  员工管理", 0)
            );
            TreeItem<MyTreeNode> adminViewItem = new TreeItem<>(
                    new MyTreeNode(null, "admin-view", "👑  管理员列表", 0)  // ← 改成"列表"
            );

            userManageItem.getChildren().add(employeeViewItem);
            userManageItem.getChildren().add(adminViewItem);
            root.getChildren().add(userManageItem);

            TreeItem<MyTreeNode> adminApproveItem = new TreeItem<>(
                    new MyTreeNode(null, "admin-approve", "✅  管理员审批", 1)
            );
            root.getChildren().add(adminApproveItem);

            TreeItem<MyTreeNode> statisticsItem = new TreeItem<>(
                    new MyTreeNode(null, "statistics", "💰  金额统计", 1)
            );
            root.getChildren().add(statisticsItem);
        } else if ("employee".equals(role)) {
            TreeItem<MyTreeNode> statisticsItem = new TreeItem<>(
                    new MyTreeNode(null, "statistics", "💰  金额统计", 1)
            );
            root.getChildren().add(statisticsItem);
        }
    }

    private void setupWelcomeMessage(String role) {
        String roleText = "admin".equals(role) ? "管理员" : "员工";
        String username = AppStore.getJwt().getUsername();

        if (username == null || username.isEmpty()) {
            username = "用户";
        }

        String welcomeText = "您好！" + roleText + " " + username;
        welcomeLabel.setText(welcomeText);
    }

    private void startTimeUpdate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, e -> {
                    timeLabel.setText(LocalDateTime.now().format(formatter));
                }),
                new KeyFrame(Duration.seconds(1))
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void loadDashboardData() {
        new Thread(() -> {
            try {
                System.out.println("=== [仪表盘] 开始加载数据 ===");
                long startTime = System.currentTimeMillis();

                DataRequest req = new DataRequest();

                // 并行请求所有接口
                Thread t1 = new Thread(() -> {
                    try {
                        System.out.println("[仪表盘] 请求: /api/material/list");
                        DataResponse materialRes = HttpRequestUtil.request("/api/material/list", req);
                        int count = 0;
                        if (materialRes != null && materialRes.getCode() == 200 && materialRes.getData() instanceof List) {
                            count = ((List<?>) materialRes.getData()).size();
                            System.out.println("[仪表盘] ✓ 物资总数: " + count);
                        }
                        updateLabel(totalMaterialsLabel, count);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                Thread t2 = new Thread(() -> {
                    try {
                        System.out.println("[仪表盘] 请求: /stock-in/list");
                        DataResponse stockInRes = HttpRequestUtil.request("/stock-in/list", req);
                        int count = 0;
                        if (stockInRes != null && stockInRes.getCode() == 200 && stockInRes.getData() instanceof List) {
                            List<?> list = (List<?>) stockInRes.getData();
                            String today = java.time.LocalDate.now().toString();
                            for (Object item : list) {
                                if (item instanceof Map) {
                                    Object createTime = ((Map<?, ?>) item).get("createTime");
                                    if (createTime != null && createTime.toString().startsWith(today)) {
                                        count++;
                                    }
                                }
                            }
                            System.out.println("[仪表盘] ✓ 今日入库: " + count);
                        }
                        updateLabel(todayInLabel, count);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                Thread t3 = new Thread(() -> {
                    try {
                        System.out.println("[仪表盘] 请求: /api/stockOut/list");
                        DataResponse stockOutRes = HttpRequestUtil.request("/api/stockOut/list", req);
                        int count = 0;
                        if (stockOutRes != null && stockOutRes.getCode() == 200 && stockOutRes.getData() != null) {
                            Object data = stockOutRes.getData();
                            List<?> list = null;

                            // 处理分页数据结构 {records: [...], total: ..., ...}
                            if (data instanceof Map) {
                                Map<?, ?> map = (Map<?, ?>) data;
                                Object records = map.get("records");
                                if (records instanceof List) {
                                    list = (List<?>) records;
                                }
                            } else if (data instanceof List) {
                                list = (List<?>) data;
                            }

                            if (list != null) {
                                String today = java.time.LocalDate.now().toString();
                                for (Object item : list) {
                                    if (item instanceof Map) {
                                        Object createTime = ((Map<?, ?>) item).get("createTime");
                                        if (createTime != null && createTime.toString().startsWith(today)) {
                                            count++;
                                        }
                                    }
                                }
                            }
                            System.out.println("[仪表盘] ✓ 今日出库: " + count);
                        }
                        updateLabel(todayOutLabel, count);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                Thread t4 = new Thread(() -> {
                    try {
                        System.out.println("[仪表盘] 请求: /api/material/warning");
                        DataResponse warningRes = HttpRequestUtil.request("/api/material/warning", req);
                        int count = 0;
                        if (warningRes != null && warningRes.getCode() == 200 && warningRes.getData() instanceof List) {
                            count = ((List<?>) warningRes.getData()).size();
                            System.out.println("[仪表盘] ✓ 库存预警: " + count);
                        }
                        updateLabel(warningCountLabel, count);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                // 启动所有线程
                t1.start();
                t2.start();
                t3.start();
                t4.start();

                // 等待所有线程完成
                t1.join();
                t2.join();
                t3.join();
                t4.join();

                long endTime = System.currentTimeMillis();
                System.out.println("=== [仪表盘] 加载完成，耗时: " + (endTime - startTime) + "ms ===\n");

            } catch (Exception e) {
                System.err.println("=== [仪表盘] 加载失败 ===");
                e.printStackTrace();
            }
        }).start();
    }

    private void updateLabel(Label label, int value) {
        javafx.application.Platform.runLater(() -> {
            label.setText(String.valueOf(value));
        });
    }

    @FXML
    private void handleRefresh() {
        loadDashboardData();
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

        dashboardView.setVisible(false);
        dashboardView.setManaged(false);
        contentTabPane.setVisible(true);
        contentTabPane.setManaged(true);

        String fxmlPath = name;

        if ("material".equals(name)) {
            fxmlPath = "/view/MaterialView";
        } else if ("category".equals(name)) {
            fxmlPath = "/view/CategoryView";
        } else if ("stock-warning".equals(name) || "warning".equals(name)) {
            fxmlPath = "/view/StockWarningView";
        } else if (name.contains("stockin")) {
            fxmlPath = "/com/teach/javafx/base/stockin-panel";
        } else if (name.contains("stockout")) {
            fxmlPath = "/com/teach/javafx/base/outbound-panel";
        } else if (name.contains("outorder") || name.contains("出库审批")) {
            fxmlPath = "/com/teach/javafx/base/outorder-list-panel";
        } else if (name.contains("user-audit") || name.contains("用户注册审批")) {
            fxmlPath = "/com/teach/javafx/base/user-audit";
        } else if (name.contains("admin-approve") || name.contains("管理员审批")) {
            fxmlPath = "/com/teach/javafx/base/admin-approve";
        } else if (name.contains("employee-view") || name.contains("员工管理")) {
            fxmlPath = "/com/teach/javafx/base/employee-view";
        } else if (name.contains("admin-view") || name.contains("管理员管理")) {
            fxmlPath = "/com/teach/javafx/base/admin-view";
        } else if (name.contains("statistics") || name.contains("金额统计")) {
            fxmlPath = "/com/teach/javafx/base/statistics-panel";
        } else if (name.contains("profile")) {
            fxmlPath = "/com/teach/javafx/base/profile-panel";
        } else if (name.contains("password")) {
            fxmlPath = "/com/teach/javafx/base/password-panel";
        } else if (name.contains("dictionary")) {
            fxmlPath = "/com/teach/javafx/base/dictionary-panel";
        } else {
            return;
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
                    scene.getStylesheets().add(getClass().getResource("/styles/modern-style.css").toExternalForm());
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

        if (contentTabPane.getTabs().isEmpty()) {
            dashboardView.setVisible(true);
            dashboardView.setManaged(true);
            contentTabPane.setVisible(false);
            contentTabPane.setManaged(false);
        }
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
