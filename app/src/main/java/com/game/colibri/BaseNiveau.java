package com.game.colibri;

import java.util.LinkedList;
import java.util.Random;

import android.util.SparseArray;


public class BaseNiveau {
	
	private int[][] carte, champs;
	private Random ran;
	private int arc_index;
	private int nbArcs;
	private SparseArray<int[]> rainbows;
	private LinkedList<Integer> aire;
	
	public BaseNiveau(Random random, int[][] carteOrigin, SparseArray<int[]> rbs, int nbArcs) {
		carte = carteOrigin;
		rainbows = rbs;
		this.nbArcs = nbArcs;
		champs = new int[12][20];
		aire = new LinkedList<Integer>();
		ran = random;
		arc_index=10;
		//generate(4,2,2,1);
	}
	
	@Override
	public String toString() {
		String s="Carte :\n";
		for(int i=0; i<12; i++) {
			for(int j=0; j<20; j++) {
				s+=carte[i][j]+" ";
			}
			s+="\n";
		}
		s+="Champs :\n";
		for(int i=0; i<12; i++) {
			for(int j=0; j<20; j++) {
				s+=champs[i][j]+" ";
			}
			s+="\n";
		}
		s+=aire.size()+" Aires :\n";
		for(int a : aire) {
			s+=a+", ";
		}
		return s;
	}
	
	/**
	 * Remplis la grille par des motifs en menhirs selon les paramètres.
	 * @param lines nombre de lignes
	 * @param polys nombre de polygônes
	 * @param ellipses nombre d'ellipses
	 * @param mode mode de répartition : 1:tout sur l'ensemble de la carte ; 2:lignes sur les bords ;
	 * 			3:séparation gauche/droite ; 4: rectangle/ellipse au centre
	 * @return {dep_r, dep_c, arc_index} la position de départ du colibri suivi de l'index à utiliser
	 * pour la prochaine paire d'arc-en-ciel.
	 */
	public int[] generate(int lines, int polys, int ellipses, int mode) {
		if(mode==1) {
			for(int i=0; i<ellipses; i++) {
				ellipse(ran.nextInt(12),ran.nextInt(20),ran.nextInt(12),ran.nextInt(20),1);
			}
			for(int i=0; i<polys; i++) {
				rectangle(ran.nextInt(12),ran.nextInt(20),ran.nextInt(12),ran.nextInt(20),1+ran.nextInt(4));
			}
			for(int i=0; i<lines; i++) {
				ligne(ran.nextInt(12),ran.nextInt(20),ran.nextInt(12),ran.nextInt(20),1+ran.nextInt(4));
			}
		} else if(mode==2) {
			for(int i=0; i<lines; i++) {
				ligne(ran.nextInt(12),ran.nextInt(20),ran.nextInt(12),ran.nextInt(20),1+ran.nextInt(4));
			}
			vide(ran.nextInt(6),ran.nextInt(6),6+ran.nextInt(6),14+ran.nextInt(6));
		} else if(mode==3) {
			if(ran.nextInt(2)==0)
				rectangle(ran.nextInt(4),ran.nextInt(3),8+ran.nextInt(4),7+ran.nextInt(3),1);
			else
				ellipse(ran.nextInt(4),ran.nextInt(3),8+ran.nextInt(4),7+ran.nextInt(3),1);
			if(ran.nextInt(2)==0)
				rectangle(ran.nextInt(4),10+ran.nextInt(3),8+ran.nextInt(4),17+ran.nextInt(3),1);
			else
				ellipse(ran.nextInt(4),10+ran.nextInt(3),8+ran.nextInt(4),17+ran.nextInt(3),1);
			for(int i=0; i<lines; i++) {
				ligne(ran.nextInt(12),ran.nextInt(20),ran.nextInt(12),ran.nextInt(20),1+ran.nextInt(4));
			}
		} else if(mode==4) {
			if(ran.nextInt(2)==0)
				rectangle(ran.nextInt(4),ran.nextInt(3),8+ran.nextInt(4),17+ran.nextInt(3),1);
			else
				ellipse(ran.nextInt(4),ran.nextInt(3),8+ran.nextInt(4),17+ran.nextInt(3),1);
			for(int i=0; i<lines; i++) {
				ligne(ran.nextInt(12),ran.nextInt(20),ran.nextInt(12),ran.nextInt(20),1+ran.nextInt(4));
			}
		}
		champsConnexes();
		passagesEntreChamps();
		int ma=0,ind=1;
		for(int i=0; i<aire.size(); i++) {
			if(aire.get(i)>ma) {
				ma=aire.get(i);
				ind=i+1;
			}
		}
		int[] depart = getRandomPosInArea(ind);
		return new int[] {depart[0], depart[1], arc_index};
	}
	
