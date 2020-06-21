package com.game.colibri;

import java.util.LinkedList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;

/**
 * RelativeLayout gérant l'affichage complet d'un niveau.
 * Le layout contient un fond sur lequel est dessiné la carte (statique) que l'on rafraîchit
 * seulement lorsque nécessaire avec fond.invalidate() (commandé par le moteur de jeu avec "fond.invalidate()").
 * Il contient aussi les éléments mobiles sous forme de View. (Animal)
 * On spécifie le niveau à afficher par la méthode publique loadNiveau.
 * Nécessité de rafraîchir fond à chaque élément ramassé.
 */
public class Carte extends RelativeLayout {
	
	public static int ww,wh; // windowWidth/Height
	public static double cw,ch; // caseWidth/Height en pixels
	public static final int LIG=12, COL=20;
	
	private double cwi,chi; // cwi & chi de l'instance
	public Niveau niv=null; // Le niveau à afficher
	public int n_fleur,n_dyna; // Le nombre de fleurs sur la carte et le nombre de dynamites ramassées.
	private Bitmap menhir,fleur,fleurm,dyna,menhir_rouge,rainbow;
	public Colibri colibri;
	public LinkedList<Vache> vaches = new LinkedList<>(); // La liste des vaches du niveau
	public LinkedList<Chat> chats = new LinkedList<>(); // La liste des chats du niveau
	public LinkedList<Dynamite> dynamites = new LinkedList<>(); // La liste des dynamites posées
	public SparseArray<int[]> rainbows = new SparseArray<>();
	private int[] colors = new int[] {0x50FAE96C, 0x50552DA2, 0x502FE0D6, 0x50FA9B44, 0x50FA95E5, 0x50F92722, 0x5000FF54};
	public View mort,sang,fond;
	private RelativeLayout dynaLayer;

    public Carte(Context context, AttributeSet attrs) {
        super(context, attrs);
        loadImg(context);
    }

    public Carte(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        loadImg(context);
    }

    private void loadImg(Context context) {
    	mort = new View(context);
    	mort.setBackgroundResource(R.drawable.skull);
    	sang = new View(context);
    	sang.setBackgroundResource(R.drawable.sang);
    	// Définition du fond qui comporte la carte statique.
    	final Paint mPaint = new Paint();
    	mPaint.setStyle(Paint.Style.STROKE);
    	mPaint.setStrokeJoin(Paint.Join.ROUND);
    	mPaint.setStrokeCap(Paint.Cap.ROUND);
    	mPaint.setStrokeWidth(5);
    			fond=new View(this.getContext()) {
    	    		// Dessin du canvas : événement déclenché par fond.invalidate()
    	    	    @Override
    	    	    protected void onDraw(Canvas can) {
    	    	    	if (niv!=null) {
    	    	    		mPaint.setColor(0x708B4500);
    	    	    		for(Vache v : vaches) {
    	    	    			for(int i=1; i<=3; i++) {
    	    	    				mPaint.setStrokeWidth((float) ((0.4-i*0.1)*cwi));
    	    	    				can.drawPath(v.path, mPaint);
    	    	    			}
    	    	    		}
    	    	    		for(Chat c : chats) {
    	    	    			for(int i=1; i<=3; i++) {
    	    	    				mPaint.setStrokeWidth((float) ((0.4-i*0.1)*cwi));
    	    	    				can.drawPath(c.path, mPaint);
    	    	    			}
    	    	    		}
    	    		    	for (int l=0; l<LIG; l++) {
    	    		    		for (int c=0; c<COL; c++) {
    	    		    			if (niv.carte[l][c]==1)
    	    		    				can.drawBitmap(menhir, (int)(c*cwi-cwi/8), (int)(l*chi), null);
    	    		    			else if (niv.carte[l][c]==2)
    	    		    				can.drawBitmap(fleur, (int)(c*cwi), (int)(l*chi), null);
    	    		    			else if (niv.carte[l][c]==3)
    	    		    				can.drawBitmap(fleurm, (int)(c*cwi), (int)(l*chi), null);
    	    		    			else if (niv.carte[l][c]==4)
    	    		    				can.drawBitmap(dyna, (int)(c*cwi), (int)(l*chi), null);
    	    		    			else if (niv.carte[l][c]==5)
    	    		    				can.drawBitmap(menhir_rouge, (int)(c*cwi-cwi/8), (int)(l*chi), null);
    	    		    			else if (niv.carte[l][c]>=10) {
    	    		    				mPaint.setColor(colors[(niv.carte[l][c]-10)%colors.length]);
    	    		    				for(int i=1; i<=4; i++) {
    	    	    	    				mPaint.setStrokeWidth((float) ((0.8-i*0.15)*cwi));
    	    	    	    				can.drawPoint((float) (cwi*(c+0.5)), (float) (chi*(l+0.7)), mPaint);
    	    		    				}
    	    		    				can.drawBitmap(rainbow, (int)(c*cwi-cwi/4), (int)(l*chi), null);
    	    		    			}
    	    		    		}
    	    		    	}
    	    	    	}
    	    	    }
    	    	};
    }

