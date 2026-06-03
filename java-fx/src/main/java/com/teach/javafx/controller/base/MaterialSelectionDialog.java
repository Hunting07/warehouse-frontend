package com.teach.javafx.controller.base;

import com.teach.javafx.request.OptionItem;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.List;

public class MaterialSelectionDialog extends Stage {

    @FXML
    private TextField searchField;

    @FXML
    private ListView<OptionItem> materialListView;

    @FXML
    private Label countLabel;

    private final ObservableList<OptionItem> allMaterials = FXCollections.observableArrayList();
    private FilteredList<OptionItem> filteredMaterials;
    private OptionItem selectedMaterial;

    public MaterialSelectionDialog() {
    }

    public static MaterialSelectionDialog createDialog(List<OptionItem> materials) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    MaterialSelectionDialog.class.getResource("/com/teach/javafx/base/material-selection-dialog.fxml"));

            javafx.scene.Scene scene = new javafx.scene.Scene(loader.load(), 500, 600);
            scene.getStylesheets().add(MaterialSelectionDialog.class.getResource("/styles/modern-style.css").toExternalForm());

            MaterialSelectionDialog dialog = loader.getController();
            if (dialog == null) {
                throw new RuntimeException("无法获取控制器实例");
            }

            dialog.setScene(scene);
            dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialog.setTitle("选择物资");
            dialog.setResizable(false);

            dialog.initData(materials);

            return dialog;
        } catch (Exception e) {
            System.err.println("加载物资选择对话框失败: " + e.getMessage());
            e.printStackTrace();
            MessageDialog.showDialog("打开物资选择对话框失败：" + e.getMessage());
            return null;
        }
    }

    private void initData(List<OptionItem> materials) {
        allMaterials.addAll(materials);
        filteredMaterials = new FilteredList<>(allMaterials, p -> true);

        materialListView.setItems(filteredMaterials);
        materialListView.setCellFactory(lv -> new javafx.scene.control.ListCell<OptionItem>() {
            @Override
            protected void updateItem(OptionItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(item.getName());
                    setStyle("-fx-background-color: white; -fx-padding: 12 16 12 16; -fx-font-size: 14px;");

                    if (item == materialListView.getSelectionModel().getSelectedItem()) {
                        setStyle("-fx-background-color: #e6f7ff; -fx-padding: 12 16 12 16; -fx-font-size: 14px;");
                    }
                }
            }
        });

        materialListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedMaterial = newVal;
        });

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredMaterials.setPredicate(material -> {
                if (newVal == null || newVal.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newVal.toLowerCase();
                return material.getName().toLowerCase().contains(lowerCaseFilter);
            });
        });

        countLabel.setText("(" + allMaterials.size() + ")");

        materialListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                selectedMaterial = materialListView.getSelectionModel().getSelectedItem();
                if (selectedMaterial != null) {
                    close();
                }
            }
        });
    }

    @FXML
    protected void onConfirmButtonClick() {
        if (selectedMaterial == null) {
            MessageDialog.showDialog("请选择一个物资");
            return;
        }
        close();
    }

    @FXML
    protected void onCancelButtonClick() {
        selectedMaterial = null;
        close();
    }

    public OptionItem getSelectedMaterial() {
        return selectedMaterial;
    }
}
