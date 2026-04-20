package projet_final;

import java.util.Scanner;

public class RobotLivraison extends RobotConnecte {
	  private int colis_actuel;   
	  private String destination;   
	  private boolean en_livraison;
	  private static final int energie_livraison  = 15; 
	  private static final int energie_chargement = 5;  
	  
	  
	 //--------------------------------------------------------------------------------------------------------------------------- 
	  public RobotLivraison(String id , int x, int y ) {
		  super(id,x,y,100);
		  this.colis_actuel=0;
		  this.destination=null;
		  this.en_livraison=false;
		  
		
	  }
	  
	  
	  
	  
	//---------------------------------------------------------------------------------------------------------------------------
	  
	  public void effectuerTache() throws RobotException{
		  Scanner scanner = new Scanner(System.in);
		  if(!en_marche) {
			  throw new RobotException("Le robot doit être démarré pour effectuer une tâche");
		  }
		  
		  else if(en_livraison) {
			  System.out.print("entrez la coordonnée X de la destination : ");
	          int destX = scanner.nextInt();
	          System.out.print("entrez la coordonnée Y de la destination : ");
	          int destY = scanner.nextInt();
	          faireLivraison(destX, destY);
			  
		  }
		  else {
	            
	            System.out.print("Voulez-vous charger un nouveau colis ? (oui/non) : ");
	            String reponse = scanner.next().trim().toLowerCase();
	 
	            if (reponse.equals("oui")) {
	                
	                verifier_energie(energie_chargement);
	                System.out.print("Entrez la destination du colis : ");
	                scanner.nextLine();
	                String dest = scanner.nextLine().trim();
	                chargerColis(dest);
	            } else {
	                ajouterHistorique("En attente de colis");
	                System.out.println("Robot en attente de colis.");
	            }
	        }
		  }
	  
	  
	  
	  
	  
	  
	  	  
	  
	//---------------------------------------------------------------------------------------------------------------------------
	  private void faireLivraison(int destX, int destY) throws RobotException {
	        
	        deplacer(destX, destY);  
	        ajouterHistorique("Livraison terminée à " + destination);
	        System.out.println("Livraison terminée à " + destination);
	 
	        this.colis_actuel = 0;
	        this.en_livraison = false;
	        this.destination = null;
	    
		  
	  }
	  
	  
	  
	  
	//---------------------------------------------------------------------------------------------------------------------------
	  public void deplacer(int x, int y) throws RobotException {
	        
	        double distance = Math.sqrt(Math.pow(x - this.x, 2) + Math.pow(y - this.y, 2));
	 
	        
	        if (distance > 100) {
	            throw new RobotException(
	                    "Déplacement impossible : distance (" + String.format("%.2f", distance)
	                    + " unités) supérieure à 100 unités");
	        }
	 
	        
	        int energieRequise = (int) Math.ceil(distance * 0.3);
	        verifier_energie(energieRequise);   
	        verifier_maintenance();              
	 
	        
	        consommer_energie(energieRequise);
	 
	        
	        this.heures_utilisation += (int) (distance / 10);
	 
	        
	        this.x = x;
	        this.y = y;
	 
	        ajouterHistorique(String.format("Déplacement vers (%d, %d) — distance : %.2f unités, énergie consommée : %d%%",x, y, distance, energieRequise));
	    }
	  
	  
	  
	//---------------------------------------------------------------------------------------------------------------------------
	  public void chargerColis(String destination) throws RobotException {
	        if (this.en_livraison) {
	            throw new RobotException("Le robot est déjà en cours de livraison");
	        }
	        if (this.colis_actuel != 0) {
	            throw new RobotException("Le robot a déjà un colis chargé");
	        }
	 
	        verifier_energie(energie_chargement); 
	 
	        this.colis_actuel  = 1;
	        this.destination  = destination;
	        this.en_livraison  = true;
	        consommer_energie(energie_chargement);
	 
	        ajouterHistorique("Chargement du colis — destination : " + destination);
	        System.out.println("Colis chargé. Destination : " + destination);
	    }
	//---------------------------------------------------------------------------------------------------------------------------
	  public int     getColisActuel()  { return colis_actuel; }
	  public String  getDestination()  { return destination; }
	  public boolean isEnLivraison()   { return en_livraison; }
	//---------------------------------------------------------------------------------------------------------------------------
	  public String toString() {
	        return "RobotLivraison [ID : " + id
	                + ", Position : (" + x + ", " + y + ")"
	                + ", Énergie : " + energie + "%"
	                + ", Heures : " + heures_utilisation
	                + ", Colis : " + colis_actuel
	                + ", Destination : " + (destination != null ? destination : "aucune")
	                + ", Connecté : " + (connecte ? "Oui" : "Non") + "]";
	    }
}
	  


