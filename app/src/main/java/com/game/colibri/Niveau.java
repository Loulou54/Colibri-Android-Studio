package com.game.colibri;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.Random;

import android.util.SparseArray;

/**
 * Classe contenant toutes les données d'UN niveau : carte, position initiale du colibri, 
 * déplacement des animaux mouvants (chats et vaches) et solution (optionnelle)
 */
public class Niveau {
	
	private static final boolean DEBUG = false;
	public static final int CAMPAGNE=0, FACILE=1, MOYEN=2, DIFFICILE=3, PERSO=4;
	
	/*
	 * Définit s'il s'agit d'un niveau aléatoire ou non.
	 */
	public boolean isRandom;
	
	/*
	 * L'expérience acquise si on résout le niveau.
	 */
	public int experience;
	
	/*
	 * Le progrès minimum requis dans campagne pour faire ce niveau. (utilisé pour les parties rapides)
	 */
	public int progressMin;
	
	/*
	 * Carte contenant les informations statiques du niveau en
	 * question c'est-à-dire les menhirs, les fleurs ainsi
	 * que les fleurs magiques et les dynamites.
	 * Elle est modifiée pendant le jeu.
	 */
	public int[][] carte;
	
	/*
	 * Carte du niveau qui ne sera pas modifiée durant le jeu. Permet de rétablir la carte si on recommence le niveau.
	 */
	private int[][] carteOrigin;
	
	/*
	 * Position initiale du Colibri (la ligne de départ) au 
	 * lancement du niveau
	 */
	
	public int db_l;
	/*
	 * Position initiale du Colibri (la colonne de départ) au 
	 * lancement du niveau
	 */
	public int db_c;
	
	/*
	 * Optionnel pour chaque niveau, la solution afin de résoudre
	 * celui-ci si l'utilisateur est trop bloqué
	 */
	public int[][] solution=null;
	
	/*
	 * Liste des déplacements des différentes vaches. Pour chaque
	 * vache on a une liste de déplacements sous la forme d'une 
	 * matrice nombre_déplacements*2 c'est-à-dire les coordonnées
	 * dans la matrice des points par lesquels elle passe.
	 */
	public LinkedList<int[][]> vaches;
	
	/*
	 * Liste des déplacements des différents chats. Pour chaque
	 * chat on a une liste de déplacements sous la forme d'une 
	 * matrice nombre_déplacements*2 c'est-à-dire les coordonnées
	 * dans la matrice des points par lesquels il passe.
	 */
	public LinkedList<int[][]> chats;
	
	/* 
	* Pour la génération de niveaux aléatoires, cette matrice
	* contient le nombre de passages du Colibri sur chaque case
	*/
	public int[][] chemin;
	
	public Passages passColibri;
	public Passages passVaches;
	public Passages passChats;
	private int frame, frameMem;
	public long seed=0;
	private Random random;
	private double step,v_max,acc;
	public int nVaches=3, nDyna=3, nChats=2, nArcs=2; // Paramètres de quantité, dans [0,6]
	public SparseArray<int[]> rainbows;
	public boolean[] presenceRainbows; // Indicates the presence of rainbows on cols and rows
	
	public LinkedList<int[]> h_fleurs; // Valeurs heuristiques de certaines fleurs pour augmenter l'efficacité du solveur.
	public int h_param = -1; // Paramètre h_param pour le solveur, si besoin pour optimiser.

	/**
	 * Consructeur de niveau de campagne
	 * @param file le fichier source du niveau
	 */
	public Niveau(InputStream file) {
		isRandom=false;
		init();
		lireNiveau(file);
		replay();
	}
	
	/**
	 * Constructeur d'un niveau aléatoire de paramètres donnés.
	 * @param mode le mode de niveau aléatoire (cf: constantes de classe en static)
	 */
	public Niveau(int mode, long graine, int[] param, int avancement) {
		isRandom=true;
		init();
		int lon, lar, base;
		switch(mode) {
		case FACILE:
			lon=8; lar=5; nVaches=1; nDyna=1; nChats=1; nArcs=1; base=1;
			break;
		case MOYEN:
			lon=12; lar=8; nVaches=3; nDyna=3; nChats=2; nArcs=2; base=1;
			break;
		case DIFFICILE:
			lon=18; lar=12; nVaches=4; nDyna=4; nChats=3; nArcs=3; base=1;
			break;
		default:
			lon = param[0];
			lar = lon/2;
			importParam(param);
			base = param[5];
		}
		if(avancement<9) nVaches=0;
		if(avancement<16) nDyna=0;
		if(avancement<21) nChats=0;
		if(avancement<23) nArcs=0;
		//graine = -9141506973145342355L;
		//graine = 8722479907385699505L;
		//graine = -4955446407346367673L;
		//graine = 6230089956759031988L;
		//graine = 1259394720530780785L;
		//graine = -5161610159804422431L; // full params
		//graine = -3162648970676374098L;
		//graine = -8952430756904305296L; // Beginner : suboptimal solution
		this.geneNivRand(lon, lar, base, graine);
		if(vaches.size()+chats.size() >= 6 && lon >= 18)
			h_param = 8;
		replay();
	}
	
	/**
	 * Initialisation des structures de données.
	 */
	private void init() {
		carteOrigin= new int[12][20];
		carte=new int[12][];
		if(isRandom) chemin= new int[12][20];
		vaches = new LinkedList<int[][]>();
		chats = new LinkedList<int[][]>();
	}
	
	/**
	 * Permet de recommencer le niveau : on rétablit la carte d'origine en copiant "carteOrigin"
	 */
	public void replay() {
		for(int i=0; i<carteOrigin.length; i++) {
			carte[i]=carteOrigin[i].clone();
		}
	}

