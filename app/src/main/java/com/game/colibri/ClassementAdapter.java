package com.game.colibri;

import static com.network.colibri.CommonUtilities.SERVER_URL;
import static com.network.colibri.CommonUtilities.addServerCACertToClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestHandle;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import cz.msebera.android.httpclient.Header;

public class ClassementAdapter extends ArrayAdapter<Joueur> {
	
	private Classements classements;
	private AsyncHttpClient client;
	private int type;
	private boolean filterFriends;
	private String search;
	private boolean endOfList, isLoading;
	private Typeface font;
	private long failTimeStamp = 0; // Pour éviter les rafales de requête en cas d'erreurs répétées
	
	public ClassementAdapter(Classements c, int t, boolean f, String s) {
		super(c, R.layout.element_classement, new ArrayList<Joueur>());
		client = new AsyncHttpClient();
		client.setMaxRetriesAndTimeout(5, 500);
		addServerCACertToClient(getContext(), client);
		classements = c;
		type = t;
		font = Typeface.createFromAsset(c.getAssets(),"fonts/Passing Notes.ttf");
		initRanking(f, s);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Joueur j = getItem(position);
		if(j==null) {
			return new ProgressBar(getContext());
		}
		final ViewHolder h;
		if(convertView==null || convertView.getTag()==null) {
			LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.element_classement, parent, false);
			h = new ViewHolder();
			h.rank = (TextView) convertView.findViewById(R.id.classAdv);
			h.avatar = (ImageView) convertView.findViewById(R.id.avatarAdv);
			h.nom = (TextView) convertView.findViewById(R.id.nomAdv);
			h.nom.setTypeface(font);
			h.nom.setSelected(true);
			h.nom.setHorizontallyScrolling(true);
			h.pays = (ImageView) convertView.findViewById(R.id.paysAdv);
			h.exp = (TextView) convertView.findViewById(R.id.expAdv);
			h.score = (TextView) convertView.findViewById(R.id.scoreAdv);
			h.defis = (TextView) convertView.findViewById(R.id.defisAdv);
			h.wins = (TextView) convertView.findViewById(R.id.winDefisAdv);
			h.winLost = (TextView) convertView.findViewById(R.id.winLostAdv);
			TextView selected =
					type==0 ? h.exp
					: type==1 ? h.score
					: type==2 ? h.defis
					: type==3 ? h.winLost
					: null;
			if(selected!=null)
				selected.setTypeface(selected.getTypeface(), Typeface.BOLD);
			convertView.setTag(h);
		} else {
			h = (ViewHolder) convertView.getTag();
		}
		h.rank.setText(""+j.getRang());
		h.avatar.setImageResource(j.getAvatar());
		h.nom.setText(j.getPseudo());
		try {
			InputStream file = getContext().getAssets().open("drapeaux/"+j.getPays().toLowerCase(Locale.FRANCE)+".png");
			h.pays.setImageBitmap(BitmapFactory.decodeStream(file));
			h.pays.setVisibility(View.VISIBLE);
		} catch (IOException e) {
			h.pays.setVisibility(View.INVISIBLE);
		}
		h.exp.setText(String.format("%,d", j.getExp()));
		h.score.setText(String.format("%,.2f", j.getScore()));
		h.defis.setText(""+j.getDefis());
		h.wins.setText(""+j.getWin());
		h.winLost.setText(""+(j.getWin() - j.getLoose()));
		return convertView;
	}
	
	static class ViewHolder {
		ImageView avatar, pays;
		TextView rank, nom, exp, score, defis, wins, winLost;
	}
	
	public RequestHandle refresh(boolean f, String s) {
		if(f!=filterFriends || !s.equalsIgnoreCase(search))
			return initRanking(f, s);
		else
			return null;
	}
	
	public RequestHandle initRanking(boolean f, String s) {
		filterFriends = f;
		search = s;
		isLoading = false;
		endOfList = false;
		clear();
		return loadNext();
	}
	
	public RequestHandle loadNext() {
		if(isLoading || endOfList || System.currentTimeMillis()-failTimeStamp < 5000)
			return null;
		RequestParams params = new RequestParams();
		params.setHttpEntityIsRepeatable(true);
		params.put("joueur", MyApp.id==0 ? "" : ""+MyApp.id);
		params.put("type", ""+type);
		if(filterFriends)
			params.put("amis", "OUI");
		if(search.length()>0)
			params.put("search", search);
		params.put("length", ""+this.getCount());
		isLoading = true;
		add(null); // Pour afficher l'icône de chargement
		return client.post(SERVER_URL+"/classement.php", params, new TextHttpResponseHandler() {
			@Override
			public void onSuccess(int statusCode, Header[] headers, String response) {
				isLoading = false;
				remove(null);
				try {
					Gson g = new Gson();
					JSONObject o = new JSONObject(response);
					if(o.has("nJoueurs")) {
						classements.nJoueurs = o.getInt("nJoueurs");
						if(o.has("user"))
							classements.userRanks[type] = g.fromJson(o.getString("user").toString(), Joueur.class);
						classements.setUserInfos();
					}
					Joueur[] classement = g.fromJson(o.getString("ranking"), Joueur[].class);
					endOfList = (classement.length<6);
					for(Joueur j : classement) {
						j.computeLastVisit();
						ClassementAdapter.this.add(j);
					}
				} catch(JsonSyntaxException e) {
					e.printStackTrace();
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}

			@Override
			public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
				failTimeStamp = System.currentTimeMillis();
				isLoading = false;
				remove(null);
				if (statusCode == 404) {
					Toast.makeText(ClassementAdapter.this.getContext(), R.string.err404, Toast.LENGTH_LONG).show();
				} else if (statusCode == 500 || statusCode == 503) {
					Toast.makeText(ClassementAdapter.this.getContext(), R.string.err500, Toast.LENGTH_LONG).show();
				} else {
					Toast.makeText(ClassementAdapter.this.getContext(), R.string.err, Toast.LENGTH_LONG).show();
				}
			}
		});
	}
}
