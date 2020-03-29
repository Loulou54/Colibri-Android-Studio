package com.game.colibri;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ResultatsAdapter extends ArrayAdapter<Participation> {
	
	public static double prog;
	public static int etape;
	private Typeface font;
	private int partEffectives = 0;
	private int t_max = 0;
	
	public ResultatsAdapter(Context context, Participation[] joueurs) {
		super(context, R.layout.element_resultat, joueurs);
		font = Typeface.createFromAsset(context.getAssets(),"fonts/Passing Notes.ttf");
		for(Participation p : joueurs) {
			if(p.t_fini < Participation.FORFAIT && p.t_fini > t_max)
				t_max = p.t_fini;
			if(p.t_fini!=Participation.NOT_PLAYED)
				partEffectives++;
		}
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Participation p = getItem(position);
		ViewHolder h;
		if(convertView==null) {
			LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.element_resultat, parent, false);
			h = new ViewHolder();
			h.rang = (TextView) convertView.findViewById(R.id.rangJoueurRes);
			h.avatar = (ImageView) convertView.findViewById(R.id.avatarRes);
			h.nom = (TextView) convertView.findViewById(R.id.nomJoueurRes);
			h.nom.setTypeface(font);
			h.nom.setSelected(true);
			h.nom.setHorizontallyScrolling(true);
			h.temps = (TextView) convertView.findViewById(R.id.tempsJoueurRes);
			h.score = (TextView) convertView.findViewById(R.id.scoreJoueurRes);
			h.cumul_score = (TextView) convertView.findViewById(R.id.cumulScoreJoueurRes);
			convertView.setTag(h);
		} else {
			h = (ViewHolder) convertView.getTag();
		}
		h.avatar.setImageResource(p.joueur.getAvatar());
		h.nom.setText(p.joueur.getPseudo() + String.format(" ( %,.2f )", p.joueur.getScore()));
		h.cumul_score.setText(String.format("%,.2f", partEffectives>1 && etape<3 ? p.cumul_score-p.score : p.cumul_score));
		if(p.t_fini<Participation.FORFAIT) {
			h.temps.setText(Jeu.getFormattedTime(etape<2 ? Math.min(p.t_fini, (int) (t_max*prog*prog)) : p.t_fini));
		} else {
			int strRes = (p.t_fini==Participation.FORFAIT) ? R.string.forfait : R.string.not_played;
			h.temps.setText(getContext().getString(strRes)+" !");
		}
		if(p.t_fini==Participation.NOT_PLAYED)
			h.score.setText("");
		else if(etape>=2)
			h.score.setText((p.score <= 0 ? "" : "+")+String.format("%,.2f", p.score));
		if(etape==3)
			h.cumul_score.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.aleat_opt_anim));
		if(etape>=4) {
			if(p.t_fini==Participation.NOT_PLAYED)
				h.nom.setTextColor(getContext().getResources().getColor(R.color.theme_gris));
			else if(p.score>=0)
				h.nom.setTextColor(getContext().getResources().getColor(R.color.vert_fonce));
			else
				h.nom.setTextColor(getContext().getResources().getColor(R.color.red));
			if(p.t_fini!=Participation.NOT_PLAYED) {
				h.rang.setText(""+p.rank);
				h.rang.setVisibility(View.VISIBLE);
			} else
				h.rang.setVisibility(View.INVISIBLE);
		}
		return convertView;
	}
	
	static class ViewHolder {
		ImageView avatar;
		TextView rang, nom, temps, score, cumul_score;
	}
}
