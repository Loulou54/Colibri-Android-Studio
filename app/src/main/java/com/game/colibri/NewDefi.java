package com.game.colibri;

import java.util.ArrayList;
import org.json.JSONArray;
import com.loopj.android.http.AsyncHttpClient;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;

public class NewDefi extends ConfigDefi {

	private callBackInterface callback;
	private String nomDefi;
	private int t_max;
	
	@SuppressLint("InlinedApi")
	public NewDefi(Context context, AsyncHttpClient client, int user, callBackInterface callback) {
		super(context, client, user, new ArrayList<Joueur>());
		nomDefi = "";
		this.callback = callback;
	}

	@Override
	void okButtonClick(Dialog dialog) {
		if(jAdapter.getCount()==0) {
			Toast.makeText(context, R.string.nojoueur, Toast.LENGTH_LONG).show();
		} else {
			dialog.dismiss();
			callback.create(nomDefi, getJSONListOfJoueurs(user), t_max);
		}
	}

	@Override
	String getOkLabel() {
		return context.getResources().getString(R.string.creer);
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
