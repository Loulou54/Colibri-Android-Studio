package com.game.colibri;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.TreeSet;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.util.SparseArray;
import android.util.SparseIntArray;

import com.game.colibri.Niveau.Occurrence;

/**
 * Le solveur de niveau basé sur A* aussi utilisé pour les indications de jeu.
 * 
 * @author Louis
 *
 */
public class Solver extends AsyncTask<Integer, LinkedList<Solver.Move>, Solver.Path> {
	
	private static final boolean DEBUG = false;
	public final static int STEPS_VACHES = 20; // 20 ticks par case
	public final static int STEPS_CHATS = 4; // 4 ticks par case
	public final static int STEPS_COLIBRI_ACC = 4; // lorsque à l'arrêt, 2 ticks de switch delay + 4 ticks pour sortir d'une case
	public final static int STEPS_COLIBRI_FULL = 2; // lancé full speed
	public final static int DYNA_EXPL_TIME = 23; // Ticks avant fin explosion dynamite
	
	public static Solver instance; // Instance en cours de cette classe singleton.
	
	public boolean OPT_TIME = true; // Optimisation du temps (true) ou du nombre de coups (false)
	private Niveau niv; // Le niveau
	private SolverInterface solverInterface; // La view de visualisation de la solution
	public int sol_r=-1, sol_c=-1; // La position pour laquelle a été calculée "solution".
	private Heuristic heuristic; // La structure de données pour un calcul rapide de l'heuristique.
	private PriorityQueue<State> openSet;
	private HashMap<Integer, Integer> collectiblesIndexes; // Pour une clé r*COL+c, retourne l'index de l'élément ramassable dans l'ordre allant de (0,0) à (ROW,COL) de gauche à droite.
	private LinkedList<Niveau.Occurrence> emptyCell = new LinkedList<Niveau.Occurrence>(); // Utilisé par DynaGrid.obstacle pour les cases sans obstacles.
	
	public interface SolverInterface {
		public static final int CANCELLED=0, NO_SOLUTION=1, OUT_OF_MEMORY=2;
		public void preExecute();
		public void progressUpdate(int r, int c, LinkedList<Move> moves);
		public void cancel(int reason);
		public void result(int r, int c, Path path);
	}

	/**
	 * Constructeur du Solveur pour le niveau niv. Il s'agit d'une AsyncTask que l'on peut exécuter sans bloquer l'UI.
	 * @param niv le niveau en question, dans l'état courant
	 * @param solverInterface les fonctions permettant de communiquer le progrès ou les résultats
	 */
	public Solver(Niveau niv, SolverInterface solverInterface) {
		this.niv = niv;
		this.solverInterface = solverInterface;
		heuristic = new Heuristic(niv);
		if(instance!=null)
			instance.cancel(true);
		instance = this;
	}
	
	/**
	 * Affiche le PathViewer.
	 */
	@Override
	protected void onPreExecute() {
		solverInterface.preExecute();
		System.gc();
		super.onPreExecute();
	}
	
	/**
	 * Cherche la solution du niveau. Fournir les int suivants en paramètres.
	 * @param p frame, r, c, nFleurs, nDynas, opt_time (1 ou 0)
	 * @return le Path solution
	 */
	@Override
	protected Path doInBackground(Integer... p) {
		return getSolution(p[0], p[1], p[2], p[3], p[4], p[5]!=0);
	}
	
	/**
	 * Affiche la meilleure solution actuelle.
	 * @param values la liste des mouvements de la meilleure solution actuelle
	 */
	@SafeVarargs
	@Override
	protected final void onProgressUpdate(LinkedList<Solver.Move>... values) {
		solverInterface.progressUpdate(sol_r, sol_c, values[0]);
		super.onProgressUpdate(values);
	}
	
	@Override
	protected void onCancelled() {
		solverInterface.cancel(SolverInterface.CANCELLED);
		if(instance==this) // Dans le cas où cette instance a été remplacée par une nouvelle, ne pas supprimer la nouvelle
			instance = null;
		super.onCancelled();
	}
	
	@Override
	protected void onPostExecute(Path result) {
		System.out.println("Moves : "+result.length+" ; t_cumul : "+result.t_cumul);
		if(result.length>0) {
			solverInterface.result(sol_r, sol_c, result);
		} else {
			solverInterface.cancel(result.t_cumul == 0 ? SolverInterface.NO_SOLUTION : SolverInterface.OUT_OF_MEMORY);
		}
		instance = null;
		super.onPostExecute(result);
	}
	
	@SuppressLint("UseSparseArrays")
	private void buildCollectibleIndexes() {
		collectiblesIndexes = new HashMap<Integer, Integer>(100);
		int index=0;
		for(int r=0; r<Position.ROW; r++) {
			for(int c=0; c<Position.COL; c++) {
				int e = niv.carte[r][c];
				if(e>=2 && e<=4)
					collectiblesIndexes.put(r*Position.COL + c, index++);
			}
		}
	}
	
	/**
	 * Pour pos_index valant r*COL+c, retourne l'index de l'élément ramassable dans
	 * la carte dans l'ordre allant de (0,0) à (ROW,COL), de gauche à droite.
	 * @param pos_index r*COL+c
	 * @return l'index de l'élément ramassable, OU null s'il ne s'agit pas d'un élément ramassable.
	 */
	public Integer getIndexOfCollectible(int pos_index) {
		return collectiblesIndexes.get(pos_index);
	}
	
	/**
	 * Retourne la solution à partir de la position (r,c), ayant nFleurs à ramasser et
	 * nDynas en stock. Si elle est invalide, on la calcule.
	 * @param frame l'instant actuel en frames
	 * @param r ligne
	 * @param c colonne
	 * @param nFleurs le nombre de fleurs à ramasser
	 * @param nDynas le nombre de dynamites en stock
	 * @param opt_time spécifie si l'on désire l'optimisation du temps ou des mouvements
	 * @return solution null si impossible
	 */
	private Path getSolution(int frame, int r, int c, int nFleurs, int nDynas, boolean opt_time) {
		sol_r=r;
		sol_c=c;
		OPT_TIME = opt_time;
		buildCollectibleIndexes();
		heuristic.prepareRootState();
		Path solution;
		try {
			solution = findSolution(frame, r, c, nFleurs, nDynas);
		} catch(CancelledSolutionRequest e) {
			solution = new Path(0);
		}
		if(DEBUG) {
			System.out.println("direction, wait, travel, rf, cf");
			System.out.println("Temps : "+(solution.t_cumul - frame)+" ; Moves : "+solution.length);
		}
		return solution;
	}
	
	/**
	 * Algorithme de recherche de solution basé sur A*.
	 * @param frame l'instant actuel en frames
	 * @param rd ligne de départ
	 * @param cd colonne de départ
	 * @param nFleurs nombre de fleurs restantes
	 * @param nDynas nombre de dynamites en stock
	 */
	@SuppressWarnings("unchecked")
	private Path findSolution(int frame, int rd, int cd, int nFleurs, int nDynas) {
		final HashSet<Hash> closedSet = new HashSet<Hash>(); // Contient les hash des états déjà visités.
		openSet = new PriorityQueue<State>(); // Contient les états à la frontière.
		long startTime = System.currentTimeMillis();
		int h_param_lim = niv.h_param!=-1 ? niv.h_param : (int)Math.ceil((60.-nFleurs)/6.);
		
		openSet.add(new State(frame, rd, cd, nFleurs, nDynas));
		int n = 0;
		final Runtime runtime = Runtime.getRuntime();
		while(!openSet.isEmpty() && !isCancelled()) {
			State current = openSet.poll();
			if(n == 800) { // Paramétrization de l'heuristique: approxime davantage si la recherche devient trop longue.
				n++;
				if(heuristic.h_param < h_param_lim) {
					heuristic.h_param += 2;
					n -= 1501;
				}
			}
			if(current.goal_reached) { // <=> h=0 : FIN !
				System.out.println("Itérations : "+(n + heuristic.h_param/2*1500)+"  |  h_param : "+heuristic.h_param+"  |  openSet : "+openSet.size()+"  |  closedSet : "+closedSet.size());
				System.out.println("Time (ms) : "+(System.currentTimeMillis()-startTime));
				return current.path;
			}
			if(closedSet.add(current.hash)) { // Ajoute l'état dans le closedSet, si déjà présent, false.
				current.computeNeighbors();
				for(State s : current.voisins) {
					if(!closedSet.contains(s.hash)) {
						s.computeScores();
						openSet.add(s);
					}
				}
				n++;
				if(n % 256==0) // Update the progress on the PathViewer
					publishProgress(current.path.getMoves());
			}
			// Prevent possible Out Of Memory Error
			double memoryOccupation = ((double)(runtime.totalMemory() - runtime.freeMemory()))/((double) runtime.maxMemory());
			if(memoryOccupation > 0.975) {
				System.out.println("OUT OF MEMORY!");
				return new Path(-1);
			}
		}
		return new Path(0);
	}
	
	
	// Structures

	private class CancelledSolutionRequest extends RuntimeException {
		public CancelledSolutionRequest() {
			super();
		}
	}
	
