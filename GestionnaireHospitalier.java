package projet_final;

import java.util.ArrayList;

public class GestionnaireHospitalier {
    private ArrayList<Robot> flotte;
    
    public GestionnaireHospitalier() {
        this.flotte = new ArrayList<>();
    }
    
    public void ajouterRobot(Robot robot) {
        flotte.add(robot);
        System.out.println("Robot ajouté: " + robot.id);
    }
    
    public void supprimerRobot(String id) {
        flotte.removeIf(robot -> robot.id.equals(id));
    }
    
    public Robot selectionnerRobotPourMission(String typeMission) {
        Robot meilleur = null;
        int maxEnergie = -1;
        
        for (Robot robot : flotte) {
            if (robot.en_marche && robot.energie > maxEnergie) {
                if (typeMission.equals("reeducation") && robot instanceof RobotAssistantReeducation) {
                    meilleur = robot;
                    maxEnergie = robot.energie;
                } else if (typeMission.equals("livraison") && robot instanceof RobotLivraison) {
                    meilleur = robot;
                    maxEnergie = robot.energie;
                }
            }
        }
        return meilleur;
    }
    
    public void alerteGenerale() {
        System.out.println("🚨 ALERTE GÉNÉRALE 🚨");
        for (Robot robot : flotte) {
            if (robot instanceof RobotAssistantReeducation) {
                RobotAssistantReeducation r = (RobotAssistantReeducation) robot;
                r.niveau_urgence = UrgenceReeducation.URGENCE_MEDICALE;
                System.out.println("Robot " + r.id + " - Urgence médicale");
            }
        }
    }
    
    public void afficherFlotte() {
        for (Robot robot : flotte) {
            System.out.println(robot);
        }
    }
}