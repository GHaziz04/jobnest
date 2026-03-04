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
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.stage.Stage;
import tn.jobnest.gentretien.service.MatchingService.DetailCompetence;
import tn.jobnest.gentretien.service.MatchingService.DetailExperience;
import tn.jobnest.gentretien.service.MatchingService.MatchingResult;

/**
 * ════════════════════════════════════════════════════════════════════════════
 *  MatchingResultController — Design Premium JobNest
 *
 *  Charte graphique cohérente avec le projet :
 *    • Police     : 'Segoe UI' (identique au .root CSS du projet)
 *    • Bleu marine: #1E3A5F  (sidebar, titres, hero)
 *    • Bleu act.  : #2563EB  (accents, barres, scores compétences)
 *    • Orange     : #F97316  (CTA, highlights expériences, logo)
 *    • Fond dark  : #0D1B2E / #132039 / #1A2B45
 *    • Texte clair: #CBD5E1 / #8BAECF
 * ════════════════════════════════════════════════════════════════════════════
 */
public class MatchingResultController {

    @FXML private VBox rootVBox;

    // ─── Palette JobNest Dark ─────────────────────────────────────────────────
    // Fonds (déclinaisons sombres de la sidebar #1E3A5F)
    private static final String BG_DEEP    = "#0D1B2E";   // fond ultime
    private static final String BG_HERO    = "#132039";   // fond hero
    private static final String BG_BODY    = "#0F2034";   // fond corps
    private static final String BG_CARD    = "#1A2B45";   // carte
    private static final String BG_HDR_B   = "#1E3A5F";   // header section bleu (=sidebar)
    private static final String BG_HDR_O   = "#1F2D1A";   // header section orange (vert sombre)
    private static final String BG_ROW_ODD = "#172336";   // ligne impaire
    private static final String BG_ROW_EVN = "#1A2B45";   // ligne paire

    // Couleurs primaires du projet
    private static final String C_BLEU     = "#2563EB";   // bleu primaire
    private static final String C_BLEU_LT  = "#60A5FA";   // bleu clair (texte sur dark)
    private static final String C_MARINE   = "#1E3A5F";   // bleu marine
    private static final String C_ORANGE   = "#F97316";   // orange CTA
    private static final String C_ORANGE_LT= "#FED7AA";   // orange clair (texte)
    private static final String C_VERT     = "#10B981";   // vert succès
    private static final String C_ROUGE    = "#EF4444";   // rouge erreur

    // Textes
    private static final String TXT_MAIN   = "#E2E8F0";   // texte principal
    private static final String TXT_SUB    = "#8BAECF";   // texte secondaire (=sidebar inactif)
    private static final String TXT_MUTED  = "#4A6080";   // texte atténué

    // Bordures
    private static final String BDR_BLEU   = "#1D4ED8";
    private static final String BDR_ORANGE = "#92400E";
    private static final String BDR_CARD   = "#1E3A5F";

    // ════════════════════════════════════════════════════════════════════════
    //  POINT D'ENTRÉE
    // ════════════════════════════════════════════════════════════════════════

    public void afficherResultat(MatchingResult result, String nomCandidat) {
        if (rootVBox == null) return;
        rootVBox.getChildren().clear();
        rootVBox.setStyle(bg(BG_DEEP));

        rootVBox.getChildren().addAll(
                buildHero(result, nomCandidat),
                buildAccentBar(),
                buildBody(result)
        );
    }

    // ════════════════════════════════════════════════════════════════════════
    //  HERO
    // ════════════════════════════════════════════════════════════════════════

    private VBox buildHero(MatchingResult result, String nomCandidat) {
        VBox hero = new VBox(0);
        hero.setStyle(
                "-fx-background-color:linear-gradient(to bottom right," + BG_DEEP + "," + BG_HERO + ");" +
                        "-fx-padding:26 32 24 32;");

        // ── Top bar ──────────────────────────────────────────────────────────
        HBox topBar = new HBox();
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(0, 0, 20, 0));

        // Logo + titres
        VBox titreZone = new VBox(4);
        HBox.setHgrow(titreZone, Priority.ALWAYS);