	public void lireNiveau(InputStream ips){
		String [][]matrice =null;
		passVaches = new Passages(20);
		passChats = new Passages(20);
		rainbows = new SparseArray<int[]>();
		try {
			//InputStream ips=new FileInputStream("niveaux/niveau"+niveau+".txt");
							// ouverture du fichier texte voulu
			InputStreamReader ipsr=new InputStreamReader(ips);
			BufferedReader br=new BufferedReader(ipsr);
			String ligne;
			String text="";
			int nbLigne=0;
			// boucle permettant de compter le nombre de lignes dans le fichier texte
			// et remplissant une variable "text" de type String avec les données dans
			// le fichier texte
			while ((ligne=br.readLine())!=null){
				text=text+ ligne+ "\n";
				nbLigne++;
			}
			br.close();
			// permet de créer une liste avec les différentes lignes du fichier texte
			String[] list=text.split("\n");
			
			/* 
			* Matrice qui contiendra les différents éléments de la matrice du fichier texte 
			* mais ceux-ci seront des String */
			matrice=new String[12][];
			for(int i=0;i<12;i++){
				matrice[i]=list[i].split(",");
			}
			/*
			 *  Changement des éléments String en éléments int pour obtenir la 
			 *  matrice désirée + construction de la structure rainbows
			 */
			for(int i=0;i<12;i++){
				for(int j=0;j<20;j++){
					int v = Integer.valueOf(matrice[i][j]);
					carteOrigin[i][j] = v;
					if(v >= 10) { // Arc-en-ciel
						int[] arc_pair = rainbows.get(v);
						if(arc_pair==null)
							rainbows.put(v, new int[] {i, j, 0, 0});
						else {
							arc_pair[2] = i;
							arc_pair[3] = j;
						}
					}
				}
			}
			// La ligne 13 possède les informations de départ du colibri
			String[] depart = list[12].split(",");
			db_l = Integer.valueOf(depart[0]);
			db_c = Integer.valueOf(depart[1]);
			/* 
			 * Les lignes suivantes posèdent des informations quant aux 
			 * animaux mouvants ainsi que la solution. On ne connaît pas
			 * leur nombre au préalable et la solution est optionnelle, d'où
			 * l'importance d'avoir compté le nombre de lignes auparavant
			 */
			for (int i = 13; i < nbLigne ;i++) {
				/*
				 *  Si la ligne décrit les déplacements d'une vache et est
				 *  donc sous la forme "vache=" suivi d'une succession de 
				 *  nombres et de virgules
				 */
				if (list[i].startsWith("vache", 0)) {
					String line = list[i].substring(6, list[i].length());
					String[] elements = line.split(",");
					//nombre de points de déplacements que possède la vache
					int nombre_deplacements = (elements.length)/2;
					int[][] list_vache = new int[nombre_deplacements][2];
					int period = 0; // La période en frames du parcours de la vache.
					for (int j = 0; j < nombre_deplacements; j ++) {
						list_vache[j][0] = Integer.valueOf(elements[2*j]);
						list_vache[j][1] = Integer.valueOf(elements[2*j+1]);
						if(j>0)
							period += 20*Math.max(Math.abs(list_vache[j][0]-list_vache[j-1][0]), Math.abs(list_vache[j][1]-list_vache[j-1][1]));
					}
					period += 20*Math.max(Math.abs(list_vache[0][0]-list_vache[nombre_deplacements-1][0]), Math.abs(list_vache[0][1]-list_vache[nombre_deplacements-1][1]));
					addAnimal(list_vache, period, passVaches, vaches, 20, 18, 0);
				}
				/*
				 *  Si la ligne décrit les déplacements d'un chat et est
				 *  donc sous la forme "chat=" suivi d'une succession de 
				 *  nombres et de virgules
				 */
				if (list[i].startsWith("chat", 0)) {
					String line = list[i].substring(5, list[i].length());
					String[] elements = line.split(",");
					//nombre de points de déplacements que possède le chat
					int nombre_deplacements = (elements.length)/2;
					int[][] list_chat = new int[nombre_deplacements][2];
					int period = 0; // La période en frames du parcours du chat.
					for (int j = 0; j < nombre_deplacements; j ++) {
						list_chat[j][0] = Integer.valueOf(elements[2*j]);
						list_chat[j][1] = Integer.valueOf(elements[2*j+1]);
						if(j>0)
							period += 2 + 4*Math.max(Math.abs(list_chat[j][0]-list_chat[j-1][0]), Math.abs(list_chat[j][1]-list_chat[j-1][1]));
					}
					period += 2 + 4*Math.max(Math.abs(list_chat[0][0]-list_chat[nombre_deplacements-1][0]), Math.abs(list_chat[0][1]-list_chat[nombre_deplacements-1][1]));
					addAnimal(list_chat, period, passChats, chats, 4, 6, 2);
				}
				/*
				 *  Si la ligne décrit la solution et est donc sous la forme 
				 *  "solution=" suivi d'une succession de nombres et de virgules
				 *  ATTENTION : la solution dans les fichiers est décrite selon (lig,col) alors que dans le jeu c'est (x,y) !!
				 */
				if (list[i].startsWith("solution",0)) { // Mouvements enchaînés (pas de notion de temps) (doublets (l,c))
					String line = list[i].substring(9, list[i].length());
					String[] elements = line.split(",");
					int nombre_deplacements = (elements.length)/2;
					solution = new int[nombre_deplacements][3];
					for (int j = 0; j < nombre_deplacements; j ++) {
						solution[j][2] = 0;
						solution[j][1] = Integer.valueOf(elements[2*j]);
						solution[j][0] = Integer.valueOf(elements[2*j+1]);
					}
				} else if(list[i].startsWith("sol_temp",0)) { // Triplets (l,c,t)
					String line = list[i].substring(9, list[i].length());
					String[] elements = line.split(",");
					int nombre_deplacements = (elements.length)/3;
					solution = new int[nombre_deplacements][3];
					for (int j = 0; j < nombre_deplacements; j ++) {
						solution[j][2] = Integer.valueOf(elements[3*j+2]);
						solution[j][1] = Integer.valueOf(elements[3*j]);
						solution[j][0] = Integer.valueOf(elements[3*j+1]);
					}
				}
				/*
				 * Poids de certaines fleurs pour augmenter l'efficacité du solveur.
				 * Format : (r,c,h)
				 */
				if(list[i].startsWith("h_fleur",0)) {
					if(h_fleurs==null)
						h_fleurs = new LinkedList<int[]>();
					String[] elements = list[i].substring(8, list[i].length()).split(",");
					int n = elements.length/3;
					for(int j = 0; j < n; j++) {
						h_fleurs.add(new int[] {
								Integer.valueOf(elements[3*j]),
								Integer.valueOf(elements[3*j+1]),
								Integer.valueOf(elements[3*j+2])
								});
					}
				}
				/*
				 * Paramètre spécifique h_param pour le solveur.
				 * Format : h_param=8
				 */
				if(list[i].startsWith("h_param",0)) {
					h_param = Integer.valueOf(list[i].substring(8, list[i].length()));
				}
			}
			computePresenceRainbows();
		}
		//si une exception est levée au cours de l'exécution
		catch (Exception e) {
			System.out.println("Mauvaise syntaxe du niveau : " + e.toString());
		} 
	}
	
	public int[][] getCarte() {
		return carteOrigin;
	}


	public int getDb_l() {
		return db_l;
	}


	public int getDb_c() {
		return db_c;
	}


	public int[][] getSolution() {
		return solution;
	}


	public LinkedList<int[][]> getVaches() {
		return vaches;
	}


	public LinkedList<int[][]> getChats() {
		return chats;
	}


	public int[][] getChemin() {
		return chemin;
	}

	
	
	
	// NIVEAU ALEATOIRE 
	
	/**
	 * Permet de définir un passage possiblement récurrent.
	 * 
	 * @author Louis
	 *
	 */
	public static class Occurrence {
		int r,mod,delta;
		int dir_in = 0;
		
		/**
		 * Constructeur
		 * @param r time%mod lorsque le passage a lieu.
		 * @param mod la période
		 * @param delta
		 */
		public Occurrence(int r, int mod, int delta) {
			if(mod==0)
				mod=Integer.MAX_VALUE;
			this.r=r%mod;
			this.mod=mod;
			this.delta=delta;
		}
		
		public Occurrence(int r, int mod, int delta, int dir_in) {
			if(mod==0)
				mod=Integer.MAX_VALUE;
			this.r=r%mod;
			this.mod=mod;
			this.delta=delta;
			this.dir_in=dir_in;
		}
		
		@Override
		public String toString() {
			return "("+r+","+mod+","+delta+")";
		}
		
		private int gcd(int a, int b) { return b==0 ? a : gcd(b, a%b); }
		
		private int distance(int r1, int r2, int m) {
			return Math.min(((r1-r2)%m+m)%m , ((r2-r1)%m+m)%m);
		}
		
		public int difference(int time) {
			return distance(time,r,mod);
		}
		
		public boolean isColidingWith(Occurrence v) {
			if(mod==Integer.MAX_VALUE && v.mod==Integer.MAX_VALUE)
				return Math.abs(r-v.r)<=delta+v.delta;
			else if(mod==Integer.MAX_VALUE)
				return distance(v.r,r,v.mod)<=delta+v.delta;
			else if(v.mod==Integer.MAX_VALUE)
				return distance(v.r,r,mod)<=delta+v.delta;
			else {
				int pgcd = gcd(mod,v.mod);
				return distance(v.r,r,pgcd) <= delta+v.delta;
			}
		}
	}
	
	/**
	 * Structure contenant les passages d'objets mouvants sur les cases de la carte.
	 * 
	 * @author Louis
	 *
	 */
	public static class Passages {
		public int col;
		private SparseArray<LinkedList<Occurrence>> passages = new SparseArray<LinkedList<Occurrence>>();
		
		public Passages(int colonnes) {
			col = colonnes;
			passages = new SparseArray<LinkedList<Occurrence>>();
		}
		
		/**
		 * Ajoute un passage sur la case (r,c) passant à l'instant time et de période period,
		 * delta est le nombre de frames pris par l'objet pour sortir de la case.
		 * La fonction retourne également l'occurence ajoutée.
		 * @param r ligne
		 * @param c colonne
		 * @param time temps (nb de frames depuis le début)
		 * @param period période du mouvement en nb de frames (==Integer.MAX_VALUE si passage unique)
		 * @param delta nombre de frames pris par l'objet pour sortir de la case
		 * @return oc l'occurence ajoutée.
		 */
		public Occurrence addOccurrence(int r, int c, int time, int period, int delta, int dr, int dc) {
			LinkedList <Occurrence> occurences = passages.get(r*col+c);
			if(occurences==null) {
				occurences = new LinkedList<Occurrence>();
				passages.put(r*col+c, occurences);
			}
			Occurrence oc = new Occurrence(time,period,delta, dr==-1?1:(dr==1?4:(dc==-1?3:(dc==1?2:0))));
			occurences.addLast(oc);
			return oc;
		}
		
