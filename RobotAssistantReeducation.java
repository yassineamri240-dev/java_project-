package projet_final;

/**
 * RobotAssistantReeducation — robot de rééducation médicale.
 *
 * Bug #1 corrigé : deplacer() utilisait (int)(distance * 0.2),
 *   ce qui donnait 0 pour les petits déplacements → énergie gratuite.
 *   Solution : Math.max(1, (int) Math.ceil(distance * 0.2)).
 *
 * Bug #2 corrigé : demarrerExercice() appelait consommer_energie()
 *   directement sans passer par verifier_energie() → robot à 0%
 *   pouvait "effectuer" un exercice.
 *   Solution : verifier_energie(cout) ajouté avant chaque consommation.
 *
 * Bug #5 corrigé : deplacer() n'appelait pas verifier_maintenance(),
 *   contrairement à RobotLivraison. Un robot à 100h+ pouvait se déplacer
 *   indéfiniment sans déclencher de MaintenanceRequiseException.
 *   Solution : verifier_maintenance() ajouté dans deplacer().
 */
public class RobotAssistantReeducation extends RobotConnecte {

    private String             id_patient;
    private TypeExercice       exerciceActuel;
    private int                repetition_effectuer;
    private int                repetition_requise;
    private boolean            patient_en_securite;
    private final int          ENERGIE_EXERCICE_NORMAL = 3;
    private final int          ENERGIE_URGENCE         = 15;
    private final String       CODE_ACCES;
    private UrgenceReeducation niveau_urgence;
    private boolean            alerte_declenche;

    // ── Constructeur ──────────────────────────────────────────────────────
    public RobotAssistantReeducation(String id, int x, int y) {
        super(id, x, y, 100);
        this.id_patient           = null;
        this.exerciceActuel       = null;
        this.repetition_effectuer = 0;
        this.repetition_requise   = 0;
        this.patient_en_securite  = true;
        this.alerte_declenche     = false;
        this.niveau_urgence       = UrgenceReeducation.ROUTINE;
        this.CODE_ACCES           = "REEDUC2025";
    }

    // ── Getters / Setters ─────────────────────────────────────────────────
    public UrgenceReeducation getNiveauUrgence()                       { return niveau_urgence; }
    public void               setNiveauUrgence(UrgenceReeducation u)   { this.niveau_urgence = u; }
    public boolean            isPatientEnSecurite()                    { return patient_en_securite; }
    public String             getIdPatient()                           { return id_patient; }
    public TypeExercice       getExerciceActuel()                      { return exerciceActuel; }
    public boolean            isAlerteDeclenche()                      { return alerte_declenche; }

    // ── effectuerTache ────────────────────────────────────────────────────
    @Override
    public void effectuerTache() throws RobotException {
        if (!en_marche)
            throw new RobotException("Le robot doit être démarré");
        if (!patient_en_securite) {
            if (!alerte_declenche) detecterChute();
            else                   ajouterHistorique("En attente des secours");
            return;
        }
        if (exerciceActuel == null) ajouterHistorique("En attente d'exercice");
        else                        ajouterHistorique("Exercice en cours : " + exerciceActuel);
    }

    // ── detecterChute ─────────────────────────────────────────────────────
    public void detecterChute() throws RobotException {
        if (!en_marche) {
            ajouterHistorique("Impossible de détecter une chute : robot éteint");
            throw new ChutePatientException("Robot éteint — chute non détectable");
        }
        // verifier_energie avant consommer (cohérence Bug #2)
        verifier_energie(ENERGIE_URGENCE);
        consommer_energie(ENERGIE_URGENCE);

        patient_en_securite = false;
        niveau_urgence      = UrgenceReeducation.URGENCE_MEDICALE;

        try {
            envoyerDonnees("ALERTE CHUTE PATIENT : " + id_patient);
        } catch (RobotException e) {
            ajouterHistorique("Échec envoi alerte : " + e.getMessage());
        }
        alerte_declenche = true;
        ajouterHistorique("CHUTE DÉTECTÉE — Alerte envoyée pour patient " + id_patient);
        throw new ChutePatientException(
            "Chute patient " + id_patient + " : intervention requise");
    }

    // ── accederDossierPatient ─────────────────────────────────────────────
    public void accederDossierPatient(String code, String id) throws RobotException {
        if (!CODE_ACCES.equals(code)) {
            ajouterHistorique("Tentative d'accès non autorisé au dossier patient");
            throw new RobotException("Code d'accès invalide — accès refusé");
        }
        this.id_patient = id;
        ajouterHistorique("Accès autorisé — dossier patient : " + id);
    }

