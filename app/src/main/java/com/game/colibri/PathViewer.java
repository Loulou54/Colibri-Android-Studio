package com.game.colibri;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.ListIterator;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.RelativeLayout;

/**
 * Permet de visualiser une solution en superposition du niveau.
 * @author Louis
 *
 */
public class PathViewer extends RelativeLayout {
	
	private static final int MOVE_TIME_FAST = 120;
	private static final int MOVE_TIME_COLIB = 500;
	private static final float TIME_FACTOR = 0.9f;
	private static final int DURATION_BLUR_ANIM_BASE = 800;
	private static final int DURATION_BLUR_ANIM_PROP = 30;
	private static final int DURATION_FRAME = 1000/25;
	public static int STEPS_COLIBRI = 6;
	public static MoteurJeu mj;
	
	private LinkedList<Solver.Move> moves;
	private ListIterator<Solver.Move> movesIterator;
	private float xd, yd, xc, yc; // point de départ de la solution, et point de contrôle pour la prochaine courbe de Bézier
	private Path solPath; // Les premiers pas en gras et animés, le reste en fin
	private Paint pathPaint;
	public AlphaAnimation anim;
	private View fond; // Le fond de la view avec le dessin de solPath
	private View colibri; // Le colibri fantôme pour la solution
	private Bitmap explo, dyna_img, vache_img, warning, fleche_poussee;
	private LinkedList<int[]> dynaPos, vachePos, waitPos; // Position (x,y) des images de dynamites et vaches
	private LinkedList<Matrix> poussVachePosEtDir;// Position et direction des poussées de vache
	private float STROKE_WIDTH_MIN, STROKE_WIDTH_DELTA, BLUR_RADIUS_MIN, BLUR_RADIUS_DELTA;
	private int moveTime, stepsCount;
	private float blurStep;
	private float blurProgress;
	private float pathWidth, blurRadius;
	private DecelerateInterpolator interpolator = new DecelerateInterpolator();
	private boolean quickHint = false;

	public PathViewer(Context context) {
		super(context);
		init();
	}
	
	public PathViewer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PathViewer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

