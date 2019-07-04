package com.game.colibri;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.TextHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import cz.msebera.android.httpclient.Header;

import static com.network.colibri.CommonUtilities.APP_TOKEN;
import static com.network.colibri.CommonUtilities.SERVER_URL;

public class RegisterUser {
	
	private Context context;
	private ProgressDialog prgDialog;
	private callBackInterface callback;
	private AsyncHttpClient client;
	private int avatar = -1;
	private boolean register;
	private String firebaseId;
	
	@SuppressLint("InlinedApi")
	public RegisterUser(Context context, AsyncHttpClient client, callBackInterface callback) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			this.context = new ContextThemeWrapper(context, android.R.style.Theme_Holo_Light_Dialog);
		} else {
			this.context = context;
		}
		this.client = client;
		this.callback= callback;
		prgDialog = new ProgressDialog(this.context);
		prgDialog.setMessage(context.getString(R.string.progress));
		prgDialog.setCancelable(false);
		register = true;
	}
	
	/**
	 * Affiche la boîte de dialogue d'inscription / connexion.
	 */
	public void show() {
		final PaperDialog boxRegister = new PaperDialog(context, R.layout.register_layout);
		boxRegister.setTitle(R.string.multi);
		final LinearLayout lay = (LinearLayout) boxRegister.getContentView();
		if(callback.getExp()!=0) {
			TextView expTV = (TextView) lay.findViewById(R.id.expToSyncMsg);
			expTV.setText(context.getString(R.string.expToSyncMsg, String.format("%,d", callback.getExp())));
			expTV.setVisibility(View.VISIBLE);
		}
		final View reg = lay.findViewById(R.id.sw_reg), con = lay.findViewById(R.id.sw_con);
		reg.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				register = true;
				reg.setClickable(false);
				reg.setBackgroundColor(context.getResources().getColor(R.color.theme_gris_alpha));
				con.setClickable(true);
				con.setBackgroundColor(context.getResources().getColor(R.color.theme_vert));
				((EditText) lay.findViewById(R.id.pseudo)).setHint(R.string.name);
				lay.findViewById(R.id.lostPassword).setVisibility(View.GONE);
				View layAv = lay.findViewById(R.id.pickAvatar);
				layAv.setVisibility(View.VISIBLE);
				layAv.startAnimation(AnimationUtils.loadAnimation(context, R.anim.aleat_opt_anim));
			}
		});
		con.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				register = false;
				con.setClickable(false);
				con.setBackgroundColor(context.getResources().getColor(R.color.theme_gris_alpha));
				reg.setClickable(true);
				reg.setBackgroundColor(context.getResources().getColor(R.color.theme_vert));
				((EditText) lay.findViewById(R.id.pseudo)).setHint(R.string.name_or_mail);
				lay.findViewById(R.id.pickAvatar).setVisibility(View.GONE);
				lay.findViewById(R.id.lostPassword).setVisibility(View.VISIBLE);
			}
		});
		lay.findViewById(R.id.lost_chkbox).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				lay.findViewById(R.id.lost_exp).setVisibility(((CheckBox) v).isChecked() ? View.VISIBLE : View.GONE);
			}
		});
		if(!register) {
			con.setClickable(false);
			con.setBackgroundColor(context.getResources().getColor(R.color.theme_gris_alpha));
			reg.setClickable(true);
			reg.setBackgroundColor(context.getResources().getColor(R.color.theme_vert));
			lay.findViewById(R.id.pickAvatar).setVisibility(View.GONE);
			lay.findViewById(R.id.lostPassword).setVisibility(View.VISIBLE);
		} else
			reg.setClickable(false);
		final LinearLayout imagePicker = (LinearLayout) lay.findViewById(R.id.imagePicker);
		final ImageView avatarReg = (ImageView) lay.findViewById(R.id.avatarReg);
		avatarReg.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				avatarReg.setVisibility(View.GONE);
				imagePicker.setVisibility(View.VISIBLE);
			}
		});
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, context.getResources().getDisplayMetrics()), LinearLayout.LayoutParams.MATCH_PARENT);
		OnClickListener click = new OnClickListener() {
			@Override
			public void onClick(View v) {
				imagePicker.setVisibility(View.GONE);
				avatar = v.getId();
				avatarReg.setImageResource(Joueur.img[avatar]);
				avatarReg.setVisibility(View.VISIBLE);
				avatarReg.startAnimation(AnimationUtils.loadAnimation(context, R.anim.aleat_opt_anim));
			}
		};
		for(int i=0; i<Joueur.img.length; i++) {
			ImageView iv= new ImageView(context);
			iv.setLayoutParams(params);
			iv.setId(i);
			iv.setImageResource(Joueur.img[i]);
			iv.setOnClickListener(click);
			imagePicker.addView(iv);
		}
		boxRegister.setCancelable(false);
		boxRegister.setPositiveButton(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String name = ((EditText) lay.findViewById(R.id.pseudo)).getText().toString().trim();
				String mdp = ((EditText) lay.findViewById(R.id.mdp)).getText().toString();
				String mail = ((EditText) lay.findViewById(R.id.mail)).getText().toString();
				boolean lostMdp = ((CheckBox) lay.findViewById(R.id.lost_chkbox)).isChecked();
				if(name.length()<3 || name.length()>20 && register) {
					Toast.makeText(context, R.string.nom_invalide, Toast.LENGTH_LONG).show();
				} else if(!register && lostMdp) {
					lostMdp(name, boxRegister);
				} else if(mdp.length()<6) {
					Toast.makeText(context, R.string.mdp_invalide, Toast.LENGTH_LONG).show();
				} else if(register && !android.util.Patterns.EMAIL_ADDRESS.matcher(mail).matches()) {
					Toast.makeText(context, R.string.mail_invalide, Toast.LENGTH_LONG).show();
				} else if(register && avatar < 0) {
					Toast.makeText(context, R.string.pick_avatar, Toast.LENGTH_LONG).show();
				} else if(register) {
					registerUser(name, mdp, mail, boxRegister);
				} else {
					connectUser(name, mdp, boxRegister);
				}
			}
		}, null);
		boxRegister.setNegativeButton(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				boxRegister.dismiss();
				callback.cancelled();
			}
		}, null);
		// Récupère l'id firebase avant d'ouvrir la box
		FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
			@Override
			public void onComplete(@NonNull Task<InstanceIdResult> task) {
				if(!task.isSuccessful()) {
					System.out.println("getInstanceId failed"+task.getException());
					callback.cancelled();
					return;
				}
				firebaseId = task.getResult().getToken();
				boxRegister.show();
			}
		});
	}
	
	public interface callBackInterface {
		int getExp();
		int getProgress();
		int getColiBrains();
		int getExpProgCB();
		boolean registered(String JSONresponse, String name, boolean sync);
		void cancelled();
	}
	
	private void registerUser(final String name, String mdp, String mail, final PaperDialog box) {
		RequestParams params = new RequestParams();
		params.setHttpEntityIsRepeatable(true);
		params.put("token", APP_TOKEN);
		params.put("pseudo", name);
		params.put("password", mdp);
		params.put("mail", mail);
		params.put("avatar", ""+avatar);
		params.put("exp", ""+callback.getExp());
		params.put("progress", ""+callback.getProgress());
		params.put("coliBrains", ""+callback.getColiBrains());
		params.put("expProgCB", ""+callback.getExpProgCB());
		params.put("regId", firebaseId);
		params.put("pays", Resources.getSystem().getConfiguration().locale.getCountry());
		prgDialog.show();
		client.post(SERVER_URL+"/register.php", params, new TextHttpResponseHandler() {
			@Override
			public void onSuccess(int statusCode, Header[] headers, String response) {
				prgDialog.dismiss();
				if(response.equalsIgnoreCase("pris")) { // Nom déjà pris
					Toast.makeText(context, R.string.deja_pris, Toast.LENGTH_LONG).show();
				} else if(response.equalsIgnoreCase("error")) { // Erreur
					Toast.makeText(context, R.string.errServ, Toast.LENGTH_LONG).show();
				} else { // Succès
					if(callback.registered(response, name, false)) {
						box.dismiss();
						Toast.makeText(context, R.string.enregistre, Toast.LENGTH_LONG).show();
					} else {
						Toast.makeText(context, R.string.errServ, Toast.LENGTH_LONG).show();
					}
				}
			}

			@Override
			public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
				prgDialog.dismiss();
				if (statusCode == 404) {
					Toast.makeText(context, R.string.err404, Toast.LENGTH_LONG).show();
				} else if (statusCode == 500 || statusCode == 503) {
					Toast.makeText(context, R.string.err500, Toast.LENGTH_LONG).show();
				} else {
					Toast.makeText(context, R.string.err, Toast.LENGTH_LONG).show();
				}
			}
		});
	}
	
	private void connectUser(final String pseudoOrMail, String mdp, final PaperDialog box) {
		RequestParams params = new RequestParams();
		params.setHttpEntityIsRepeatable(true);
		params.put("token", APP_TOKEN);
		params.put("pseudoOrMail", pseudoOrMail);
		params.put("password", mdp);
		params.put("regId", firebaseId);
		prgDialog.show();
		client.post(SERVER_URL+"/connect.php", params, new TextHttpResponseHandler() {
			@Override
			public void onSuccess(int statusCode, Header[] headers, String response) {
				prgDialog.dismiss();
				if(response.equalsIgnoreCase("not registered")) { // Le pseudo n'existe pas
					Toast.makeText(context, R.string.not_registered, Toast.LENGTH_LONG).show();
				} else if(response.equalsIgnoreCase("wrong password")) { // Mauvais mot de passe
					Toast.makeText(context, R.string.wrong_password, Toast.LENGTH_LONG).show();
				} else { // Succès
					if(callback.registered(response, pseudoOrMail, true)) {
						box.dismiss();
						Toast.makeText(context, R.string.connected, Toast.LENGTH_LONG).show();
					} else {
						Toast.makeText(context, R.string.errServ, Toast.LENGTH_LONG).show();
					}
				}
			}

			@Override
			public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
				prgDialog.dismiss();
				if (statusCode == 404) {
					Toast.makeText(context, R.string.err404, Toast.LENGTH_LONG).show();
				} else if (statusCode == 500 || statusCode == 503) {
					Toast.makeText(context, R.string.err500, Toast.LENGTH_LONG).show();
				} else {
					Toast.makeText(context, R.string.err, Toast.LENGTH_LONG).show();
				}
			}
		});
	}
	
	private void lostMdp(final String pseudoOrMail, final PaperDialog box) {
		RequestParams params = new RequestParams();
		params.setHttpEntityIsRepeatable(true);
		params.put("pseudoOrMail", pseudoOrMail);
		prgDialog.show();
		client.post(SERVER_URL+"/mail_mdp.php", params, new TextHttpResponseHandler() {
			@Override
			public void onSuccess(int statusCode, Header[] headers, String response) {
				prgDialog.dismiss();
				if(response.equalsIgnoreCase("not found")) { // Le pseudo n'existe pas
					Toast.makeText(context, R.string.not_registered, Toast.LENGTH_LONG).show();
				} else if(response.equalsIgnoreCase("already sent")) { // Un token de moins d'un jour est déjà attribué à cet utilisateur
					((CheckBox) box.findViewById(R.id.lost_chkbox)).setChecked(false);
					box.findViewById(R.id.lost_exp).setVisibility(View.GONE);
					Toast.makeText(context, R.string.lost_mdp_already_sent, Toast.LENGTH_LONG).show();
				} else if(response.equalsIgnoreCase("OK")) { // Succès
					((CheckBox) box.findViewById(R.id.lost_chkbox)).setChecked(false);
					box.findViewById(R.id.lost_exp).setVisibility(View.GONE);
					Toast.makeText(context, R.string.lost_mdp_ok, Toast.LENGTH_LONG).show();
				} else { // Problème d'envoi
					Toast.makeText(context, R.string.lost_mdp_pb, Toast.LENGTH_LONG).show();
				}
			}

			@Override
			public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
				prgDialog.dismiss();
				if (statusCode == 404) {
					Toast.makeText(context, R.string.err404, Toast.LENGTH_LONG).show();
				} else if (statusCode == 500 || statusCode == 503) {
					Toast.makeText(context, R.string.err500, Toast.LENGTH_LONG).show();
				} else {
					Toast.makeText(context, R.string.err, Toast.LENGTH_LONG).show();
				}
			}
		});
	}
	
}