	private static class Hash {
		// Chaque bit représente l'état des objets ramassables (1=ramassé), dans l'ordre de leur disposition.
		private BitSet collectiblesState;
		// Positions du colibri (byte 0) + des dynas posées OU explosées dans l'ordre de leur disposition (byte 1-6) + état explosé (1) ou non (0) (byte 7 > bits 0-5)
		private long coliDynaPositions = 0;
		
		public Hash() {
			collectiblesState = new BitSet();
		}
		
		public Hash(Hash h) {
			collectiblesState = (BitSet)h.collectiblesState.clone();
			coliDynaPositions = h.coliDynaPositions;
		}
		
		@Override
		public int hashCode() {
			return collectiblesState.hashCode();
		}
		
		@Override
		public boolean equals(Object o) {
			Hash h = (Hash)o;
			return collectiblesState.equals(h.collectiblesState) && coliDynaPositions==h.coliDynaPositions;
		}
		
		/**
		 * "Ramasse" l'élément à index.
		 * @param index l'index de l'élément tel que retourné par getIndexOfCollectible(pos_index).
		 */
		public void pick(int index) {
			collectiblesState.set(index);
		}
		
		/**
		 * Détermine si l'élément à l'index spécifié a été ramassé.
		 * @param index l'index de l'élément tel que retourné par getIndexOfCollectible(pos_index).
		 * @return true=ramassé ; false=non ramassé
		 */
		public boolean hasBeenPicked(int index) {
			return collectiblesState.get(index);
		}
		
		/**
		 * Sets the value of the hash concerning the positions of the colibri and dynamites
		 * and the state of the dynamites.
		 * @param value a long containing 8 bytes with: [0:pos_colibri][1-6:pos_dyna][7:exploded_flag_bits]
		 */
		public void setColiDynaPositions(long value) {
			coliDynaPositions = value;
		}
	}
	
	private class State implements Comparable<State> {
		private State parent; // L'état parent, null si root.
		private Position pos; // Position du colibri dans cet état.
		private Hash hash; // Identifie un état : pos_coli+grid.hash
		private DynaGrid grid; // Grille du niveau avec les changements.
		private Path path; // Les déplacements jusqu'ici.
		private LinkedList<State> voisins; // Les prochaines destinations possibles
		private IntervalsModuloHandler possibPresence; // Les intervalles possibles de présence sur cette case.
		private int f_score_t, f_score_m; // f_score: time & moves (f coût total départ->arrivée, g coût parcouru, h estimation heuristique : f=g+h)
		private boolean goal_reached = false; // true si cet état est final (h==0)
		
		/**
		 * Constructeur pour l'état racine, partant de la position (r,c).
		 * @param frame l'instant actuel en frames
		 * @param r ligne
		 * @param c colonne
		 * @param nFleurs fleurs restantes
		 * @param nDynas dynamites restantes
		 */
		public State(int frame, int r, int c, int nFleurs, int nDynas) {
			parent = null;
			path = new Path(frame);
			grid = new DynaGrid(nFleurs, nDynas);
			pos = new Position(r, c, 0, path.t_cumul);
			voisins = new LinkedList<State>();
			possibPresence = grid.getFreeIntervals(pos, 0, STEPS_COLIBRI_ACC);
			computeHash();
			computeScores();
		}
		
		/**
		 * Constructeur pour un état voisin obtenu à partir de s et du mouvement m.
		 * @param s l'état d'origine
		 * @param m le mouvement à effectuer
		 */
		public State(State s, Move m) {
			parent = s;
			path = new Path(s.path, m);
			grid = new DynaGrid(s.grid, s.pos, m, s.path.t_cumul);
			pos = new Position(m.posFinale.r, m.posFinale.c, m.step, path.t_cumul);
			voisins = new LinkedList<State>();
			possibPresence = grid.getFreeIntervals(pos, 0, STEPS_COLIBRI_ACC);
			computeHash();
		}
		
		@Override
		public String toString() {
			return pos.r+","+pos.c+","+path.length+": f_t="+f_score_t+", f_m="+f_score_m;
		}
		
		/**
		 * Calcule le hash représentant cet état : position du colibri et état de la grille.
		 */
		private void computeHash() {
			hash = grid.getHash((byte) (pos.r*Position.COL + pos.c), path.t_cumul);
		}
		
		/**
		 * Calcule l'heuristique et le score total.
		 */
		public void computeScores() {
			heuristic.compute(pos, grid);
			if(heuristic.h_m==0)
				goal_reached = true;
			f_score_m = path.length + heuristic.h_m;
			f_score_t = path.t_cumul + heuristic.h_t + f_score_m*8;
		}
		
		@Override
		public int compareTo(State s2) {
			if(OPT_TIME) {
				int df = f_score_t - s2.f_score_t;
				return df!=0 ? df : f_score_m - s2.f_score_m;
			} else {
				int df = f_score_m - s2.f_score_m;
				return df!=0 ? df : f_score_t - s2.f_score_t;
			}
		}
		
		/**
		 * Calcule et ajoute les voisins à la liste.
		 */
		public void computeNeighbors() {
			// Les quatre directions
			for(int i=1; i<=4; i++) {
				LinkedList<Move> pMoves = possibleMovesInDirection(i);
				for(Move m : pMoves) {
					voisins.add(new State(this, m));
				}
			}
		}
		
		private LinkedList<Move> possibleMovesInDirection(int dir) {
			LinkedList<Move> pMoves = new LinkedList<Move>();
			Position tempPos = new Position(pos);
			LinkedList<Niveau.Occurrence> cell = grid.obstacle(tempPos.step(dir)); // null=obstacle fixe, sinon, occurrences des vaches.
			// Dynamite
			if(cell==null) { // On est bloqué : si menhir (rouge), dynamite ?
				int cellVal = grid.getCell(tempPos);
				if((cellVal==1 || cellVal==5) && grid.nDynas>0)
					addMoveDynamite(pMoves, dir);
				return pMoves;
			}
			// Poussée de vache
			LinkedList<Niveau.Occurrence> occs = grid.getVachesPushingTo(pos,dir);
			if(!occs.isEmpty()) {
				addMovePushedByVache(pMoves, dir, occs, null);
			}
			// Déplacements
			int nFleurs = grid.nFleurs; // Permet d'arrêter un déplacement si on a ramassé toutes les fleurs
			boolean looped = false;
			cell = grid.addCatsObstacles(tempPos, cell); // Pour ajouter les chats comme obstacles au déplacement
			if(tempPos.simul(cell, niv, grid))
				nFleurs--;
			cell = grid.obstacle(tempPos.step(dir));
			int steps = 0; // Pour détecter une boucle infinie entre deux arcs-en-ciel.
			while(cell!=null && nFleurs > 0 && steps < 2*Position.COL) {
				if(tempPos.equals(pos))
					looped = true;
				if(!cell.isEmpty() && cell.getFirst().mod!=Integer.MAX_VALUE) // Arrêt contre une vache (et non une dynamite qui va exploser)
					addMoveToVache(pMoves, tempPos.prev(dir), dir, cell, null);
				cell = grid.addCatsObstacles(tempPos, cell);
				if(tempPos.simul(cell, niv, grid) && !looped)
					nFleurs--;
				cell = grid.obstacle(tempPos.step(dir), looped);
				steps++;
			}
			if(cell==null || nFleurs==0) // Arrêt contre menhir ou dernière fleur
				addMoveToMenhir(pMoves, tempPos.prev(dir), dir, null);
			return pMoves;
		}
		
		private void addMoveDynamite(LinkedList<Move> pMoves, int dir) {
			pMoves.add(new Move(10+dir, 0, (path.move!=null && path.move.direction==dir ? 1 : 3), path.move!=null ? path.move.step : 0, pos));
		}
		
		private void addMovePushedByVache(LinkedList<Move> pMoves, int dir, LinkedList<Niveau.Occurrence> occs, IntervalsModulo arrivVoulue) {
			Position dest = pos.next(dir);
			dest.simulPushVache(niv, grid);
			IntervalsModuloHandler possibIntervals = grid.getFreeIntervals(dest, STEPS_VACHES, STEPS_VACHES-2);
			possibIntervals.intersectArrivee(arrivVoulue, STEPS_VACHES, STEPS_VACHES);
			possibIntervals.intersectOccArrivee(occs, path.t_cumul, STEPS_VACHES, STEPS_VACHES);
			addMoveWithin(pMoves, dest, -dir, possibIntervals);
		}
		
		private void addMoveToVache(LinkedList<Move> pMoves, Position dest, int dir, LinkedList<Niveau.Occurrence> occs, IntervalsModulo arrivVoulue) {
			IntervalsModuloHandler possibIntervals = dest.getDepartsIntervals(); // Instants où le départ est possible
			possibIntervals.intersectArrivee(arrivVoulue, dest.t_travel[0], dest.t_travel[1]); // Instant où le départ est possible ET l'arrivée voulue
			possibIntervals.intersectOccArrivee(occs, path.t_cumul, dest.t_travel[0], dest.t_travel[1]);
			addMoveWithin(pMoves, dest, dir, possibIntervals);
			//int k = Math.ceil((path.t_cumul+t_travel-v.r)/v.mod);
			//int wait = v.r + k*v.mod - path.t_cumul - t_travel;
		}
		
