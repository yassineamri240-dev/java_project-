package projet_final;
import java.util.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public abstract class Robot {
	protected String id;
	protected int x,y;
	protected int energie;
	protected int heures_utilisation;
	protected boolean en_marche ;
	protected List<String> historique_actions ;
	public Robot(String id, int x, int y, int energie) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.energie = energie;
        this.heures_utilisation = 0;
        this.en_marche = false;
        this.historique_actions = new ArrayList<>();
        }
	 public void ajouterHistorique(String action) {
	        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm:ss");
	        String dateTime = LocalDateTime.now().format(formatter);
	        historique_actions.add(dateTime + " " + action);
	    }
	public void verifier_energie(int energie_requise) throws EnergieInsuffisanteException{
		if(this.energie< energie_requise) {
			throw new EnergieInsuffisanteException("energie insuffisante !");
		}
	}
    public void verifierMaintenance() throws MaintenanceRequiseException {
        if (this.heures_utilisation >= 100) {
            throw new MaintenanceRequiseException("Maintenance requise ! " + this.heures_utilisation + " heures d'utilisation");
        }
    }
    public void demarrer() throws RobotException {
        int energie_minimale = 10;  
        
        if (this.energie < energie_minimale) {
            ajouterHistorique("Échec démarrage : énergie insuffisante (" + this.energie + "%)");
            throw new RobotException("Impossible de démarrer : énergie < 10%");
        }
        
        if (this.en_marche) {
            ajouterHistorique("Tentative de démarrage alors que robot déjà allumé");
            throw new RobotException("Le robot est déjà allumé");
        }
        
        this.en_marche = true;
        ajouterHistorique("Démarrage du robot");
    }
    
    // Arrêter le robot
    public void arreter() {
        if (this.en_marche) {
            this.en_marche = false;
            ajouterHistorique("Arrêt du robot");
        } else {
            ajouterHistorique("Tentative d'arrêt alors que robot déjà éteint");
        }
    }
	
	
}
