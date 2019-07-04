package com.game.colibri;

import android.content.Context;

public class Joueur {
	
	public static int[] img = new int[] {R.drawable.colibri_d1, R.drawable.fleur, R.drawable.fleurm, R.drawable.menhir, R.drawable.dynamite_allumee, R.drawable.explo2, R.drawable.rainbow, R.drawable.skull, R.drawable.chat_0, R.drawable.vache_0,
		R.drawable.doge, R.drawable.doge_lunettes, R.drawable.pro_husky, R.drawable.pro_grumpy_cat, R.drawable.pro_hamster, R.drawable.pro_lama, R.drawable.pro_lama2, R.drawable.pro_monkey, R.drawable.pro_ostrich, R.drawable.pro_owl, R.drawable.pro_raccoon, R.drawable.pro_sloth,
		R.drawable.megusta, R.drawable.pro_cry, R.drawable.pro_haha, R.drawable.pro_happy, R.drawable.pro_how_cute, R.drawable.pro_derpina_brown, R.drawable.pro_derpina_blond, R.drawable.pro_derpina_blond_haha, R.drawable.pro_troll, R.drawable.pro_rage, R.drawable.pro_serious, R.drawable.pro_stop_it, R.drawable.pro_thumbs_up, R.drawable.pro_lol};
	
	private String pseudo, pays;
	private int id, exp, progress, coliBrains, expProgCB, defis, win, loose, avatar, rang;
	private double score;
	private long playTime, time;
	
	/**
	 * Crée un nouveau Joueur.
	 * @param id
	 * @param pseudo
	 * @param pays
	 * @param exp
	 * @param progress
	 * @param coliBrains
	 * @param expProgCB
	 * @param defis
	 * @param win
	 * @param loose
	 * @param score
	 * @param playTime
	 * @param avatar
	 * @param time
	 */
	public Joueur(int id, String pseudo, String pays, int exp, int progress, int coliBrains,
			int expProgCB, int defis, int win, int loose, double score, long playTime, int avatar, long time) {
		this.id=id;
		this.pseudo=pseudo;
		this.pays=pays;
		this.exp=exp;
		this.progress=progress;
		this.coliBrains=coliBrains;
		this.expProgCB=expProgCB;
		this.defis=defis;
		this.win=win;
		this.loose=loose;
		this.score=score;
		this.playTime=playTime;
		this.avatar=avatar;
		this.time=time;
		this.rang=0;
	}
	
	public int getId() {
		return id;
	}
	
	public String getPseudo() {
		return pseudo;
	}
	
	public String getPays() {
		return pays;
	}
	
	public int getExp() {
		return exp;
	}
	
	public int getProgress() {
		return progress;
	}
	
	public int getColiBrains() {
		return coliBrains;
	}
	
	public int getExpProgCB() {
		return expProgCB;
	}
	
	public int getDefis() {
		return defis;
	}
	
	public int getWin() {
		return win;
	}
	
	public int getLoose() {
		return loose;
	}
	
	public double getScore() {
		return score;
	}
	
	public long getPlayTime() {
		return playTime;
	}
	
	public int getAvatar() {
		return img[avatar];
	}
	
	public int getRang() {
		return rang;
	}
	
	public String getLastVisit(Context c) {
		long t = System.currentTimeMillis()/1000-time;
		if(t<3600)
			return c.getResources().getString(R.string.minutesAgo, t/60, t/60>1 ? "s" : "");
		else if(t<3600*24)
			return c.getResources().getString(R.string.hoursAgo, t/3600, t/3600>1 ? "s" : "");
		else
			return c.getResources().getString(R.string.daysAgo, t/(3600*24), t/(3600*24)>1 ? "s" : "");
	}
	
	/**
	 * Pour transformer le temps relatif en absolu de time. A appeler après désérialisation JSON.
	 */
	public void computeLastVisit() {
		time=System.currentTimeMillis()/1000-time;
	}
	
	/**
	 * Incrémente le compteur de défis du joueur.
	 */
	public void defi() {
		defis++;
	}
	
	/**
	 * Incrémente le compteur de victoires du joueur.
	 */
	public void win(int n) {
		win+=n;
	}
	
	/**
	 * Incrémente le compteur de défaites du joueur.
	 */
	public void loose(int n) {
		loose+=n;
	}
	
	/**
	 * Ajoute de l'expérience au joueur.
	 * @param e
	 */
	public void addExp(int e) {
		exp+=e;
	}
	
	/**
	 * Ajoute les points p (positif ou négatif) au score de ce joueur.
	 * @param p points gagnés ou perdus
	 */
	public void addScore(double points) {
		score += points;
	}
	
	/**
	 * Ajoute le temps de jeu t.
	 * @param t temps de jeu à ajouter en ms.
	 */
	public void addPlayTime(long t) {
		playTime += t;
	}
	
	@Override
	public String toString() {
		return pseudo+";"+pays+";"+exp+";"+progress+";"+coliBrains+";"+expProgCB+";"+defis+";"+win+";"+loose+";"+score+";"+playTime+";"+avatar+";"+time;
	}
}
