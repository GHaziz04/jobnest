package tn.jobnest.gentretien.controller;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.ArcType;
import javafx.stage.Stage;
import tn.jobnest.gentretien.service.MatchingService.DetailCompetence;
import tn.jobnest.gentretien.service.MatchingService.DetailExperience;
import tn.jobnest.gentretien.service.MatchingService.MatchingResult;

/**
 * ════════════════════════════════════════════════════════════════════════════
 *  MatchingResultController  —  Design Dark Premium (JavaFX pur)
 *
 *  Reproduit fidèlement la maquette HTML :
 *   • Hero sombre avec gauge en arc Canvas
 *   • Barre d'accent tricolore
 *   • Tableau de matching ligne par ligne
 *   • Score monospace + mini-barre inline
 * ════════════════════════════════════════════════════════════════════════════
 */
public class MatchingResultController {

    @FXML private VBox rootVBox;

    // ─── Palette ──────────────────────────────────────────────────────────────
    private static final String C_BG_DEEP   = "#0F1623";
    private static final String C_BG_HERO   = "#1A2540";
    private static final String C_BG_BODY   = "#151E2E";
    private static final String C_BG_CARD   = "#1C2537";
    private static final String C_BG_HEADER = "#1E2D47";
    private static final String C_BG_HDR_E  = "#1E2A30";
    private static final String C_ROW_ODD   = "#212D42";
    private static final String C_ROW_EVEN  = "#1C2537";
    private static final String C_ROW_HOVER = "#263150";

    private static final String C_BLEU      = "#4B7BEC";
    private static final String C_VERT      = "#10B981";
    private static final String C_ORANGE    = "#F59E0B";
    private static final String C_ROUGE     = "#EF4444";

    private static final String C_TEXT      = "#CBD5E1";
    private static final String C_TEXT_DIM  = "#64748B";
    private static final String C_TEXT_DARK = "#334155";
    private static final String C_BORDER_B  = "#263252";
    private static final String C_BORDER_G  = "#2D3748";

    // ════════════════════════════════════════════════════════════════════════
    //  POINT D'ENTRÉE PUBLIC
    // ════════════════════════════════════════════════════════════════════════

    public void afficherResultat(MatchingResult result, String nomCandidat) {
        if (rootVBox == null) return;
        rootVBox.getChildren().clear();
        rootVBox.setStyle(bg(C_BG_DEEP));

        // 1 — Hero
        rootVBox.getChildren().add(construireHero(result, nomCandidat));

        // 2 — Barre accent tricolore
        rootVBox.getChildren().add(construireAccent());

        // 3 — Corps scrollable
        rootVBox.getChildren().add(construireCorps(result));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  HERO
    // ════════════════════════════════════════════════════════════════════════

    private VBox construireHero(MatchingResult result, String nomCandidat) {
        VBox hero = new VBox(0);
        hero.setStyle(
                "-fx-background-color:linear-gradient(to bottom right," + C_BG_DEEP + "," + C_BG_HERO + ");" +
                        "-fx-padding:28 32 26 32;");

        // ── Top bar ──────────────────────────────────────────────────────────
        HBox topBar = new HBox();
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(0, 0, 22, 0));

        VBox titreZone = new VBox(3);
        HBox.setHgrow(titreZone, Priority.ALWAYS);
        Label eyebrow = label("ANALYSE DE CANDIDATURE",
                "font-family:'Courier New'; font-size:10px; font-weight:bold; " +
                        "text-fill:" + C_BLEU + "; -fx-letter-spacing:3;");
        Label titre = label("Rapport de Matching",
                "font-family:'Georgia'; font-size:22px; font-weight:bold; text-fill:white;");
        titreZone.getChildren().addAll(eyebrow, titre);

        Button btnClose = new Button("✕");
        btnClose.setStyle(
                "-fx-background-color:rgba(255,255,255,0.06);" +
                        "-fx-text-fill:#94A3B8; -fx-font-size:15px;" +
                        "-fx-background-radius:50; -fx-cursor:hand;" +
                        "-fx-min-width:36; -fx-min-height:36;" +
                        "-fx-border-color:rgba(255,255,255,0.12);" +
                        "-fx-border-radius:50; -fx-border-width:1;");
        btnClose.setOnAction(e -> fermer());
        topBar.getChildren().addAll(titreZone, btnClose);

        // ── Gauge + métriques ─────────────────────────────────────────────────
        HBox bodyRow = new HBox(28);
        bodyRow.setAlignment(Pos.CENTER_LEFT);

        bodyRow.getChildren().addAll(
                construireGauge(result.scoreFinal),
                construireMetriques(result, nomCandidat)
        );

        hero.getChildren().addAll(topBar, bodyRow);
        return hero;
    }

