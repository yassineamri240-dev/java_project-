package projet_final;

public abstract class RobotConnecte extends Robot implements Connectable{
	
	protected boolean connecte;
	protected String reseau_connecte; 
	
	//---------------------------------------------------------------------------------------------------------------------------
	public RobotConnecte(String id, int x, int y, int energie) {
		super(id, x,  y, energie);
		this.connecte=false;
		this.reseau_connecte=null;
		
	}
	
	//---------------------------------------------------------------------------------------------------------------------------
	public void connecter(String reseau) throws RobotException {
        verifier_energie(5); 
        this.reseau_connecte = reseau;
        this.connecte = true;
        consommer_energie(5);
        ajouterHistorique("Connexion au réseau : " + reseau);
    
	}
	
	//---------------------------------------------------------------------------------------------------------------------------
	public void deconnecter() {
        if (this.connecte) {
            ajouterHistorique("Déconnexion du réseau : " + reseau_connecte);
            this.connecte = false;
            this.reseau_connecte = null;
        } else {
            ajouterHistorique("Tentative de déconnexion alors que le robot n'est pas connecté");
        }
    }
	
	//---------------------------------------------------------------------------------------------------------------------------
	public void envoyerDonnees(String donnees) throws RobotException {
        if (!this.connecte) {
            throw new RobotException("Impossible d'envoyer des données : le robot n'est pas connecté à un réseau");
        }
        verifier_energie(3); 
        consommer_energie(3);
        ajouterHistorique("Envoi de données sur " + reseau_connecte + " : " + donnees);
	}
	
	
	//---------------------------------------------------------------------------------------------------------------------------
	 public String toString() {
	        return super.toString()
	                + ", Connecté : " + connecte
	                + (connecte ? ", Réseau : " + reseau_connecte : "");
	    }
	}
	
	
	
	
	
	
	


