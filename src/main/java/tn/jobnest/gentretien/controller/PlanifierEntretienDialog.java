package tn.jobnest.gentretien.controller;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import tn.jobnest.gentretien.model.Entretien;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class PlanifierEntretienDialog {

    // ─── Résultat ─────────────────────────────────────────────────────────
    public enum Action { CREER_NOUVEAU, REJOINDRE_EXISTANT, ANNULER }

    public static class DialogResult {
        public final Action    action;
        public final Entretien entretienChoisi;
        public DialogResult(Action a, Entretien e) { this.action = a; this.entretienChoisi = e; }
    }

    // ─── Palette ──────────────────────────────────────────────────────────
    private static final String BLUE_DARK    = "#1E3A5F";
    private static final String BLUE_PRIMARY = "#2563EB";

    // ─── Gradients valides JavaFX (pas de degrés, pas de % dans les stops) ──
    private static final String GRAD_HEADER       = "linear-gradient(from 0% 0% to 100% 100%, #1E3A5F, #2563EB)";
    private static final String GRAD_ORANGE       = "linear-gradient(from 0% 0% to 100% 0%, #F97316, #EA580C)";
    private static final String GRAD_ORANGE_HOVER = "linear-gradient(from 0% 0% to 100% 0%, #EA580C, #D95F00)";
    private static final String GRAD_BLUE         = "linear-gradient(from 0% 0% to 100% 0%, #2563EB, #1D4ED8)";
    private static final String GRAD_BLUE_HOVER   = "linear-gradient(from 0% 0% to 100% 0%, #1D4ED8, #1E40AF)";

    // ══════════════════════════════════════════════════════════════════════
    //  POINT D'ENTRÉE
    // ══════════════════════════════════════════════════════════════════════
    public static DialogResult show(List<Entretien> entretiensExistants,
                                    String nomCandidat,
                                    String titreOffre,
                                    Stage ownerStage) {

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.setResizable(false);
        if (ownerStage != null) dialog.initOwner(ownerStage);

        final DialogResult[] result = { new DialogResult(Action.ANNULER, null) };
        boolean hasExisting = entretiensExistants != null && !entretiensExistants.isEmpty();

        // ── ROOT ──────────────────────────────────────────────────────────
        VBox root = new VBox(0);
        root.setPrefWidth(660);
        root.setMaxWidth(660);
        root.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 20;" +
                        "-fx-effect: dropshadow(gaussian, rgba(15,30,55,0.35), 50, 0.1, 0, 12);"
        );

        // ── HEADER ────────────────────────────────────────────────────────
        VBox header = buildHeader(nomCandidat, titreOffre, hasExisting);
        root.getChildren().add(header);

        // ── BODY ──────────────────────────────────────────────────────────
        ToggleGroup toggleGroup = new ToggleGroup();
        VBox[] listHolder = { null };
        root.getChildren().add(buildBody(entretiensExistants, toggleGroup, listHolder, nomCandidat, titreOffre));

        // ── FOOTER ────────────────────────────────────────────────────────
        root.getChildren().add(buildFooter(dialog, result, toggleGroup));

        // ── SCÈNE ─────────────────────────────────────────────────────────
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);

        // Draggable
        double[] drag = new double[2];
        header.setOnMousePressed(e -> { drag[0] = dialog.getX() - e.getScreenX(); drag[1] = dialog.getY() - e.getScreenY(); });
        header.setOnMouseDragged(e -> { dialog.setX(e.getScreenX() + drag[0]);    dialog.setY(e.getScreenY() + drag[1]); });

        // ✅ UN SEUL APPEL — showAndWait() uniquement, jamais show() + showAndWait()
        dialog.showAndWait();
        return result[0];
    }

    // ══════════════════════════════════════════════════════════════════════
    //  HEADER
    // ══════════════════════════════════════════════════════════════════════
    private static VBox buildHeader(String nomCandidat, String titreOffre, boolean hasExisting) {
        VBox header = new VBox(16);
        header.setStyle(
                "-fx-background-color: " + GRAD_HEADER + ";" +
                        "-fx-background-radius: 20 20 0 0;" +
                        "-fx-padding: 28 32 24 32; -fx-cursor: move;"
        );

        // Ligne titre
        HBox titleRow = new HBox(16);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        StackPane iconCircle = new StackPane();
        Circle iconBg = new Circle(28);
        iconBg.setFill(Color.rgb(255, 255, 255, 0.14));
        iconBg.setStroke(Color.rgb(255, 255, 255, 0.25));
        iconBg.setStrokeWidth(1.5);
        Label iconLbl = new Label(hasExisting ? "📅" : "✨");
        iconLbl.setStyle("-fx-font-size: 22px;");
        iconCircle.getChildren().addAll(iconBg, iconLbl);

        VBox titleTexts = new VBox(5);
        Label mainTitle = new Label("Planifier un entretien");
        mainTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: white;");
        Label subLbl = new Label(hasExisting
                ? "Des entretiens existent déjà pour cette offre"
                : "Premier entretien pour cette offre");
        subLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.65);");
        titleTexts.getChildren().addAll(mainTitle, subLbl);
        titleRow.getChildren().addAll(iconCircle, titleTexts);

        // Bandeau info
        HBox infoStrip = new HBox(0);
        infoStrip.setAlignment(Pos.CENTER_LEFT);
        infoStrip.setStyle(
                "-fx-background-color: rgba(255,255,255,0.10);" +
                        "-fx-background-radius: 12; -fx-border-color: rgba(255,255,255,0.18);" +
                        "-fx-border-width: 1; -fx-border-radius: 12; -fx-padding: 14 20 14 20;"
        );

        VBox candCol = infoCol("👤  CANDIDAT",
                (nomCandidat != null && !nomCandidat.isBlank()) ? nomCandidat : "—", "white");
        HBox.setHgrow(candCol, Priority.ALWAYS);

        Region vDiv = new Region();
        vDiv.setPrefWidth(1); vDiv.setPrefHeight(40);
        vDiv.setStyle("-fx-background-color: rgba(255,255,255,0.2);");
        HBox.setMargin(vDiv, new Insets(0, 20, 0, 20));

        VBox offreCol = infoCol("💼  OFFRE",
                (titreOffre != null && !titreOffre.isBlank()) ? titreOffre : "—", "#93C5FD");
        HBox.setHgrow(offreCol, Priority.ALWAYS);

        infoStrip.getChildren().addAll(candCol, vDiv, offreCol);
        VBox.setMargin(infoStrip, new Insets(4, 0, 0, 0));
        header.getChildren().addAll(titleRow, infoStrip);
        return header;
    }

    private static VBox infoCol(String key, String value, String valueColor) {
        VBox col = new VBox(4);
        Label k = new Label(key);
        k.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: rgba(255,255,255,0.45);");
        Label v = new Label(value);
        v.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: " + valueColor + ";");
        v.setMaxWidth(240);
        col.getChildren().addAll(k, v);
        return col;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  BODY
    // ══════════════════════════════════════════════════════════════════════
    private static VBox buildBody(List<Entretien> entretiens, ToggleGroup tg,
                                  VBox[] listHolder, String nom, String offre) {
        VBox body = new VBox(0);
        body.setStyle("-fx-background-color: #F8FAFC;");

        boolean has = entretiens != null && !entretiens.isEmpty();

        if (has) {
            // Alerte bleue
            VBox intro = new VBox(10);
            intro.setStyle("-fx-padding: 24 32 20 32;");
            HBox alertBox = new HBox(12);
            alertBox.setAlignment(Pos.CENTER_LEFT);
            alertBox.setStyle(
                    "-fx-background-color: #EFF6FF; -fx-background-radius: 12;" +
                            "-fx-border-color: #BFDBFE; -fx-border-width: 1; -fx-border-radius: 12;" +
                            "-fx-padding: 14 18;"
            );
            Label ai = new Label("ℹ️"); ai.setStyle("-fx-font-size: 18px;");
            Label at = new Label(
                    "Des entretiens ont déjà été planifiés pour cette offre.\n" +
                            "Sélectionnez-en un pour y ajouter " +
                            (nom != null && !nom.isBlank() ? "« " + nom + " »" : "ce candidat") +
                            ", ou créez un entretien indépendant."
            );
            at.setStyle("-fx-font-size: 13px; -fx-text-fill: #1E40AF; -fx-line-spacing: 3;");
            at.setWrapText(true);
            HBox.setHgrow(at, Priority.ALWAYS);
            alertBox.getChildren().addAll(ai, at);
            intro.getChildren().add(alertBox);
            body.getChildren().add(intro);

            // Liste
            VBox listSection = new VBox(10);
            listSection.setStyle("-fx-padding: 0 32 0 32;");

            HBox sh = new HBox(10); sh.setAlignment(Pos.CENTER_LEFT);
            Label counter = new Label(String.valueOf(entretiens.size()));
            counter.setStyle(
                    "-fx-background-color: " + BLUE_PRIMARY + "; -fx-text-fill: white;" +
                            "-fx-font-size: 11px; -fx-font-weight: 800; -fx-padding: 2 9; -fx-background-radius: 30;"
            );
            Label sl = new Label("Entretiens existants pour cette offre");
            sl.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: " + BLUE_DARK + ";");
            sh.getChildren().addAll(counter, sl);
            listSection.getChildren().add(sh);

            VBox list = new VBox(10);
            list.setStyle("-fx-background-color: transparent;");
            listHolder[0] = list;

            DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm");

            for (Entretien ent : entretiens) {
                RadioButton rb = new RadioButton();
                rb.setToggleGroup(tg); rb.setUserData(ent);
                rb.setVisible(false); rb.setManaged(false);

                HBox card = entretienCard(ent, df, tf);
                final RadioButton rRef = rb;
                card.setOnMouseClicked(ev -> { tg.selectToggle(rRef); updateCards(list, card); });
                list.getChildren().addAll(rb, card);
            }

            ScrollPane scroll = new ScrollPane(list);
            scroll.setFitToWidth(true);
            scroll.setPrefHeight(Math.min(entretiens.size() * 105, 230));
            scroll.setMaxHeight(230);
            scroll.setStyle(
                    "-fx-background-color: transparent; -fx-background: transparent;" +
                            "-fx-border-color: transparent; -fx-padding: 2 0;"
            );
            listSection.getChildren().add(scroll);
            body.getChildren().add(listSection);
            body.getChildren().add(orSeparator());

        } else {
            // Premier entretien
            VBox z = new VBox(12); z.setAlignment(Pos.CENTER);
            z.setStyle("-fx-padding: 30 32 24 32;");
            Label icon = new Label("🎉"); icon.setStyle("-fx-font-size: 40px;");
            Label title = new Label("Premier entretien pour cette offre !");
            title.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: " + BLUE_DARK + ";");
            Label desc = new Label(
                    "Aucun entretien n'existe encore pour l'offre " +
                            (offre != null ? "« " + offre + " »" : "") + ".\nVous créez le premier !"
            );
            desc.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B;");
            desc.setWrapText(true); desc.setMaxWidth(480);
            desc.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            z.getChildren().addAll(icon, title, desc);
            body.getChildren().add(z);
        }
        return body;
    }

    private static HBox orSeparator() {
        HBox sep = new HBox(16); sep.setAlignment(Pos.CENTER);
        sep.setStyle("-fx-padding: 18 32 14 32;");
        Region l = new Region(); l.setPrefHeight(1); l.setStyle("-fx-background-color: #E2E8F0;");
        HBox.setHgrow(l, Priority.ALWAYS);
        Label ou = new Label("ou créez un nouveau");
        ou.setStyle(
                "-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #94A3B8;" +
                        "-fx-background-color: #F8FAFC; -fx-padding: 6 16;" +
                        "-fx-background-radius: 20; -fx-border-color: #E2E8F0;" +
                        "-fx-border-radius: 20; -fx-border-width: 1;"
        );
        Region r = new Region(); r.setPrefHeight(1); r.setStyle("-fx-background-color: #E2E8F0;");
        HBox.setHgrow(r, Priority.ALWAYS);
        sep.getChildren().addAll(l, ou, r);
        return sep;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  FOOTER
    // ══════════════════════════════════════════════════════════════════════
    private static HBox buildFooter(Stage dialog, DialogResult[] result, ToggleGroup tg) {
        HBox footer = new HBox(12); footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle(
                "-fx-background-color: white; -fx-background-radius: 0 0 20 20;" +
                        "-fx-padding: 18 32 24 32; -fx-border-color: #F1F5F9; -fx-border-width: 1 0 0 0;"
        );

        // Annuler
        Button btnAnn = btn("✕  Annuler",
                "-fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-font-size: 13px;" +
                        "-fx-font-weight: 700; -fx-background-radius: 10; -fx-border-color: #E2E8F0;" +
                        "-fx-border-width: 1.5; -fx-border-radius: 10; -fx-cursor: hand; -fx-padding: 10 22;",
                "-fx-background-color: #E2E8F0; -fx-text-fill: #334155; -fx-font-size: 13px;" +
                        "-fx-font-weight: 700; -fx-background-radius: 10; -fx-border-color: #CBD5E1;" +
                        "-fx-border-width: 1.5; -fx-border-radius: 10; -fx-cursor: hand; -fx-padding: 10 22;"
        );
        btnAnn.setPrefHeight(44);
        btnAnn.setOnAction(e -> dialog.close());

        // Rejoindre
        final String sOff =
                "-fx-background-color: #E2E8F0; -fx-text-fill: #94A3B8; -fx-font-size: 13px;" +
                        "-fx-font-weight: 700; -fx-background-radius: 10; -fx-cursor: default; -fx-padding: 10 22;";
        final String sOn =
                "-fx-background-color: " + GRAD_BLUE + "; -fx-text-fill: white; -fx-font-size: 13px;" +
                        "-fx-font-weight: 800; -fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 10 22;" +
                        "-fx-effect: dropshadow(gaussian, rgba(37,99,235,0.4), 12, 0, 0, 4);";
        final String sOnH =
                "-fx-background-color: " + GRAD_BLUE_HOVER + "; -fx-text-fill: white; -fx-font-size: 13px;" +
                        "-fx-font-weight: 800; -fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 10 22;" +
                        "-fx-effect: dropshadow(gaussian, rgba(37,99,235,0.55), 16, 0, 0, 5);";

        Button btnRej = new Button("🔗  Rejoindre l'entretien");
        btnRej.setPrefHeight(44); btnRej.setDisable(true); btnRej.setStyle(sOff);

        tg.selectedToggleProperty().addListener((obs, old, nv) -> {
            boolean sel = nv != null;
            btnRej.setDisable(!sel);
            btnRej.setStyle(sel ? sOn : sOff);
            if (sel) {
                btnRej.setOnMouseEntered(e -> btnRej.setStyle(sOnH));
                btnRej.setOnMouseExited( e -> btnRej.setStyle(sOn));
            }
        });
        btnRej.setOnAction(e -> {
            Toggle sel = tg.getSelectedToggle();
            if (sel != null) result[0] = new DialogResult(Action.REJOINDRE_EXISTANT, (Entretien) sel.getUserData());
            dialog.close();
        });

        // Créer nouveau — orange
        Button btnCree = btn("✚  Nouvel entretien",
                "-fx-background-color: " + GRAD_ORANGE + "; -fx-text-fill: white; -fx-font-size: 13px;" +
                        "-fx-font-weight: 800; -fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 10 22;" +
                        "-fx-effect: dropshadow(gaussian, rgba(249,115,22,0.4), 12, 0, 0, 4);",
                "-fx-background-color: " + GRAD_ORANGE_HOVER + "; -fx-text-fill: white; -fx-font-size: 13px;" +
                        "-fx-font-weight: 800; -fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 10 22;" +
                        "-fx-effect: dropshadow(gaussian, rgba(249,115,22,0.6), 18, 0, 0, 5);"
        );
        btnCree.setPrefHeight(44);
        btnCree.setOnAction(e -> { result[0] = new DialogResult(Action.CREER_NOUVEAU, null); dialog.close(); });

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        footer.getChildren().addAll(spacer, btnAnn, btnRej, btnCree);
        return footer;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  CARTE ENTRETIEN
    // ══════════════════════════════════════════════════════════════════════
    private static HBox entretienCard(Entretien ent, DateTimeFormatter df, DateTimeFormatter tf) {
        HBox card = new HBox(16);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPrefWidth(Double.MAX_VALUE);
        cardDefault(card);

        // Radio visuel
        StackPane radio = new StackPane(); radio.setPrefSize(22, 22); radio.setMinSize(22, 22);
        Circle outer = new Circle(10); outer.setFill(Color.TRANSPARENT);
        outer.setStroke(Color.web("#CBD5E1")); outer.setStrokeWidth(2);
        Circle inner = new Circle(5); inner.setFill(Color.TRANSPARENT);
        radio.getChildren().addAll(outer, inner);
        card.setUserData(new Circle[]{ outer, inner });

        // Icône type dans carré
        boolean pres = "présentiel".equalsIgnoreCase(ent.getTypeEntretien());
        StackPane typeBox = new StackPane(); typeBox.setPrefSize(44, 44); typeBox.setMinSize(44, 44);
        Region ibg = new Region(); ibg.setPrefSize(44, 44);
        ibg.setStyle("-fx-background-color: " + (pres ? "#EDE9FE" : "#DBEAFE") + "; -fx-background-radius: 12;");
        Label ilbl = new Label(pres ? "🏢" : "💻"); ilbl.setStyle("-fx-font-size: 18px;");
        typeBox.getChildren().addAll(ibg, ilbl);

        // Infos texte
        VBox info = new VBox(6); HBox.setHgrow(info, Priority.ALWAYS);

        String ds = ent.getDateEntretien() != null ? ent.getDateEntretien().toLocalDate().format(df) : "—";
        String h1 = ent.getHeureDebut() != null ? ent.getHeureDebut().toLocalTime().format(tf) : "--:--";
        String h2 = ent.getHeureFin()   != null ? ent.getHeureFin().toLocalTime().format(tf)   : "--:--";
        long dur  = (ent.getHeureDebut() != null && ent.getHeureFin() != null)
                ? TimeUnit.MILLISECONDS.toMinutes(ent.getHeureFin().getTime() - ent.getHeureDebut().getTime()) : 0;

        Label dateL = new Label("📅  " + ds + "   ·   " + h1 + " → " + h2 + (dur > 0 ? "   (" + dur + " min)" : ""));
        dateL.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: " + BLUE_DARK + ";");

        HBox meta = new HBox(10); meta.setAlignment(Pos.CENTER_LEFT);
        Label typeBadge = badge(pres ? "Présentiel" : "Visioconférence",
                pres ? "#EDE9FE" : "#DBEAFE", pres ? "#7C3AED" : "#1D4ED8");
        String lieuTxt = pres
                ? (ent.getLieu() != null && !ent.getLieu().isBlank() ? "📍 " + ent.getLieu() : "📍 Lieu à définir")
                : "🔗 Visioconférence";
        Label lieuL = new Label(lieuTxt);
        lieuL.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;"); lieuL.setMaxWidth(220);
        meta.getChildren().addAll(typeBadge, lieuL);
        info.getChildren().addAll(dateL, meta);

        // Statut
        Label statBadge = badge(
                statIcon(ent.getStatut()) + " " + (ent.getStatut() != null ? ent.getStatut() : "—"),
                statBg(ent.getStatut()), statColor(ent.getStatut())
        );
        VBox sw = new VBox(); sw.setAlignment(Pos.CENTER); sw.getChildren().add(statBadge);

        card.getChildren().addAll(radio, typeBox, info, sw);
        card.setOnMouseEntered(e -> { if (!isSel(card)) cardHover(card); });
        card.setOnMouseExited( e -> { if (!isSel(card)) cardDefault(card); });
        return card;
    }

    private static void updateCards(VBox list, HBox selected) {
        for (javafx.scene.Node n : list.getChildren()) {
            if (!(n instanceof HBox)) continue;
            HBox c = (HBox) n;
            if (!(c.getUserData() instanceof Circle[])) continue;
            Circle[] circles = (Circle[]) c.getUserData();
            if (c == selected) {
                cardSelected(c);
                circles[0].setStroke(Color.web(BLUE_PRIMARY));
                circles[1].setFill(Color.web(BLUE_PRIMARY));
            } else {
                cardDefault(c);
                circles[0].setStroke(Color.web("#CBD5E1"));
                circles[1].setFill(Color.TRANSPARENT);
            }
        }
    }

    private static void cardDefault(HBox c) {
        c.setStyle("-fx-background-color: white; -fx-background-radius: 14;" +
                "-fx-border-color: #E2E8F0; -fx-border-width: 1.5; -fx-border-radius: 14;" +
                "-fx-padding: 14 18; -fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 8, 0, 0, 2);");
    }
    private static void cardHover(HBox c) {
        c.setStyle("-fx-background-color: #F8FAFF; -fx-background-radius: 14;" +
                "-fx-border-color: #93C5FD; -fx-border-width: 1.5; -fx-border-radius: 14;" +
                "-fx-padding: 14 18; -fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(37,99,235,0.12), 12, 0, 0, 3);");
    }
    private static void cardSelected(HBox c) {
        c.setStyle("-fx-background-color: #EFF6FF; -fx-background-radius: 14;" +
                "-fx-border-color: " + BLUE_PRIMARY + "; -fx-border-width: 2; -fx-border-radius: 14;" +
                "-fx-padding: 14 18; -fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(37,99,235,0.22), 14, 0, 0, 4);");
    }
    private static boolean isSel(HBox c) {
        return c.getStyle() != null && c.getStyle().contains("#EFF6FF");
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private static Button btn(String text, String sNorm, String sHov) {
        Button b = new Button(text); b.setStyle(sNorm);
        b.setOnMouseEntered(e -> b.setStyle(sHov));
        b.setOnMouseExited( e -> b.setStyle(sNorm));
        return b;
    }
    private static Label badge(String text, String bg, String fg) {
        Label l = new Label(text);
        l.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg + ";" +
                "-fx-font-size: 11px; -fx-font-weight: 700; -fx-padding: 4 12; -fx-background-radius: 30;");
        return l;
    }
    private static String statIcon(String s) {
        if (s == null) return "⚪";
        switch (s.toLowerCase()) { case "proposé": return "🟡"; case "confirmé": return "🟢";
            case "réalisé": return "🔵"; case "annulé": return "🔴"; default: return "⚪"; }
    }
    private static String statBg(String s) {
        if (s == null) return "#F1F5F9";
        switch (s.toLowerCase()) { case "proposé": return "#FEF3C7"; case "confirmé": return "#D1FAE5";
            case "réalisé": return "#DBEAFE"; case "annulé": return "#FEE2E2"; default: return "#F1F5F9"; }
    }
    private static String statColor(String s) {
        if (s == null) return "#64748B";
        switch (s.toLowerCase()) { case "proposé": return "#D97706"; case "confirmé": return "#059669";
            case "réalisé": return "#1D4ED8"; case "annulé": return "#DC2626"; default: return "#64748B"; }
    }
}