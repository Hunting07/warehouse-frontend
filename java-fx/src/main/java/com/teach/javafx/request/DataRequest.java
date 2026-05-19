package com.teach.javafx.request;

import java.util.HashMap;
import java.util.Map;

public class DataRequest {
    private final Map<String, Object> params = new HashMap<>();

    public void put(String key, Object value) {
        params.put(key, value);
    }

    public Map<String, Object> getParams() {
        return params;
    }
}