		private void addMoveToMenhir(LinkedList<Move> pMoves, Position dest, int dir, IntervalsModulo arrivVoulue) {
			IntervalsModuloHandler possibIntervals = dest.getDepartsIntervals(); // Instants où le départ est possible
			possibIntervals.intersectArrivee(arrivVoulue, dest.t_travel[0], dest.t_travel[1]); // Instant où le départ est possible ET l'arrivée voulue
			addMoveWithin(pMoves, dest, dir, possibIntervals);
		}
		
		private void addMoveWithin(LinkedList<Move> pMoves, Position dest, int dir, IntervalsModuloHandler possibIntervals) {
			int depart = possibIntervals.getFirstPossib();
			if(depart==Integer.MAX_VALUE) { // Voyage impossible...
				return;
			} else if(depart > 0 && possibPresence.firstClose() - 2 < depart) { // attente impossible (+2 pour prendre en compte deux frames supplémentaires d'accélération après un wait)
				if(parent==null)
					return;
				// Appel au parent pour venir plus tard
				parent.createDelayedMoveTo(path.move, possibPresence.getIntervalsForDepartureIn(possibIntervals));
			} else { // OK !
				int stoppedIndex = depart==0 ? 0 : 1;
				pMoves.add(new Move(dir, depart, dest.t_travel[stoppedIndex], dest.step[stoppedIndex], dest));
			}
		}
		
		/**
		 * Crée un état tel que l'instant d'arrivée du Move m soit inclus dans les intervals arrivVoulue.
		 * (Appelé par un fils.)
		 * @param m le mouvement à effectuer
		 * @param arrivVoulue les intervals d'arrivée voulus
		 */
		protected void createDelayedMoveTo(Move m, IntervalsModulo arrivVoulue) {
			arrivVoulue.translate(m.wait+m.travel); // Pour replacer l'origine de cet IntervalsModulo au même que cet état
			LinkedList<Move> pMoves = new LinkedList<Move>();
			if(m.direction>10) { // Pose de dynamite
				if(parent!=null)
					parent.createDelayedMoveTo(path.move, arrivVoulue);
			} else if(m.direction<0) { // Poussée de vache
				LinkedList<Niveau.Occurrence> occs = grid.getVachesPushingTo(m.posFinale,-m.direction);
				addMovePushedByVache(pMoves, -m.direction, occs, arrivVoulue);
			} else {
				LinkedList<Niveau.Occurrence> cell = grid.obstacle(m.posFinale.nextSimplePos(m.direction));
				if(cell==null)
					addMoveToMenhir(pMoves, m.posFinale, m.direction, arrivVoulue);
				else
					addMoveToVache(pMoves, m.posFinale, m.direction, cell, arrivVoulue);
			}
			if(pMoves.isEmpty())
				return;
			State s = new State(this, pMoves.getFirst());
			s.hash.setColiDynaPositions(-path.t_cumul);; // Pour rendre cet état unique
			voisins.add(s);
			s.computeScores();
			openSet.add(s);
		}
	}
	
	private class DynaGrid {
		public Hash hash; // Représente l'état de la grille i.e., les modifications par rapport à niv.
		public SparseIntArray dynamites; // Les dynamites posées à r*col+c, et l'instant de dépos.
		public byte[] flowersBySegment; // Dénote le nombre de fleurs non ramassées par segment de heuristic.minSegments
		public int nFleurs, nDynas; // Fleurs restantes et dynamites en stock
		
		/**
		 * Constructeur de la grille d'origine correspondant au point de départ.
		 * @param nF fleurs restantes
		 * @param nD dynamites en stock
		 */
		public DynaGrid(int nF, int nD) {
			nFleurs = nF;
			nDynas = nD;
			hash = new Hash();
			dynamites = new SparseIntArray();
			flowersBySegment = new byte[heuristic.minSegments.size()];
			int i = 0;
			for(Heuristic.Segment s : heuristic.minSegments) {
				flowersBySegment[i++] = s.nFleurs;
			}
			// Pour le cas où un objet est présent là où se trouve initialement le colibri
			int cell = niv.carte[sol_r][sol_c];
			if(cell==2 || cell==3) { // fleur ou fleur magique
				int index = sol_r*Position.COL + sol_c;
				hash.pick(getIndexOfCollectible(index));
				nFleurs--;
				flowersBySegment[heuristic.fleurToSegment.get(index)]--;
			} else if(cell==4) { // Dynamite
				hash.pick(getIndexOfCollectible(sol_r*Position.COL + sol_c));
				nDynas++;
			}
		}

		/**
		 * Constructeur d'une grille copie de dg suivie d'un mouvement m.
		 * @param dg grille d'origine
		 * @param p_init position initiale dans la grille
		 * @param m mouvement à effectuer
		 * @param t_cumul l'instant t
		 */
		public DynaGrid(DynaGrid dg, Position p_init, Move m, int t_cumul) {
			nFleurs = dg.nFleurs;
			nDynas = dg.nDynas;
			hash = new Hash(dg.hash);
			dynamites = dg.dynamites.clone();
			flowersBySegment = dg.flowersBySegment.clone();
			performMove(p_init, m, t_cumul);
		}

		/**
		 * Effectue le mouvement m sur la carte courante et modifie les données en relation.
		 * (hash, flowersBySegment, nFleurs, nDynas, ...)
		 * @param p_init position initiale
		 * @param m le mouvement à effectuer
		 * @param t_cumul l'instant t
		 */
		private void performMove(Position p_init, Move m, int t_cumul) {
			SimplePos p = new SimplePos(p_init);
			int cell;
			while(p.r!=m.posFinale.r || p.c!=m.posFinale.c) {
				p.step(m.direction);
				cell = getCell(p);
				if(cell==2 || cell==3) { // fleur ou fleur magique
					int index = p.r*Position.COL + p.c;
					hash.pick(getIndexOfCollectible(index));
					nFleurs--;
					flowersBySegment[heuristic.fleurToSegment.get(index)]--;
				} else if(cell==4) { // Dynamite
					hash.pick(getIndexOfCollectible(p.r*Position.COL + p.c));
					nDynas++;
				} else if(cell>=10) { // Arc-en-ciel
					int[] dest = niv.rainbows.get(cell);
					if(dest[0]==p.r && dest[1]==p.c) {
						p.r=dest[2]; p.c=dest[3];
					} else {
						p.r=dest[0]; p.c=dest[1];
					}
				}
			}
			if(m.direction > 10) { // Pose de dynamite
				p.step(m.direction - 10);
				dynamites.put(p.r*Position.COL + p.c, -t_cumul-m.travel);
				nDynas--;
			}
		}
		
		/**
		 * Calcule et retourne le hash représentant les changements (donc l'état) de cette DynaGrid.
		 * @param pos_coli la position du colibri dans le State correspondant
		 * @param t_cumul l'instant présent en frames
		 * @return le hash de la grille et de la position du colibri
		 */
		public Hash getHash(int pos_coli, int t_cumul) {
			long coliDynaPositions = pos_coli;
			int dynaExplodedState = 0;
			int ndyna = dynamites.size();
			for(int d=0; d < ndyna; d++) {
				int k = dynamites.keyAt(d), v = dynamites.valueAt(d);
				if(v<0) {
					if(-v+DYNA_EXPL_TIME < t_cumul) { // Menhir déjà explosé
						dynamites.put(k, 0);
						//v = 0;
						continue;
					}
				}
				coliDynaPositions = (coliDynaPositions << 8) | k;
				dynaExplodedState = (dynaExplodedState << 1) | (v==0 ? 1 : 0);
			}
			hash.setColiDynaPositions((coliDynaPositions << 8) | dynaExplodedState);
			return hash;
		}
		
		/**
		 * Retourne le contenu de la cellule à la position pos, prenant en compte
		 * les transformations effectuées à la carte.
		 * @param pos la position
		 * @return contenu de la case
		 */
		public int getCell(SimplePos pos) {
			// Dans le cas d'un restart, niv.carte change avant la fin de l'exécution de doInBackground et getCell peut donner des résultats incohérents
			if(isCancelled()) {
				throw new CancelledSolutionRequest();
			}
			if(pos.isOut())
				return -1;
			int origCell = niv.carte[pos.r][pos.c];
			if(origCell==0) {
				return 0;
			} else if(origCell==1 || origCell==5) {
				return dynamites.get(pos.r*Position.COL + pos.c, 1);
			} else {
				Integer index = getIndexOfCollectible(pos.r*Position.COL + pos.c);
				if(index!=null && hash.hasBeenPicked(index)) {
					if(origCell==3) {
						return dynamites.get(pos.r*Position.COL + pos.c, 1);
					} else {
						return 0;
					}
				} else {
					return origCell;
				}
			}
		}
		