		/**
		 * Supprime la dernière occurence à la case (r,c) s'il y a lieu.
		 * @param r ligne
		 * @param c colonne
		 */
		public void removeLast(int r, int c) {
			LinkedList <Occurrence> occurences = passages.get(r*col+c);
			if(occurences!=null)
				occurences.removeLast();
		}
		
		/**
		 * Retourne la liste des passages (occurences) sur la case (r,c)
		 * @param r
		 * @param c
		 * @return la liste d'occurences
		 */
		public LinkedList <Occurrence> getOccurrences(int r, int c) {
			return passages.get(r*col+c);
		}
		
		public boolean isColidingWith(int r, int c, Occurrence v) {
			LinkedList <Occurrence> occurences = getOccurrences(r,c);
			if(occurences==null)
				return false;
			boolean ici=false;
			for(Occurrence oc : occurences) {
				ici=oc.isColidingWith(v);
				if(ici)
					break;
			}
			return ici;
		}
		
		public Occurrence returnColisionWith(int r, int c, Occurrence v) {
			LinkedList <Occurrence> occurences = getOccurrences(r,c);
			if(occurences==null)
				return null;
			Occurrence ici=null;
			for(Occurrence oc : occurences) {
				if(oc.isColidingWith(v)) {
					ici = oc;
					break;
				}
			}
			return ici;
		}
		
		@Override
		public String toString() {
			String s="";
			for(int i=0; i<passages.size(); i++) {
				LinkedList<Occurrence> occs = passages.valueAt(i);
				s+="("+(passages.keyAt(i)/col)+","+(passages.keyAt(i)%col)+") : ";
				for(Occurrence o : occs) {
					s+=o.r+" "+o.mod+" "+o.delta+" | ";
				}
				s+="\n";
			}
			return s;
		}
	}
	
	
	private void importParam(int[] param) {
		nVaches = Math.max(param[1],0);
		nDyna = Math.max(param[2],0);
		nChats = Math.max(param[3],0);
		nArcs = Math.max(param[4],0);
	}
	
	/**
	 * Construit presenceRainbows utilisé par la visualisation de solution.
	 * presenceRainbows est indexé par colonne ou N_COL + ligne et contient true
	 * si au moins un arc-en-ciel est présent sur la colonne/ligne.
	 */
	public void computePresenceRainbows() {
		presenceRainbows = new boolean[20+12];
		int n = rainbows.size();
		for(int i=0; i<n; i++) {
			int[] arcPair = rainbows.valueAt(i);
			presenceRainbows[20+arcPair[0]] = true;
			presenceRainbows[arcPair[1]] = true;
			presenceRainbows[20+arcPair[2]] = true;
			presenceRainbows[arcPair[3]] = true;
		}
	}
	
	private void fillPosAleat(int dist, LinkedList<Integer> pos, int sig) { // Génération des possibilités.
		int tot = 0;
		int n;
		while(tot<=dist-3) {
			n=random.nextInt(3)+1;
			tot+=n;
			pos.add(sig*n);
		}
		if(tot!=dist)
			pos.add(sig*(dist-tot));
	}
	
	// GÉNÉRATION PARCOURS CHATS
	
	private boolean valideCheminRChat(int r, int c, int rf, int cf, int frameMod, int period) {
		int s = Integer.signum(cf-c);
		Occurrence chat = new Occurrence(frameMod, period, 5);
		frameMod+=2; // Dû à l'accélération.
		while(c>=0 && c<20 && c!=cf+s && Math.abs(carteOrigin[r][c]-7)!=1 && carteOrigin[r][c]<10 && carteOrigin[r][c]%2==0 && !passVaches.isColidingWith(r, c, chat) && !passColibri.isColidingWith(r, c, chat) && !passChats.isColidingWith(r, c, chat)) {
			frameMod=(frameMod+4)%period;
			chat = new Occurrence(frameMod, period, 5);
			c+=s;
		}
		return c==cf+s;
	}
	
	private boolean valideCheminCChat(int r, int c, int rf, int cf, int frameMod, int period) {
		int s = Integer.signum(rf-r);
		Occurrence chat = new Occurrence(frameMod, period, 5);
		frameMod+=2;
		while(r>=0 && r<12 && r!=rf+s && Math.abs(carteOrigin[r][c]-7)!=1 && carteOrigin[r][c]<10 && carteOrigin[r][c]%2==0 && !passVaches.isColidingWith(r, c, chat) && !passColibri.isColidingWith(r, c, chat) && !passChats.isColidingWith(r, c, chat)) {
			frameMod=(frameMod+4)%period;
			chat = new Occurrence(frameMod, period, 5);
			r+=s;
		}
		return r==rf+s;
	}
	
	/**
	 * Génère un parcours pour un chat devant passer à (r,c) aux instants 0%lon[0].
	 * La longueur du parcours généré sera stocké dans lon.
	 * @param r ligne de l'emplacement voulu
	 * @param c colonne de l'emplacement voulu
	 * @param lon longueur (en frames) d'un cycle du parcours
	 * @return la liste des checkpoints de la vache si succès, en faisant démarrer la vache
	 * de manière à ce qu'elle arrive en (r,c) à l'instant voulu ; null sinon.
	 */
	private int[][] geneChat(int r, int c, int[] lon) {
		int distLig = random.nextInt(6)+1;
		int distCol = random.nextInt(6)+1;
		LinkedList<Integer> posLig = new LinkedList<Integer>(); // Possibilités de déplacement sur les lignes, dont la somme fait 0. Ex : [3,1,2,-1,-1,-3,-1]
		LinkedList<Integer> posCol = new LinkedList<Integer>(); // Idem sur les colonnes.
		fillPosAleat(distLig,posLig,1);
		fillPosAleat(distLig,posLig,-1);
		fillPosAleat(distCol,posCol,1);
		fillPosAleat(distCol,posCol,-1);
		int pof;
		int cs=posCol.size(),ls=posLig.size();
		int dep;
		int n;
		lon[0] = (distLig+distCol)*8+(posLig.size()+posCol.size())*2;
		int frameMod = 0;
		boolean valide;
		int[][] deplacements = new int[cs+ls][2];
		int cpt=0, boucles=0;
		while((cs!=0 || ls!=0) && boucles<=Math.max(cs+ls , 4)) {
			if(boucles==0) {
				deplacements[cpt][0]=r; deplacements[cpt][1]=c;}
			pof=random.nextInt(2);
			if(pof==0 && cs!=0 || ls==0) { // Déplacement selon une colonne
				n=random.nextInt(cs);
				dep=posCol.get(n);
				valide=valideCheminCChat(r, c, r+dep, c, frameMod, lon[0]);
				if(valide) {
					r+=dep;
					frameMod=(frameMod+2+4*Math.abs(dep))%lon[0];
					cpt++;
					posCol.remove(n);
					cs--;
					boucles=0;
				} else
					boucles++;
			} else { // Déplacement selon une ligne
				n=random.nextInt(ls);
				dep=posLig.get(n);
				valide=valideCheminRChat(r, c, r, c+dep, frameMod, lon[0]);
				if(valide) {
					c+=dep;
					frameMod=(frameMod+2+4*Math.abs(dep))%lon[0];
					cpt++;
					posLig.remove(n);
					ls--;
					boucles=0;
				} else
					boucles++;
			}
		}
		if(boucles==0) { // On a trouvé un chemin de chat !
			return deplacements;
		} else { // Il n'y a pas assez de place pour le chemin prévu.
			return null;
		}
	}
	
	// GÉNÉRATION PARCOURS VACHES
	
