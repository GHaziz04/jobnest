module tn.jobnest.gentretien {
    // Modules JavaFX
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.base;
    requires javafx.web;

    // Java standard
    requires java.sql;
    requires java.desktop;
    requires jdk.httpserver;

    // Google API (seulement pour Google Meet)
    requires com.google.api.client;
    requires com.google.api.client.auth;
    requires com.google.api.client.extensions.java6.auth;
    requires com.google.api.client.extensions.jetty.auth;
    requires com.google.api.client.json.gson;
    requires com.google.api.services.calendar;
    requires google.api.client;
    requires com.google.gson;

    // Exports
    exports tn.jobnest.gentretien;
    exports tn.jobnest.gentretien.controller;
    exports tn.jobnest.gentretien.model;
    exports tn.jobnest.gentretien.service;

    // Opens
    opens tn.jobnest.gentretien.controller to javafx.fxml;
    opens tn.jobnest.gentretien.model to javafx.base, javafx.fxml;
}