	/**
	 * Remplis la matrice champs en donnant un même indice à toutes les cases connexes.
	 * (De 1 à n, avec n le nombre d'aires différentes.)
	 */
	private void champsConnexes() {
		int n=1;
		for(int r=0; r<carte.length; r++) {
			for(int c=0; c<carte[0].length; c++) {
				if(champs[r][c]==0 && carte[r][c]!=1) {
					aire.addLast(remplirDepuis(r,c,n++));
				}
			}
		}
	}
	
	private void passagesEntreChamps() {
		int[] p1,p2; // Extrémités de la ligne blanche à tracer.
		for(int n1=1; n1<=aire.size(); n1++) {
			for(int n2=n1+1; n2<=aire.size();n2++) {
				if(ran.nextInt(aire.get(n1-1))>2 && ran.nextInt(aire.get(n2-1))>2) {
					p1=getRandomPosInArea(n1);
					p2=getRandomPosInArea(n2);
					if(ran.nextInt(3)==0 && nbArcs>0) { // Arc-en-ciel
						carte[p1[0]][p1[1]]=arc_index;
						carte[p2[0]][p2[1]]=arc_index;
						rainbows.put(arc_index, new int[] {p1[0],p1[1],p2[0],p2[1]});
						nbArcs--;
						arc_index+=2;
					} else // Ligne
						ligne(p1[0],p1[1],p2[0],p2[1],2);
				}
			}
		}
	}
	
	/**
	 * Retourne une case de l'aire d'indice indiceAire
	 * @param indiceAire
	 * @return (ligne, colonne) de la case sous int[]
	 */
	private int[] getRandomPosInArea(int indiceAire) {
		int ra = ran.nextInt(aire.get(indiceAire-1)); // Nombre aléatoire d'unité d'aire à compter pour désigner une case d'une certaine aire.
		int cp=0;
		for(int r=0; r<carte.length; r++) {
			for(int c=0; c<carte[0].length; c++) {
				if(champs[r][c]==indiceAire) {
					if(cp==ra) {
						if(carte[r][c]!=0)
							return getRandomPosInArea(indiceAire);
						else
							return new int[] {r,c};
					}
					cp++;
				}
			}
		}
		return new int[] {0,0};
	}
	
	/**
	 * Remplis de l'indice n dans la matrice champs toutes les cases connexes à la case (r,c) et retourne
	 * le nombre de cases couvertes.
	 * @param r ligne
	 * @param c colonne
	 * @param n indice d'aire
	 * @return l'aire en nombre de cases.
	 */
	private int remplirDepuis(int r, int c, int n) {
		int aire=0;
		if(r>=0 && r<carte.length && c>=0 && c<carte[0].length && carte[r][c]!=1 && champs[r][c]==0) {
			champs[r][c]=n;
			aire++;
			aire+=remplirDepuis(r+1,c,n);
			aire+=remplirDepuis(r-1,c,n);
			aire+=remplirDepuis(r,c+1,n);
			aire+=remplirDepuis(r,c-1,n);
		}
		return aire;
	}
	