	private boolean valideCheminRVache(int r, int c, int rf, int cf, int frameMod, int period, Occurrence colibri, int rc, int cc) {
		int s = Integer.signum(cf-c);
		Occurrence vache = new Occurrence(frameMod, period, 18); // Delta = 20-2 car la vitesse d'une vache est de 1/20 case/frame et que l'on peut passer même quand la vache est un peu sur la case.
		while(c>=0 && c<20 && c!=cf+s && Math.abs(carteOrigin[r][c]-7)!=1 && carteOrigin[r][c]<10 && carteOrigin[r][c]%2==0 && !passVaches.isColidingWith(r, c, vache) && !passColibri.isColidingWith(r, c, vache) && !(r==rc && c==cc && colibri.isColidingWith(vache))) {
			frameMod=(frameMod+20)%period;
			vache = new Occurrence(frameMod, period, 18);
			c+=s;
		}
		return c==cf+s;
	}
	
	private boolean valideCheminCVache(int r, int c, int rf, int cf, int frameMod, int period, Occurrence colibri, int rc, int cc) {
		int s = Integer.signum(rf-r);
		Occurrence vache = new Occurrence(frameMod, period, 18); // Delta = 20-2 car la vitesse d'une vache est de 1/20 case/frame et que l'on peut passer même quand la vache est un peu sur la case.
		while(r>=0 && r<12 && r!=rf+s && Math.abs(carteOrigin[r][c]-7)!=1 && carteOrigin[r][c]<10 && carteOrigin[r][c]%2==0 && !passVaches.isColidingWith(r, c, vache) && !passColibri.isColidingWith(r, c, vache) && !(r==rc && c==cc && colibri.isColidingWith(vache))) {
			frameMod=(frameMod+20)%period;
			vache = new Occurrence(frameMod, period, 18);
			r+=s;
		}
		return r==rf+s;
	}
	
	/**
	 * Génère un parcours pour une vache devant passer à (r,c) à l'instant frameDeb.
	 * La longueur du parcours généré sera stocké dans lon, et le colibri se trouvant
	 * en (rColi,cColi) ne sera pas poussé par la vache générée. 
	 * @param r ligne de l'emplacement voulu
	 * @param c colonne de l'emplacement voulu
	 * @param frameDeb instant en frames lors duquel la vache doit se trouver en (r,c)
	 * @param lon longueur (en frames) d'un cycle du parcours
	 * @param rColi ligne du colibri
	 * @param cColi colonne du colibri
	 * @return la liste des checkpoints de la vache si succès, en faisant démarrer la vache
	 * de manière à ce qu'elle arrive en (r,c) à l'instant voulu ; null sinon.
	 */
	private int[][] geneVache(int r, int c, int frameDeb, int[] lon, int rColi, int cColi) {
		int distLig = random.nextInt(6)+1;
		int distCol = random.nextInt(6)+1;
		LinkedList<Integer> posLig = new LinkedList<Integer>(); // Possibilités de déplacement sur les lignes, dont la somme fait 0. Ex : [3,1,2,-1,-1,-3,-1]
		LinkedList<Integer> posCol = new LinkedList<Integer>(); // Idem sur les colonnes.
		fillPosAleat(distLig,posLig,1);
		fillPosAleat(distLig,posLig,-1);
		fillPosAleat(distCol,posCol,1);
		fillPosAleat(distCol,posCol,-1);
		int pof;
		int cs=posCol.size(),ls=posLig.size();
		int dep;
		int n;
		lon[0] = (distLig+distCol)*40;
		int frameMod = frameDeb%lon[0];
		boolean valide;
		int[][] deplacements = new int[cs+ls][2];
		int[] depart = {-1,-1}; // Si le départ ne tombe pas sur un checkpoint, on spécifiera ses coordonnées.
		int shift=0;
		int cpt=0, boucles=0;
		Occurrence colibri = new Occurrence(frameDeb+10, 0, 20); // Création d'une occurence représentant la position du colibri une fois arrêté contre la vache, pour éviter qu'il soit poussé par la vache.
		while((cs!=0 || ls!=0) && boucles<=Math.max(cs+ls , 4)) {
			if(boucles==0) {
				deplacements[cpt][0]=r; deplacements[cpt][1]=c;}
			pof=random.nextInt(2);
			if(pof==0 && cs!=0 || ls==0) { // Déplacement selon une colonne
				n=random.nextInt(cs);
				dep=posCol.get(n);
				valide=valideCheminCVache(r, c, r+dep, c, frameMod, lon[0], colibri, rColi, cColi);
				if(valide) {
					r+=dep;
					frameMod+=20*Math.abs(dep);
					cpt++;
					if(frameMod>lon[0]) {
						frameMod=frameMod%lon[0];
						depart[0]=r-Integer.signum(dep)*frameMod/20;
						depart[1]=c;
						shift=cpt;
					} else if(frameMod==lon[0]) {
						frameMod=0;
						shift=cpt;
					}
					posCol.remove(n);
					cs--;
					boucles=0;
				} else
					boucles++;
			} else { // Déplacement selon une ligne
				n=random.nextInt(ls);
				dep=posLig.get(n);
				valide=valideCheminRVache(r, c, r, c+dep, frameMod, lon[0], colibri, rColi, cColi);
				if(valide) {
					c+=dep;
					frameMod+=20*Math.abs(dep);
					cpt++;
					if(frameMod>lon[0]) {
						frameMod=frameMod%lon[0];
						depart[0]=r;
						depart[1]=c-Integer.signum(dep)*frameMod/20;
						shift=cpt;
					} else if(frameMod==lon[0]) {
						frameMod=0;
						shift=cpt;
					}
					posLig.remove(n);
					ls--;
					boucles=0;
				} else
					boucles++;
			}
		}
		if(boucles==0) { // On a trouvé un chemin de vache !
			int[][] deplacShift;
			int decalage=0;
			if(depart[0]!=-1)
				decalage=1;
			deplacShift = new int[deplacements.length+decalage][];
			if(decalage==1)
				deplacShift[0]=depart;
			for(int i=0; i<deplacements.length; i++) {
				deplacShift[i+decalage]=deplacements[(i+shift)%deplacements.length];
			}
			return deplacShift;
		} else { // Il n'y a pas assez de place pour le chemin prévu.
			return null;
		}
	}

	/**
	 * @param rd la ligne de depart 
	 * @param cd la colonne de depart 
	 * @param rf ligne de fin 
	 * @param cf colonne de fin 
	 * @param s sens deplacement selon la ligne 
	 * @return un chemin
	 */
	public int[] valideCheminR(int rd, int cd, int rf,int cf, int s, int wait){
		double av = cd+0.5;
		int i = cd, rdd=rd;
		double step2 = (wait==0) ? step : 0;
		frame+=wait;
		int frColib;
		passColibri.removeLast(rd,cd); // On supprime l'occurence temporaire qui va être remplacée par la vraie dans la boucle.
		while((rd!=rf || i!=cf) && i+s>=0 && i+s<20 && carteOrigin[rd][i+s]%2==0 && !passVaches.isColidingWith(rd, i+s, new Occurrence(frame+2, 0, 3))){ // L'occurence construite ici n'est qu'une estimation de la prochaine.
			while((int) av==i) {
				step2=Math.min(step2+acc, v_max);
				av+=s*step2;
				frame++;
			}
			i = (int) av;
			chemin[rd][i]+=1;
			frColib=(frame+frameMem)/2;
			passColibri.addOccurrence(rd,i-s,frColib,0,frame-frameMem,0,s); // On ajoute l'occurence de la case qu'on vient de QUITTER.
			frameMem = frame;
			if(random.nextInt(4)==0 && carteOrigin[rd][i]==0){
				carteOrigin[rd][i]=2;
			}
			if(carteOrigin[rd][i]>=10) { //Arc-en-ciel
				passColibri.addOccurrence(rd,i,frColib+1,0,frame-frameMem,0,s);
				int[] dest = rainbows.get(carteOrigin[rd][i]);
				if(dest[0]==rd && dest[1]==i) {
					rd=dest[2]; i=dest[3];
				} else {
					rd=dest[0]; i=dest[1];
				}
				chemin[rd][i]+=1;
				frame++;
				av=i+0.5;
			}
		}
		passColibri.addOccurrence(rd,i,frame+10,0,20,0,s); // Occurrence temporaire (qui sera enlevée avant le prochain déplacement) pour éviter qu'une vache dans le prochain déplacement marche ici.
		if(i!=cd || rd!=rdd) {
			frame += (i!=cf && i+s>=0 && i+s<20 && carteOrigin[rd][i+s]%2==0) ? 1 : 2;
			step=step2;
		} else
			frame-=wait;
		return new int[] {rd,i};
	}