		/**
		 * Retourne les intervalles où la case est non occupée, en rétractant les intervalles de Delta.
		 * @param pos la position sur la grille
		 * @param shift décalage à ajouter aux occurrences
		 * @param delta le temps de présence de l'animal en question (colibri) à prendre en compte
		 * @return IntervalsModuloHandler correspondant
		 */
		public IntervalsModuloHandler getFreeIntervals(Position pos, int shift, int delta) {
			LinkedList<Niveau.Occurrence> occs = null;
			LinkedList<Niveau.Occurrence> vaches = niv.passVaches.getOccurrences(pos.r, pos.c);
			LinkedList<Niveau.Occurrence> chats = niv.passChats.getOccurrences(pos.r, pos.c);
			if(vaches!=null && chats!=null) {
				occs = new LinkedList<Niveau.Occurrence>();
				occs.addAll(vaches);
				occs.addAll(chats);
			}
			else if(vaches!=null)
				occs = vaches;
			else if(chats!=null)
				occs = chats;
			else
				return new IntervalsModuloHandler(true);
			IntervalsModuloBuilder imb = new IntervalsModuloBuilder(true);
			imb.addOccurrences(occs, pos.t_cumul+shift, delta, false);
			return imb.getHandler();
		}

		/**
		 * Retourne l'obstacle présent à la position pos : null pour un obstacle
		 * fixe (menhir ou bord) ; la liste des occurrences des vaches sinon.
		 * @param pos la position dans la grille
		 * @return liste d'occurrences ou null
		 */
		public LinkedList<Niveau.Occurrence> obstacle(SimplePos pos) {
			return obstacle(pos, false);
		}
		
		/**
		 * Retourne l'obstacle présent à la position pos : null pour un obstacle
		 * fixe (menhir ou bord) ; la liste des occurrences des vaches sinon.
		 * @param pos la position dans la grille
		 * @param looped considère une fleur magique comme un menhir (dans une boucle d'arc-en-ciel)
		 * @return liste d'occurrences ou null
		 */
		public LinkedList<Niveau.Occurrence> obstacle(SimplePos pos, boolean looped) {
			int cell = getCell(pos);
			if(pos.isOut() || cell==1 || cell==5 || looped && cell==3) { // Obstacle fixe : bord ou menhir ou menhir rouge
				return null;
			} else if(cell < 0) { // Dynamite posée à t = -cell
				LinkedList<Niveau.Occurrence> dyna_ex = new LinkedList<Niveau.Occurrence>();
				dyna_ex.add(new Niveau.Occurrence(-cell, 0, DYNA_EXPL_TIME));
				return dyna_ex;
			} else { // Occurrences de vaches (possiblement vides)
				LinkedList<Niveau.Occurrence> occs = niv.passVaches.getOccurrences(pos.r,  pos.c);
				return occs==null ? emptyCell : occs;
			}
		}
		
		/**
		 * Ajoute les occurrences des chats aux occurrences de cell.
		 * @param pos la position sur la carte
		 * @param cell les occurrences sur cette case auquels ajouter les chats
		 */
		public LinkedList<Occurrence> addCatsObstacles(Position pos, LinkedList<Occurrence> cell) {
			LinkedList<Niveau.Occurrence> chats = niv.passChats.getOccurrences(pos.r, pos.c);
			if(chats==null) // Pas de chats
				return cell;
			if(cell.isEmpty()) // Pas de vache
				return chats;
			// Dans le cas où il y a des vaches ET des chats sur la même case, on crée une nouvelle instance qui rassemble toutes les occurrences.
			LinkedList<Occurrence> allOccs = new LinkedList<Occurrence>(cell);
			allOccs.addAll(chats);
			return allOccs;
		}
		
		/**
		 * Retourne l'ensemble des occurrences de vaches à pos poussant
		 * (en entrant sur la case) dans la direction dir.
		 * @param pos la position
		 * @param dir la direction de poussée
		 * @return les occurrences
		 */
		public LinkedList<Niveau.Occurrence> getVachesPushingTo(Position pos, int dir) {
			LinkedList<Niveau.Occurrence> vaches = niv.passVaches.getOccurrences(pos.r, pos.c);
			if(vaches==null)
				return emptyCell;
			LinkedList<Niveau.Occurrence> res = new LinkedList<Niveau.Occurrence>();
			for(Niveau.Occurrence v : vaches) {
				if(v.dir_in==dir)
					res.add(v);
			}
			return res;
		}
	}
	
	public static class Path {
		public Move move;
		public Path prevPath;
		public int length;
		public int t_cumul;
		
		/**
		 * Constructeur de chemin initial, vide.
		 * @param frames l'instant actuel en frames
		 */
		public Path(int frames) {
			move = null;
			prevPath = null;
			length = 0;
			t_cumul = frames;
		}
		
		/**
		 * Constructeur d'un chemin copie de p, suivi du mouvement m.
		 * @param p le chemin auquel ajouter m
		 * @param m le mouvement suivant
		 */
		public Path(Path p, Move m) {
			move = m;
			prevPath = p;
			length = p.length;
			if(m.direction >= 0) // On ne compte pas les poussées de vache comme Move.
				length++;
			t_cumul = p.t_cumul + m.wait + m.travel;
		}
		
		public boolean isEmpty() {
			return move==null;
		}

		/**
		 * Retourne la liste de Moves.
		 * @return liste de Moves de ce Path dans l'ordre chronologique
		 */
		public LinkedList<Move> getMoves() {
			return getMoves(true);
		}

		/**
		 * Retourne la liste de Moves.
		 * @param firstCall true si cet appel à getMoves est le premier de la suite récursive.
		 * @return liste de Moves de ce Path dans l'ordre chronologique
		 */
		public LinkedList<Move> getMoves(boolean firstCall) {
			if(move==null)
				return new LinkedList<Move>();
			LinkedList<Move> moves = prevPath.getMoves(false);
			addMoveAndDecomposeRainbows(moves, move, firstCall);
			return moves;
		}

		/**
		 * Ajoute le move m à la liste en le décomposant en plusieurs moves s'il
		 * passe à travers des arcs-en-ciel. (Uniquement pour la visualisation
		 * de la solution dans PathViewer)
		 * @param moves liste des mouvements à laquelle ajouter m
		 * @param m le mouvement à ajouter et potentiellement décomposer
		 * @param lastMove true s'il s'agit du dernier mouvement
		 */
		private void addMoveAndDecomposeRainbows(LinkedList<Move> moves, Move m, boolean lastMove) {
			if(m.direction>=10) {
				moves.add(m);
				return;
			}
			SimplePos stopPos = m.posFinale.nextSimplePos(m.direction);
			if(!stopPos.isOut() && Solver.instance.niv.carte[stopPos.r][stopPos.c]%2!=1 && !lastMove) // Arrêt contre vache : on copie m pour mettre s à -1 pour signaler PathViewer
				m = new Move(m.direction, m.wait, m.travel, -1, m.posFinale);
			SimplePos lastP = moves.isEmpty() ? new SimplePos(Solver.instance.sol_r, Solver.instance.sol_c) : moves.getLast().posFinale;
			boolean onRow = m.direction==Move.LEFT || m.direction==Move.RIGHT;
			Position p = new Position(lastP.r, lastP.c, 0, 0); // position initiale
			outerloop:
			while(Solver.instance.niv.presenceRainbows[onRow ? Position.COL + p.r : p.c]) {
				do {
					if(p.equals(m.posFinale)) // Arrivé !
						break outerloop;
					p.step(m.direction);
				} while(Solver.instance.niv.carte[p.r][p.c]<10);
				// Ajoute Move intermédiaire
				moves.add(new Move(m.direction, 0, 0, 0, p));
				int[] arcPair = Solver.instance.niv.rainbows.get(Solver.instance.niv.carte[p.r][p.c]);
				if(p.r==arcPair[0] && p.c==arcPair[1])
					p = new Position(arcPair[2], arcPair[3], 0, 0);
				else
					p = new Position(arcPair[0], arcPair[1], 0, 0);
				boolean fin = p.equals(m.posFinale);
				moves.add(new Move(m.direction, -1, fin?-1:0, fin?m.step:0, new Position(p))); // wait==-1 pour signaler PathViewer un moveTo au lieu d'un cubicTo ; travel==-1 s'il n'y a pas de Move après l'arc.
				if(fin)
					return;
			}
			moves.add(m);
		}
		
		/**
		 * Retourne une liste de mouvements interprétables par le jeu.
		 * (dx, dy, wait)
		 * @return une liste de triplets (dirX, dirY, wait_absolute)
		 */
		public LinkedList<int[]> getGamesMoves() {
			if(move==null)
				return new LinkedList<int[]>();
			else {
				LinkedList<int[]> gMoves = prevPath.getGamesMoves();
				if(move.direction>=10) { // move supplémentaire en cas de dyna pour se diriger vers le bon menhir.
					int prevDir = prevPath.isEmpty() ? -1 : prevPath.move.direction;
					if(prevDir!=move.direction-10) {
						move.direction-=10;
						gMoves.add(move.getGameMove(prevPath.t_cumul, false));
						move.direction+=10;
					}
				}
				if(move.direction >= 0) // Ne pas ajouter les poussées de vache.
					gMoves.add(move.getGameMove(prevPath.t_cumul, prevPath.move!=null && prevPath.move.direction < 0));
				if(DEBUG)
					System.out.println(move.toString()+" : end_frame="+t_cumul);
				return gMoves;
			}
		}
		