	/**
	 * Trace une ligne de menhirs entre (rd,cd) et (rf,cf) selon le style défini par mode.
	 * @param rd 
	 * @param cd
	 * @param rf
	 * @param cf
	 * @param mode 1:plein ; 2:effacer ; 3:un sur deux ; 4:pose aléatoire.
	 */
	private void ligne(int rd, int cd, int rf, int cf, int mode) {
		double dr=rf-rd, dc=cf-cd;
		int sens;
		if(Math.abs(dr)>Math.abs(dc)) {
			sens = (int) Math.signum(dr);
			for(int i=0; i<=Math.abs(dr); i++) {
				poseMenhir(rd+i*sens, (int) (cd+i*sens*dc/dr+0.5), mode, i); // +0.5 car (int) (x+0.5) <=> round(x)
			}
		} else {
			sens = (int) Math.signum(dc);
			for(int i=0; i<=Math.abs(dc); i++) {
				poseMenhir((int) (rd+i*sens*dr/dc+0.5), cd+i*sens, mode, i);
			}
		}
	}
	
	/**
	 * Trace un rectangle de menhir défini par les deux coins (rd,cd) et (rf,cf) selon le style défini par mode.
	 * @param rd
	 * @param cd
	 * @param rf
	 * @param cf
	 * @param mode 1:plein ; 2:effacer ; 3:un sur deux ; 4:pose aléatoire.
	 */
	private void rectangle(int rd, int cd, int rf, int cf, int mode) {
		ligne(rd,cd,rd,cf,mode);
		ligne(rd,cf,rf,cf,mode);
		ligne(rf,cf,rf,cd,mode);
		ligne(rf,cd,rd,cd,mode);
	}
	
	/**
	 * Trace un cercle de centre (r,c) et de rayon ray selon le style défini par mode.
	 * @param r ligne
	 * @param c colonne
	 * @param b demi-axe lignes (y)
	 * @param a demi-axe colonnes (x)
	 * @param mode 1:plein ; 2:effacer ; 3:un sur deux ; 4:pose aléatoire.
	 */
	private void ellipse(int rd, int cd, int rf, int cf, int mode) {
		double r=(rd+rf)/2., c=(cd+cf)/2.;
		double a=Math.abs(cf-cd)/2., b=Math.abs(rf-rd)/2.;
		double cote_c = toInt(a*Math.sqrt(2)/2) + c-((int) c);
		double cote_r = toInt(b*Math.sqrt(2)/2) + r-((int) r);
		double calc;
		for(double co=-cote_c; co<=cote_c; co++) { // Selon les colonnes (pour les dessus et dessous de l'ellipse)
			calc=Math.sqrt(a*a-co*co)*b/a;
			poseMenhir(toInt(r+calc), toInt(c+co), mode, toInt(co));
			poseMenhir(toInt(r-calc), toInt(c-co), mode, toInt(co));
		}
		for(double ro=-cote_r; ro<=cote_r; ro++) { // Selonles lignes (pour les côtés de l'ellipse).
			calc=Math.sqrt(b*b-ro*ro)*a/b;
			poseMenhir(toInt(r+ro), toInt(c+calc), mode, toInt(ro));
			poseMenhir(toInt(r-ro), toInt(c-calc), mode, toInt(ro));
		}
	}
	
	private void vide(int r1, int c1, int r2, int c2) {
		for(int i=r1; i<=r2; i++) {
			for(int j=c1; j<=c2; j++) {
				carte[i][j]=0;
			}
		}
	}
	
	/**
	 * Pose un menhir en (r,c) selon le mode voulu.
	 * @param r
	 * @param c
	 * @param mode 1:plein ; 2:effacer ; 3:indice%2 ; 4:pose aléatoire
	 * @param indice utilisé dans le cas mode==3
	 */
	private void poseMenhir(int r, int c, int mode, int indice) {
		if(r<0 || r>=carte.length || c<0 || c>=carte[0].length || carte[r][c]>=10)
			return; // Hors de la carte ou emplacement arc-en-ciel.
		switch(mode) {
		case 1:
			carte[r][c]=1;
			break;
		case 2:
			carte[r][c]=0;
			break;
		case 3:
			carte[r][c]=Math.abs(indice)%2;
			break;
		case 4:
			carte[r][c]=ran.nextInt(2);
			break;
		}
	}
	
	private int toInt(double x) {
		return (int) (x+0.5);
	}
	
}
