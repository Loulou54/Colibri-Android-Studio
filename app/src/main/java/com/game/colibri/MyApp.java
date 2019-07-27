package com.game.colibri;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.media.MediaPlayer;
import android.preference.PreferenceManager;

public class MyApp extends Application {
	
	private static MyApp singleton;
	
	public static final int DEFAULT_MAX_COLI_BRAINS = 6;
	public static final int EXP_LEVEL_PER_COLI_BRAIN = 4000;
	
	public static int id;
	public static String pseudo;
	public static int appareil;
	public static int avancement; // Progression du joueur dans les niveaux campagne.
	public static int experience, expToSync; // L'expérience du joueur et l'expérience encore non synchronisée avec le serveur.
	public static long playTime; // Le temps de jeu cumulé en ms
	public static int coliBrains, maxCB, expProgCB, cumulExpCB; // Le nombre de bonus d'aide colibrains, le maximum cumulable et le progrès en expérience vers le prochain colibrain.
	public static int versionCode; // Le code de version de la dernière version de Colibri exécutée.
	public static long last_update; // Timestamp donné par le serveur de la dernière mise-à-jour.
	private static int nActiveActivities = 0; // Pour déterminer si l'on doit mettre en pause la musique ou non lorsqu'une activité passe en fond.
	
	public SharedPreferences pref;
	public SharedPreferences.Editor editor;
	public MediaPlayer intro=null,boucle=null;
	
	public static MyApp getApp(){
		return singleton;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		singleton = this;
		pref = PreferenceManager.getDefaultSharedPreferences(this);
		editor = pref.edit();
		loadData();
	}
	
	/**
	 * On récupère les préférences et l'avancement de l'utilisateur.
	 */
	public void loadData() {
		id = pref.getInt("id", 0);
		pseudo = pref.getString("pseudo", null);
		appareil = pref.getInt("appareil", 0);
		avancement = pref.getInt("niveau", 1);
		experience = pref.getInt("exp", 0);
		expToSync = pref.getInt("expToSync", experience);
		playTime = pref.getLong("playTime", 0);
		coliBrains = pref.getInt("coliBrains", 0);
		maxCB = pref.getInt("maxCB", DEFAULT_MAX_COLI_BRAINS);
		expProgCB = pref.getInt("expProgCB", 0);
		cumulExpCB = pref.getInt("cumulExpCB", 0);
		versionCode = pref.getInt("versionCode", 0);
		last_update = pref.getLong("last_update", 0);
		ParamAleat.loadParams(pref);
		int versionActuelle=0;
		try {
			versionActuelle = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		if(versionActuelle!=versionCode) {
			editor.putString("adversaires", null)
				.putInt("versionCode", versionActuelle)
				.commit();
		}
	}
	
	public void connectUser(int i, String n, int a) {
		id = i;
		pseudo = n;
		appareil= a;
		editor.putString("pseudo", n)
			.putInt("id", i)
			.putInt("appareil", a)
			.commit();
	}

	public static void addPlayTime(long time_ms) {
		playTime += time_ms;
		getApp().editor.putLong("playTime", playTime).commit();
	}
	
	public static void updateExpProgCB(int exp) {
		expProgCB += exp;
		int n = expProgCB/EXP_LEVEL_PER_COLI_BRAIN;
		expProgCB = expProgCB % EXP_LEVEL_PER_COLI_BRAIN;
		coliBrains = Math.min(maxCB, coliBrains+n);
		if(coliBrains == maxCB) {
			cumulExpCB += exp-expProgCB;
			expProgCB = 0;
		} else {
			cumulExpCB += exp;
		}
	}
	
	/**
	 * On sauve les préférences et l'avancement de l'utilisateur.
	 */
	public void saveData() {
		editor.putInt("niveau", avancement)
			.putLong("playTime", playTime)
			.putInt("exp", experience)
			.putInt("expToSync", expToSync)
			.putInt("coliBrains", coliBrains)
			.putInt("maxCB", maxCB)
			.putInt("expProgCB", expProgCB)
			.putInt("cumulExpCB", cumulExpCB)
			.putLong("last_update", last_update)
			.commit();
	}
	
	public static void resumeActivity() {
		if(nActiveActivities==0 && singleton!=null && singleton.pref.getBoolean("musique", true))
			singleton.startMusic();
		nActiveActivities++;
	}
	
	public static void stopActivity() {
		nActiveActivities--;
		if(nActiveActivities==0 && singleton!=null)
			singleton.stopMusic();
	}
	
	public void startMusic() {
		if(intro==null && boucle==null) {
			intro = MediaPlayer.create(this, R.raw.intro);
			intro.setLooping(false);
			boucle = MediaPlayer.create(this, R.raw.boucle);
			boucle.setLooping(true);
			intro.start();
			intro.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
				@Override
				public void onCompletion(MediaPlayer mp) {
					intro.release();
				    intro = null;
					boucle.start();
				}
			});
		} else if(intro==null)
			boucle.start();
		else
			intro.start();
	}
	
	public void stopMusic() {
		if(intro==null && boucle!=null)
			boucle.pause();
		else if(intro!=null)
			intro.pause();
	}
	
	public void releaseMusic() {
		if (intro!=null) {
		    intro.release();
		    intro = null;
		}
		if (boucle!=null) {
		    boucle.release();
		    boucle = null;
		}
	}
	
}
