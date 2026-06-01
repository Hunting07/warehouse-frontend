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
                System.out.println("\n========== [金额统计] 开始加载 ==========");

                // 获取入库金额
                System.out.println("\n--- 请求入库金额 ---");
                double stockInAmount = getStockInAmount();
                System.out.println("入库金额结果: " + stockInAmount);

                // 获取出库金额
                System.out.println("\n--- 请求出库金额 ---");
                double stockOutAmount = getStockOutAmount();
                System.out.println("出库金额结果: " + stockOutAmount);

                // 计算净金额
                double netAmount = stockInAmount - stockOutAmount;
                System.out.println("\n净金额: " + netAmount);

                javafx.application.Platform.runLater(() -> {
                    stockInAmountLabel.setText(String.format("¥%.2f", stockInAmount));
                    stockOutAmountLabel.setText(String.format("¥%.2f", stockOutAmount));
                    netAmountLabel.setText(String.format("¥%.2f", netAmount));
                });

                System.out.println("========== [金额统计] 加载完成 ==========\n");
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
            System.out.println("请求URL: " + url);
            System.out.println("Token: " + AppStore.getJwt().getTokenValue());

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .headers("satoken", AppStore.getJwt().getTokenValue())
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            System.out.println("响应状态码: " + response.statusCode());
            System.out.println("响应内容: " + response.body());

            if (response.statusCode() == 200) {
                Map<String, Object> result = gson.fromJson(response.body(), new TypeToken<Map<String, Object>>(){}.getType());
                System.out.println("解析结果: " + result);

                if (result.get("code").equals(200.0)) {
                    Map<String, Object> data = (Map<String, Object>) result.get("data");
                    System.out.println("data 内容: " + data);

                    if (data != null && data.get("totalAmount") != null) {
                        double amount = ((Number) data.get("totalAmount")).doubleValue();
                        System.out.println("入库金额: " + amount);
                        return amount;
                    } else {
                        System.out.println("⚠️ data 中没有 totalAmount 字段！");
                    }
                } else {
                    System.out.println("⚠️ 接口返回错误：code=" + result.get("code") + ", msg=" + result.get("msg"));
                }
            } else {
                System.out.println("⚠️ HTTP请求失败，状态码：" + response.statusCode());
            }
        } catch (Exception e) {
            System.out.println("️ 异常信息：" + e.getMessage());
            e.printStackTrace();
        }
        return 0.0;
    }

    private double getStockOutAmount() {
        try {
            String url = HttpRequestUtil.serverUrl + "/api/outbound/statistics/amount";
            System.out.println("请求URL: " + url);
            System.out.println("Token: " + AppStore.getJwt().getTokenValue());

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .headers("satoken", AppStore.getJwt().getTokenValue())
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            System.out.println("响应状态码: " + response.statusCode());
            System.out.println("响应内容: " + response.body());

            if (response.statusCode() == 200) {
                Map<String, Object> result = gson.fromJson(response.body(), new TypeToken<Map<String, Object>>(){}.getType());
                System.out.println("解析结果: " + result);

                if (result.get("code").equals(200.0)) {
                    Map<String, Object> data = (Map<String, Object>) result.get("data");
                    System.out.println("data 内容: " + data);

                    if (data != null && data.get("totalAmount") != null) {
                        double amount = ((Number) data.get("totalAmount")).doubleValue();
                        System.out.println("出库金额: " + amount);
                        return amount;
                    } else {
                        System.out.println("️ data 中没有 totalAmount 字段！");
                    }
                } else {
                    System.out.println("⚠️ 接口返回错误：code=" + result.get("code") + ", msg=" + result.get("msg"));
                }
            } else {
                System.out.println("⚠️ HTTP请求失败，状态码：" + response.statusCode());
            }
        } catch (Exception e) {
            System.out.println("⚠️ 异常信息：" + e.getMessage());
            e.printStackTrace();
        }
        return 0.0;
    }
}
