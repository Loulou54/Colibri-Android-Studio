package com.game.colibri;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Classe gérant la boîte de dialogue de définition des paramètres de génération des niveaux aléatoires.
 * @author Louis
 *
 */
public class ParamAleat {
	
	/**
	 *  Paramètres : {lon, nVaches, nDyna, nChats, nArcs, base} avec nX dans [-6,6] (négatif==uncheck), lon la longueur de la solution dans [5,30], base dans [0,1] définissant l'usage ou non de BaseNiveau.
	 */
	public static int[] param = new int[6];
	public final static int[] paramDefaut = new int[] {12, 3, 3, 2, 2, 1};
	public final static int LEVEL_UNLOCK_COWS = 8, LEVEL_UNLOCK_DYNA = 15, LEVEL_UNLOCK_CATS = 20, LEVEL_UNLOCK_RAINBOWS = 22;
	
	private PaperDialog boxAleat = null;
	private Context context;
	private callBackInterface callback;
	private int avancement;
	private TempParam tp;
	
	public static void loadParams(SharedPreferences pref) {
		for(int i=0; i<param.length; i++) {
			param[i] = pref.getInt("paramAleat"+i, paramDefaut[i]);
		}
	}
	
	public static void saveParams(SharedPreferences.Editor editor) {
		for(int i=0; i<param.length; i++) {
			editor.putInt("paramAleat"+i, param[i]);
		}
		editor.commit();
	}
	
	@SuppressLint("InlinedApi")
	public ParamAleat(callBackInterface callback, Context context, int avancement) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			this.context = new ContextThemeWrapper(context, android.R.style.Theme_Holo_Light_Dialog);
		} else {
			this.context = context;
		}
		this.callback = callback;
		this.avancement = avancement-1;
	}
	
	public interface callBackInterface {
		void launchFunction(int mode);
	}
	
	private void prepareParam(final LinearLayout parent, ParamElement[] sliders) {
		final TextView tv = (TextView) parent.findViewById(R.id.long_text);
		final SeekBar sb = (SeekBar) parent.findViewById(R.id.long_seekbar);
		final CheckBox cb = (CheckBox) parent.findViewById(R.id.base_checkbox);
		sb.setProgress(tp.p[0]-5);
		cb.setChecked(tp.p[5]==1);
		tv.setText(context.getString(R.string.longueur)+" "+(tp.p[0]+tp.p[0]/4)+" ± "+(tp.p[0]/4));
		tp.updateExp(parent);
		for(ParamElement pe : sliders) {
			pe.setValueFromParams();
		}
		sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				tv.setText(context.getString(R.string.longueur)+" "+(5*(progress+5)/4)+" ± "+((progress+5)/4));
				tp.p[0] = progress+5;
				tp.updateExp(parent);
			}
		});
		cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				tp.p[5] = isChecked ? 1 : 0;
			}
		});
	}
	
	public void show() {
		tp = new TempParam();
		boxAleat = new PaperDialog(context, R.layout.param_aleat);
		final LinearLayout lay = (LinearLayout) boxAleat.getContentView();
		final ParamElement sliders[] = {
			new ParamElement(1,lay,LEVEL_UNLOCK_COWS,R.drawable.vache_0,R.string.vaches),
			new ParamElement(2,lay,LEVEL_UNLOCK_DYNA,R.drawable.dynamite,R.string.dynamites),
			new ParamElement(3,lay,LEVEL_UNLOCK_CATS,R.drawable.chat_0,R.string.chats),
			new ParamElement(4,lay,LEVEL_UNLOCK_RAINBOWS,R.drawable.rainbow,R.string.arcs)
		};
		prepareParam(lay, sliders);
		boxAleat.setTitle(R.string.paramAleat);
		boxAleat.setPositiveButton(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				tp.commit();
				saveParams(MyApp.getApp().editor);
				callback.launchFunction(Niveau.PERSO);
				boxAleat.dismiss();
			}
		}, null);
		boxAleat.setNeutralButton(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				tp.p = paramDefaut.clone();
				prepareParam(lay, sliders);
			}
		}, null);
		boxAleat.setNegativeButton(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				boxAleat.dismiss();
			}
		}, null);
		boxAleat.show();
	}
	
	/**
	 * Contient la vue et les informations d'une ligne de paramètres.
	 * @author Louis
	 *
	 */
	private class ParamElement {
		
		private LinearLayout elem;
		private LinearLayout parent;
		private int paramIndex;
		private int seuil;

		/**
		 * Constructeur d'un élément de réglage pour les paramètres des niveaux aléatoires.
		 * @param index L'index de l'élément de réglage
		 * @param parent Le Layout parent.
		 * @param seuil Le seuil de progression dans Campagne nécessaire pour débloquer l'option.
		 * @param resDrawable La ressource de l'image à dessiner.
		 * @param resString La ressource du texte à afficher.
		 */
		@SuppressLint("InflateParams")
		public ParamElement(int index, final LinearLayout parent, int seuil, int resDrawable, int resString) {
			paramIndex = index;
			this.parent = parent;
			this.seuil = seuil;
			elem =(LinearLayout) LayoutInflater.from(context).inflate(R.layout.param_element, null);
			((ImageView) elem.findViewById(R.id.imageParam)).setImageResource(resDrawable);
			((TextView) elem.findViewById(R.id.textParam)).setText(resString);
			setValueFromParams();
			parent.addView(elem, index);
		}
		
		public void setValueFromParams() {
			final int i = paramIndex;
			if(avancement<seuil) { // L'option n'est pas encore débloquée
				tp.p[i] = -Math.abs(tp.p[i]);
				setEnabled(false);
				((SeekBar) elem.findViewById(R.id.seekBarParam)).setVisibility(View.GONE);
				TextView tb = (TextView) elem.findViewById(R.id.blockParam);
				tb.setVisibility(View.VISIBLE);
				tb.setText(context.getString(R.string.progression, avancement, seuil));
			} else {
				CheckBox cb = (CheckBox) elem.findViewById(R.id.checkBoxParam);
				SeekBar sb = (SeekBar) elem.findViewById(R.id.seekBarParam);
				cb.setChecked(tp.p[i]>0);
				sb.setEnabled(tp.p[i]>0);
				sb.setProgress(Math.abs(tp.p[i])-1);
				cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						tp.p[i] = (isChecked ? 1 : -1)*Math.abs(tp.p[i]);
						((SeekBar) elem.findViewById(R.id.seekBarParam)).setEnabled(isChecked);
						tp.updateExp(parent);
					}
				});
				sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {}
					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {}
					@Override
					public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
						tp.p[i] = progress+1;
						tp.updateExp(parent);
					}
				});
			}
		}
		
		public void setEnabled(boolean e) {
			for(int i = 0; i < elem.getChildCount(); i++) {
				elem.getChildAt(i).setEnabled(e);
			}
		}
	}
	
	private class TempParam {
		
		public int[] p;
		
		public TempParam() {
			p = param.clone();
		}
		
		public void updateExp(LinearLayout parent) {
			int min = p[0]*(10+p[0]/4);
			int lmax = 3*p[0]/2;
			int max = lmax*(10+lmax/4)+Math.max(p[1],0)*20+Math.max(p[2],0)*15+Math.max(p[3],0)*40+Math.max(p[4],0)*30;
			((TextView) parent.findViewById(R.id.param_exp)).setText(context.getString(R.string.exp)+" : "+min+" - "+max);
		}
		
		public void commit() {
			param = p.clone();
		}
		
	}
}
