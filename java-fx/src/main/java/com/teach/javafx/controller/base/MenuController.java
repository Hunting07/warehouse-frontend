package com.teach.javafx.controller.base;

import com.teach.javafx.request.*;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;

import java.util.List;
import java.util.StringTokenizer;

/**
 * MenuController 菜单管理控制类 对应 base/menu-panel.fxml
 * 适配仓储管理系统：仅保留“管理员”和“员工”两种身份
 */
public class MenuController {
    @FXML
    private TreeView<MyTreeNode> menuTreeView;
    @FXML
    private Label nodeParentLabel;
    @FXML
    private TextField nodeIdField;
    @FXML
    private TextField nodeNameField;
    @FXML
    private TextField nodeTitleField;

    // 仅保留两个身份复选框
    @FXML
    private CheckBox nodeAdminCheckBox;   // 管理员 (ID: 1)
    @FXML
    private CheckBox nodeEmployeeCheckBox; // 员工 (ID: 2)

    private TreeItem<MyTreeNode> treeItem;
    private MyTreeNode editNode = null;
    private TreeItem<MyTreeNode> root;
    private Integer editType = 0;

    private TreeItem<MyTreeNode> getTreeItem(MyTreeNode node) {
        TreeItem<MyTreeNode> item = new TreeItem<>(node);
        List<MyTreeNode> sList = node.getChildren();
        if (sList == null) return item;
        for (MyTreeNode child : sList) {
            item.getChildren().add(getTreeItem(child));
        }
        return item;
    }

    public void valueChanged(TreeItem.TreeModificationEvent<MyTreeNode> e) {
        MyTreeNode nodeValue = e.getSource().getValue();
        System.out.println(nodeValue);
    }

    /**
     * 页面初始化
     */
    @FXML
    public void initialize() {
        MyTreeNode node = new MyTreeNode(null, "", "", 0);
        root = new TreeItem<>(node);
        menuTreeView.setRoot(root);
        menuTreeView.setShowRoot(false);
        updateTreeView();

        ContextMenu contextMenu = new ContextMenu();
        MenuItem add = new MenuItem("添加");
        add.setOnAction(this::onAddButtonClick);
        MenuItem edit = new MenuItem("编辑");
        edit.setOnAction(this::onEditButtonClick);
        MenuItem delete = new MenuItem("删除");
        delete.setOnAction(this::onDeleteButtonClick);
        contextMenu.getItems().addAll(add, edit, delete);
        menuTreeView.setContextMenu(contextMenu);

        menuTreeView.addEventFilter(MouseEvent.MOUSE_CLICKED, event ->
                treeItem = menuTreeView.getSelectionModel().getSelectedItem()
        );
    }

    public void updateTreeView() {
        DataRequest req = new DataRequest();
        List<MyTreeNode> nodeList = HttpRequestUtil.requestTreeNodeList("/api/base/getMenuTreeNodeList", req);
        if (nodeList == null || nodeList.isEmpty()) return;
        for (MyTreeNode node : nodeList) {
            root.getChildren().add(getTreeItem(node));
        }
    }

    /**
     * 根据节点权限设置复选框状态
     */
    public void setRoleCheckBox() {
        nodeAdminCheckBox.setSelected(false);
        nodeEmployeeCheckBox.setSelected(false);

        if (editNode == null) return;
        String useTypeIds = editNode.getUserTypeIds();
        if (useTypeIds == null || useTypeIds.isEmpty()) return;

        StringTokenizer sz = new StringTokenizer(useTypeIds, ",");
        while (sz.hasMoreTokens()) {
            String str = sz.nextToken();
            if ("1".equals(str)) nodeAdminCheckBox.setSelected(true);
            if ("2".equals(str)) nodeEmployeeCheckBox.setSelected(true);
        }
    }

    public void updateNodePanel() {
        if (editNode == null) {
            nodeIdField.setText("");
            nodeNameField.setText("");
            nodeTitleField.setText("");
        } else {
            nodeIdField.setText(editNode.getId() == null ? "" : editNode.getId().toString());
            nodeNameField.setText(editNode.getValue());
            nodeTitleField.setText(editNode.getTitle());
        }
        setRoleCheckBox();
    }

    @FXML
    protected void onAddRootButtonClick() {
        editType = 0;
        editNode = new MyTreeNode();
        editNode.setParentTitle("");
        updateNodePanel();
    }

    protected void onAddButtonClick(ActionEvent e) {
        if (treeItem == null || treeItem.getValue() == null) {
            MessageDialog.showDialog("没有选择，无法添加");
            return;
        }
        MyTreeNode node = treeItem.getValue();
        editNode = new MyTreeNode();
        editNode.setParentTitle(node.getTitle());
        editNode.setPid(node.getId());
        editType = 1;
        updateNodePanel();
    }

    @FXML
    protected void onEditButtonClick(ActionEvent e) {
        if (treeItem == null || treeItem.getValue() == null) {
            MessageDialog.showDialog("没有选择，无法修改");
            return;
        }
        editType = 2;
        editNode = treeItem.getValue();
        updateNodePanel();
    }

    @FXML
    protected void onDeleteButtonClick(ActionEvent e) {
        if (treeItem == null || treeItem.getValue() == null) {
            MessageDialog.showDialog("没有选择，无法删除");
            return;
        }
        MyTreeNode node = treeItem.getValue();
        TreeItem<MyTreeNode> parent = treeItem.getParent();
        if (parent == null) {
            MessageDialog.showDialog("不能删除根节点");
        } else {
            int ret = MessageDialog.choiceDialog("确认要删除菜单：" + node.getLabel() + " 吗？");
            if (ret != MessageDialog.CHOICE_YES) return;

            DataRequest req = new DataRequest();
            req.put("id", node.getId());
            DataResponse res = HttpRequestUtil.request("/api/base/menuDelete", req);
            if (res != null && res.getCode() == 0) {
                MessageDialog.showDialog("删除成功！");
                parent.getChildren().remove(treeItem);
                treeItem = null;
            } else {
                MessageDialog.showDialog(res != null ? res.getMsg() : "删除失败");
            }
        }
    }

    @FXML
    protected void onSubmitButtonClick() {
        if (nodeIdField.getText().isEmpty()) {
            MessageDialog.showDialog("ID 不能为空");
            return;
        }
        editNode.setId(Integer.parseInt(nodeIdField.getText()));
        editNode.setValue(nodeNameField.getText());
        editNode.setTitle(nodeTitleField.getText());

        // 组装权限字符串（1=管理员，2=员工）
        String str = null;
        if (nodeAdminCheckBox.isSelected()) {
            str = (str == null) ? "1" : str + ",1";
        }
        if (nodeEmployeeCheckBox.isSelected()) {
            str = (str == null) ? "2" : str + ",2";
        }
        editNode.setUserTypeIds(str);
        editNode.setLabel(editNode.getId() + "-" + editNode.getTitle());

        DataRequest req = new DataRequest();
        req.put("editType", editType);
        req.put("node", editNode);
        DataResponse res = HttpRequestUtil.request("/api/base/menuSave", req);

        if (res != null && res.getCode() == 0) {
            MessageDialog.showDialog("保存成功！");
            if (editType == 0) {
                root.getChildren().add(new TreeItem<>(editNode));
            } else if (editType == 1) {
                treeItem.getChildren().add(new TreeItem<>(editNode));
            } else if (editType == 2) {
                treeItem.setValue(editNode);
            }
        } else {
            MessageDialog.showDialog(res != null ? res.getMsg() : "保存失败");
        }
    }
}