	private void init() {
    	setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    	pathPaint = new Paint();
    	pathPaint.setStyle(Paint.Style.STROKE);
    	pathPaint.setStrokeJoin(Paint.Join.ROUND);
    	pathPaint.setStrokeCap(Paint.Cap.ROUND);
    	pathPaint.setColor(Color.argb(80, 255, 255, 180));
    	anim = new AlphaAnimation(0.6f, 0);
    	anim.setStartOffset(5000);
    	anim.setDuration(3000);
    	anim.setAnimationListener(new AlphaAnimation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
				PathViewer.this.setVisibility(View.VISIBLE);
			}
			@Override
			public void onAnimationRepeat(Animation animation) {}
			@Override
			public void onAnimationEnd(Animation animation) {
				PathViewer.this.setVisibility(View.GONE);
				colibri.setAnimation(null);
			}
		});
    	fond = new View(getContext()) {
    		@Override
    	    protected void onDraw(Canvas can) {
    			if(solPath!=null) {
					for(int i=0; i<3; i++) { // Pour faire un effet de flou plus rapide que BlurMaskFilter
						pathPaint.setStrokeWidth(pathWidth + blurRadius*(1-i));
						can.drawPath(solPath, pathPaint);
					}
				}
    			for(int[] pos : dynaPos) {
    				can.drawBitmap(explo, pos[0], pos[1], null);
    				can.drawBitmap(dyna_img, pos[0], pos[1], null);
    			}
    			for(int[] pos : vachePos) {
    				can.drawBitmap(vache_img, pos[0], pos[1], null);
    			}
    			for(int[] pos : waitPos) {
    				can.drawBitmap(warning, pos[0], pos[1], null);
    			}
				for(Matrix mat : poussVachePosEtDir) {
					can.drawBitmap(fleche_poussee, mat, null);
				}
    		}
    	};
    	fond.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    	addView(fond);
    	colibri = new View(getContext());
    	colibri.setBackgroundResource(R.drawable.colibri_d_ghost);
    	colibri.setVisibility(View.GONE);
    	addView(colibri);
    	dynaPos = new LinkedList<int[]>();
    	vachePos = new LinkedList<int[]>();
    	waitPos = new LinkedList<int[]>();
    	poussVachePosEtDir = new LinkedList<Matrix>();
    }
    
	public void clear() {
		if(dyna_img==null) { // Car on ne peut pas initialiser ça dans le constructeur, cw/ch pas encore définis.
			explo = Bitmap.createScaledBitmap(((BitmapDrawable) getResources().getDrawable(R.drawable.explo0)).getBitmap(), (int)(3*Carte.cw/2), (int)(3*Carte.ch/2), true);
			dyna_img = Bitmap.createScaledBitmap(((BitmapDrawable) getResources().getDrawable(R.drawable.dynamite_allumee)).getBitmap(), (int)(3*Carte.cw/2), (int)(3*Carte.ch/2), true);
	    	vache_img = Bitmap.createScaledBitmap(((BitmapDrawable) getResources().getDrawable(R.drawable.vache_ghost)).getBitmap(), (int)(Carte.cw*1.25), (int)(Carte.ch*1.25), true);
	    	warning = Bitmap.createScaledBitmap(((BitmapDrawable) getResources().getDrawable(R.drawable.warning)).getBitmap(), (int)(Carte.cw*0.6), (int)(Carte.ch*0.6), true);
			fleche_poussee = Bitmap.createScaledBitmap(((BitmapDrawable) getResources().getDrawable(R.drawable.fleche_poussee)).getBitmap(), (int)(Carte.cw*0.5), (int)(Carte.ch*0.5), true);
	    	explo.setDensity(0);
	    	dyna_img.setDensity(0);
	    	vache_img.setDensity(0);
	    	warning.setDensity(0);
			fleche_poussee.setDensity(0);
		}
		dynaPos.clear();
		vachePos.clear();
		waitPos.clear();
		poussVachePosEtDir.clear();
		handler.removeMessages(RefreshHandler.ANIM_COLIB);
		colibri.setAnimation(null);
		colibri.setVisibility(View.GONE);
		setAnimation(null);
		setBackgroundColor(getResources().getColor(R.color.path_viewer_bg));
		STROKE_WIDTH_MIN = (float) Math.ceil(0.05*Carte.cw);
		STROKE_WIDTH_DELTA = (float) Math.ceil(0.05*Carte.cw);
		BLUR_RADIUS_MIN = (float) Math.ceil(0.02*Carte.cw);
		BLUR_RADIUS_DELTA = (float) Math.ceil(0.06*Carte.cw);
		pathWidth = STROKE_WIDTH_MIN;
		blurRadius = BLUR_RADIUS_MIN;
		solPath = null;
	}
	
	/**
	 * Spécifie un Path complet à dessiner (lors de la recherche de solution).
	 * @param rd ligne départ
	 * @param cd colonne départ
	 * @param moves les déplacements
	 */
	public void setPath(int rd, int cd, LinkedList<Solver.Move> moves) {
		solPath = new Path();
		dynaPos.clear();
		vachePos.clear();
		waitPos.clear();
		poussVachePosEtDir.clear();
		xc = (float) (cd*Carte.cw);
		yc = (float) (rd*Carte.ch);
		solPath.moveTo(xc + (float) (0.5*Carte.cw), yc + (float) (0.5*Carte.ch));
		for(Solver.Move m : moves) {
			curveTo(m);
		}
		fond.invalidate();
	}
	
	private RefreshHandler handler = new RefreshHandler(this);
	
	private static class RefreshHandler extends Handler {
		
		public static final int ANIM_PATH=1, BLUR_ANIM=2, ANIM_COLIB=3;
		
		private final WeakReference<PathViewer> pv_r;
		
		public RefreshHandler(PathViewer pv) {
			pv_r = new WeakReference<PathViewer>(pv);
		}
		
		public void step(int state, int moveTime) {
			sendMessageDelayed(obtainMessage(state), moveTime);
		}
		
		@Override
		public void handleMessage(Message msg) {
			if(pv_r.get()==null)
				return;
			switch(msg.what) {
			case ANIM_PATH:
				pv_r.get().animStepFast();
				break;
			case BLUR_ANIM:
				pv_r.get().animBlurPath();
				break;
			case ANIM_COLIB:
				pv_r.get().animStepColibri();
			}
		}
	};
	
	/**
	 * Appelée par onCancel dans Solver lorsque la recherche a été interrompue.
	 */
	public void cancelResearch() {
		mj.start();
		setVisibility(View.GONE);
		moves = null;
		movesIterator = null;
	}
	
	/**
	 * Spécifie un Path à animer (solution). Appelée par onPostExecute dans Solver.
	 * @param rd ligne départ
	 * @param cd colonne départ
	 * @param moves les déplacements
	 */
	public void setPathAndAnimate(int rd, int cd, LinkedList<Solver.Move> moves, boolean quickHint) {
		this.moves = moves;
		this.quickHint = quickHint;
		xd = (float) (cd*Carte.cw);
		yd = (float) (rd*Carte.ch);
		solPath = new Path();
		if(quickHint) {
			setBackgroundResource(0); // On enlève le filtre sombre
			blurProgress = -1;
			animBlurPath();
			return;
		}
		// Init phase 1 :
		moveTime = MOVE_TIME_FAST;
		movesIterator = moves.listIterator();
		dynaPos.clear();
		vachePos.clear();
		waitPos.clear();
		poussVachePosEtDir.clear();
		xc = xd; yc = yd;
		solPath.moveTo(xc + (float) (0.5*Carte.cw), yc + (float) (0.5*Carte.ch));
		animStepFast();
	}
	
	/**
	 * Animation phase 1 :
	 * Parcours rapide de la solution.
	 */
	private void animStepFast() {
		if(!movesIterator.hasNext()) {
			setBackgroundResource(0); // On enlève le filtre sombre
			// Init phase 2 :
			blurStep = 2*(float)DURATION_FRAME/(DURATION_BLUR_ANIM_BASE + DURATION_BLUR_ANIM_PROP*moves.size());
			blurProgress = blurStep;
			animBlurPath();
			return;
		}
		Solver.Move m = movesIterator.next();
		curveTo(m);
		fond.invalidate();
		moveTime *= TIME_FACTOR;
		handler.step(RefreshHandler.ANIM_PATH, moveTime);
	}
	
	/**
	 * Animation phase 2 :
	 * Animation de la largeur et du flou de la solution.
	 */
	private void animBlurPath() {
		if(blurProgress > 1f) { // Inverse le pas
			blurStep = -blurStep;
			blurProgress = 1f;
		} else if(blurProgress < 0f) {
			// Init phase 3 :
			moveTime = MOVE_TIME_COLIB;
			stepsCount = 0;
			movesIterator = moves.listIterator();
			solPath.reset();
			dynaPos.clear();
			vachePos.clear();
			waitPos.clear();
			poussVachePosEtDir.clear();
			xc = xd; yc = yd;
			solPath.moveTo(xc + (float) (0.5*Carte.cw), yc + (float) (0.5*Carte.ch));
			pathWidth = STROKE_WIDTH_MIN + STROKE_WIDTH_DELTA;
			blurRadius = BLUR_RADIUS_MIN;
			colibri.setLayoutParams(new RelativeLayout.LayoutParams((int) Carte.cw, (int) Carte.ch));
			colibri.setVisibility(View.VISIBLE);
			((AnimationDrawable) colibri.getBackground()).start();
			mj.state = MoteurJeu.SOL_READY;
			animStepColibri();
			return;
		}
		if(blurStep > 0)
			pathWidth = STROKE_WIDTH_MIN + interpolator.getInterpolation(blurProgress)*STROKE_WIDTH_DELTA;
		blurRadius = BLUR_RADIUS_MIN + interpolator.getInterpolation(blurProgress)*BLUR_RADIUS_DELTA;
		fond.invalidate();
		blurProgress += blurStep;
		handler.step(RefreshHandler.BLUR_ANIM, DURATION_FRAME);
	}
	
	/**
	 * Animation phase 3 :
	 * Parcours des prochains pas par le colibri.
	 */
	private void animStepColibri() {
		Solver.Move m = movesIterator.hasNext() ? movesIterator.next() : null;
		if(m==null || stepsCount >= STEPS_COLIBRI && m.wait!=-1) {
			// Fin de l'animation !
			if(mj.state==MoteurJeu.SOL_READY)
				mj.start();
			showAndFadeOut();
			((AnimationDrawable) colibri.getBackground()).stop();
			moves = null;
			movesIterator = null;
			return;
		}
		float xd = xc, yd = yc;
		curveTo(m);
		fond.invalidate();
		// Anime le colibri vers la prochaine destination
		if(m.wait==-1) { // Téléportation arc-en-ciel 
			if(m.travel==0) stepsCount--; // Seulement si le Move a un reste après l'arc
			animStepColibri();
			return;
		}
		TranslateAnimation ta = new TranslateAnimation(Animation.ABSOLUTE, xd, Animation.ABSOLUTE, xc, Animation.ABSOLUTE, yd, Animation.ABSOLUTE, yc);
		ta.setDuration(moveTime);
		ta.setFillAfter(true);
		colibri.startAnimation(ta);
		moveTime *= TIME_FACTOR;
		stepsCount++;
		handler.step(RefreshHandler.ANIM_COLIB, moveTime);
	}
	
	/**
	 * Trace la courbe de Bézier pour le mouvement m, utilisant (xc, yc) pour
	 * point de contrôle.
	 * @param m mouvement
	 */
	private void curveTo(Solver.Move m) {
		int dir = Math.abs(m.direction);
		float dxf = (dir%3==1 ? 0.5f : (dir==Solver.Move.RIGHT ? 0f : 1f));
		float dyf = (dir%3!=1 ? 0.5f : (dir==Solver.Move.DOWN ? 0f : 1f));
		float xf = (float) (m.posFinale.c*Carte.cw);
		float yf = (float) (m.posFinale.r*Carte.ch);
		if(m.wait==-1) { // Téléportation arc-en-ciel
			solPath.moveTo(xf + (float) (0.5*Carte.cw), yf + (float) (0.5*Carte.ch));
		} else if(m.direction>=10) { // Dynamite
			Solver.Position p_dyna = m.posFinale.next(m.direction-10);
			dynaPos.add(new int[] {(int) ((p_dyna.c-0.25)*Carte.cw), (int) (p_dyna.r*Carte.ch)});
		} else { // Move normal
			xc = xc + ((float) ((0.3 + 0.4*Math.random())*Carte.cw));
			yc = yc + ((float) ((0.3 + 0.4*Math.random())*Carte.ch));
			solPath.cubicTo(xc, yc, xc, yc, xf + dxf*((float) Carte.cw), yf + dyf*((float) Carte.ch));
		}
		if(m.direction < 0) { // Poussée de Vache
			Solver.Position p_vache = m.posFinale.prev(dir);
			Matrix mat = new Matrix();
			int deg_rot = 0;
			switch(dir) {
				case Solver.Move.UP:
					deg_rot = 270;
					break;
				case Solver.Move.LEFT:
					deg_rot = 180;
					break;
				case Solver.Move.DOWN:
					deg_rot = 90;
			}
			mat.setRotate(deg_rot, (float)(Carte.cw*0.5/2), (float)(Carte.ch*0.5/2));
			mat.postTranslate((float)((p_vache.c - 0.3f + 1.1f*(1f-dxf))*Carte.cw), (float)((p_vache.r - 0.3f + 1.1f*(1f-dyf))*Carte.ch));
			poussVachePosEtDir.add(mat);
			vachePos.add(new int[] {(int) ((p_vache.c-0.125)*Carte.cw), (int) ((p_vache.r-0.125)*Carte.ch)});
		} else if(m.step==-1) { // Arrêt contre Vache
			Solver.Position p_vache = m.posFinale.next(m.direction);
			vachePos.add(new int[] {(int) ((p_vache.c-0.125)*Carte.cw), (int) ((p_vache.r-0.125)*Carte.ch)});
		}
		if(m.wait > 2) { // Attente
			waitPos.add(new int[] {(int) (xc-0.3*Carte.cw), (int) (yc-0.3*Carte.ch)});
		}
		xc = xf; yc = yf;
	}
	
	/**
	 * Makes the view visible for a delay and disappear.
	 */
	private void showAndFadeOut() {
		startAnimation(anim);
	}
}
