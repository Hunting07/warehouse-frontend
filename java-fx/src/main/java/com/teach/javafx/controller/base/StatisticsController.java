package com.teach.javafx.controller.base;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.teach.javafx.AppStore;
import com.teach.javafx.request.HttpRequestUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class StatisticsController {

    @FXML
    private Label stockInAmountLabel;
    @FXML
    private Label stockOutAmountLabel;
    @FXML
    private Label netAmountLabel;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    @FXML
    public void initialize() {
        loadStatistics();
    }

    @FXML
    public void loadStatistics() {
        new Thread(() -> {
            try {
                // 获取入库金额
                double stockInAmount = getStockInAmount();

                // 获取出库金额
                double stockOutAmount = getStockOutAmount();

                // 计算净金额
                double netAmount = stockInAmount - stockOutAmount;

                javafx.application.Platform.runLater(() -> {
                    stockInAmountLabel.setText(String.format("¥%.2f", stockInAmount));
                    stockOutAmountLabel.setText(String.format("¥%.2f", stockOutAmount));
                    netAmountLabel.setText(String.format("¥%.2f", netAmount));
                });
            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() ->
                        MessageDialog.showDialog("加载统计数据失败：" + e.getMessage())
                );
            }
        }).start();
    }

    private double getStockInAmount() {
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(HttpRequestUtil.serverUrl + "/stockin/statistics/amount"))
                    .GET()
                    .headers("satoken", AppStore.getJwt().getTokenValue())
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, Object> result = gson.fromJson(response.body(), new TypeToken<Map<String, Object>>(){}.getType());

                if (result.get("code").equals(200.0)) {
                    Map<String, Object> data = (Map<String, Object>) result.get("data");
                    if (data != null && data.get("totalAmount") != null) {
                        return ((Number) data.get("totalAmount")).doubleValue();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    private double getStockOutAmount() {
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(HttpRequestUtil.serverUrl + "/outbound/statistics/amount"))
                    .GET()
                    .headers("satoken", AppStore.getJwt().getTokenValue())
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, Object> result = gson.fromJson(response.body(), new TypeToken<Map<String, Object>>(){}.getType());

                if (result.get("code").equals(200.0)) {
                    Map<String, Object> data = (Map<String, Object>) result.get("data");
                    if (data != null && data.get("totalAmount") != null) {
                        return ((Number) data.get("totalAmount")).doubleValue();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0.0;
    }
}