		@Override
		public String toString() {
			return length > 0 ? prevPath.toString()+move.toString() : "";
		}
	}
	
	public static class Move {
		public static final int UP=1,RIGHT=2,LEFT=3,DOWN=4;
		
		public int direction;
		public int wait;
		public int travel;
		public double step;
		public Position posFinale; // La position d'arrivée, contenant notamment les departsPossibles
		
		public Move(int d, int w, int t, double s, Position pf) {
			direction = d;
			wait = w;
			travel = t;
			step = s;
			posFinale = pf;
		}
		
		/**
		 * Retourne un triplet correspondant à un move dans le jeu.
		 * @param t_cumul le temps cumulé jusqu'à ce move
		 * @param after_push vrai si après une poussée de vache
		 * @return le triplet correspondant (dx, dy, wait_absolute)
		 */
		public int[] getGameMove(int t_cumul, boolean after_push) {
			return new int[] {
					direction==LEFT ? -1 : (direction==RIGHT ? 1 : 0),
					direction==UP ? -1 : (direction==DOWN ? 1 : 0),
					wait==0 && !after_push ? 0 : t_cumul + wait
			};
		}
		
		@Override
		public String toString() {
			return direction+" "+wait+" "+travel+" "+posFinale.toString()+"\n";
		}
	}
	
	/**
	 * Une classe basique qui n'opère que sur (row, column) d'une position.
	 */
	public static class SimplePos {
		public static final int ROW=12, COL=20;
		
		public int r;
		public int c;
		
		public SimplePos(int r, int c) {
			this.r = r;
			this.c = c;
		}
		
		public SimplePos(SimplePos p) {
			r = p.r;
			c = p.c;
		}
		
		@Override
		public String toString() {
			return "("+r+","+c+")";
		}
		
		public boolean isOut() {
			return r<0 || r>=ROW || c<0 || c>=COL;
		}
		
		public boolean equals(SimplePos p) {
			return r==p.r && c==p.c;
		}
		
		/**
		 * Distance de Manhattan entre this et p.
		 * @param p l'autre position
		 * @return la distance de Manhattan
		 */
		public int distance(SimplePos p) {
			return Math.abs(r-p.r) + Math.abs(c-p.c);
		}
		
		/**
		 * Fait progresser la position d'un pas vers la direction dir
		 * et retourne l'objet modifié.
		 * @param dir la direction
		 * @return this après modification
		 */
		public SimplePos step(int dir) {
			switch(Math.abs(dir)) {
			case Move.UP:
				r--;
				break;
			case Move.RIGHT:
				c++;
				break;
			case Move.LEFT:
				c--;
				break;
			case Move.DOWN:
				r++;
			}
			return this;
		}
		
		public SimplePos nextSimplePos(int dir) {
			SimplePos p = new SimplePos(this);
			p.step(dir);
			return p;
		}
	}
	
	/**
	 * Une classe plus complexe qui gère les déplacements du colibri et les contraintes liées.
	 * (État présent de la carte, instants des départs possibles, dynamique du colibri, ...)
	 * Hérite de SimplePos.
	 */
	public static class Position extends SimplePos {
		private static final double v_max = 0.7501;
		private static final double acc = 0.1;
		/* Variables de simulation pour déterminer le temps de déplacement dans le cas
		   vitesse conservée (0) ou vitesse réinitialisée (1) */
		private double[] dep; // Le déplacement effectué dans le moteur physique
		private double[] step; // La vitesse (le pas par itéreation)
		private int[] t_travel; // Le nombre de frames écoulées pour effectuer le déplacement
		private int t_cumul;
		private IntervalsModuloBuilder departsPossibles; // Les intervals de départ possibles pour faire ce déplacement
		
		public Position(int r, int c, double currentStep, int t_cumul) {
			super(r,c);
			dep = new double[] {0.5, 0.5};
			step = new double[] {currentStep, 0};
			t_travel = new int[] {2, 1}; // Init : temps de commutation entre deux coups.
			this.t_cumul = t_cumul;
			departsPossibles = null;
		}
		
		public Position(Position p) {
			super(p);
			dep = p.dep.clone();
			step = p.step.clone();
			t_travel = p.t_travel.clone();
			t_cumul = p.t_cumul;
			if(p.departsPossibles!=null)
				departsPossibles = new IntervalsModuloBuilder(p.departsPossibles);
			else
				departsPossibles = null;
		}
		
		@Override
		public String toString() {
			return super.toString()+" : "+(departsPossibles==null ? "[]" : departsPossibles.intervalsPerMod.toString());
		}
		
		public IntervalsModuloHandler getDepartsIntervals() {
			return departsPossibles==null ? new IntervalsModuloHandler(true) : departsPossibles.getHandler();
		}
		
		/**
		 * Simule un pas dans le moteur physique pour déterminer le temps de voyage.
		 * @param occs les occurrences de la prochaine case à ajouter dans departsPossibles
		 * @param niv référence vers le niveau en cours pour avoir les correspondances d'arc-en-ciel
		 * @param grid la DynaGrid pour obtenir la cellule courante
		 * @return true si une fleur a été ramassée
		 */
		public boolean simul(LinkedList<Niveau.Occurrence> occs, Niveau niv, DynaGrid grid) {
			// Effectue le pas
			int [] t_travel = {0, 0};
			for(int i=0; i<2; i++) {
				int n = (int) dep[i];
				boolean limitReached = false;
				do {
					if(!limitReached) {
						step[i] += acc;
						if(step[i] > v_max) {
							step[i] = v_max;
							limitReached = true;
						}
					}
					dep[i] += step[i];
					t_travel[i]++;
				} while(n == (int) dep[i]);
			}
			int cell = grid.getCell(this);
			if(cell>=10) { // Arc-en-ciel
				int[] dest = niv.rainbows.get(cell);
				if(dest[0]==r && dest[1]==c) {
					r=dest[2]; c=dest[3];
				} else {
					r=dest[0]; c=dest[1];
				}
				t_travel[0]++;
				t_travel[1]++;
				dep[0] = 0.5;
				dep[1] = 0.5;
			}
			// Ajoute les occurrences de la prochaine case aux departsPossibles
			if(occs.size()!=0) {
				if(departsPossibles==null)
					departsPossibles = new IntervalsModuloBuilder(true);
				departsPossibles.addOccurrences(occs, t_cumul+this.t_travel[1], (int)Math.ceil(1/step[1]/2), false);
				departsPossibles.evaluateT0(occs, t_cumul+this.t_travel[0], (int)Math.ceil(1/step[0]/2), false);
			}
			this.t_travel[0] += t_travel[0];
			this.t_travel[1] += t_travel[1];
			return cell==2 || cell==3;
		}
		
		/**
		 * Simule une poussée de vache dans le moteur physique pour déterminer le temps de
		 * voyage et la téléportation par arc-en-ciel.
		 * @param niv référence vers le niveau en cours pour avoir les correspondances d'arc-en-ciel
		 * @param grid la DynaGrid pour obtenir la cellule courante
		 */
		public void simulPushVache(Niveau niv, DynaGrid grid) {
			t_travel[0] = STEPS_VACHES;
			t_travel[1] = STEPS_VACHES;
			step[0] = 0;
			int cell = grid.getCell(this);
			if(cell>=10) { // Arc-en-ciel
				int[] dest = niv.rainbows.get(cell);
				if(dest[0]==r && dest[1]==c) {
					r=dest[2]; c=dest[3];
				} else {
					r=dest[0]; c=dest[1];
				}
				dep[0] = 0.5;
				dep[1] = 0.5;
			}
		}
		
		/**
		 * Nouvel objet Position après un pas dans la direction dir.
		 * @param dir la direction
		 * @return nouvelle position
		 */
		public Position next(int dir) {
			Position p = new Position(this);
			p.step(dir);
			return p;
		}
		
		/**
		 * Nouvel objet Position après un pas dans la direction opposée à dir.
		 * @param dir la direction
		 * @return nouvelle position
		 */
		public Position prev(int dir) {
			Position p = new Position(this);
			p.step(5-dir);
			return p;
		}
	}
	
	public static class Bound implements Comparable<Bound> {
		public int value; // The value of the bound
		public boolean open; // true if [value, ...[ ; false if [..., value[
		
		public Bound(int v, boolean o) {
			value = v;
			open = o;
		}
		
		@Override
		public int compareTo(Bound b) {
			return value - b.value;
		}
		
		public boolean equals(Bound b) {
			return value == b.value;
		}
		
		@Override
		public String toString() {
			return value+" "+open;
		}
	}
	
	/**
	 * Cette classe permet de construire un set d'IntervalModulo de différentes
	 * périodes à partir d'occurrences. On peut ensuite en extraire un IntervalsModuloHandler
	 * pour effectuer d'autres opérations.
	 * @author Louis
	 *
	 */
	public static class IntervalsModuloBuilder {
		public SparseArray<IntervalsModulo> intervalsPerMod; // Map un modulo à un IntervalsModulo
		public boolean state_t0;
		
