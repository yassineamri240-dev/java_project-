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
	
	//---------------------------------------------------------------------------------------------------------------------------
	public Robot(String id, int x, int y, int energie) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.energie = energie;
        this.heures_utilisation = 0;
        this.en_marche = false;
        this.historique_actions = new ArrayList<>();
        ajouterHistorique("Robot créé");
        }
	
	
//AJOUTER HISTORIQUE --------------------------------------------------------------------------------------------------------------
	
	public void ajouterHistorique(String action) {
	        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm:ss");
	        String dateTime = LocalDateTime.now().format(formatter);
	        historique_actions.add(dateTime + " " + action);
	    }
	 
 
//VERIFIER ENERGIE  ------------------------------------------------------------------------------------------------------------------------ 
	
	 
	 public void verifier_energie(int energie_requise) throws EnergieInsuffisanteException{
		
			
		if(this.energie< energie_requise) {
			throw new EnergieInsuffisanteException("energie insuffisante !");
		}
		
	}
	
//VERIFIER MAINTENENCE  -------------------------------------------------------------------------------------------------------------------
   
	public void verifier_maintenance() throws MaintenanceRequiseException {
        if (this.heures_utilisation >= 100) {
            throw new MaintenanceRequiseException("Maintenance requise ! " + this.heures_utilisation + " heures d'utilisation");
        }
    }
    
//DEMARER--------------------------------------------------------------------------------------------------------------------------
   
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
    
//ARRETER  ---------------------------------------------------------------------------------------------------------------------
   
    public void arreter() {
        if (this.en_marche) {
            this.en_marche = false;
            ajouterHistorique("Arrêt du robot");
        } else {
            ajouterHistorique("Tentative d'arrêt alors que robot déjà éteint");
        }
    }
    
    
    
//consommer energie  ----------------------------------------------------------------------------------------------------------
   
    public void consommer_energie(int quantite) {
        this.energie = Math.max(0, this.energie - quantite);
    }
	
	
    
 //recharger ---------------------------------------------------------------------------------------------------------------------
    public void recharger(int quantite) {
    	if(this.energie+quantite<=100) {
    		
	        this.energie+=quantite;
	        }
    	
    }
 //--------------------------------------------------------------------------------------------------------------------------------

    public abstract void deplacer(int x, int y) throws RobotException;
    public abstract void effectuerTache() throws RobotException;
    

 //historique comeplet ------------------------------------------------------------------------------------------------------------
    public String get_historique() {
    	String historique_complet="";
    	for ( String element : this.historique_actions) {
    		historique_complet=historique_complet+"\n"+element ;
    	}
    	return historique_complet;
    }
    
  //------------------------------------------------------------------------------------------------------------------------------    
    public  String  toString() {
    	return "RobotIndustriel"+ "["+this.id+" : "+"position : ("+String.valueOf(this.x)+","+String.valueOf(this.y)+
    			",Energie : "+String.valueOf(this.energie)+"% , Heurres : "+String.valueOf(this.heures_utilisation)+"]"; 
    } 
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
}