        // Badge "JobNest" style projet
        HBox logoRow = new HBox(8);
        logoRow.setAlignment(Pos.CENTER_LEFT);
        StackPane logoIcon = new StackPane();
        logoIcon.setPrefSize(22, 22);
        logoIcon.setStyle(
                "-fx-background-color:linear-gradient(to bottom right,#F97316,#EA6B00);" +
                        "-fx-background-radius:6;");
        Label logoTxt = new Label("JN");
        logoTxt.setStyle("-fx-font-family:'Segoe UI'; -fx-font-size:10px; " +
                "-fx-font-weight:bold; -fx-text-fill:white;");
        logoIcon.getChildren().add(logoTxt);

        Label eyebrow = new Label("RAPPORT DE MATCHING");
        eyebrow.setStyle("-fx-font-family:'Segoe UI'; -fx-font-size:10px; " +
                "-fx-font-weight:bold; -fx-text-fill:" + C_BLEU_LT + "; -fx-letter-spacing:2;");
        logoRow.getChildren().addAll(logoIcon, eyebrow);

        Label titre = new Label("Analyse de Candidature");
        titre.setStyle("-fx-font-family:'Segoe UI'; -fx-font-size:22px; " +
                "-fx-font-weight:bold; -fx-text-fill:" + TXT_MAIN + ";");
        titreZone.getChildren().addAll(logoRow, titre);

        // Bouton fermer (style cohérent menu-item hover)
        Button btnClose = new Button("✕");
        btnClose.setStyle(
                "-fx-background-color:rgba(255,255,255,0.07);" +
                        "-fx-text-fill:" + TXT_SUB + "; -fx-font-size:15px;" +
                        "-fx-background-radius:50; -fx-cursor:hand;" +
                        "-fx-min-width:36; -fx-min-height:36;" +
                        "-fx-border-color:rgba(255,255,255,0.12);" +
                        "-fx-border-radius:50; -fx-border-width:1;");
        btnClose.setOnAction(e -> fermer());
        topBar.getChildren().addAll(titreZone, btnClose);

        // ── Gauge + métriques ─────────────────────────────────────────────────
        HBox gaugeRow = new HBox(28);
        gaugeRow.setAlignment(Pos.CENTER_LEFT);
        gaugeRow.getChildren().addAll(
                buildGauge(result.scoreFinal),
                buildMetrics(result, nomCandidat)
        );