	/**
	 * @param rd  la ligne de depart 
	 * @param cd la colonne de depart 
	 * @param rf ligne de fin 
	 * @param cf colonne de fin 
	 * @param s sens deplacement selon la ligne 
	 * @return un  chemin
	 */
	public int[] valideCheminC(int rd, int cd, int rf, int cf, int s, int wait){
		double av = rd+0.5;
		int i = rd, cdd = cd;
		double step2 = (wait==0) ? step : 0;
		frame+=wait;
		int frColib;
		passColibri.removeLast(rd,cd); // On supprime l'occurence temporaire qui va être remplacée par la vraie dans la boucle.
		while((i!=rf || cd!=cf) && i+s>=0 && i+s<12 && carteOrigin[i+s][cd]%2==0 && !passVaches.isColidingWith(i+s, cd, new Occurrence(frame+2, 0, 3))) { // L'occurence construite ici n'est qu'une estimation de la prochaine.
			while((int) av==i) {
				step2=Math.min(step2+acc, v_max);
				av+=s*step2;
				frame++;
			}
			i = (int) av;
			chemin[i][cd]+=1;
			frColib=(frame+frameMem)/2;
			passColibri.addOccurrence(i-s,cd,frColib,0,frame-frameMem,s,0); // On ajoute l'occurence de la case qu'on vient de QUITTER.
			frameMem = frame;
			if(random.nextInt(4)==0 && carteOrigin[i][cd]==0){
				carteOrigin[i][cd]=2;
			}
			if(carteOrigin[i][cd]>=10) { //Arc-en-ciel
				passColibri.addOccurrence(i,cd,frColib+1,0,frame-frameMem,s,0);
				int[] dest = rainbows.get(carteOrigin[i][cd]);
				if(dest[0]==i && dest[1]==cd) {
					i=dest[2]; cd=dest[3];
				} else {
					i=dest[0]; cd=dest[1];
				}
				chemin[i][cd]+=1;
				frame++;
				av=i+0.5;
			}
		}
		passColibri.addOccurrence(i,cd,frame+10,0,20,s,0); // Occurrence temporaire (qui sera enlevée avant le prochain déplacement) pour éviter qu'une vache dans le prochain déplacement marche ici.
		if(i!=rd || cd!=cdd) {
			frame += (i!=rf && i+s>=0 && i+s<12 && carteOrigin[i+s][cd]%2==0) ? 1 : 2;
			step=step2;
		} else
			frame-=wait;
		return new int[] {i,cd};
	}
	
	private boolean valideCheminCBool(int rd, int cd, int rf, int cf, int s, int[] wait){
		double av = rd+0.5;
		int i = rd, cdd = cd;
		int frame2=frame+wait[0];
		int arc=0;
		double step2 = (wait[0]==0) ? step : 0;
		while((i!=rf || cd!=cf) && i+s>=0 && i+s<12 && carteOrigin[i+s][cd]%2==0 && !passVaches.isColidingWith(i+s, cd, new Occurrence(frame2+2, 0, 3))){
			while((int) av==i) {
				step2=Math.min(step2+acc, v_max);
				av+=s*step2;
				frame2++;
			}
			i = (int) av;
			if(carteOrigin[i][cd]>=10) { //Arc-en-ciel
				int[] dest = rainbows.get(carteOrigin[i][cd]);
				if(dest[0]==i && dest[1]==cd) {
					i=dest[2]; cd=dest[3];
				} else {
					i=dest[0]; cd=dest[1];
				}
				frame2++;
				av=i+0.5;
				if(arc==carteOrigin[i][cd]) { // Pour éviter de tourner en boucle dans des arcs.
					wait[0]=-1;
					break;
				} else if(arc==0) arc=carteOrigin[i][cd];
			}
		}
		// S'assurer que l'on rentre suffisamment dans le milieu de la vache pour être sûr de ne pas la louper, sinon, l'éviter.
		Occurrence occ = passVaches.returnColisionWith(i+s, cd, new Occurrence(frame2+2, 0, 3));
		if(occ!=null) {
			if(occ.difference(frame2+2)>6) {
				if(wait[0]==0)
					wait[0]-=2;
				wait[0]=Math.max(1 , wait[0]+22-((frame2+2)%20)); // 22=20 +2 de marge. Attention : une valeur interdite : 0 (si frColib%20=18) !
				if(wait[0]>=120) { // Pour éviter trop de niveau de récursion, on annule.
					wait[0]=-1;
					return false;
				}
				return valideCheminCBool(rd,cdd,rf,cf,s,wait);
			}
		}
		if(passVaches.isColidingWith(i, cd, new Occurrence(frame2+6,0,4)) || passVaches.isColidingWith(rd, cdd, new Occurrence(frame+wait[0]/2,0,wait[0]/2+1))) { // Permet de détecter si la vache va pousser le colibri à l'arrivée OU si une autre vache va pousser le colibri avant son départ.
			wait[0]=-1;
			return false;
		}
		return i==rf && cd==cf;
	}
	
	private boolean valideCheminRBool(int rd, int cd, int rf,int cf, int s, int[] wait){
		double av = cd+0.5;
		int i = cd, rdd = rd;
		int frame2=frame+wait[0];
		int arc=0;
		double step2 = (wait[0]==0) ? step : 0;
		while((rd!=rf || i!=cf) && i+s>=0 && i+s<20 && carteOrigin[rd][i+s]%2==0 && !passVaches.isColidingWith(rd, i+s, new Occurrence(frame2+2, 0, 3))){
			while((int) av==i) {
				step2=Math.min(step2+acc, v_max);
				av+=s*step2;
				frame2++;
			}
			i = (int) av;
			if(carteOrigin[rd][i]>=10) { //Arc-en-ciel
				int[] dest = rainbows.get(carteOrigin[rd][i]);
				if(dest[0]==rd && dest[1]==i) {
					rd=dest[2]; i=dest[3];
				} else {
					rd=dest[0]; i=dest[1];
				}
				frame2++;
				av=i+0.5;
				if(arc==carteOrigin[rd][i]) { // Pour éviter de tourner en boucle dans des arcs.
					wait[0]=-1;
					break;
				}
				else if(arc==0) arc=carteOrigin[rd][i];
			}
		}
		Occurrence occ = passVaches.returnColisionWith(rd, i+s, new Occurrence(frame2+2, 0, 3));
		if(occ!=null) {
			if(occ.difference(frame2+2)>6) {
				if(wait[0]==0)
					wait[0]-=2;
				wait[0]=Math.max(1 , wait[0]+22-((frame2+2)%20));
				if(wait[0]>=120) {
					wait[0]=-1;
					return false;
				}
				return valideCheminRBool(rdd,cd,rf,cf,s,wait);
			}
		}
		if(passVaches.isColidingWith(rd, i, new Occurrence(frame2+6,0,4)) || passVaches.isColidingWith(rdd, cd, new Occurrence(frame+wait[0]/2,0,wait[0]/2+1))) { // Permet de détecter si la vache va pousser le colibri à l'arrivée OU si une autre vache va pousser le colibri avant son départ.
			wait[0]=-1;
			return false;
		}
		return rd==rf && i==cf;
	}
	
	private void addAnimal(int[][] itin, int lon, Passages pass, LinkedList<int[][]> animaux, int step, int frames, int accel) {
		int ca=0;
		int cpt=0;
		int r=itin[0][0],c=itin[0][1];
		int dr = (int)Math.signum(r-itin[itin.length-1][0]), dc = (int)Math.signum(c-itin[itin.length-1][1]);
		for(int[] m : itin) {
			while(r!=m[0] || c!=m[1]) {
				pass.addOccurrence(r, c, ca*step+cpt*accel, lon, frames, dr, dc);
				dr = (int)Math.signum(m[0]-r);
				dc = (int)Math.signum(m[1]-c);
				ca++;
				r+=dr;
				c+=dc;
			}
			cpt++;
		}
		while(r!=itin[0][0] || c!=itin[0][1]) {
			pass.addOccurrence(r, c, ca*step+cpt*accel, lon, frames, dr, dc);
			dr = (int)Math.signum(itin[0][0]-r);
			dc = (int)Math.signum(itin[0][1]-c);
			ca++;
			r+=dr;
			c+=dc;
		}
		animaux.add(itin);
	}
	
