package com.game.colibri;

import static com.network.colibri.CommonUtilities.SERVER_URL;

import java.util.ArrayList;

import org.json.JSONArray;

import com.game.colibri.DropDownAdapter.NameAndId;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Build;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import cz.msebera.android.httpclient.Header;

public class NewDefi {
	
	private Context context;
	private callBackInterface callback;
	private String nomDefi;
	private int user, t_max;
	private DropDownAdapter dropDownAdapter;
	private JoueursAdapter jAdapter;
	private AsyncHttpClient client;
	private ProgressDialog prgDialog;
	
	@SuppressLint("InlinedApi")
	public NewDefi(Context context, AsyncHttpClient client, int user, callBackInterface callback) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			this.context = new ContextThemeWrapper(context, android.R.style.Theme_Holo_Light_Dialog);
		} else {
			this.context = context;
		}
		nomDefi = "";
		this.client = client;
		this.callback= callback;
		this.user = user;
		ArrayList<Joueur> joueurs = new ArrayList<Joueur>();
		jAdapter = new JoueursAdapter(context, R.layout.element_joueur, joueurs);
		dropDownAdapter = new DropDownAdapter(context, R.layout.simple_list_element, user, joueurs);
		prgDialog = new ProgressDialog(this.context);
		prgDialog.setMessage(context.getString(R.string.progress));
		prgDialog.setCancelable(false);
	}
	
	/**
	 * Affiche la boîte de dialogue de création de nouveau défi.
	 */
	public void show() {
		final PaperDialog defiBox = new PaperDialog(context, R.layout.newdefi_layout1, true);
		defiBox.setTitle(R.string.nouveau_defi);
		((EditText) defiBox.findViewById(R.id.defiName)).setText(nomDefi);
		defiBox.setPositiveButton(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				nomDefi = ((EditText) defiBox.findViewById(R.id.defiName)).getText().toString().trim();
				t_max = fetchTimeSecond(((Spinner) defiBox.findViewById(R.id.defiLimit)).getSelectedItemPosition());
				if(nomDefi.length()>30) {
					Toast.makeText(context, R.string.nom_invalide_defi, Toast.LENGTH_LONG).show();
				} else {
					defiBox.dismiss();
					showParticipants();
				}
			}
		}, null);
		defiBox.setNegativeButton(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				defiBox.dismiss();
			}
		}, null);
		defiBox.show();
	}
	
	/**
	 * Affiche la boîte de dialogue de recherche de participants.
	 */
	public void showParticipants() {
		final PaperDialog defiBox = new PaperDialog(context, R.layout.newdefi_layout2, true);
		final SuggestionsEditText actv = (SuggestionsEditText) defiBox.findViewById(R.id.searchAdv);
		actv.setLoadingIndicator((ProgressBar) defiBox.findViewById(R.id.loading_indicator)); 
		actv.setAdapter(dropDownAdapter);
		actv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				addJoueur(dropDownAdapter.getItem(arg2));
				actv.setText("");
			}
		});
		ImageButton advAuto = (ImageButton) defiBox.findViewById(R.id.advAuto);
		advAuto.setOnClickListener(new ImageButton.OnClickListener() {
			@Override
			public void onClick(View v) {
				addJoueur(null);
				hide_keyboard_from(context, actv);
			}
		});
		ListView jlv = (ListView) defiBox.findViewById(R.id.listAdv);
		jAdapter.setTextView((TextView) defiBox.findViewById(R.id.advTextView));
		jlv.setAdapter(jAdapter);
		jAdapter.updateTextView();
		jlv.setEmptyView((TextView) defiBox.findViewById(R.id.defaultView));
		
		defiBox.setTitle(R.string.nouveau_defi);
		defiBox.setCancelable(false);
		defiBox.setPositiveButton(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(jAdapter.getCount()==0) {
					Toast.makeText(context, R.string.nojoueur, Toast.LENGTH_LONG).show();
				} else {
					defiBox.dismiss();
					createDefi();
				}
			}
		}, context.getResources().getString(R.string.creer));
		defiBox.setNegativeButton(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				prgDialog.dismiss();
				defiBox.dismiss();
			}
		}, null);
		defiBox.show();
	}
	
	public static void hide_keyboard_from(Context context, View view) {
	    InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
	    inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
	}
	
	private JSONArray getJSONListOfJoueurs() {
		JSONArray joueurs = new JSONArray();
		joueurs.put(user);
		int fin = jAdapter.getCount();
		for(int i=0; i<fin; i++) {
			joueurs.put(jAdapter.getItem(i).getId());
		}
		return joueurs;
	}
	
	private void addJoueur(NameAndId j) {
		prgDialog.show();
		RequestParams params = new RequestParams();
		params.setHttpEntityIsRepeatable(true);
		if(j==null) // Mode auto. On spécifie la liste des joueurs déjà pris.
			params.put("auto", getJSONListOfJoueurs().toString());
		else
			params.put("joueur", ""+j.id);
		client.post(SERVER_URL+"/get_joueur.php", params, new TextHttpResponseHandler() {
			@Override
			public void onSuccess(int statusCode, Header[] headers, String response) {
				prgDialog.hide();
				Gson g = new Gson();
				try {
					Joueur j = g.fromJson(response, Joueur.class);
					jAdapter.add(j);
					jAdapter.updateTextView();
					jAdapter.notifyDataSetChanged();
				} catch (JsonSyntaxException e) {
					System.out.println(response);
					Toast.makeText(context, R.string.nojoueurfound, Toast.LENGTH_LONG).show();
					return;
				}
			}

			@Override
			public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
				prgDialog.hide();
				Toast.makeText(context, R.string.err, Toast.LENGTH_LONG).show();
			}
		});
	}
	
	private void createDefi() {
		callback.create(nomDefi, getJSONListOfJoueurs(), t_max);
	}
	
	public interface callBackInterface {
		void create(String nomDefi, JSONArray participants, int t_max);
	}
	
	/**
	 * Retourne la limite temporelle en secondes correspondant à l'élément choisi dans le spinner.
	 * @param pos position de l'item choisi
	 * @return temps en s
	 */
	private static int fetchTimeSecond(int pos) {
		switch (pos) {
		case 0:
			return 0;
		case 1:
			return 3600*24;
		case 2:
			return 3600*24*2;
		case 3:
			return 3600*24*3;
		case 4:
			return 3600*24*5;
		case 5:
			return 3600*24*7;
		case 6:
			return 3600*24*10;
		case 7:
			return 3600*24*7*2;
		case 8:
			return 3600*24*7*3;
		case 9:
			return 3600*24*7*4;
		default:
			return 0;
		}
	}
}