    // ── Gauge arc Canvas ──────────────────────────────────────────────────────

    private StackPane construireGauge(double score) {
        StackPane pane = new StackPane();
        pane.setPrefSize(160, 160);
        pane.setMinSize(160, 160);
        pane.setMaxSize(160, 160);
        pane.setStyle(
                "-fx-background-color:rgba(255,255,255,0.04);" +
                        "-fx-background-radius:80;" +
                        "-fx-border-color:rgba(255,255,255,0.08);" +
                        "-fx-border-radius:80; -fx-border-width:1;");

        // Canvas pour l'arc
        Canvas canvas = new Canvas(160, 160);
        dessinerArc(canvas, score);

        // Texte central
        VBox center = new VBox(3);
        center.setAlignment(Pos.CENTER);
        String couleur = couleurScore(score);
        Label lblPct = new Label(String.format("%.0f%%", score));
        lblPct.setStyle("-fx-font-family:'Georgia'; -fx-font-size:40px; " +
                "-fx-font-weight:bold; -fx-text-fill:" + couleur + ";");
        Label lblSub = new Label("SCORE FINAL");
        lblSub.setStyle("-fx-font-family:'Courier New'; -fx-font-size:8px; " +
                "-fx-text-fill:" + C_BLEU + "; -fx-letter-spacing:2;");
        center.getChildren().addAll(lblPct, lblSub);

        pane.getChildren().addAll(canvas, center);
        return pane;
    }

    private void dessinerArc(Canvas canvas, double score) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double cx = 80, cy = 80, r = 64, ep = 9;
        gc.clearRect(0, 0, 160, 160);

        // Fond de l'arc
        gc.setStroke(Color.web("#263150"));
        gc.setLineWidth(ep);
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.strokeArc(cx - r, cy - r, r * 2, r * 2, -210, 240, ArcType.OPEN);

