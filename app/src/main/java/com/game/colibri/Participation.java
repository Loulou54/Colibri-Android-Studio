package com.game.colibri;

public class Participation {
	
	public static final int FORFAIT = Integer.MAX_VALUE-1, NOT_PLAYED = Integer.MAX_VALUE;
	
	public Joueur joueur;
	public double cumul_score;
	public int t_cours;
	public int t_fini;
	public double score;
	public int rank;
	private double cotisation;
	
	/**
	 * Crée une nouvelle participation.
	 * @param joueur
	 * @param cumul_score
	 * @param t_cours
	 * @param t_fini
	 * @param score
	 * @param rank
	 */
	public Participation(Joueur joueur, double cumul_score, int t_cours, int t_fini, double score, int rank) {
		this.joueur = joueur;
		this.cumul_score = cumul_score;
		this.t_cours = t_cours;
		this.t_fini = t_fini;
		this.score = score;
		this.rank = rank;
	}
	
	public void solved(int t) {
		t_cours = t;
	}
	
	public void setCotisation(double cotis) {
		cotisation = cotis;
	}
	
	public void fini(int classement, int participants, double scoreWon) {
		t_fini = t_cours;
		t_cours = 0;
		if(t_fini == NOT_PLAYED) {
			rank = 0;
			return;
		}
		rank = classement;
		joueur.win(participants-classement);
		joueur.loose(classement-1);
		score = Math.max(scoreWon - cotisation, -joueur.getScore());
		cumul_score += score;
		joueur.addScore(score);
		if(participants > 1)
			joueur.defi();
	}
}
