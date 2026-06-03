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

                // 计算净金额（利润 = 卖出收入 - 买入成本）
                double netAmount = stockOutAmount - stockInAmount;

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
            String url = HttpRequestUtil.serverUrl + "/api/stockin/statistics/amount";

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .headers("satoken", AppStore.getJwt().getTokenValue())
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, Object> result = gson.fromJson(response.body(), new TypeToken<Map<String, Object>>(){}.getType());

                if (result.get("code").equals(200.0)) {
                    Map<String, Object> data = (Map<String, Object>) result.get("data");

                    if (data != null && data.get("totalAmount") != null) {
                        double amount = ((Number) data.get("totalAmount")).doubleValue();
                        return amount;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("获取入库金额异常：" + e.getMessage());
            e.printStackTrace();
        }
        return 0.0;
    }

    private double getStockOutAmount() {
        try {
            String url = HttpRequestUtil.serverUrl + "/api/outbound/statistics/amount";

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .headers("satoken", AppStore.getJwt().getTokenValue())
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, Object> result = gson.fromJson(response.body(), new TypeToken<Map<String, Object>>(){}.getType());

                if (result.get("code").equals(200.0)) {
                    Map<String, Object> data = (Map<String, Object>) result.get("data");

                    if (data != null && data.get("totalAmount") != null) {
                        double amount = ((Number) data.get("totalAmount")).doubleValue();
                        return amount;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("获取出库金额异常：" + e.getMessage());
            e.printStackTrace();
        }
        return 0.0;
    }
}
