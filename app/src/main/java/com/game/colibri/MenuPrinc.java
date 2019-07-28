package com.game.colibri;

import java.io.IOException;

import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewStub;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;

/**
 * Menu principal : activité lancée au démarage.
 */
public class MenuPrinc extends Activity {
	
	public int ww,wh;
	private int n_niv; // Niveau sélectionné dans Campagne
	private boolean brandNew=true;
	public int screen=0; // Définit quel écran est affiché : 0:menu, 1:choix niveaux, 2:infos, 3:instructions. Remplace l'utilisation de ViewFlipper qui pouvait causer des outOfMemoryError.
	private RelativeLayout root;
	private ViewFlipper MenuSel;
	private Carte carte; // L'instance de carte permettant de faire un apercu dans le menu de sélection de niveaux.
	private LinearLayout opt_aleat;
	private LinearLayout opt_infos;
	private View instrus, infos, coliBrainsInfos;
	private float initialX;
	private double[][] points = new double[][] {{0.07625, 0.8145833333333333}, {0.18875, 0.7645833333333333}, {0.31625, 0.7354166666666667}, {0.24875, 0.8208333333333333}, {0.1125, 0.94375}, {0.25, 0.9458333333333333}, {0.405, 0.9208333333333333}, {0.52, 0.9416666666666667}, {0.6275, 0.9333333333333333}, {0.765, 0.9354166666666667}, {0.765, 0.8166666666666667}, {0.83, 0.74375}};
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_menu_princ);
		root = (RelativeLayout) findViewById(R.id.root);
		n_niv = MyApp.avancement;
		PushNotificationService.createNorificationsChannels(this);
		// Lancé depuis une notification ?
		if (getIntent().getExtras() != null && getIntent().getExtras().getString("com.game.colibri.notification") != null) {
			((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancelAll();
			multijoueur(null);
		}
		// ATTENTION: This was auto-generated to handle app links.
		/*Intent appLinkIntent = getIntent();
		String appLinkAction = appLinkIntent.getAction();
		Uri appLinkData = appLinkIntent.getData();*/
	}
	
	@Override
	protected void onStart() {
		MyApp.resumeActivity();
		super.onStart();
	}
	
	@Override
	protected void onStop() {
		MyApp.stopActivity();
		super.onStop();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode==1 && screen==0) { // On met à jour le niveau d'expérience.
			TextView exp = (TextView) findViewById(R.id.exp_menu);
			if(exp!=null) { // En retour de notification, l'affichage de MenuPrinc n'a pas été effectué
				exp.setText(getString(R.string.exp)+" : "+String.format("%,d", MyApp.experience));
				exp.startAnimation(AnimationUtils.loadAnimation(MenuPrinc.this, R.anim.aleat_opt_anim));
				findViewById(R.id.coupe).setVisibility(MyApp.avancement>Jeu.NIV_MAX ? View.VISIBLE : View.GONE);
				TextView coliBrains = (TextView) findViewById(R.id.colibrains_home);
				coliBrains.setText("\n/"+MyApp.maxCB);
				((ColiBrain) coliBrains.getCompoundDrawables()[0])
					.setProgress(MyApp.expProgCB/(float)MyApp.EXP_LEVEL_PER_COLI_BRAIN)
					.setText(""+MyApp.coliBrains);
			}
		} else if(requestCode==3) { // Retour des artifices
			Toast toast = Toast.makeText(MenuPrinc.this, R.string.toast_fin2, Toast.LENGTH_LONG);
	    	TextView tv = (TextView) toast.getView().findViewById(android.R.id.message);
	    	if( tv != null) tv.setGravity(Gravity.CENTER);
	    	toast.show();
		}
		if(resultCode==2) { // Lancement artifices !
			finCampagne(null);
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	/**
	 * Pour afficher le menu principal dans root
	 */
	private void displayMenu() {
		delReferences();
		screen=0;
		root.removeAllViews();
		System.gc();
		root.addView(View.inflate(this, R.layout.menu_princ, null));
		opt_aleat = (LinearLayout) findViewById(R.id.opt_aleat);
		opt_infos = (LinearLayout) findViewById(R.id.opt_infos);
		final Typeface font = Typeface.createFromAsset(getAssets(),"fonts/Passing Notes.ttf");
		// Instrus
		instrus = findViewById(R.id.instrus);
		((ViewStub) instrus).setOnInflateListener(new ViewStub.OnInflateListener() {
			@Override
			public void onInflate(ViewStub stub, View inflated) {
				((TextView) inflated.findViewById(R.id.titreInstru)).setTypeface(font);
				((TextView) inflated.findViewById(R.id.instru1)).setTypeface(font);
				((TextView) inflated.findViewById(R.id.instru2)).setTypeface(font);
				((TextView) inflated.findViewById(R.id.instru3)).setTypeface(font);
				((TextView) inflated.findViewById(R.id.quitInstru)).setTypeface(font);
				instrus = inflated;
			}
		});
		// Infos / A propos
		infos = findViewById(R.id.a_propos);
		((ViewStub) infos).setOnInflateListener(new ViewStub.OnInflateListener() {
			@Override
			public void onInflate(ViewStub stub, View inflated) {
				((TextView) inflated.findViewById(R.id.titreInfos)).setTypeface(font);
				((TextView) inflated.findViewById(R.id.infos1)).setTypeface(font);
				((TextView) inflated.findViewById(R.id.infos2)).setTypeface(font);
				((TextView) inflated.findViewById(R.id.quitInfos)).setTypeface(font);
				infos = inflated;
			}
		});
		// ColiBrains infos
		coliBrainsInfos = findViewById(R.id.colibrains_infos);
		((ViewStub) coliBrainsInfos).setOnInflateListener(new ViewStub.OnInflateListener() {
			@Override
			public void onInflate(ViewStub stub, View inflated) {
				((TextView) inflated.findViewById(R.id.titreColibrains)).setTypeface(font);
				((TextView) inflated.findViewById(R.id.quitColibrains)).setTypeface(font);
				LinearLayout content = ((LinearLayout) inflated.findViewById(R.id.contentColibrains));
				for(int i=0; i < content.getChildCount(); i++) {
					View v = content.getChildAt(i);
					if(v instanceof TextView) {
						((TextView) v).setTypeface(font);
					}
				}
				ColiBrain cb = new ColiBrain(MenuPrinc.this, "2", 0.6f);
				((ImageView) findViewById(R.id.limitedColibrains)).setImageDrawable(cb);
				cb = new ColiBrain(MenuPrinc.this, "∞", 0);
				((ImageView) findViewById(R.id.unlimitedColibrains)).setImageDrawable(cb);
				coliBrainsInfos = inflated;
			}
		});
		placeButton();
		if(MyApp.avancement==1) {
			((TextView) findViewById(R.id.bout1)).setText(R.string.commencer);
		} else if(MyApp.avancement==Jeu.NIV_MAX+1) {
			findViewById(R.id.coupe).setVisibility(View.VISIBLE);
		}
		if(!MyApp.getApp().pref.getBoolean("musique", true))
			((ImageButton) findViewById(R.id.bout_musique)).setImageResource(R.drawable.nosound);
		// Display experience
		TextView exp = (TextView) findViewById(R.id.exp_menu);
		exp.setText(getString(R.string.exp)+" : "+String.format("%,d", MyApp.experience));
		exp.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/Passing Notes.ttf"));
		// Display ColiBrains
		ColiBrain coliBrainDrawable = new ColiBrain(this, ""+MyApp.coliBrains, MyApp.expProgCB/(float)MyApp.EXP_LEVEL_PER_COLI_BRAIN);
		TextView coliBrains = (TextView) findViewById(R.id.colibrains_home);
		coliBrains.setText("\n/"+MyApp.maxCB);
		coliBrains.setCompoundDrawables(coliBrainDrawable, null, null, null);
		// Title animation and font
		TextView title = (TextView) findViewById(R.id.main_title);
		title.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/Sketch_Block.ttf"));
		Animation a = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
		a.setDuration(4000);
		title.setAnimation(a);
		// ColiBrains animation
		Animation a2 = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
		a2.setDuration(2000);
		findViewById(R.id.colibrains_home).setAnimation(a2);
	}
	
	/**
	 * Pour afficher l'écran de choix de niveaux dans root
	 */
	private void displayChoixNiveaux() {
		delReferences();
		screen=1;
		root.removeAllViews();
		root.addView(View.inflate(this, R.layout.choix_niveaux, null));
		MenuSel = (ViewFlipper) findViewById(R.id.flipper);
        MenuSel.setInAnimation(this, android.R.anim.fade_in);
        MenuSel.setOutAnimation(this, android.R.anim.fade_out);
        carte = (Carte) findViewById(R.id.apercu);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ww/2,wh/2);
		params.leftMargin = (int) (ww/4);
	    params.topMargin = (int) (wh/12);
	    carte.setLayoutParams(params);
	}
	
	private void delReferences() {
		opt_aleat = null;
		opt_infos = null;
		instrus = null;
		infos = null;
		coliBrainsInfos = null;
		MenuSel = null;
		carte = null;
	}

	// Le placement des boutons est calculé ici en fonction des dimensions de l'écran. (Astuce pour contourner le temps d'établissement de l'affichage empêchant ces opérations dans le onCreate)
	/* (non-Javadoc)
	 * @see android.app.Activity#onWindowFocusChanged(boolean)
	 */
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		ww = root.getWidth();
		wh = root.getHeight();
		if(brandNew) {
			brandNew=false;
			displayMenu();
		}
		if(screen==1 && hasFocus)
			campagne(carte); // Permet de rafraîchir la progression lorsque l'on quitte le jeu et revient au menu de sélection.
	}
	
	/**
	 * Place le premier bouton à la position voulue. Les autres sont placés par rapport à lui. Redimensionne chaque bouton à la largeur/hauteur voulue.
	 */
	private void placeButton() {
		int[] boutons = new int[] {R.id.bout1,R.id.bout2,R.id.bout3,R.id.bout4,R.id.bout5};
		Typeface font = Typeface.createFromAsset(getAssets(),"fonts/Passing Notes.ttf");
		for(int i=0; i<boutons.length; i++) {
			Button btn_lay = (Button)findViewById(boutons[i]);
			RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) btn_lay.getLayoutParams();
			if(i==0) {
				layoutParams.leftMargin = ww*5/9;
			    layoutParams.topMargin = wh/4;
			}
		    layoutParams.width = 4*ww/10;
		    layoutParams.height = 23*wh/250;
		    btn_lay.setLayoutParams(layoutParams);
		    btn_lay.setTypeface(font);
		    Animation a = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
		    a.setStartOffset(800+i*150);
		    a.setDuration(1000);
		    btn_lay.setAnimation(a);
		}
		for(int i=0; i<opt_aleat.getChildCount(); i++) {
			((Button) opt_aleat.getChildAt(i)).setTypeface(font);
		}
		for(int i=0; i<opt_infos.getChildCount(); i++) {
			((Button) opt_infos.getChildAt(i)).setTypeface(font);
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK) {
			if(screen!=0) {
				displayMenu();
			}
			else if (opt_infos.getVisibility()==View.VISIBLE) {
				opt_infos.setVisibility(View.INVISIBLE);
			}
			else if (coliBrainsInfos.getVisibility()==View.VISIBLE) {
				quitColiBrainsInfos(null);
			}
			else if (infos.getVisibility()==View.VISIBLE) {
				quitInfos(null);
			}
			else if (instrus.getVisibility()==View.VISIBLE) {
				quitInstrus(null);
			}
			else if(opt_aleat.getVisibility()==View.INVISIBLE) {
				MyApp.getApp().releaseMusic();
				this.finish(); // On quitte le jeu !
			} else {
				// Animation pour rétablir le menu.
				Animation a2 = AnimationUtils.loadAnimation(MenuPrinc.this, R.anim.menu_right);
    			opt_aleat.startAnimation(a2);
				Button btn_aleat = (Button)findViewById(R.id.bout3);
				btn_aleat.startAnimation(AnimationUtils.loadAnimation(this, R.anim.bouton_aleat_down));
				for(int i=0; i<opt_aleat.getChildCount(); i++) {
    				((Button) opt_aleat.getChildAt(i)).setClickable(false);
				}
    			a2.setAnimationListener(new AnimationListener() {
    	    		public void onAnimationStart(Animation an) {
    	    			
    	    		}
    	    		public void onAnimationRepeat(Animation an) {
    	    			
    	    		}
    	    		public void onAnimationEnd(Animation an) { // On réaffiche le menu principal une fois que les déplacements sont finis
						opt_aleat.setVisibility(View.INVISIBLE);
						Animation a = AnimationUtils.loadAnimation(MenuPrinc.this, R.anim.aleat_opt_anim);
						((Button)findViewById(R.id.bout1)).startAnimation(a);
						((Button)findViewById(R.id.bout2)).startAnimation(a);
						((Button)findViewById(R.id.bout4)).startAnimation(a);
						((Button)findViewById(R.id.bout5)).startAnimation(a);
						((Button)findViewById(R.id.bout1)).setClickable(true);
						((Button)findViewById(R.id.bout2)).setClickable(true);
						((Button)findViewById(R.id.bout3)).setClickable(true);
						((Button)findViewById(R.id.bout4)).setClickable(true);
						((Button)findViewById(R.id.bout5)).setClickable(true);
    	    		}
    	    	});
			}
			return true;
		} else
			return false;
	}
	
	/**
	 * Swipe de MenuSel
	 */
	@Override
    public boolean onTouchEvent(MotionEvent touchevent) {
        if (screen==1) {
	        switch (touchevent.getAction()) {
	        case MotionEvent.ACTION_DOWN:
	            initialX = touchevent.getX();
	            break;
	        case MotionEvent.ACTION_UP:
	            float finalX = touchevent.getX();
	            if (initialX-finalX > 10) {
	            	nextScreen(findViewById(R.id.nextScreen));
	            } else if (initialX-finalX < -10) {
	                prevScreen(findViewById(R.id.prevScreen));
	            }
	        }
        }
        return false;
	}
	
	public void prevScreen(View v) {
		int a=MenuSel.getDisplayedChild();
		if(a>0) {
			if(a==1)
				v.setVisibility(View.GONE);
			else
				findViewById(R.id.nextScreen).setVisibility(View.VISIBLE);
			MenuSel.showPrevious();
			hideApercu();
		}
	}
	
	public void nextScreen(View v) {
		int a=MenuSel.getDisplayedChild();
		if(a<2) {
			if(a==1)
				v.setVisibility(View.GONE);
			else
				findViewById(R.id.prevScreen).setVisibility(View.VISIBLE);
			MenuSel.showNext();
			hideApercu();
		}
	}
	
	private void hideApercu() {
		if(carte.getVisibility()==View.VISIBLE) {
            carte.setAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
			carte.setVisibility(View.INVISIBLE);
			findViewById(R.id.n_niv).setVisibility(View.GONE);
			deplaceColibri(new double[] {0.1,0.6},true);
        }
	}
	
	/**
	 * Lance l'activité Resultats pour le feu d'artifice de fin de campagne ! :3
	 * @param v
	 */
	public void finCampagne(View v) {
		startActivityForResult(new Intent(this, Resultats.class), 3);
	}
	
	/**
	 * Fonctions appelées par le "onClick" des boutons définis dans activity_menu.xml
	 * 		@param v le bouton appuyé.
	 */
	public void continuer(View v) {
		opt_infos.setVisibility(View.INVISIBLE);
		if(MyApp.avancement==1)
			((TextView) findViewById(R.id.bout1)).setText(R.string.jouer);
		Intent intent = new Intent(this, Jeu.class);
		intent.putExtra("mode", Niveau.CAMPAGNE);
		intent.putExtra("n_niv", Math.min(MyApp.avancement,Jeu.NIV_MAX));
		startActivityForResult(intent, 1);
	}
	
	public void campagne(View v) {
		displayChoixNiveaux();
		((TextView) findViewById(R.id.n_niv)).setTypeface(Typeface.createFromAsset(getAssets(),"fonts/Sketch_Block.ttf"));
		RelativeLayout lay_sel;
		Button img;
		RelativeLayout.LayoutParams params;
		Colibri coli = (Colibri) findViewById(R.id.coli);
		params=new RelativeLayout.LayoutParams(ww/17,wh/10);
		params.leftMargin = ww/10;
	    params.topMargin = wh/2;
	    coli.setLayoutParams(params);
	    coli.savePosFinAnim(ww/10, wh/2, false);
	    coli.mx=1;
    	coli.setSpriteDirection();
		coli.start();
		int[] r = new int[] {R.id.menu_selection_1,R.id.menu_selection_2,R.id.menu_selection_3};
		for(int ecran=0; ecran<3; ecran++) {
			lay_sel = (RelativeLayout) findViewById(r[ecran]);
			lay_sel.removeAllViews();
			for(int i=0; i<points.length; i++) {
				img = new Button(this);
				if(ecran*points.length+i+1 >= MyApp.avancement)
					img.setBackgroundResource(R.drawable.fleur);
				else if(Math.random()<0.5)
					img.setBackgroundResource(R.drawable.emplacement1);
				else
					img.setBackgroundResource(R.drawable.emplacement2);
				int d;
				if(i<6) d=wh/12;
				else d=wh/10;
				params = new RelativeLayout.LayoutParams(d,d);
				params.leftMargin = (int) (ww*points[i][0]-d/2);
			    params.topMargin = (int) (wh*points[i][1]-d/2);
			    img.setLayoutParams(params);
			    img.setId(ecran*points.length+i+1);
			    img.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						if(v.getId()<=MyApp.avancement) setApercu(v.getId());
					}
			    });
			    if(ecran*points.length+i+1 <= MyApp.avancement) {
					Animation a = AnimationUtils.loadAnimation(this, R.anim.dilat_anim);
					a.setStartOffset(i*10);
					img.startAnimation(a);
			    }
			    lay_sel.addView(img);
			}
		}
		int iEcran = (n_niv-1)/points.length;
		if(iEcran>2) iEcran=0;
		MenuSel.setDisplayedChild(iEcran);
		findViewById(R.id.prevScreen).setVisibility(iEcran==0 ? View.GONE : View.VISIBLE);
		findViewById(R.id.nextScreen).setVisibility(iEcran==2 ? View.GONE : View.VISIBLE);
	}
	
	/**
	 * Charge le niveau d'index n_niv dans l'aperçu et déplace le colibri sur le point.
	 * @param n_niv l'index du niveau à afficher
	 */
	private void setApercu(int n) {
		if(n!=n_niv || carte.getVisibility()==View.INVISIBLE) deplaceColibri(points[(n-1)%points.length],false);
		if(carte.getVisibility()==View.INVISIBLE) {
			carte.setAnimation(AnimationUtils.loadAnimation(this, R.anim.aleat_opt_anim));
			carte.setVisibility(View.VISIBLE);
		}
		n_niv=n;
		TextView textN_niv = (TextView) findViewById(R.id.n_niv);
		textN_niv.setVisibility(View.VISIBLE);
		textN_niv.setText(n_niv+" / "+Jeu.NIV_MAX);
		Niveau niv;
		try {
			niv = new Niveau(MenuPrinc.this.getAssets().open("niveaux/niveau"+n_niv+".txt"));
			carte.loadNiveau(niv);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private void deplaceColibri(double[] co, boolean regardeVersDroite) {
		Colibri coli = (Colibri) findViewById(R.id.coli);
		if(coli.getAnimation()!=null && !coli.getAnimation().hasEnded()) // Au cas où l'animation n'était pas terminée.
			coli.setPosFinAnim();
		RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) coli.getLayoutParams();
		int xf=(int) (ww*co[0])-params.width/2, yf=(int) (wh*co[1])-3*params.height/4;
	    TranslateAnimation anim = new TranslateAnimation(0,xf-params.leftMargin,0,yf-params.topMargin);
	    anim.setDuration(800);
	    anim.setInterpolator(new AccelerateDecelerateInterpolator());
	    coli.savePosFinAnim(xf,yf,regardeVersDroite);
    	coli.mx=(int) Math.signum(xf-params.leftMargin);
    	coli.setSpriteDirection();
	    coli.setAnimation(anim);
	}
	
	public void aleatoire(View v) {
		opt_infos.setVisibility(View.INVISIBLE);
		((Button)findViewById(R.id.bout1)).setClickable(false);
		((Button)findViewById(R.id.bout2)).setClickable(false);
		((Button)findViewById(R.id.bout4)).setClickable(false);
		((Button)findViewById(R.id.bout5)).setClickable(false);
		Animation a = AnimationUtils.loadAnimation(this, R.anim.menu_right);
		((Button)findViewById(R.id.bout1)).startAnimation(a);
		((Button)findViewById(R.id.bout2)).startAnimation(a);
		((Button)findViewById(R.id.bout4)).startAnimation(a);
		((Button)findViewById(R.id.bout5)).startAnimation(a);
		Button btn_aleat = (Button)findViewById(R.id.bout3);
		btn_aleat.setClickable(false);
		btn_aleat.startAnimation(AnimationUtils.loadAnimation(this, R.anim.bouton_aleat_up));
		(btn_aleat.getAnimation()).setAnimationListener(new AnimationListener() {
    		public void onAnimationStart(Animation an) {
    			
    		}
    		public void onAnimationRepeat(Animation an) {
    			
    		}
    		public void onAnimationEnd(Animation an) { // On affiche les options une fois que les déplacements sont finis
    			Button btn_aleat = (Button)findViewById(R.id.bout3);
    			int w=btn_aleat.getWidth(), h=btn_aleat.getHeight();
    			RelativeLayout.LayoutParams p = (RelativeLayout.LayoutParams) opt_aleat.getLayoutParams();
    			p.width=3*w/4;
    			p.height=opt_aleat.getChildCount()*h;
    			p.leftMargin=w/8;
    			opt_aleat.setLayoutParams(p);
    			Animation a2 = AnimationUtils.loadAnimation(MenuPrinc.this, R.anim.aleat_opt_anim);
    			opt_aleat.startAnimation(a2);
				for(int i=0; i<opt_aleat.getChildCount(); i++) {
    				((Button) opt_aleat.getChildAt(i)).setClickable(true);
				}
    			opt_aleat.setVisibility(View.VISIBLE);
    		}
    	});
	}
	
	public void launchNiv(View v) {
		Intent intent = new Intent(this, Jeu.class);
		intent.putExtra("mode", Niveau.CAMPAGNE);
		intent.putExtra("n_niv", n_niv);
		startActivityForResult(intent, 2);
	}
	
	public void paramAleat(View v) {
		ParamAleat pa = new ParamAleat(new ParamAleat.callBackInterface() {
			@Override
			public void launchFunction(int mode) {
				launchAleat(mode);
			}
		}, this, MyApp.avancement);
		pa.show(); // Si appui sur "OK", lance un niveau aléatoire en mode PERSO.
	}
	
	public void facile(View v) {
		launchAleat(Niveau.FACILE);
	}
	
	public void moyen(View v) {
		launchAleat(Niveau.MOYEN);
	}

	public void difficile(View v) {
		launchAleat(Niveau.DIFFICILE);
	}
	
	public void launchAleat(int mode) {
		Intent intent = new Intent(this, Jeu.class);
		intent.putExtra("mode", mode);
		startActivityForResult(intent, 1);
	}
	
	public void multijoueur(View v) {
		if(opt_infos!=null)
			opt_infos.setVisibility(View.INVISIBLE);
		startActivityForResult(new Intent(this, Multijoueur.class), 1);
	}
	
	public void classements(View v) {
		opt_infos.setVisibility(View.INVISIBLE);
		startActivity(new Intent(this, Classements.class));
	}
	
	public void reglages(View v) {
		if (opt_infos.getVisibility()==View.INVISIBLE) {
			opt_infos.setVisibility(View.VISIBLE);
			opt_infos.startAnimation(AnimationUtils.loadAnimation(this, R.anim.aleat_opt_anim));
		} else
			opt_infos.setVisibility(View.INVISIBLE);
	}
	
	public void infos(View v) {
		opt_infos.setVisibility(View.INVISIBLE);
		infos.setVisibility(View.VISIBLE);
		infos.startAnimation(AnimationUtils.loadAnimation(this, R.anim.aleat_opt_anim));
	}
	
	public void quitInfos(View v) {
		infos.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right));
		infos.setVisibility(View.GONE);
	}
	
	public void instrus(View v) {
		opt_infos.setVisibility(View.INVISIBLE);
		instrus.setVisibility(View.VISIBLE);
		instrus.startAnimation(AnimationUtils.loadAnimation(this, R.anim.aleat_opt_anim));
	}
	
	public void quitInstrus(View v) {
		instrus.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right));
		instrus.setVisibility(View.GONE);
	}
	
	public void coliBrainsInfos(View v) {
		coliBrainsInfos.setVisibility(View.VISIBLE);
		coliBrainsInfos.startAnimation(AnimationUtils.loadAnimation(this, R.anim.aleat_opt_anim));
	}
	
	public void quitColiBrainsInfos(View v) {
		coliBrainsInfos.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right));
		coliBrainsInfos.setVisibility(View.GONE);
	}
	
	public void musique(View v) {
		ImageButton iv = (ImageButton) v;
		opt_infos.setVisibility(View.INVISIBLE);
		if(!MyApp.getApp().pref.getBoolean("musique", true)) {
			MyApp.getApp().startMusic();
			iv.setImageResource(R.drawable.sound);
			MyApp.getApp().editor.putBoolean("musique", true);
		} else {
			MyApp.getApp().stopMusic();
			iv.setImageResource(R.drawable.nosound);
			MyApp.getApp().editor.putBoolean("musique", false);
		}
		MyApp.getApp().editor.commit();
	}
	
}