	private int getCoord(int po, int indice, int max) { // Avec i=-1..2, on obtient po+ (-1,0,1,0)
		return Math.min(max,Math.max(0,po+indice%2));
	}
	
	/**
	 * Renvoie la position (ligne OU colonne) du premier arc rencontré en se déplaçant selon direc dans
	 * le sens s.
	 * @param rd
	 * @param cd
	 * @param rf
	 * @param cf
	 * @param direc
	 * @param s
	 * @return
	 */
	private int getNearestArc(int rd, int cd, int rf, int cf, int direc, int s) {
		if(direc==0) { // Selon la colonne
			int rr;
			for(rr=rd; carteOrigin[rr][cd]<10 && (rr!=rf || cd!=cf); rr+=s) {}
			return rr;
		} else { // Selon la ligne
			int cc;
			for(cc=cd; carteOrigin[rd][cc]<10 && (rd!=rf || cc!=cf); cc+=s) {}
			return cc;
		}
	}
	
	/**
	 * Calcule le temps de voyage (en frames) pour aller de coVarDep à coVarFin, selon coFixe(Dep/Fin).
	 * Si on passe dans une paire d'arc-en-ciel, numArcs, direc et s doivent être spécifiés. Sinon, numArcs=0.
	 * @param coVarDep
	 * @param coFixeDep
	 * @param coVarFin
	 * @param coFixeFin
	 * @param numArcs
	 * @param direc
	 * @param s
	 * @return
	 */
	private int tempsVoyage(int coVarDep, int coFixeDep, int coVarFin, int coFixeFin, int numArcs, int direc, int s) {
		if(numArcs!=0) {
			int[] arcPos = rainbows.get(numArcs);
			int arc1, arc2;
			if(coFixeDep==coFixeFin) {
				if(Math.signum(arcPos[direc]-coVarDep)==s && (Math.signum(arcPos[direc+2]-coVarDep)!=s || Math.abs(arcPos[direc]-coVarDep)<Math.abs(arcPos[direc+2]-coVarDep))) {
					arc1 = arcPos[direc];
					arc2 = arcPos[direc+2];
				} else {
					arc2 = arcPos[direc];
					arc1 = arcPos[direc+2];
				}
			} else if(coFixeDep==arcPos[1-direc]) {
				arc1 = arcPos[direc];
				arc2 = arcPos[direc+2];
			} else {
				arc2 = arcPos[direc];
				arc1 = arcPos[direc+2];
			}
			return (Math.abs(coVarFin-arc2)+Math.abs(arc1-coVarDep))*4/3+2;
		} else {
			return Math.abs(coVarFin-coVarDep)*4/3+2;
		}
	}
	