		public IntervalsModuloBuilder(boolean full) {
			intervalsPerMod = new SparseArray<IntervalsModulo>();
			state_t0 = full;
		}
		
		public IntervalsModuloBuilder(IntervalsModuloBuilder imb) {
			int len = imb.intervalsPerMod.size();
			intervalsPerMod = new SparseArray<IntervalsModulo>(len);
			for(int i=0; i<len; i++) {
				intervalsPerMod.put(imb.intervalsPerMod.keyAt(i), new IntervalsModulo(imb.intervalsPerMod.valueAt(i)));
			}
			state_t0 = imb.state_t0;
		}
		
		/**
		 * Évalue l'état du départ à t=0 (sans perte de vitesse). S'il y a collision
		 * avec l'une des Occurrences dans occs à t_instant avec un temps de présence
		 * delta, state_t0 est mis à possib.
		 * @param occs les occurrences
		 * @param t_instant l'instant t
		 * @param delta le delta de présence
		 * @param possib l'état à attribuer à state_t0 en cas de collision
		 */
		public void evaluateT0(LinkedList<Niveau.Occurrence> occs, int t_instant, int delta, boolean possib) {
			if(state_t0==possib)
				return;
			Niveau.Occurrence coli = new Niveau.Occurrence(t_instant, 0, delta);
			for(Niveau.Occurrence o : occs) {
				if(coli.isColidingWith(o)) {
					state_t0 = possib;
					break;
				}
			}
		}
		
		/**
		 * Insère l'état possib pour le temps des occurrences occs, rapportées à l'instant
		 * t_instant et élargies de delta.
		 * @param occs les occurrences
		 * @param t_instant l'instant t
		 * @param delta le delta de temps à ajouter
		 * @param possib l'état à attribuer pour le temps de présence des occurrences
		 */
		public void addOccurrences(LinkedList<Niveau.Occurrence> occs, int t_instant, int delta, boolean possib) {
			for(Niveau.Occurrence o : occs) {
				IntervalsModulo im = intervalsPerMod.get(o.mod);
				if(im==null) {
					im = new IntervalsModulo(o.mod, !possib);
					intervalsPerMod.put(o.mod, im);
				}
				im.addOccurrence(o, t_instant, delta, possib);
			}
		}
		
		public IntervalsModuloHandler getHandler() {
			return new IntervalsModuloHandler(this);
		}
	}
	
	/**
	 * Cette classe permet de manipuler des IntervalsModulo, notamment pour y ajouter
	 * des intervalles à intersecter, et appeler les fonctions firstClose et
	 * getFirstPossib.
	 * @author Louis
	 *
	 */
	public static class IntervalsModuloHandler {
		public int ppcmMod; // L'hyper période des IntervalsModulo périodiques.
		public int lastSporadicEvent; // Le dernier instant des événements sporadiques.
		public ArrayList<IntervalsModulo> intervalsSet;
		public boolean state_t0;
		
		public IntervalsModuloHandler(boolean full) {
			ppcmMod = 1;
			lastSporadicEvent = 0;
			intervalsSet = null;
			state_t0 = full;
		}
		
		public IntervalsModuloHandler(IntervalsModuloBuilder imb) {
			ppcmMod = 1;
			lastSporadicEvent = 0;
			state_t0 = imb.state_t0;
			int len = imb.intervalsPerMod.size();
			if(len==0) {
				intervalsSet = null;
				return;
			}
			intervalsSet = new ArrayList<IntervalsModulo>(len);
			for(int i = 0; i < len; i++) {
				addIntervalsModulo(imb.intervalsPerMod.valueAt(i));
			}
		}
		
		/**
		 * Retourne la prochaine borne ouvrante à partir de from_t de l'intersection des
		 * IntervalsModulo de ce handler.
		 * @param from_t l'instant à partir duquel chercher
		 * @return le prochain instant d'ouverture après from_t
		 */
		public int getNextOpen(int from_t) {
			if(intervalsSet==null)
				return from_t;
			Bound nextOpen = new Bound(from_t, true);
			int i = 0, posCount = 0, len = intervalsSet.size(), time_limit = from_t+ppcmMod;
			if(from_t < lastSporadicEvent)
				time_limit+=lastSporadicEvent;
			while(posCount < len && nextOpen.value <= time_limit) {
				int fp = intervalsSet.get(i).getNextState(true, nextOpen);
				if(fp==nextOpen.value) {
					posCount++;
				} else {
					posCount = 1;
					nextOpen.value = fp;
				}
				i++;
				if(i==len)
					i = 0;
			}
			return nextOpen.value <= time_limit ? nextOpen.value : Integer.MAX_VALUE;
		}
		
		/**
		 * Retourne la prochaine borne fermante à partir de from_t de l'intersection des
		 * IntervalsModulo de ce handler.
		 * @param from_t l'instant à partir duquel chercher
		 * @return le prochain instant de fermeture après from_t
		 */
		public int getNextClose(int from_t) {
			if(intervalsSet==null)
				return Integer.MAX_VALUE;
			Bound nextClose = new Bound(from_t, true);
			int minClose = Integer.MAX_VALUE;
			int tMin;
			for(IntervalsModulo im : intervalsSet) {
				tMin = im.getNextState(false, nextClose);
				if(tMin < minClose)
					minClose = tMin;
			}
			return minClose;
		}
		
		public int getFirstPossib() {
			if(state_t0)
				return 0;
			int o = getNextOpen(1);
			return o;
		}
		
		public int firstClose() {
			return getNextClose(0);
		}
		
		private boolean isInstantValid(int t) {
			if(intervalsSet==null)
				return true;
			int len = intervalsSet.size();
			for(int i = 0; i < len; i++) {
				if(!intervalsSet.get(i).isInstantValid(t))
					return false;
			}
			return true;
		}
		
		public void addIntervalsModulo(IntervalsModulo im) {
			if(im.isFull())
				return;
			if(intervalsSet==null)
				intervalsSet = new ArrayList<Solver.IntervalsModulo>();
			if(im.mod==Integer.MAX_VALUE)
				lastSporadicEvent = Math.max(lastSporadicEvent, im.intervals.last().value);
			else
				ppcmMod = IntervalsModulo.lcm(ppcmMod, im.mod);
			intervalsSet.add(im);
		}
		
		public void addIntervalsModulo(IntervalsModuloHandler imh) {
			if(intervalsSet==null)
				intervalsSet = new ArrayList<Solver.IntervalsModulo>();
			ppcmMod = IntervalsModulo.lcm(ppcmMod, imh.ppcmMod);
			lastSporadicEvent = Math.max(lastSporadicEvent, imh.lastSporadicEvent);
			if(imh.intervalsSet!=null)
				intervalsSet.addAll(imh.intervalsSet);
		}
		
		/**
		 * Intersectionne arrivVoulue avec this.intervals en translatant arrivVoulue
		 * de t_travel dans le cas t==0 et t!=0.
		 * @param arrivVoulue intervals voulus après t_travel ATTENTION: affecté par la fonction
		 * @param t_travelVitesseCons t_travel dans le cas départ à t==0
		 * @param t_travelVitesse0 t_travel dans le cas départ à t!=0
		 */
		public void intersectArrivee(IntervalsModulo arrivVoulue, int t_travelVitesseCons, int t_travelVitesse0) {
			if(arrivVoulue==null)
				return;
			if(!arrivVoulue.isInstantValid(t_travelVitesseCons))
				state_t0 = false;
			arrivVoulue.translate(-t_travelVitesse0);
			addIntervalsModulo(arrivVoulue);
		}
		
		/**
		 * Intersecte l'IntervalsModulo avec les instants de présence des occs.
		 * @param occs les occurrences
		 * @param t_instant instant de référence de l'IntervalModulo
		 * @param t_travelVitesseCons t_travel dans le cas départ à t==0
		 * @param t_travelVitesse0 t_travel dans le cas départ à t!=0
		 */
		public void intersectOccArrivee(LinkedList<Niveau.Occurrence> occs, int t_instant, int t_travelVitesseCons, int t_travelVitesse0) {
			IntervalsModuloBuilder occsIntervals = new IntervalsModuloBuilder(false);
			occsIntervals.addOccurrences(occs, t_instant + t_travelVitesse0, -3*STEPS_VACHES/4, true);
			IntervalsModuloHandler imh = occsIntervals.getHandler();
			// Cas t==0 (conservation vitesse)
			if(state_t0 && !imh.isInstantValid(t_travelVitesseCons - t_travelVitesse0))
				state_t0 = false;
			addIntervalsModulo(imh);
		}
		
