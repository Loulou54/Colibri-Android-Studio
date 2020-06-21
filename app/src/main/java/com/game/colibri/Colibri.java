package com.game.colibri;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

public class Colibri extends Animal {
	
	private double xf=10,yf=10; // Pour le colibri du menu de sélection : définit les coord en pixels de la position visée.
	private boolean versDroite=false;
	private boolean inGame = false;
	
	public Colibri(Context context, double dbx, double dby, double w, double h) {
		super(context,dbx,dby,w,h);
		this.setBackgroundResource(R.drawable.colibri_d);
		acc=0.1;
		v_max=0.7501;
	    step=0;
	    inGame = true;
	}
	
	/**
	 * Constructeurs XML d'un colibri
	 * 		@param context
	 * 		@param attrs
	 */
	public Colibri(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.setBackgroundResource(R.drawable.colibri_d);
	}
	
	public Colibri(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.setBackgroundResource(R.drawable.colibri_d);
	}
	
	@Override
	public void deplacer() {
		step=Math.min(step+acc, v_max); // Vitesse plafonnée à v_max case/frame.
		deplacer(mx*step,my*step);
	}
	
	public void setSpriteDirection() {
		boolean running = ((AnimationDrawable) this.getBackground()).isRunning();
		if(mx==1)
			this.setBackgroundResource(R.drawable.colibri_d);
		else if(mx==-1)
			this.setBackgroundResource(R.drawable.colibri_g);
		if(running)
			this.start();
	}
	
	/**
	 * Pour le menu de sélection, permet d'enregistrer la position du colibri à la fin d'un déplacement.
	 */
	public void savePosFinAnim(double x, double y, boolean regardeVersDroite) {
		xf=x;
		yf=y;
		versDroite=regardeVersDroite;
	}
	
	/**
	 * Pour le menu de sélection, permet de définir la position du colibri selon xf et yf.
	 */
	public void setPosFinAnim() {
		params=(RelativeLayout.LayoutParams) this.getLayoutParams();
		setPos(xf/Carte.cw,yf/Carte.ch);
	    if(versDroite) {
	    	mx=1;
	    	setSpriteDirection();
	    }
	}
	
	/**
	 * Permet de définir l'emplacement du colibri dans le menu de sélection à la fin de l'animation.
	 * Contournement du bug du listener d'animation. 
	 */
	@Override
	protected void onAnimationEnd() {
	    super.onAnimationEnd();
	    if(!inGame)
		    setPosFinAnim();
	}
}
