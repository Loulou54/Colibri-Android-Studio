package com.game.colibri;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class Dynamite extends ImageView {

	public static final int REMOVE_MENHIR_DELAY = 22; // Le temps après lequel le menhir est enlevé
	private static final int[] ANIM_FRAMES = new int[] {0, 15, 17, 19, 21, 23, 25};

	private int ligne, colonne;
	private int frameDepot;
	private AnimationDrawable exploAnim;

	public Dynamite(Context context, double cellWidth, double cellHeight, int ligne, int colonne, int frameDepot) {
		super(context);
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams((int)(3*cellWidth/2), (int)(3*cellHeight/2));
		params.leftMargin = (int)(colonne*cellWidth-cellWidth/4);
		params.topMargin = (int)(ligne*cellHeight);
		params.bottomMargin = Integer.MAX_VALUE;
		params.rightMargin = Integer.MAX_VALUE;
		this.ligne = ligne;
		this.colonne = colonne;
		this.frameDepot = frameDepot;
		setBackgroundResource(R.drawable.explosion);
		exploAnim = (AnimationDrawable) getBackground();
		setLayoutParams(params);
	}

	public Dynamite(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public Dynamite(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public void animStep(int currentFrame) {
		int elapsedFrames = currentFrame - frameDepot;
		for(int i=ANIM_FRAMES.length-1; i >= 0; i--) {
			if(ANIM_FRAMES[i] <= elapsedFrames) {
				exploAnim.selectDrawable(i);
				return;
			}
		}
	}

	public boolean shouldRemoveMenhir(int currentFrame) {
		return currentFrame - frameDepot == REMOVE_MENHIR_DELAY;
	}

	public int getLigne() {
		return ligne;
	}

	public int getColonne() {
		return colonne;
	}
}
