package projet_final;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Station de Contrôle Médicale — Interface Graphique.
 *
 * Tous les bugs identifiés sont corrigés dans les classes métier.
 * La GUI est mise à jour pour refléter les corrections :
 *
 * Bug #3 côté GUI : le bouton "Livrer" appelle maintenant robot.livrer()
 *   (nouvelle méthode publique) qui enchaîne deplacer() + fin de mission.
 *   Avant : deplacer() seul → robot bloqué sur "En livraison" indéfiniment.
 *
 * Bug #6 côté GUI : effectuerTache() ne lit plus System.in.
 *   Toute l'interaction utilisateur est gérée ici dans le GUI.
 *
 * Bug #8 côté GUI : les champs d'affichage du colis montrent
 *   le nom String du colis (ex: "Pharmacie") au lieu de 0/1.
 */
public class StationControle extends JFrame {

    // ── Palette ───────────────────────────────────────────────────────────
    static final Color BG_DARK  = new Color(10, 18, 38);
    static final Color BG_PANEL = new Color(18, 32, 60);
    static final Color BG_CARD  = new Color(25, 42, 78);
    static final Color ACCENT   = new Color(0, 175, 215);
    static final Color C_GREEN  = new Color(0, 205, 115);
    static final Color C_RED    = new Color(220, 55, 55);
    static final Color C_ORANGE = new Color(255, 165, 0);
    static final Color C_YELLOW = new Color(255, 220, 50);
    static final Color TEXT_PRI = new Color(215, 232, 255);
    static final Color TEXT_SEC = new Color(110, 148, 190);
    static final Color BORDER_C = new Color(35, 62, 112);

    // ── Lieux hospitaliers (HashMap lieu -> coordonnees) ──────────────────
    private static final LinkedHashMap<String, int[]> LIEUX = new LinkedHashMap<>();
    static {
        LIEUX.put("Base (0,0)",        new int[]{0, 0});
        LIEUX.put("Pharmacie",         new int[]{3, 7});
        LIEUX.put("Chambre 101",       new int[]{1, 2});
        LIEUX.put("Chambre 202",       new int[]{2, 4});
        LIEUX.put("Chambre 303",       new int[]{4, 6});
        LIEUX.put("Bloc Operatoire",   new int[]{8, 8});
        LIEUX.put("Urgences",          new int[]{9, 1});
        LIEUX.put("Salle Reeducation", new int[]{5, 5});
        LIEUX.put("Radiologie",        new int[]{7, 3});
        LIEUX.put("Laboratoire",       new int[]{6, 9});
        LIEUX.put("Accueil Principal", new int[]{0, 9});
        LIEUX.put("Sterilisation",     new int[]{4, 1});
    }
    private static final String[] LIEUX_KEYS =
            LIEUX.keySet().toArray(new String[0]);

    // ── Data ──────────────────────────────────────────────────────────────
    private final GestionnaireHospitalier         gestionnaire    = new GestionnaireHospitalier();
    private final List<RobotAssistantReeducation>  robotsReeduc   = new ArrayList<>();
    private final List<RobotLivraison>             robotsLivraison = new ArrayList<>();

    // ── UI ────────────────────────────────────────────────────────────────
    private GrillePanel    grillePanel;
    private JPanel         dashPanel;
    private JTextPane      consolePane;
    private StyledDocument consoleDoc;
    private JLabel         clockLabel;
    private JLabel         fleetStatusLabel;
    private Timer          refreshTimer;

    // Reeduc form
    private JComboBox<String> reedCombo;
    private JPasswordField    codeField;
    private JTextField        patientField;
    private JComboBox<String> exerciceCombo;
    private JComboBox<String> urgenceCombo;
    private JTextField        repsField;
    private JComboBox<String> lieuReedCombo;

    // Livraison form
    private JComboBox<String> livCombo;
    private JComboBox<String> lieuLivCombo;

