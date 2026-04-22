package projet_final;

/**
 * RobotLivraison — robot spécialisé dans la livraison de colis.
 *
 * Bug #3 corrigé : faireLivraison() était private, le GUI ne pouvait pas
 *   terminer la mission → robot bloqué indéfiniment "En livraison".
 *   Solution : méthode publique livrer(int x, int y) qui enchaîne
 *   deplacer() + fin de mission en un seul appel.
 *
 * Bug #6 corrigé : effectuerTache() utilisait new Scanner(System.in),
 *   bloquant l'Event Dispatch Thread de Swing.
 *   Solution : effectuerTache() lève une exception métier claire ;
 *   toute interaction utilisateur est gérée par le GUI.
 *
 * Bug #8 corrigé : colisActuel était int (0 ou 1), non conforme au cahier
 *   des charges qui exige String ("PC Portable", "Vaccins"...).
 *   Solution : colisActuel est maintenant un String, null si pas de colis.
 */
public class RobotLivraison extends RobotConnecte {

    // ── Bug #8 : colisActuel est String (spec : "PC Portable", "Vaccins") ─
    private String  colisActuel;   // null = pas de colis
    private String  destination;
    private boolean en_livraison;

    private static final int ENERGIE_LIVRAISON  = 15;
    private static final int ENERGIE_CHARGEMENT = 5;

    // ── Constructeur ──────────────────────────────────────────────────────
    public RobotLivraison(String id, int x, int y) {
        super(id, x, y, 100);
        this.colisActuel  = null;   // Bug #8 : null au lieu de 0
        this.destination  = null;
        this.en_livraison = false;
    }

    // ── Getters ───────────────────────────────────────────────────────────
    public String  getColisActuel() { return colisActuel; }
    public String  getDestination() { return destination; }
    public boolean isEnLivraison()  { return en_livraison; }

    // ── effectuerTache ────────────────────────────────────────────────────
    /**
     * Bug #6 corrigé : l'ancienne version utilisait Scanner(System.in),
     * ce qui bloquait le thread Swing. La méthode communique maintenant
     * l'état du robot via des exceptions ou un retour, et le GUI
     * prend en charge toute la logique d'interaction.
     */
    @Override
    public void effectuerTache() throws RobotException {
        if (!en_marche)
            throw new RobotException(
                "Le robot doit être démarré pour effectuer une tâche");
        if (en_livraison)
            ajouterHistorique("En cours de livraison vers : " + destination);
        else
            ajouterHistorique("En attente de colis");
    }

    // ── chargerColis ─────────────────────────────────────────────────────
    public void chargerColis(String destination) throws RobotException {
        if (this.en_livraison)
            throw new RobotException("Le robot est déjà en cours de livraison");
        // Bug #8 : vérification sur String (null) au lieu de int (0)
        if (this.colisActuel != null)
            throw new RobotException("Le robot a déjà un colis chargé : "
                    + this.colisActuel);
        verifier_energie(ENERGIE_CHARGEMENT);

        this.colisActuel  = destination; // Bug #8 : on stocke la description comme String
        this.destination  = destination;
        this.en_livraison = true;
        consommer_energie(ENERGIE_CHARGEMENT);
        ajouterHistorique("Colis chargé — destination : " + destination);
    }

    // ── livrer (Bug #3 — méthode publique) ───────────────────────────────
    /**
     * Bug #3 corrigé : remplace le private faireLivraison().
     * Le GUI peut maintenant appeler livrer() directement pour enchaîner
     * déplacement ET fin de mission en un seul appel cohérent.
     */
    public void livrer(int destX, int destY) throws RobotException {
        if (!en_livraison)
            throw new RobotException(
                "Aucune livraison en cours — chargez d'abord un colis");
        if (!en_marche)
            throw new RobotException("Robot éteint — démarrez le robot d'abord");

        deplacer(destX, destY);

        // Fin de mission
        ajouterHistorique("Livraison terminée à : " + destination);
        this.colisActuel  = null;   // Bug #8 : null au lieu de 0
        this.en_livraison = false;
        this.destination  = null;
    }

    // ── deplacer ──────────────────────────────────────────────────────────
    @Override
    public void deplacer(int x, int y) throws RobotException {
        double distance      = Math.sqrt(Math.pow(x - this.x, 2) + Math.pow(y - this.y, 2));
        if (distance > 100)
            throw new RobotException(
                "Déplacement impossible : distance (" + String.format("%.2f", distance)
                + ") supérieure à 100 unités");

        // Coût minimal 1 pour éviter les déplacements "gratuits" (Bug #1 côté livraison
        // était déjà géré par ceil — on le conserve et on s'assure du minimum 1)
        int energieRequise = Math.max(1, (int) Math.ceil(distance * 0.3));

        verifier_energie(energieRequise);
        verifier_maintenance();

        consommer_energie(energieRequise);
        this.heures_utilisation += (int) (distance / 10);
        this.x = x;
        this.y = y;
        ajouterHistorique(String.format(
            "Déplacement vers (%d,%d) — %.2f unités — %d%% consommés",
            x, y, distance, energieRequise));
    }

    // ── toString ──────────────────────────────────────────────────────────
    @Override
    public String toString() {
        return "RobotLivraison [ID:" + id
                + ", pos:(" + x + "," + y + ")"
                + ", énergie:" + energie + "%"
                + ", heures:" + heures_utilisation
                // Bug #8 : affiche le nom du colis (String) ou "aucun"
                + ", colis:" + (colisActuel != null ? colisActuel : "aucun")
                + ", destination:" + (destination != null ? destination : "aucune")
                + ", connecté:" + (connecte ? reseau_connecte : "Non") + "]";
    }
}
