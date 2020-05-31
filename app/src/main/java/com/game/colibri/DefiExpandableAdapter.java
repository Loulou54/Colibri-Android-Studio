package com.game.colibri;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class DefiExpandableAdapter extends BaseExpandableListAdapter {
	
	private Context context;
	private int user;
	private ArrayList<Defi> defiList;
	private boolean launchEnabled = true;
	private Typeface font1, font2;
	
	public DefiExpandableAdapter(Context c, int user, ArrayList<Defi> defis) {
		context = c;
		this.user = user;
		defiList = defis;
		font1 = Typeface.createFromAsset(c.getAssets(),"fonts/YummyCupcakes.ttf");
		font2 = Typeface.createFromAsset(c.getAssets(),"fonts/Passing Notes.ttf");
	}
	
	public void setLaunchEnabled(boolean b) {
		launchEnabled = b;
	}
	
	public boolean getLaunchEnabled() {
		return launchEnabled;
	}
	
	@Override
	public Object getChild(int groupPosition, int childPosition) {
		return defiList.get(groupPosition).participants.valueAt(childPosition);
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
		Participation p = (Participation) getChild(groupPosition, childPosition);
		ChildViewHolder h;
		if(convertView==null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.list_participation, parent, false);
			h = new ChildViewHolder();
			h.avatar = (ImageView) convertView.findViewById(R.id.avatar);
			h.nom = (TextView) convertView.findViewById(R.id.nomJoueur);
			h.nom.setTypeface(font1);
			h.nom.setSelected(true);
			h.nom.setHorizontallyScrolling(true);
			h.score = (TextView) convertView.findViewById(R.id.scoreJoueur);
			h.etat = (ImageView) convertView.findViewById(R.id.etatJoueur);
			h.exp = (TextView) convertView.findViewById(R.id.expJoueur);
			convertView.setTag(h);
		} else {
			h = (ChildViewHolder) convertView.getTag();
		}
		h.avatar.setImageResource(p.joueur.getAvatar());
		h.nom.setText(p.joueur.getPseudo() + String.format(" (%,.2f)", p.joueur.getScore()));
		h.score.setText(String.format("%,.2f", p.cumul_score));
		h.etat.setImageResource((p.t_cours==0) ? R.drawable.horloge : R.drawable.check);
		h.exp.setText(String.format("%,d", p.joueur.getExp()));
		return convertView;
	}
	
	static class ChildViewHolder {
		ImageView avatar;
		TextView nom;
		TextView score;
		ImageView etat;
		TextView exp;
	}
	
	@Override
	public int getChildrenCount(int groupPosition) {
		return defiList.get(groupPosition).participants.size();
	}

	@Override
	public Object getGroup(int groupPosition) {
		return defiList.get(groupPosition);
	}

	@Override
	public int getGroupCount() {
		return defiList.size();
	}

	@Override
	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
		Defi d = (Defi) getGroup(groupPosition);
		GroupViewHolder h;
		if(convertView==null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.list_defi, parent, false);
			h = new GroupViewHolder();
			h.nom = (TextView) convertView.findViewById(R.id.nomDefi);
			h.nom.setTypeface(font1);
			h.nom.setSelected(true);
			h.nom.setHorizontallyScrolling(true);
			h.nMatch = (TextView) convertView.findViewById(R.id.nMatch);
			h.etat = (Button) convertView.findViewById(R.id.etat);
			h.etat.setTypeface(font2);
			h.fermer = (Button) convertView.findViewById(R.id.fermer);
			convertView.setTag(h);
		} else {
			h = (GroupViewHolder) convertView.getTag();
		}
		h.nom.setText(d.nom);
		if(d.type!=0) {
			SpannableString nPart = new SpannableString(d.participants.size()+"/"+d.type);
			nPart.setSpan(new RelativeSizeSpan(0.75f), 0, nPart.length(), 0);
			nPart.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.choco)), 0, nPart.length(), 0);
			h.nMatch.setText(nPart);
		} else {
			h.nMatch.setText(""+d.nMatch);
		}
		SpannableString etat;
		int color=0;
		long t_restant = d.limite-System.currentTimeMillis()/1000;
		switch(d.getEtat(user)) {
		case Defi.ATTENTE:
			etat = new SpannableString(context.getString(R.string.etat_attente)+(d.t_max==0 ? "" : "\n"+getTimeLeft(t_restant)));
			color = R.color.bleu_moyen;
			break;
		case Defi.RESULTATS:
			etat = new SpannableString(context.getString(R.string.etat_resultats)+(d.t_max==0 || d.match==null ? "" : "\n"+getTimeLeft(t_restant)));
			color = R.color.violet;
			break;
		case Defi.RELEVER:
			etat = new SpannableString(context.getString(R.string.etat_relever)+(d.t_max==0 ? "" : "\n"+getTimeLeft(t_restant)));
			color = R.color.red;
			break;
		case Defi.LANCER:
			etat = new SpannableString(context.getString(R.string.etat_lancer));
			color = R.color.vert_fonce;
			break;
		case Defi.OBSOLETE:
			etat = new SpannableString(context.getString(R.string.etat_obsolete));
			color = R.color.choco;
			break;
		default:
			etat = new SpannableString("-");
		}
		String s = etat.toString();
		if(s.indexOf("\n")>0) {
			etat.setSpan(new RelativeSizeSpan(0.75f), s.indexOf("\n"), s.length(), 0);
			if(t_restant<12*3600)
				etat.setSpan(new ForegroundColorSpan(Color.RED), s.indexOf("\n"), s.length(), 0);
			else
				etat.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.vert_fonce)), s.indexOf("\n"), s.length(), 0);
		}
		h.etat.setText(etat);
		h.etat.setTextColor(context.getResources().getColor(color));
		h.etat.setTag(groupPosition);
		h.etat.setEnabled(launchEnabled);
		h.fermer.setTag(groupPosition);
		h.fermer.setVisibility(d.type==0 ? View.VISIBLE : View.INVISIBLE);
		return convertView;
	}
	
	private String getTimeLeft(long t) {
		if(t>=3600*24) {
			return context.getResources().getString(R.string.temps_restant, t/(3600*24), (t/3600)%24);
		} else if (t>0) {
			return context.getResources().getString(R.string.temps_restant2, t/3600, (t/60)%60);
		} else
			return "/!\\";
	}
	
	static class GroupViewHolder {
		TextView nom;
		TextView nMatch;
		Button etat;
		Button fermer;
	}

	@Override
	public boolean hasStableIds() {
		return false;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}

}
