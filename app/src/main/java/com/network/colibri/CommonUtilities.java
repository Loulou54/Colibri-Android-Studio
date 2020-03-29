package com.network.colibri;
import android.content.Context;
import android.content.Intent;

public final class CommonUtilities {

	// previous address: https://ssl17.ovh.net/~louisworix/
    // then: https://cluster017.hosting.ovh.net/~louisworix/
    public static final String SERVER_URL = "https://cluster017.hosting.ovh.net/~louisworix/colibri";
    
    // Communication token
    public static final String APP_TOKEN = ",!:éçè_zd453s;/+°hjzd";

    /**
     * Tag used on log messages.
     */
    public static final String BROADCAST_MESSAGE_ACTION =
            "com.game.colibri.BROADCAST_MESSAGE";
 
    public static final String EXTRA_MESSAGE = "message";
 
    /**
     * Envoie un message au BroadcastReceiver de Multijoueur
     *
     * @param context application's context.
     * @param message message to be broadcasted.
     */
    public static void broadcastMessage(Context context, String message) {
        Intent intent = new Intent(BROADCAST_MESSAGE_ACTION);
        intent.putExtra(EXTRA_MESSAGE, message);
        context.sendBroadcast(intent);
    }
}