    // ── demarrerExercice ──────────────────────────────────────────────────
    /**
     * Bug #2 corrigé : l'ancienne version appelait consommer_energie()
     * directement. Un robot à 0% pouvait "démarrer" un exercice.
     * Correction : verifier_energie(cout) est appelé avant chaque
     * consommer_energie() pour lever EnergieInsuffisanteException si besoin.
     */
    public void demarrerExercice(TypeExercice exercice, int repetitions,
                                  UrgenceReeducation urgence) throws RobotException {
        if (!en_marche)
            throw new RobotException("Robot éteint — démarrez le robot d'abord");

        // Calcul du coût selon l'urgence
        int cout;
        switch (urgence) {
            case ROUTINE:          cout = ENERGIE_EXERCICE_NORMAL;      break;
            case SURVEILLANCE:     cout = ENERGIE_EXERCICE_NORMAL + 2;  break;
            case CRITIQUE:         cout = ENERGIE_EXERCICE_NORMAL + 7;  break;
            case URGENCE_MEDICALE: cout = ENERGIE_URGENCE;              break;
            default:               cout = ENERGIE_EXERCICE_NORMAL;
        }

        // Bug #2 : vérification AVANT consommation
        verifier_energie(cout);
        consommer_energie(cout);

        this.exerciceActuel       = exercice;
        this.repetition_requise   = repetitions;
        this.niveau_urgence       = urgence;
        this.repetition_effectuer = 0;

        ajouterHistorique("Exercice démarré : " + exercice + " — " + urgence
                + " — " + repetitions + " rép. prévues");

        for (int i = 1; i <= repetitions; i++) {
            repetition_effectuer++;
            ajouterHistorique("Répétition " + i + "/" + repetitions);
        }
        ajouterHistorique("Exercice terminé : " + exercice);
    }

    // ── reinitialiserApresChute ───────────────────────────────────────────
    public void reinitialiserApresChute() {
        patient_en_securite = true;
        alerte_declenche    = false;
        niveau_urgence      = UrgenceReeducation.ROUTINE;
        exerciceActuel      = null;           // on remet aussi l'exercice à zéro
        ajouterHistorique("Urgence terminée — Robot réinitialisé en mode ROUTINE");
    }

    // ── Overrides ─────────────────────────────────────────────────────────
    @Override
    public void arreter() {
        if (!patient_en_securite) {
            ajouterHistorique("Impossible d'arrêter — urgence médicale en cours");
            return;
        }
        super.arreter();
    }

    /**
     * Bug #1 corrigé : (int)(distance * 0.2) donnait 0 pour
     *   distance < 5 unités → deplacement gratuit même à 0% d'énergie.
     *   Correction : Math.max(1, (int) Math.ceil(distance * 0.2))
     *   garantit un coût minimal de 1%.
     *
     * Bug #5 corrigé : verifier_maintenance() absente dans cette méthode
     *   alors qu'elle est présente dans RobotLivraison.deplacer().
     *   Ajout de l'appel pour une cohérence totale dans la hiérarchie.
     */
    @Override
    public void deplacer(int x, int y) throws RobotException {
        double distance = Math.sqrt(Math.pow(x - this.x, 2) + Math.pow(y - this.y, 2));

        if (distance > 50)
            throw new RobotException(
                "Distance trop grande (" + String.format("%.1f", distance)
                + " unités) — limite 50 pour un robot de rééducation");

        // Bug #1 : coût minimal 1 garanti via Math.ceil + Math.max
        int energieRequise = Math.max(1, (int) Math.ceil(distance * 0.2));

        verifier_energie(energieRequise);    // lève EnergieInsuffisanteException si besoin
        verifier_maintenance();              // Bug #5 : maintenant présent ici aussi

        consommer_energie(energieRequise);
        this.heures_utilisation += (int) (distance / 10);
        this.x = x;
        this.y = y;
        ajouterHistorique(String.format(
            "Déplacement vers (%d,%d) — %.1f unités — %d%% consommés",
            x, y, distance, energieRequise));
    }

    // ── toString ──────────────────────────────────────────────────────────
    @Override
    public String toString() {
        return "RobotAssistantReeducation [ID:" + id
                + ", pos:(" + x + "," + y + ")"
                + ", énergie:" + energie + "%"
                + ", heures:" + heures_utilisation
                + ", patient:" + (id_patient     != null ? id_patient     : "aucun")
                + ", exercice:" + (exerciceActuel != null ? exerciceActuel : "aucun")
                + ", urgence:" + niveau_urgence
                + ", connecté:" + (connecte ? reseau_connecte : "Non") + "]";
    }
}