	/**
	 * Fonction principale de génération du niveau aléatoire.
	 * @param n le nombre de mouvements
	 * @param r la ligne de départ
	 * @param c la colonne de départ
	 * @param nbVaches le nombre de vaches
	 * @param nbDyna le nombre de dynamites
	 * @param nCats le nombre de chats
	 * @param nbArcs le nombre d'arc-en-ciels
	 * @param numArcs le numéro de la prochaine paire d'arc-en-ciels à poser.
	 * @return true si la génération a réussi ; false sinon
	 */
	public boolean geneChemin(int n,int r,int c, int nbVaches, int nbDyna, int nCats, int nbArcs, int numArcs){
		int rd=r, cd=c;
		int direc, bord, ran=0, s=1;
		int rf,cf;
		int[] posf;
		int loop=0;
		int[] w = {0};
		int frameDepart;
		int[][] list_vache;
		int stockDyna=0, cote=0, iiMen=0; // Le nombre de dyna à poser ; le nombre de dyna posée mais pas utilisée ; le cote du menhir explosé lorsqu'une dyna est utilisée.
		boolean poseArc=false, dropDyna=false, exploseMen=false; // Indique si une dynamite doit être posée pour le déplacement considéré.
		for (int k=0; k<n && loop<17;k++) { // Pour garantir une proba voulue (pv) de ne pas interrompre un cas encore possible de proba p, il faut une limite de n>=ln(1-pv)/ln(1-p). On a 17 pour p=0.133 et pv=0.9.
			if(DEBUG)
				System.out.println("Fr="+frame+" Pos : "+r+","+c);
			direc=random.nextInt(2); // Choisit si le prochain deplacement se fera selon une ligne ou une colonne.
			bord=random.nextInt(10); // Il y a une probabilité de 1/5 d'aller jusqu'à la cloture du niveau. Cela permet de ne pas trop confiner le chemin au milieu de la carte, et de donner davantage de possibilités de résolution au joueur...
			w[0]=0; frameDepart=frame;
			rf=r; cf=c;
			if(nbDyna>0 && k<n-4 && random.nextInt(n-k-4)<nbDyna) { // Détermine si l'on va poser une dynamite à ce tour.
				progressMin = 16; // Niveau min requis
				dropDyna = true;
				nbDyna--;
			}
			if(exploseMen) {
				carteOrigin[getCoord(r,iiMen,11)][getCoord(c,1-iiMen,19)]-=5; // Dans le cas où le cas précédent n'a pas fonctionné.
				exploseMen=false;
			}
			if(stockDyna!=0 && k<=n-3 && random.nextInt(n-k-2)<stockDyna) { // On veut exploser un menhir
				LinkedList<Integer> menh = new LinkedList<Integer>(); // Recueille les indices ayant des menhirs.
				for(int ii=-1; ii<=2; ii++) {
					if(carteOrigin[getCoord(r,ii,11)][getCoord(c,1-ii,19)]%2==1)
						menh.add(ii);
				}
				if(menh.size()!=0) {
					int ii = menh.get(random.nextInt(menh.size()));
					iiMen=ii;
					cote = (ii%2==0) ? 1-ii/2 : (ii+1)/2; // 0=gauche/haut ; 1=droite/bas
					direc = 1-Math.abs(ii)%2;
					if(bord<2 || cote==0 && (direc==0 && r==1 || direc==1 && c==1) || cote==1 && (direc==0 && r==10 || direc==1 && c==18))
						bord = cote;
					exploseMen = true;
					w[0]=25; // Temps pris par l'explosion de la dynamite
					carteOrigin[getCoord(r,ii,11)][getCoord(c,1-ii,19)]+=5; // 6 ou 8 code un menhir démoli.
					if(DEBUG)
						System.out.println("Explo : "+getCoord(r,ii,11)+" , "+getCoord(c,1-ii,19));
				}
			}
			if(poseArc || nbArcs>0 && k<n-4 && random.nextInt(n-k-4)<nbArcs) { // On veut poser une paire d'arcs-en-ciel
				if(poseArc) { // Dans le cas où le cas précédent n'a pas fonctionné.
					int[] arcPos = rainbows.get(numArcs);
					carteOrigin[arcPos[0]][arcPos[1]]=0;
					carteOrigin[arcPos[2]][arcPos[3]]=0;
				} else {
					nbArcs--;
				}
				int dir,rr,cc,nloop=0;
				int[] wa={0};
				boolean bon=false;
				do {
					wa[0]=0;
					dir=(exploseMen) ? direc : random.nextInt(2);
					if(dir==0) {
						rr=(exploseMen) ? cote*r+random.nextInt((cote==0)?r:12-r) : random.nextInt(12);
						cc=c;
						s=Integer.signum(rr-r);
					} else {
						rr=r;
						cc=(exploseMen) ? cote*c+random.nextInt((cote==0)?c:20-c) : random.nextInt(20);
						s=Integer.signum(cc-c);
					}
					if(s!=0 && chemin[rr][cc]==0 && carteOrigin[rr][cc]==0 && passVaches.getOccurrences(rr, cc)==null)
						bon=(dir==0) ? valideCheminCBool(r,c,rr,cc,s,wa) : valideCheminRBool(r,c,rr,cc,s,wa);
				} while(nloop++<20 && !bon);
				if(nloop<20) { // On a trouvé un emplacement pour le premier arc.
					poseArc=true;
					carteOrigin[rr][cc]=numArcs;
					int[] arcPos = new int[] {rr,cc,0,0};
					if(DEBUG)
						System.out.println("ARC 1 : "+rr+" , "+cc);
					direc=dir;
					cote=(s+1)/2;
					do { // On cherche l'emplacement du second arc.
						rr=random.nextInt(12);
						cc=random.nextInt(20);
					} while(rr==r && cc==c || chemin[rr][cc]!=0 || carteOrigin[rr][cc]!=0 || passVaches.getOccurrences(rr, cc)!=null);
					carteOrigin[rr][cc]=numArcs;
					arcPos[2]=rr; arcPos[3]=cc;
					if(DEBUG)
						System.out.println("ARC 2 : "+rr+" , "+cc);
					rainbows.put(numArcs, arcPos);
					if(bord<2 || cote==0 && (direc==0 && rr<=1 || direc==1 && cc<=1) || cote==1 && (direc==0 && rr>=10 || direc==1 && cc>=18))
						bord = cote;
					rf=rr; cf=cc;
					if(bord>=2) {
						if(direc==0)
							ran=1+cote*rr+random.nextInt((cote==0)?rr-1:10-rr);
						else
							ran=1+cote*cc+random.nextInt((cote==0)?cc-1:18-cc);
					}
				} else
					poseArc=false;
			}
			if (direc==0) { // SELON LA COLONNE
				if (bord<2) {
					ran=bord*11;
					s=2*bord-1;
					valideCheminCBool(r,c,ran,cf,s,w);
					if(w[0]!=-1) posf=valideCheminC(r,c,ran,cf,s,w[0]);
					else posf=new int[] {r,c};
				} else {
					if(!poseArc) {
						if(!exploseMen) ran=1+random.nextInt(10); // on tire une position sur la ligne (hors bords)
						else ran = 1+cote*r+random.nextInt((cote==0)?r-1:10-r); // On tire une position du côté où le colibri vient de casser le menhir.
						s=Integer.signum(ran-r); // sens du déplacement selon la ligne s=1 ou -1
					}
					if((ran==r && cf==c) || Math.abs(carteOrigin[ran+s][cf]-7)==1 || carteOrigin[ran+s][cf]>=10 || chemin[ran+s][cf]>6 || (ran+s==rd && cf==cd)) // il faut en particulier vérifier que l'emplacement du menhir d'arrêt (aux coord (ran+s,c)) ne bloque pas le chemin précédemment généré ! Pour cela : si chemin[ran+s][c]==0 c'est bon; si ==1 on met une fleur magique, car elle deviendra menhir après le passage; si >=2 (i.e. le colibri est déjà passé plus de 2 fois sur cette case) ce n'est pas possible, il faut choisir un autre emplacement !
						posf=new int[] {r,c}; // Destination (ran,c) non valide
					else if(chemin[ran+s][cf]>1 || carteOrigin[ran+s][cf]==4 || passVaches.getOccurrences(ran+s, cf)!=null || nbVaches>0 && random.nextInt(n-k)<nbVaches) { // Ajout d'une vache !
						if(nbVaches>0) {
							nbVaches--;
							//int tvoy=Math.abs(ran-r)*4/3+2;
							int tvoy=tempsVoyage(r,c,ran,cf,poseArc ? numArcs : 0,direc,s);
							int frameDeb = ((frame+w[0]+tvoy)/20+1)*20;
							w[0] += frameDeb-frame-w[0]-tvoy;
							int wait=w[0];
							if(valideCheminCBool(r,c,ran,cf,s,w)) { // On peut accéder à la destination, on va mettre un vache.
								int boucles=0;
								int[] lon={0};
								list_vache=null;
								while(list_vache==null && boucles<10) {
									list_vache = geneVache(ran+s, cf, frameDeb+w[0]-wait, lon, ran, cf);
									boucles++;
								}
								if(list_vache==null)
									posf=new int[] {r,c};
								else {
									posf=valideCheminC(r,c,ran,cf,s,w[0]);
									frame--; // Rectification, la détection d'une vache se fait en une frame ou deux de moins qu'un menhir..
									addAnimal(list_vache,lon[0],passVaches,vaches,20,18,0);
								}
							}
							else { // On s'arrête avant sur un obstacle.
								w[0]=exploseMen?25:0;
								if(valideCheminCBool(r,c,ran,cf,s,w) || w[0]==-1)
									posf=new int[] {r,c};
								else
									posf=valideCheminC(r,c,ran,cf,s,w[0]);
							}
						} else { // pVaches==false
							posf=new int[] {r,c};
						}
					}
					else {
						valideCheminCBool(r,c,ran,cf,s,w);
						if(w[0]!=-1) posf=valideCheminC(r,c,ran,cf,s,w[0]); // On obtient la destination prévue "ran" sauf si un menhir se trouvait sur le chemin entre (r,c) et (ran,c) : dans ce cas, on obtient la ligne avant le menhir qui nous bloque.
						else posf=new int[] {r,c};
					}
				}
				if (posf[0]==r && posf[1]==c) { // si finalement on a pas bougé, on refait un tour de boucle en plus (ou si une vache nous pousse).
					k--;
					loop++;
				} else { // sinon, on a donc un mouvement valide de prévu, la nouvelle position du colibri est donc (rf,c)
					if(exploseMen) {
						// On pointe vers le menhir à détruire, puis on ajoute {0,0,0} pour utiliser une dynamite
						solution[k][0]=0;
						solution[k][1]=s;
						k+=2;
						exploseMen=false;
						stockDyna--;
						frameDepart++;
					}
					solution[k][0]=0;
					solution[k][1]=s;
					solution[k][2]=(w[0]==0) ? 0 : frameDepart+w[0];
					if(dropDyna) {
						int a,b,cp=0;
						int ligArc1 = getNearestArc(r, c, posf[0], posf[1], direc, s);
						int ligArc2 = getNearestArc(posf[0], posf[1], r, c, direc, -s);
						int d1 = Math.abs(ligArc1-r)+1;
						int d2 = Math.abs(posf[0]-ligArc2)+1;
						do {
							// a=Math.min(r, posf[0])+random.nextInt(Math.abs(posf[0]-r)+1);
							if(random.nextInt(d1+d2)<d1) {
								a = Math.min(r, ligArc1)+random.nextInt(d1);
								b = c;
							} else {
								a = Math.min(posf[0], ligArc2)+random.nextInt(d2);
								b = posf[1];
							}
						} while((chemin[a][b]==0 || carteOrigin[a][b]==4 || carteOrigin[a][b]>=10 || Math.abs(carteOrigin[a][b]-7)==1) && cp++<15);
						if(cp<=15) {
							carteOrigin[a][b]=4;
							if(DEBUG)
								System.out.println("POSE DYNA : "+a+" , "+b);
							dropDyna=false;
							stockDyna++;
						}
					}
					if(poseArc) {
						int[] arcPos = rainbows.get(numArcs);
						if(chemin[arcPos[0]][arcPos[1]]>0) {
							numArcs+=2;
							poseArc=false;
						}
					}
					r=posf[0]; c=posf[1];
					loop=0;
					if (r==ran && c==cf && ran!=0 && ran!=11 && passVaches.getOccurrences(ran+s, c)==null) {
						carteOrigin[ran+s][c]=1+2*chemin[ran+s][c]; // On ajoute le menhir d'arrêt, ou bien la fleur magique d'arrêt. (NB : menhirs codés par "1" dans la carte, fleurs par "2", fleurs magiques par "3")
					}
				}
			} else {// SELON LA LIGNE  (il s'agit de la même chose mais le déplacement se fait sur la ligne)				
				if (bord<2) {
					ran=bord*19;
					s=2*bord-1;
					valideCheminRBool(r,c,rf,ran,s,w);
					if(w[0]!=-1) posf=valideCheminR(r,c,rf,ran,s,w[0]);
					else posf=new int[] {r,c};
				} else {
					if(!poseArc) {
						if(!exploseMen) ran=random.nextInt(18)+1;
						else ran = 1+cote*c+random.nextInt((cote==0)?c-1:18-c);
						s=Integer.signum(ran-c);
					}
					if((rf==r && ran==c) || Math.abs(carteOrigin[rf][ran+s]-7)==1 || carteOrigin[rf][ran+s]>=10 || chemin[rf][ran+s]>6 || (ran+s==cd && rf==rd))
						posf=new int[] {r,c};
					else if(chemin[rf][ran+s]>1 || carteOrigin[rf][ran+s]==4 || passVaches.getOccurrences(rf, ran+s)!=null || nbVaches>0 && random.nextInt(n-k)<nbVaches) {
						if(nbVaches>0) {
							nbVaches--;
							//int tvoy=Math.abs(ran-c)*4/3+2;
							int tvoy=tempsVoyage(c,r,ran,rf,poseArc ? numArcs : 0,direc,s);
							int frameDeb = ((frame+w[0]+tvoy)/20+1)*20;
							w[0] += frameDeb-frame-w[0]-tvoy;
							int wait=w[0];
							if(valideCheminRBool(r,c,rf,ran,s,w)) {
								int boucles=0;
								int[] lon={0};
								list_vache=null;
								while(list_vache==null && boucles<10) {
									list_vache = geneVache(rf, ran+s, frameDeb+w[0]-wait, lon, rf, ran);
									boucles++;
								}
								if(list_vache==null)
									posf=new int[] {r,c};
								else {
									posf=valideCheminR(r,c,rf,ran,s,w[0]);
									frame--;
									addAnimal(list_vache,lon[0],passVaches,vaches,20,18,0);
								}
							}
							else {
								w[0]=exploseMen?25:0;
								if(valideCheminRBool(r,c,rf,ran,s,w) || w[0]==-1)
									posf=new int[] {r,c};
								else
									posf=valideCheminR(r,c,rf,ran,s,w[0]);
							}
						} else {
							posf=new int[] {r,c};
						}
					}
					else {
						valideCheminRBool(r,c,rf,ran,s,w);
						if(w[0]!=-1) posf=valideCheminR(r,c,rf,ran,s,w[0]);
						else posf=new int[] {r,c};
					}
				}
				if (posf[0]==r && posf[1]==c) {
					k--;
					loop++;
				} else {
					if(exploseMen) {
						solution[k][0]=s;
						solution[k][1]=0;
						k+=2;
						exploseMen=false;
						stockDyna--;
						frameDepart++;
					}
					solution[k][0]=s;
					solution[k][1]=0;
					solution[k][2]=(w[0]==0) ? 0 : frameDepart+w[0];
					if(dropDyna) {
						int a,b,cp=0;
						int colArc1 = getNearestArc(r, c, posf[0], posf[1], direc, s);
						int colArc2 = getNearestArc(posf[0], posf[1], r, c, direc, -s);
						int d1 = Math.abs(colArc1-c)+1;
						int d2 = Math.abs(posf[1]-colArc2)+1;
						do {
							//a=Math.min(c, posf[1])+random.nextInt(Math.abs(posf[1]-c)+1);
							if(random.nextInt(d1+d2)<d1) {
								a = r;
								b = Math.min(c, colArc1)+random.nextInt(d1);
							} else {
								a = posf[0];
								b = Math.min(posf[1], colArc2)+random.nextInt(d2);
							}
						} while((chemin[a][b]==0 || carteOrigin[a][b]==4 || carteOrigin[a][b]>=10 || Math.abs(carteOrigin[a][b]-7)==1) && cp++<15);
						if(cp<=15) {
							carteOrigin[a][b]=4;
							if(DEBUG)
								System.out.println("POSE DYNA : "+a+" , "+b);
							dropDyna=false;
							stockDyna++;
						}
					}
					if(poseArc) {
						int[] arcPos = rainbows.get(numArcs);
						if(chemin[arcPos[0]][arcPos[1]]>0) {
							numArcs+=2;
							poseArc=false;
						}
					}
					c=posf[1]; r=posf[0];
					loop=0;
					if (c==ran && r==rf && ran!=0 && ran!=19 && passVaches.getOccurrences(r, ran+s)==null) {
						carteOrigin[r][ran+s]=1+2*chemin[r][ran+s];
					}
				}
			}
		}
		if(loop!=0)
			return false;
		if(carteOrigin[r][c]==0)
			carteOrigin[r][c]=2;
		if(nDyna!=0) { // On change les menhirs détruits (6) ou (8) en menhirs (1) ou fleurs magiques (3)
			for(int a=0; a<12; a++) {
				for(int b=0; b<20; b++) {
					if(Math.abs(carteOrigin[a][b]-7)==1)
						carteOrigin[a][b]-=5;
				}
			}
		}
		passColibri.addOccurrence(rd, cd, 0, 0, 75, 0, 0); // Pour éviter de se faire bouffer dans les 3 premières secondes !
		if(nChats!=0) { // On ajoute des chats si voulu.
			for(int i=0; i<nCats; i++) {
				int lcat=random.nextInt(12), ccat=random.nextInt(20);
				while(carteOrigin[lcat][ccat]%2==1) {
					lcat=random.nextInt(12);
					ccat=random.nextInt(20);
				}
				int boucles=0;
				int[] lon={0};
				list_vache=null; // ~=list_chat, je ne fais que réutiliser la référence.
				while(list_vache==null && boucles<10) {
					list_vache = geneChat(lcat, ccat, lon);
					boucles++;
				}
				if(list_vache==null)
					i--;
				else {
					addAnimal(list_vache,lon[0],passChats,chats,4,6,2);
				}
			}
		}
		progressMin = Math.max(progressMin, numArcs>10 ? 23 : (chats.size()>0 ? 21 : (vaches.size()>0 ? 9 : 0)));
		return true;
	}
	
