module tn.jobnest.gentretien {

    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;     // ← solves many mysterious missing classes
    requires javafx.base;

    requires java.sql;
    requires java.desktop;
    requires com.google.api.client;
    requires com.google.api.client.auth;
    requires com.google.api.client.extensions.java6.auth;
    requires com.google.api.client.extensions.jetty.auth;
    requires com.google.api.client.json.gson;
    requires com.google.api.services.calendar;
    requires google.api.client;
    requires jdk.httpserver;

    exports tn.jobnest.gentretien;
    exports tn.jobnest.gentretien.controller;
    exports tn.jobnest.gentretien.model;
    exports tn.jobnest.gentretien.service;

    opens tn.jobnest.gentretien.controller to javafx.fxml;
    opens tn.jobnest.gentretien.model     to javafx.base, javafx.fxml;
}