    // ── Entry point ───────────────────────────────────────────────────────
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new StationControle().setVisible(true));
    }

    // ── Constructor ───────────────────────────────────────────────────────
    public StationControle() {
        super("Station de Controle Medicale - Hopital INSAT");
        initData();
        buildUI();
        startRefreshTimer();
        log("Station de controle medicale initialisee", C_GREEN);
        log("Flotte : " + (robotsReeduc.size() + robotsLivraison.size())
                + " robots  |  " + LIEUX.size() + " lieux hospitaliers", ACCENT);
        log("------------------------------------------------------", TEXT_SEC);
    }

    // ── Data init ─────────────────────────────────────────────────────────
    private void initData() {
        try {
            RobotAssistantReeducation r1 = new RobotAssistantReeducation("REED-01", 5, 5);
            r1.demarrer(); r1.connecter("HospNet-5G");
            robotsReeduc.add(r1); gestionnaire.ajouterRobot(r1);

            RobotAssistantReeducation r2 = new RobotAssistantReeducation("REED-02", 1, 2);
            r2.demarrer();
            robotsReeduc.add(r2); gestionnaire.ajouterRobot(r2);

            RobotLivraison rl1 = new RobotLivraison("LIV-01", 3, 7);
            rl1.demarrer(); rl1.connecter("LogiNet-5G");
            robotsLivraison.add(rl1); gestionnaire.ajouterRobot(rl1);

            RobotLivraison rl2 = new RobotLivraison("LIV-02", 0, 9);
            rl2.demarrer();
            robotsLivraison.add(rl2); gestionnaire.ajouterRobot(rl2);
        } catch (RobotException e) {
            System.err.println("Init error: " + e.getMessage());
        }
    }

    // ── Build UI ──────────────────────────────────────────────────────────
    private void buildUI() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1500, 960);
        setMinimumSize(new Dimension(1200, 800));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout());
        add(buildHeader(),  BorderLayout.NORTH);
        add(buildCenter(),  BorderLayout.CENTER);
        add(buildConsole(), BorderLayout.SOUTH);
    }

    // ── Header ────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(BG_PANEL);
        h.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 0, ACCENT),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        JLabel ico = lbl2("H", Font.BOLD, 26, ACCENT);
        JLabel ti  = lbl2("Station de Controle Medicale", Font.BOLD, 21, TEXT_PRI);
        JLabel su  = lbl2("   Hopital INSAT  |  Gestion de Robots Medicaux", Font.PLAIN, 12, TEXT_SEC);
        left.add(ico); left.add(ti); left.add(su);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        right.setOpaque(false);
        JButton addBtn = btn("+ Ajouter un Robot", new Color(0, 110, 80));
        addBtn.addActionListener(e -> doAjouterRobot());
        fleetStatusLabel = lbl2("SYSTEME OPERATIONNEL", Font.BOLD, 11, C_GREEN);
        clockLabel = new JLabel(); clockLabel.setFont(new Font("Courier New", Font.BOLD, 15));
        clockLabel.setForeground(ACCENT); updateClock();
        right.add(addBtn); right.add(fleetStatusLabel); right.add(clockLabel);

        h.add(left, BorderLayout.WEST); h.add(right, BorderLayout.EAST);
        return h;
    }

    // ── Center ────────────────────────────────────────────────────────────
    private JPanel buildCenter() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(BG_DARK);
        p.setBorder(BorderFactory.createEmptyBorder(6, 8, 4, 8));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH; gc.weighty = 1.0;
        gc.weightx = 0.25; gc.gridx = 0; gc.insets = new Insets(0,0,0,6); p.add(buildMapPanel(), gc);
        gc.weightx = 0.35; gc.gridx = 1;                                   p.add(buildDashPanel(), gc);
        gc.weightx = 0.40; gc.gridx = 2; gc.insets = new Insets(0,0,0,0); p.add(buildCommandPanel(), gc);
        return p;
    }

    // ── Map ───────────────────────────────────────────────────────────────
    private JPanel buildMapPanel() {
        JPanel card = makeCard("Carte de Positionnement (10x10)");
        grillePanel = new GrillePanel();
        card.add(grillePanel, BorderLayout.CENTER);
        JPanel leg = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 3)); leg.setOpaque(false);
        leg.add(chip("Reeducation", C_GREEN));
        leg.add(chip("Reeducation URGENT", C_RED));
        leg.add(chip("Livraison libre", ACCENT));
        leg.add(chip("Livraison active", C_ORANGE));
        card.add(leg, BorderLayout.SOUTH);
        return card;
    }

    // ── Dashboard ─────────────────────────────────────────────────────────
    private JPanel buildDashPanel() {
        JPanel card = makeCard("Tableau de Bord - Etat de la Flotte");
        dashPanel = new JPanel();
        dashPanel.setLayout(new BoxLayout(dashPanel, BoxLayout.Y_AXIS));
        dashPanel.setBackground(BG_PANEL);
        JScrollPane sc = new JScrollPane(dashPanel); sc.setBorder(null);
        sc.setBackground(BG_PANEL); sc.getViewport().setBackground(BG_PANEL);
        sc.getVerticalScrollBar().setUnitIncrement(16);
        card.add(sc, BorderLayout.CENTER); refreshDashboard(); return card;
    }

    // ── Commands ──────────────────────────────────────────────────────────
    private JPanel buildCommandPanel() {
        JPanel w = new JPanel(new GridLayout(2, 1, 0, 6)); w.setBackground(BG_DARK);
        w.add(buildReeducCmds()); w.add(buildLivCmds()); return w;
    }

    private JPanel buildReeducCmds() {
        JPanel card = makeCard("Centre de Commande - Reeducation");
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(BG_PANEL); form.setBorder(BorderFactory.createEmptyBorder(6,10,6,10));
        GridBagConstraints g = gbc();

        reedCombo = combo(robotsReeduc.stream().map(RobotAssistantReeducation::getId).toArray(String[]::new));
        row(form, g, 0, "Robot :", reedCombo);
        codeField = new JPasswordField("REEDUC2025"); styleField(codeField);
        row(form, g, 1, "Code securite :", codeField);
        patientField = field("PAT-001");
        row(form, g, 2, "ID Patient :", patientField);
        exerciceCombo = combo("MARCHE", "BRAS", "EQUILIBRE", "PREHENSION");
        row(form, g, 3, "Exercice :", exerciceCombo);
        urgenceCombo = combo("ROUTINE", "SURVEILLANCE", "CRITIQUE", "URGENCE_MEDICALE");
        row(form, g, 4, "Urgence :", urgenceCombo);
        repsField = field("10");
        row(form, g, 5, "Repetitions :", repsField);

        g.gridy=6; g.gridwidth=1; g.weightx=0.33;
        g.gridx=0; JButton bD=btn("Dossier",   ACCENT);  form.add(bD,g);
        g.gridx=1; JButton bE=btn("Exercice",  C_GREEN); form.add(bE,g);
        g.gridx=2; JButton bC=btn("Sim.Chute", C_RED);   form.add(bC,g);
        g.gridy=7; g.gridx=0; g.gridwidth=3; g.weightx=1.0;
        JButton bR=btn("Reinitialiser apres urgence", new Color(80,50,0)); form.add(bR,g);
        g.gridy=8; g.gridwidth=1; g.weightx=0.30;
        g.gridx=0; form.add(lbl("Deplacer vers :"),g);
        g.gridx=1; g.weightx=0.50; lieuReedCombo=combo(LIEUX_KEYS); form.add(lieuReedCombo,g);
        g.gridx=2; g.weightx=0.20; JButton bMv=btn(">>", new Color(0,100,180)); form.add(bMv,g);
        g.gridy=9; g.gridx=0; g.gridwidth=3; g.weightx=1.0;
        JButton bA=btn("ALERTE GENERALE - Toute la Flotte", new Color(140,15,15));
        bA.setFont(new Font("Segoe UI",Font.BOLD,13)); form.add(bA,g);

        bD.addActionListener(e->doAccesDossier());
        bE.addActionListener(e->doExercice());
        bC.addActionListener(e->doChute());
        bR.addActionListener(e->doReinitialiser());
        bMv.addActionListener(e->doDeplacerReeduc());
        bA.addActionListener(e->doAlerteGenerale());
        card.add(form, BorderLayout.CENTER); return card;
    }

    private JPanel buildLivCmds() {
        JPanel card = makeCard("Centre de Commande - Livraison");
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(BG_PANEL); form.setBorder(BorderFactory.createEmptyBorder(6,10,6,10));
        GridBagConstraints g = gbc();

        livCombo = combo(robotsLivraison.stream().map(RobotLivraison::getId).toArray(String[]::new));
        row(form, g, 0, "Robot :", livCombo);
        lieuLivCombo = combo(LIEUX_KEYS); lieuLivCombo.setSelectedItem("Pharmacie");
        row(form, g, 1, "Destination :", lieuLivCombo);

        g.gridy=2; g.gridwidth=1; g.weightx=0.33;
        g.gridx=0; JButton bCh=btn("Charger colis", ACCENT);   form.add(bCh,g);
        // Bug #3 : "Livrer" appelle livrer() (deplacer + fin de mission)
        // au lieu de deplacer() seul — le robot ne reste plus bloqué
        g.gridx=1; JButton bLv=btn("Livrer",          C_GREEN); form.add(bLv,g);
        g.gridx=2; JButton bNt=btn("Reseau",           C_ORANGE);form.add(bNt,g);
        g.gridy=3; g.gridx=0; g.gridwidth=3; g.weightx=1.0;
        JButton bRe=btn("Recharger toute la flotte (+30%)", new Color(0,90,140)); form.add(bRe,g);

        bCh.addActionListener(e->doChargerColis());
        bLv.addActionListener(e->doLivrer());        // Bug #3 : appel à doLivrer()
        bNt.addActionListener(e->doConnexion());
        bRe.addActionListener(e->doRechargerTous());
        card.add(form, BorderLayout.CENTER); return card;
    }

    // ── Console ───────────────────────────────────────────────────────────
    private JPanel buildConsole() {
        JPanel p = new JPanel(new BorderLayout(0,4));
        p.setBackground(BG_PANEL);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(2,0,0,0,ACCENT),
            BorderFactory.createEmptyBorder(6,12,6,12)));
        p.setPreferredSize(new Dimension(0,162));

        JPanel top = new JPanel(new BorderLayout()); top.setOpaque(false);
        JLabel title = lbl2("Console de Monitoring - Historique en Temps Reel", Font.BOLD, 12, ACCENT);
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT,6,0)); btns.setOpaque(false);

        JButton exportBtn = new JButton("Exporter Rapport");
        exportBtn.setFont(new Font("Segoe UI",Font.BOLD,11)); exportBtn.setForeground(TEXT_PRI);
        exportBtn.setBackground(new Color(0,80,110)); exportBtn.setBorderPainted(false);
        exportBtn.setFocusPainted(false); exportBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        exportBtn.addActionListener(e->doExporterRapport());

        JButton clrBtn = new JButton("Effacer");
        clrBtn.setFont(new Font("Segoe UI",Font.PLAIN,11)); clrBtn.setForeground(TEXT_SEC);
        clrBtn.setBackground(BG_CARD); clrBtn.setBorderPainted(false); clrBtn.setFocusPainted(false);
        clrBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clrBtn.addActionListener(e->{ try{consoleDoc.remove(0,consoleDoc.getLength());}catch(BadLocationException ignored){} });

        btns.add(exportBtn); btns.add(clrBtn);
        top.add(title,BorderLayout.WEST); top.add(btns,BorderLayout.EAST);

        consolePane = new JTextPane(); consolePane.setEditable(false);
        consolePane.setBackground(new Color(5,10,24));
        consolePane.setFont(new Font("Courier New",Font.PLAIN,12));
        consoleDoc = consolePane.getStyledDocument();
        JScrollPane sc = new JScrollPane(consolePane); sc.setBorder(BorderFactory.createLineBorder(BORDER_C));
        p.add(top,BorderLayout.NORTH); p.add(sc,BorderLayout.CENTER); return p;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ACTIONS — REEDUCATION
    // ═══════════════════════════════════════════════════════════════════════

    private void doAccesDossier() {
        RobotAssistantReeducation r = selReeduc(); if(r==null) return;
        String code = new String(codeField.getPassword()).trim();
        String pid  = patientField.getText().trim();
        if(pid.isEmpty()){log("Saisissez un ID patient",C_ORANGE);return;}
        try {
            r.accederDossierPatient(code, pid);
            log("["+r.getId()+"] Acces accorde - Dossier : "+pid, C_GREEN);
            showDossierPatient(r); refreshAll();
        } catch(RobotException e) {
            showCriticalAlert("Acces Refuse","Robot : "+r.getId()+"\n"+e.getMessage());
            log("["+r.getId()+"] "+e.getMessage(), C_RED);
        }
    }

    private void showDossierPatient(RobotAssistantReeducation r) {
        String exo = r.getExerciceActuel()!=null ? r.getExerciceActuel().name() : "Aucun";
        String sec = r.isPatientEnSecurite() ? "En securite" : "EN DANGER";
        Object[][] data = {{"Robot",r.getId()},{"Patient",r.getIdPatient()},
            {"Exercice",exo},{"Urgence",r.getNiveauUrgence().name()},{"Securite",sec}};
        JPanel panel = new JPanel(new GridLayout(data.length+1,2,8,6));
        panel.setBackground(BG_CARD); panel.setBorder(BorderFactory.createEmptyBorder(14,18,14,18));
        panel.add(lbl2("Champ",Font.BOLD,13,ACCENT)); panel.add(lbl2("Valeur",Font.BOLD,13,ACCENT));
        for(Object[] row : data){ panel.add(lbl2(row[0].toString(),Font.BOLD,13,TEXT_SEC)); panel.add(lbl2(row[1].toString(),Font.PLAIN,13,TEXT_PRI)); }
        JOptionPane.showMessageDialog(this,panel,"Dossier Patient - "+r.getIdPatient(),JOptionPane.INFORMATION_MESSAGE);
        log("["+r.getId()+"] Dossier patient consulte",TEXT_SEC);
    }

    private void doExercice() {
        RobotAssistantReeducation r = selReeduc(); if(r==null) return;
        try {
            TypeExercice       type = TypeExercice.valueOf((String)exerciceCombo.getSelectedItem());
            UrgenceReeducation urg  = UrgenceReeducation.valueOf((String)urgenceCombo.getSelectedItem());
            int reps = Integer.parseInt(repsField.getText().trim());
            if(reps<=0) throw new NumberFormatException();
            r.demarrerExercice(type, reps, urg);
            log("["+r.getId()+"] "+type+" x"+reps+" - "+urg, urgColor(urg));
            if(urg==UrgenceReeducation.URGENCE_MEDICALE||urg==UrgenceReeducation.CRITIQUE)
                showCriticalAlert("Exercice a Haute Criticite",
                    "Robot : "+r.getId()+"\nExercice : "+type
                    +"\nUrgence : "+urg+"\nEnergie restante : "+r.getEnergie()+"%");
            refreshAll();
        } catch(MaintenanceRequiseException e) {
            showCriticalAlert("Maintenance Requise","Robot : "+r.getId()+"\n"+e.getMessage());
            log("["+r.getId()+"] "+e.getMessage(), C_ORANGE);
        } catch(EnergieInsuffisanteException e) {
            // Bug #2 maintenant détecté ici correctement
            log("["+r.getId()+"] Energie insuffisante : "+e.getMessage(), C_ORANGE);
        } catch(RobotException e) {
            log("["+r.getId()+"] "+e.getMessage(), C_RED);
        } catch(NumberFormatException e) {
            log("Repetitions invalides (entier > 0 requis)", C_ORANGE);
        }
    }

    private void doChute() {
        RobotAssistantReeducation r = selReeduc(); if(r==null) return;
        try {
            r.detecterChute();
        } catch(ChutePatientException e) {
            showCriticalAlert("CHUTE PATIENT DETECTEE !",
                "Robot : "+r.getId()+"\nPatient : "
                +(r.getIdPatient()!=null?r.getIdPatient():"non identifie")
                +"\n\n"+e.getMessage()+"\n\nAppeler les secours immediatement !");
            log("["+r.getId()+"] CHUTE DETECTEE - "+e.getMessage(), C_RED);
        } catch(EnergieInsuffisanteException e) {
            log("["+r.getId()+"] Energie insuffisante : "+e.getMessage(), C_ORANGE);
        } catch(RobotException e) {
            log("["+r.getId()+"] "+e.getMessage(), C_RED);
        }
        refreshAll();
    }

    private void doReinitialiser() {
        RobotAssistantReeducation r = selReeduc(); if(r==null) return;
        r.reinitialiserApresChute();
        log("["+r.getId()+"] Reinitialise - Mode ROUTINE", C_GREEN); refreshAll();
    }

    private void doDeplacerReeduc() {
        RobotAssistantReeducation r = selReeduc(); if(r==null) return;
        String lieu   = (String)lieuReedCombo.getSelectedItem();
        int[]  coords = LIEUX.get(lieu);
        try {
            r.deplacer(coords[0], coords[1]);
            log("["+r.getId()+"] -> "+lieu+" ("+coords[0]+","+coords[1]+")", C_GREEN); refreshAll();
        } catch(MaintenanceRequiseException e) {
            showCriticalAlert("Maintenance Requise","Robot : "+r.getId()+"\n"+e.getMessage());
            log("["+r.getId()+"] "+e.getMessage(), C_ORANGE);
        } catch(RobotException e) {
            log("["+r.getId()+"] Deplacement refuse : "+e.getMessage(), C_RED);
        }//Pas de vérification à la compilation
    }

    private void doAlerteGenerale() {
        int c = JOptionPane.showConfirmDialog(this,
            "Declencher une ALERTE GENERALE ?\nToutes les activites secondaires seront interrompues.",
            "Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if(c==JOptionPane.YES_OPTION) {
            gestionnaire.alerteGenerale(); // Bug #7 corrigé : interruption réelle
            showCriticalAlert("ALERTE GENERALE DECLENCHEE",
                "Tous les robots en URGENCE_MEDICALE.\nActivites secondaires interrompues.\nReseau libere pour communications medicales.");
            log("ALERTE GENERALE - Activites secondaires interrompues sur toute la flotte", C_RED);
            refreshAll();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ACTIONS — LIVRAISON
    // ═══════════════════════════════════════════════════════════════════════

    private void doChargerColis() {
        RobotLivraison r = selLiv(); if(r==null) return;
        String lieu = (String)lieuLivCombo.getSelectedItem();
        try {
            r.chargerColis(lieu); // Bug #8 : lieu (String) stocké comme colisActuel
            log("["+r.getId()+"] Colis charge - Destination : "+lieu, ACCENT); refreshAll();
        } catch(EnergieInsuffisanteException e) {
            log("["+r.getId()+"] Energie : "+e.getMessage(), C_ORANGE);
        } catch(RobotException e) {
            log("["+r.getId()+"] "+e.getMessage(), C_RED);
        }
    }

    /**
     * Bug #3 corrigé — ancienne version appelait deplacer() seul :
     *   r.deplacer(coords[0], coords[1]);
     * → Le robot arrivait à destination mais en_livraison restait true
     *   → couleur orange permanente, mission jamais terminée.
     *
     * Nouvelle version : appel à r.livrer() qui enchaîne
     *   deplacer() + remise à zéro de l'état de livraison.
     */
    private void doLivrer() {
        RobotLivraison r = selLiv(); if(r==null) return;
        String lieu   = (String)lieuLivCombo.getSelectedItem();
        int[]  coords = LIEUX.get(lieu);
        try {
            r.livrer(coords[0], coords[1]); // Bug #3 : livrer() au lieu de deplacer()
            log("["+r.getId()+"] Livraison terminee -> "+lieu
                +" ("+coords[0]+","+coords[1]+")", C_GREEN);
            refreshAll();
        } catch(MaintenanceRequiseException e) {
            showCriticalAlert("Maintenance Requise","Robot : "+r.getId()+"\n"+e.getMessage());
            log("["+r.getId()+"] "+e.getMessage(), C_ORANGE);
        } catch(EnergieInsuffisanteException e) {
            log("["+r.getId()+"] Energie : "+e.getMessage(), C_ORANGE);
        } catch(RobotException e) {
            log("["+r.getId()+"] "+e.getMessage(), C_RED);
        }
    }

    private void doConnexion() {
        RobotLivraison r = selLiv(); if(r==null) return;
        try {
            if(r.isConnecte()){r.deconnecter();log("["+r.getId()+"] Deconnecte",TEXT_SEC);}
            else               {r.connecter("LogiNet-5G");log("["+r.getId()+"] Connecte - LogiNet-5G",C_GREEN);}
            refreshAll();
        } catch(RobotException e){ log("["+r.getId()+"] "+e.getMessage(),C_RED); }
    }

    private void doRechargerTous() {
        // Bug #4 corrigé dans Robot.recharger() : Math.min(100, energie+quantite)
        for(RobotAssistantReeducation r : robotsReeduc)    r.recharger(30);
        for(RobotLivraison            r : robotsLivraison) r.recharger(30);
        log("Recharge +30% appliquee a toute la flotte", C_GREEN); refreshAll();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // AJOUTER UN ROBOT (3 etapes)
    // ═══════════════════════════════════════════════════════════════════════
    private void doAjouterRobot() {
        String[] types = {"Robot de Reeducation","Robot de Livraison"};
        String type = (String)JOptionPane.showInputDialog(this,"Type de robot :","Ajouter (1/3)",
            JOptionPane.QUESTION_MESSAGE,null,types,types[0]);
        if(type==null) return;

        String id = JOptionPane.showInputDialog(this,"Identifiant (ex: REED-03) :","Ajouter (2/3)");
        if(id==null||id.trim().isEmpty()){
            JOptionPane.showMessageDialog(this,"Identifiant vide.","Attention",JOptionPane.WARNING_MESSAGE); return;
        }
        id = id.trim().toUpperCase();
        final String fid = id;
        if(robotsReeduc.stream().anyMatch(r->r.getId().equals(fid))
         ||robotsLivraison.stream().anyMatch(r->r.getId().equals(fid))){
            JOptionPane.showMessageDialog(this,"Un robot '"+fid+"' existe deja.","Attention",JOptionPane.WARNING_MESSAGE); return;
        }

        String lieu = (String)JOptionPane.showInputDialog(this,"Position initiale :","Ajouter (3/3)",
            JOptionPane.QUESTION_MESSAGE,null,LIEUX_KEYS,LIEUX_KEYS[0]);
        if(lieu==null) return;

        int[] coords = LIEUX.get(lieu);
        try {
            if(type.contains("Reeducation")){
                RobotAssistantReeducation r = new RobotAssistantReeducation(fid,coords[0],coords[1]);
                r.demarrer(); robotsReeduc.add(r); gestionnaire.ajouterRobot(r); reedCombo.addItem(fid);
                log("["+fid+"] Robot reeducation ajoute a "+lieu, C_GREEN);
            } else {
                RobotLivraison r = new RobotLivraison(fid,coords[0],coords[1]);
                r.demarrer(); robotsLivraison.add(r); gestionnaire.ajouterRobot(r); livCombo.addItem(fid);
                log("["+fid+"] Robot livraison ajoute a "+lieu, ACCENT);
            }
            refreshAll();
            JOptionPane.showMessageDialog(this,"Robot "+fid+" ajoute !\nPosition : "+lieu,"Succes",JOptionPane.INFORMATION_MESSAGE);
        } catch(RobotException e){ log("Erreur creation : "+e.getMessage(),C_RED); }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // EXPORT RAPPORT
    // ═══════════════════════════════════════════════════════════════════════
    private void doExporterRapport() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("rapport_hopital_"
            +new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date())+".txt"));
        fc.setDialogTitle("Exporter le Rapport Hospitalier");
        if(fc.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION) return;

        try(BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(fc.getSelectedFile()), StandardCharsets.UTF_8))){
            String sep="=".repeat(70)+"\n", sep2="-".repeat(70)+"\n";
            String now=new SimpleDateFormat("dd/MM/yyyy a HH:mm:ss").format(new Date());
            bw.write(sep+"  RAPPORT HOPITAL INSAT\n  Genere le : "+now+"\n"+sep+"\n");
            bw.write("RESUME\n"+sep2);
            bw.write("  Robots reeducation : "+robotsReeduc.size()+"\n");
            bw.write("  Robots livraison   : "+robotsLivraison.size()+"\n\n");
            bw.write("ETAT ACTUEL\n"+sep2);
            for(RobotAssistantReeducation r:robotsReeduc) bw.write(r.toString()+"\n");
            for(RobotLivraison r:robotsLivraison)          bw.write(r.toString()+"\n");
            bw.write("\nHISTORIQUE\n"+sep2);
            for(RobotAssistantReeducation r:robotsReeduc){bw.write("\n-- "+r.getId()+" --\n");bw.write(r.get_historique()+"\n");}
            for(RobotLivraison r:robotsLivraison)          {bw.write("\n-- "+r.getId()+" --\n");bw.write(r.get_historique()+"\n");}
            bw.write("\n"+sep+"CONSOLE\n"+sep2+consolePane.getText()+"\n"+sep+"Fin du rapport\n"+sep);
            log("Rapport exporte : "+fc.getSelectedFile().getName(), C_GREEN);
            JOptionPane.showMessageDialog(this,"Rapport sauvegarde !\n"+fc.getSelectedFile().getAbsolutePath(),"Export reussi",JOptionPane.INFORMATION_MESSAGE);
        } catch(IOException e){
            showCriticalAlert("Erreur d'Export","Impossible de sauvegarder :\n"+e.getMessage());
            log("Erreur export : "+e.getMessage(), C_RED);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // POPUP CRITIQUE
    // ═══════════════════════════════════════════════════════════════════════
    private void showCriticalAlert(String title, String message) {
        JPanel panel = new JPanel(new BorderLayout(14,10));
        panel.setBackground(new Color(50,6,6));
        panel.setBorder(BorderFactory.createEmptyBorder(14,18,14,18));
        JLabel icon=new JLabel("(!)",SwingConstants.CENTER);
        icon.setFont(new Font("Segoe UI",Font.BOLD,36)); icon.setForeground(C_RED);
        icon.setBorder(BorderFactory.createEmptyBorder(0,0,0,12));
        JLabel msg=new JLabel("<html><center>"+message.replace("\n","<br>")+"</center></html>",SwingConstants.CENTER);
        msg.setFont(new Font("Segoe UI",Font.BOLD,14)); msg.setForeground(new Color(255,190,190));
        panel.add(icon,BorderLayout.WEST); panel.add(msg,BorderLayout.CENTER);
        JOptionPane opt=new JOptionPane(panel,JOptionPane.ERROR_MESSAGE,JOptionPane.DEFAULT_OPTION);
        JDialog dialog=opt.createDialog(this,title); dialog.setAlwaysOnTop(true); dialog.setVisible(true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // REFRESH
    // ═══════════════════════════════════════════════════════════════════════
    private void refreshAll(){ refreshDashboard(); grillePanel.repaint(); updateFleetStatus(); }

    private void updateFleetStatus(){
        boolean urg=robotsReeduc.stream()
            .anyMatch(r->r.getNiveauUrgence()==UrgenceReeducation.URGENCE_MEDICALE
                      ||r.getNiveauUrgence()==UrgenceReeducation.CRITIQUE);
        fleetStatusLabel.setText(urg?"URGENCE EN COURS":"SYSTEME OPERATIONNEL");
        fleetStatusLabel.setForeground(urg?C_RED:C_GREEN);
    }

    private void refreshDashboard(){
        dashPanel.removeAll();
        dashPanel.add(Box.createVerticalStrut(5));
        dashPanel.add(secLabel("Robots de Reeducation ("+robotsReeduc.size()+")"));
        dashPanel.add(Box.createVerticalStrut(4));
        for(RobotAssistantReeducation r:robotsReeduc){dashPanel.add(reedCard(r));dashPanel.add(Box.createVerticalStrut(5));}
        dashPanel.add(Box.createVerticalStrut(8));
        dashPanel.add(secLabel("Robots de Livraison ("+robotsLivraison.size()+")"));
        dashPanel.add(Box.createVerticalStrut(4));
        for(RobotLivraison r:robotsLivraison){dashPanel.add(livCard(r));dashPanel.add(Box.createVerticalStrut(5));}
        dashPanel.add(Box.createVerticalGlue());
        dashPanel.revalidate(); dashPanel.repaint();
    }

    private JPanel reedCard(RobotAssistantReeducation r){
        UrgenceReeducation urg=r.getNiveauUrgence();
        JPanel card=robotCard(urgColor(urg));
        JPanel top=hp(); JPanel bg=new JPanel(new FlowLayout(FlowLayout.RIGHT,4,0)); bg.setOpaque(false);
        bg.add(stBadge(r.isEnMarche())); bg.add(urgBadge(urg));
        top.add(lbl2("  "+r.getId(),Font.BOLD,13,TEXT_PRI),BorderLayout.WEST); top.add(bg,BorderLayout.EAST);
        JPanel mid=hp(); mid.add(energyBar(r.getEnergie()),BorderLayout.CENTER);
        mid.add(chip("("+r.getX()+","+r.getY()+")",TEXT_SEC),BorderLayout.EAST);
        JPanel bot=new JPanel(new FlowLayout(FlowLayout.LEFT,10,0)); bot.setOpaque(false);
        bot.add(chip(r.isConnecte()?r.getReseauConnecte():"Non connecte",r.isConnecte()?C_GREEN:TEXT_SEC));
        bot.add(chip(r.getHeuresUtilisation()+"h",TEXT_SEC));
        if(r.getIdPatient()!=null) bot.add(chip("P:"+r.getIdPatient(),ACCENT));
        if(!r.isPatientEnSecurite()) bot.add(chip("DANGER",C_RED));
        card.add(top,BorderLayout.NORTH); card.add(mid,BorderLayout.CENTER); card.add(bot,BorderLayout.SOUTH); return card;
    }

    private JPanel livCard(RobotLivraison r){
        JPanel card=robotCard(r.isEnLivraison()?C_ORANGE:BORDER_C);
        JPanel top=hp(); JPanel bg=new JPanel(new FlowLayout(FlowLayout.RIGHT,4,0)); bg.setOpaque(false);
        bg.add(stBadge(r.isEnMarche()));
        if(r.isEnLivraison()) bg.add(chip("EN LIVRAISON",C_ORANGE));
        top.add(lbl2("  "+r.getId(),Font.BOLD,13,TEXT_PRI),BorderLayout.WEST); top.add(bg,BorderLayout.EAST);
        JPanel mid=hp(); mid.add(energyBar(r.getEnergie()),BorderLayout.CENTER);
        mid.add(chip("("+r.getX()+","+r.getY()+")",TEXT_SEC),BorderLayout.EAST);
        JPanel bot=new JPanel(new FlowLayout(FlowLayout.LEFT,10,0)); bot.setOpaque(false);
        bot.add(chip(r.isConnecte()?r.getReseauConnecte():"Non connecte",r.isConnecte()?C_GREEN:TEXT_SEC));
        // Bug #8 : affiche le nom String du colis (plus 0/1)
        bot.add(chip(r.isEnLivraison()&&r.getDestination()!=null?r.getDestination():"Disponible",
                     r.isEnLivraison()?ACCENT:TEXT_SEC));
        bot.add(chip(r.getHeuresUtilisation()+"h",TEXT_SEC));
        card.add(top,BorderLayout.NORTH); card.add(mid,BorderLayout.CENTER); card.add(bot,BorderLayout.SOUTH); return card;
    }

    // ── Logging ───────────────────────────────────────────────────────────
    private void log(String msg, Color color){
        SwingUtilities.invokeLater(()->{
            try{
                String ts=new SimpleDateFormat("HH:mm:ss").format(new Date());
                StyleContext sc=StyleContext.getDefaultStyleContext();
                AttributeSet ta=sc.addAttribute(SimpleAttributeSet.EMPTY,StyleConstants.Foreground,TEXT_SEC);
                ta=sc.addAttribute(ta,StyleConstants.FontFamily,"Courier New"); ta=sc.addAttribute(ta,StyleConstants.FontSize,12);
                consoleDoc.insertString(consoleDoc.getLength(),"["+ts+"]  ",ta);
                AttributeSet ma=sc.addAttribute(SimpleAttributeSet.EMPTY,StyleConstants.Foreground,color);
                ma=sc.addAttribute(ma,StyleConstants.FontFamily,"Courier New"); ma=sc.addAttribute(ma,StyleConstants.FontSize,12);
                consoleDoc.insertString(consoleDoc.getLength(),msg+"\n",ma);
                consolePane.setCaretPosition(consoleDoc.getLength());
            }catch(BadLocationException ignored){}
        });
    }

    // ── Timer ─────────────────────────────────────────────────────────────
    private void startRefreshTimer(){
        refreshTimer=new Timer(true);
        refreshTimer.scheduleAtFixedRate(new TimerTask(){
            @Override public void run(){SwingUtilities.invokeLater(()->{updateClock();refreshAll();});}
        },0,2000);
    }
    private void updateClock(){if(clockLabel!=null)clockLabel.setText(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date()));}

    // ── Component helpers ─────────────────────────────────────────────────
    private JPanel makeCard(String title){
        JPanel p=new JPanel(new BorderLayout(0,6)); p.setBackground(BG_PANEL);
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_C),BorderFactory.createEmptyBorder(8,8,8,8)));
        JPanel tb=new JPanel(new BorderLayout()); tb.setOpaque(false);
        tb.setBorder(BorderFactory.createMatteBorder(0,0,1,0,BORDER_C));
        JLabel tl=new JLabel(title); tl.setFont(new Font("Segoe UI",Font.BOLD,13)); tl.setForeground(ACCENT);
        tl.setBorder(BorderFactory.createEmptyBorder(0,0,6,0)); tb.add(tl,BorderLayout.WEST); p.add(tb,BorderLayout.NORTH); return p;
    }
    private JPanel robotCard(Color bc){
        JPanel p=new JPanel(new BorderLayout(4,3)); p.setBackground(BG_CARD);
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(bc,1),BorderFactory.createEmptyBorder(7,11,7,11)));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE,100)); p.setAlignmentX(Component.LEFT_ALIGNMENT); return p;
    }
    private JPanel hp(){JPanel p=new JPanel(new BorderLayout()); p.setOpaque(false); return p;}
    private JPanel energyBar(final int val){
        JComponent bar=new JComponent(){
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                int w=getWidth(),h=getHeight();
                g2.setColor(new Color(14,28,55)); g2.fillRoundRect(0,h/2-5,w,10,8,8);
                Color c=val>50?C_GREEN:val>25?C_ORANGE:C_RED;
                g2.setColor(c); g2.fillRoundRect(0,h/2-5,Math.max(8,w*val/100),10,8,8); g2.dispose();
            }
        };
        bar.setPreferredSize(new Dimension(80,22));
        Color vc=val>50?C_GREEN:val>25?C_ORANGE:C_RED;
        JLabel pc=chip(val+"%",vc); pc.setFont(new Font("Segoe UI",Font.BOLD,11));
        JPanel w=new JPanel(new BorderLayout(6,0)); w.setOpaque(false);
        w.add(bar,BorderLayout.CENTER); w.add(pc,BorderLayout.EAST); return w;
    }
    private JLabel chip(String t,Color c){JLabel l=new JLabel(t);l.setFont(new Font("Segoe UI",Font.PLAIN,11));l.setForeground(c);return l;}
    private JLabel lbl(String t)         {JLabel l=new JLabel(t);l.setFont(new Font("Segoe UI",Font.PLAIN,12));l.setForeground(TEXT_PRI);return l;}
    private JLabel lbl2(String t,int s,int sz,Color c){JLabel l=new JLabel(t);l.setFont(new Font("Segoe UI",s,sz));l.setForeground(c);return l;}
    private JLabel stBadge(boolean on)   {JLabel l=new JLabel(on?"EN MARCHE":"ETEINT");l.setFont(new Font("Segoe UI",Font.BOLD,11));l.setForeground(on?C_GREEN:C_RED);return l;}
    private JLabel urgBadge(UrgenceReeducation u){JLabel l=new JLabel("  "+u.name());l.setFont(new Font("Segoe UI",Font.BOLD,11));l.setForeground(urgColor(u));return l;}
    private JLabel secLabel(String t)    {JLabel l=new JLabel(t);l.setFont(new Font("Segoe UI",Font.BOLD,12));l.setForeground(TEXT_SEC);l.setBorder(BorderFactory.createEmptyBorder(0,4,0,0));l.setAlignmentX(Component.LEFT_ALIGNMENT);return l;}
    private JTextField field(String t)   {JTextField f=new JTextField(t);styleField(f);return f;}
    private void styleField(JTextField f){f.setFont(new Font("Segoe UI",Font.PLAIN,12));f.setBackground(new Color(10,22,48));f.setForeground(TEXT_PRI);f.setCaretColor(ACCENT);f.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_C),BorderFactory.createEmptyBorder(4,7,4,7)));}
    private JComboBox<String> combo(String...items){JComboBox<String> cb=new JComboBox<>(items);cb.setFont(new Font("Segoe UI",Font.PLAIN,12));cb.setBackground(new Color(10,22,48));cb.setForeground(TEXT_PRI);return cb;}
    private JButton btn(String text,Color color){
        JButton b=new JButton(text){
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                Color base=getModel().isPressed()?color.darker():getModel().isRollover()?color.brighter():color;
                g2.setColor(base);g2.fillRoundRect(0,0,getWidth(),getHeight(),8,8);g2.dispose();super.paintComponent(g);
            }
        };
        b.setFont(new Font("Segoe UI",Font.BOLD,12));b.setForeground(Color.WHITE);b.setOpaque(false);b.setContentAreaFilled(false);
        b.setBorderPainted(false);b.setFocusPainted(false);b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(6,10,6,10));return b;
    }
    private GridBagConstraints gbc(){GridBagConstraints g=new GridBagConstraints();g.fill=GridBagConstraints.HORIZONTAL;g.insets=new Insets(3,4,3,4);return g;}
    private void row(JPanel form,GridBagConstraints g,int r,String lb,Component comp){
        g.gridy=r;g.gridx=0;g.gridwidth=1;g.weightx=0.35;form.add(lbl(lb),g);
        g.gridx=1;g.gridwidth=2;g.weightx=0.65;form.add(comp,g);
    }
    private Color urgColor(UrgenceReeducation u){switch(u){case ROUTINE:return C_GREEN;case SURVEILLANCE:return C_YELLOW;case CRITIQUE:return C_ORANGE;case URGENCE_MEDICALE:return C_RED;default:return TEXT_SEC;}}
    private RobotAssistantReeducation selReeduc(){int i=reedCombo.getSelectedIndex();if(i<0||i>=robotsReeduc.size()){log("Aucun robot de reeducation selectionne",C_ORANGE);return null;}return robotsReeduc.get(i);}
    private RobotLivraison selLiv(){int i=livCombo.getSelectedIndex();if(i<0||i>=robotsLivraison.size()){log("Aucun robot de livraison selectionne",C_ORANGE);return null;}return robotsLivraison.get(i);}

    // ── Grille 2D ─────────────────────────────────────────────────────────
    class GrillePanel extends JPanel {
        private static final int N=10;
        GrillePanel(){setBackground(new Color(5,10,24));}
        @Override protected void paintComponent(Graphics g){
            super.paintComponent(g);
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int w=getWidth(),h=getHeight(),cW=w/N,cH=h/N;
            g2.setColor(new Color(22,46,82));
            for(int i=0;i<=N;i++){g2.drawLine(i*cW,0,i*cW,h);g2.drawLine(0,i*cH,w,i*cH);}
            g2.setFont(new Font("Segoe UI",Font.PLAIN,7));
            for(Map.Entry<String,int[]> e:LIEUX.entrySet()){
                int[] c=e.getValue(); String nm=e.getKey(); if(nm.length()>9)nm=nm.substring(0,8)+".";
                g2.setColor(new Color(45,80,130)); g2.drawString(nm,c[0]*cW+2,c[1]*cH+10);
            }
            g2.setColor(new Color(50,82,130)); g2.setFont(new Font("Courier New",Font.PLAIN,7));
            for(int i=0;i<N;i++){g2.drawString(String.valueOf(i),i*cW+cW-9,h-2);g2.drawString(String.valueOf(i),1,i*cH+10);}
            for(RobotAssistantReeducation r:robotsReeduc){
                Color col=r.getNiveauUrgence()==UrgenceReeducation.URGENCE_MEDICALE?C_RED
                         :r.getNiveauUrgence()==UrgenceReeducation.CRITIQUE?C_ORANGE:C_GREEN;
                drawBot(g2,r.getX(),r.getY(),r.getId(),col,cW,cH);
            }
            for(RobotLivraison r:robotsLivraison)
                drawBot(g2,r.getX(),r.getY(),r.getId(),r.isEnLivraison()?C_ORANGE:ACCENT,cW,cH);
            g2.dispose();
        }
        private void drawBot(Graphics2D g2,int gx,int gy,String id,Color col,int cW,int cH){
            gx=Math.max(0,Math.min(N-1,gx));gy=Math.max(0,Math.min(N-1,gy));
            int cx=gx*cW+cW/2,cy=gy*cH+cH/2,r=Math.min(cW,cH)/3;
            g2.setColor(new Color(col.getRed(),col.getGreen(),col.getBlue(),50));g2.fillOval(cx-r-7,cy-r-7,(r+7)*2,(r+7)*2);
            g2.setColor(col);g2.fillOval(cx-r,cy-r,r*2,r*2);
            g2.setColor(Color.WHITE);g2.setStroke(new BasicStroke(1.2f));g2.drawOval(cx-r,cy-r,r*2,r*2);
            g2.setColor(TEXT_PRI);g2.setFont(new Font("Segoe UI",Font.BOLD,8));
            FontMetrics fm=g2.getFontMetrics(); String lb=id.length()>7?id.substring(0,7):id;
            g2.drawString(lb,cx-fm.stringWidth(lb)/2,cy+r+12);
        }
    }
    public static void main(String[] args ) {
    	new StationControle().setVisible(true);;
    	
    	
    }
}
