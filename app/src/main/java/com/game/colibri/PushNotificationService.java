package com.game.colibri;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.network.colibri.DBController;

import org.json.JSONException;
import org.json.JSONObject;

import static com.network.colibri.CommonUtilities.broadcastMessage;

public class PushNotificationService extends FirebaseMessagingService {

	private static String CHANNEL_NEW_MATCH = "NEW_MATCH";
	private static String CHANNEL_RESULTS = "RESULTS";
	private static String CHANNEL_MESSAGE = "MESSAGE";

	@Override
	public void onMessageReceived(RemoteMessage remoteMessage) {
		super.onMessageReceived(remoteMessage);

		// Data message
		if(remoteMessage.getData().size() > 0) {
			System.out.println(remoteMessage.getData());
			String msg = remoteMessage.getData().get("msg");
			broadcastMessage(this, msg);
			if(!Multijoueur.active)
				generateNotification(this, msg);
		}

		RemoteMessage.Notification notif = remoteMessage.getNotification();
		// Notification message
		if(notif != null) {
			System.out.println(notif.getTitle()+" : "+notif.getBody());
		}
	}

	/**
	 * Called if InstanceID token is updated. This may occur if the security of
	 * the previous token had been compromised. Note that this is called when the InstanceID token
	 * is initially generated so this is where you would retrieve the token.
	 */
	@Override
	public void onNewToken(String token) {
		System.out.println("New token: " + token);
		(new DBController(this)).clearDB();
		MyApp.id = 0;
		MyApp.getApp().editor.remove("id");
		MyApp.getApp().editor.commit();
	}

	/**
	 * Déclaration des notification channels, au lancement de l'application.
	 * @param context le contexte de l'appli
	 */
	public static void createNorificationsChannels(Context context) {
		createNotificationChannel(context, CHANNEL_NEW_MATCH, R.string.channel_new_match, R.string.channel_new_match_desc);
		createNotificationChannel(context, CHANNEL_RESULTS, R.string.channel_results, R.string.channel_results_desc);
		createNotificationChannel(context, CHANNEL_MESSAGE, R.string.channel_message, R.string.channel_message_desc);
	}

	/**
	 * Pour les API >= 26, il faut déclarer des notification channels
	 */
	private static void createNotificationChannel(Context context, String channelId, int nameResId, int descResId) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			CharSequence name = context.getString(nameResId);
			String description = context.getString(descResId);
			int importance = NotificationManager.IMPORTANCE_DEFAULT;
			NotificationChannel channel = new NotificationChannel(channelId, name, importance);
			channel.setDescription(description);
			NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
			notificationManager.createNotificationChannel(channel);
		}
	}

	/**
	 * Issues a notification to inform the user that server has sent a message.
	 */
	private static void generateNotification(Context context, String message) {
		// Détermination du contenu de la notification
		int icon = R.drawable.ic_launcher;
		int id = 3 + (int) (Math.random()*(Integer.MAX_VALUE-3));
		long when = System.currentTimeMillis();
		String title = context.getString(R.string.app_name), msg=message, channel=CHANNEL_MESSAGE;
		SharedPreferences pref = MyApp.getApp().pref;
		SharedPreferences.Editor editor = MyApp.getApp().editor;
		int nNewM = pref.getInt("nNewM", 0), nRes = pref.getInt("nRes", 0);
		long lastNotif = pref.getLong("lastNotif", 0);
		try {
			JSONObject o = new JSONObject(message);
			String typ = o.getString("type");
			if(typ.equals("newMatch")) {
				id = 1;
				nNewM++;
				if(nNewM>1) {
					title = context.getString(R.string.nouveau_defi);
					msg = context.getString(R.string.notif_n_newmatch, nNewM);
				} else {
					title = o.getString("nomDefi");
					msg = context.getString(R.string.notif_newdefi, o.getString("initPlayer"));
				}
				channel = CHANNEL_NEW_MATCH;
			} else if(typ.equals("results")) {
				id = 2;
				nRes++;
				if(nRes>1) {
					title = context.getString(R.string.etat_resultats);
					msg = context.getString(R.string.notif_n_results, nRes);
				} else {
					title = o.getString("nomDefi");
					if(o.has("initPlayer"))
						msg = context.getString(R.string.notif_results, o.getString("initPlayer"));
					else
						msg = context.getString(R.string.notif_results_exp);
				}
				channel = CHANNEL_RESULTS;
			} else if(typ.equals("message")) {
				if(o.has("title"))
					title = o.getString("title");
				msg = o.getString("message");
			}
			editor.putInt("nNewM", nNewM)
					.putInt("nRes", nRes)
					.putLong("lastNotif", when)
					.commit();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		// Génération de la notification
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		Intent notificationIntent = new Intent(context, MenuPrinc.class);
		// On ajoute les infos dans l'Intent :
		notificationIntent.putExtra("com.game.colibri.notification", message);
		// set intent so it does not start a new activity
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent intent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
		// The notification :
		Notification notification = new NotificationCompat.Builder(context, channel)
				.setContentIntent(intent)
				.setSmallIcon(icon)
				.setContentTitle(title)
				.setContentText(msg)
				.setTicker(msg)
				.setWhen(when)
				.setAutoCancel(true)
				.build();
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		// Play default notification sound
		//notification.defaults |= Notification.DEFAULT_SOUND;
		// Vibrate if vibrate is enabled
		if(when - lastNotif > 5000) // Pour ne pas faire vibrer le téléphone à chaque notif lors de rafales !
			notification.defaults |= Notification.DEFAULT_VIBRATE;
		notificationManager.notify(id, notification);
		System.out.println("NOTIF : "+title+" "+msg);
	}
}
