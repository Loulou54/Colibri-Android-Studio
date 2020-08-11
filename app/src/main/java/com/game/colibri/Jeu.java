package com.game.colibri;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Random;

import com.game.colibri.Solver.Move;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdCallback;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.gson.JsonSyntaxException;
import com.network.colibri.DBController;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

/**
 * Activité gérant le jeu proprement dit. Elle affiche notamment la View "Carte" en plein écran.
 */
public class Jeu extends Activity {
	
	public static final int NIV_MAX=36;
	public static WeakReference<Multijoueur> multijoueur;
	
	public Niveau niv;
	public Carte carte;
	public MoteurJeu play;
	public ViewGroup menu_lateral;
	public View pause;
	public View perdu; 
	public View gagne; 
	public Button bout_dyna;
	private boolean multi, brandNew=true, forfait=false, solved=false, finipartous=false;
	private int first_time = 0; // Le temps de première résolution du niveau
	public int n_niv;
	private Bundle opt, savedInstanceState;
	private Defi defi=null;
	private InterstitialAd endLevelAd;
	private boolean quitAction = false;

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		opt = getIntent().getExtras();
		multi = opt.containsKey("defi"); // Détermine si l'on est en mode multi
		if(multi) {
			try {
				defi = Defi.DefiFromJSON(opt.getString("defi"));
			} catch(JsonSyntaxException e) {
				e.printStackTrace();
				finish();
			}
		}
		if(multi) { // Interdire Screenshots
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
		}
		setContentView(R.layout.activity_jeu);
		n_niv = opt.getInt("n_niv", 1);
		carte = (Carte) findViewById(R.id.carte);
		bout_dyna = (Button) findViewById(R.id.bout_dyna);
		// Menu latéral
		menu_lateral = (ViewGroup) findViewById(R.id.menu_lateral);
		menu_lateral.findViewById(R.id.back_in_time).setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View view) {
				recommencer(view);
				return true;
			}
		});
		menu_lateral.findViewById(R.id.av_rapide).setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch(event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						MoteurJeu.PERIODE = 1000/50; // Vitesse * 2
						break;
					case MotionEvent.ACTION_UP:
						MoteurJeu.PERIODE = 1000/25;
						v.performClick();
						break;
					default:
						break;
				}
				return false;
			}
		});
		// Bouton ColiBrains du menu latéral
		ImageButton coliBrains = (ImageButton) findViewById(R.id.colibrains_ingame);
		coliBrains.setImageDrawable(new ColiBrain(this, "", 0));
		// fonts menu Pause
		final Typeface font = Typeface.createFromAsset(getAssets(),"fonts/Passing Notes.ttf");
		pause = findViewById(R.id.pause);
		((ViewStub) pause).setOnInflateListener(new ViewStub.OnInflateListener() {
			@Override
			public void onInflate(ViewStub stub, View inflated) {
				pause = inflated;
				// Hack inespéré pour rétrécir légèrement le 1er et 3è bouton de manière robuste
				pause.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
					@Override
					public boolean onPreDraw() {
						if (pause.getViewTreeObserver().isAlive())
							pause.getViewTreeObserver().removeOnPreDrawListener(this);
						LinearLayout buts = pause.findViewById(R.id.pause_buttons);
						for(int i=2; i <=4; i+=2) {
							View but = buts.getChildAt(i);
							LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) but.getLayoutParams();
							lp.width = but.getWidth()*4/5;
							but.setLayoutParams(lp);
						}
						return true;
					}
				});
				((TextView) pause.findViewById(R.id.titlePause)).setTypeface(font);
				LinearLayout buts = pause.findViewById(R.id.pause_buttons);
				for(int i=0; i<buts.getChildCount(); i++) {
					((TextView) buts.getChildAt(i)).setTypeface(font);
				}
			}
		});
		// fonts menu Pause
        perdu = findViewById(R.id.perdu);
        ((ViewStub) perdu).setOnInflateListener(new ViewStub.OnInflateListener() {
			@Override
			public void onInflate(ViewStub stub, View inflated) {
				perdu = inflated;
				((TextView) inflated.findViewById(R.id.perdu_txt)).setTypeface(font);
			}
		});
     // fonts menu gagne
		gagne = findViewById(R.id.gagner);
		((ViewStub) gagne).setOnInflateListener(new ViewStub.OnInflateListener() {
			@Override
			public void onInflate(ViewStub stub, View inflated) {
				gagne = inflated;
				((TextView) inflated.findViewById(R.id.bravo_txt)).setTypeface(font);
				LinearLayout res = (LinearLayout) inflated.findViewById(R.id.gagne_resultats);
				for(int i=0; i<2; i++)
					((TextView) res.getChildAt(i)).setTypeface(Typeface.createFromAsset(getAssets(),"fonts/YummyCupcakes.ttf"));
				((TextView) inflated.findViewById(R.id.gagne_resultats_phrase)).setTypeface(Typeface.createFromAsset(getAssets(),"fonts/YummyCupcakes.ttf"));
				((TextView) inflated.findViewById(R.id.gagne_sol_colibrain)).setTypeface(font);
				((TextView) inflated.findViewById(R.id.gagne_sol_gen)).setTypeface(font);
				((TextView) inflated.findViewById(R.id.gagne_quit)).setTypeface(Typeface.createFromAsset(getAssets(),"fonts/Sketch_Block.ttf"));
			}
		});
		// Création du moteur de jeu
		play = new MoteurJeu(this,carte);
		PathViewer.mj = play;
		this.savedInstanceState = savedInstanceState;
		if(multi) { // On sauvegarde le fait que ce défi a été entammé.
			MyApp.getApp().editor.putInt("defi_fuit", defi.id);
			MyApp.getApp().editor.commit();
		}
		endLevelAd = new InterstitialAd(this);
		endLevelAd.setAdUnitId("ca-app-pub-4419736604084888/5266386581"); // Prod ad
		//endLevelAd.setAdUnitId("ca-app-pub-3940256099942544/1033173712"); // Test ad
		endLevelAd.setAdListener(new AdListener() {
			@Override
			public void onAdOpened() {
				super.onAdOpened();
				MyApp.stopActivity();
			}
			@Override
			public void onAdClosed() {
				super.onAdClosed();
				MyApp.resumeActivity();
				if(quitAction) {
					finish();
				} else {
					suivant();
				}
			}
		});
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
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean("forfait", forfait);
		outState.putBoolean("solved", solved);
		outState.putBoolean("finipartous", finipartous);
		outState.putInt("total_frames", play.state==MoteurJeu.GAGNE ? 0 : play.total_frames+play.frame);
		outState.putInt("first_time", first_time);
		outState.putLong("seed", niv.seed);
		super.onSaveInstanceState(outState);
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onWindowFocusChanged(boolean)
	 */
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		if (brandNew) { // événement appelé lorsque le RelativeLayout "lay" est prêt ! C'est ici que l'on peut charger le niveau et ajouter les View "Animal".
			launch_niv(false);
			if(savedInstanceState!=null) { // Si Jeu a été détruite mais restaurée
				forfait = savedInstanceState.getBoolean("forfait", false);
				solved = savedInstanceState.getBoolean("solved", false);
				finipartous = savedInstanceState.getBoolean("finipartous", false);
				play.total_frames = savedInstanceState.getInt("total_frames", 0);
				first_time = savedInstanceState.getInt("first_time", 0);
				savedInstanceState = null;
			}
			brandNew=false;
		} else if(!hasFocus && play.state==MoteurJeu.RUNNING) {
			play.pause(MoteurJeu.PAUSE_MENU);
		} else if(hasFocus && play.state==MoteurJeu.PAUSE_MENU && perdu.getVisibility()!=View.VISIBLE && gagne.getVisibility()!=View.VISIBLE) {
			pause.setVisibility(View.VISIBLE);
			setLevelInfo();
		}
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onTouchEvent(android.view.MotionEvent)
	 */
	@Override
	public boolean onTouchEvent (MotionEvent ev) {
		if(play.state==MoteurJeu.RUNNING) {
			menuLateral(play.onTouch(ev), ev.getX());
		} else if(play.state==MoteurJeu.SOL_READY) {
			play.start();
			play.onTouch(ev);
		} else { // Pour éviter un comportement non voulu si l'ACTION_DOWN s'est produit avant le lancement du jeu.
			ev.setAction(MotionEvent.ACTION_DOWN);
			play.onTouch(ev);
		}
		return true;
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(play.state==MoteurJeu.RUNNING) { // Commande en cours de jeu
			if(keyCode == KeyEvent.KEYCODE_DPAD_UP) {
				play.direction(MoteurJeu.UP);
			} else if(keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
				play.direction(MoteurJeu.RIGHT);
			} else if(keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
				play.direction(MoteurJeu.LEFT);
			} else if(keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
				play.direction(MoteurJeu.DOWN);
			} else if(keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_ESCAPE) {
				play.pause(MoteurJeu.PAUSE_MENU);
				menuLateral(0,0);
				pause.setVisibility(View.VISIBLE);
				setLevelInfo();
				pause.startAnimation(AnimationUtils.loadAnimation(this, R.anim.aleat_opt_anim));
			} else
				return false;
		} else if(play.state==MoteurJeu.PAUSE_MENU) { // Jeu en pause
			if(keyCode == KeyEvent.KEYCODE_BACK)
				reprendre(null);
			else
				return false;
		} else if(play.state==MoteurJeu.MORT) { // Mort
			if(keyCode == KeyEvent.KEYCODE_BACK) {
				carte.sang.clearAnimation();
				carte.mort.clearAnimation();
				recommencer(null);
			} else
				return false;
		} else if(play.state==MoteurJeu.SOL_RESEARCH) { // Interrupt research
			if(Solver.instance!=null)
				Solver.instance.cancel(true);
		} else {
			return false;
		}
		return true;
	}
	
	private void setLevelInfo() {
		TextView mode = (TextView) findViewById(R.id.mode);
		TextView niv = (TextView) findViewById(R.id.niveau_courant);
		if(multi)
			mode.setText(defi.nom);
		else
			mode.setText(R.string.aleat);
		switch(opt.getInt("mode", 0)) {
		case Niveau.CAMPAGNE:
			mode.setText(R.string.campagne);
			niv.setText(n_niv+"/"+NIV_MAX);
			break;
		case Niveau.FACILE:
			niv.setText(R.string.fac);
			break;
		case Niveau.MOYEN:
			niv.setText(R.string.moy);
			break;
		case Niveau.DIFFICILE:
			niv.setText(R.string.dif);
			break;
		default:
			niv.setText(R.string.perso);
		}
	}
	
	public void updateMenuLateral() {
		if(menu_lateral.getVisibility()==View.VISIBLE) {
			int t_ms = (play.total_frames+play.frame)*MoteurJeu.PERIODE_NORMALE;
			((TextView) menu_lateral.findViewById(R.id.temps)).setText(
					String.format("%d.%02d s", t_ms/1000, (t_ms%1000)/10)
			);
		}
	}
	
	private boolean infiniteColiBrains() {
		return solved || (!multi && (opt.getInt("mode", 0)>0 || n_niv<MyApp.avancement));
	}
	
	private void updateColiBrains() {
		ImageButton coliBrains = (ImageButton) findViewById(R.id.colibrains_ingame);
		//coliBrains.setEnabled(MyApp.coliBrains > 0 || infiniteColiBrains());
		((ColiBrain) coliBrains.getDrawable())
			.setProgress(MyApp.expProgCB/(float)MyApp.EXP_LEVEL_PER_COLI_BRAIN)
			.setText(infiniteColiBrains() ? "∞" : ""+MyApp.coliBrains);
	}

	private void unlockedElementPopup(int levelUnlocked) {
		String unlocked_next_txt;
		int unlocked_img, unlocked_name, unlocked_next_img=0, unlocked_next_name=0;
		switch(levelUnlocked) {
			case ParamAleat.LEVEL_UNLOCK_COWS:
				unlocked_img = R.drawable.vache_0;
				unlocked_name = R.string.vaches;
				unlocked_next_img = R.drawable.dynamite;
				unlocked_next_name = R.string.dynamites;
				unlocked_next_txt = getString(R.string.unlocked_msg_2_1, ParamAleat.LEVEL_UNLOCK_DYNA-ParamAleat.LEVEL_UNLOCK_COWS);
				break;
			case ParamAleat.LEVEL_UNLOCK_DYNA:
				unlocked_img = R.drawable.dynamite;
				unlocked_name = R.string.dynamites;
				unlocked_next_img = R.drawable.chat_0;
				unlocked_next_name = R.string.chats;
				unlocked_next_txt = getString(R.string.unlocked_msg_2_1, ParamAleat.LEVEL_UNLOCK_CATS-ParamAleat.LEVEL_UNLOCK_DYNA);
				break;
			case ParamAleat.LEVEL_UNLOCK_CATS:
				unlocked_img = R.drawable.chat_0;
				unlocked_name = R.string.chats;
				unlocked_next_img = R.drawable.rainbow;
				unlocked_next_name = R.string.arcs;
				unlocked_next_txt = getString(R.string.unlocked_msg_2_1, ParamAleat.LEVEL_UNLOCK_RAINBOWS-ParamAleat.LEVEL_UNLOCK_CATS);
				break;
			case ParamAleat.LEVEL_UNLOCK_RAINBOWS:
				unlocked_img = R.drawable.rainbow;
				unlocked_name = R.string.arcs;
				unlocked_next_txt = getString(R.string.unlocked_msg_2_2);
				break;
			default:
				return;
		}
		final PaperDialog unlocked = new PaperDialog(new ContextThemeWrapper(this, android.R.style.Theme_Holo_Light_Dialog), R.layout.unlocked_element_info);
		unlocked.setTitle(R.string.unlocked_title);
		((ImageView) unlocked.findViewById(R.id.unlocked_img)).setImageResource(unlocked_img);
		((TextView) unlocked.findViewById(R.id.unlocked_name)).setText(unlocked_name);
		((TextView) unlocked.findViewById(R.id.unlocked_next_txt)).setText(unlocked_next_txt);
		if(unlocked_next_img!=0) {
			((ImageView) unlocked.findViewById(R.id.unlocked_next_img)).setImageResource(unlocked_next_img);
			((TextView) unlocked.findViewById(R.id.unlocked_next_name)).setText(unlocked_next_name);
		} else {
			unlocked.findViewById(R.id.unlocked_next_img).setVisibility(View.GONE);
			unlocked.findViewById(R.id.unlocked_next_name).setVisibility(View.GONE);
		}
		unlocked.setPositiveButton(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				unlocked.dismiss();
			}
		}, null);
		unlocked.show();
	}
	
	/**
	 * Affichage des résultats de fin de niveau.
	 * -> Événement déclenché par "play" lorsque le niveau a été gagné.
	 * @param temps_total_ms
	 */
	public void gagne(int temps_total_ms) {
		boolean solved = this.solved;
		this.solved = true;
		if(temps_total_ms==Participation.FORFAIT) {
			forfait = true;
		}
		gagne.setVisibility(View.VISIBLE);
		play.pause(MoteurJeu.GAGNE);
		if(carte.n_dyna>0) hideDyna();
		menuLateral(0,0);
		if(!endLevelAd.isLoaded() && !endLevelAd.isLoading()) { // Si la pub ne s'est pas chargée avant (soit par erreur, soit car la limite de fréquence était atteinte)
			endLevelAd.loadAd(new AdRequest.Builder().build());
		}
		// Détermination de l'expérience
		int exp;
		if(opt.getInt("mode",0)>0) {
			exp = play.niv.experience;
		} else {
			if(n_niv==MyApp.avancement)
				exp = n_niv*(50+n_niv/4);
			else
				exp = n_niv*(10+n_niv/8);
		}
		// A la première résolution, ajouter le progrès au profil
		if(!solved && !forfait) {
			first_time = temps_total_ms;
			if(opt.getInt("mode",0)==0 && n_niv==MyApp.avancement) { // Niveau suivant débloqué !
				MyApp.avancement++;
				unlockedElementPopup(n_niv);
			}
			MyApp.experience+=exp;
			MyApp.expToSync+=exp;
			MyApp.updateExpProgCB(exp);
			MyApp.getApp().saveData(); // On sauvegarde la progression.
		}
		// Affichage
		String s1;
		SpannableString s2;
		if(temps_total_ms==Participation.FORFAIT) {
			s1 = getString(R.string.forfait)+" !";
			s2 = new SpannableString("");
		} else {
			s1 = getString(R.string.temps)+" :\n"
					+ (!solved ? "" : getString(R.string.first_time)+" :\n")
					+ getString(R.string.exp)+" :";
			String s = getFormattedTime(temps_total_ms)+"\n"
					+ (!solved ? "" : first_time==0 ? getString(R.string.forfait)+" !\n" : getFormattedTime(first_time)+"\n")
					+ "+ "+(forfait ? "0 ("+exp+")" : exp);
			s2 = new SpannableString(s);
			int virgule = s.indexOf(".");
			while(virgule >= 0) {
				s2.setSpan(new RelativeSizeSpan(0.75f), virgule+1, virgule+3, 0);
				virgule = s.indexOf(".", virgule+1);
			}
		}
		LinearLayout res = (LinearLayout) findViewById(R.id.gagne_resultats);
		TextView phrase = (TextView) findViewById(R.id.gagne_resultats_phrase);
		if (multi) { // Mode multijoueur
			if(!solved) {
				MyApp.getApp().editor.remove("defi_fuit");
				MyApp.getApp().editor.commit();
				finipartous = defi.finMatch(new DBController(this), MyApp.id, temps_total_ms);
				Multijoueur multijoueur = Jeu.multijoueur!=null ? Jeu.multijoueur.get() : null;
				if(multijoueur!=null) {
					finipartous = multijoueur.defi.finMatch(null, MyApp.id, temps_total_ms);
					multijoueur.syncData();
				}
			}
			phrase.setText(!finipartous ? getString(R.string.joueur_suivant) : getString(R.string.resultats));
			phrase.setVisibility(View.VISIBLE);
		} else {
			phrase.setVisibility(View.GONE);
		}
		((TextView) res.getChildAt(0)).setText(s1);
		((TextView) res.getChildAt(1)).setText(s2);
	}
	
	public static String getFormattedTime(int time) {
		if(time < 0)
			return "xxx";
		return String.format(Locale.FRANCE, (time>=60000 ? (time/60000)+" min " : "")+"%d.%02d s", (time%60000)/1000, (time%1000)/10);
	}
	
	/**
	 * Le colibri est mort : affiche l'écran associé.
	 * @param isVache détermine si une vache ou un chat est à l'origine de l'accident !
	 */
	public void mort(boolean isVache) {
		if(play.state!=MoteurJeu.RUNNING) {
			perdu.setVisibility(View.VISIBLE);
			perdu.findViewById(R.id.perdu_background)
				.setBackgroundResource(isVache ? R.drawable.perdu_background_vache : R.drawable.perdu_background_chat);
		}
	}
	
	/**
	 * Explose le menhir en face du colibri à l'aide d'une dynamite. Appelé par l'appui sur le bouton dédié.
	 * 	@param v le bouton
	 */
	public void exploser(View v) {
		if(play.state==MoteurJeu.RUNNING && carte.n_dyna>0)
			play.dynamite();
	}
	
	/**
	 * Montre le bouton des dynamites avec une animation.
	 */
	public void showDyna() {
		bout_dyna.setVisibility(View.VISIBLE);
		bout_dyna.startAnimation(AnimationUtils.loadAnimation(this, R.anim.bouton_dyna_down));
	}
	
	/**
	 * Cache le bouton des dynamites avec une animation.
	 */
	public void hideDyna() {
		bout_dyna.startAnimation(AnimationUtils.loadAnimation(this, R.anim.bouton_dyna_up));
		bout_dyna.setVisibility(View.GONE);
	}
	
	/**
	 * Affiche ou cache le menu latéral par dessus le jeu, sur le côté gauche ou droit
	 * selon la position du clic. Utilise plusieurs "tricks" pour adapter la disposition
	 * des boutons selon l'emplacement du menu (gauche ou droite).
	 * @param disp 0:cacher ; 1:afficher ; 2:rien
	 * @param xClic la coordonnée en x du clic
	 */
	private void menuLateral(int disp, float xClic) {
		if(disp==1 && menu_lateral.getVisibility()!=View.VISIBLE) { // Afficher
			int ww = findViewById(R.id.lay).getWidth();
			int cote = (int) Math.signum(2*xClic-ww); // -1=LEFT ; +1=RIGHT
			// Positionnement menu
			RelativeLayout.LayoutParams p = (RelativeLayout.LayoutParams) menu_lateral.getLayoutParams();
			p.addRule(10-cote, 0); // pour annuler l'autre contrainte
			p.addRule(10+cote); // car RelativeLayout.ALIGN_PARENT_LEFT = 9 & RelativeLayout.ALIGN_PARENT_RIGHT = 11
			menu_lateral.setLayoutParams(p);
			// Positionnement éléments du menu
			LinearLayout boutons = (LinearLayout) menu_lateral.findViewById(R.id.menu_lateral_boutons);
			boutons.setGravity(4+cote); // car Gravity.LEFT = 3 & Gravity.RIGHT = 5
			int nChildren = boutons.getChildCount();
			for(int i=0; i < nChildren; i++) {
				View v = boutons.getChildAt(i);
				LinearLayout.LayoutParams p2 = (LinearLayout.LayoutParams) v.getLayoutParams();
				int m = p2.leftMargin + p2.rightMargin;
				p2.setMargins(cote <= 0 ? m : 0, p2.topMargin, cote > 0 ? m : 0, p2.bottomMargin);
				int pad = v.getPaddingLeft() + v.getPaddingRight();
				v.setPadding(cote <= 0 ? pad : 0, 0, cote > 0 ? pad : 0, 0);
			}
			// Animation
			TranslateAnimation anim = new TranslateAnimation(cote*menu_lateral.getWidth(),0,0,0);
		    anim.setDuration(250);
		    anim.setInterpolator(new AccelerateDecelerateInterpolator());
			menu_lateral.setVisibility(View.VISIBLE);
			menu_lateral.startAnimation(anim);
		} else if(disp<=1 && menu_lateral.getVisibility()==View.VISIBLE) { // Cacher
			int cote = (((RelativeLayout.LayoutParams) menu_lateral.getLayoutParams()).getRules()[RelativeLayout.ALIGN_PARENT_LEFT]==0) ? 1 : -1;
			TranslateAnimation anim = new TranslateAnimation(0,cote*menu_lateral.getWidth(),0,0);
		    anim.setDuration(250);
			menu_lateral.startAnimation(anim);
			menu_lateral.setVisibility(View.INVISIBLE);
		}
	}
	
	/**
	 * S'occupe de charger un niveau dans la "carte" et de lancer le moteur de jeu "play".
	 */
	private void launch_niv(boolean replay) {
		if(Solver.instance != null) {
			Solver.instance.cancel(true);
		}
		if(replay) {
			niv.replay();
		} else {
			if(!endLevelAd.isLoaded() && !endLevelAd.isLoading()) {
				endLevelAd.loadAd(new AdRequest.Builder().build());
			}
			forfait = false;
			solved = false;
			first_time = 0;
			if(opt.getInt("mode", 0)>0) {
				if(multi) { // Mode multijoueur
					niv = new Niveau(opt.getInt("mode"), opt.getLong("seed"), opt.getIntArray("param"), opt.getInt("avancement"));
					if(defi.match==null) {
						defi.match = new Defi.Match(opt.getInt("mode"), opt.getLong("seed"), opt.getIntArray("param"), opt.getInt("avancement"), niv.progressMin, niv.experience);
						defi.limite = System.currentTimeMillis()/1000 + defi.t_max;
						Multijoueur multijoueur = Jeu.multijoueur!=null ? Jeu.multijoueur.get() : null;
						if(multijoueur!=null) {
							multijoueur.defi.match = defi.match;
							multijoueur.defi.limite = defi.limite;
							multijoueur.adapt.notifyDataSetChanged();
						}
						(new DBController(this)).updateDefi(defi);
						if(multijoueur!=null) multijoueur.syncData(); // Envoi du nouveau défi créé
					}
				} else // Mode Carte Aléatoire
					niv = new Niveau(opt.getInt("mode"), savedInstanceState==null ? (new Random()).nextLong() : savedInstanceState.getLong("seed"), ParamAleat.param, MyApp.avancement);
			} else {
				try { // On ouvre le Niveau index_niv.
					niv = new Niveau(this.getAssets().open("niveaux/niveau"+n_niv+".txt"));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		carte.loadNiveau(niv);
		// Selon le niveau d'expérience, le temps avant d'obtenir une aide ColiBrain augmente.
		double progressFactor = 1. - Math.exp(-MyApp.experience/200000.);
		play.init(replay, (int)(25*(6. + 9*progressFactor)), (int)(25*(15. + 15*progressFactor)));
		updateColiBrains();
		if(savedInstanceState!=null) {
			pause.setVisibility(View.VISIBLE);
			setLevelInfo();
			return;
		}
		if(opt.containsKey("startMsg")) { // Affichage d'un message avant le démarrage du jeu.
			Toast.makeText(this, opt.getString("startMsg"), Toast.LENGTH_SHORT).show();
			opt.remove("startMsg");
			final Handler handler = new Handler();
			handler.postDelayed(new Runnable() {
			  @Override
			  public void run() {
			    play.start();
			  }
			}, 2000);
		} else
			play.start();
	}
	
	/*
	 * Les fonctions suivantes sont déclenchées par les appuis sur les boutons des menus Pause, Gagné et Mort.
	 */
	
	public void reprendre(View v) {
		pause.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right));
		pause.setVisibility(View.GONE);
        play.start();
	}
	
	public void recommencer(View v) {
		if(pause.getVisibility()==View.VISIBLE) {
			pause.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right));
			pause.setVisibility(View.GONE);
		}
		gagne.setVisibility(View.GONE);
		perdu.setVisibility(View.GONE);
    	if(carte.n_dyna>0) hideDyna();
    	menuLateral(0,0);
    	launch_niv(true);
	}

	public void quitter(View v) {
		if(multi && !solved) {
			passer(null);
		} else if(solved && endLevelAd.isLoaded()) {
			quitAction = true;
			endLevelAd.show();
		} else {
			finish();
		}
	}

	public void cancelMove(View v) {
		play.cancelLastMove();
	}

	private void coliBrainRewardedAd() {
		final RewardedAd coliBrainAd = new RewardedAd(this, "ca-app-pub-4419736604084888/1889839232"); // Prod ad
		//final RewardedAd coliBrainAd = new RewardedAd(this, "ca-app-pub-3940256099942544/5224354917"); // Test ad
		final boolean[] launchReqAndLoadFailCbAd = new boolean[] {false, false}; // 0: l'utilisateur a demandé à regarder la pub mais elle n'est pas chargée ; 1: le chargement de la pub a échoué
		final RewardedAdCallback coliBrainAdCallback = new RewardedAdCallback() {
			@Override
			public void onRewardedAdOpened() {
				super.onRewardedAdOpened();
				MyApp.stopActivity();
			}
			@Override
			public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
				MyApp.updateExpProgCB(MyApp.EXP_LEVEL_PER_COLI_BRAIN);
			}
			@Override
			public void onRewardedAdClosed() {
				super.onRewardedAdClosed();
				MyApp.resumeActivity();
				play.start();
				if(MyApp.coliBrains > 0)
					coliBrainHelp(null);
			}
		};
		coliBrainAd.loadAd(new AdRequest.Builder().build(), new RewardedAdLoadCallback() {
			@Override
			public void onRewardedAdLoaded() {
				super.onRewardedAdLoaded();
				if(launchReqAndLoadFailCbAd[0]) {
					coliBrainAd.show(Jeu.this, coliBrainAdCallback);
				}
			}
			@Override
			public void onRewardedAdFailedToLoad(LoadAdError loadAdError) {
				super.onRewardedAdFailedToLoad(loadAdError);
				if(launchReqAndLoadFailCbAd[0]) {
					Toast.makeText(Jeu.this, R.string.cb_ad_load_failed, Toast.LENGTH_LONG).show();
					play.start();
				} else {
					launchReqAndLoadFailCbAd[1] = true;
				}
			}
		});
		play.pause(MoteurJeu.DIALOG);
		final PaperDialog unlockCbBox = new PaperDialog(new ContextThemeWrapper(this, android.R.style.Theme_Holo_Light_Dialog), 0);
		unlockCbBox.setTitle(R.string.unlock_cb_title);
		unlockCbBox.setMessage(R.string.unlock_cb_text);
		unlockCbBox.setPositiveButton(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(coliBrainAd.isLoaded()) {
					coliBrainAd.show(Jeu.this, coliBrainAdCallback);
				} else if(launchReqAndLoadFailCbAd[1]) {
					Toast.makeText(Jeu.this, R.string.cb_ad_load_failed, Toast.LENGTH_LONG).show();
					play.start();
				} else
					launchReqAndLoadFailCbAd[0] = true;
				unlockCbBox.dismiss();
			}
		}, getResources().getString(R.string.unlock_cb_ok));
		unlockCbBox.setNegativeButton(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				unlockCbBox.dismiss();
				play.start();
			}
		}, null);
		unlockCbBox.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialogInterface) {
				play.start();
			}
		});
		unlockCbBox.show();
	}
	
	public void coliBrainHelp(View v) {
		if(MyApp.coliBrains <= 0 && !infiniteColiBrains()) { // Rewarded ad
			menuLateral(0,0);
			coliBrainRewardedAd();
			return;
		} else if(play.state!=MoteurJeu.RUNNING)
			return;
		menuLateral(0,0);
		play.pause(MoteurJeu.SOL_RESEARCH);
		final PathViewer pv = (PathViewer) findViewById(R.id.path_viewer);
		(new Solver(niv, new Solver.SolverInterface() {
			@Override
			public void result(int r, int c, Solver.Path path) {
				if(!infiniteColiBrains()) {
					MyApp.coliBrains--;
					MyApp.getApp().saveData();
					updateColiBrains();
				}
				PathViewer.STEPS_COLIBRI = 6;
				pv.setPathAndAnimate(r, c, path.getMoves(), false);
			}
			@Override
			public void progressUpdate(int r, int c, LinkedList<Move> moves) {
				pv.setPath(r, c, moves);
			}
			@Override
			public void preExecute() {
				pv.clear();
				pv.setVisibility(View.VISIBLE);
			}
			@Override
			public void cancel(int reason) {
				pv.cancelResearch();
				if(reason == CANCELLED)
					Toast.makeText(MyApp.getApp(), R.string.cancel_sol, Toast.LENGTH_SHORT).show();
				else if(reason == NO_SOLUTION)
					Toast.makeText(MyApp.getApp(), R.string.no_solution, Toast.LENGTH_SHORT).show();
				else if(reason == OUT_OF_MEMORY)
					Toast.makeText(MyApp.getApp(), R.string.out_of_memory, Toast.LENGTH_LONG).show();
			}
		})).execute(play.frame, carte.colibri.getRow(), carte.colibri.getCol(), carte.n_fleur, carte.n_dyna, 1);
	}
	
	public void solutionGenerated(View v) {
		if(niv.solution==null) // il n'y a pas de solution
			return;
		if(opt.getInt("mode", 0)==Niveau.CAMPAGNE && n_niv==MyApp.avancement) {
			Toast.makeText(this, R.string.solution_bloque, Toast.LENGTH_SHORT).show();
			return;
		}
		recommencer(v);
    	play.solution(niv.solution);
	}
	
	public void solutionColiBrain(View v) {
		if(opt.getInt("mode", 0)==Niveau.CAMPAGNE && n_niv==MyApp.avancement) {
			Toast.makeText(this, R.string.solution_bloque, Toast.LENGTH_SHORT).show();
			return;
		}
		recommencer(v);
		play.pause(MoteurJeu.SOL_RESEARCH);
		final PathViewer pv = (PathViewer) findViewById(R.id.path_viewer);
		(new Solver(niv, new Solver.SolverInterface() {
			@Override
			public void result(int r, int c, Solver.Path path) {
				play.solution(path.getGamesMoves().toArray(new int[0][]));
				pv.cancelResearch();
			}
			@Override
			public void progressUpdate(int r, int c, LinkedList<Move> moves) {
				pv.setPath(r, c, moves);
			}
			@Override
			public void preExecute() {
				pv.clear();
				pv.setVisibility(View.VISIBLE);
			}
			@Override
			public void cancel(int reason) {
				pv.cancelResearch();
				if(reason == CANCELLED)
					Toast.makeText(MyApp.getApp(), R.string.cancel_sol, Toast.LENGTH_SHORT).show();
				else if(reason == NO_SOLUTION)
					Toast.makeText(MyApp.getApp(), R.string.no_solution, Toast.LENGTH_SHORT).show();
				else if(reason == OUT_OF_MEMORY)
					Toast.makeText(MyApp.getApp(), R.string.out_of_memory, Toast.LENGTH_LONG).show();
			}
		})).execute(play.frame, carte.colibri.getRow(), carte.colibri.getCol(), carte.n_fleur, carte.n_dyna, 1);
	}

	public void passer(View v) {
		if(solved) {
			gagne(-1); // Pour réafficher les résultats
			return;
		} else if(!multi) {
			if(opt.getInt("mode", 0)==Niveau.CAMPAGNE && n_niv>=MyApp.avancement)
				Toast.makeText(this, R.string.bloque, Toast.LENGTH_SHORT).show();
			else
				gagne(Participation.FORFAIT);
			return;
		}
		play.pause(MoteurJeu.DIALOG);
		final PaperDialog forfait = new PaperDialog(new ContextThemeWrapper(this, android.R.style.Theme_Holo_Light_Dialog), 0);
		forfait.setTitle(R.string.forfait);
		forfait.setMessage(R.string.forfait_msg);
		forfait.setPositiveButton(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				gagne(Participation.FORFAIT);
				forfait.dismiss();
			}
		}, null);
		forfait.setNegativeButton(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				forfait.dismiss();
				play.start();
			}
		}, null);
		forfait.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialogInterface) {
				play.start();
			}
		});
		forfait.show();
	}

	public void suivant(View v) {
		if(endLevelAd.isLoaded()) {
			quitAction = false;
			endLevelAd.show();
		} else {
			suivant();
		}
	}
	
	public void suivant() {
		gagne.setVisibility(View.GONE);
		if(bout_dyna.getVisibility()==View.VISIBLE) hideDyna();
		if(opt.getInt("mode", 0)==Niveau.CAMPAGNE) {
			if(n_niv==NIV_MAX) {
				setResult(2);
				quitter(null);
				return;
			}
			n_niv++;
		} else if(multi) {
			if(finipartous) {
				//finDefi();
				Intent intent = new Intent();
				intent.putExtra("defi", defi.toJSON());
				setResult(RESULT_FIRST_USER, intent);
				finish();
			} else {
				quitter(null);
			}
			return;
		}
		PathViewer pv = (PathViewer) findViewById(R.id.path_viewer);
		if(pv.getVisibility()==View.VISIBLE) {
			pv.clear();
			pv.setVisibility(View.GONE);
		}
		launch_niv(false);
	}

	public void coliBrainAutoHint() {
		final PathViewer pv = (PathViewer) findViewById(R.id.path_viewer);
		(new Solver(niv, new Solver.SolverInterface() {
			@Override
			public void result(int r, int c, Solver.Path path) {
				PathViewer.STEPS_COLIBRI = 4;
				pv.clear();
				pv.setVisibility(View.VISIBLE);
				pv.setPathAndAnimate(r, c, path.getMoves(), true);
			}
			@Override
			public void progressUpdate(int r, int c, LinkedList<Move> moves) {}
			@Override
			public void preExecute() {}
			@Override
			public void cancel(int reason) {
				if(reason == NO_SOLUTION) {
					Toast.makeText(MyApp.getApp(), R.string.no_solution_go_back, Toast.LENGTH_LONG).show();
					if(menu_lateral.getVisibility()!=View.VISIBLE)
						menuLateral(1, Carte.ww-10);
					AlphaAnimation anim = new AlphaAnimation(1f, 0.4f);
					anim.setDuration(200);
					anim.setRepeatMode(Animation.REVERSE);
					anim.setRepeatCount(5);
					menu_lateral.findViewById(R.id.back_in_time).startAnimation(anim);
				} else if(reason == OUT_OF_MEMORY)
					Toast.makeText(MyApp.getApp(), R.string.out_of_memory, Toast.LENGTH_LONG).show();
			}
		})).execute(play.frame + 25, carte.colibri.getRow(), carte.colibri.getCol(), carte.n_fleur, carte.n_dyna, 1);
	}
}
