package projet_final;

import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Classe abstraite Robot — base de toute la hiérarchie.
 *
 * Bug #4 corrigé : recharger() utilisait une condition stricte (<=100)
 * qui empêchait toute recharge dès que energie+quantite dépassait 100,
 * même légèrement. Remplacé par Math.min(100, energie+quantite).
 */
public abstract class Robot {

    // ── Attributs (protected : visibilité optimale pour la hiérarchie) ────
    protected String       id;
    protected int          x, y;
    protected int          energie;
    protected int          heures_utilisation;
    protected boolean      en_marche;
    protected List<String> historique_actions;

    // ── Constructeur ──────────────────────────────────────────────────────
    public Robot(String id, int x, int y, int energie) {
        this.id                 = id;
        this.x                  = x;
        this.y                  = y;
        this.energie            = energie;
        this.heures_utilisation = 0;
        this.en_marche          = false;
        this.historique_actions = new ArrayList<>();
        ajouterHistorique("Robot créé");
    }

    // ── Getters publics (encapsulation stricte) ───────────────────────────
    public String  getId()                { return id; }
    public int     getX()                 { return x; }
    public int     getY()                 { return y; }
    public int     getEnergie()           { return energie; }
    public boolean isEnMarche()           { return en_marche; }
    public int     getHeuresUtilisation() { return heures_utilisation; }

    // ── Historique ────────────────────────────────────────────────────────
    public void ajouterHistorique(String action) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm:ss");
        historique_actions.add(LocalDateTime.now().format(fmt) + " " + action);
    }

    public String get_historique() {
        StringBuilder sb = new StringBuilder();
        for (String e : historique_actions) sb.append("\n").append(e);
        return sb.toString();
    }

    // ── Énergie ───────────────────────────────────────────────────────────
    public void verifier_energie(int energie_requise) throws EnergieInsuffisanteException {
        if (this.energie < energie_requise)
            throw new EnergieInsuffisanteException(
                "Énergie insuffisante ! (" + this.energie + "% disponible, "
                + energie_requise + "% requis)");
    }

    public void consommer_energie(int quantite) {
        this.energie = Math.max(0, this.energie - quantite);
    }

    /**
     * BUG #4 CORRIGÉ — ancienne implémentation :
     *   if (this.energie + quantite <= 100) { this.energie += quantite; }
     * → Si énergie=80 et quantite=30 : 110 > 100, donc RIEN ne se passait.
     * Correction : on plafonne à 100 avec Math.min.
     */
    public void recharger(int quantite) {
        int avant = this.energie;
        this.energie = Math.min(100, this.energie + quantite);
        ajouterHistorique("Recharge +" + (this.energie - avant)
                + "% — Énergie : " + this.energie + "%");
    }

    // ── Maintenance ───────────────────────────────────────────────────────
    public void verifier_maintenance() throws MaintenanceRequiseException {
        if (this.heures_utilisation >= 100)
            throw new MaintenanceRequiseException(
                "Maintenance requise après " + this.heures_utilisation + "h d'utilisation");
    }

    // ── Démarrer / Arrêter ────────────────────────────────────────────────
    public void demarrer() throws RobotException {
        if (this.energie < 10) {
            ajouterHistorique("Échec démarrage — énergie insuffisante (" + this.energie + "%)");
            throw new RobotException("Impossible de démarrer : énergie < 10%");
        }
        if (this.en_marche) {
            ajouterHistorique("Tentative de démarrage : robot déjà allumé");
            throw new RobotException("Le robot est déjà allumé");
        }
        this.en_marche = true;
        ajouterHistorique("Démarrage du robot");
    }

    public void arreter() {
        if (this.en_marche) {
            this.en_marche = false;
            ajouterHistorique("Arrêt du robot");
        } else {
            ajouterHistorique("Tentative d'arrêt : robot déjà éteint");
        }
    }

    // ── Méthodes abstraites ───────────────────────────────────────────────
    public abstract void deplacer(int x, int y) throws RobotException;
    public abstract void effectuerTache()        throws RobotException;

    // ── toString ──────────────────────────────────────────────────────────
    @Override
    public String toString() {
        return "Robot[" + id + ", pos:(" + x + "," + y + "), énergie:"
                + energie + "%, heures:" + heures_utilisation + "]";
    }
}
