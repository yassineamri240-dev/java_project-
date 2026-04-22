package projet_final;

import java.util.ArrayList;

/**
 * GestionnaireHospitalier — pilote une flotte de robots via polymorphisme.
 *
 * Bug #7 corrigé : alerteGenerale() se contentait de changer le label
 *   niveau_urgence, sans réellement interrompre les activités secondaires.
 *   Correction : la méthode arrête les exercices en cours, réinitialise
 *   l'état des robots de rééducation, et déconnecte les robots de livraison
 *   de leurs missions secondaires — conformément au cahier des charges.
 *
 * Encapsulation : tous les accès aux champs robots passent par les getters.
 */
public class GestionnaireHospitalier {

    private ArrayList<Robot> flotte;

    public GestionnaireHospitalier() {
        this.flotte = new ArrayList<>();
    }

    // ── Gestion de la flotte ──────────────────────────────────────────────
    public void ajouterRobot(Robot robot) {
        flotte.add(robot);
        System.out.println("Robot ajouté : " + robot.getId());
    }

    public void supprimerRobot(String id) {
        flotte.removeIf(r -> r.getId().equals(id));
    }

    public ArrayList<Robot> getFlotte() { return flotte; }

    // ── Sélection du meilleur robot (polymorphisme) ───────────────────────
    /**
     * Sélectionne le robot disponible avec le plus d'énergie
     * pour un type de mission donné ("reeducation" ou "livraison").
     * Utilise les getters publics — aucun accès direct aux champs.
     */
    public Robot selectionnerRobotPourMission(String typeMission) {
        Robot meilleur   = null;
        int   maxEnergie = -1;

        for (Robot robot : flotte) {
            if (!robot.isEnMarche() || robot.getEnergie() <= maxEnergie) continue;

            if (typeMission.equals("reeducation")
                    && robot instanceof RobotAssistantReeducation) {
                meilleur   = robot;
                maxEnergie = robot.getEnergie();
            } else if (typeMission.equals("livraison")
                    && robot instanceof RobotLivraison) {
                meilleur   = robot;
                maxEnergie = robot.getEnergie();
            }
        }
        return meilleur;
    }

    // ── Alerte générale ───────────────────────────────────────────────────
    /**
     * Bug #7 corrigé — ancienne version :
     *   r.setNiveauUrgence(URGENCE_MEDICALE);  // juste un label
     *
     * Nouvelle version : interruption réelle de toutes les activités
     * secondaires sur toute la flotte, conformément au cahier des charges
     * ("méthode capable d'interrompre toutes les activités secondaires
     *  en cas d'alerte générale").
     *
     * Actions effectuées :
     *   - Robots rééducation : passage en URGENCE_MEDICALE + exercice annulé
     *     + réinitialisation sécurité pour permettre l'arrêt si nécessaire.
     *   - Robots livraison  : déconnexion réseau pour libérer la bande
     *     passante (les données médicales sont prioritaires).
     */
    public void alerteGenerale() {
        System.out.println("=== ALERTE GÉNÉRALE DÉCLENCHÉE ===");

        for (Robot robot : flotte) {

            // ─ Robots de rééducation ─────────────────────────────────────
            if (robot instanceof RobotAssistantReeducation) {
                RobotAssistantReeducation r = (RobotAssistantReeducation) robot;

                // 1. Passer en urgence médicale (setter — encapsulation)
                r.setNiveauUrgence(UrgenceReeducation.URGENCE_MEDICALE);

                // 2. Annuler l'exercice en cours si applicable
                //    (reinitialiserApresChute remet exerciceActuel=null et
                //     patient_en_securite=true ce qui permet l'arrêt d'urgence)
                if (!r.isPatientEnSecurite()) {
                    // Chute déjà en cours : on préserve l'état de danger
                    r.ajouterHistorique("ALERTE GÉNÉRALE — maintien urgence chute");
                } else {
                    // Interrompre l'exercice secondaire
                    r.reinitialiserApresChute(); // remet l'état propre
                    r.setNiveauUrgence(UrgenceReeducation.URGENCE_MEDICALE); // re-forcer
                    r.ajouterHistorique(
                        "ALERTE GÉNÉRALE — exercice interrompu, mode URGENCE_MEDICALE");
                }

                System.out.println("  [" + r.getId() + "] URGENCE_MEDICALE — activités secondaires interrompues");
            }

            // ─ Robots de livraison ────────────────────────────────────────
            if (robot instanceof RobotLivraison) {
                RobotLivraison r = (RobotLivraison) robot;

                // Libérer la bande passante réseau pour les communications médicales
                if (r.isConnecte()) {
                    r.deconnecter();
                    r.ajouterHistorique("ALERTE GÉNÉRALE — déconnexion réseau (priorité médicale)");
                } else {
                    r.ajouterHistorique("ALERTE GÉNÉRALE — en attente (non connecté)");
                }

                System.out.println("  [" + r.getId() + "] Réseau libéré pour urgence médicale");
            }
        }

        System.out.println("=== FIN DE L'INITIALISATION D'ALERTE ===");
    }

    // ── Affichage ─────────────────────────────────────────────────────────
    public void afficherFlotte() {
        System.out.println("=== FLOTTE (" + flotte.size() + " robots) ===");
        for (Robot robot : flotte) System.out.println("  " + robot);
    }
}
