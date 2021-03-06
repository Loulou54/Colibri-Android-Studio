package com.game.colibri;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.TextHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
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
import static com.network.colibri.CommonUtilities.upgradeMessage;

public class RegisterUser {
	
	private Context context;
	private ProgressDialog prgDialog;
	private callBackInterface callback;
	private AsyncHttpClient client;
	private int avatar = -1;
	private boolean register;
	private String firebaseId;
	private PaperDialog boxConnectionChoice = null;

	public RegisterUser(Context context, AsyncHttpClient client, callBackInterface callback) {
		this.context = new ContextThemeWrapper(context, android.R.style.Theme_Holo_Light_Dialog);
		this.client = client;
		this.callback= callback;
		prgDialog = new ProgressDialog(this.context);
		prgDialog.setMessage(context.getString(R.string.progress));
		prgDialog.setCancelable(false);
		register = true;
	}

	/**
	 * Commence la procédure d'inscription ou connexion.
	 */
	public void start() {
		prgDialog.show();
		// Récupère l'id firebase avant d'ouvrir la boîte de dialogue
		FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
			@Override
			public void onComplete( Task<InstanceIdResult> task) {
				prgDialog.dismiss();
				if(!task.isSuccessful()) {
					if(task.getException().getMessage()=="MISSING_INSTANCEID_SERVICE") { // Firebase isn't available
						firebaseId = "";
					} else {
						Toast.makeText(context, R.string.connexion_register, Toast.LENGTH_LONG).show();
						System.out.println("getInstanceId failed: "+task.getException().getMessage());
						callback.cancelled();
						return;
					}
				} else {
					firebaseId = task.getResult().getToken();
				}
				showConnectionChoice();
			}
		});
	}

	/**
	 * Affiche le choix de connexion par Google ou standard
	 */
	private void showConnectionChoice() {
		boxConnectionChoice = new PaperDialog(context, R.layout.multi_connection_choice);
		boxConnectionChoice.setTitle(R.string.multi);
		final LinearLayout lay = (LinearLayout) boxConnectionChoice.getContentView();
		final TextView signInButton = lay.findViewById(R.id.google_sign_in_button);
		final TextView standardConnectionButton = lay.findViewById(R.id.standard_connection_button);
		signInButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				callback.callGoogleSignIn();
			}
		});
		standardConnectionButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				boxConnectionChoice.dismiss();
				boxConnectionChoice = null;
				showForm(null);
			}
		});
		signInButton.setTypeface(boxConnectionChoice.getFont());
		standardConnectionButton.setTypeface(boxConnectionChoice.getFont());
		boxConnectionChoice.setCancelable(false);
		boxConnectionChoice.setNegativeButton(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				boxConnectionChoice.dismiss();
				boxConnectionChoice = null;
				callback.cancelled();
			}
		}, null);
		boxConnectionChoice.show();
	}

	/**
	 * L'utilisateur s'est connecté à un compte Google (ou une erreur s'est produite)
	 * @param task La tâche est forcément complétée
	 */
	public void googleSignInResult(Task<GoogleSignInAccount> task) {
		try {
			GoogleSignInAccount account = task.getResult(ApiException.class);
			connectUser("", "", boxConnectionChoice, account);
		} catch(ApiException e) {
			Toast.makeText(context,
					context.getString(R.string.google_sign_in_error, "\n"+GoogleSignInStatusCodes.getStatusCodeString(e.getStatusCode())),
					Toast.LENGTH_LONG).show();
		} catch (Exception e) {
			Toast.makeText(context,
					context.getString(R.string.google_sign_in_error, ""),
					Toast.LENGTH_LONG).show();
		}
	}
	
	/**
	 * Affiche la boîte de dialogue d'inscription / connexion.
	 */
	private void showForm(final GoogleSignInAccount account) {
		final PaperDialog boxRegister = new PaperDialog(context, R.layout.register_layout);
		boxRegister.setTitle(R.string.multi);
		final LinearLayout lay = (LinearLayout) boxRegister.getContentView();
		if(MyApp.expToSync!=0) {
			TextView expTV = (TextView) lay.findViewById(R.id.expToSyncMsg);
			expTV.setText(context.getString(R.string.expToSyncMsg, String.format("%,d", MyApp.expToSync)));
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
		if(account!=null) { // Google Sign In
			((TextView) lay.findViewById(R.id.enter_pseudo_instru)).setText(R.string.enter_pseudo);
			((EditText) lay.findViewById(R.id.pseudo)).setText(account.getDisplayName());
			lay.findViewById(R.id.sw_reg_con_layout).setVisibility(View.GONE);
			lay.findViewById(R.id.mdp).setVisibility(View.GONE);
			lay.findViewById(R.id.mail).setVisibility(View.GONE);
			lay.findViewById(R.id.mail_expl).setVisibility(View.GONE);
		}
		boxRegister.setCancelable(false);
		boxRegister.setPositiveButton(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String name = ((EditText) lay.findViewById(R.id.pseudo)).getText().toString().trim();
				String mdp = ((EditText) lay.findViewById(R.id.mdp)).getText().toString();
				String mail = ((EditText) lay.findViewById(R.id.mail)).getText().toString().trim();
				boolean lostMdp = ((CheckBox) lay.findViewById(R.id.lost_chkbox)).isChecked();
				if(name.length()<3 || name.length()>20 && register) {
					Toast.makeText(context, R.string.nom_invalide, Toast.LENGTH_LONG).show();
				} else if(!register && lostMdp) {
					lostMdp(name, boxRegister);
				} else if(mdp.length()<6 && account==null) {
					Toast.makeText(context, R.string.mdp_invalide, Toast.LENGTH_LONG).show();
				} else if(register && !android.util.Patterns.EMAIL_ADDRESS.matcher(mail).matches() && account==null) {
					Toast.makeText(context, R.string.mail_invalide, Toast.LENGTH_LONG).show();
				} else if(register && avatar < 0) {
					Toast.makeText(context, R.string.pick_avatar, Toast.LENGTH_LONG).show();
				} else if(register) {
					registerUser(name, mdp, mail, boxRegister, account);
				} else {
					connectUser(name, mdp, boxRegister, account);
				}
			}
		}, null);
		boxRegister.setNegativeButton(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(account!=null) {
					callback.callGoogleSignOut();
				}
				boxRegister.dismiss();
				showConnectionChoice();
			}
		}, null);
		boxRegister.show();
	}
	
	public interface callBackInterface {
		boolean registered(String JSONresponse);
		void cancelled();
		void callGoogleSignIn();
		void callGoogleSignOut();
	}
	
	private void registerUser(String name, String mdp, String mail, final PaperDialog box, GoogleSignInAccount account) {
		RequestParams params = new RequestParams();
		params.setHttpEntityIsRepeatable(true);
		params.put("token", APP_TOKEN);
		params.put("pseudo", name);
		if(account!=null) {
			params.put("tokenId", account.getIdToken());
		} else {
			params.put("password", mdp);
			params.put("mail", mail);
		}
		params.put("avatar", ""+avatar);
		params.put("regId", firebaseId);
		params.put("pays", Resources.getSystem().getConfiguration().locale.getCountry());
		prgDialog.show();
		client.post(SERVER_URL+"/register.php", params, new TextHttpResponseHandler() {
			@Override
			public void onSuccess(int statusCode, Header[] headers, String response) {
				prgDialog.dismiss();
				if(response.equalsIgnoreCase("upgrade")) {
					upgradeMessage(context);
				} else if(response.equalsIgnoreCase("pris")) { // Nom déjà pris
					Toast.makeText(context, R.string.deja_pris, Toast.LENGTH_LONG).show();
				} else if(response.equalsIgnoreCase("google authentication failed")) { // Erreur d'authentification par le tokenId
					Toast.makeText(context,
							context.getString(R.string.google_sign_in_error, "TokenId error"),
							Toast.LENGTH_LONG).show();
				} else { // Succès
					if(callback.registered(response)) {
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

	private void connectUser(String pseudoOrMail, String mdp, final PaperDialog box, final GoogleSignInAccount account) {
		RequestParams params = new RequestParams();
		params.setHttpEntityIsRepeatable(true);
		params.put("token", APP_TOKEN);
		if(account!=null) {
			params.put("tokenId", account.getIdToken());
		} else {
			params.put("pseudoOrMail", pseudoOrMail);
			params.put("password", mdp);
		}
		params.put("regId", firebaseId);
		prgDialog.show();
		client.post(SERVER_URL+"/connect.php", params, new TextHttpResponseHandler() {
			@Override
			public void onSuccess(int statusCode, Header[] headers, String response) {
				prgDialog.dismiss();
				if(response.equalsIgnoreCase("upgrade")) {
					upgradeMessage(context);
				} else if(response.equalsIgnoreCase("not registered")) { // Le pseudo n'existe pas
					Toast.makeText(context, R.string.not_registered, Toast.LENGTH_LONG).show();
				} else if(response.equalsIgnoreCase("wrong password")) { // Mauvais mot de passe
					Toast.makeText(context, R.string.wrong_password, Toast.LENGTH_LONG).show();
				} else if(response.equalsIgnoreCase("google not registered")) { // Non inscrit avec google ou gmail
					box.dismiss();
					showForm(account);
				} else if(response.equalsIgnoreCase("google authentication failed")) { // Erreur d'authentification par le tokenId
					Toast.makeText(context,
							context.getString(R.string.google_sign_in_error, "TokenId error"),
							Toast.LENGTH_LONG).show();
				} else { // Succès
					if(callback.registered(response)) {
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
