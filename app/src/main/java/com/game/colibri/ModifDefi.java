package com.game.colibri;

import android.app.Dialog;
import android.content.Context;
import android.util.SparseArray;
import android.view.View;

import com.loopj.android.http.AsyncHttpClient;

import java.util.ArrayList;

public class ModifDefi extends ConfigDefi {

	private callBackInterface callback;
	private Defi defi;
	private boolean userRemoved;

	public ModifDefi(Context context, AsyncHttpClient client, int user, Defi defi, callBackInterface callback) {
		super(context, client, user, defi.getJoueurs(user));
		this.defi = defi;
		this.callback= callback;
	}

	@Override
	void okButtonClick(final Dialog dialog) {
		final SparseArray<Participation> prevJoueurs = defi.participants.clone();
		final ArrayList<Integer> addedJoueurs = new ArrayList<>();
		for(int i = 0; i < jAdapter.getCount(); i++) {
			Joueur j = jAdapter.getItem(i);
			if(prevJoueurs.get(j.getId()) == null) {
				addedJoueurs.add(j.getId());
			} else {
				prevJoueurs.remove(j.getId());
			}
		}
		// Il ne reste dans prevJoueurs que les joueurs qui ont été retirés
		final ArrayList<Integer> removedJoueurs = new ArrayList<>();
		final StringBuilder removedJoueursNames = new StringBuilder();
		userRemoved = false;
		for(int i = 0; i < prevJoueurs.size(); i++) {
			Joueur j = prevJoueurs.valueAt(i).joueur;
			removedJoueurs.add(j.getId());
			removedJoueursNames.append(", ").append(j.getPseudo());
			if(j.getId() == user) userRemoved = true;
		}
		removedJoueursNames.delete(0, 2);
		if(removedJoueurs.size() == 0 && jAdapter.getCount() > 1) {
			dialog.dismiss();
			callback.setNewParticipants(defi, removedJoueurs, addedJoueurs);
			return;
		}
		final PaperDialog confirmModif = new PaperDialog(context, 0);
		if(jAdapter.getCount() <= 1) {
			confirmModif.setTitle(R.string.supprDefi);
			confirmModif.setMessage(context.getString(R.string.supprDefiComplConf, defi.nom));
			userRemoved = false;
		} else if(!userRemoved || removedJoueurs.size() > 1) { // Il y a d'autres joueurs que user
			confirmModif.setTitle(R.string.supprParticipConfTitle);
			confirmModif.setMessage(context.getString(R.string.supprParticipConf, defi.nom, removedJoueursNames.toString()));
		} else {
			confirmModif.setTitle(R.string.supprDefi);
			confirmModif.setMessage(context.getString(R.string.supprDefiConf, defi.nom));
			userRemoved = false;
		}
		confirmModif.setPositiveButton(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				confirmModif.dismiss();
				if(userRemoved) {
					userRemoved = false;
					confirmModif.setTitle(R.string.supprDefi);
					confirmModif.setMessage(context.getString(R.string.supprDefiConf, defi.nom));
					confirmModif.show();
				} else {
					dialog.dismiss();
					callback.setNewParticipants(defi, removedJoueurs, addedJoueurs);
				}
			}
		}, null);
		confirmModif.setNegativeButton(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				confirmModif.dismiss();
			}
		}, null);
		confirmModif.show();
	}

	@Override
	String getOkLabel() {
		return null; // Texte par défaut : OK
	}

	public interface callBackInterface {
		void setNewParticipants(Defi defi, ArrayList<Integer> removedJoueurs, ArrayList<Integer> addedJoueurs);
	}
}