        hero.getChildren().addAll(topBar, gaugeRow);
        return hero;
    }

    // ── Gauge circulaire en Canvas ────────────────────────────────────────────

    private StackPane buildGauge(double score) {
        StackPane pane = new StackPane();
        pane.setPrefSize(158, 158);
        pane.setMinSize(158, 158);
        pane.setMaxSize(158, 158);
        pane.setStyle(
                "-fx-background-color:rgba(37,99,235,0.07);" +
                        "-fx-background-radius:79;" +
                        "-fx-border-color:" + BDR_BLEU + ";" +
                        "-fx-border-radius:79; -fx-border-width:1;");

        Canvas cv = new Canvas(158, 158);
        drawArc(cv, score);

        VBox center = new VBox(3);
        center.setAlignment(Pos.CENTER);

        String couleur = scoreColor(score);
        Label lblPct = new Label(String.format("%.0f%%", score));
        lblPct.setStyle("-fx-font-family:'Segoe UI'; -fx-font-size:38px; " +
                "-fx-font-weight:bold; -fx-text-fill:" + couleur + ";");

        Label lblSub = new Label("SCORE FINAL");
        lblSub.setStyle("-fx-font-family:'Segoe UI'; -fx-font-size:8px; " +
                "-fx-font-weight:bold; -fx-text-fill:" + C_BLEU_LT + "; -fx-letter-spacing:2;");
        center.getChildren().addAll(lblPct, lblSub);

        pane.getChildren().addAll(cv, center);
        return pane;
    }

    private void drawArc(Canvas cv, double score) {
        GraphicsContext gc = cv.getGraphicsContext2D();
        double cx = 79, cy = 79, r = 62, ep = 8;
        gc.clearRect(0, 0, 158, 158);

        // Anneau fond (couleur sidebar atténuée)
        gc.setStroke(Color.web("#1E3A5F", 0.8));
        gc.setLineWidth(ep);
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.strokeArc(cx - r, cy - r, r * 2, r * 2, -210, 240, ArcType.OPEN);

        if (score <= 0) return;

        // Arc coloré
        double sweep = (score / 100.0) * 240;
        Color c = score >= 80 ? Color.web(C_VERT)
                : score >= 40 ? Color.web(C_ORANGE)
                : Color.web(C_ROUGE);
        gc.setStroke(c);
        gc.setLineWidth(ep);
        gc.strokeArc(cx - r, cy - r, r * 2, r * 2, -210, sweep, ArcType.OPEN);

        // Point terminal avec halo
        if (score > 3) {
            double rad = Math.toRadians(-(-210 + sweep));
            double px  = cx + r * Math.cos(rad);
            double py  = cy + r * Math.sin(rad);
            gc.setFill(Color.web(score >= 80 ? C_VERT
                    : score >= 40 ? C_ORANGE : C_ROUGE, 0.28));
            gc.fillOval(px - 9, py - 9, 18, 18);
            gc.setFill(c);
            gc.fillOval(px - 5, py - 5, 10, 10);
        }
    }

    // ── Métriques (droite de la gauge) ───────────────────────────────────────

    private VBox buildMetrics(MatchingResult result, String nomCandidat) {
        VBox box = new VBox(14);
        HBox.setHgrow(box, Priority.ALWAYS);

        // Badge décision
        box.getChildren().add(buildDecisionBadge(result));

        // Nom candidat (style avatar-text du projet)
        HBox candRow = new HBox(10);
        candRow.setAlignment(Pos.CENTER_LEFT);

        // Mini avatar initiales
        StackPane avatarMini = new StackPane();
        avatarMini.setPrefSize(34, 34);
        avatarMini.setMinSize(34, 34);
        avatarMini.setStyle(
                "-fx-background-color:" + C_BLEU + ";" +
                        "-fx-background-radius:17;" +
                        "-fx-effect:dropshadow(gaussian,rgba(37,99,235,0.4),8,0,0,2);");
        String initials = buildInitials(nomCandidat);
        Label ini = new Label(initials);
        ini.setStyle("-fx-font-family:'Segoe UI'; -fx-font-size:12px; " +
                "-fx-font-weight:bold; -fx-text-fill:white;");
        avatarMini.getChildren().add(ini);

        Label lblCand = new Label(nomCandidat != null ? nomCandidat : "—");
        lblCand.setStyle("-fx-font-family:'Segoe UI'; -fx-font-size:14px; " +
                "-fx-font-weight:600; -fx-text-fill:" + TXT_MAIN + ";");
        candRow.getChildren().addAll(avatarMini, lblCand);
        box.getChildren().add(candRow);

        // Barres scores
        box.getChildren().addAll(
                buildScoreBar("Compétences", result.scoreCompetences, "60%", C_BLEU, C_BLEU_LT),
                buildScoreBar("Expériences",  result.scoreExperiences,  "40%", C_ORANGE, C_ORANGE_LT)
        );
        return box;
    }

    private Label buildDecisionBadge(MatchingResult result) {
        String txt, bg, fg, border;
        switch (result.decision) {
            case "ACCEPTÉ":
                txt    = "✓  Candidature ACCEPTÉE automatiquement";
                bg     = "rgba(16,185,129,0.13)"; fg = C_VERT; border = C_VERT; break;
            case "EN_REVISION":
                txt    = "⟳  Mise EN RÉVISION — décision manuelle requise";
                bg     = "rgba(249,115,22,0.13)"; fg = C_ORANGE; border = C_ORANGE; break;
            default:
                txt    = "✗  Candidature REFUSÉE automatiquement";
                bg     = "rgba(239,68,68,0.13)"; fg = C_ROUGE; border = C_ROUGE;
        }
        Label l = new Label(txt);
        l.setMaxWidth(Double.MAX_VALUE);
        l.setStyle(
                "-fx-font-family:'Segoe UI'; -fx-font-size:13px; -fx-font-weight:bold;" +
                        "-fx-padding:10 18; -fx-background-radius:10;" +
                        "-fx-text-fill:" + fg + ";" +
                        "-fx-background-color:" + bg + ";" +
                        "-fx-border-color:" + border + ";" +
                        "-fx-border-width:1; -fx-border-radius:10;");
        return l;
    }

    private VBox buildScoreBar(String label, double score, String poids,
                               String couleur, String couleurLight) {
        VBox box = new VBox(5);

        HBox top = new HBox();
        top.setAlignment(Pos.CENTER_LEFT);
        Label lbl = lbl(label, "'Segoe UI'; 11px; 700; " + TXT_SUB);
        HBox.setHgrow(lbl, Priority.ALWAYS);
        Label val = lbl(String.format("%.1f%%", score),
                "'Segoe UI'; 12px; 800; " + couleurLight);
        Label wgt = lbl("  · " + poids, "'Segoe UI'; 10px; 400; " + TXT_MUTED);
        top.getChildren().addAll(lbl, val, wgt);

        StackPane track = new StackPane();
        track.setPrefHeight(8);
        Region fond = new Region();
        fond.setPrefHeight(8);
        fond.setMaxWidth(Double.MAX_VALUE);
        fond.setStyle("-fx-background-color:rgba(37,99,235,0.15); -fx-background-radius:4;");
        Region fill = new Region();
        fill.setPrefHeight(8);
        fill.setPrefWidth(Math.min(score / 100.0 * 420, 420));
        fill.setStyle("-fx-background-color:" + couleur + "; -fx-background-radius:4;" +
                "-fx-effect:dropshadow(gaussian," + couleur + ",6,0,0,1);");
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);
        track.getChildren().addAll(fond, fill);

        box.getChildren().addAll(top, track);
        return box;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  BARRE ACCENT (identique au gradient accent du header projet)
    // ════════════════════════════════════════════════════════════════════════

    private Region buildAccentBar() {
        Region r = new Region();
        r.setPrefHeight(3);
        r.setMaxWidth(Double.MAX_VALUE);
        // Reprend les couleurs du projet : bleu → orange (logo accent)
        r.setStyle("-fx-background-color:linear-gradient(to right," +
                C_BLEU + ",#1D4ED8," + C_ORANGE + ");");
        return r;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CORPS SCROLLABLE
    // ════════════════════════════════════════════════════════════════════════

    private ScrollPane buildBody(MatchingResult result) {
        VBox body = new VBox(18);
        body.setPadding(new Insets(22, 28, 22, 28));
        body.setStyle(bg(BG_BODY));

        body.getChildren().addAll(
                buildSection(result, true),   // Compétences
                buildSection(result, false),  // Expériences
                buildCloseBtn()
        );

        ScrollPane sp = new ScrollPane(body);
        sp.setFitToWidth(true);
        sp.setPrefHeight(520);
        sp.setStyle(
                "-fx-background:" + BG_BODY + ";" +
                        "-fx-background-color:" + BG_BODY + ";" +
                        "-fx-border-color:transparent;");
        VBox.setVgrow(sp, Priority.ALWAYS);
        return sp;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  SECTION CARTE (compétences ou expériences)
    // ════════════════════════════════════════════════════════════════════════

    private VBox buildSection(MatchingResult result, boolean isComp) {
        int nb = isComp
                ? (result.detailsCompetences != null ? result.detailsCompetences.size() : 0)
                : (result.detailsExperiences != null ? result.detailsExperiences.size()  : 0);

        String icone   = isComp ? "◈" : "◈";
        String titre   = isComp ? "COMPÉTENCES TECHNIQUES" : "EXPÉRIENCES PROFESSIONNELLES";
        String compte  = nb + " critère" + (nb > 1 ? "s" : "") + " analysé" + (nb > 1 ? "s" : "");
        String couleur = isComp ? C_BLEU_LT : C_ORANGE_LT;
        String bgHdr   = isComp ? BG_HDR_B  : BG_HDR_O;
        String border  = isComp ? BDR_BLEU  : BDR_ORANGE;

        // Enveloppe card (style .card du projet)
        VBox card = new VBox(0);
        card.setStyle(
                "-fx-background-color:" + BG_CARD + ";" +
                        "-fx-background-radius:14;" +
                        "-fx-border-color:" + border + ";" +
                        "-fx-border-width:1; -fx-border-radius:14;" +
                        "-fx-effect:dropshadow(gaussian,rgba(15,30,55,0.55),18,0,5,0);");

        // En-tête section
        HBox hdr = new HBox(10);
        hdr.setAlignment(Pos.CENTER_LEFT);
        hdr.setPadding(new Insets(12, 20, 12, 20));
        hdr.setStyle(
                "-fx-background-color:" + bgHdr + ";" +
                        "-fx-background-radius:14 14 0 0;" +
                        "-fx-border-color:" + border + ";" +
                        "-fx-border-width:0 0 1 0;");

        Label ico = new Label(icone);
        ico.setStyle("-fx-font-size:15px; -fx-text-fill:" + couleur + ";");
        Label lTitre = new Label(titre);
        lTitre.setStyle("-fx-font-family:'Segoe UI'; -fx-font-size:11px; " +
                "-fx-font-weight:bold; -fx-text-fill:" + couleur + "; -fx-letter-spacing:2;");
        HBox.setHgrow(lTitre, Priority.ALWAYS);
        Label lCompte = new Label(compte);
        lCompte.setStyle("-fx-font-family:'Segoe UI'; -fx-font-size:10px; -fx-text-fill:" + TXT_MUTED + ";");

        hdr.getChildren().addAll(ico, lTitre, lCompte);
        card.getChildren().add(hdr);

        // Tableau
        VBox tableau = new VBox(0);
        if (isComp) {
            if (result.detailsCompetences == null || result.detailsCompetences.isEmpty()) {
                tableau.getChildren().add(emptyRow("Aucune compétence définie pour cette offre"));
            } else {
                for (int i = 0; i < result.detailsCompetences.size(); i++) {
                    tableau.getChildren().add(buildCompRow(result.detailsCompetences.get(i), i));
                }
            }
        } else {
            if (result.detailsExperiences == null || result.detailsExperiences.isEmpty()) {
                tableau.getChildren().add(emptyRow("Aucune expérience définie pour cette offre"));
            } else {
                for (int i = 0; i < result.detailsExperiences.size(); i++) {
                    tableau.getChildren().add(buildExpRow(result.detailsExperiences.get(i), i));
                }
            }
        }
        card.getChildren().add(tableau);
        return card;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  LIGNE COMPÉTENCE
    // ════════════════════════════════════════════════════════════════════════

    private HBox buildCompRow(DetailCompetence d, int idx) {
        String bg     = idx % 2 == 0 ? BG_ROW_ODD : BG_ROW_EVN;
        String couleur = scoreColor(d.scoreObtenu);
        boolean found = !"—".equals(d.nomCandidat);

        HBox row = new HBox(0);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPrefHeight(54);
        row.setStyle(bg(bg));

        // Bande colorée gauche
        Region band = band(couleur);

        // Numéro
        Label num = num(idx + 1, bg);

        // Cellule requise — style bleu (comme .competence-tag du projet)
        VBox reqCell = cell(
                d.nomRequis,
                d.niveauRequis != null ? d.niveauRequis.toUpperCase() : "N/A",
                C_BLEU_LT, bg, 190);

        Label arrow = arrow(bg);

        // Cellule candidat
        VBox candCell = cell(
                found ? d.nomCandidat : "Non trouvé",
                found ? (d.niveauCandidat != null ? d.niveauCandidat.toUpperCase() : "N/A") : "—",
                found ? C_VERT : C_ROUGE, bg, 190);

        Region spacer = spacer(bg);

        VBox scoreZone = scoreZone(d.scoreObtenu, couleur, bg);

        row.getChildren().addAll(band, num, reqCell, arrow, candCell, spacer, scoreZone);
        return row;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  LIGNE EXPÉRIENCE
    // ════════════════════════════════════════════════════════════════════════

    private HBox buildExpRow(DetailExperience d, int idx) {
        String bg     = idx % 2 == 0 ? BG_ROW_ODD : BG_ROW_EVN;
        String couleur = scoreColor(d.scoreObtenu);
        boolean found = !"—".equals(d.posteCandidat);

        HBox row = new HBox(0);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPrefHeight(54);
        row.setStyle(bg(bg));

        Region band = band(couleur);
        Label  num  = num(idx + 1, bg);

        // Cellule requise — style orange (comme .button-map du projet)
        String reqAns = d.anneesRequises > 0
                ? String.format("%.0f an%s requis", d.anneesRequises, d.anneesRequises >= 2 ? "s" : "")
                : "Durée libre";
        VBox reqCell = cell(d.nomRequis, reqAns, C_ORANGE_LT, bg, 190);

        Label arrow = arrow(bg);

        // Cellule candidat
        String candAns = found
                ? String.format("%.1f an%s", d.anneesCandidat, d.anneesCandidat >= 2 ? "s" : "")
                : "—";
        if (found && d.anneesRequises > 0 && d.anneesCandidat < d.anneesRequises) {
            double manque = d.anneesRequises - d.anneesCandidat;
            candAns += String.format("  ⚠ -%.0f an%s", manque, manque >= 2 ? "s" : "");
        }
        VBox candCell = cell(
                found ? d.posteCandidat : "Non trouvé",
                candAns,
                found ? C_VERT : C_ROUGE, bg, 190);

        Region spacer = spacer(bg);
        VBox   score  = scoreZone(d.scoreObtenu, couleur, bg);

        row.getChildren().addAll(band, num, reqCell, arrow, candCell, spacer, score);
        return row;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  COMPOSANTS RÉUTILISABLES
    // ════════════════════════════════════════════════════════════════════════

    /** Bande colorée latérale (indicateur de score) */
    private Region band(String couleur) {
        Region r = new Region();
        r.setPrefWidth(4); r.setPrefHeight(54);
        r.setStyle("-fx-background-color:" + couleur + ";");
        return r;
    }

    /** Numéro de ligne 01, 02 … (style Segoe UI monospace-like) */
    private Label num(int n, String bg) {
        Label l = new Label(String.format("%02d", n));
        l.setPrefWidth(38); l.setMinWidth(38);
        l.setAlignment(Pos.CENTER);
        l.setStyle("-fx-font-family:'Segoe UI'; -fx-font-size:11px; -fx-font-weight:bold; " +
                "-fx-text-fill:" + TXT_MUTED + "; -fx-background-color:" + bg + "; -fx-padding:0 8;");
        return l;
    }

    /** Cellule : titre en blanc + sous-titre coloré */
    private VBox cell(String titre, String sous, String couleurSous, String bg, double w) {
        VBox box = new VBox(2);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPrefWidth(w); box.setPrefHeight(54);
        box.setPadding(new Insets(6, 10, 6, 10));
        box.setStyle(bg(bg));

        Label t = new Label(titre);
        t.setStyle("-fx-font-family:'Segoe UI'; -fx-font-size:12px; " +
                "-fx-font-weight:700; -fx-text-fill:" + TXT_MAIN + ";");
        t.setWrapText(false);

        Label s = new Label(sous);
        s.setStyle("-fx-font-family:'Segoe UI'; -fx-font-size:10px; " +
                "-fx-font-weight:600; -fx-text-fill:" + couleurSous + ";");
        box.getChildren().addAll(t, s);
        return box;
    }

    /** Flèche → */
    private Label arrow(String bg) {
        Label l = new Label("→");
        l.setPrefWidth(30); l.setMinWidth(30);
        l.setAlignment(Pos.CENTER);
        l.setStyle("-fx-font-size:13px; -fx-text-fill:" + TXT_MUTED + "; " +
                "-fx-background-color:" + bg + ";");
        return l;
    }

    /** Spacer extensible */
    private Region spacer(String bg) {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        r.setStyle(bg(bg));
        return r;
    }

    /** Zone score : % en grand + mini barre */
    private VBox scoreZone(double score, String couleur, String bg) {
        VBox zone = new VBox(4);
        zone.setAlignment(Pos.CENTER_RIGHT);
        zone.setPrefWidth(110); zone.setPrefHeight(54);
        zone.setPadding(new Insets(8, 16, 8, 0));
        zone.setStyle(bg(bg));

        Label pct = new Label(String.format("%.0f%%", score));
        pct.setStyle("-fx-font-family:'Segoe UI'; -fx-font-size:16px; " +
                "-fx-font-weight:bold; -fx-text-fill:" + couleur + ";");

        // Mini-barre inline
        StackPane bar = new StackPane();
        bar.setPrefHeight(4); bar.setPrefWidth(82);
        Region fond = new Region();
        fond.setPrefHeight(4); fond.setMaxWidth(Double.MAX_VALUE);
        fond.setStyle("-fx-background-color:rgba(37,99,235,0.15); -fx-background-radius:2;");
        Region fill = new Region();
        fill.setPrefHeight(4);
        fill.setPrefWidth(Math.min(score / 100.0 * 82, 82));
        fill.setStyle("-fx-background-color:" + couleur + "; -fx-background-radius:2;");
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);
        bar.getChildren().addAll(fond, fill);

        zone.getChildren().addAll(pct, bar);
        return zone;
    }

    /** Ligne message vide */
    private HBox emptyRow(String msg) {
        HBox box = new HBox();
        box.setPadding(new Insets(16, 16, 16, 16));
        Label l = new Label(msg);
        l.setStyle("-fx-font-family:'Segoe UI'; -fx-font-size:12px; " +
                "-fx-text-fill:" + TXT_MUTED + "; -fx-font-style:italic;");
        box.getChildren().add(l);
        return box;
    }

    /** Bouton fermer — style .button-primary du projet */
    private Button buildCloseBtn() {
        Button btn = new Button("Fermer le rapport");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(44);
        btn.setStyle(
                "-fx-background-color:" + C_BLEU + ";" +
                        "-fx-text-fill:white;" +
                        "-fx-font-family:'Segoe UI'; -fx-font-size:13px; -fx-font-weight:bold;" +
                        "-fx-background-radius:10; -fx-cursor:hand;" +
                        "-fx-effect:dropshadow(gaussian,rgba(37,99,235,0.45),10,0,0,3);");
        btn.setOnAction(e -> fermer());
        return btn;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  UTILITAIRES
    // ════════════════════════════════════════════════════════════════════════

    /** Couleur selon seuils : vert ≥80%, orange 40–79%, rouge <40% */
    private String scoreColor(double s) {
        if (s >= 80) return C_VERT;
        if (s >= 40) return C_ORANGE;
        return C_ROUGE;
    }

    private String bg(String hex) {
        return "-fx-background-color:" + hex + ";";
    }

    /**
     * Crée un Label avec style compact.
     * Format attendu : "'Police'; taillePx; poids; couleur"
     */
    private Label lbl(String text, String style) {
        Label l = new Label(text);
        String[] p = style.split(";\\s*");
        StringBuilder sb = new StringBuilder();
        if (p.length > 0) sb.append("-fx-font-family:").append(p[0].trim()).append(";");
        if (p.length > 1) sb.append("-fx-font-size:").append(p[1].trim()).append(";");
        if (p.length > 2) sb.append("-fx-font-weight:").append(p[2].trim()).append(";");
        if (p.length > 3) sb.append("-fx-text-fill:").append(p[3].trim()).append(";");
        l.setStyle(sb.toString());
        return l;
    }

    /** Génère les initiales d'un nom (ex: "Ahmed Ben Salah" → "AB") */
    private String buildInitials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        sb.append(Character.toUpperCase(parts[0].charAt(0)));
        if (parts.length > 1)
            sb.append(Character.toUpperCase(parts[parts.length - 1].charAt(0)));
        return sb.toString();
    }

    @FXML
    private void fermer() {
        if (rootVBox != null && rootVBox.getScene() != null)
            ((Stage) rootVBox.getScene().getWindow()).close();
    }
}