    /**
     * Charge un niveau sur la carte
     * @param niveau le niveau à charger
     */
    public void loadNiveau(Niveau niveau) {
    	// Supprimer les "Animaux" du niveau précédent.
		removeAllViews();
		vaches.clear();
		chats.clear();
		dynamites.clear();
		rainbows.clear();

		cw = cwi; ch = chi; // 
		niv=niveau;
		addView(fond);
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ww,wh);
	    fond.setLayoutParams(params);
		dynaLayer = new RelativeLayout(getContext());
		addView(dynaLayer);
	    dynaLayer.setLayoutParams(params);
	    
	    /*
	    Random ran = new Random();
	    for(int i=0; i<colors.length; i++) {
	    	colors[i] = Color.argb(0x40, 56+ran.nextInt(200), 56+ran.nextInt(200), 56+ran.nextInt(200));
	    }
	    */
    	n_dyna=0;
    	n_fleur=0;
    	for(int l=0; l<LIG; l++) {
    		for(int c=0; c<COL; c++) {
    			if(niv.carte[l][c]==2 || niv.carte[l][c]==3)
    				n_fleur++;
    			else if(niv.carte[l][c]>=10) {
    				int[] autre = rainbows.get(niv.carte[l][c]);
    				if(autre!=null) { // On ajoute la case correspondante à celle déjà enregistrée.
    					autre[2]=l;
    					autre[3]=c;
    				} else {
    					rainbows.put(niv.carte[l][c] , new int[] {l,c,0,0});
    				}
    			}
    		}
    	}
    	colibri = new Colibri(this.getContext(), niv.db_c, niv.db_l, 1, 1);
    	addView(colibri);
    	addView(sang);
    	sang.setVisibility(INVISIBLE);
    	// On crée les vaches et les chats
    	for (int[][] itin : niv.vaches) {
    		vaches.addLast(new Vache(this.getContext(), 1, 1, itin));
    		addView(vaches.getLast());
    	}
    	for (int[][] itin : niv.chats) {
    		chats.addLast(new Chat(this.getContext(), 1, 1.25, itin));
    		addView(chats.getLast());
    	}
    	addView(mort);
    	mort.setVisibility(INVISIBLE);
    	this.invalidate();
    }
    
    /**
     * Effectue l'animation de mort du colibri. :( (flaque de sang qui se répend + tête de mort qui s'envole)
     */
    public void animMort() {
    	RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams((int)(3*cwi/2), (int)(3*chi/2));
    	double[] pos = colibri.getPos();
		params.leftMargin = (int)(cwi*pos[0]-cwi/4);
	    params.topMargin = (int)(chi*pos[1]-chi/4);
	    mort.setLayoutParams(params);
	    mort.setVisibility(VISIBLE);
	    sang.setLayoutParams(params);
	    sang.setVisibility(VISIBLE);
    	mort.startAnimation(AnimationUtils.loadAnimation(this.getContext(), R.anim.dead_anim));
    	sang.startAnimation(AnimationUtils.loadAnimation(this.getContext(), R.anim.blood_anim));
    }

    public void poseDynamite(int l, int c, int frame) {
    	dynamites.addLast(new Dynamite(this.getContext(), cwi, chi, l, c, frame));
    	dynaLayer.addView(dynamites.getLast());
    }

    public void cancelLastDynamite() {
    	dynaLayer.removeView(dynamites.pollLast());
	}
    
    // Événement utilisé pour récupérer les dimensions de la View.
    /* (non-Javadoc)
     * @see android.view.View#onSizeChanged(int, int, int, int)
     */
    @Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		ww=super.getWidth();
		wh=super.getHeight();
		cwi=((double)ww)/COL;
		chi=((double)wh)/LIG;
		menhir = Bitmap.createScaledBitmap(((BitmapDrawable) getResources().getDrawable(R.drawable.menhir)).getBitmap(), (int)(5*cwi/4), (int)(5*chi/4), true);
		fleur = Bitmap.createScaledBitmap(((BitmapDrawable) getResources().getDrawable(R.drawable.fleur)).getBitmap(), (int)cwi, (int)chi, true);
		fleurm = Bitmap.createScaledBitmap(((BitmapDrawable) getResources().getDrawable(R.drawable.fleurm)).getBitmap(), (int)cwi, (int)chi, true);
		dyna = Bitmap.createScaledBitmap(((BitmapDrawable) getResources().getDrawable(R.drawable.dynamite)).getBitmap(), (int)cwi, (int)chi, true);
		menhir_rouge = Bitmap.createScaledBitmap(((BitmapDrawable) getResources().getDrawable(R.drawable.menhir_rouge)).getBitmap(), (int)(5*cwi/4), (int)(5*chi/4), true);
		rainbow = Bitmap.createScaledBitmap(((BitmapDrawable) getResources().getDrawable(R.drawable.rainbow)).getBitmap(), (int)(3*cwi/2), (int)chi, true);
    	this.invalidate();
	}
}
