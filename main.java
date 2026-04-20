package projet_final;

public class main {

    public static void main(String[] args) {

        System.out.println("=== Simulation d'une livraison avec RobotLivraison ===\n");

        try {
            // 1. Créer le robot (position initiale (0,0), énergie 100%)
            RobotLivraison robot = new RobotLivraison("R2-D2", 0, 0);
            System.out.println("Robot créé : " + robot);

            // 2. Démarrer le robot
            robot.demarrer();
            System.out.println("Robot démarré.");

            // 3. Connexion à un réseau
            robot.connecter("ResauLogistique-5G");
            System.out.println("Robot connecté.");

            // 4. Envoyer des données via le réseau
            robot.envoyerDonnees("Nouveau colis à prendre en charge : PC Portable");

            // 5. Charger un colis
            robot.chargerColis("23 rue de la paix, Ariana");

            // 6. Effectuer la livraison (déplacement vers la destination (30, 40))
            System.out.println("\n--- Livraison en cours ---");
            robot.deplacer(30, 40);

            // Mettre à jour manuellement après déplacement (faireLivraison via effectuerTache en prod)
            // Ici on simule directement le résultat final :
            System.out.println("Livraison terminée !");

            // 7. Afficher l'état du robot
            System.out.println("\nÉtat final du robot :");
            System.out.println(robot);

            // 8. Afficher l'historique complet
            System.out.println("\n=== Historique des actions ===");
            System.out.println(robot.get_historique());

        } catch (EnergieInsuffisanteException e) {
            System.out.println("ERREUR - Énergie insuffisante : " + e.getMessage());
        } catch (MaintenanceRequiseException e) {
            System.out.println("ERREUR - Maintenance requise : " + e.getMessage());
        } catch (RobotException e) {
            System.err.println("ERREUR - Robot : " + e.getMessage());
        }
    }
}