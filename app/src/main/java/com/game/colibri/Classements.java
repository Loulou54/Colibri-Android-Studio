package com.game.colibri;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Locale;

import com.loopj.android.http.RequestHandle;
import com.network.colibri.ConnectionDetector;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.ViewFlipper;

public class Classements extends Activity {
	
	private static final int AUTOCOMPLETE_DELAY = 800;
	
	private ViewFlipper vf;
	private int classementIndex = 0;
	private boolean filterFriends = false;
	private String search = "";
	private final InputHandler inputHandler = new InputHandler(this);
	private RequestHandle rh = null;
	public int nJoueurs = 0;
	public Joueur[] userRanks;
	
	private static class InputHandler extends Handler {
		
		private final WeakReference<Classements> act;
		
		public InputHandler(Classements a) {
			act = new WeakReference<Classements>(a);
		}
		
        @Override
        public void handleMessage(Message msg) {
        	Classements c = act.get();
			if(c!=null) {
				c.search = (String) msg.obj;
				c.refresh();
			}
        }
    };
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_classements);
		if(!(new ConnectionDetector(this)).isConnectedToInternet()) {
			Toast.makeText(this, R.string.connexion_ranking, Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		vf = (ViewFlipper) findViewById(R.id.rankingFlipper);
		EditText et = (EditText) findViewById(R.id.searchClassement);
		et.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				inputHandler.removeMessages(0);
		        inputHandler.sendMessageDelayed(inputHandler.obtainMessage(0, s.toString().trim()), AUTOCOMPLETE_DELAY);
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override
			public void afterTextChanged(Editable s) {}
		});
		((TextView) findViewById(R.id.nomAdv)).setTypeface(Typeface.createFromAsset(getAssets(),"fonts/Passing Notes.ttf"));
		userRanks = new Joueur[vf.getChildCount()];
		dispClassement(0);
	}
	
	@Override
	protected void onStart() {
		MyApp.resumeActivity();
		super.onStart();
	}
	
	@Override
	protected void onStop() {
		MyApp.stopActivity();
		super.onStop();
	}
	
	private void refresh() {
		if(rh!=null)
        	rh.cancel(true);
		ListView lv = (ListView) vf.getChildAt(vf.getDisplayedChild());
		rh = ((ClassementAdapter) lv.getAdapter()).refresh(filterFriends, search);
	}
	
	private TextView selectTitleColumn(int cl) {
		return (TextView) (
				cl==0 ? findViewById(R.id.expTitleRanking)
				: cl==1 ? findViewById(R.id.scoreTitleRanking)
				: cl==2 ? findViewById(R.id.defisTitleRanking)
				: cl==3 ? findViewById(R.id.winLostTitleRanking)
				: null
		);
	}
	
	private void dispClassement(int cl) {
		TextView selected = selectTitleColumn(classementIndex);
		if(selected!=null)
			selected.setTypeface(null, Typeface.NORMAL);
		classementIndex = cl;
		selected = selectTitleColumn(cl);
		if(selected!=null)
			selected.setTypeface(selected.getTypeface(), Typeface.BOLD);
		int nCl = vf.getChildCount();
		int dpINpx = (int)Math.ceil(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics()));
		LinearLayout tabs = (LinearLayout) findViewById(R.id.tabs);
		LinearLayout.LayoutParams lp;
		for(int i=0; i<nCl; i++) {
			View v = tabs.getChildAt(i);
			lp = (LinearLayout.LayoutParams) v.getLayoutParams();
			lp.setMargins(0, 0, dpINpx, i==cl ? 0 : 2*dpINpx);
			v.setLayoutParams(lp);
			v.setBackgroundResource(i==cl ? R.color.theme_beige : R.color.theme_vert);
		}
		final ListView lv = (ListView) vf.getChildAt(cl);
		if(lv.getAdapter()==null) {
			lv.setAdapter(new ClassementAdapter(this, cl, filterFriends, search));
			lv.setOnScrollListener(new OnScrollListener() {
				@Override
				public void onScrollStateChanged(AbsListView view, int scrollState) {}
				@Override
				public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
					if(firstVisibleItem+visibleItemCount == totalItemCount && (rh==null || rh.isFinished() || rh.isCancelled()))
						rh = ((ClassementAdapter) lv.getAdapter()).loadNext();
				}
			});
			lv.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
					Joueur j = (Joueur) arg0.getAdapter().getItem(arg2);
					if(j!=null)
						(new DispJoueur(Classements.this, j)).show();
				}
			});
		} else {
			rh = ((ClassementAdapter) lv.getAdapter()).refresh(filterFriends, search);
			if(rh==null)
				setUserInfos(cl);
		}
		vf.setDisplayedChild(cl);
	}
	
	public void dispClassement(View v) {
		dispClassement(Integer.parseInt((String) v.getTag()));
	}
	
	public void filterFriends(View v) {
		filterFriends = ((ToggleButton) v).isChecked();
		refresh();
	}
	
	public void search(View v) {
		boolean checked = ((ToggleButton) v).isChecked();
		EditText et = (EditText) findViewById(R.id.searchClassement);
		et.setVisibility(checked ? View.VISIBLE : View .INVISIBLE);
		if(!checked)
			search = "";
		else {
			search = et.getText().toString().trim();
			et.requestFocus();
		}
		refresh();
	}
	
	public void setUserInfos() {
		setUserInfos(vf.getDisplayedChild());
	}
	
	private void setUserInfos(int cl) {
		((TextView) findViewById(R.id.nJoueurs)).setText("/"+nJoueurs);
		Joueur j = userRanks[cl];
		if(j==null)
			return;
		LinearLayout lay = (LinearLayout) findViewById(R.id.userRank);
		lay.findViewById(R.id.classAdv);
		((TextView) lay.findViewById(R.id.classAdv)).setText(""+j.getRang());
		((ImageView) lay.findViewById(R.id.avatarAdv)).setImageResource(j.getAvatar());
		((TextView) lay.findViewById(R.id.nomAdv)).setText(j.getPseudo());
		ImageView pays = ((ImageView) lay.findViewById(R.id.paysAdv));
		try {
			InputStream file = this.getAssets().open("drapeaux/"+j.getPays().toLowerCase(Locale.FRANCE)+".png");
			pays.setImageBitmap(BitmapFactory.decodeStream(file));
			pays.setVisibility(View.VISIBLE);
		} catch (IOException e) {
			pays.setVisibility(View.INVISIBLE);
		}
		((TextView) lay.findViewById(R.id.expAdv)).setText(String.format("%,d", j.getExp()));
		((TextView) lay.findViewById(R.id.scoreAdv)).setText(String.format("%,.2f", j.getScore()));
		((TextView) lay.findViewById(R.id.defisAdv)).setText(""+j.getDefis());
		((TextView) lay.findViewById(R.id.winDefisAdv)).setText(""+j.getWin());
		((TextView) lay.findViewById(R.id.winLostAdv)).setText(""+(j.getWin() - j.getLoose()));
	}
}
