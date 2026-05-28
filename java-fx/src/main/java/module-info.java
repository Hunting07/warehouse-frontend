module com.teach.javafx {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires java.logging;
    requires com.google.gson;
    requires java.net.http;
    requires javafx.graphics;


    opens com.teach.javafx.request to com.google.gson, javafx.fxml;
    opens com.teach.javafx.controller.base to com.google.gson, javafx.fxml;
    opens com.teach.javafx.models to com.google.gson, javafx.fxml;
    opens com.teach.javafx.util to com.google.gson, javafx.fxml;

    exports com.teach.javafx;
    exports com.teach.javafx.controller.base;
    exports com.teach.javafx.request;
    exports com.teach.javafx.util;
    exports com.teach.javafx.models;
    opens com.teach.javafx to com.google.gson, javafx.fxml;
    exports com.teach.javafx.bean;
    opens com.teach.javafx.bean to com.google.gson, javafx.fxml;

}