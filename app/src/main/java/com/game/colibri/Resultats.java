package com.game.colibri;

import java.lang.ref.WeakReference;
import java.util.Comparator;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.JsonSyntaxException;

public class Resultats extends Activity {
	
	public static int PERIODE = 1000/20;
	
	private RefreshHandler handler;
	private ResultatsAdapter adapt;
	private Niveau niv;
	private boolean display_res;
	private boolean has_won;
	private String[] defi_list;
	private int[] resVus;
	private int defi_displayed = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		display_res = (getIntent().getExtras()!=null);
		if(!display_res) { // Fin de campagne !
			setContentView(R.layout.fin_campagne);
			((Artifices) findViewById(R.id.artifices_doge)).doge = true;
			return;
		} else {
			setContentView(R.layout.activity_resultats);
			defi_list = getIntent().getExtras().getStringArray("defi");
			resVus = new int[defi_list.length+1];
			handler = new RefreshHandler(this);
			displayNextResults(null);
		}
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
	
	public void displayNextResults(View v) {
		handler.removeCallbacksAndMessages(null);
		if(ResultatsAdapter.etape!=5 && v!=null) { // Animation en cours, on passe l'animation.
			if(has_won)
				((Artifices) findViewById(R.id.artifices)).setVisibility(View.VISIBLE);
			handler.handleMessage(handler.obtainMessage(5));
			return;
		}
		((ImageButton) findViewById(R.id.nextRes)).setImageResource(R.drawable.next);
		Defi defi;
		try {
			defi = Defi.DefiFromJSON(defi_list[defi_displayed]);
			resVus[defi_displayed] = defi.id;
			defi_displayed++;
		} catch (JsonSyntaxException e) {
			e.printStackTrace();
			finish();
			return;
		}
		ResultatsAdapter.prog=0;
		ResultatsAdapter.etape=0;
		Participation[] parts = new Participation[defi.participants.size()];
		for(int i=0; i<parts.length; i++) {
			parts[i] = defi.participants.valueAt(i);
		}
		adapt = new ResultatsAdapter(this, parts);
		ListView lv = (ListView) findViewById(R.id.listRes);
		lv.setAdapter(adapt);
		lv.setOnItemClickListener(new ListView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				Joueur j = ((Participation) arg0.getAdapter().getItem(arg2)).joueur;
				(new DispJoueur(Resultats.this, j)).show();
			}
		});
		((TextView) findViewById(R.id.nomDefiRes)).setText(defi.nom);
		handler.sendMessageDelayed(handler.obtainMessage(0), 1200); // Commence les animations après 1200ms
		niv = new Niveau(defi.matchFini.mode, defi.matchFini.seed, defi.matchFini.param, defi.matchFini.avancement);
		has_won = (defi.participants.get(MyApp.id).rank==1);
		((Carte) findViewById(R.id.apercuResMini)).loadNiveau(niv);
		((Carte) findViewById(R.id.apercuResMaxi)).loadNiveau(niv);
		((Artifices) findViewById(R.id.artifices)).setVisibility(View.GONE);
	}
	
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		if(hasFocus) {
			if(!display_res) {
				((TextView) findViewById(R.id.the_end)).startAnimation(AnimationUtils.loadAnimation(this, R.anim.artifice_anim_spin));
				Toast.makeText(this, R.string.toast_fin, Toast.LENGTH_SHORT).show();
			} else {
				((Carte) findViewById(R.id.apercuResMini)).loadNiveau(niv);
				Carte c = ((Carte) findViewById(R.id.apercuResMaxi));
				c.loadNiveau(niv);
				c.setVisibility(View.GONE);
			}
	    	
		}
	}
	
	public void clickApercu(View v) {
		View c = findViewById(R.id.apercuResMaxi);
		if(c.getVisibility()==View.GONE) {
			c.setVisibility(View.VISIBLE);
			c.startAnimation(AnimationUtils.loadAnimation(this, R.anim.aleat_opt_anim));
		} else {
			c.setVisibility(View.GONE);
		}
	}
	
	private void animStep(int what) {
		if(what==0) {
			ResultatsAdapter.prog+=0.02;
			adapt.notifyDataSetChanged();
			if(ResultatsAdapter.prog<1)
				handler.sleep(PERIODE);
			else
				handler.sendMessageDelayed(handler.obtainMessage(1), 400);
		} else {
			ResultatsAdapter.etape = what;
			if(what==4) {
				if(has_won)
					((Artifices) findViewById(R.id.artifices)).setVisibility(View.VISIBLE);
			}
			if(what==5) {
				adapt.sort(new Comparator<Participation>() {
					@Override
					public int compare(Participation lhs, Participation rhs) {
						return lhs.t_fini - (rhs.t_fini);
					}
				});
				((ImageButton) findViewById(R.id.nextRes)).setImageResource(R.drawable.fw);
				if(defi_displayed==defi_list.length)
					findViewById(R.id.nextRes).setVisibility(View.GONE);
			} else {
				handler.sendMessageDelayed(handler.obtainMessage(what+1), 600);
				adapt.notifyDataSetChanged();
			}
		}
	}
	
	private static class RefreshHandler extends Handler {
		
		private final WeakReference<Resultats> act;
		
		public RefreshHandler(Resultats a) {
			act = new WeakReference<Resultats>(a);
		}
		
		@Override
		public void handleMessage(Message msg) {
			if(act.get()!=null)
				act.get().animStep(msg.what);
		}
		
		public void sleep(long delayMillis) {
			this.removeMessages(0);
			sendMessageDelayed(obtainMessage(0), delayMillis);
		}
	};

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
			if(display_res && findViewById(R.id.apercuResMaxi).getVisibility()==View.VISIBLE) {
				findViewById(R.id.apercuResMaxi).setVisibility(View.GONE);
			} else {
				if(display_res) {
					handler.removeCallbacksAndMessages(null);
					Intent intent = new Intent();
					intent.putExtra("resVus", resVus);
					setResult(RESULT_FIRST_USER, intent);
				}
				finish();
			}
			return true;
		}
		return false;
	}
    
}
