package com.game.colibri;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public abstract class Animal extends ImageView {
	
	protected RelativeLayout.LayoutParams params;
	private double ww,hh; // Largeur et hauteur de l'animal en pixels
	protected double xx,yy; // Position en cases
	public double v_max; // Vitesse maximale en case/frame
	public double step; // pas des mouvements en case/frame
	protected double acc; // accélération en case/frame/frame
	public int mx,my; // sens du mouvement de l'animal (1,-1,0)
	
	/**
	 * Constructeur java d'un animal 
	 * 
	 * @param context le contexte de création 
	 * @param dbx abscisse du coin supérieur gauche en cases
	 * @param dby ordonnée du coin supérieur gauche en cases
	 * @param w largeur de l'animal en cases
	 * @param h hauteur de l'animal en cases
	 */
	public Animal(Context context, double dbx, double dby, double w, double h) {
		super(context);
		ww=w; hh=h;
		params=new RelativeLayout.LayoutParams((int)(w*Carte.cw), (int)(h*Carte.ch));
		setPos(dbx,dby);
	    mx=0;
	    my=0;
	}
	
	
	/**
	 * Constructeurs XML d'un animal
	 * 		@param context
	 * 		@param attrs
	 */
	public Animal(Context context, AttributeSet attrs) {
		super(context, attrs);
		params=(RelativeLayout.LayoutParams) this.getLayoutParams();
	}
	
	public Animal(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		params=(RelativeLayout.LayoutParams) this.getLayoutParams();
	}
	
	
	/**
	 * Met a jour la position en cases.
	 * 		@param x 
	 * 		@param y
	 */
	public void setPos(double x, double y) {
		xx=x; yy=y;
		params.leftMargin = (int)(xx*Carte.cw);
	    params.topMargin = (int)(yy*Carte.ch);
		this.setLayoutParams(params);
	}
	
	/**
	 * Donne la direction de déplacement de l'animal, en attribuant les valeurs mx et my.
	 * 		@param dir un couple donnant la direction x/y de déplacement de l'animal. (ex : {0,-1} = vers la gauche)
	 * Donne la direction de déplacement de l'animal, en attribuant les valeurs mx et my.
	 * 		@param dir un couple donnant la direction x/y de déplacement de l'animal. (ex : {0,-1} = vers la gauche)
	 */
	public void setDirection(int[] dir) {
		mx=dir[0];
		my=dir[1];
	}
	
	/**
	 * Renvoi la direction de déplacement de l'animal { mx, my}.
	 *
	 */
	public int[] getDirection() {
		int[] dir = new int[2];
		dir[0]=mx;
		dir[1]=my;
		return dir;
	}
	
	/**
	 * Déplace l'animal rapport a sa position d'origne d'une certaine valeur en cases.
	 * 		@param dx le delta dont il faut déplacer l'abcisse 
	 * 		@param dy le delta dont il faut délacer l'ordonné
	 */
	public void deplacer(double dx, double dy) {
		xx+=dx; yy+=dy;
		params.leftMargin = (int)(xx*Carte.cw);
	    params.topMargin = (int)(yy*Carte.ch);
		this.setLayoutParams(params);
	}
	
	/**
	 * Déplace l'animal selon l'implémentation voulue par la classe fille. (méthode abstraite)
	 */
	public abstract void deplacer();
	
	/**
	 * Retourne la positon de l'animal en nombre de cases.
	 * 
	 * 		@return la position {x,y} de l'animal dans un tableau 
	 */
	public double[] getPos() {
	    return new double[] {xx , yy};
	}
	
	/**
	 * Retourne la ligne sur laquelle se trouve le centre de l'animal
	 */
	public int getRow() {
		return (int)(yy+hh/2);
	}
	
	/**
	 * Retourne la colonne sur laquelle se trouve le centre de l'animal
	 */
	public int getCol() {
		return (int)(xx+ww/2);
	}
	
	/**
	 * Commencer l'animation
	 */
	public void start() {
		((AnimationDrawable) this.getBackground()).start();
	}
	
	
	/**
	 * Stopper l'animation
	 */
	public void stop() {
		((AnimationDrawable) this.getBackground()).stop();
	}
}
