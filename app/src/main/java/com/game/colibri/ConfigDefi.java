package com.game.colibri;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.game.colibri.DropDownAdapter.NameAndId;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

import org.json.JSONArray;

import java.util.List;

import cz.msebera.android.httpclient.Header;

import static com.network.colibri.CommonUtilities.SERVER_URL;

public abstract class ConfigDefi {
	private static final int MAX_PLAYERS = 50;

	Context context;
	int user;
	private DropDownAdapter dropDownAdapter;
	JoueursAdapter jAdapter;
	private AsyncHttpClient client;
	private ProgressDialog prgDialog;
	private boolean modifDefi;

	public ConfigDefi(Context context, AsyncHttpClient client, int user, List<Joueur> joueurs) {
		this.context = new ContextThemeWrapper(context, android.R.style.Theme_Holo_Light_Dialog);
		this.client = client;
		this.user = user;
		modifDefi = !joueurs.isEmpty();
		jAdapter = new JoueursAdapter(context, R.layout.element_joueur, joueurs);
		dropDownAdapter = new DropDownAdapter(context, R.layout.simple_list_element, user, joueurs);
		prgDialog = new ProgressDialog(this.context);
		prgDialog.setMessage(context.getString(R.string.progress));
		prgDialog.setCancelable(false);
	}

	abstract void okButtonClick(Dialog dialog);

	abstract String getOkLabel();
	
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
		jAdapter.setDialogBox(defiBox);
		jlv.setAdapter(jAdapter);
		jAdapter.updateTextView();
		jlv.setEmptyView((TextView) defiBox.findViewById(R.id.defaultView));

		defiBox.setCancelable(false);
		defiBox.setPositiveButton(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				okButtonClick(defiBox);
			}
		}, getOkLabel());
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

	/**
	 * JSONArray des id des adversaires incluant userToAdd si différent de 0.
	 * @param userToAdd id du joueur à ajouter
	 * @return la liste des adversaires ou des joueurs du défi
	 */
	JSONArray getJSONListOfJoueurs(int userToAdd) {
		JSONArray joueurs = new JSONArray();
		if(userToAdd != 0) joueurs.put(userToAdd);
		int fin = jAdapter.getCount();
		for(int i=0; i<fin; i++) {
			joueurs.put(jAdapter.getItem(i).getId());
		}
		return joueurs;
	}
	
	private void addJoueur(NameAndId j) {
		if(jAdapter.getCount() + (modifDefi ? 0 : 1) >= MAX_PLAYERS) {
			Toast.makeText(context, R.string.nojoueurfound, Toast.LENGTH_LONG).show();
			return;
		}
		prgDialog.show();
		RequestParams params = new RequestParams();
		params.setHttpEntityIsRepeatable(true);
		if(j==null) // Mode auto. On spécifie la liste des joueurs déjà pris.
			params.put("auto", getJSONListOfJoueurs(modifDefi ? 0 : user).toString());
		else
			params.put("joueur", ""+j.id);
		client.post(SERVER_URL+"/get_joueur.php", params, new TextHttpResponseHandler() {
			@Override
			public void onSuccess(int statusCode, Header[] headers, String response) {
				prgDialog.dismiss();
				Gson g = new Gson();
				try {
					Joueur j = g.fromJson(response, Joueur.class);
					jAdapter.add(j);
					jAdapter.updateTextView();
					jAdapter.notifyDataSetChanged();
				} catch (JsonSyntaxException e) {
					Toast.makeText(context, R.string.nojoueurfound, Toast.LENGTH_LONG).show();
					return;
				}
			}

			@Override
			public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
				prgDialog.dismiss();
				Toast.makeText(context, R.string.err, Toast.LENGTH_LONG).show();
			}
		});
	}
}