	private int casesParcourues() {
		int cases=0;
		for(int i=0; i<chemin.length; i++) {
			for(int j=0; j<chemin[0].length; j++) {
				if(chemin[i][j]!=0)
					cases++;
			}
		}
		return cases;
	}
	
	/**
	 * Génère un niveau aléatoire ! La longueur de la solution du niveau sera : lon+rand[0:var].
	 *  @param lon longueur minimale de la solution générée
	 *  @param var intervalle de variation aléaoire de la longueur du chemin
	 *  @param base indice indiquant si l'on utilise le générateur de base en menhirs
	 */
	public void geneNivRand(int lon, int var, int base, long graine){
		random = new Random();
		seed=graine;
		System.out.println("SEED : "+seed);
		random.setSeed(seed);
		int nbVaches, nbDyna, nCats, nbArcs;
		progressMin = 0;
		nbVaches = random.nextInt(1+nVaches);
		nbDyna = random.nextInt(1+nDyna);
		nCats = random.nextInt(1+nChats);
		nbArcs = random.nextInt(1+nArcs);
		int longueur = lon+random.nextInt(var)+nbDyna*2;
		frame = 0; frameMem = 0;
		step = 0.;
		v_max = 0.7501;
		acc = 0.1;
		solution = new int[longueur][3]; //Liste des mouvements à effectuer pour résoudre le niveau
		passColibri = new Passages(20);
		passVaches = new Passages(20);
		passChats = new Passages(20);
		rainbows = new SparseArray<int[]>();
		int numArcs;
		if(base>0) {
			BaseNiveau bn = new BaseNiveau(random, carteOrigin, rainbows, nbArcs);
			int[] res = bn.generate(random.nextInt(5),random.nextInt(3),random.nextInt(3),1+random.nextInt(4));
			//System.out.println(bn.toString());
			db_l = res[0];
			db_c = res[1];
			numArcs = res[2];
			nbArcs = nbArcs-rainbows.size();
		} else {
			db_l = random.nextInt(12);
			db_c = random.nextInt(20); // on tire un emplacement départ
			numArcs = 10;
		}
		chemin[db_l][db_c]=1;
		if(!geneChemin(longueur,db_l,db_c,nbVaches,nbDyna,nCats,nbArcs,numArcs) || casesParcourues()<15) { // on génère la carte pour une solution en "longueur" coups !
			// Si la génération s'est retrouvée dans une impasse ou si le niveau généré est trop simple (coincé dans un secteur)
			init();
			geneNivRand(lon,var,base,random.nextLong());
		}
		computePresenceRainbows();
		experience = solution.length*(10+solution.length/4)+vaches.size()*20+nbDyna*15+chats.size()*40+nbArcs*30;
	}

}