package com.game.colibri;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Build;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class DispJoueur {
	
	private Context context;
	private Joueur j;
	
	@SuppressLint("InlinedApi")
	public DispJoueur(Context context, Joueur joueur) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			this.context = new ContextThemeWrapper(context, android.R.style.Theme_Holo_Light_Dialog);
		} else {
			this.context = context;
		}
		j = joueur;
	}
	
	public void show() {
		PaperDialog box = new PaperDialog(context, R.layout.details_joueur_layout);
		((TextView) box.findViewById(R.id.nomDetails)).setText(j.getPseudo());
		((TextView) box.findViewById(R.id.nomDetails)).setTypeface(Typeface.createFromAsset(context.getAssets(),"fonts/Passing Notes.ttf"));
		((ImageView) box.findViewById(R.id.avatarDetails)).setImageResource(j.getAvatar());
		((TextView) box.findViewById(R.id.paysDetails)).setText((new Locale("", j.getPays())).getDisplayCountry(Resources.getSystem().getConfiguration().locale));
		ImageView pays = (ImageView) box.findViewById(R.id.flagDetails);
		try {
			InputStream file = context.getAssets().open("drapeaux/"+j.getPays().toLowerCase(Locale.FRANCE)+".png");
			pays.setImageBitmap(BitmapFactory.decodeStream(file));
			pays.setVisibility(View.VISIBLE);
		} catch (IOException e) {
			pays.setVisibility(View.INVISIBLE);
		}
		((TextView) box.findViewById(R.id.lastConnectDetails)).setText(""+j.getLastVisit(context));
		((TextView) box.findViewById(R.id.progressDetails)).setText(""+(j.getProgress()-1)+"/"+Jeu.NIV_MAX);
		((TextView) box.findViewById(R.id.expDetails)).setText(String.format("%,d", j.getExp()));
		((TextView) box.findViewById(R.id.scoreDetails)).setText(String.format("%,.2f", j.getScore()));
		((TextView) box.findViewById(R.id.defisDetails)).setText(""+j.getDefis());
		((TextView) box.findViewById(R.id.winsDetails)).setText(""+j.getWin());
		((TextView) box.findViewById(R.id.looseDetails)).setText(""+j.getLoose());
		long pT_mins = j.getPlayTime()/60000;
		((TextView) box.findViewById(R.id.playTime)).setText(context.getString(R.string.play_time_format, pT_mins/60, pT_mins%60));
		box.show();
	}
	
}