		/**
		 * Crée un IntervalsModulo décrivant les instants d'arrivée voulus sur cette case,
		 * permettant de partir dans possibDeparture.
		 * Dans ce contexte, this décrit les intervalles de présence possibles sur la case,
		 * et possibDeparture décrit les intervalles de départ possibles.
		 * Le résultat est inclu dans this, mais chaque intervalle doit être temporellement
		 * connexe à un intervalle de possibDeparture.
		 * @param possibDeparture les instants de départ possibles
		 * @return les instants d'arrivée voulus sur la case courante
		 */
		public IntervalsModulo getIntervalsForDepartureIn(IntervalsModuloHandler possibDeparture) {
			int ppcm = IntervalsModulo.lcm(ppcmMod, possibDeparture.ppcmMod); // Définition de la nouvelle période commune
			if(lastSporadicEvent!=0 || possibDeparture.lastSporadicEvent!=0)
				ppcm*=2; // Pour prendre en compte les événements sporadiques, on étend à ppcm*2.
			if(ppcm > 3600) // On limite l'étendue du ppcm..
				ppcm = 3600;
			IntervalsModulo arrivVoulues = new IntervalsModulo(ppcm, false);
			int tOpen = getNextOpen(0), tClose;
			if(tOpen==0) {
				tOpen = getNextOpen(getNextClose(0));
				tClose = getNextClose(tOpen);
			} else {
				tClose = getNextClose(tOpen);
			}
			for(; tOpen <= ppcm; tOpen=getNextOpen(tClose), tClose=getNextClose(tOpen)) {
				int tDepOpen = possibDeparture.getNextOpen(tOpen), tDepClose = 0;
				for(; tDepOpen < tClose; tDepOpen=possibDeparture.getNextOpen(tDepClose)) {
					tDepClose=possibDeparture.getNextClose(tDepOpen);
					if(tDepClose >= tClose)
						break;
				}
				if(tDepClose==0)
					continue;
				arrivVoulues.intervals.add(new Bound(tOpen==ppcm ? 0 : tOpen, true));
				if(tDepOpen >= tClose)
					arrivVoulues.intervals.add(new Bound(tDepClose>ppcm ? tDepClose-ppcm : tDepClose, false));
				else
					arrivVoulues.intervals.add(new Bound(tClose>ppcm ? tClose-ppcm : tClose, false));
			}
			return arrivVoulues;
		}
	}
	
	public static class IntervalsModulo {
		public int mod;
		public TreeSet<Bound> intervals;
		public boolean state_t0; // Cas particulier pour t=0 puisque la vitesse est conservée
		public boolean empty_state; // L'état lorsque vide.
		
		/**
		 * Crée un nouvel interval plein ou vide selon la valeur de full.
		 * @param period la période de cet IntervalsModulo (immutable)
		 * @param full true pour plein, false pour vide
		 */
		public IntervalsModulo(int period, boolean full) {
			mod = period;
			intervals = new TreeSet<Bound>();
			state_t0 = full;
			empty_state = full;
		}
		
		/**
		 * Crée une copie de im.
		 * @param im l'IntervalsModulo source
		 */
		@SuppressWarnings("unchecked")
		public IntervalsModulo(IntervalsModulo im) {
			mod = im.mod;
			intervals = (TreeSet<Bound>) im.intervals.clone();
			state_t0 = im.state_t0;
			empty_state = im.empty_state;
		}

		@Override
		public String toString() {
			return mod+":"+(intervals.isEmpty() ? "["+empty_state+"]" : intervals.toString());
		}
		
		public static int lcm(int a, int b) {
			return a * (b / gcd(a, b));
		}
		
		public static int gcd(int a, int b) {
			return b==0 ? a : gcd(b, a%b);
		}
		
		private int modulo(int v) {
			return mod==Integer.MAX_VALUE ? v : (v%mod + mod)%mod;
		}

		/**
		 * Retourne le premier instant possible d'état open, depuis l'instant spécifié inclus.
		 * @param open état de open à considérer
		 * @param from_b instant à partir duquel rechercher (peut être supérieur au modulo)
		 * @return le prochain instant d'état open après from_b, ou from_b si open est l'état présent.
		 */
		public int getNextState(boolean open, Bound from_b) {
			if(intervals.isEmpty())
				return (open == empty_state) ? from_b.value : Integer.MAX_VALUE;
			Bound b;
			if(from_b.value >= mod) {
				int memValue = from_b.value;
				from_b.value = modulo(memValue);
				b = intervals.higher(from_b);
				from_b.value = memValue;
			} else {
				b = intervals.higher(from_b);
			}
			int tour = 0;
			if(b==null) {
				if(mod==Integer.MAX_VALUE)
					return intervals.last().open==open ? from_b.value : Integer.MAX_VALUE;
				b = intervals.first();
				tour = 1;
			}
			if(b.open==open)
				return (tour + from_b.value/mod)*mod + b.value;
			else
				return from_b.value;
		}
		
		/**
		 * Retourne l'état de l'instant t selon l'IntervalsModulo.
		 * @param t un instant par rapport à la même origine (le 0).
		 * @return vrai si l'instant t est valide
		 */
		private boolean isInstantValid(int t) {
			if(intervals.isEmpty())
				return empty_state;
			Bound b = new Bound(modulo(t), true);
			Bound b_prec = intervals.floor(b);
			return b_prec==null ? !intervals.ceiling(b).open : b_prec.open;
		}
		
		public boolean isFull() {
			return intervals.isEmpty() && empty_state;
		}
		
		public boolean isEmpty() {
			return intervals.isEmpty() && !empty_state;
		}
		
		private void setFullWithState(boolean state) {
			intervals.clear();
			empty_state = state;
		}
		
		/**
		 * Translate l'IntervalsModulo de delta_t.
		 * @param delta_t le temps à translater (positif ou négatif)
		 */
		private void translate(int delta_t) {
			TreeSet<Bound> newIntervals = new TreeSet<Bound>();
			for(Bound b : intervals) {
				newIntervals.add(new Bound(modulo(b.value + delta_t), b.open));
			}
			intervals = newIntervals;
		}
		
		/**
		 * Insère l'état possib pour le temps de l'occurrence o, rapportée à l'instant
		 * t_instant et élargie de delta.
		 * @param o l'occurrence qui doit être de même période que l'IntervalsModulo
		 * @param t_instant l'instant t
		 * @param delta le temps à ajouter aux temps de présence des occurrences
		 * @param possib l'état à attribuer pour le temps de présence des occurrences
		 */
		public void addOccurrence(Niveau.Occurrence o, int t_instant, int delta, boolean possib) {
			if(empty_state==possib && intervals.isEmpty()) // Déjà complet
				return;
			if(o.mod==Integer.MAX_VALUE) { // Occurrence sporadique et non périodique
				int end = o.r + o.delta - t_instant;
				if(end > 0) // Pas dépassée
					addInterval(0, end, possib);
				return;
			}
			if(2*(o.delta+delta)>=mod) { // Ensemble complet
				setFullWithState(possib);
				return;
			}
			addInterval(o.r-t_instant - o.delta-delta, o.r-t_instant + o.delta+delta, possib);
		}
		
		public void addInterval(int open, int close, boolean state) {
			Bound b0 = new Bound(modulo(open), state);
			Bound b1 = new Bound(modulo(close), !state);
			// Borne ouvrante
			addOpenBound(b0);
			// Supprime les bornes entre les deux
			cleanInBetween(b0,b1,state);
			// Borne fermante
			addCloseBound(b1);
			if(intervals.isEmpty())
				setFullWithState(state);
		}
		
		private void cleanInBetween(Bound b0, Bound b1, boolean stateIfFull) {
			if(b0.compareTo(b1)<0) { // Dans le bon ordre
				Bound b = intervals.higher(b0);
				while(b!=null && b.compareTo(b1)<0) {
					intervals.remove(b);
					b = intervals.higher(b0);
				}
			} else { // Aux extrémités
				Bound b;
				try {
					b = intervals.first();
					while(b.compareTo(b1)<0) {
						intervals.remove(b);
						b = intervals.first();
					}
					b = intervals.last();
					while(b0.compareTo(b)<0) {
						intervals.remove(b);
						b = intervals.last();
					}
				} catch(NoSuchElementException e) {
					setFullWithState(stateIfFull);
				}
			}
		}

		private void addOpenBound(Bound b) {
			if(intervals.isEmpty()) {
				intervals.add(b);
				return;
			}
			Bound prev = intervals.floor(b);
			if(prev==null) prev = intervals.last();
			// prev est la borne précédente ou égale modulo mod
			if(prev.equals(b)) { // Borne égale
				if(prev.open!=b.open)
					intervals.remove(prev);
			} else if(prev.open!=b.open) { // Borne strictement inférieure
				intervals.add(b);
			}
		}
		
		private void addCloseBound(Bound b) {
			if(intervals.isEmpty()) {
				return;
			}
			Bound next = intervals.ceiling(b);
			if(next==null) next = intervals.first();
			// next est la borne supérieure ou égale modulo mod
			if(next.equals(b)) { // Borne égale
				if(next.open!=b.open)
					intervals.remove(next);
			} else if(next.open!=b.open) { // Borne strictement supérieure
				intervals.add(b);
			}
		}
	}
	
	public static class Heuristic {
		private Niveau niv;
		public int h_param = 0; // nb de frames en plus par segment : utilisé pour paramétrer la résolution de l'heuristique
		public int h_t, h_m; // Heuristique temporelle et nombre de coups
		public SparseIntArray fleurToSegment; // Segment parent de la fleur d'indice r*COL+c.
		public ArrayList<Segment> minSegments;
		private SparseArray<LinkedList<DistantSegment>> distToSegments;
		
