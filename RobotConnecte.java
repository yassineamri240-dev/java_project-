package projet_final;

/**
 * Classe abstraite RobotConnecte.
 * Étend Robot et implémente l'interface Connectable.
 */
public abstract class RobotConnecte extends Robot implements Connectable {

    protected boolean connecte;
    protected String  reseau_connecte;

    // ── Constructeur ──────────────────────────────────────────────────────
    public RobotConnecte(String id, int x, int y, int energie) {
        super(id, x, y, energie);
        this.connecte        = false;
        this.reseau_connecte = null;
    }

    // ── Getters ───────────────────────────────────────────────────────────
    public boolean isConnecte()        { return connecte; }
    public String  getReseauConnecte() { return reseau_connecte; }

    // ── Connectable ───────────────────────────────────────────────────────
    @Override
    public void connecter(String reseau) throws RobotException {
        verifier_energie(5);
        this.reseau_connecte = reseau;
        this.connecte        = true;
        consommer_energie(5);
        ajouterHistorique("Connexion au réseau : " + reseau);
    }

    @Override
    public void deconnecter() {
        if (this.connecte) {
            ajouterHistorique("Déconnexion du réseau : " + reseau_connecte);
            this.connecte        = false;
            this.reseau_connecte = null;
        } else {
            ajouterHistorique("Tentative de déconnexion : robot non connecté");
        }
    }

    @Override
    public void envoyerDonnees(String donnees) throws RobotException {
        if (!this.connecte)
            throw new RobotException(
                "Impossible d'envoyer des données : robot non connecté");
        verifier_energie(3);
        consommer_energie(3);
        ajouterHistorique("Envoi sur " + reseau_connecte + " : " + donnees);
    }

    @Override
    public String toString() {
        return super.toString()
                + ", connecté:" + connecte
                + (connecte ? "(" + reseau_connecte + ")" : "");
    }
}