        // Arc coloré
        if (score > 0) {
            double angleBalayage = (score / 100.0) * 240;
            Color couleur = score >= 80 ? Color.web(C_VERT)
                    : score >= 40 ? Color.web(C_ORANGE)
                    : Color.web(C_ROUGE);
            gc.setStroke(couleur);
            gc.setLineWidth(ep);
            gc.strokeArc(cx - r, cy - r, r * 2, r * 2, -210, angleBalayage, ArcType.OPEN);

            // Point terminal lumineux
            if (score > 3) {
                double rad = Math.toRadians(-(-210 + angleBalayage));
                double px  = cx + r * Math.cos(rad);
                double py  = cy + r * Math.sin(rad);
                // Halo
                gc.setFill(Color.web(score >= 80 ? C_VERT : score >= 40 ? C_ORANGE : C_ROUGE, 0.25));
                gc.fillOval(px - 9, py - 9, 18, 18);
                // Point
                gc.setFill(couleur);
                gc.fillOval(px - 5, py - 5, 10, 10);
            }
        }
    }

    // ── Métriques droite ──────────────────────────────────────────────────────

    private VBox construireMetriques(MatchingResult result, String nomCandidat) {
        VBox metrics = new VBox(14);
        HBox.setHgrow(metrics, Priority.ALWAYS);

        // Badge décision
        metrics.getChildren().add(construireDecision(result));

        // Candidat
        HBox candRow = new HBox(8);
        candRow.setAlignment(Pos.CENTER_LEFT);
        candRow.getChildren().addAll(
                label("👤", "font-size:14px;"),
                label(nomCandidat != null ? nomCandidat : "—",
                        "font-size:14px; font-weight:600; text-fill:" + C_TEXT + ";")
        );
        metrics.getChildren().add(candRow);

        // Barres scores
        VBox barres = new VBox(12);
        barres.getChildren().addAll(
                construireBarre("Compétences", result.scoreCompetences, "60%", C_BLEU),
                construireBarre("Expériences",  result.scoreExperiences, "40%", C_VERT)
        );
        metrics.getChildren().add(barres);
        return metrics;
    }

    private Label construireDecision(MatchingResult result) {
        String txt, bg, fg, border;
        switch (result.decision) {
            case "ACCEPTÉ":
                txt = "✓  Candidature ACCEPTÉE automatiquement";
                bg = "rgba(16,185,129,0.12)"; fg = C_VERT; border = C_VERT; break;
            case "EN_REVISION":
                txt = "⟳  Mise EN RÉVISION — décision manuelle requise";
                bg = "rgba(245,158,11,0.12)"; fg = C_ORANGE; border = C_ORANGE; break;
            default:
                txt = "✗  Candidature REFUSÉE automatiquement";
                bg = "rgba(239,68,68,0.12)"; fg = C_ROUGE; border = C_ROUGE;
        }
        Label l = new Label(txt);
        l.setMaxWidth(Double.MAX_VALUE);
        l.setStyle(
                "-fx-font-size:13px; -fx-font-weight:bold;" +
                        "-fx-padding:10 18; -fx-background-radius:8;" +
                        "-fx-text-fill:" + fg + ";" +
                        "-fx-background-color:" + bg + ";" +
                        "-fx-border-color:" + border + ";" +
                        "-fx-border-width:1; -fx-border-radius:8;");
        return l;
    }

    private VBox construireBarre(String libelle, double score, String poids, String couleur) {
        VBox box = new VBox(5);

        // Ligne top
        HBox top = new HBox();
        top.setAlignment(Pos.CENTER_LEFT);
        Label lbl = label(libelle, "font-size:11px; font-weight:700; text-fill:" + C_TEXT_DIM + ";");
        HBox.setHgrow(lbl, Priority.ALWAYS);
        Label valLbl = label(String.format("%.1f%%", score),
                "font-size:12px; font-weight:800; text-fill:" + couleur + ";");
        Label poidsLbl = label("  · " + poids, "font-size:10px; text-fill:" + C_TEXT_DARK + ";");
        top.getChildren().addAll(lbl, valLbl, poidsLbl);

        // Track + fill
        StackPane track = new StackPane();
        track.setPrefHeight(8);
        Region fond = new Region();
        fond.setPrefHeight(8);
        fond.setMaxWidth(Double.MAX_VALUE);
        fond.setStyle("-fx-background-color:rgba(255,255,255,0.06); -fx-background-radius:4;");
        Region fill = new Region();
        fill.setPrefHeight(8);
        fill.setPrefWidth(Math.min(score / 100.0 * 400, 400));
        fill.setStyle("-fx-background-color:" + couleur + "; -fx-background-radius:4;");
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);
        track.getChildren().addAll(fond, fill);

        box.getChildren().addAll(top, track);
        return box;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  BARRE ACCENT
    // ════════════════════════════════════════════════════════════════════════

    private Region construireAccent() {
        Region r = new Region();
        r.setPrefHeight(3);
        r.setMaxWidth(Double.MAX_VALUE);
        r.setStyle("-fx-background-color:linear-gradient(to right," +
                C_BLEU + "," + C_VERT + "," + C_ORANGE + ");");
        return r;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CORPS (ScrollPane)
    // ════════════════════════════════════════════════════════════════════════

    private ScrollPane construireCorps(MatchingResult result) {
        VBox body = new VBox(20);
        body.setPadding(new Insets(24, 28, 24, 28));
        body.setStyle(bg(C_BG_BODY));

        // Section compétences
        body.getChildren().add(construireSectionCompetences(result));

        // Section expériences
        body.getChildren().add(construireSectionExperiences(result));

        // Bouton fermer
        body.getChildren().add(construireBoutonFermer());

        ScrollPane sp = new ScrollPane(body);
        sp.setFitToWidth(true);
        sp.setPrefHeight(540);
        sp.setStyle(
                "-fx-background:" + C_BG_BODY + ";" +
                        "-fx-background-color:" + C_BG_BODY + ";" +
                        "-fx-border-color:transparent;");
        VBox.setVgrow(sp, Priority.ALWAYS);
        return sp;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  SECTION COMPÉTENCES
    // ════════════════════════════════════════════════════════════════════════

    private VBox construireSectionCompetences(MatchingResult result) {
        int nb = result.detailsCompetences != null ? result.detailsCompetences.size() : 0;
        VBox section = construireEnveloppeSectionHeader(
                "◈", "COMPÉTENCES TECHNIQUES",
                nb + " critère" + (nb > 1 ? "s" : "") + " analysé" + (nb > 1 ? "s" : ""),
                C_BLEU, C_BG_HEADER, C_BORDER_B);

        VBox tableau = new VBox(0);
        if (result.detailsCompetences == null || result.detailsCompetences.isEmpty()) {
            tableau.getChildren().add(ligneVide("Aucune compétence définie pour cette offre"));
        } else {
            for (int i = 0; i < result.detailsCompetences.size(); i++) {
                tableau.getChildren().add(creerLigneCompetence(result.detailsCompetences.get(i), i));
            }
        }
        section.getChildren().add(tableau);
        return section;
    }

    private HBox creerLigneCompetence(DetailCompetence d, int index) {
        String bg = index % 2 == 0 ? C_ROW_ODD : C_ROW_EVEN;
        String couleur = couleurScore(d.scoreObtenu);
        boolean trouve = !"—".equals(d.nomCandidat);

        HBox ligne = new HBox(0);
        ligne.setAlignment(Pos.CENTER_LEFT);
        ligne.setPrefHeight(52);
        ligne.setStyle(bg(bg));

        // Bande couleur gauche
        Region bande = new Region();
        bande.setPrefWidth(4);
        bande.setPrefHeight(52);
        bande.setStyle("-fx-background-color:" + couleur + ";");

        // Numéro
        Label num = creerNum(index + 1, bg);

        // Cellule requise (bleu)
        VBox cellReq = creerCellule(
                d.nomRequis,
                d.niveauRequis != null ? d.niveauRequis.toUpperCase() : "N/A",
                C_BLEU, bg, 185);

        // Flèche
        Label fleche = creerFleche(bg);

        // Cellule candidat (vert ou rouge)
        VBox cellCand = creerCellule(
                trouve ? d.nomCandidat : "Non trouvé",
                trouve ? (d.niveauCandidat != null ? d.niveauCandidat.toUpperCase() : "N/A") : "—",
                trouve ? C_VERT : C_ROUGE, bg, 185);

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        spacer.setStyle(bg(bg));

        // Zone score
        VBox scoreZone = creerScoreZone(d.scoreObtenu, couleur, bg);

        ligne.getChildren().addAll(bande, num, cellReq, fleche, cellCand, spacer, scoreZone);
        return ligne;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  SECTION EXPÉRIENCES
    // ════════════════════════════════════════════════════════════════════════

    private VBox construireSectionExperiences(MatchingResult result) {
        int nb = result.detailsExperiences != null ? result.detailsExperiences.size() : 0;
        VBox section = construireEnveloppeSectionHeader(
                "◈", "EXPÉRIENCES PROFESSIONNELLES",
                nb + " critère" + (nb > 1 ? "s" : "") + " analysé" + (nb > 1 ? "s" : ""),
                C_VERT, C_BG_HDR_E, C_BORDER_G);

        VBox tableau = new VBox(0);
        if (result.detailsExperiences == null || result.detailsExperiences.isEmpty()) {
            tableau.getChildren().add(ligneVide("Aucune expérience définie pour cette offre"));
        } else {
            for (int i = 0; i < result.detailsExperiences.size(); i++) {
                tableau.getChildren().add(creerLigneExperience(result.detailsExperiences.get(i), i));
            }
        }
        section.getChildren().add(tableau);
        return section;
    }

    private HBox creerLigneExperience(DetailExperience d, int index) {
        String bg = index % 2 == 0 ? C_ROW_ODD : C_ROW_EVEN;
        String couleur = couleurScore(d.scoreObtenu);
        boolean trouve = !"—".equals(d.posteCandidat);

        HBox ligne = new HBox(0);
        ligne.setAlignment(Pos.CENTER_LEFT);
        ligne.setPrefHeight(52);
        ligne.setStyle(bg(bg));

        // Bande
        Region bande = new Region();
        bande.setPrefWidth(4);
        bande.setPrefHeight(52);
        bande.setStyle("-fx-background-color:" + couleur + ";");

        // Numéro
        Label num = creerNum(index + 1, bg);

        // Cellule requise (orange)
        String reqAns = d.anneesRequises > 0
                ? String.format("%.0f an%s requis", d.anneesRequises, d.anneesRequises >= 2 ? "s" : "")
                : "Durée libre";
        VBox cellReq = creerCellule(d.nomRequis, reqAns, C_ORANGE, bg, 185);

        // Flèche
        Label fleche = creerFleche(bg);

        // Cellule candidat
        String candAns = trouve
                ? String.format("%.1f an%s", d.anneesCandidat, d.anneesCandidat >= 2 ? "s" : "")
                : "—";
        // Mention manque d'années
        if (trouve && d.anneesRequises > 0 && d.anneesCandidat < d.anneesRequises) {
            double manque = d.anneesRequises - d.anneesCandidat;
            candAns += String.format("  ⚠ -%.0f an%s", manque, manque >= 2 ? "s" : "");
        }
        VBox cellCand = creerCellule(
                trouve ? d.posteCandidat : "Non trouvé",
                candAns,
                trouve ? C_VERT : C_ROUGE, bg, 185);

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        spacer.setStyle(bg(bg));

        // Zone score
        VBox scoreZone = creerScoreZone(d.scoreObtenu, couleur, bg);

        ligne.getChildren().addAll(bande, num, cellReq, fleche, cellCand, spacer, scoreZone);
        return ligne;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  COMPOSANTS RÉUTILISABLES
    // ════════════════════════════════════════════════════════════════════════

    /** Enveloppe section avec header coloré + border */
    private VBox construireEnveloppeSectionHeader(
            String icone, String titre, String compte,
            String couleur, String bgHeader, String borderColor) {

        VBox section = new VBox(0);
        section.setStyle(
                "-fx-background-color:" + C_BG_CARD + ";" +
                        "-fx-background-radius:14;" +
                        "-fx-border-color:" + borderColor + ";" +
                        "-fx-border-width:1; -fx-border-radius:14;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.4),16,0,0,6);");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(13, 20, 13, 20));
        header.setStyle(
                "-fx-background-color:" + bgHeader + ";" +
                        "-fx-background-radius:14 14 0 0;" +
                        "-fx-border-color:" + borderColor + ";" +
                        "-fx-border-width:0 0 1 0;");

        Label ico = new Label(icone);
        ico.setStyle("-fx-font-size:15px; -fx-text-fill:" + couleur + ";");

        Label lTitre = new Label(titre);
        lTitre.setStyle("-fx-font-family:'Courier New'; -fx-font-size:11px; " +
                "-fx-font-weight:bold; -fx-text-fill:" + couleur + "; -fx-letter-spacing:2;");
        HBox.setHgrow(lTitre, Priority.ALWAYS);

        Label lCompte = new Label(compte);
        lCompte.setStyle("-fx-font-size:10px; -fx-text-fill:" + C_TEXT_DIM + ";");

        header.getChildren().addAll(ico, lTitre, lCompte);
        section.getChildren().add(header);
        return section;
    }

    /** Cellule : titre + sous-titre coloré */
    private VBox creerCellule(String titre, String sous, String couleurSous, String bg, double largeur) {
        VBox box = new VBox(2);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPrefWidth(largeur);
        box.setPrefHeight(52);
        box.setPadding(new Insets(6, 10, 6, 10));
        box.setStyle(bg(bg));

        Label lTitre = new Label(titre);
        lTitre.setStyle("-fx-font-size:12px; -fx-font-weight:700; -fx-text-fill:" + C_TEXT + ";");
        lTitre.setWrapText(false);

        Label lSous = new Label(sous);
        lSous.setStyle("-fx-font-family:'Courier New'; -fx-font-size:10px; " +
                "-fx-font-weight:600; -fx-text-fill:" + couleurSous + ";");

        box.getChildren().addAll(lTitre, lSous);
        return box;
    }

    /** Numéro de ligne (ex: 01, 02) */
    private Label creerNum(int index, String bg) {
        Label l = new Label(String.format("%02d", index));
        l.setPrefWidth(36);
        l.setMinWidth(36);
        l.setAlignment(Pos.CENTER);
        l.setStyle("-fx-font-family:'Courier New'; -fx-font-size:11px; " +
                "-fx-font-weight:bold; -fx-text-fill:" + C_TEXT_DARK + "; " +
                "-fx-background-color:" + bg + "; -fx-padding:0 8;");
        return l;
    }

    /** Flèche centrale → */
    private Label creerFleche(String bg) {
        Label l = new Label("→");
        l.setPrefWidth(32);
        l.setMinWidth(32);
        l.setAlignment(Pos.CENTER);
        l.setStyle("-fx-font-size:14px; -fx-text-fill:" + C_TEXT_DARK + "; " +
                "-fx-background-color:" + bg + ";");
        return l;
    }

    /** Zone score : % + mini barre */
    private VBox creerScoreZone(double score, String couleur, String bg) {
        VBox zone = new VBox(4);
        zone.setAlignment(Pos.CENTER_RIGHT);
        zone.setPrefWidth(100);
        zone.setPrefHeight(52);
        zone.setPadding(new Insets(8, 16, 8, 0));
        zone.setStyle(bg(bg));

        Label pct = new Label(String.format("%.0f%%", score));
        pct.setStyle("-fx-font-family:'Courier New'; -fx-font-size:15px; " +
                "-fx-font-weight:bold; -fx-text-fill:" + couleur + ";");

        // Mini barre
        StackPane miniBar = new StackPane();
        miniBar.setPrefHeight(4);
        miniBar.setPrefWidth(80);
        Region fond = new Region();
        fond.setPrefHeight(4);
        fond.setMaxWidth(Double.MAX_VALUE);
        fond.setStyle("-fx-background-color:rgba(255,255,255,0.07); -fx-background-radius:2;");
        Region fill = new Region();
        fill.setPrefHeight(4);
        fill.setPrefWidth(Math.min(score / 100.0 * 80, 80));
        fill.setStyle("-fx-background-color:" + couleur + "; -fx-background-radius:2;");
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);
        miniBar.getChildren().addAll(fond, fill);

        zone.getChildren().addAll(pct, miniBar);
        return zone;
    }

    /** Ligne vide (message) */
    private HBox ligneVide(String msg) {
        HBox box = new HBox();
        box.setPadding(new Insets(14, 16, 14, 16));
        Label l = new Label(msg);
        l.setStyle("-fx-font-size:12px; -fx-text-fill:" + C_TEXT_DIM + "; -fx-font-style:italic;");
        box.getChildren().add(l);
        return box;
    }

    /** Bouton fermer en bas */
    private Button construireBoutonFermer() {
        Button btn = new Button("Fermer le rapport");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(44);
        btn.setStyle(
                "-fx-background-color:rgba(75,123,236,0.10);" +
                        "-fx-text-fill:" + C_BLEU + ";" +
                        "-fx-font-weight:700; -fx-font-size:13px;" +
                        "-fx-background-radius:10; -fx-cursor:hand;" +
                        "-fx-border-color:" + C_BLEU + ";" +
                        "-fx-border-radius:10; -fx-border-width:1;");
        btn.setOnAction(e -> fermer());
        return btn;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  UTILITAIRES
    // ════════════════════════════════════════════════════════════════════════

    private String couleurScore(double score) {
        if (score >= 80) return C_VERT;
        if (score >= 40) return C_ORANGE;
        return C_ROUGE;
    }

    private Label label(String text, String inlineStyle) {
        Label l = new Label(text);
        l.setStyle(normaliserStyle(inlineStyle));
        return l;
    }

    /** Convertit les raccourcis CSS en style JavaFX valide */
    private String normaliserStyle(String s) {
        return s.replace("font-family:", "-fx-font-family:")
                .replace("font-size:", "-fx-font-size:")
                .replace("font-weight:", "-fx-font-weight:")
                .replace("text-fill:", "-fx-text-fill:")
                .replace("letter-spacing:", "-fx-letter-spacing:");
    }

    private String bg(String hex) {
        return "-fx-background-color:" + hex + ";";
    }

    @FXML
    private void fermer() {
        if (rootVBox != null && rootVBox.getScene() != null)
            ((Stage) rootVBox.getScene().getWindow()).close();
    }
}