		public Heuristic(Niveau niveau) {
			niv = niveau;
		}
		
		/**
		 * Prépare les segments recouvrant la grille qui seront ensuite utilisés
		 * par "compute" pour le calcul de l'heuristique de chaque état.
		 */
		public void prepareRootState() {
			distToSegments = new SparseArray<LinkedList<DistantSegment>>();
			minSegments = new ArrayList<Segment>();
			fleurToSegment = new SparseIntArray();
			PriorityQueue<Segment> segments = new PriorityQueue<Segment>();
			Segment colSegment = new Segment();
			Segment[] rowSegments = new Segment[Position.ROW];
			for(int r=0; r < Position.ROW; r++)
				rowSegments[r] = new Segment();
			// Préparation de tous les segments
			for(int c=0; c < Position.COL; c++) {
				for(int r=0; r < Position.ROW; r++) {
					int cell = niv.carte[r][c];
					if(cell==2 || cell==3) { // Fleur
						Flower f = new Flower(r, c);
						colSegment.addFlower(f);
						rowSegments[r].addFlower(f);
					} else if(cell==1 || cell==5 || cell>=10) { // Menhir (ou menhir rouge car en cours de jeu !) ou arc-en-ciel
						if(colSegment.nFleurs!=0) {
							segments.add(colSegment);
							colSegment = new Segment();
						}
						if(rowSegments[r].nFleurs!=0) {
							segments.add(rowSegments[r]);
							rowSegments[r] = new Segment();
						}
					}
				}
				if(colSegment.nFleurs!=0) {
					segments.add(colSegment);
					colSegment = new Segment();
				}
			}
			for(int r=0; r < Position.ROW; r++) {
				if(rowSegments[r].nFleurs!=0) {
					segments.add(rowSegments[r]);
				}
			}
			// Sélection d'un set non recouvrant par ordre des plus grands segments
			Segment s;
			int indexSeg = 0;
			while(!segments.isEmpty()) {
				s = segments.poll();
				if(s.nFleurs==0)
					break;
				minSegments.add(s);
				s.invalidFlowers(segments, fleurToSegment, indexSeg++);
				s.computeLength();
			}
			// Valeurs heuristiques pré-calculées.
			if(niv.h_fleurs!=null) {
				for(int[] h_f : niv.h_fleurs) {
					minSegments.get(fleurToSegment.get(h_f[0]*Position.COL + h_f[1])).length = h_f[2];
				}
			}
		}
		
		/**
		 * Calcule l'heuristique de la grille par rapport à la position du colibri.
		 * Utilise l'ensemble minimal de segments pour calculer une estimation
		 * d'itinéraire entre ces derniers.
		 * @param coli la position du colibri dans l'état considéré
		 * @param grid DynaGrid de l'état actuel de niv
		 */
		public void compute(SimplePos coli, DynaGrid grid) {
			int i = 0;
			double h_t = 0;
			double h_m = 0;
			for(Segment s : minSegments) {
				if(grid.flowersBySegment[i++] > 0) { // Le segment n'a pas été complètement ramassé.
					if(h_param==0) {
						h_t += s.length/0.75;
						h_m += 1;
						if(s.pick_factor < 1.8) { // Pondération des segments par fréquence de ramassage
							s.pick_factor *= 1.01;
						}
					} else {
						h_t += (s.length/0.75 + h_param)*s.pick_factor;
						h_m += (1 + h_param/2)*s.pick_factor;
					}
				} else if(h_param==0 && s.pick_factor > 0.8) {
					s.pick_factor *= 0.98;
				}
			}
			// On ajoute la distance au plus proche segment (en temps et moves)
			DistantSegment dSeg = closestDistantSegment(coli, grid);
			if(dSeg!=null) {
				if(h_param==0) {
					h_t += dSeg.distance/0.75;
					h_m += dSeg.moves;
				} else {
					h_t += dSeg.distance/0.75*dSeg.segment.pick_factor;
					h_m += dSeg.moves*dSeg.segment.pick_factor;
				}
			}
			this.h_t = (int)Math.round(h_t);
			this.h_m = (int)Math.ceil(h_m);
		}
		
		/**
		 * Retourne le segment le plus proche de la position coli.
		 * @param coli la position du colibri
		 * @param grid l'état de la grille courante
		 * @return le DistantSegment le plus proche
		 */
		private DistantSegment closestDistantSegment(SimplePos coli, DynaGrid grid) {
			LinkedList<DistantSegment> distantSegments = distToSegments.get(coli.r*Position.COL + coli.c);
			if(distantSegments==null) { // Première fois demandé: on construit la liste ordonnée pour cette position
				PriorityQueue<DistantSegment> orderedSegs = new PriorityQueue<DistantSegment>();
				for(Segment s : minSegments) {
					orderedSegs.add(new DistantSegment(coli, s));
				}
				distantSegments = new LinkedList<DistantSegment>();
				while(!orderedSegs.isEmpty()) {
					distantSegments.add(orderedSegs.poll());
				}
				distToSegments.put(coli.r*Position.COL + coli.c, distantSegments);
			}
			// Récupère le segment le plus proche qui n'est pas encore totalement ramassé
			for(DistantSegment dSeg : distantSegments) {
				if(grid.flowersBySegment[dSeg.segment.index]>0) { // Segment non ramassé
					return dSeg;
				}
			}
			return null;
		}
		
		public static class Flower {
			public int r, c;
			public LinkedList<Segment> segments;
			
			public Flower(int r, int c) {
				this.r = r;
				this.c = c;
				segments = new LinkedList<Segment>();
			}
			
			public void addSegment(Segment s) {
				segments.add(s);
			}
		}
		
		public static class Segment implements Comparable<Segment>  {
			public int index; // L'index du segment dans minSegments
			public double pick_factor = 1.0; // Facteur d'influence en fonction de la fréquence de ramassage
			public SimplePos e1, e2; // Les extrémités du segment
			public byte nFleurs = 0; // le nombre de fleurs non attribuées qu'il recouvre
			public int length = 0; // La longueur du segment
			public LinkedList<Flower> flowers;
			
			public Segment() {
				flowers = new LinkedList<Flower>();
			}
			
			@Override
			public String toString() {
				return e1+"->"+e2+": "+((int)nFleurs);
			}
			
			@Override
			public int compareTo(Segment s) {
				return s.nFleurs - nFleurs;
			}
			
			public void addFlower(Flower f) {
				flowers.add(f);
				f.addSegment(this);
				nFleurs++;
			}
			
			/**
			 * Calcule la longueur du segment en nombre de cases, définit ses extrémités,
			 * puis efface les fleurs.
			 */
			public void computeLength() {
				Flower f1 = flowers.getFirst();
				Flower f2 = flowers.getLast();
				length = f2.r - f1.r + f2.c - f1.c + 1;
				e1 = new SimplePos(f1.r, f1.c);
				e2 = new SimplePos(f2.r, f2.c);
				flowers = null; // Pour ne pas garder les objets Fleur en mémoire.
			}
			
			/**
			 * Invalide ses fleurs dans les autres segments et référence ce segment
			 * comme parent de ses fleurs. Aussi, enregistre son index dans minSegments.
			 * @param segs queue de tous les segments
			 * @param fleurToSegment map de fleur à segment parent
			 * @param indexSeg indice du segment dans minSegments
			 */
			public void invalidFlowers(PriorityQueue<Segment> segs, SparseIntArray fleurToSegment, int indexSeg) {
				this.index = indexSeg;
				for(Flower f : flowers) {
					fleurToSegment.put(f.r*Position.COL + f.c, indexSeg);
					for(Segment s : f.segments) {
						if(s!=this) {
							segs.remove(s);
							s.flowers.remove(f);
							s.nFleurs--;
							segs.add(s); // Pour replacer le segment dans la queue
						}
					}
				}
			}
		}
		
		public static class DistantSegment implements Comparable<DistantSegment> {
			public Segment segment; // Le segment en question
			public byte distance; // la distance minimale, en cases, à l'une ou l'autre extrémité du segment
			public byte moves; // le nombre de moves à effectuer pour rejoindre l'une ou l'autre extrémité du segment
			
			public DistantSegment(SimplePos coli, Segment s) {
				segment = s;
				computeDistance(coli);
			}
			
			private void computeDistance(SimplePos coli) {
				distance = (byte) Math.min(coli.distance(segment.e1), coli.distance(segment.e2));
				if(segment.e1.r==segment.e2.r) { // Selon la ligne
					moves = (byte) ((coli.r==segment.e1.r) ? 0 : 1);
					if(segment.e1.c < coli.c && coli.c < segment.e2.c)
						moves++;
				} else { // selon la colonne
					moves = (byte) ((coli.c==segment.e1.c) ? 0 : 1);
					if(segment.e1.r < coli.r && coli.r < segment.e2.r)
						moves++;
				}
			}
			
			@Override
			public int compareTo(DistantSegment ds) {
				return distance - ds.distance;
			}
		}
		
	}
}
