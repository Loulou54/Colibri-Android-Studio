package com.game.colibri;

import static com.network.colibri.CommonUtilities.BROADCAST_MESSAGE_ACTION;
import static com.network.colibri.CommonUtilities.EXTRA_MESSAGE;
import static com.network.colibri.CommonUtilities.SERVER_URL;
import static com.network.colibri.CommonUtilities.APP_TOKEN;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;
import com.network.colibri.ConnectionDetector;
import com.network.colibri.DBController;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.SparseArray;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import cz.msebera.android.httpclient.Header;

public class Multijoueur extends Activity {
	
	public static boolean active = false;
	
	private ExpandableListView lv;
	public DefiExpandableAdapter adapt;
	private SparseArray<Joueur> joueurs;
	private ArrayList<Defi> adversaires;
	private PaperDialog boxNiv;
	private AlertDialog.Builder messageDialog = null;
	private ViewSwitcher loader;
	public Defi defi;
	public Joueur user;
	public ConnectionDetector connect;
	public AsyncHttpClient client;
	public DBController base;
	private long lastPress = 0; // timestamp du dernier appui sur un bouton défi pour éviter les doubles clics
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_multijoueur);
		active = true;
		connect = new ConnectionDetector(this);
		client = new AsyncHttpClient();
		client.setMaxRetriesAndTimeout(5, 500);
		base = new DBController(this);
		joueurs = new SparseArray<Joueur>();
		adversaires = new ArrayList<Defi>();
		Jeu.multijoueur = null; // Dans le cas où Multijoueur a été destroyed lorsque Jeu était par dessus
		((TextView) findViewById(R.id.titreMulti)).setTypeface(Typeface.createFromAsset(getAssets(),"fonts/Adventure.otf"));
		((TextView) findViewById(R.id.nvDefi)).setTypeface(Typeface.createFromAsset(getAssets(),"fonts/Sketch_Block.ttf"));
		((TextView) findViewById(R.id.nvPRapide)).setTypeface(Typeface.createFromAsset(getAssets(),"fonts/Sketch_Block.ttf"));
		((TextView) findViewById(R.id.user_name)).setTypeface(Typeface.createFromAsset(getAssets(),"fonts/YummyCupcakes.ttf"), Typeface.BOLD);
		lv = (ExpandableListView) findViewById(R.id.listView1);
		lv.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
			@Override
			public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
				Participation p = (Participation) adapt.getChild(groupPosition, childPosition);
				(new DispJoueur(Multijoueur.this, p.joueur)).show();
				return true;
			}
		});
		lv.setEmptyView((TextView) findViewById(R.id.defaultViewDefis));
		loader = (ViewSwitcher) findViewById(R.id.loader);
		registerReceiver(mHandleMessageReceiver, new IntentFilter(BROADCAST_MESSAGE_ACTION));
		// Initialisation ColiBrains
		ColiBrain coliBrainDrawable = new ColiBrain(this, ""+MyApp.coliBrains, MyApp.expProgCB/(float)MyApp.EXP_LEVEL_PER_COLI_BRAIN);
		((ImageButton) findViewById(R.id.colibrains_multi)).setImageDrawable(coliBrainDrawable);
		loadData();
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
    protected void onDestroy() {
		active = false;
		try {
            unregisterReceiver(mHandleMessageReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
		super.onDestroy();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Jeu.multijoueur = null;
		if(requestCode==1 && resultCode==RESULT_FIRST_USER) { // Afficher résultats de la partie qui a été terminée
			Intent intent = new Intent(this, Resultats.class);
			intent.putExtra("defi", new String[] {data.getStringExtra("defi")});
			startActivityForResult(intent, 2);
		} else if(requestCode==2 && resultCode==RESULT_FIRST_USER) { // Résultats vus.
			int[] resVus = data.getIntArrayExtra("resVus");
			for(int r=0; resVus[r]!=0; r++) {
				for(int i=0; i<adversaires.size(); i++) {
					if(adversaires.get(i).id==resVus[r]) {
						defi = adversaires.get(i);
						if(defi.type>0) { // Partie rapide
							base.removeDefi(adversaires.remove(i), MyApp.id);
						} else {
							defi.resVus = defi.nMatch;
							base.setResultatsVus(defi.id,defi.nMatch);
						}
						break;
					}
				}
			}
			adapt.notifyDataSetChanged();
			syncData();
		}
		if(messageDialog!=null) {
			messageDialog.show();
			messageDialog = null;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	/**
	 * Crée toutes les instances de Joueur pour l'utilisateur du jeu et pour tous ses adversaires en liste.
	 */
	private void loadData() {
		int userId = MyApp.id;
		if(MyApp.getApp().pref.contains("defi_fuit")) { // L'application a été quittée de force pendant un défi.
			int defiId = MyApp.getApp().pref.getInt("defi_fuit", 0);
			base.forfaitDefi(defiId, userId);
			MyApp.getApp().editor.remove("defi_fuit");
			MyApp.getApp().editor.commit();
			AlertDialog.Builder box = new AlertDialog.Builder(this);
			box.setTitle(R.string.forfait);
			box.setMessage(R.string.force_quit_msg);
			box.show();
		}
		loadJoueurs();
		if(userId==0) { // Utilisateur non inscrit !
			if(!connect.isConnectedToInternet()) {
				Toast.makeText(this, R.string.connexion_register, Toast.LENGTH_LONG).show();
				finish();
				return;
			}
			registerUser();
		} else {
			base.getDefis(userId,joueurs,adversaires);
			adapt = new DefiExpandableAdapter(this, userId, adversaires);
			lv.setAdapter(adapt);
			dispUser();
			if(!connect.isConnectedToInternet()) {
				Toast.makeText(this, R.string.hors_connexion, Toast.LENGTH_SHORT).show();
			} else {
				syncData();
			}
		}
	}
	
	private void loadJoueurs() {
		int userId = MyApp.id;
		base.getJoueurs(joueurs);
		System.out.println("Nombre de Joueurs : "+joueurs.size());
		user = joueurs.get(userId);
		if(user==null) {
			if(!connect.isConnectedToInternet()) {
				Toast.makeText(this, R.string.hors_connexion, Toast.LENGTH_SHORT).show();
				finish();
			} else {
				user = new Joueur(userId, MyApp.pseudo, "", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
				MyApp.last_update = 0;
			}
		}
	}
	
	public void choixNiveau() {
		boxNiv = new PaperDialog(this, R.layout.choix_niveau_multi);
		boxNiv.setTitle(defi.nom);
		LinearLayout lay = (LinearLayout) boxNiv.getContentView();
		Typeface font = Typeface.createFromAsset(getAssets(),"fonts/Passing Notes.ttf");
		((TextView) lay.getChildAt(0)).setTypeface(font);
		LinearLayout opt_aleat = (LinearLayout) lay.getChildAt(1);
		for(int i=0; i<opt_aleat.getChildCount(); i++) {
			((TextView) opt_aleat.getChildAt(i)).setTypeface(font);
		}
		boxNiv.show();
	}
	
	public void paramAleat(View v) {
		ParamAleat pa = new ParamAleat(new ParamAleat.callBackInterface() {
			@Override
			public void launchFunction(int mode) {
				newMatch(mode);
			}
		}, this, defi.getProgressMin());
		pa.show(); // Si appui sur "OK", lance un niveau aléatoire en mode PERSO.
	}
	
	public void facile(View v) {
		newMatch(Niveau.FACILE);
	}
	
	public void moyen(View v) {
		newMatch(Niveau.MOYEN);
	}

	public void difficile(View v) {
		newMatch(Niveau.DIFFICILE);
	}
	
	/**
	 * Lance un NOUVEAU niveau.
	 * @param mode
	 */
	public void newMatch(int mode) {
		boxNiv.dismiss();
		Jeu.multijoueur = new WeakReference<Multijoueur>(this);
		long seed = (new Random()).nextLong();
		Intent intent = new Intent(this, Jeu.class);
		intent.putExtra("startMsg", getString(R.string.c_est_parti)+" "+user.getPseudo()+" !")
			.putExtra("mode", mode)
			.putExtra("seed", seed)
			.putExtra("param", ParamAleat.param)
			.putExtra("avancement", defi.getProgressMin())
			.putExtra("defi", defi.toJSON());
		startActivityForResult(intent, 1);
	}
	
	/**
	 * Lance un niveau défini par le contenu de defi.match.
	 * 
	 */
	public void releverMatch() {
		Jeu.multijoueur = new WeakReference<Multijoueur>(this);
		Intent intent = new Intent(this, Jeu.class);
		intent.putExtra("startMsg", getString(R.string.c_est_parti)+" "+user.getPseudo()+" !")
			.putExtra("mode", defi.match.mode)
			.putExtra("seed", defi.match.seed)
			.putExtra("param", defi.match.param)
			.putExtra("avancement", defi.match.avancement)
			.putExtra("defi", defi.toJSON());
		startActivityForResult(intent, 1);
	}
	
	public void nouveauDefi(View v) {
		if(!connect.isConnectedToInternet() || user==null) {
			Toast.makeText(this, R.string.hors_connexion, Toast.LENGTH_SHORT).show();
			return;
		}
		if(base.getTasks().contains("\"task\":\"newDefi\"")) {
			syncData();
			return;
		}
		(new NewDefi(this, client, user.getId(), new NewDefi.callBackInterface() {
			@Override
			public void create(String nomDefi, JSONArray participants, int t_max) {
				base.newDefi(nomDefi, participants, t_max);
				syncData();
			}
		})).show();
	}
	
	public void partieRapide(View v) {
		if(!connect.isConnectedToInternet() || user==null) {
			Toast.makeText(this, R.string.hors_connexion, Toast.LENGTH_SHORT).show();
			return;
		}
		if(!base.getTasks().contains("\"task\":\"partieRapide\""))
			base.partieRapide();
		syncData();
	}
	
	public void supprDefi(View v) {
		final int groupPosition = (Integer) v.getTag();
		new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle(R.string.supprDefi)
			.setMessage(this.getString(R.string.supprDefiConf, adversaires.get(groupPosition).nom))
			.setPositiveButton(R.string.accept, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					base.removeDefi(adversaires.remove(groupPosition), user.getId());
					syncData();
				}
			})
			.setNegativeButton(R.string.annuler, null)
			.show();
	}
	
	public void actionDefi(View v) {
		if(System.currentTimeMillis() - lastPress < 1000) // Pour éviter les doubles clics
			return;
		lastPress = System.currentTimeMillis();
		final int groupPosition = (Integer) v.getTag();
		defi = adversaires.get(groupPosition);
		switch(defi.getEtat(user.getId())) {
		case Defi.ATTENTE: // Patience !
			Toast.makeText(this, R.string.patience, Toast.LENGTH_LONG).show();
			break;
		case Defi.RESULTATS: // Afficher les résultats
			ArrayList<String> defRes = new ArrayList<String>();
			int nDef = adversaires.size();
			for(int i=0; i<nDef; i++) {
				Defi d = adversaires.get((groupPosition + i)%nDef);
				if(d.getEtat(user.getId())==Defi.RESULTATS)
					defRes.add(d.toJSON());
			}
			Intent intent = new Intent(this, Resultats.class);
			intent.putExtra("defi", defRes.toArray(new String[0]));
			startActivityForResult(intent, 2);
			break;
		case Defi.RELEVER: // Lancer le jeu avec Seed donnée
			releverMatch();
			break;
		case Defi.LANCER: // Lancer nouveau défi
			choixNiveau();
			break;
		case Defi.OBSOLETE: // Deadline dépassée -> il faudrait synchroniser.
			Toast.makeText(this, R.string.obsolete_txt, Toast.LENGTH_LONG).show();
		}
	}
	
	/**
	 * Supprime le defi courant de la liste.
	 */
	public void removeDefi() {
		adversaires.remove(defi);
	}
	
	public void syncTotale(View v) {
		if(!connect.isConnectedToInternet()) {
			Toast.makeText(this, R.string.hors_connexion, Toast.LENGTH_SHORT).show();
			return;
		}
		MyApp.last_update = -Math.abs(MyApp.last_update);
		syncData();
	}
	
	private void disconnect() {
		final ProgressDialog prgDialog = new ProgressDialog(this);
		prgDialog.setMessage(getString(R.string.progress));
		prgDialog.setCancelable(false);
		prgDialog.show();
		RequestParams params = new RequestParams();
		params.setHttpEntityIsRepeatable(true);
		params.put("token", APP_TOKEN);
		params.put("joueur", ""+MyApp.id);
		params.put("appareil", ""+MyApp.appareil);
		params.put("tasks", base.getTasks());
		//System.out.println("Tasks sent: "+base.getTasks());
		params.put("progress", ""+MyApp.avancement);
		client.post(SERVER_URL+"/disconnect.php", params, new TextHttpResponseHandler() {
			@Override
			public void onSuccess(int statusCode, Header[] headers, String response) {
				prgDialog.dismiss();
				base.clearDB();
				boolean musique = MyApp.getApp().pref.getBoolean("musique", true);
				MyApp.getApp().editor.clear().commit();
				MyApp.getApp().editor.putBoolean("musique", musique);
				MyApp.getApp().loadData();
				registerUser();
			}

			@Override
			public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
				prgDialog.dismiss();
				Toast.makeText(Multijoueur.this, R.string.sync_fail, Toast.LENGTH_SHORT).show();
			}
		});
	}
	
	public void profileClick(View v) {
		new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle(R.string.disconnect_title)
			.setMessage(this.getString(R.string.disconnect))
			.setPositiveButton(R.string.accept, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					disconnect();
				}
			})
			.setNegativeButton(R.string.annuler, null)
			.show();
	}
	
	private void dispUser() {
		((ImageView) findViewById(R.id.user_avatar)).setImageResource(user.getAvatar());
		((TextView) findViewById(R.id.user_name)).setText(user.getPseudo());
		// Affichage expérience et son gain
		String expStr = getString(R.string.exp)+" :\n\u0009"+String.format("%,d", user.getExp());
		int dbSpan = expStr.length();
		if(user.getExp()>MyApp.experience) {
			expStr += " +"+String.format("%,d", user.getExp() - MyApp.experience);
		}
		SpannableString expTxt = new SpannableString(expStr);
		expTxt.setSpan(new RelativeSizeSpan(1.2f), dbSpan, expTxt.length(), 0);
		expTxt.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.violet)), dbSpan, expTxt.length(), 0);
		((TextView) findViewById(R.id.user_exp)).setText(expTxt);
		// Affichage score et son gain
		String scoreStr = getString(R.string.score_compet)+" :\n\u0009"+String.format("%,.2f", user.getScore());
		int dbSpan2 = scoreStr.length();
		double scoreDiff = user.getScore() - MyApp.getApp().pref.getFloat("score", (float) user.getScore());
		if(scoreDiff >= 1.) {
			scoreStr += (scoreDiff<0 ? " " : " +")+String.format("%,.2f", scoreDiff);
		}
		SpannableString scoreTxt = new SpannableString(scoreStr);
		scoreTxt.setSpan(new RelativeSizeSpan(1.2f), dbSpan2, scoreTxt.length(), 0);
		scoreTxt.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.violet)), dbSpan2, scoreTxt.length(), 0);
		((TextView) findViewById(R.id.user_score)).setText(scoreTxt);
		// Affichage défis
		((TextView) findViewById(R.id.user_defis)).setText(getString(R.string.defis_joues)+" :\n\u0009"+user.getDefis());
		// Affichage ColiBrains
		((ColiBrain) ((ImageButton) findViewById(R.id.colibrains_multi)).getDrawable())
			.setProgress(MyApp.expProgCB/(float)MyApp.EXP_LEVEL_PER_COLI_BRAIN)
			.setText(""+MyApp.coliBrains);
	}
	
	private final BroadcastReceiver mHandleMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String newMessage = intent.getExtras().getString(EXTRA_MESSAGE);
			System.out.println("onReceiveNotification : "+newMessage);
			String title = context.getString(R.string.notification), msg=newMessage;
			try {
				JSONObject o = new JSONObject(newMessage);
				String typ = o.getString("type");
				if(typ.equals("newMatch")) {
					title = o.getString("nomDefi");
					msg = context.getString(R.string.notif_newdefi, o.getString("initPlayer"));
				} else if(typ.equals("results")) {
					title = o.getString("nomDefi");
					if(o.has("initPlayer"))
						msg = context.getString(R.string.notif_results, o.getString("initPlayer"));
					else
						msg = context.getString(R.string.notif_results_exp);
				} else if(typ.equals("message")) {
					if(o.has("title"))
						title = o.getString("title");
					msg = o.getString("message");
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
			boolean inGame = (Jeu.multijoueur!=null);
			Toast.makeText(Multijoueur.this, title+"|"+msg, inGame ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG, inGame).show();
			long lastSync = MyApp.getApp().pref.getLong("lastNotif", 0);
			if(System.currentTimeMillis() - lastSync > 5000 && !inGame)
				syncData();
		}
	};
	
	private void registerUser() {
		(new RegisterUser(this, client, new RegisterUser.callBackInterface() {
			@Override
			public boolean registered(String JSONresponse, String name, boolean sync) {
				try {
					JSONArray j = new JSONArray(JSONresponse);
					base.insertJSONJoueurs(j);
					MyApp.getApp().connectUser(j.getJSONObject(0).getInt("id"), name, j.getJSONObject(0).getInt("appareil"));
					loadJoueurs();
					base.getDefis(user.getId(),joueurs,adversaires);
					adapt = new DefiExpandableAdapter(Multijoueur.this, user.getId(), adversaires);
					lv.setAdapter(adapt);
					syncData();
					return true;
				} catch (JSONException e) {
					e.printStackTrace();
					return false;
				}
			}
			@Override
			public void cancelled() {
				finish();
			}
		})).show();
	}
	
	public void syncData() {
		adapt.notifyDataSetChanged();
		if(!connect.isConnectedToInternet() || !adapt.getLaunchEnabled())
			return;
		adapt.setLaunchEnabled(false);
		loader.showNext();
		loader.setEnabled(false);
		((TextView) findViewById(R.id.nvDefi)).setEnabled(false);
		((TextView) findViewById(R.id.nvPRapide)).setEnabled(false);
		int coliBrainsLastSync = MyApp.getApp().pref.getInt("coliBrainsLastSync", 0);
		long playTimeLastSync = MyApp.getApp().pref.getLong("playTimeLastSync", 0);
		if(playTimeLastSync!=MyApp.playTime || MyApp.expToSync!=0 || MyApp.coliBrains!=coliBrainsLastSync) {
			int coliBrainsWon = MyApp.cumulExpCB/MyApp.EXP_LEVEL_PER_COLI_BRAIN;
			if(MyApp.cumulExpCB % MyApp.EXP_LEVEL_PER_COLI_BRAIN > MyApp.expProgCB)
				coliBrainsWon++;
			// coliBrainsDiff = coliBrains-lastColiBrains = won - used <=> used = won + lastColiBrains - coliBrains
			base.syncExpAndColiBrains(MyApp.playTime - playTimeLastSync, MyApp.expToSync, MyApp.cumulExpCB, coliBrainsWon + coliBrainsLastSync - MyApp.coliBrains);
			MyApp.expToSync = 0;
			MyApp.cumulExpCB = 0;
			MyApp.getApp().editor
				.putLong("playTimeLastSync", MyApp.playTime)
				.putInt("coliBrainsLastSync", MyApp.coliBrains)
				.putInt("expToSync", MyApp.expToSync)
				.putInt("cumulExpCB", MyApp.cumulExpCB)
				.commit();
		}
		RequestParams params = new RequestParams();
		params.setHttpEntityIsRepeatable(true);
		params.put("token", APP_TOKEN);
		params.put("joueur", ""+MyApp.id);
		params.put("appareil", ""+MyApp.appareil);
		params.put("tasks", base.getTasks());
		//System.out.println("Tasks sent: "+base.getTasks());
		params.put("last_update", ""+MyApp.last_update);
		params.put("progress", ""+MyApp.avancement);
		client.post(SERVER_URL+"/sync_data.php", params, new TextHttpResponseHandler() {
			@Override
			public void onSuccess(int statusCode, Header[] headers, String response) {
				loader.showNext();
				loader.setEnabled(true);
				((TextView) findViewById(R.id.nvDefi)).setEnabled(true);
				((TextView) findViewById(R.id.nvPRapide)).setEnabled(true);
				adapt.setLaunchEnabled(true);
				MyApp.getApp().editor.putLong("lastNotif", System.currentTimeMillis()).commit();
				if(insertJSONData(response)) {
					base.clearTasks();
				}
			}

			@Override
			public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
				loader.showNext();
				loader.setEnabled(true);
				((TextView) findViewById(R.id.nvDefi)).setEnabled(true);
				((TextView) findViewById(R.id.nvPRapide)).setEnabled(true);
				adapt.setLaunchEnabled(true);
				adapt.notifyDataSetChanged();
				Toast.makeText(Multijoueur.this, R.string.sync_fail, Toast.LENGTH_SHORT).show();
			}
		});
	}
	
	/**
	 * Insert ou met à jour les défis contenus dans def sous le format :
	 * {defis:[{Defi},{Defi},...],participations:[{Participation},{Participation},...],joueurs:[{Joueur},{Joueur},...],tasks:[{task:"delete", defi:3},{task:"message", msg:"Message!"},...]}
	 * @param def
	 */
	private boolean insertJSONData(String def) {
		boolean res;
		//System.out.println("Sync Data Received: "+def);
		long last_up = 0;
		try {
			JSONObject o = new JSONObject(def);
			last_up = o.getLong("last_update");
			if(MyApp.last_update<=0) { // Sync totale
				base.taskSyncTotale(user.getId());
			}
			if(o.has("tasks"))
				messageDialog = base.execJSONTasks(this, (JSONArray) o.get("tasks"), user.getId());
			if(o.has("joueurs")) {
				base.insertJSONJoueurs((JSONArray) o.get("joueurs"));
				loadJoueurs();
			}
			if(o.has("defis"))
				base.insertJSONDefis((JSONArray) o.get("defis"));
			if(o.has("participations"))
				base.insertJSONParticipations((JSONArray) o.get("participations"));
			res = true;
		} catch (JSONException e) {
			e.printStackTrace();
			System.out.println(def);
			res = false;
			if(def.equalsIgnoreCase("upgrade"))
				Toast.makeText(Multijoueur.this, R.string.maj_req, Toast.LENGTH_LONG).show();
			else
				Toast.makeText(Multijoueur.this, R.string.err500, Toast.LENGTH_LONG).show();
		}
		if(messageDialog!=null && Jeu.multijoueur==null) { // S'il y a un message à afficher et qu'on est pas en cours de jeu
			messageDialog.show();
			messageDialog = null;
		}
		if(res) {
			MyApp.experience = user.getExp();
			MyApp.avancement = user.getProgress();
			MyApp.playTime = user.getPlayTime();
			MyApp.last_update = last_up;
			MyApp.coliBrains = user.getColiBrains();
			MyApp.expProgCB = user.getExpProgCB();
			MyApp.getApp().editor
				.putLong("playTimeLastSync", MyApp.playTime)
				.putInt("coliBrainsLastSync", MyApp.coliBrains)
				.putFloat("score", (float) user.getScore()) // Pour l'affichage du score gagné après synchro
				.putInt("nNewM", 0) // Pour les notifications
				.putInt("nRes", 0);
			MyApp.getApp().saveData();
			dispUser();
		}
		int pRapide = base.getDefis(user.getId(),joueurs,adversaires);
		adapt.notifyDataSetChanged();
		if(pRapide!=-1) {
			View v = new View(this);
			v.setTag(pRapide);
			actionDefi(v);
		}
		return res;
	}
	
}
