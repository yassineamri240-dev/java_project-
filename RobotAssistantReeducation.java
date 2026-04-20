package projet_final;

import java.util.Scanner;

public class RobotAssistantReeducation extends RobotConnecte{
	private String id_patient;
	private TypeExercice exerciceActuel;
	private int repetition_effectuer;
	private int repetition_requise;
	private boolean patient_en_securite;
	private final int  energie_exercice_normal=3;
	private final int energie_urgence=15;
	private final String code_acces;
	private UrgenceReeducation niveau_urgence ;
	private boolean alerte_declenche;
	
	public RobotAssistantReeducation(String id, int x, int y) {
	    super(id, x, y, 100);  // Appel au constructeur de RobotConnecte
	    this.id_patient = null;
	    this.exerciceActuel = null;
	    this.repetition_effectuer = 0;
	    this.repetition_requise = 0;
	    this.patient_en_securite = true;
	    this.alerte_declenche = false;
	    this.niveau_urgence = UrgenceReeducation.ROUTINE;
	    this.code_acces = "REEDUC2025";
	}


	//----------------------------------------------------------------------------------------------------------------------------------------
	public void effectuerTache() throws RobotException {
	    if(!en_marche) {
	        throw new RobotException("Le robot doit être démarré");
	    }
	    
	    if(!patient_en_securite) {
	        if(!alerte_declenche) {
	            detecterChute();
	        } else {
	            ajouterHistorique("En attente des secours");
	        }
	        return;
	    }
	    
	    if(exerciceActuel == null) {
	        ajouterHistorique("En attente d'exercice");
	    } else {
	        ajouterHistorique("Exercice en cours: " + exerciceActuel);
	    }
	}
	
	
//------------------------------------------------------------------------------------------------------------------------------------------------------------
	public void detecterChute() throws ChutePatientException {
        if (!en_marche) {
            ajouterHistorique("Impossible de détecter une chute : robot éteint");
            throw new ChutePatientException("Robot éteint, chute non détectable");
        }

        if (energie < 15) {
            ajouterHistorique("Énergie insuffisante pour intervention d'urgence");
            throw new EnergieInsuffisanteException("Intervention chute impossible");
        }

        consommer_energie(15);
        patient_en_securite = false;
        niveau_urgence = UrgenceReeducation.URGENCE_MEDICALE;

        try {
            envoyerDonnees("ALERTE CHUTE PATIENT : " + id_patient);
        } catch (RobotException e) {
            ajouterHistorique("Échec envoi alerte chute : " + e.getMessage());
        }

        alerte_declenche = true;
        ajouterHistorique("🚨 CHUTE DÉTECTÉE - Alerte envoyée");
        throw new ChutePatientException("Chute patient : intervention requise");
    }
    
//---------------------------------------------------------------------------------------------------------------------------------------------------------------
	public void accederDossierPatient(String code, String id) throws RobotException {
        if (!code_acces.equals(code)) {
            ajouterHistorique("Tentative d'accès non autorisé");
            throw new RobotException("Code d'accès invalide");
        }
        this.id_patient = id;
        ajouterHistorique("Accès autorisé au dossier patient: " + id);
    }
//-------------------------------------------------------------------------------------------------------------------------------------------------------------
	public void demarrerExercice(TypeExercice exercice, int repetitions, UrgenceReeducation urgence) 
            throws RobotException {
        if (!en_marche) {
            throw new RobotException("Robot éteint");
        }
        
        this.exerciceActuel = exercice;
        this.repetition_requise = repetitions;
        this.niveau_urgence = urgence;
        this.repetition_effectuer = 0;
        
        switch(urgence) {
            case ROUTINE:
                consommer_energie(energie_exercice_normal);
                break;
            case SURVEILLANCE:
                consommer_energie(energie_exercice_normal + 2);
                break;
            case CRITIQUE:
                consommer_energie(energie_exercice_normal + 7);
                break;
            case URGENCE_MEDICALE:
                consommer_energie(energie_urgence);
                break;
        }
        
        ajouterHistorique("Exercice démarré: " + exercice + " - Urgence: " + urgence);
        
        // Simulation des répétitions
        for (int i = 1; i <= repetitions; i++) {
            repetition_effectuer++;
            ajouterHistorique("Répétition " + i + "/" + repetitions);
        }
        
        ajouterHistorique("Exercice terminé");
    }
//------------------------------------------------------------------------------------------------------------------------------------------------------------
	
	 public void reinitialiserApresChute() {
	        patient_en_securite = true;
	        alerte_declenche = false;
	        niveau_urgence = UrgenceReeducation.ROUTINE;
	        ajouterHistorique("Urgence terminée - Robot réinitialisé");
	    }
//----------------------------------------------------------------------------------------------------------------------------------------------------------
	 @Override
	    public void arreter() {
	        if (!patient_en_securite) {
	            ajouterHistorique("Impossible d'arrêter - Urgence médicale en cours");
	            return;
	        }
	        super.arreter();
	    }
	    
	    @Override
	    public void deplacer(int x, int y) throws RobotException {
	        double distance = Math.sqrt(Math.pow(x - this.x, 2) + Math.pow(y - this.y, 2));
	        if (distance > 50) {
	            throw new RobotException("Distance trop grande pour un robot de rééducation");
	        }
	        int energieConsommee = (int)(distance * 0.2);
	        verifier_energie(energieConsommee);
	        consommer_energie(energieConsommee);
	        heures_utilisation += (int)(distance / 10);
	        this.x = x;
	        this.y = y;
	        ajouterHistorique("Déplacement vers (" + x + "," + y + ")");
	    }
//----------------------------------------------------------------------------------------------------------------------------------------------------------
	    
	    
	    @Override
	    public String toString() {
	        return "RobotAssistantReeducation [ID: " + id + ", Position: (" + x + "," + y + 
	               "), Énergie: " + energie + "%, Patient: " + (id_patient != null ? id_patient : "aucun") +
	               ", Urgence: " + niveau_urgence + "]";
	    }
	 
	 
	 
	 
	 
	 
	 
	 
	
}
