package com.game.colibri;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

/**
 * Implémentation spécifique de Toast pour Colibri !
 * Implémente le makeText du Toast standard avec possibilité d'avoir titre et contenu
 * en utilisant le séparateur '|' :
 * ex : "titre|contenu" OU "contenu" OU "titre|"
 * 
 * @author Louis
 *
 */
public class Toast extends android.widget.Toast {
	
	public static Toast makeText(Context context, int resId, int duration) {
		return new Toast(context, context.getText(resId).toString(), duration, false);
	}
	
	public static Toast makeText(Context context, CharSequence text, int duration) {
		return new Toast(context, text.toString(), duration, false);
	}
	
	public static Toast makeText(Context context, CharSequence text, int duration, boolean inGame) {
		return new Toast(context, text.toString(), duration, inGame);
	}
	
	@SuppressLint("InflateParams")
	public Toast(Context context, String text, int duration, boolean inGame) {
		super(context);
		LayoutInflater inflater = LayoutInflater.from(context);
		View toastView = inflater.inflate(R.layout.toast_custom, null);
		int sep = text.indexOf("|");
		String title, content;
		if(sep>=0) { // Titre et contenu
			title = text.substring(0, sep);
			content = text.substring(sep+1);
		} else {
			title = "";
			content = text;
		}
		TextView titleTV = (TextView) toastView.findViewById(R.id.toast_title);
		TextView contentTV = (TextView) toastView.findViewById(R.id.toast_content);
		if(title.length()!=0) {
			titleTV.setTypeface(Typeface.createFromAsset(context.getAssets(),"fonts/Passing Notes.ttf"));
			titleTV.setText(title);
		} else
			titleTV.setVisibility(View.GONE);
		if(content.length()!=0) {
			contentTV.setTypeface(Typeface.createFromAsset(context.getAssets(),"fonts/Passing Notes.ttf"));
			contentTV.setText(content);
		} else
			contentTV.setVisibility(View.GONE);
		if(inGame)
			toastView.setBackgroundResource(R.drawable.toast_dis);
		else
			toastView.setBackgroundResource(R.drawable.toast);
		setView(toastView);
		setGravity(Gravity.CENTER_HORIZONTAL | (inGame ? Gravity.TOP : Gravity.CENTER_VERTICAL), 0, 0);
		setDuration(duration);
	}
	
}
