module tn.jobnest.gentretien {

    // JavaFX
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.base;
    requires javafx.web;
    requires javafx.swing;      // ← nécessaire pour SwingFXUtils (QR Code)

    // Java standard
    requires java.sql;
    requires java.desktop;
    requires jdk.httpserver;

    // Google API / Calendar
    requires com.google.api.client;
    requires com.google.api.client.auth;
    requires com.google.api.client.extensions.java6.auth;
    requires com.google.api.client.extensions.jetty.auth;
    requires com.google.api.client.json.gson;
    requires com.google.api.services.calendar;
    requires google.api.client;
    requires com.google.gson;

    // Google Authenticator (2FA / OTP)
    requires googleauth;

    // QR Code (ZXing)
    requires com.google.zxing;
    requires com.google.zxing.javase;

    // Mail + JSON
    requires jakarta.mail;
    requires org.json;

    // Exports
    exports tn.jobnest.gentretien;
    exports tn.jobnest.gentretien.controller;
    exports tn.jobnest.gentretien.model;
    exports tn.jobnest.gentretien.service;
    exports tn.jobnest.gentretien.dao;
    exports tn.jobnest.gentretien.utils;
    exports tn.jobnest.gentretien.voice;

    // Opens pour JavaFX FXML
    opens tn.jobnest.gentretien            to javafx.fxml;
    opens tn.jobnest.gentretien.controller to javafx.fxml;
    opens tn.jobnest.gentretien.model      to javafx.base, javafx.fxml;
}